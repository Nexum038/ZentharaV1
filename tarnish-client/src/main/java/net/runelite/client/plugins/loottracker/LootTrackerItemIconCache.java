package net.runelite.client.plugins.loottracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe item icon cache.
 * Sprites are fetched on the game thread and stored as BufferedImages.
 * The panel reads from the cache on the EDT.
 */
@Slf4j
public class LootTrackerItemIconCache {

    // itemId -> BufferedImage (populated on game thread)
    private final Map<Integer, BufferedImage> cache = new ConcurrentHashMap<>();

    // Items pending sprite fetch
    private final Set<Integer> pending = ConcurrentHashMap.newKeySet();

    // Placeholder for items still loading
    public static final BufferedImage LOADING = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /**
     * Request an icon. Returns cached image or null if not yet loaded.
     * Call from EDT (panel).
     */
    public BufferedImage get(int itemId) {
        BufferedImage img = cache.get(itemId);
        if (img == null) {
            pending.add(itemId);
        }
        return img;
    }

    /**
     * Fetch all pending sprites. Call from game thread (onGameTick).
     */
    public void fetchPending(Client client) {
        if (pending.isEmpty()) return;

        Set<Integer> toFetch = new HashSet<>(pending);
        pending.clear();

        for (int itemId : toFetch) {
            if (cache.containsKey(itemId)) continue;
            try {
                SpritePixels sp = client.createItemSprite(itemId, 1, 1, 0, 0, false, 1);
                if (sp == null) continue;

                BufferedImage img = spriteToImage(sp);
                if (img != null) {
                    cache.put(itemId, img);
                }
            } catch (Exception e) {
                log.debug("[LootTracker] Failed to get sprite for item {}", itemId);
            }
        }
    }

    private BufferedImage spriteToImage(SpritePixels sp) {
        try {
            // The Sprite class itself has: raster (the actual item pixels), width, height
            // Rasterizer2D superclass has: pixels (full screen buffer) - we must SKIP this
            // So we use getDeclaredField on Sprite class only (not superclass)
            Class<?> spriteCls = sp.getClass(); // com.osroyale.Sprite

            java.lang.reflect.Field rasterF = spriteCls.getDeclaredField("raster");
            java.lang.reflect.Field widthF  = spriteCls.getDeclaredField("width");
            java.lang.reflect.Field heightF = spriteCls.getDeclaredField("height");
            rasterF.setAccessible(true);
            widthF.setAccessible(true);
            heightF.setAccessible(true);

            int[] raster = (int[]) rasterF.get(sp);
            int w = (int) widthF.get(sp);
            int h = (int) heightF.get(sp);

            if (raster == null || w <= 0 || h <= 0) return null;

            // Crop to w*h in case raster is larger
            int size = Math.min(raster.length, w * h);
            int[] argb = new int[w * h];
            for (int i = 0; i < size; i++) {
                int rgb = raster[i] & 0xFFFFFF;
                // 0 and very dark colors (item background) are transparent
                if (rgb == 0 || rgb == 0x100010 || rgb == 0x000001) {
                    argb[i] = 0x00000000;
                } else {
                    argb[i] = 0xFF000000 | rgb;
                }
            }
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, w, h, argb, 0, w);
            return img;
        } catch (Exception e) {
            log.debug("[LootTracker] spriteToImage failed", e);
            return null;
        }
    }

    public void clear() {
        cache.clear();
        pending.clear();
    }
}
package net.runelite.client.plugins.loottracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@PluginDescriptor(
        name = "Loot Tracker",
        description = "Tracks loot received from NPCs",
        enabledByDefault = true
)
public class LootTrackerPlugin extends Plugin {

    private static final int MAX_DISTANCE = 8;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File SAVE_DIR = new File(
            System.getProperty("user.home") + File.separator + ".runelite" + File.separator + "loot-tracker");

    @Inject private ClientToolbar clientToolbar;
    @Inject private Client client;
    @Inject private LootTrackerConfig config;

    private LootTrackerPanel panel;
    private NavigationButton navButton;

    private final Map<String, LootTrackerRecord> records = new LinkedHashMap<>();
    private final Map<WorldPoint, List<int[]>> pendingItems = new HashMap<>();
    private boolean playerDropping = false;
    private final Set<Integer> attackedNpcIndices = new HashSet<>();
    // itemName (lowercase) -> server price
    final Map<String, Integer> itemPriceCache = new HashMap<>();
    final LootTrackerItemIconCache iconCache = new LootTrackerItemIconCache();
    private String currentUsername = null;
    private boolean sceneUnloading = false;

    @Override
    protected void startUp() {
        panel = new LootTrackerPanel(config, iconCache);
        panel.addClearListener(this::clearAll);

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/loot_tracker_icon.png");
        navButton = NavigationButton.builder()
                .tooltip("Loot Tracker")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);

        loadData();
        refreshPanel();
    }

    @Override
    protected void shutDown() {
        saveData();
        clientToolbar.removeNavigation(navButton);
        records.clear();
        pendingItems.clear();
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        if (sceneUnloading) return;

        NPC npc = event.getNpc();
        if (npc == null || npc.getName() == null) return;

        final WorldPoint npcLocation = npc.getWorldLocation();
        final String npcName = npc.getName();

        List<WorldPoint> matched = new ArrayList<>();
        for (Map.Entry<WorldPoint, List<int[]>> entry : pendingItems.entrySet()) {
            if (entry.getKey().distanceTo(npcLocation) <= 3) {
                matched.add(entry.getKey());
                for (int[] item : entry.getValue()) {
                    {
                        LootTrackerRecord rec = records.computeIfAbsent(npcName, LootTrackerRecord::new);
                        com.osroyale.ItemDefinition def2 = com.osroyale.ItemDefinition.lookup(item[0]);
                        String iName = def2 != null && def2.name != null ? def2.name : "Unknown";
                        int iPrice = itemPriceCache.getOrDefault(iName.toLowerCase(), def2 != null ? def2.value : 0);
                        rec.addItem(item[0], iName, item[1], iPrice);
                    }
                }
            }
        }

        boolean wasAttacked = attackedNpcIndices.remove(npc.getIndex());

        if (!matched.isEmpty()) {
            matched.forEach(pendingItems::remove);
            // Remove and re-add to move to end of LinkedHashMap (so reverse puts it on top)
            LootTrackerRecord record = records.remove(npcName);
            if (record == null) record = new LootTrackerRecord(npcName);
            record.incrementKill();
            records.put(npcName, record);
            saveData();
            refreshPanel();
        } else if (wasAttacked) {
            // NPC died but dropped nothing - still count as kill
            LootTrackerRecord record = records.remove(npcName);
            if (record == null) record = new LootTrackerRecord(npcName);
            record.incrementKill();
            records.put(npcName, record);
            saveData();
            refreshPanel();
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        playerDropping = event.getMenuOption().equals("Drop");
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event) {
        if (playerDropping) {
            playerDropping = false;
            return;
        }

        final Player local = client.getLocalPlayer();
        if (local == null) return;

        final WorldPoint itemLocation = event.getTile().getWorldLocation();
        if (local.getWorldLocation().distanceTo(itemLocation) > MAX_DISTANCE) return;

        pendingItems.computeIfAbsent(itemLocation, k -> new ArrayList<>())
                .add(new int[]{event.getItem().itemId, event.getItem().itemAmount});
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOADING) {
            sceneUnloading = true;
            pendingItems.clear();
            attackedNpcIndices.clear();
        } else if (event.getGameState() == GameState.LOGGED_IN) {
            sceneUnloading = false;
            // Check if player switched accounts
            String newUsername = client.getUsername();
            if (newUsername != null && !newUsername.equals(currentUsername)) {
                if (currentUsername != null) {
                    saveData();
                }
                currentUsername = newUsername;
                records.clear();
                loadData();
                refreshPanel();
                log.info("[LootTracker] Switched to player: {}", currentUsername);
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        String msg = event.getMessage();
        if (msg == null || !msg.startsWith("@lootprice@")) return;
        try {
            // Format: "@lootprice@Big bones:295"
            String data = msg.substring(11);
            int colonIdx = data.lastIndexOf(":");
            if (colonIdx < 0) return;
            String itemName = data.substring(0, colonIdx).trim();
            int price = Integer.parseInt(data.substring(colonIdx + 1).trim());
            if (price > 0) {
                itemPriceCache.put(itemName.toLowerCase(), price);
                log.debug("[LootTracker] Price from server: {} = {} gp", itemName, price);
                // Update any existing records
                boolean changed = false;
                for (LootTrackerRecord record : records.values()) {
                    for (LootTrackerItem item : record.getItems()) {
                        if (item.getName().equalsIgnoreCase(itemName) && item.getPricePerItem() != price) {
                            item.setPricePerItem(price);
                            changed = true;
                        }
                    }
                }
                if (changed) refreshPanel();
            }
        } catch (Exception ignored) {}
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if (!(event.getSource() instanceof Player)) return;
        if (event.getTarget() instanceof NPC) {
            NPC npc = (NPC) event.getTarget();
            attackedNpcIndices.add(npc.getIndex());
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        sceneUnloading = false;
        pendingItems.clear();
        // Fetch pending item sprites on game thread (safe)
        iconCache.fetchPending(client);
    }

    // ── Persistence ──

    private File getSaveFile() {
        String username = currentUsername != null ? currentUsername.toLowerCase().replaceAll("[^a-z0-9_-]", "_") : "default";
        return new File(SAVE_DIR, username + ".json");
    }

    private void saveData() {
        try {
            // Convert to a serializable format
            List<SavedRecord> toSave = new ArrayList<>();
            for (LootTrackerRecord r : records.values()) {
                SavedRecord sr = new SavedRecord();
                sr.name = r.getName();
                sr.kills = r.getKills();
                sr.items = new ArrayList<>();
                for (LootTrackerItem item : r.getItems()) {
                    SavedItem si = new SavedItem();
                    si.id = item.getId();
                    si.name = item.getName();
                    si.quantity = item.getQuantity();
                    si.price = item.getPricePerItem();
                    sr.items.add(si);
                }
                toSave.add(sr);
            }

            File dir = SAVE_DIR;
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(getSaveFile())) {
                GSON.toJson(toSave, writer);
            }
            log.info("[LootTracker] Saved {} records to: {}", toSave.size(), getSaveFile().getAbsolutePath());
        } catch (Exception e) {
            log.warn("[LootTracker] Failed to save data", e);
        }
    }

    private void loadData() {
        if (!getSaveFile().exists()) return;

        try (FileReader reader = new FileReader(getSaveFile())) {
            Type type = new TypeToken<List<SavedRecord>>(){}.getType();
            List<SavedRecord> saved = GSON.fromJson(reader, type);
            if (saved == null) return;

            for (SavedRecord sr : saved) {
                LootTrackerRecord record = new LootTrackerRecord(sr.name);
                record.setKills(sr.kills);
                if (sr.items != null) {
                    for (SavedItem si : sr.items) {
                        // Use saved name to avoid ItemDefinition.lookup() at startup
                        String name = (si.name != null && !si.name.isEmpty()) ? si.name : "Unknown";
                        record.addItem(si.id, name, si.quantity, si.price);
                    }
                }
                records.put(sr.name, record);
            }

            log.info("[LootTracker] Loaded {} records from: {}", records.size(), getSaveFile().getAbsolutePath());
        } catch (Exception e) {
            log.warn("[LootTracker] Failed to load data", e);
        }
    }

    private void refreshPanel() {
        List<LootTrackerRecord> list = new ArrayList<>(records.values());
        java.util.Collections.reverse(list);
        panel.updateRecords(list);
    }

    private void clearAll() {
        records.clear();
        pendingItems.clear();
        saveData();
        panel.clearRecords();
    }

    @Provides
    LootTrackerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LootTrackerConfig.class);
    }

    // ── Save models ──

    private static class SavedRecord {
        String name;
        int kills;
        List<SavedItem> items;
    }

    private static class SavedItem {
        int id;
        String name;
        int quantity;
        int price;
    }
}
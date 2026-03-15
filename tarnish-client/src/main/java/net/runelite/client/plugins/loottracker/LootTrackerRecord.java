package net.runelite.client.plugins.loottracker;

import com.osroyale.ItemDefinition;
import net.runelite.client.game.ItemStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LootTrackerRecord {

    private final String name;
    private int kills;
    private final List<LootTrackerItem> items = new ArrayList<>();

    public LootTrackerRecord(String name) {
        this.name = name;
        this.kills = 0;
    }

    public String getName() { return name; }
    public int getKills() { return kills; }
    public List<LootTrackerItem> getItems() { return items; }

    public void incrementKill() {
        kills++;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addItem(int itemId, int quantity) {
        final ItemDefinition def = ItemDefinition.lookup(itemId);
        final String itemName = def != null ? def.name : "Unknown";
        final int value = def != null ? def.value : 0;
        // log.debug("[LootTracker] item {} value={}", itemName, value);

        for (LootTrackerItem existing : items) {
            if (existing.getId() == itemId) {
                existing.addQuantity(quantity);
                return;
            }
        }
        items.add(new LootTrackerItem(itemId, itemName, quantity, value));
    }

    public void addItem(int itemId, String itemName, int quantity) {
        for (LootTrackerItem existing : items) {
            if (existing.getId() == itemId) {
                existing.addQuantity(quantity);
                return;
            }
        }
        items.add(new LootTrackerItem(itemId, itemName, quantity, 0));
    }

    public void addItem(int itemId, String itemName, int quantity, int pricePerItem) {
        for (LootTrackerItem existing : items) {
            if (existing.getId() == itemId) {
                existing.addQuantity(quantity);
                if (pricePerItem > 0) existing.setPricePerItem(pricePerItem);
                return;
            }
        }
        items.add(new LootTrackerItem(itemId, itemName, quantity, pricePerItem));
    }

    public void addKill(Collection<ItemStack> loot) {
        kills++;
        for (ItemStack stack : loot) {
            addItem(stack.getId(), stack.getQuantity());
        }
    }

    public int getTotalValue() {
        return items.stream().mapToInt(LootTrackerItem::getTotalValue).sum();
    }
}
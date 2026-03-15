package net.runelite.client.plugins.loottracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("loottracker")
public interface LootTrackerConfig extends Config {

    @ConfigItem(
        keyName = "showPrices",
        name = "Show item prices",
        description = "Show the total value of each item in the loot tracker",
        position = 0
    )
    default boolean showPrices() {
        return true;
    }

    @ConfigItem(
        keyName = "showKillCount",
        name = "Show kill count",
        description = "Show the kill count for each NPC",
        position = 1
    )
    default boolean showKillCount() {
        return true;
    }

    @ConfigItem(
        keyName = "sortByValue",
        name = "Sort items by value",
        description = "Sort items by their total value (highest first)",
        position = 2
    )
    default boolean sortByValue() {
        return true;
    }
}

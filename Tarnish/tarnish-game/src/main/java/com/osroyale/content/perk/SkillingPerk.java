package com.osroyale.content.perk;

/**
 * All available skilling perks.
 */
public enum SkillingPerk {

    SWIFT_HANDS("Swift Hands", "Reduces gathering skill tick speed.", 2, new int[]{1, 2});

    public final String name;
    public final String description;
    public final int maxLevel;
    public final int[] effectPerLevel;

    SkillingPerk(String name, String description, int maxLevel, int[] effectPerLevel) {
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.effectPerLevel = effectPerLevel;
    }

    public int getEffect(int level) {
        if (level <= 0 || level > maxLevel) return 0;
        return effectPerLevel[level - 1];
    }
}
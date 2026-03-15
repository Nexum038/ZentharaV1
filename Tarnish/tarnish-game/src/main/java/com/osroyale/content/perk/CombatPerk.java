package com.osroyale.content.perk;

/**
 * All available combat perks.
 * Each perk has a max level and an effect value per level.
 *
 * For SWIFT_STRIKES the effect is a melee tick reduction:
 *   level 1 = -1 tick, level 2 = -2 ticks
 */
public enum CombatPerk {

    SWIFT_STRIKES("Swift Strikes", "Reduces melee attack speed.", 2, new int[]{1, 2});

    public final String name;
    public final String description;
    public final int maxLevel;

    /** effectPerLevel[0] = level 1 effect, [1] = level 2 effect, etc. */
    public final int[] effectPerLevel;

    CombatPerk(String name, String description, int maxLevel, int[] effectPerLevel) {
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.effectPerLevel = effectPerLevel;
    }

    /** Returns the effect value for the given level (1-based). Returns 0 if locked or out of range. */
    public int getEffect(int level) {
        if (level <= 0 || level > maxLevel) return 0;
        return effectPerLevel[level - 1];
    }
}

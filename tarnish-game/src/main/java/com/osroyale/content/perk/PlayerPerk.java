package com.osroyale.content.perk;

import com.osroyale.game.world.entity.mob.player.Player;
import plugin.click.button.PerkButtonPlugin;
import plugin.click.button.SkillingButtonPlugin;

public class PlayerPerk {

    // -----------------------------------------------------------------
    // Combat — persisted
    // -----------------------------------------------------------------
    public int combatPointsEarned = 0;
    public int combatPoints = 0;
    public int[] combatPerkLevels = new int[CombatPerk.values().length];
    public int activeCombatPerk = -1;

    // -----------------------------------------------------------------
    // Skilling — persisted
    // -----------------------------------------------------------------
    public int skillingPointsEarned = 0;
    public int skillingPoints = 0;
    public int[] skillingPerkLevels = new int[SkillingPerk.values().length];
    public int activeSkillingPerk = -1;

    // -----------------------------------------------------------------
    // Transient
    // -----------------------------------------------------------------
    public transient double combatXpAccumulator = 0.0;
    public transient double skillingXpAccumulator = 0.0;
    private transient Player player;

    // -----------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------
    private static final double BASE_XP = 1_000.0;
    private static final double STEP_XP = 500.0;

    // -----------------------------------------------------------------
    // Init
    // -----------------------------------------------------------------
    public void init(Player player) {
        this.player = player;
    }

    // -----------------------------------------------------------------
    // Combat XP -> Points
    // -----------------------------------------------------------------
    public void onCombatXpGained(double xp) {
        combatXpAccumulator += xp;
        double threshold = nextCombatPointCost();
        boolean earned = false;
        while (combatXpAccumulator >= threshold) {
            combatXpAccumulator -= threshold;
            combatPointsEarned++;
            combatPoints++;
            threshold = nextCombatPointCost();
            earned = true;
            if (player != null)
                player.message("<col=ff9900>[Perks]</col> You earned a combat perk point! "
                        + "You now have <col=ffffff>" + combatPoints + "</col> point(s).");
        }
        if (earned && player != null)
            PerkButtonPlugin.refreshInterface(player);
    }

    public double nextCombatPointCost() {
        return BASE_XP + (combatPointsEarned * STEP_XP);
    }

    // kept for backwards compat
    public double nextPointCost() {
        return nextCombatPointCost();
    }

    // -----------------------------------------------------------------
    // Skilling XP -> Points
    // -----------------------------------------------------------------
    public void onSkillingXpGained(double xp) {
        skillingXpAccumulator += xp;
        double threshold = nextSkillingPointCost();
        boolean earned = false;
        while (skillingXpAccumulator >= threshold) {
            skillingXpAccumulator -= threshold;
            skillingPointsEarned++;
            skillingPoints++;
            threshold = nextSkillingPointCost();
            earned = true;
            if (player != null)
                player.message("<col=00b4ff>[Perks]</col> You earned a skilling perk point! "
                        + "You now have <col=ffffff>" + skillingPoints + "</col> point(s).");
        }
        if (earned && player != null)
            SkillingButtonPlugin.refreshInterface(player);
    }

    public double nextSkillingPointCost() {
        return BASE_XP + (skillingPointsEarned * STEP_XP);
    }

    // -----------------------------------------------------------------
    // Combat unlock/upgrade/activate
    // -----------------------------------------------------------------
    public boolean unlockOrUpgrade(CombatPerk perk, int cost) {
        int currentLevel = combatPerkLevels[perk.ordinal()];
        if (currentLevel >= perk.maxLevel) {
            if (player != null) player.message("<col=ff9900>[Perks]</col> This perk is already at maximum level!");
            return false;
        }
        if (combatPoints < cost) {
            if (player != null) player.message("<col=ff9900>[Perks]</col> You need <col=ffffff>" + cost
                    + "</col> combat perk point(s) but only have <col=ffffff>" + combatPoints + "</col>.");
            return false;
        }
        combatPoints -= cost;
        combatPerkLevels[perk.ordinal()]++;
        int newLevel = combatPerkLevels[perk.ordinal()];
        if (player != null) player.message("<col=ff9900>[Perks]</col> <col=ffffff>" + perk.name
                + "</col> is now level <col=ffffff>" + newLevel + "</col>!");
        return true;
    }

    public void setActive(CombatPerk perk) {
        if (combatPerkLevels[perk.ordinal()] == 0) {
            if (player != null) player.message("<col=ff9900>[Perks]</col> You have not unlocked " + perk.name + " yet!");
            return;
        }
        activeCombatPerk = perk.ordinal();
    }

    public void deactivate() {
        activeCombatPerk = -1;
    }

    // -----------------------------------------------------------------
    // Skilling unlock/upgrade/activate
    // -----------------------------------------------------------------
    public boolean unlockOrUpgradeSkilling(SkillingPerk perk, int cost) {
        int currentLevel = skillingPerkLevels[perk.ordinal()];
        if (currentLevel >= perk.maxLevel) {
            if (player != null) player.message("<col=00b4ff>[Perks]</col> This perk is already at maximum level!");
            return false;
        }
        if (skillingPoints < cost) {
            if (player != null) player.message("<col=00b4ff>[Perks]</col> You need <col=ffffff>" + cost
                    + "</col> skilling perk point(s) but only have <col=ffffff>" + skillingPoints + "</col>.");
            return false;
        }
        skillingPoints -= cost;
        skillingPerkLevels[perk.ordinal()]++;
        int newLevel = skillingPerkLevels[perk.ordinal()];
        if (player != null) player.message("<col=00b4ff>[Perks]</col> <col=ffffff>" + perk.name
                + "</col> is now level <col=ffffff>" + newLevel + "</col>!");
        return true;
    }

    public void setActiveSkilling(SkillingPerk perk) {
        if (skillingPerkLevels[perk.ordinal()] == 0) {
            if (player != null) player.message("<col=00b4ff>[Perks]</col> You have not unlocked " + perk.name + " yet!");
            return;
        }
        activeSkillingPerk = perk.ordinal();
    }

    public void deactivateSkilling() {
        activeSkillingPerk = -1;
    }

    // -----------------------------------------------------------------
    // Effect queries
    // -----------------------------------------------------------------
    public int getMeleeTickReduction() {
        if (activeCombatPerk != CombatPerk.SWIFT_STRIKES.ordinal()) return 0;
        return CombatPerk.SWIFT_STRIKES.getEffect(combatPerkLevels[CombatPerk.SWIFT_STRIKES.ordinal()]);
    }

    public int getSkillingTickReduction(SkillingPerk perk) {
        if (activeSkillingPerk != perk.ordinal()) return 0;
        return perk.getEffect(skillingPerkLevels[perk.ordinal()]);
    }
}
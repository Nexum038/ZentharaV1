package plugin.click.button;

import com.osroyale.content.perk.CombatPerk;
import com.osroyale.content.perk.PlayerPerk;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.net.packet.out.SendString;
import com.osroyale.net.packet.out.SendMessage;
import com.osroyale.game.plugin.PluginContext;

/**
 * Combat Perks interface (id 60200).
 * Close:         60205 -> -5331
 * Tab Skilling:  58801 -> -6735
 * Left  btns: -5282,-5275,-5268,-5261,-5254,-5247,-5240,-5233
 * Right btns: -5280,-5273,-5266,-5259,-5252,-5245,-5238,-5231
 */
public class PerkButtonPlugin extends PluginContext {

    private static final int INTERFACE_ID = 60200;
    public  static final int UNLOCK_COST  = 10;

    private static final int WID_POINTS = 60209;
    private static final int WID_COST   = 60211;
    private static final int WID_EARNED = 60213;

    private static final int CLOSE_BTN    = -5331;
    private static final int TAB_SKILLING = -6735;

    private static final int[] ROW_LEFT_BTNS  = { -5282, -5275, -5268, -5261, -5254, -5247, -5240, -5233 };
    private static final int[] ROW_RIGHT_BTNS = { -5280, -5273, -5266, -5259, -5252, -5245, -5238, -5231 };

    public static int rowNameWidget(int i)       { return 60252 + i * 7; }
    public static int rowBadgeWidget(int i)      { return 60253 + i * 7; }
    public static int rowLeftLabelWidget(int i)  { return 60255 + i * 7; }
    public static int rowRightLabelWidget(int i) { return 60257 + i * 7; }

    @Override
    protected boolean onClick(Player player, int button) {
        if (button == CLOSE_BTN) {
            player.interfaceManager.close();
            return true;
        }
        if (button == TAB_SKILLING) {
            SkillingButtonPlugin.open(player);
            return true;
        }
        for (int i = 0; i < ROW_LEFT_BTNS.length; i++) {
            if (button == ROW_LEFT_BTNS[i]) {
                handleLeftButton(player, i);
                return true;
            }
        }
        for (int i = 0; i < ROW_RIGHT_BTNS.length; i++) {
            if (button == ROW_RIGHT_BTNS[i]) {
                handleRightButton(player, i);
                return true;
            }
        }
        return false;
    }

    private static void handleLeftButton(Player player, int row) {
        CombatPerk[] perks = CombatPerk.values();
        if (row >= perks.length) return;
        CombatPerk perk = perks[row];
        PlayerPerk pp   = player.perk;
        int curLevel    = pp.combatPerkLevels[perk.ordinal()];
        if (curLevel >= perk.maxLevel) {
            player.send(new SendMessage(perk.name + " is already at max level!"));
            return;
        }
        if (pp.unlockOrUpgrade(perk, UNLOCK_COST)) {
            refreshInterface(player);
        }
    }

    private static void handleRightButton(Player player, int row) {
        CombatPerk[] perks = CombatPerk.values();
        if (row >= perks.length) return;
        CombatPerk perk = perks[row];
        PlayerPerk pp   = player.perk;
        int curLevel    = pp.combatPerkLevels[perk.ordinal()];
        if (curLevel == 0) {
            player.send(new SendMessage("You need to unlock " + perk.name + " first!"));
            return;
        }
        if (pp.activeCombatPerk == perk.ordinal()) {
            pp.deactivate();
            player.message("<col=ff9900>[Perks]</col> " + perk.name + " deactivated.");
        } else {
            pp.setActive(perk);
            player.message("<col=ff9900>[Perks]</col> " + perk.name + " activated!");
        }
        refreshInterface(player);
    }

    public static void open(Player player) {
        refreshInterface(player);
        player.interfaceManager.open(INTERFACE_ID);
    }

    public static void refreshInterface(Player player) {
        PlayerPerk pp = player.perk;
        player.send(new SendString(String.valueOf(pp.combatPoints),                          WID_POINTS));
        player.send(new SendString(String.format("%,d XP", (long) pp.nextCombatPointCost()), WID_COST));
        player.send(new SendString(String.format("%,d", pp.combatPointsEarned),              WID_EARNED));

        CombatPerk[] perks = CombatPerk.values();
        for (int i = 0; i < ROW_LEFT_BTNS.length; i++) {
            if (i < perks.length) {
                CombatPerk perk  = perks[i];
                int curLevel     = pp.combatPerkLevels[perk.ordinal()];
                int maxLevel     = perk.maxLevel;
                boolean isActive = pp.activeCombatPerk == perk.ordinal();

                player.send(new SendString(perk.name, rowNameWidget(i)));
                player.send(new SendString("Lvl " + curLevel + " / " + maxLevel, rowBadgeWidget(i)));

                String leftLabel = curLevel == 0      ? "Unlock"
                        : curLevel < maxLevel ? "Upgrade"
                        :                       "MAX";
                player.send(new SendString(leftLabel, rowLeftLabelWidget(i)));

                String rightLabel = curLevel == 0 ? "--"
                        : isActive       ? "Deactivate"
                        :                  "Activate";
                player.send(new SendString(rightLabel, rowRightLabelWidget(i)));
            } else {
                player.send(new SendString("",   rowNameWidget(i)));
                player.send(new SendString("",   rowBadgeWidget(i)));
                player.send(new SendString("--", rowLeftLabelWidget(i)));
                player.send(new SendString("--", rowRightLabelWidget(i)));
            }
        }
    }
}
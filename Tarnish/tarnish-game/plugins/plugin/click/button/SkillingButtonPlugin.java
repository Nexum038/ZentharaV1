package plugin.click.button;

import com.osroyale.content.perk.SkillingPerk;
import com.osroyale.content.perk.PlayerPerk;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.net.packet.out.SendString;
import com.osroyale.net.packet.out.SendMessage;
import com.osroyale.game.plugin.PluginContext;

/**
 * Skilling Perks interface (id 59700).
 * Close:        59705 -> -5831
 * Tab Combat:   58900 -> -6636
 * Left  btns: -5782,-5775,-5768,-5761,-5754,-5747,-5740,-5733
 * Right btns: -5780,-5773,-5766,-5759,-5752,-5745,-5738,-5731
 */
public class SkillingButtonPlugin extends PluginContext {

    private static final int INTERFACE_ID = 59700;
    public  static final int UNLOCK_COST  = 10;

    private static final int WID_POINTS = 59709;
    private static final int WID_COST   = 59711;
    private static final int WID_EARNED = 59713;

    private static final int CLOSE_BTN  = -5831;
    private static final int TAB_COMBAT = -6636;

    private static final int[] ROW_LEFT_BTNS  = { -5782, -5775, -5768, -5761, -5754, -5747, -5740, -5733 };
    private static final int[] ROW_RIGHT_BTNS = { -5780, -5773, -5766, -5759, -5752, -5745, -5738, -5731 };

    public static int rowNameWidget(int i)       { return 59752 + i * 7; }
    public static int rowBadgeWidget(int i)      { return 59753 + i * 7; }
    public static int rowLeftLabelWidget(int i)  { return 59755 + i * 7; }
    public static int rowRightLabelWidget(int i) { return 59757 + i * 7; }

    @Override
    protected boolean onClick(Player player, int button) {
        if (button == CLOSE_BTN) {
            player.interfaceManager.close();
            return true;
        }
        if (button == TAB_COMBAT) {
            PerkButtonPlugin.open(player);
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
        SkillingPerk[] perks = SkillingPerk.values();
        if (row >= perks.length) return;
        SkillingPerk perk = perks[row];
        PlayerPerk pp     = player.perk;
        int curLevel      = pp.skillingPerkLevels[perk.ordinal()];
        if (curLevel >= perk.maxLevel) {
            player.send(new SendMessage(perk.name + " is already at max level!"));
            return;
        }
        if (pp.unlockOrUpgradeSkilling(perk, UNLOCK_COST)) {
            refreshInterface(player);
        }
    }

    private static void handleRightButton(Player player, int row) {
        SkillingPerk[] perks = SkillingPerk.values();
        if (row >= perks.length) return;
        SkillingPerk perk = perks[row];
        PlayerPerk pp     = player.perk;
        int curLevel      = pp.skillingPerkLevels[perk.ordinal()];
        if (curLevel == 0) {
            player.send(new SendMessage("You need to unlock " + perk.name + " first!"));
            return;
        }
        if (pp.activeSkillingPerk == perk.ordinal()) {
            pp.deactivateSkilling();
            player.message("<col=00b4ff>[Perks]</col> " + perk.name + " deactivated.");
        } else {
            pp.setActiveSkilling(perk);
            player.message("<col=00b4ff>[Perks]</col> " + perk.name + " activated!");
        }
        refreshInterface(player);
    }

    public static void open(Player player) {
        refreshInterface(player);
        player.interfaceManager.open(INTERFACE_ID);
    }

    public static void refreshInterface(Player player) {
        PlayerPerk pp = player.perk;
        player.send(new SendString(String.valueOf(pp.skillingPoints),                           WID_POINTS));
        player.send(new SendString(String.format("%,d XP", (long) pp.nextSkillingPointCost()),  WID_COST));
        player.send(new SendString(String.format("%,d", pp.skillingPointsEarned),               WID_EARNED));

        SkillingPerk[] perks = SkillingPerk.values();
        for (int i = 0; i < ROW_LEFT_BTNS.length; i++) {
            if (i < perks.length) {
                SkillingPerk perk = perks[i];
                int curLevel      = pp.skillingPerkLevels[perk.ordinal()];
                int maxLevel      = perk.maxLevel;
                boolean isActive  = pp.activeSkillingPerk == perk.ordinal();

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
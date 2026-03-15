package plugin.click.button;

import com.osroyale.Config;
import com.osroyale.content.skill.impl.magic.teleport.Teleportation;
import com.osroyale.content.teleport.TeleportHandler;
import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.combat.strategy.player.special.CombatSpecial;
import com.osroyale.game.plugin.PluginContext;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.mob.player.PlayerRight;
import com.osroyale.net.packet.out.SendMessage;
import com.osroyale.net.packet.out.SendRunEnergy;

/**
 * Handles all client-side hotkey button packets.
 *
 * Button IDs:
 *   -9217 = CTRL+B (open bank)
 *   -9216 = CTRL+H (home teleport)
 *
 * To add a new hotkey:
 *   1. Add a new button ID constant below
 *   2. Add a case in onClick()
 *   3. Add sendHotkey(id) in client-side KeybindManager.java
 *   4. Add the key trigger in client-side KeyHandler.java
 */
public class HotkeyButtonPlugin extends PluginContext {

    private static final int HOTKEY_OPEN_BANK      = -9217;
    private static final int HOTKEY_HOME_TELEPORT  = -9216;
    private static final int HOTKEY_TELEPORT_MENU  = -9215;
    // Add future hotkeys here:
    // private static final int HOTKEY_OPEN_EQUIPMENT = -9215;

    @Override
    protected boolean onClick(Player player, int button) {
        switch (button) {

            case HOTKEY_OPEN_BANK:
                return handleOpenBank(player);

            case HOTKEY_HOME_TELEPORT:
                return handleHomeTeleport(player);

            case HOTKEY_TELEPORT_MENU:
                return handleTeleportMenu(player);

            // Add future hotkeys here:
            // case HOTKEY_OPEN_EQUIPMENT:
            //     return handleOpenEquipment(player);

            default:
                return false;
        }
    }

    /** CTRL+B — Opens the bank. Requires moderator+. */
    private boolean handleOpenBank(Player player) {
        if (!PlayerRight.isModerator(player)) {
            player.send(new SendMessage("You need to be a moderator or higher to use this hotkey."));
            return true;
        }
        player.bank.open();
        return true;
    }

    /** CTRL+H — Teleports the player home. Extreme+ gets a full stat restore. */
    private boolean handleHomeTeleport(Player player) {
        if (player.getCombat().isUnderAttackByPlayer()) {
            player.send(new SendMessage("You can not teleport whilst in combat!"));
            return true;
        }
        Teleportation.teleport(player, Config.DEFAULT_POSITION);
        if (PlayerRight.isExtreme(player)) {
            World.schedule(3, () -> {
                player.unpoison();
                player.unvenom();
                player.playerAssistant.resetEffects();
                player.playerAssistant.setWidget();
                player.runEnergy = 100;
                player.teleblockTimer.set(0);
                player.send(new SendRunEnergy());
                player.skills.restoreAll();
                CombatSpecial.restore(player, 100);
                player.send(new SendMessage("<img=7><col=158A76> Thank you for your extreme support! Your stats have been fully restored."));
            });
        } else {
            player.send(new SendMessage("You have been teleported home."));
        }
        return true;
    }

    /** CTRL+T — Opens the teleport menu. */
    private boolean handleTeleportMenu(Player player) {
        TeleportHandler.open(player);
        return true;
    }

}
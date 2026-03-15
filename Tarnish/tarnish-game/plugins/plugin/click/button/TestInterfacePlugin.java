package plugin.click.button;

import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.plugin.PluginContext;

public class TestInterfacePlugin extends PluginContext {

    private static final int INTERFACE_ID = 24127;
    private static final int TEST_BUTTON = 24205;

    @Override
    protected boolean onClick(Player player, int button) {
        if (button == TEST_BUTTON) {
            player.message("Button 24205 werkt!");
            return true;
        }
        return false;
    }

    public static void open(Player player) {
        player.interfaceManager.open(INTERFACE_ID);
    }
}

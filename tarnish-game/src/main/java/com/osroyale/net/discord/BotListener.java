package com.osroyale.net.discord;

import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.skill.Skill;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Optional;

public class BotListener implements EventListener {

    private static final java.util.Set<String> ADMIN_IDS = new java.util.HashSet<>(
            java.util.Arrays.asList("348208280201592843")
    );

    private boolean isAdmin(MessageReceivedEvent msg) {
        return ADMIN_IDS.contains(msg.getAuthor().getId());
    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (DiscordPlugin.getJDA() == null) {
            System.out.println("Returning JDA FAIL");
            return;
        }
        if (!(genericEvent instanceof MessageReceivedEvent)) return;

        MessageReceivedEvent msg = (MessageReceivedEvent) genericEvent;

        // Ignore bot messages
        if (msg.getAuthor().isBot()) return;

        String message = msg.getMessage().getContentDisplay();
        System.out.println("[Discord] Received message: " + message + " from " + msg.getAuthor().getName());

        if (!message.startsWith(Constants.COMMAND_PREFIX)) return;
        System.out.println("[Discord] Processing command: " + message);

        String command = message.substring(Constants.COMMAND_PREFIX.length()).toLowerCase();
        String[] cmd = command.split(" ");

        switch (cmd[0]) {

            case "players":
            case "online":
                int count = 0;
                StringBuilder playerList = new StringBuilder();
                for (Player p : World.getPlayers()) {
                    count++;
                    playerList.append("• ").append(p.getUsername()).append(" (lvl ").append(p.skills.getCombatLevel()).append(")\n");
                }
                EmbedBuilder onlineEmbed = new EmbedBuilder()
                        .setTitle("🟢 Online Players — " + count)
                        .setColor(Color.GREEN)
                        .setDescription(count == 0 ? "No players online." : playerList.toString());
                msg.getChannel().sendMessageEmbeds(onlineEmbed.build()).queue();
                break;

            case "stats":
                if (cmd.length < 2) {
                    msg.getChannel().sendMessage("Usage: `!stats <playername>`").queue();
                    break;
                }
                String targetName = cmd[1];
                Optional<Player> found = Optional.empty();
                for (Player p : World.getPlayers()) {
                    if (p.getUsername().equalsIgnoreCase(targetName)) {
                        found = Optional.of(p);
                        break;
                    }
                }
                if (!found.isPresent()) {
                    msg.getChannel().sendMessage("❌ Player `" + targetName + "` is not online.").queue();
                    break;
                }
                Player target = found.get();

                // Calculate bank value
                long bankValue = 0;
                for (int i = 0; i < target.bank.capacity(); i++) {
                    com.osroyale.game.world.items.Item item = target.bank.get(i);
                    if (item != null) {
                        bankValue += (long) item.getValue() * item.getAmount();
                    }
                }

                // Skills string
                String skills = String.format(
                        "⚔️ Attack: %d | 🛡️ Defence: %d | 💪 Strength: %d\n" +
                                "❤️ Hitpoints: %d | 🏹 Ranged: %d | 🙏 Prayer: %d\n" +
                                "🔮 Magic: %d | 🍳 Cooking: %d | 🪓 Woodcutting: %d\n" +
                                "🏹 Fletching: %d | 🎣 Fishing: %d | 🔥 Firemaking: %d\n" +
                                "⚒️ Crafting: %d | ⚙️ Smithing: %d | ⛏️ Mining: %d\n" +
                                "🌿 Herblore: %d | 🏃 Agility: %d | 🗡️ Thieving: %d\n" +
                                "💀 Slayer: %d | 🌾 Farming: %d | 🔯 Runecrafting: %d\n" +
                                "🏹 Hunter: %d | 🔨 Construction: %d",
                        target.skills.getMaxLevel(Skill.ATTACK),
                        target.skills.getMaxLevel(Skill.DEFENCE),
                        target.skills.getMaxLevel(Skill.STRENGTH),
                        target.skills.getMaxLevel(Skill.HITPOINTS),
                        target.skills.getMaxLevel(Skill.RANGED),
                        target.skills.getMaxLevel(Skill.PRAYER),
                        target.skills.getMaxLevel(Skill.MAGIC),
                        target.skills.getMaxLevel(Skill.COOKING),
                        target.skills.getMaxLevel(Skill.WOODCUTTING),
                        target.skills.getMaxLevel(Skill.FLETCHING),
                        target.skills.getMaxLevel(Skill.FISHING),
                        target.skills.getMaxLevel(Skill.FIREMAKING),
                        target.skills.getMaxLevel(Skill.CRAFTING),
                        target.skills.getMaxLevel(Skill.SMITHING),
                        target.skills.getMaxLevel(Skill.MINING),
                        target.skills.getMaxLevel(Skill.HERBLORE),
                        target.skills.getMaxLevel(Skill.AGILITY),
                        target.skills.getMaxLevel(Skill.THIEVING),
                        target.skills.getMaxLevel(Skill.SLAYER),
                        target.skills.getMaxLevel(Skill.FARMING),
                        target.skills.getMaxLevel(Skill.RUNECRAFTING),
                        target.skills.getMaxLevel(Skill.HUNTER),
                        target.skills.getMaxLevel(Skill.CONSTRUCTION)
                );

                EmbedBuilder statsEmbed = new EmbedBuilder()
                        .setTitle("📊 Stats — " + target.getUsername())
                        .setColor(new Color(0xFF981F))
                        .addField("⚔️ Combat Level", String.valueOf(target.skills.getCombatLevel()), true)
                        .addField("📈 Total Level", String.valueOf(target.skills.getTotalLevel()), true)
                        .addField("💰 Bank Value", formatValue(bankValue) + " gp", true)
                        .addField("📋 Skills", skills, false);

                msg.getChannel().sendMessageEmbeds(statsEmbed.build()).queue();
                break;

            case "pollyn":
                String question = command.substring(7);
                DiscordPlugin.pollYN(question);
                break;

            case "updatelog":
                if (msg.getMember() != null && msg.getMember().isOwner()) {
                    DiscordPlugin.sendUpdateMessage("Zenthara");
                }
                break;

            case "giveitem":
                // !giveitem <player> <itemId> <amount>
                if (!isAdmin(msg)) {
                    msg.getChannel().sendMessage("❌ You don't have permission to use this command.").queue();
                    break;
                }
                if (cmd.length < 4) {
                    msg.getChannel().sendMessage("Usage: `!giveitem <player> <itemId> <amount>`").queue();
                    break;
                }
                String giveName = cmd[1];
                int itemId, itemAmount;
                try {
                    itemId = Integer.parseInt(cmd[2]);
                    itemAmount = Integer.parseInt(cmd[3]);
                } catch (NumberFormatException e) {
                    msg.getChannel().sendMessage("❌ Invalid item ID or amount.").queue();
                    break;
                }
                Player giveTarget = null;
                for (Player p : World.getPlayers()) {
                    if (p.getUsername().equalsIgnoreCase(giveName)) {
                        giveTarget = p;
                        break;
                    }
                }
                if (giveTarget == null) {
                    msg.getChannel().sendMessage("❌ Player `" + giveName + "` is not online.").queue();
                    break;
                }
                com.osroyale.game.world.items.Item giveItem = new com.osroyale.game.world.items.Item(itemId, itemAmount);
                giveTarget.inventory.add(itemId, itemAmount);
                msg.getChannel().sendMessage("✅ Gave **" + itemAmount + "x " + giveItem.getName() + "** to **" + giveTarget.getUsername() + "**.").queue();
                break;

            case "help":
                EmbedBuilder helpEmbed = new EmbedBuilder()
                        .setTitle("📖 Zenthara Bot Commands")
                        .setColor(new Color(0xFF981F))
                        .addField("!online", "Show all online players", false)
                        .addField("!stats <name>", "Show player stats", false)
                        .addField("!pollyn <question>", "Create a yes/no poll", false);
                msg.getChannel().sendMessageEmbeds(helpEmbed.build()).queue();
                break;
        }
    }

    private String formatValue(long value) {
        if (value >= 1_000_000_000) return String.format("%.1fB", value / 1_000_000_000.0);
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }
}
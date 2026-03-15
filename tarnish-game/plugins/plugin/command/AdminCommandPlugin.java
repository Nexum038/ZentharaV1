package plugin.command;

import com.osroyale.Config;
import com.osroyale.content.skill.impl.magic.Spellbook;
import com.osroyale.game.plugin.extension.CommandExtension;
import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.mob.Direction;
import com.osroyale.game.world.entity.mob.Mob;
import com.osroyale.game.world.entity.mob.data.LockType;
import com.osroyale.game.world.entity.mob.npc.Npc;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.mob.player.PlayerRight;
import com.osroyale.game.world.entity.mob.player.command.Command;
import com.osroyale.game.world.entity.mob.player.command.CommandParser;
import com.osroyale.game.world.entity.skill.Skill;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.items.ItemDefinition;
import com.osroyale.game.world.items.containers.ItemContainer;
import com.osroyale.game.world.position.Position;
import com.osroyale.net.packet.out.SendItemOnInterface;
import com.osroyale.net.packet.out.SendMessage;
import com.osroyale.net.packet.out.SendScrollbar;
import com.osroyale.net.packet.out.SendString;
import com.osroyale.content.perk.CombatPerk;
import com.osroyale.game.world.entity.combat.strategy.player.special.CombatSpecial;
import com.osroyale.util.MessageColor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdminCommandPlugin extends CommandExtension {

    @Override
    public void register() {
        commands.add(new Command("pnpc") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.playerAssistant.transform(parser.nextInt());
            }
        });

        commands.add(new Command("spawnnpc") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    int id = parser.nextInt();
                    Npc npc = new Npc(id, player.getPosition(), Config.NPC_WALKING_RADIUS, Mob.DEFAULT_INSTANCE, Direction.NORTH);
                    npc.register();
                    npc.locking.lock(LockType.MASTER);
                    Path path = Paths.get("./data/def/npc/npc_spawns.json");

                    try {
                        if (!Files.exists(path)) {
                            Files.createFile(path);
                        }
                        FileWriter fileWriter = new FileWriter(path.toFile(), true);
                        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                        bufferedWriter.write("  {");
                        bufferedWriter.write("    \"id\": " + id + ",");
                        bufferedWriter.write("    \"radius\": \"" + Config.NPC_WALKING_RADIUS + "\",");
                        bufferedWriter.write("    \"facing\": \"NORTH\",");
                        bufferedWriter.write("    \"position\": {");
                        bufferedWriter.write("      \"x\": " + player.getPosition().getX() + ",");
                        bufferedWriter.write("      \"y\": " + player.getPosition().getY() + ",");
                        bufferedWriter.write("      \"height\": " + player.getPosition().getHeight() + "");
                        bufferedWriter.write("    }");
                        bufferedWriter.write("  },");
                        bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    player.send(new SendMessage("Npc " + id + " has been spawned."));
                }
            }
        });

        commands.add(new Command("points") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.dragonfireCharges = 50;
                player.slayer.setPoints(50000);
                player.donation.setCredits(50000);
                player.votePoints = 500000;
                player.pestPoints = 500000;
                player.skillingPoints = 500000;
                player.message("Enjoy deh points");
            }
        });

        commands.add(new Command("demote") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());
                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }
                    World.search(name.toString()).ifPresent(other -> {
                        if (PlayerRight.isDeveloper(other)) {
                            return;
                        }

                        other.right = PlayerRight.PLAYER;
                        other.dialogueFactory.sendStatement("You have been demoted!").execute();
                        player.message("demote was complete");
                    });
                } else {
                    player.message("Invalid command use; ::demote settings");
                }
            }
        });

        commands.add(new Command("save", "saveworld", "savegame") {
            @Override
            public void execute(Player player, CommandParser parser) {
                World.save();
                player.send(new SendMessage("All data has been successfully saved."));
            }
        });

        commands.add(new Command("bank") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.bank.open();
            }
        });

        commands.add(new Command("move") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext(3)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = parser.nextInt();
                    player.move(player.getPosition().transform(x, y, z));
                } else if (parser.hasNext(2)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = player.getHeight();
                    player.move(player.getPosition().transform(x, y, z));
                } else return;

                if (player.debug) {
                    player.send(new SendMessage("You have teleported to the coordinates: " + player.getPosition(), MessageColor.BLUE));
                }
            }
        });

        commands.add(new Command("tele") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext(3)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = parser.nextInt();
                    player.move(new Position(x, y, z));
                } else if (parser.hasNext(2)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = player.getHeight();
                    player.move(new Position(x, y, z));
                } else return;
                if (player.debug) {
                    player.send(new SendMessage("You have teleported to the coordinates: " + player.getPosition(), MessageColor.BLUE));
                }
            }
        });

        commands.add(new Command("spellbook") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    String spellbook = parser.nextString();
                    switch (spellbook.toUpperCase()) {
                        case "LUNAR":
                            player.spellbook = Spellbook.LUNAR;
                            break;
                        case "MODERN":
                            player.spellbook = Spellbook.MODERN;
                            break;
                        case "ANCIENT":
                            player.spellbook = Spellbook.ANCIENT;
                            break;
                    }
                    player.interfaceManager.setSidebar(Config.MAGIC_TAB, player.spellbook.getInterfaceId());
                }
            }
        });

        commands.add(new Command("starterbank") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.bank.clear();
                player.bank.addAll(Config.STARTER_BANK);
                System.arraycopy(Config.STARTER_BANK_AMOUNT, 0, player.bank.tabAmounts, 0, Config.STARTER_BANK_AMOUNT.length);
                player.bank.shift();
                player.bank.open();
            }
        });

        commands.add(new Command("bigbank") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.bank.clear();
                player.bank.addAll(Config.LEET_BANK_ITEMS);
                System.arraycopy(Config.LEET_BANK_AMOUNTS, 0, player.bank.tabAmounts, 0, Config.LEET_BANK_AMOUNTS.length);
                player.bank.shift();
                player.bank.open();
            }
        });

        commands.add(new Command("item", "pickup") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    int id = parser.nextInt();
                    int amount = 1;
                    if (parser.hasNext()) {
                        amount = Integer.parseInt(parser.nextString().toLowerCase().replace("k", "000").replace("m", "000000").replace("b", "000000000"));
                    }

                    final ItemDefinition def = ItemDefinition.get(id);

                    if (def == null || def.getName() == null) {
                        return;
                    }

                    if (def.getName().equalsIgnoreCase("null")) {
                        return;
                    }

                    player.inventory.add(id, amount);
                }
            }
        });

        commands.add(new Command("find", "give") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    final String name = parser.nextLine();
                    ItemContainer container = new ItemContainer(400, ItemContainer.StackPolicy.ALWAYS);
                    int count = 0;
                    for (final ItemDefinition def : ItemDefinition.DEFINITIONS) {
                        if (def == null || def.getName() == null || def.isNoted())
                            continue;
                        if (def.getName().toLowerCase().trim().contains(name)) {
                            container.add(new Item(def.getId()));
                            count++;
                            if (count == 400)
                                break;
                        }
                    }
                    player.send(new SendString("Search: <col=FF5500>" + name, 37506));
                    player.send(new SendString(String.format("Found <col=FF5500>%s</col> item%s", count, count != 1 ? "s" : ""), 37507));
                    player.send(new SendScrollbar(37520, count / 8 * 52 + ((count % 8) == 0 ? 0 : 52)));
                    player.send(new SendItemOnInterface(37521, container.getItems()));
                    player.interfaceManager.open(37500);
                    player.send(new SendMessage(String.format("Found %s item%s containing the key '%s'.", count, count != 1 ? "s" : "", name)));
                }
            }
        });

        commands.add(new Command("pos", "mypos", "coords") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.send(new SendMessage("Your location is: " + player.getPosition() + "."));
                System.out.println("Your location is: " + player.getPosition() + ".");
            }
        });

        commands.add(new Command("perkpoints") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (!parser.hasNext()) {
                    player.message("Usage: ::perkpoints <amount>");
                    return;
                }
                int amount = parser.nextInt();
                player.perk.combatPoints += amount;
                player.perk.combatPointsEarned += amount;
                player.message("<col=ff9900>[Perks]</col> Added <col=ffffff>" + amount + "</col> combat perk points. Total: <col=ffffff>" + player.perk.combatPoints + "</col>.");
            }
        });

        commands.add(new Command("perkfullreset") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.perk.combatPoints = 0;
                player.perk.combatPointsEarned = 0;
                player.perk.combatXpAccumulator = 0.0;
                player.perk.activeCombatPerk = -1;
                java.util.Arrays.fill(player.perk.combatPerkLevels, 0);
                player.message("<col=ff9900>[Perks]</col> Full perk reset — all levels, points and XP cleared.");
            }
        });

        commands.add(new Command("perkreset") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.perk.combatPoints = 0;
                player.perk.combatPointsEarned = 0;
                player.perk.combatXpAccumulator = 0.0;
                player.message("<col=ff9900>[Perks]</col> Combat perk progress has been reset.");
            }
        });

        commands.add(new Command("perkunlock") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (!parser.hasNext()) {
                    player.message("Usage: ::perkunlock <perk_name>  (e.g. swift_strikes)");
                    return;
                }
                String name = parser.nextString().toUpperCase();
                try {
                    CombatPerk perk = CombatPerk.valueOf(name);
                    player.perk.combatPerkLevels[perk.ordinal()] = perk.maxLevel;
                    player.message("<col=ff9900>[Perks]</col> <col=ffffff>" + perk.name + "</col> unlocked to max level " + perk.maxLevel + ".");
                } catch (IllegalArgumentException e) {
                    player.message("Unknown perk: " + name + ". Available: SWIFT_STRIKES");
                }
            }
        });

        commands.add(new Command("perkactivate") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (!parser.hasNext()) {
                    player.message("Usage: ::perkactivate <perk_name>  (e.g. swift_strikes)");
                    return;
                }
                String name = parser.nextString().toUpperCase();
                try {
                    CombatPerk perk = CombatPerk.valueOf(name);
                    player.perk.setActive(perk);
                } catch (IllegalArgumentException e) {
                    player.message("Unknown perk: " + name + ". Available: SWIFT_STRIKES");
                }
            }
        });

        commands.add(new Command("perkinfo") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.message("<col=ff9900>[Perks]</col> Combat points: <col=ffffff>" + player.perk.combatPoints + "</col> | Earned: <col=ffffff>" + player.perk.combatPointsEarned + "</col>");
                for (CombatPerk perk : CombatPerk.values()) {
                    int level = player.perk.combatPerkLevels[perk.ordinal()];
                    boolean active = player.perk.activeCombatPerk == perk.ordinal();
                    player.message("  <col=ffffff>" + perk.name + "</col> - Level: " + level + "/" + perk.maxLevel + (active ? " <col=00ff00>[ACTIVE]</col>" : ""));
                }
            }
        });

        commands.add(new Command("perks") {
            @Override
            public void execute(Player player, CommandParser parser) {
                plugin.click.button.PerkButtonPlugin.open(player);
            }
        });

        commands.add(new Command("skillingperks") {
            @Override
            public void execute(Player player, CommandParser parser) {
                plugin.click.button.SkillingButtonPlugin.open(player);
            }
        });


        commands.add(new Command("resetskills") {
            @Override
            public void execute(Player player, CommandParser parser) {
                int[] skills = {
                        Skill.ATTACK, Skill.DEFENCE, Skill.STRENGTH, Skill.HITPOINTS,
                        Skill.RANGED, Skill.PRAYER, Skill.MAGIC, Skill.COOKING,
                        Skill.WOODCUTTING, Skill.FLETCHING, Skill.FISHING, Skill.FIREMAKING,
                        Skill.CRAFTING, Skill.SMITHING, Skill.MINING, Skill.HERBLORE,
                        Skill.AGILITY, Skill.THIEVING, Skill.SLAYER, Skill.FARMING,
                        Skill.RUNECRAFTING, Skill.HUNTER, Skill.CONSTRUCTION
                };
                for (int skill : skills) {
                    if (skill == Skill.HITPOINTS) {
                        player.skills.get(skill).setLevel(10);
                        player.skills.get(skill).setExperience(1154);
                    } else {
                        player.skills.get(skill).setLevel(1);
                        player.skills.get(skill).setExperience(0);
                    }
                }
                player.skills.refresh();
                player.send(new SendMessage("Your skills have been reset to level 1 (Hitpoints reset to level 10)."));
            }
        });

        commands.add(new Command("god") {
            @Override
            public void execute(Player player, CommandParser parser) {
                int[] combatSkills = {
                        Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE,
                        Skill.RANGED, Skill.MAGIC, Skill.HITPOINTS, Skill.PRAYER
                };
                for (int skill : combatSkills) {
                    player.skills.get(skill).setLevel(255);
                }
                player.skills.refresh();
                CombatSpecial.restore(player, 100);
                player.attributes.set("GOD_MODE", true);
                // Auto restore spec every tick
                World.schedule(new com.osroyale.game.task.Task(1) {
                    @Override
                    protected void execute() {
                        if (!player.isRegistered() || !Boolean.TRUE.equals(player.attributes.get("GOD_MODE", Boolean.class))) {
                            cancel();
                            return;
                        }
                        CombatSpecial.restore(player, 100);
                    }
                });
                player.send(new SendMessage("<col=ff0000>God mode activated! Skills set to 255 and spec auto-restores. Use ::ungod to disable."));
            }
        });

        commands.add(new Command("ungod") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.attributes.set("GOD_MODE", false);
                int[] combatSkills = {
                        Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE,
                        Skill.RANGED, Skill.MAGIC, Skill.HITPOINTS, Skill.PRAYER
                };
                for (int skill : combatSkills) {
                    player.skills.get(skill).setLevel(99);
                }
                player.skills.get(Skill.HITPOINTS).setLevel(99);
                player.skills.refresh();
                player.send(new SendMessage("<col=00ff00>God mode deactivated! Skills reset to 99."));
            }
        });

    }


    @Override
    public boolean canAccess(Player player) {
        return PlayerRight.isAdministrator(player);
    }
}
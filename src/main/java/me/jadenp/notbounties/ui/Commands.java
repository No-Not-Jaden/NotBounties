package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.bountyEvents.BountyEditEvent;
import me.jadenp.notbounties.bountyEvents.BountyRemoveEvent;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.ui.map.BountyMap;
import me.jadenp.notbounties.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.ui.gui.GUI.openGUI;
import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static me.jadenp.notbounties.utils.NumberFormatting.*;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.LanguageOptions.*;

public class Commands implements CommandExecutor, TabCompleter {

    public Commands() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("notbounties")) {
            Player parser = getParser(sender);
            if (args.length > 0) {
                final String whitelistText = "whitelist";
                if (args[0].equalsIgnoreCase("help") && sender.hasPermission("notbounties.basic")) {
                    LanguageOptions.sendHelpMessage(sender);
                } else if (args[0].equalsIgnoreCase("update-notification")) {
                    if (!sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                        return true;
                    }
                    if (args.length == 1) {
                        updateNotification = !updateNotification;
                    } else {
                        updateNotification = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("enable");
                    }
                    NotBounties.getInstance().reloadConfig();
                    NotBounties.getInstance().getConfig().set("update-notification", updateNotification);
                    NotBounties.getInstance().saveConfig();
                    if (updateNotification) {
                        sender.sendMessage(parse(prefix + ChatColor.YELLOW + "The update notification is now " + ChatColor.GREEN + "enabled" + ChatColor.YELLOW + ".", parser));
                    } else {
                        sender.sendMessage(parse(prefix + ChatColor.YELLOW + "The update notification is now " + ChatColor.RED + "disabled" + ChatColor.YELLOW + ".", parser));
                    }
                } else if (args[0].equalsIgnoreCase("debug")) {
                    if (!sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(parse(prefix + noPermission, parser));
                        return true;
                    }
                    try {
                        NotBounties.getInstance().sendDebug(sender);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("board") && sender.hasPermission("notbounties.admin")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Only players can use this command!");
                        return true;
                    }
                    int rank = 0;
                    if (args.length > 1) {
                        if (args[1].equalsIgnoreCase("clear")) {
                            sender.sendMessage(parse(prefix + ChatColor.DARK_RED + "Removed " + bountyBoards.size() + " bounty boards.", parser));
                            removeBountyBoard();

                            return true;
                        }
                        if (args[1].equalsIgnoreCase("remove")) {
                            sender.sendMessage(parse(prefix + ChatColor.RED + "Right click the bounty board to remove.", parser));
                            NotBounties.boardSetup.put(parser.getUniqueId(), -1);
                            return true;
                        }
                        try {
                            rank = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(parse(prefix + unknownNumber, parser));
                        }
                    } else {
                        for (BountyBoard board : bountyBoards) {
                            if (board.getRank() > rank)
                                rank = board.getRank();
                        }
                        rank += 1;
                    }
                    if (rank < 1)
                        rank = 1;
                    sender.sendMessage(parse(prefix + ChatColor.DARK_AQUA + ChatColor.BOLD + "<Rank " + rank + "> " + ChatColor.AQUA + "Punch a block to place the bounty board on.", parser));
                    NotBounties.boardSetup.put(parser.getUniqueId(), rank);
                    return true;
                } else if (args[0].equalsIgnoreCase("tutorial") && sender.hasPermission("notbounties.basic")) {
                    Tutorial.onCommand(sender, args);
                    return true;
                } else if (args[0].equalsIgnoreCase("whitelist") && bountyWhitelistEnabled) {
                    if (sender instanceof Player) {
                        if (!sender.hasPermission("notbounties.whitelist")) {
                            sender.sendMessage(parse(prefix + noPermission, parser));
                            return true;
                        }
                        if (args.length == 1) {
                            openGUI(parser, "set-whitelist", 1);
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("offline")) {
                            openGUI(parser, "set-whitelist", 1, "offline");
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("reset")) {
                            getPlayerWhitelist(parser.getUniqueId()).setList(new ArrayList<>());
                            sender.sendMessage(parse(prefix + whitelistReset, parser));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("view")) {
                            List<UUID> whitelist = getPlayerWhitelist((parser).getUniqueId()).getList();
                            StringBuilder names = new StringBuilder(" ");
                            for (UUID uuid : whitelist) {
                                names.append(NotBounties.getPlayerName(uuid)).append(", ");
                            }
                            if (names.length() > 1) {
                                names.replace(names.length() - 2, names.length() - 1, "");
                            } else {
                                names.append("<none>");
                            }
                            sender.sendMessage(parse(prefix + whitelistedPlayers + names, parser));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("toggle") && enableBlacklist) {
                            if (getPlayerWhitelist(((Player) sender).getUniqueId()).toggleBlacklist()) {
                                sender.sendMessage(parse(prefix + blacklistToggle, parser));
                            } else {
                                sender.sendMessage(parse(prefix + whitelistToggle, parser));
                            }
                            return true;
                        }
                        if (args.length > 2) {
                            List<UUID> whitelist = new ArrayList<>();
                            List<UUID> previousWhitelist = NotBounties.playerWhitelist.containsKey((parser).getUniqueId()) ? playerWhitelist.get((parser).getUniqueId()).getList() : new ArrayList<>();
                            for (int i = 2; i < Math.min(args.length, 12); i++) {
                                OfflinePlayer player = getPlayer(args[i]);
                                if (player == null) {
                                    // unknown player
                                    sender.sendMessage(parse(prefix + unknownPlayer, args[i], parser));
                                    return true;
                                }
                                if (!whitelist.contains(player.getUniqueId()))
                                    whitelist.add(player.getUniqueId());
                            }

                            if (args[1].equalsIgnoreCase("add")) {
                                whitelist.stream().filter(uuid -> !previousWhitelist.contains(uuid)).forEach(previousWhitelist::add);
                                while (previousWhitelist.size() > 10)
                                    previousWhitelist.remove(10);
                                getPlayerWhitelist(parser.getUniqueId()).setList(previousWhitelist);
                                sender.sendMessage(parse(prefix + whitelistChange, parser));
                                if (previousWhitelist.size() == 10)
                                    sender.sendMessage(parse(prefix + whitelistMax, parser));
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("remove")) {
                                if (!previousWhitelist.removeIf(whitelist::contains)) {
                                    // nobody removed
                                    StringBuilder builder = new StringBuilder(args[2]);
                                    for (int i = 3; i < args.length; i++) {
                                        builder.append(", ").append(args[i]);
                                    }
                                    sender.sendMessage(parse(prefix + unknownPlayer, builder.toString(), parser));
                                    return true;
                                }
                                getPlayerWhitelist(parser.getUniqueId()).setList(previousWhitelist);
                                sender.sendMessage(parse(prefix + whitelistChange, parser));
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("set")) {
                                getPlayerWhitelist(parser.getUniqueId()).setList(whitelist);
                                sender.sendMessage(parse(prefix + whitelistChange, parser));
                                return true;
                            }
                        }
                        // usage
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " (add/remove/set) (" + whitelistText + "ed players)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Change the players that can claim the bounties you set.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set whitelist GUI.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " reset" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Resets your whitelisted players.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " view" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Displays your whitelisted players in chat.");
                        return true;
                    } else {
                        sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command.");
                    }
                } else if (args[0].equalsIgnoreCase("currency")) {
                    if (!sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(parse(prefix + noPermission, parser));
                        return true;
                    }
                    CurrencySetup.onCommand(sender, args);
                } else if (args[0].equalsIgnoreCase("set")) {
                    if (sender.hasPermission("notbounties.set")) {
                        if (sender instanceof Player) {
                            if (args.length > 1)
                                openGUI(parser, "set-bounty", 1, args[1]);
                            else
                                openGUI(parser, "set-bounty", 1);
                        } else {
                            sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command.");
                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    }
                } else if (args[0].equalsIgnoreCase("bdc") && sender.hasPermission("notbounties.basic")) {
                    if (sender instanceof Player) {
                        if (NotBounties.disableBroadcast.contains((parser).getUniqueId())) {
                            // enable
                            NotBounties.disableBroadcast.remove((parser).getUniqueId());
                            sender.sendMessage(parse(prefix + enableBroadcast, parser));
                        } else {
                            // disable
                            NotBounties.disableBroadcast.add((parser).getUniqueId());
                            sender.sendMessage(parse(prefix + LanguageOptions.disableBroadcast, parser));
                        }
                    } else {
                        sender.sendMessage("You can't disable bounty broadcast! (yet?) If you really want to disable, send a message in the discord!");
                    }
                } else if (args[0].equalsIgnoreCase("stat")) {
                    if (!sender.hasPermission("notbounties.view")) {
                        sender.sendMessage(parse(prefix + noPermission, parser));
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Only players can use this command!");
                        return true;
                    }
                    if (args.length == 1) {
                        Leaderboard.ALL.displayStats(parser, false);
                        return true;
                    }
                    if (args.length != 2) {
                        // usage
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        return true;
                    }
                    try {
                        Leaderboard.valueOf(args[1].toUpperCase()).displayStats(parser, false);
                    } catch (IllegalArgumentException e) {
                        // more usage
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        return true;
                    }


                } else if (args[0].equalsIgnoreCase("top")) {
                    if (sender.hasPermission("notbounties.view")) {
                        Leaderboard leaderboard;
                        if (args.length > 1) {
                            try {
                                leaderboard = Leaderboard.valueOf(args[1].toUpperCase());
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty top (all/kills/claimed/deaths/set/immunity) <list>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists the top 10 players with the respective stats.");
                                return true;
                            }
                        } else {
                            leaderboard = Leaderboard.ALL;
                        }
                        if (args.length > 2 && args[2].equalsIgnoreCase("list")) {
                            leaderboard.displayTopStat(sender, 10);
                        } else {
                            openGUI(parser, "leaderboard", 1, leaderboard);
                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("notbounties.admin")) {
                        try {
                            NotBounties.getInstance().loadConfig();
                        } catch (IOException e) {
                            Bukkit.getLogger().warning(e.toString());
                        }
                        reopenBountiesGUI();
                        sender.sendMessage(parse(prefix, parser) + ChatColor.GREEN + "Reloaded NotBounties version " + NotBounties.getInstance().getDescription().getVersion());
                    } else {
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                    }
                } else if (args[0].equalsIgnoreCase("immunity")) {
                    if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
                        if (args.length == 2) {
                            // reg command
                            if (sender.hasPermission("notbounties.removeimmunity") || sender.hasPermission("notbounties.admin")) {
                                if (sender instanceof Player) {
                                    // remove immunity
                                    double immunitySpent = SQL.isConnected() ? data.getImmunity((parser).getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat((parser).getUniqueId());
                                    if (immunitySpent > 0) {
                                        if (SQL.isConnected())
                                            data.setImmunity((parser).getUniqueId().toString(), 0);
                                        else
                                            BountyManager.immunitySpent.put((parser).getUniqueId(), 0.0);
                                        if (immunityType == 3)
                                            immunityTimeTracker.remove((parser).getUniqueId());
                                        sender.sendMessage(parse(prefix + removedImmunity, sender.getName(), (parser)));
                                    } else {
                                        // doesn't have immunity
                                        sender.sendMessage(parse(prefix + noImmunity, sender.getName(), (parser)));
                                    }
                                } else {
                                    Bukkit.getLogger().info("You don't have immunity!");
                                }
                            } else {
                                // usage
                                if (sender instanceof Player)
                                    if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                        if (immunityType == 1) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                        } else if (immunityType == 2) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        } else if (immunityType == 3) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + ConfigOptions.timeImmunity + " seconds.");
                                        }
                                    } else {
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    }
                            }
                        } else if (args.length == 3) {
                            //admin command
                            if (sender.hasPermission("notbounties.admin")) {
                                OfflinePlayer p = getPlayer(args[2]);
                                if (p == null) {
                                    sender.sendMessage(parse(prefix + unknownPlayer, args[0], parser));
                                    return true;
                                }
                                double immunitySpent = SQL.isConnected() ? data.getImmunity(p.getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(p.getUniqueId());
                                if (immunitySpent > 0) {
                                    if (SQL.isConnected())
                                        data.setImmunity(p.getUniqueId().toString(), 0);
                                    else
                                        BountyManager.immunitySpent.put(p.getUniqueId(), 0.0);
                                    if (immunityType == 3)
                                        immunityTimeTracker.remove(p.getUniqueId());
                                    sender.sendMessage(parse(prefix + removedOtherImmunity, p.getName(), p));
                                } else {
                                    // doesn't have immunity
                                    sender.sendMessage(parse(prefix + noImmunityOther, p.getName(), p));
                                }
                            } else {
                                if (sender.hasPermission("notbounties.removeimmunity")) {
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                                } else if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    if (immunityType == 1) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                    } else if (immunityType == 2) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                    } else if (immunityType == 3) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + ConfigOptions.timeImmunity + " seconds.");
                                    }
                                } else {
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                }
                            }
                        } else {
                            // usage
                            if (sender.hasPermission("notbounties.admin")) {
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from a player.");
                            } else if (sender.hasPermission("notbounties.removeimmunity")) {
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                            } else if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                                if (immunityType == 1) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                } else if (immunityType == 2) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                } else if (immunityType == 3) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + ConfigOptions.timeImmunity + " seconds.");
                                }
                            } else {
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            }
                        }
                    } else {
                        if (sender instanceof Player) {
                            if (immunityType > 0) {
                                if (sender.hasPermission("notbounties.buyimmunity")) {
                                    if (immunityType == 1) {
                                        double immunitySpent = SQL.isConnected() ? data.getImmunity((parser).getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat((parser).getUniqueId());
                                        if (immunitySpent < permanentCost && !sender.hasPermission("notbounties.immune")) {
                                            if ((repeatBuyCommand2.containsKey((parser).getUniqueId().toString()) && System.currentTimeMillis() - repeatBuyCommand2.get((parser).getUniqueId().toString()) < 30000) || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"))) {
                                                // try to find bounty and buy it
                                                repeatBuyCommand2.remove((parser).getUniqueId().toString());
                                                if (checkBalance(parser, permanentCost)) {
                                                    NumberFormatting.doRemoveCommands(parser, permanentCost, new ArrayList<>());
                                                    // successfully bought perm immunity
                                                    if (SQL.isConnected()) {
                                                        data.addData((parser).getUniqueId().toString(), 0, 0, 0, 0, permanentCost, 0);
                                                        sender.sendMessage(parse(prefix + buyPermanentImmunity, data.getImmunity((parser).getUniqueId().toString()), parser));
                                                    } else {
                                                        BountyManager.immunitySpent.put((parser).getUniqueId(), Leaderboard.IMMUNITY.getStat((parser).getUniqueId()) + permanentCost);
                                                        sender.sendMessage(parse(prefix + buyPermanentImmunity, Leaderboard.IMMUNITY.getStat((parser).getUniqueId()), parser));
                                                    }
                                                } else {
                                                    sender.sendMessage(parse(prefix + broke, permanentCost, parser));
                                                }

                                            } else {
                                                // ask to repeat
                                                repeatBuyCommand2.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(parse(prefix + repeatCommandImmunity, permanentCost, parser));
                                            }
                                        } else {
                                            // already bought immunity
                                            sender.sendMessage(parse(prefix + alreadyBoughtPerm, parser));
                                        }
                                    } else {
                                        if (args.length > 1) {
                                            double amount;
                                            try {
                                                amount = tryParse(args[1]);
                                            } catch (NumberFormatException ignored) {
                                                sender.sendMessage(parse(prefix + unknownNumber, parser));
                                                return true;
                                            }
                                            if ((repeatBuyCommand2.containsKey((parser).getUniqueId().toString()) && System.currentTimeMillis() - repeatBuyCommand2.get((parser).getUniqueId().toString()) < 30000) || args.length > 2 && args[2].equalsIgnoreCase("--confirm")) {
                                                // try to find bounty and buy it
                                                repeatBuyCommand2.remove((parser).getUniqueId().toString());
                                                if (checkBalance(parser, amount)) {
                                                    NumberFormatting.doRemoveCommands(parser, amount, new ArrayList<>());
                                                    // successfully bought scaling immunity - amount x scalingRatio
                                                    double immunity;
                                                    if (SQL.isConnected()) {
                                                        data.addData((parser).getUniqueId().toString(), 0, 0, 0, 0, amount, 0);
                                                        immunity = data.getImmunity((parser).getUniqueId().toString());
                                                    } else {
                                                        immunitySpent.put((parser).getUniqueId(), Leaderboard.IMMUNITY.getStat((parser).getUniqueId()) + (amount));
                                                        immunity = Leaderboard.IMMUNITY.getStat((parser).getUniqueId());
                                                    }
                                                    if (immunityType == 2) {
                                                        sender.sendMessage(parse(prefix + buyScalingImmunity, immunity * scalingRatio, parser));
                                                    } else {
                                                        sender.sendMessage(parse(prefix + buyTimeImmunity.replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime((long) (immunity * ConfigOptions.timeImmunity * 1000L)))), immunity * ConfigOptions.timeImmunity, parser));
                                                        immunityTimeTracker.put((parser).getUniqueId(), (long) (System.currentTimeMillis() + immunity * ConfigOptions.timeImmunity * 1000L));
                                                    }
                                                } else {
                                                    // broke
                                                    sender.sendMessage(parse(prefix + broke, amount, parser));
                                                }
                                            } else {
                                                // ask to repeat
                                                repeatBuyCommand2.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(parse(prefix + repeatCommandImmunity, amount, parser));
                                            }
                                        } else {
                                            // usage
                                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        }
                                    }
                                } else {
                                    // no permission
                                    sender.sendMessage(parse(prefix + noPermission, parser));
                                }
                            } else {
                                // unknown command
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            }
                        } else {
                            sender.sendMessage("Only players can buy immunity!");
                        }
                    }
                } else if (args[0].equalsIgnoreCase("buy")) {
                    if (sender instanceof Player) {
                        if (buyBack) {
                            if (sender.hasPermission("notbounties.buyown")) {
                                if (hasBounty(parser)) {
                                    Bounty bounty = getBounty(parser);
                                    assert bounty != null;
                                    if ((repeatBuyCommand.containsKey((parser).getUniqueId().toString()) && System.currentTimeMillis() - repeatBuyCommand.get((parser).getUniqueId().toString()) < 30000) || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"))) {
                                        // try to find bounty and buy it
                                        repeatBuyCommand.remove((parser).getUniqueId().toString());
                                        if (checkBalance(parser, (bounty.getTotalBounty() * buyBackInterest))) {
                                            Bounty bought = new Bounty(bounty);
                                            BountyRemoveEvent event = new BountyRemoveEvent(sender, true, bought);
                                            Bukkit.getPluginManager().callEvent(event);
                                            if (event.isCancelled())
                                                return true;
                                            NumberFormatting.doRemoveCommands(parser, (bounty.getTotalBounty() * buyBackInterest), new ArrayList<>());
                                            if (SQL.isConnected()) {
                                                data.removeBounty(bounty.getUUID());
                                            } else {
                                                bountyList.remove(bounty);
                                            }
                                            reopenBountiesGUI();
                                            sender.sendMessage(parse(prefix + successRemoveBounty, sender.getName(), parser));
                                        } else {
                                            sender.sendMessage(parse(prefix + broke, (bounty.getTotalBounty() * buyBackInterest), parser));
                                        }


                                    } else {
                                        // ask to repeat
                                        repeatBuyCommand.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                        sender.sendMessage(parse(prefix + repeatCommandBounty, (bounty.getTotalBounty() * buyBackInterest), parser));
                                    }
                                } else {
                                    sender.sendMessage(parse(prefix + noBounty, sender.getName(), parser));
                                }
                            } else {
                                sender.sendMessage(parse(prefix + noPermission, parser));
                            }
                        } else {
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                        }
                    } else {
                        sender.sendMessage("You don't have a bounty!");
                    }
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (sender.hasPermission("notbounties.view")) {
                        if (args.length > 1) {
                            OfflinePlayer p = getPlayer(args[1]);
                            if (p == null) {
                                sender.sendMessage(parse(prefix + unknownPlayer, args[1], parser));
                                return true;
                            }
                            if (!hasBounty(p)) {
                                sender.sendMessage(parse(prefix + noBounty, p.getName(), parser));
                                return true;
                            }
                            Bounty bounty = getBounty(p);
                            assert bounty != null;
                            double bountyAmount = showWhitelistedBounties || sender.hasPermission("notbounties.admin") || !(sender instanceof Player) ? bounty.getTotalBounty() : bounty.getTotalBounty(parser);
                            if (bountyAmount == 0) {
                                sender.sendMessage(parse(prefix + noBounty, p.getName(), parser));
                                return true;
                            }

                            sender.sendMessage(parse(prefix + checkBounty, p.getName(), bounty.getTotalBounty(parser), bounty.getLatestSetter(), parser));
                            for (Setter setters : bounty.getSetters()) {
                                if (showWhitelistedBounties || sender.hasPermission("notbounties.admin") || !(sender instanceof Player) || setters.canClaim(parser)) {
                                    sender.sendMessage(parse(prefix + listSetter, setters.getName(), setters.getAmount(), setters.getTimeCreated(), parser));
                                    if (!setters.canClaim(parser))
                                        notWhitelistedLore.stream().filter(s -> !s.isEmpty()).map(s -> parse(s, parser)).forEach(sender::sendMessage);
                                }
                            }
                            return true;

                        } else {
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sender.sendMessage(ChatColor.BLUE + "/bounty check (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Checks a bounty.");
                        }
                    } else {
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    if (sender.hasPermission("notbounties.view")) {
                        int page = 1;
                        if (args.length > 1) {
                            try {
                                page = Integer.parseInt(args[1]);
                            } catch (NumberFormatException ignored) {

                            }
                        }

                        listBounties(sender, page - 1);
                    } else {
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    }
                } else if (args[0].equalsIgnoreCase("remove")) {
                    if (sender.hasPermission("notbounties.admin")) {
                        if (args.length > 1) {
                            Bounty toRemove = null;
                            List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                            for (Bounty bounty : bountyList) {
                                if (bounty.getName().equalsIgnoreCase(args[1])) {
                                    toRemove = bounty;
                                    break;
                                }
                            }
                            if (toRemove != null) {
                                if (args.length == 2) {
                                    Bounty bounty = new Bounty(toRemove);
                                    BountyRemoveEvent event = new BountyRemoveEvent(sender, false, bounty);
                                    Bukkit.getPluginManager().callEvent(event);
                                    if (event.isCancelled())
                                        return true;
                                    if (SQL.isConnected()) {
                                        data.removeBounty(toRemove.getUUID());
                                    } else {
                                        bountyList.remove(toRemove);
                                    }
                                    refundBounty(toRemove);
                                    // successfully removed
                                    NotBounties.removeWantedTag(toRemove.getUUID());
                                    Player player = Bukkit.getPlayer(toRemove.getUUID());
                                    if (player != null) {
                                        sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), player));
                                    } else {
                                        sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                    }
                                } else if (args.length == 4) {
                                    if (args[2].equalsIgnoreCase("from")) {
                                        Setter actualRemove = null;
                                        for (Setter setter : toRemove.getSetters()) {
                                            if (setter.getName().equalsIgnoreCase(args[3])) {
                                                actualRemove = setter;
                                                break;
                                            }
                                        }
                                        Bounty bounty = new Bounty(toRemove.getUUID(), Collections.singletonList(actualRemove), toRemove.getName());
                                        BountyRemoveEvent event = new BountyRemoveEvent(sender, false, bounty);
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (event.isCancelled())
                                            return true;
                                        if (actualRemove != null) {
                                            toRemove.removeBounty(actualRemove.getUuid());
                                            if (toRemove.getSetters().isEmpty()) {
                                                if (SQL.isConnected()) {
                                                    data.removeBounty(toRemove.getUUID());
                                                } else {
                                                    bountyList.remove(toRemove);
                                                }
                                            } else if (SQL.isConnected()) {
                                                data.removeSetter(toRemove.getUUID(), actualRemove.getUuid());
                                            }
                                            refundSetter(actualRemove);
                                            // reopen gui for everyone
                                            reopenBountiesGUI();
                                            // successfully removed
                                            if (toRemove.getTotalBounty() < ConfigOptions.minBounty)
                                                NotBounties.removeWantedTag(toRemove.getUUID());

                                            Player player = Bukkit.getPlayer(toRemove.getUUID());
                                            if (player != null) {
                                                sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), player));
                                            } else {
                                                sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                            }

                                        } else {
                                            // couldnt find setter

                                            Player player = Bukkit.getPlayer(toRemove.getUUID());
                                            if (player != null) { // player then receiver
                                                sender.sendMessage(parse(prefix + noSetter, args[3], toRemove.getName(), player));
                                            } else {
                                                sender.sendMessage(parse(prefix + noSetter, args[3], toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                            }
                                        }

                                    } else {
                                        // usage
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                }
                            } else {
                                // could not find bounty
                                sender.sendMessage(parse(prefix + noBounty, args[1], parser));

                            }
                        } else {
                            // usage
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");

                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    }
                } else if (args[0].equalsIgnoreCase("edit")) {
                    if (sender.hasPermission("notbounties.admin")) {
                        if (args.length > 2) {
                            Bounty toEdit = null;
                            List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                            for (Bounty bounty : bountyList) {
                                if (bounty.getName().equalsIgnoreCase(args[1])) {
                                    toEdit = bounty;
                                    break;
                                }
                            }
                            if (toEdit != null) {
                                if (args.length == 3) {
                                    double amount;
                                    try {
                                        amount = tryParse(args[2]);
                                    } catch (NumberFormatException ignored) {
                                        // unknown number - 2?
                                        sender.sendMessage(parse(prefix + unknownNumber, parser));

                                        return true;
                                    }
                                    Bounty before = new Bounty(toEdit);
                                    Bounty after = new Bounty(toEdit);
                                    after.addBounty(amount - toEdit.getTotalBounty(), new Whitelist(new ArrayList<>(), false));
                                    BountyEditEvent event = new BountyEditEvent(sender, before, after);
                                    Bukkit.getPluginManager().callEvent(event);
                                    if (event.isCancelled())
                                        return true;
                                    if (SQL.isConnected()) {
                                        data.editBounty(toEdit.getUUID(), new UUID(0, 0), amount - toEdit.getTotalBounty());
                                    } else {
                                        toEdit.addBounty(amount - toEdit.getTotalBounty(), new Whitelist(new ArrayList<>(), false));
                                    }
                                    // successfully edited bounty
                                    sender.sendMessage(parse(prefix + successEditBounty, toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));


                                } else if (args.length == 5) {
                                    if (args[2].equalsIgnoreCase("from")) {
                                        Setter actualEdit = null;
                                        for (Setter setter : toEdit.getSetters()) {
                                            if (setter.getName().equalsIgnoreCase(args[3])) {
                                                actualEdit = setter;
                                                break;
                                            }
                                        }
                                        if (actualEdit != null) {
                                            double amount;
                                            try {
                                                amount = tryParse(args[4]);
                                            } catch (NumberFormatException ignored) {
                                                // unknown number - 2?
                                                sender.sendMessage(parse(prefix + unknownNumber, parser));

                                                return true;
                                            }
                                            Bounty before = new Bounty(toEdit);
                                            Bounty after = new Bounty(toEdit);
                                            after.editBounty(actualEdit.getUuid(), amount);
                                            BountyEditEvent event = new BountyEditEvent(sender, before, after);
                                            Bukkit.getPluginManager().callEvent(event);
                                            if (event.isCancelled())
                                                return true;
                                            if (SQL.isConnected()) {
                                                data.editBounty(toEdit.getUUID(), actualEdit.getUuid(), amount - toEdit.getTotalBounty());
                                            } else {
                                                toEdit.editBounty(actualEdit.getUuid(), amount);
                                            }

                                            // successfully edited bounty
                                            Player player = Bukkit.getPlayer(toEdit.getUUID());
                                            if (player != null) {
                                                sender.sendMessage(parse(prefix + successEditBounty, toEdit.getName(), player));
                                            } else {
                                                sender.sendMessage(parse(prefix + successEditBounty, toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                            }

                                        } else {
                                            // couldnt find setter

                                            sender.sendMessage(parse(prefix + noSetter, args[3], toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));

                                        }

                                    } else {
                                        // usage
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                }
                            } else {
                                // couldn't find bounty
                                sender.sendMessage(parse(prefix + noBounty, args[1], parser));

                            }
                        } else {
                            // usage
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                            sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");

                        }
                    } else {
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    }
                } else if (args[0].equalsIgnoreCase("poster")) {
                    if (!(giveOwnMap || sender.hasPermission("notbounties.admin"))) {
                        // no permission
                        sender.sendMessage(parse(prefix + noPermission, parser));
                        return true;
                    }
                    if (args.length == 1) {
                        // usage
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sender.sendMessage(ChatColor.BLUE + "/bounty poster (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a poster of a player's bounty.");
                        if (sender.hasPermission("notbounties.admin"))
                            sender.sendMessage(ChatColor.BLUE + "/bounty poster (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a poster of a player's bounty.");

                        return true;
                    }
                    OfflinePlayer player = getPlayer(args[1]);
                    if (!hasBounty(player)) {
                        // couldn't find bounty
                        sender.sendMessage(parse(prefix + noBounty, args[1], parser));
                        return true;

                    }
                    Bounty bounty = getBounty(player);
                    assert bounty != null;
                    Player receiver;
                    if (args.length > 2) {
                        receiver = Bukkit.getPlayer(args[2]);
                        if (receiver == null) {
                            // can't find player
                            sender.sendMessage(parse(prefix + unknownPlayer, args[2], parser));
                            return true;
                        }
                        sender.sendMessage(parse(prefix + mapGive, args[2], bounty.getName(), player));
                    } else if (sender instanceof Player) {
                        receiver = parser;
                    } else {
                        sender.sendMessage("You cant give yourself this item!");
                        return true;
                    }
                    BountyMap.giveMap(receiver, bounty);
                    receiver.sendMessage(parse(prefix + mapReceive, bounty.getName(), player));
                } else if (args[0].equalsIgnoreCase("tracker")) {
                    // give a tracker that points toward a certain player with a bounty
                    if (tracker)
                        if (sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker"))) {
                            if (args.length > 1) {
                                Bounty tracking = null;
                                List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                                for (Bounty bounty : bountyList) {
                                    if (bounty.getName().equalsIgnoreCase(args[1])) {
                                        tracking = bounty;
                                        break;
                                    }
                                }
                                if (tracking != null) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(tracking.getUUID());
                                    ItemStack compass = new ItemStack(Material.COMPASS, 1);
                                    ItemMeta meta = compass.getItemMeta();
                                    assert meta != null;
                                    meta.setDisplayName(parse(bountyTrackerName, player.getName(), player));

                                    ArrayList<String> lore = new ArrayList<>();
                                    for (String str : trackerLore) {
                                        lore.add(parse(str, player.getName(), player));
                                    }
                                    int i = 0;
                                    if (trackedBounties.containsValue(tracking.getUUID().toString())) {
                                        // get already used number
                                        for (Map.Entry<Integer, String> entry : trackedBounties.entrySet()) {
                                            if (entry.getValue().equals(tracking.getUUID().toString())) {
                                                i = entry.getKey();
                                                break;
                                            }
                                        }
                                    } else {
                                        // get new number
                                        while (trackedBounties.containsKey(i)) {
                                            i++;
                                        }
                                        trackedBounties.put(i, tracking.getUUID().toString());
                                    }

                                    lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + i);
                                    meta.setLore(lore);
                                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                    compass.setItemMeta(meta);
                                    compass.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                                    if (args.length > 2) {
                                        // /bounty tracker (player) (receiver)
                                        if (!sender.hasPermission("notbounties.admin")) {
                                            sender.sendMessage(parse(prefix + noPermission, parser));
                                            return true;
                                        }
                                        // find player to give to
                                        Player receiver = Bukkit.getPlayer(args[2]);
                                        if (receiver != null) {
                                            NumberFormatting.givePlayer(receiver, compass, 1);
                                            // you have been given & you have received
                                            sender.sendMessage(parse(prefix + trackerGive, receiver.getName(), player.getName(), player));

                                            receiver.sendMessage(parse(prefix + trackerReceive, player.getName(), player));
                                        } else {
                                            sender.sendMessage(parse(prefix + unknownPlayer, args[2], parser));

                                        }
                                    } else {
                                        if (sender instanceof Player) {
                                            NumberFormatting.givePlayer(parser, compass, 1);
                                            // you have been given
                                            sender.sendMessage(parse(prefix + trackerReceive, player.getName(), player));
                                        } else {
                                            sender.sendMessage("You are not a player!");
                                        }
                                    }
                                } else {
                                    // couldn't find bounty
                                    sender.sendMessage(parse(prefix + noBounty, args[1], parser));

                                }
                            } else {
                                // usage
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a compass that tracks a player with a bounty.");
                                sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a compass that tracks a player with a bounty.");

                            }
                        } else {
                            sender.sendMessage(parse(prefix + noPermission, parser));
                        }
                } else {
                    if (sender.hasPermission("notbounties.set")) {
                        OfflinePlayer player = getPlayer(args[0]);
                        if (player == null) {
                            // can't find player
                            if (args.length == 1) {
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty (player) (amount) <whitelisted players>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Adds a bounty to a player.");
                                sender.sendMessage(ChatColor.BLUE + "/bounty set <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set bounty GUI.");
                            } else {
                                sender.sendMessage(parse(prefix + unknownPlayer, args[0], parser));
                            }

                            return true;
                        }
                        if (args.length == 1) {
                            if (sender instanceof Player)
                                openGUI(parser, "select-price", ConfigOptions.minBounty, player.getUniqueId().toString());
                            return true;
                        }

                        // get whitelisted people
                        Whitelist whitelist = (sender instanceof Player) && sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled ? getPlayerWhitelist((parser).getUniqueId()) : new Whitelist(new ArrayList<>(), false);
                        if (args.length > 3 && sender.hasPermission("notbounties.whitelist")) {
                            List<UUID> newWhitelist = new ArrayList<>();
                            for (int i = 3; i < Math.min(args.length, 13); i++) {
                                if (args[i].equalsIgnoreCase("--confirm"))
                                    continue;
                                OfflinePlayer p = getPlayer(args[i]);
                                if (p == null) {
                                    // unknown player
                                    sender.sendMessage(parse(prefix + unknownPlayer, args[i], parser));
                                    return true;
                                }
                                newWhitelist.add(p.getUniqueId());
                            }
                            whitelist.setList(newWhitelist);
                        }

                        // check if max setters reached
                        if (ConfigOptions.maxSetters > -1) {
                            if (hasBounty(player)) {
                                Bounty bounty = getBounty(player);
                                assert bounty != null;
                                if (bounty.getSetters().size() >= ConfigOptions.maxSetters) {
                                    sender.sendMessage(parse(prefix + LanguageOptions.maxSetters, args[0], parser));
                                    return true;
                                }
                            }
                        }

                        // check if we can get the amount
                        double amount;
                        try {
                            amount = tryParse(args[1]);
                        } catch (NumberFormatException ignored) {
                            sender.sendMessage(parse(prefix + unknownNumber, parser));
                            return true;
                        }
                        if (amount < ConfigOptions.minBounty) {
                            sender.sendMessage(parse(prefix + LanguageOptions.minBounty, ConfigOptions.minBounty, parser));
                            return true;
                        }
                        double total = amount + amount * bountyTax + whitelist.getList().size() * bountyWhitelistCost;

                        // check if it is a player

                        if (sender instanceof Player) {
                            if (NotBounties.gracePeriod.containsKey(player.getUniqueId().toString())) {
                                long timeSinceDeath = System.currentTimeMillis() - NotBounties.gracePeriod.get(player.getUniqueId().toString());
                                if (timeSinceDeath < graceTime * 1000L) {
                                    // still in grace period
                                    long timeLeft = (graceTime * 1000L) - timeSinceDeath;

                                    String message = parse(prefix + LanguageOptions.gracePeriod.replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime(timeLeft))), player.getName(), player);
                                    sender.sendMessage(message);
                                    return true;
                                } else {
                                    NotBounties.gracePeriod.remove(player.getUniqueId().toString());
                                }
                            }
                            double immunitySpent = SQL.isConnected() ? data.getImmunity(player.getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(player.getUniqueId());
                            if ((player.isOnline() && Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immune")) || (!player.isOnline() && immunePerms.contains(player.getUniqueId().toString())) || (immunityType == 1 && immunitySpent >= permanentCost)) {
                                // has permanent immunity
                                sender.sendMessage(parse(prefix + permanentImmunity, player.getName(), immunitySpent, player));
                                return true;
                            }
                            if (immunityType == 3 && immunityTimeTracker.containsKey(player.getUniqueId())) {
                                // has time immunity
                                sender.sendMessage(parse(prefix + LanguageOptions.timeImmunity.replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime(immunityTimeTracker.get(player.getUniqueId()) - System.currentTimeMillis()))), player.getName(), immunitySpent, player));
                                return true;
                            }
                            if (amount <= immunitySpent && immunityType == 2) {
                                // has scaling immunity
                                sender.sendMessage(parse(prefix + scalingImmunity, player.getName(), immunitySpent, player));
                                return true;
                            }

                            if (!args[args.length - 1].equalsIgnoreCase("--confirm") && confirmation) {
                                openGUI(parser, "confirm-bounty", (long) amount, player.getUniqueId(), (long) amount);
                                return true;
                            }

                            if (checkBalance(parser, total)) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {

                                            // async ban check
                                            if (!player.isOnline() && removeBannedPlayers && (player.isBanned() || (liteBansEnabled && !(new LiteBansClass().isPlayerNotBanned(player.getUniqueId()))))) {
                                                new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        // has permanent immunity
                                                        sender.sendMessage(parse(prefix + permanentImmunity, player.getName(), immunitySpent, player));
                                                    }
                                                }.runTask(NotBounties.getInstance());
                                            } else {
                                                new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        if (manualEconomy != ManualEconomy.PARTIAL)
                                                            NumberFormatting.doRemoveCommands(parser, total, new ArrayList<>());
                                                        addBounty(parser, player, amount, whitelist);
                                                        reopenBountiesGUI();
                                                    }
                                                }.runTask(NotBounties.getInstance());
                                            }
                                    }
                                }.runTaskAsynchronously(NotBounties.getInstance());
                            } else {
                                sender.sendMessage(parse(prefix + broke, total, parser));
                            }

                        } else {
                            addBounty(player, amount, whitelist);
                            reopenBountiesGUI();
                        }

                    } else {
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    }
                }
            } else {
                // open gui
                if (sender instanceof Player) {
                    if (sender.hasPermission("notbounties.view")) {
                        openGUI(parser, "bounty-gui", 1);
                    } else {
                        sender.sendMessage(parse(noPermission, parser));
                    }
                } else {
                    if (sender.hasPermission("notbounties.admin")) {
                        try {
                            NotBounties.getInstance().sendDebug(sender);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        List<String> tab = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("notbounties")) {
            if (args.length == 1) {
                if (sender.hasPermission("notbounties.basic")) {
                    tab.add("help");
                    tab.add("bdc");
                    tab.add("tutorial");
                }
                if (sender.hasPermission("notbounties.set")) {
                    for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                        tab.add(NotBounties.getPlayerName(p.getUniqueId()));
                    }
                    tab.add("set");
                }
                if (sender.hasPermission("notbounties.view")) {
                    tab.add("check");
                    tab.add("list");
                    tab.add("top");
                    tab.add("stat");
                }
                if (sender.hasPermission("notbounties.admin")) {
                    tab.add("remove");
                    tab.add("edit");
                    tab.add("reload");
                    tab.add("currency");
                    tab.add("debug");
                    tab.add("board");
                }
                if (sender.hasPermission("notbounties.admin") || giveOwnMap)
                    tab.add("poster");
                if (sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker")))
                    if (tracker)
                        tab.add("tracker");
                if (sender.hasPermission("notbounties.buyown") && buyBack) {
                    tab.add("buy");
                }
                if (immunityType > 0 || sender.hasPermission("notbounties.admin")) {
                    if (sender.hasPermission("notbounties.buyimmunity")) {
                        tab.add("immunity");
                    }
                }
                if (sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled) {
                    tab.add("whitelist");
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("check") && sender.hasPermission("notbounties.view")) {
                    List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit") && sender.hasPermission("notbounties.admin"))) {
                    List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if (args[0].equalsIgnoreCase("immunity")) {
                    if ((sender.hasPermission("notbounties.admin") || sender.hasPermission("notbounties.removeimmunity")))
                        tab.add("remove");
                    if ((sender.hasPermission("notbounties.admin") || sender.hasPermission("notbounties.buyimmunity")) && immunityType == 1)
                        tab.add("--confirm");
                } else if ((args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("stat")) && sender.hasPermission("notbounties.view")) {
                    tab.add("all");
                    tab.add("kills");
                    tab.add("claimed");
                    tab.add("deaths");
                    tab.add("set");
                    tab.add("immunity");
                    tab.add("current");
                } else if (args[0].equalsIgnoreCase("tracker") && tracker && (sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker")))) {
                    List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("notbounties.set")) {
                    tab.add("offline");
                } else if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled) {
                    tab.add("add");
                    tab.add("remove");
                    tab.add("set");
                    tab.add("offline");
                    tab.add("reset");
                    tab.add("view");
                    if (enableBlacklist)
                        tab.add("toggle");
                } else if (args[0].equalsIgnoreCase("poster") && (sender.hasPermission("notbounties.admin") || giveOwnMap)) {
                    List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if (args[0].equalsIgnoreCase("board") && sender.hasPermission("notbounties.admin")) {
                    tab.add("clear");
                    tab.add("remove");
                }
            } else if (args.length == 3) {
                if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit")) && sender.hasPermission("notbounties.admin")) {
                    tab.add("from");
                } else if (args[0].equalsIgnoreCase("immunity")) {
                    if (args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.admin")) {
                        for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                            tab.add(NotBounties.getPlayerName(p.getUniqueId()));
                        }
                    }
                    if ((sender.hasPermission("notbounties.admin") || sender.hasPermission("notbounties.buyimmunity")) && immunityType > 1)
                        tab.add("--confirm");
                } else if (args[0].equalsIgnoreCase("tracker") && tracker) {
                    for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                        tab.add(NotBounties.getPlayerName(p.getUniqueId()));
                    }
                } else if (args[0].equalsIgnoreCase("top") && sender.hasPermission("notbounties.view")) {
                    tab.add("list");
                }
            } else if (args.length == 4) {
                if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit")) && sender.hasPermission("notbounties.admin")) {
                    List<Bounty> bountyList = SQL.isConnected() ? data.getTopBounties(2) : BountyManager.bountyList;
                    for (Bounty bounty1 : bountyList) {
                        if (bounty1.getName().equalsIgnoreCase(args[1])) {
                            for (Setter setter : bounty1.getSetters()) {
                                tab.add(setter.getName());
                            }
                            break;
                        }
                    }
                }
            }
            if (args.length > 2) {
                boolean isNumber = true;
                try {
                    tryParse(args[1]);
                } catch (NumberFormatException e) {
                    isNumber = false;
                }
                if ((sender.hasPermission("notbounties.set") && isNumber) || (args[0].equalsIgnoreCase("whitelist") && !args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.whitelist")))
                    for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                        tab.add(NotBounties.getPlayerName(p.getUniqueId()));
                    }
                if (sender.hasPermission("notbounties.set") && isNumber && confirmation)
                    tab.add("--confirm");
                if (args[0].equalsIgnoreCase("whitelist") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.whitelist")) {
                    for (UUID uuid : getPlayerWhitelist(((Player) sender).getUniqueId()).getList()) {
                        tab.add(getPlayerName(uuid));
                    }
                }
            }

            String typed = args[args.length - 1];
            tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            if (tab.isEmpty()) {
                if (args.length == 1 || args.length > 2) {
                    if (sender.hasPermission("notbounties.set")) {
                        for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                            String name = Bukkit.getOfflinePlayer(entry.getValue()).getName();
                            if (name != null)
                                tab.add(name);
                            else
                                tab.add(entry.getKey());
                        }
                    }
                }
                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("immunity") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.admin")) {
                        for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                            String name = Bukkit.getOfflinePlayer(entry.getValue()).getName();
                            if (name != null)
                                tab.add(name);
                            else
                                tab.add(entry.getKey());
                        }
                    }
                }
                if (args.length > 2) {
                    if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist")) {
                        for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                            String name = Bukkit.getOfflinePlayer(entry.getValue()).getName();
                            if (name != null)
                                tab.add(name);
                            else
                                tab.add(entry.getKey());
                        }
                    }
                }
            }
            tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            Collections.sort(tab);
            return tab;
        }
        return null;
    }

    public static void reopenBountiesGUI() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                if (!GUI.playerInfo.containsKey(player.getUniqueId()))
                    continue;
                GUIOptions gui = GUI.getGUIByTitle(player.getOpenInventory().getTitle());
                if (gui == null)
                    continue;
                if (gui.getType().equals("bounty-gui"))
                    openGUI(player, gui.getType(), GUI.playerInfo.get(player.getUniqueId()).getPage());
            }
        }
    }

    public static Player getParser(CommandSender sender) {
        if (sender instanceof Player)
            return (Player) sender;
        return null;
    }
}

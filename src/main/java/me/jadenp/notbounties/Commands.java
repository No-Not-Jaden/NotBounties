package me.jadenp.notbounties;

import me.jadenp.notbounties.bountyEvents.BountyEditEvent;
import me.jadenp.notbounties.bountyEvents.BountyRemoveEvent;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.gui.GUI;
import me.jadenp.notbounties.gui.GUIOptions;
import me.jadenp.notbounties.map.BountyBoard;
import me.jadenp.notbounties.map.BountyMap;
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

import static me.jadenp.notbounties.gui.GUI.openGUI;
import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static me.jadenp.notbounties.utils.NumberFormatting.*;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.NotBounties.*;

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
                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               " + ChatColor.BLUE + ChatColor.BOLD + " Not" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Bounties " + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "               ");
                    sender.sendMessage(ChatColor.DARK_BLUE + "Bounty tax is currently: " + ChatColor.DARK_PURPLE + (bountyTax * 100) + "%");
                    sender.sendMessage(ChatColor.DARK_BLUE + whitelistText + " cost is currently: " + ChatColor.DARK_PURPLE + currencyPrefix + bountyWhitelistCost + currencySuffix + " per player.");
                    sender.sendMessage(ChatColor.BLUE + "/bounty help" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Shows this message.");
                    sender.sendMessage(ChatColor.BLUE + "/bounty tutorial" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Shows this message.");
                    sender.sendMessage(ChatColor.BLUE + "/bounty bdc" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Toggles the bounty broadcast message.");
                    if (sender.hasPermission("notbounties.view")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty check (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Checks a bounty.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty list" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists all bounties.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty top (all/kills/claimed/deaths/set/immunity) <list>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists the top 10 players with the respective stats.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens bounty GUI.");
                    }
                    if (sender.hasPermission("notbounties.set")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty (player) (amount) <" + whitelistText + "ed players>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Adds a bounty to a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty set <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set bounty GUI.");
                    }
                    if (sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " (add/remove/set) (" + whitelistText + "ed players)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Change the players that can claim the bounties you set.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set whitelist GUI.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " reset" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Resets your whitelisted players.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " view" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Displays your whitelisted players in chat.");
                        if (enableBlacklist) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " toggle" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Toggles your whitelist into a blacklist or vice versa.");
                        }
                    }
                    if (sender.hasPermission("notbounties.buyown") & buyBack) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty buy" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buys your own bounty for " + buyBackInterest + "x the price.");
                    }
                    if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                        if (immunityType == 1) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + currencyPrefix + NumberFormatting.formatNumber(permanentCost) + currencySuffix + " currency.");
                        } else if (immunityType == 2) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + NumberFormatting.formatNumber(scalingRatio) + "x the price you spend.");
                        } else if (immunityType == 3) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + timeImmunity + " seconds.");
                        }
                    }
                    if (sender.hasPermission("notbounties.removeimmunity")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                    }
                    if (sender.hasPermission("notbounties.admin") || giveOwnMap) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty poster (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a poster of a player's bounty.");
                        if (sender.hasPermission("notbounties.admin"))
                            sender.sendMessage(ChatColor.BLUE + "/bounty poster (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a poster of a player's bounty.");
                    }
                    if (tracker)
                        if (sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker"))) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a compass that tracks a player with a bounty.");
                            if (sender.hasPermission("notbounties.admin"))
                                sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a compass that tracks a player with a bounty.");
                        }
                    if (sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty board <rank>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Creates a board for current bounties.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty board clear" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes every bounty board.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty board remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty board.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty reload" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Reloads the plugin.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty debug" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Shows debugging info.");
                    }

                    if (sender.hasPermission("notbounties.immune")) {
                        sender.sendMessage(ChatColor.DARK_PURPLE + "Note: You are immune to having bounties placed on you.");
                    }

                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                 ");
                } else if (args[0].equalsIgnoreCase("debug")) {
                    if (!sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
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
                            sender.sendMessage(parse(speakings.get(0) + ChatColor.DARK_RED + "Removed " + bountyBoards.size() + " bounty boards.", parser));
                            removeBountyBoard();

                            return true;
                        }
                        if (args[1].equalsIgnoreCase("remove")) {
                            sender.sendMessage(parse(speakings.get(0) + ChatColor.RED + "Right click the bounty board to remove.", parser));
                            NotBounties.boardSetup.put(parser.getUniqueId(), -1);
                            return true;
                        }
                        try {
                            rank = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(1), parser));
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
                    sender.sendMessage(parse(speakings.get(0) + ChatColor.DARK_AQUA + ChatColor.BOLD + "<Rank " + rank + "> " + ChatColor.AQUA + "Punch a block to place the bounty board on.", parser));
                    NotBounties.boardSetup.put(parser.getUniqueId(), rank);
                    return true;
                } else if (args[0].equalsIgnoreCase("tutorial") && sender.hasPermission("notbounties.basic")) {
                    Tutorial.onCommand(sender, args);
                    return true;
                } else if (args[0].equalsIgnoreCase("whitelist") && bountyWhitelistEnabled) {
                    if (sender instanceof Player) {
                        if (!sender.hasPermission("notbounties.whitelist")) {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
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
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(57), parser));
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
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(13) + names, parser));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("toggle") && enableBlacklist) {
                            if (getPlayerWhitelist(((Player) sender).getUniqueId()).toggleBlacklist()) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(69), parser));
                            } else {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(63), parser));
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
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[i], parser));
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
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(58), parser));
                                if (previousWhitelist.size() == 10)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(56), parser));
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("remove")) {
                                if (!previousWhitelist.removeIf(whitelist::contains)) {
                                    // nobody removed
                                    StringBuilder builder = new StringBuilder(args[2]);
                                    for (int i = 3; i < args.length; i++) {
                                        builder.append(", ").append(args[i]);
                                    }
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), builder.toString(), parser));
                                    return true;
                                }
                                getPlayerWhitelist(parser.getUniqueId()).setList(previousWhitelist);
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(58), parser));
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("set")) {
                                getPlayerWhitelist(parser.getUniqueId()).setList(whitelist);
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(58), parser));
                                return true;
                            }
                        }
                        // usage
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " (add/remove/set) (" + whitelistText + "ed players)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Change the players that can claim the bounties you set.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set whitelist GUI.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " reset" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Resets your whitelisted players.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty " + whitelistText + " view" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Displays your whitelisted players in chat.");
                        return true;
                    } else {
                        sender.sendMessage(speakings.get(0) + ChatColor.RED + "Only players can use this command.");
                    }
                } else if (args[0].equalsIgnoreCase("currency")) {
                    if (!sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
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
                            sender.sendMessage(speakings.get(0) + ChatColor.RED + "Only players can use this command.");
                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                    }
                } else if (args[0].equalsIgnoreCase("bdc") && sender.hasPermission("notbounties.basic")) {
                    if (sender instanceof Player) {
                        if (disableBroadcast.contains((parser).getUniqueId().toString())) {
                            // enable
                            disableBroadcast.remove((parser).getUniqueId().toString());
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(38), parser));
                        } else {
                            // disable
                            disableBroadcast.add((parser).getUniqueId().toString());
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(39), parser));
                        }
                    } else {
                        sender.sendMessage("You can't disable bounty broadcast! (yet?) If you really want to disable, send a message in the discord!");
                    }
                } else if (args[0].equalsIgnoreCase("stat")) {
                    if (!sender.hasPermission("notbounties.view")) {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
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
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        return true;
                    }
                    try {
                        Leaderboard.valueOf(args[1].toUpperCase()).displayStats(parser, false);
                    } catch (IllegalArgumentException e) {
                        // more usage
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
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
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
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
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("notbounties.admin")) {
                        try {
                            NotBounties.getInstance().loadConfig();
                        } catch (IOException e) {
                            Bukkit.getLogger().warning(e.toString());
                        }
                        reopenBountiesGUI();
                        sender.sendMessage(parse(speakings.get(0), parser) + ChatColor.GREEN + "Reloaded NotBounties version " + NotBounties.getInstance().getDescription().getVersion());
                    } else {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
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
                                            BountyManager.immunitySpent.put((parser).getUniqueId().toString(), 0.0);
                                        if (immunityType == 3)
                                            immunityTimeTracker.remove((parser).getUniqueId());
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(27), sender.getName(), (parser)));
                                    } else {
                                        // doesn't have immunity
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(29), sender.getName(), (parser)));
                                    }
                                } else {
                                    Bukkit.getLogger().info("You don't have immunity!");
                                }
                            } else {
                                // usage
                                if (sender instanceof Player)
                                    if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                        if (immunityType == 1) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                        } else if (immunityType == 2) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        } else if (immunityType == 3) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + timeImmunity + " seconds.");
                                        }
                                    } else {
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                    }
                            }
                        } else if (args.length == 3) {
                            //admin command
                            if (sender.hasPermission("notbounties.admin")) {
                                OfflinePlayer p = getPlayer(args[2]);
                                if (p == null) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[0], parser));
                                    return true;
                                }
                                double immunitySpent = SQL.isConnected() ? data.getImmunity(p.getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(p.getUniqueId());
                                if (immunitySpent > 0) {
                                    if (SQL.isConnected())
                                        data.setImmunity(p.getUniqueId().toString(), 0);
                                    else
                                        BountyManager.immunitySpent.put(p.getUniqueId().toString(), 0.0);
                                    if (immunityType == 3)
                                        immunityTimeTracker.remove(p.getUniqueId());
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(27), p.getName(), p));
                                } else {
                                    // doesn't have immunity
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(29), p.getName(), p));
                                }
                            } else {
                                if (sender.hasPermission("notbounties.removeimmunity")) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                                } else if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                    if (immunityType == 1) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                    } else if (immunityType == 2) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                    } else if (immunityType == 3) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + timeImmunity + " seconds.");
                                    }
                                } else {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                }
                            }
                        } else {
                            // usage
                            if (sender.hasPermission("notbounties.admin")) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from a player.");
                            } else if (sender.hasPermission("notbounties.removeimmunity")) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                            } else if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                if (immunityType == 1) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                } else if (immunityType == 2) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                } else if (immunityType == 3) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + timeImmunity + " seconds.");
                                }
                            } else {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
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
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(20), data.getImmunity((parser).getUniqueId().toString()), parser));
                                                    } else {
                                                        BountyManager.immunitySpent.put((parser).getUniqueId().toString(), Leaderboard.IMMUNITY.getStat((parser).getUniqueId()) + permanentCost);
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(20), Leaderboard.IMMUNITY.getStat((parser).getUniqueId()), parser));
                                                    }
                                                } else {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(6), permanentCost, parser));
                                                }

                                            } else {
                                                // ask to repeat
                                                repeatBuyCommand2.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(30), permanentCost, parser));
                                            }
                                        } else {
                                            // already bought immunity
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(25), parser));
                                        }
                                    } else {
                                        if (args.length > 1) {
                                            double amount;
                                            try {
                                                amount = tryParse(args[1]);
                                            } catch (NumberFormatException ignored) {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(1), parser));
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
                                                        immunitySpent.put((parser).getUniqueId().toString(), Leaderboard.IMMUNITY.getStat((parser).getUniqueId()) + (amount));
                                                        immunity = Leaderboard.IMMUNITY.getStat((parser).getUniqueId());
                                                    }
                                                    if (immunityType == 2) {
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(21), immunity * scalingRatio, parser));
                                                    } else {
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(61).replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime((long) (immunity * timeImmunity * 1000L)))), immunity * timeImmunity, parser));
                                                        immunityTimeTracker.put((parser).getUniqueId(), (long) (System.currentTimeMillis() + immunity * timeImmunity * 1000L));
                                                    }
                                                } else {
                                                    // broke
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(6), amount, parser));
                                                }
                                            } else {
                                                // ask to repeat
                                                repeatBuyCommand2.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(30), amount, parser));
                                            }
                                        } else {
                                            // usage
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        }
                                    }
                                } else {
                                    // no permission
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                                }
                            } else {
                                // unknown command
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
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
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(14), sender.getName(), parser));
                                        } else {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(6), (bounty.getTotalBounty() * buyBackInterest), parser));
                                        }


                                    } else {
                                        // ask to repeat
                                        repeatBuyCommand.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(17), (bounty.getTotalBounty() * buyBackInterest), parser));
                                    }
                                } else {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(8), sender.getName(), parser));
                                }
                            } else {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                            }
                        } else {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                        }
                    } else {
                        sender.sendMessage("You don't have a bounty!");
                    }
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (sender.hasPermission("notbounties.view")) {
                        if (args.length > 1) {
                            OfflinePlayer p = getPlayer(args[1]);
                            if (p == null) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[1], parser));
                                return true;
                            }
                            if (!hasBounty(p)) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(8), p.getName(), parser));
                                return true;
                            }
                            Bounty bounty = getBounty(p);
                            assert bounty != null;
                            double bountyAmount = showWhitelistedBounties || sender.hasPermission("notbounties.admin") || !(sender instanceof Player) ? bounty.getTotalBounty() : bounty.getTotalBounty(parser);
                            if (bountyAmount == 0) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(8), p.getName(), parser));
                                return true;
                            }

                            sender.sendMessage(parse(speakings.get(0) + speakings.get(9), p.getName(), bounty.getTotalBounty(parser), bounty.getLatestSetter(), parser));
                            for (Setter setters : bounty.getSetters()) {
                                if (showWhitelistedBounties || sender.hasPermission("notbounties.admin") || !(sender instanceof Player) || setters.canClaim(parser)) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(10), setters.getName(), setters.getAmount(), setters.getTimeCreated(), parser));
                                    if (!setters.canClaim(parser))
                                        notWhitelistedLore.stream().filter(s -> !s.isEmpty()).map(s -> parse(s, parser)).forEach(sender::sendMessage);
                                }
                            }
                            return true;

                        } else {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                            sender.sendMessage(ChatColor.BLUE + "/bounty check (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Checks a bounty.");
                        }
                    } else {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
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
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
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
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), player));
                                    } else {
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
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
                                            if (toRemove.getTotalBounty() < minBounty)
                                                NotBounties.removeWantedTag(toRemove.getUUID());

                                            Player player = Bukkit.getPlayer(toRemove.getUUID());
                                            if (player != null) {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), player));
                                            } else {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                            }

                                        } else {
                                            // couldnt find setter

                                            Player player = Bukkit.getPlayer(toRemove.getUUID());
                                            if (player != null) { // player then receiver
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(16), args[3], toRemove.getName(), player));
                                            } else {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(16), args[3], toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                            }
                                        }

                                    } else {
                                        // usage
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                }
                            } else {
                                // could not find bounty
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], parser));

                            }
                        } else {
                            // usage
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");

                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
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
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(1), parser));

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
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(15), toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));


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
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(1), parser));

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
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(15), toEdit.getName(), player));
                                            } else {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(15), toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                            }

                                        } else {
                                            // couldnt find setter

                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(16), args[3], toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));

                                        }

                                    } else {
                                        // usage
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                }
                            } else {
                                // couldn't find bounty
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], parser));

                            }
                        } else {
                            // usage
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                            sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                            sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");

                        }
                    } else {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                    }
                } else if (args[0].equalsIgnoreCase("poster")) {
                    if (!(giveOwnMap || sender.hasPermission("notbounties.admin"))) {
                        // no permission
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                        return true;
                    }
                    if (args.length == 1) {
                        // usage
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                        sender.sendMessage(ChatColor.BLUE + "/bounty poster (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a poster of a player's bounty.");
                        if (sender.hasPermission("notbounties.admin"))
                            sender.sendMessage(ChatColor.BLUE + "/bounty poster (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a poster of a player's bounty.");

                        return true;
                    }
                    OfflinePlayer player = getPlayer(args[1]);
                    if (!hasBounty(player)) {
                        // couldn't find bounty
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], parser));
                        return true;

                    }
                    Bounty bounty = getBounty(player);
                    assert bounty != null;
                    Player receiver;
                    if (args.length > 2) {
                        receiver = Bukkit.getPlayer(args[2]);
                        if (receiver == null) {
                            // can't find player
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[2], parser));
                            return true;
                        }
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(67), args[2], bounty.getName(), player));
                    } else if (sender instanceof Player) {
                        receiver = parser;
                    } else {
                        sender.sendMessage("You cant give yourself this item!");
                        return true;
                    }
                    BountyMap.giveMap(receiver, bounty);
                    receiver.sendMessage(parse(speakings.get(0) + speakings.get(68), bounty.getName(), player));
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
                                    meta.setDisplayName(parse(speakings.get(32), player.getName(), player));

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
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                                            return true;
                                        }
                                        // find player to give to
                                        Player receiver = Bukkit.getPlayer(args[2]);
                                        if (receiver != null) {
                                            NumberFormatting.givePlayer(receiver, compass, 1);
                                            // you have been given & you have received
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(33), receiver.getName(), player.getName(), player));

                                            receiver.sendMessage(parse(speakings.get(0) + speakings.get(34), player.getName(), player));
                                        } else {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[2], parser));

                                        }
                                    } else {
                                        if (sender instanceof Player) {
                                            NumberFormatting.givePlayer(parser, compass, 1);
                                            // you have been given
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(34), player.getName(), player));
                                        } else {
                                            sender.sendMessage("You are not a player!");
                                        }
                                    }
                                } else {
                                    // couldn't find bounty
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], parser));

                                }
                            } else {
                                // usage
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a compass that tracks a player with a bounty.");
                                sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a compass that tracks a player with a bounty.");

                            }
                        } else {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                        }
                } else {
                    if (sender.hasPermission("notbounties.set")) {
                        OfflinePlayer player = getPlayer(args[0]);
                        if (player == null) {
                            // can't find player
                            if (args.length == 1) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), parser));
                                sender.sendMessage(ChatColor.BLUE + "/bounty (player) (amount) <whitelisted players>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Adds a bounty to a player.");
                                sender.sendMessage(ChatColor.BLUE + "/bounty set <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set bounty GUI.");
                            } else {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[0], parser));
                            }

                            return true;
                        }
                        if (args.length == 1) {
                            if (sender instanceof Player)
                                openGUI(parser, "select-price", minBounty, player.getUniqueId().toString());
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
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[i], parser));
                                    return true;
                                }
                                newWhitelist.add(p.getUniqueId());
                            }
                            whitelist.setList(newWhitelist);
                        }

                        // check if max setters reached
                        if (maxSetters > -1) {
                            if (hasBounty(player)) {
                                Bounty bounty = getBounty(player);
                                assert bounty != null;
                                if (bounty.getSetters().size() >= maxSetters) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(65), args[0], parser));
                                    return true;
                                }
                            }
                        }

                        // check if we can get the amount
                        double amount;
                        try {
                            amount = tryParse(args[1]);
                        } catch (NumberFormatException ignored) {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(1), parser));
                            return true;
                        }
                        if (amount < minBounty) {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(23), minBounty, parser));
                            return true;
                        }
                        double total = amount + amount * bountyTax + whitelist.getList().size() * bountyWhitelistCost;

                        // check if it is a player

                        if (sender instanceof Player) {
                            if (gracePeriod.containsKey(player.getUniqueId().toString())) {
                                long timeSinceDeath = System.currentTimeMillis() - gracePeriod.get(player.getUniqueId().toString());
                                if (timeSinceDeath < graceTime * 1000L) {
                                    // still in grace period
                                    long timeLeft = (graceTime * 1000L) - timeSinceDeath;

                                    String message = parse(speakings.get(0) + speakings.get(22).replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime(timeLeft))), player.getName(), player);
                                    sender.sendMessage(message);
                                    return true;
                                } else {
                                    gracePeriod.remove(player.getUniqueId().toString());
                                }
                            }
                            double immunitySpent = SQL.isConnected() ? data.getImmunity(player.getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(player.getUniqueId());
                            if ((player.isOnline() && Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immune")) || (!player.isOnline() && immunePerms.contains(player.getUniqueId().toString())) || (immunityType == 1 && immunitySpent >= permanentCost)) {
                                // has permanent immunity
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(18), player.getName(), immunitySpent, player));
                                return true;
                            }
                            if (immunityType == 3 && immunityTimeTracker.containsKey(player.getUniqueId())) {
                                // has time immunity
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(62).replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime(immunityTimeTracker.get(player.getUniqueId()) - System.currentTimeMillis()))), player.getName(), immunitySpent, player));
                                return true;
                            }
                            if (amount <= immunitySpent && immunityType == 2) {
                                // has scaling immunity
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(19), player.getName(), immunitySpent, player));
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

                                        if (removeBannedPlayers) {
                                            // async ban check
                                            if (player.isBanned() || (liteBansEnabled && !(new LiteBansClass().isPlayerNotBanned(player.getUniqueId())))) {
                                                new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        // has permanent immunity
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(18), player.getName(), immunitySpent, player));
                                                    }
                                                }.runTask(NotBounties.getInstance());
                                            } else {
                                                new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        NumberFormatting.doRemoveCommands(parser, total, new ArrayList<>());
                                                        addBounty(parser, player, amount, whitelist);
                                                        reopenBountiesGUI();
                                                    }
                                                }.runTask(NotBounties.getInstance());
                                            }
                                        } else {
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    NumberFormatting.doRemoveCommands(parser, total, new ArrayList<>());
                                                    addBounty(parser, player, amount, whitelist);
                                                    reopenBountiesGUI();
                                                }
                                            }.runTask(NotBounties.getInstance());

                                        }
                                    }
                                }.runTaskAsynchronously(NotBounties.getInstance());
                            } else {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(6), total, parser));
                            }

                        } else {
                            addBounty(player, amount, whitelist);
                            reopenBountiesGUI();
                        }

                    } else {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), parser));
                    }
                }
            } else {
                // open gui
                if (sender instanceof Player) {
                    if (sender.hasPermission("notbounties.view")) {
                        openGUI(parser, "bounty-gui", 1);
                    } else {
                        sender.sendMessage(parse(speakings.get(5), parser));
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
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        tab.add(p.getName());
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
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            tab.add(player.getName());
                        }
                    }
                    if ((sender.hasPermission("notbounties.admin") || sender.hasPermission("notbounties.buyimmunity")) && immunityType > 1)
                        tab.add("--confirm");
                } else if (args[0].equalsIgnoreCase("tracker") && tracker) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        tab.add(player.getName());
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
                    for (Player p : Bukkit.getOnlinePlayers())
                        tab.add(p.getName());
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
                        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                            String name = Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue())).getName();
                            if (name != null)
                                tab.add(name);
                            else
                                tab.add(entry.getKey());
                        }
                    }
                }
                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("immunity") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.admin")) {
                        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                            String name = Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue())).getName();
                            if (name != null)
                                tab.add(name);
                            else
                                tab.add(entry.getKey());
                        }
                    }
                }
                if (args.length > 2) {
                    if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist")) {
                        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                            String name = Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue())).getName();
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

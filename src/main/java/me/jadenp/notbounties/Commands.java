package me.jadenp.notbounties;

import me.jadenp.notbounties.gui.GUI;
import me.jadenp.notbounties.gui.GUIOptions;
import me.jadenp.notbounties.map.BountyMap;
import me.jadenp.notbounties.utils.CurrencySetup;
import me.jadenp.notbounties.utils.NumberFormatting;
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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static me.jadenp.notbounties.utils.NumberFormatting.*;

public class Commands implements CommandExecutor, TabCompleter {
    private final NotBounties nb;

    public Commands(NotBounties nb) {
        this.nb = nb;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("notbounties")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               " + ChatColor.BLUE + "" + ChatColor.BOLD + " Not" + ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Bounties " + ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               ");
                    sender.sendMessage(ChatColor.DARK_BLUE + "Bounty tax is currently: " + ChatColor.DARK_PURPLE + (bountyTax * 100) + "%");
                    sender.sendMessage(ChatColor.DARK_BLUE + "Whitelist cost is currently: " + ChatColor.DARK_PURPLE + currencyPrefix + bountyWhitelistCost + currencySuffix + " per player.");
                    sender.sendMessage(ChatColor.BLUE + "/bounty help" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Shows this message.");
                    sender.sendMessage(ChatColor.BLUE + "/bounty bdc" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Toggles the bounty broadcast message.");
                    if (sender.hasPermission("notbounties.view")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty check (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Checks a bounty.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty list" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists all bounties.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty top (all/kills/claimed/deaths/set/immunity) <list>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists the top 10 players with the respective stats.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens bounty GUI.");
                    }
                    if (sender.hasPermission("notbounties.set")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty (player) (amount) <whitelisted players>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Adds a bounty to a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty set <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set bounty GUI.");
                    }
                    if (sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist (add/remove/set) (whitelisted players)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Change the players that can claim the bounties you set.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set whitelist GUI.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist reset" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Resets your whitelisted players.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist view" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Displays your whitelisted players in chat.");
                    }
                    if (sender.hasPermission("notbounties.buyown") & buyBack) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty buy" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buys your own bounty for " + buyBackInterest + "x the price.");
                    }
                    if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                        if (immunityType == 1) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                        } else if (immunityType == 2){
                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                        } else if (immunityType == 3){
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
                        sender.sendMessage(ChatColor.BLUE + "/bounty reload" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Reloads the plugin.");
                    }

                    if (sender.hasPermission("notbounties.immune")) {
                        sender.sendMessage(ChatColor.DARK_PURPLE + "Note: You are immune to having bounties placed on you.");
                    }

                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                 ");
                } else if (args[0].equalsIgnoreCase("whitelist") && bountyWhitelistEnabled) {
                    if (sender instanceof Player) {
                        if (!sender.hasPermission("notbounties.whitelist")) {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                            return true;
                        }
                        if (args.length == 1){
                            GUI.openGUI((Player) sender, "set-whitelist", 1);
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("offline")) {
                            GUI.openGUI((Player) sender, "set-whitelist", 1, "offline");
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("reset")){
                            nb.playerWhitelist.put(((Player) sender).getUniqueId(), new ArrayList<>());
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(57), (Player) sender));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("view")){
                            List<UUID> whitelist = nb.getPlayerWhitelist(((Player) sender).getUniqueId());
                            StringBuilder names = new StringBuilder(" ");
                            for (UUID uuid : whitelist) {
                                names.append(nb.getPlayerName(uuid)).append(", ");
                            }
                            if (names.length() > 1) {
                                names.replace(names.length() - 2, names.length() - 1, "");
                            } else {
                                names.append("<none>");
                            }
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(13) + names, (Player) sender));
                            return true;
                        }
                        if (args.length > 2) {
                            List<UUID> whitelist = new ArrayList<>();
                            List<UUID> previousWhitelist = nb.playerWhitelist.containsKey(((Player) sender).getUniqueId()) ? nb.playerWhitelist.get(((Player) sender).getUniqueId()) : new ArrayList<>();
                            for (int i = 2; i < Math.min(args.length, 12); i++) {
                                OfflinePlayer player = nb.getPlayer(args[i]);
                                if (player == null) {
                                    // unknown player
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[i], (Player) sender));
                                    return true;
                                }
                                if (!whitelist.contains(player.getUniqueId()) && !previousWhitelist.contains(player.getUniqueId()))
                                    whitelist.add(player.getUniqueId());
                            }

                            if (args[1].equalsIgnoreCase("add")) {
                                previousWhitelist.addAll(whitelist);
                                while (previousWhitelist.size() > 10)
                                    previousWhitelist.remove(10);
                                nb.playerWhitelist.put(((Player) sender).getUniqueId(), previousWhitelist);
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(58), (Player) sender));
                                if (previousWhitelist.size() == 10)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(56), (Player) sender));
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("remove")) {
                                if (!previousWhitelist.removeIf(whitelist::contains)) {
                                    // nobody removed
                                    StringBuilder builder = new StringBuilder(args[2]);
                                    for (int i = 3; i < args.length; i++) {
                                        builder.append(", ").append(args[i]);
                                    }
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), builder.toString(), (Player) sender));
                                    return true;
                                }
                                nb.playerWhitelist.put(((Player) sender).getUniqueId(), previousWhitelist);
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(58), (Player) sender));
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("set")) {
                                nb.playerWhitelist.put(((Player) sender).getUniqueId(), whitelist);
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(58), (Player) sender));
                                return true;
                            }
                        }
                        // usage
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist (add/remove/set) (whitelisted players)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Change the players that can claim the bounties you set.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist <offline>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens the set whitelist GUI.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist reset" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Resets your whitelisted players.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty whitelist view" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Displays your whitelisted players in chat.");
                        return true;
                    } else {
                        sender.sendMessage(speakings.get(0) + ChatColor.RED + "Only players can use this command.");
                    }
                } else if (args[0].equalsIgnoreCase("currency")) {
                    if (!sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                        return true;
                    }
                    CurrencySetup.onCommand(sender, args);
                } else if (args[0].equalsIgnoreCase("set")) {
                    if (sender.hasPermission("notbounties.set")) {
                        if (sender instanceof Player) {
                            if (args.length > 1)
                                GUI.openGUI((Player) sender, "set-bounty", 1, args[1]);
                            else
                                GUI.openGUI((Player) sender, "set-bounty", 1);
                        } else {
                            sender.sendMessage(speakings.get(0) + ChatColor.RED + "Only players can use this command.");
                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("bdc")) {
                    if (sender instanceof Player) {
                        if (nb.disableBroadcast.contains(((Player) sender).getUniqueId().toString())) {
                            // enable
                            nb.disableBroadcast.remove(((Player) sender).getUniqueId().toString());
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(38), (Player) sender));
                        } else {
                            // disable
                            nb.disableBroadcast.add(((Player) sender).getUniqueId().toString());
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(39), (Player) sender));
                        }
                    } else {
                        sender.sendMessage("You can't disable bounty broadcast! (yet?) If you really want to disable, send a message in the discord!");
                    }
                } else if (args[0].equalsIgnoreCase("stat")) {
                    if (!sender.hasPermission("notbounties.view")) {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Only players can use this command!");
                        return true;
                    }
                    if (args.length == 1) {
                        Leaderboard.ALL.displayStats((Player) sender, false);
                        return true;
                    }
                    if (args.length != 2) {
                        // usage
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        return true;
                    }
                    try {
                        Leaderboard.valueOf(args[1].toUpperCase()).displayStats((Player) sender, false);
                    } catch (IllegalArgumentException e) {
                        // more usage
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
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
                                if (sender instanceof Player)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                else
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), null));
                                sender.sendMessage(ChatColor.BLUE + "/bounty top (all/kills/claimed/deaths/set/immunity) <list>" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists the top 10 players with the respective stats.");
                                return true;
                            }
                        } else {
                            leaderboard = Leaderboard.ALL;
                        }
                        if (args.length > 2 && args[2].equalsIgnoreCase("list")) {
                            leaderboard.displayTopStat(sender, 10);
                        } else {
                            GUI.openGUI((Player) sender, "leaderboard", 1, leaderboard);
                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("notbounties.admin")) {
                        try {
                            nb.loadConfig();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SQLException | ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        if (sender instanceof Player)
                            sender.sendMessage(parse(speakings.get(0), (Player) sender) + ChatColor.GREEN + "Reloaded NotBounties version " + this.nb.getDescription().getVersion());
                        else
                            Bukkit.getLogger().info("[NotBounties] Reloaded NotBounties version " + this.nb.getDescription().getVersion());
                    } else {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("immunity")) {
                    if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
                        if (args.length == 2) {
                            // reg command
                            if (sender.hasPermission("notbounties.removeimmunity") || sender.hasPermission("notbounties.admin")) {
                                if (sender instanceof Player) {
                                    // remove immunity
                                    double immunitySpent = nb.SQL.isConnected() ? nb.data.getImmunity(((Player) sender).getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(((Player) sender).getUniqueId());
                                    if (immunitySpent > 0) {
                                        if (nb.SQL.isConnected())
                                            nb.data.setImmunity(((Player) sender).getUniqueId().toString(), 0);
                                        else
                                            nb.immunitySpent.put(((Player) sender).getUniqueId().toString(), 0.0);
                                        if (immunityType == 3)
                                            nb.immunityTimeTracker.remove(((Player) sender).getUniqueId());
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(27), sender.getName(), ((Player) sender)));
                                    } else {
                                        // doesn't have immunity
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(29), sender.getName(), ((Player) sender)));
                                    }
                                } else {
                                    Bukkit.getLogger().info("You don't have immunity!");
                                }
                            } else {
                                // usage
                                if (sender instanceof Player)
                                    if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                        if (immunityType == 1) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                        } else if (immunityType == 2){
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        } else if (immunityType == 3){
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + timeImmunity + " seconds.");
                                        }
                                    } else {
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    }
                            }
                        } else if (args.length == 3) {
                            //admin command
                            if (sender.hasPermission("notbounties.admin")) {
                                OfflinePlayer p = nb.getPlayer(args[2]);
                                if (p == null) {
                                    if (sender instanceof Player)
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[0], (Player) sender));
                                    return true;
                                }
                                double immunitySpent = nb.SQL.isConnected() ? nb.data.getImmunity(p.getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(p.getUniqueId());
                                if (immunitySpent > 0) {
                                    if (nb.SQL.isConnected())
                                        nb.data.setImmunity(p.getUniqueId().toString(), 0);
                                    else
                                        nb.immunitySpent.put(p.getUniqueId().toString(), 0.0);
                                    if (immunityType == 3)
                                        nb.immunityTimeTracker.remove(p.getUniqueId());
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(27), p.getName(), p));
                                } else {
                                    // doesn't have immunity
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(29), p.getName(), p));
                                }
                            } else {
                                if (sender.hasPermission("notbounties.removeimmunity")) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                                } else if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    if (immunityType == 1) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                    } else if (immunityType == 2){
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                    } else if (immunityType == 3){
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + timeImmunity + " seconds.");
                                    }
                                } else {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                }
                            }
                        } else {
                            // usage
                            if (sender.hasPermission("notbounties.admin")) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from a player.");
                            } else if (sender.hasPermission("notbounties.removeimmunity")) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                            } else if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                if (immunityType == 1) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                } else if (immunityType == 2){
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                } else if (immunityType == 3){
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties for price * " + timeImmunity + " seconds.");
                                }
                            } else {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            }
                        }
                    } else {
                        if (sender instanceof Player) {
                            if (immunityType > 0) {
                                if (sender.hasPermission("notbounties.buyimmunity")) {
                                    if (immunityType == 1) {
                                        double immunitySpent = nb.SQL.isConnected() ? nb.data.getImmunity(((Player) sender).getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(((Player) sender).getUniqueId());
                                        if (immunitySpent < permanentCost && !sender.hasPermission("notbounties.immune")) {
                                            if ((nb.repeatBuyCommand2.containsKey(((Player) sender).getUniqueId().toString()) && System.currentTimeMillis() - nb.repeatBuyCommand2.get(((Player) sender).getUniqueId().toString()) < 30000) || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"))) {
                                                // try to find bounty and buy it
                                                nb.repeatBuyCommand2.remove(((Player) sender).getUniqueId().toString());
                                                if (checkBalance((Player) sender, permanentCost)) {
                                                    NumberFormatting.doRemoveCommands((Player) sender, permanentCost, new ArrayList<>());
                                                    // successfully bought perm immunity
                                                    if (nb.SQL.isConnected()) {
                                                        nb.data.addData(((Player) sender).getUniqueId().toString(), 0, 0, 0, 0, permanentCost, 0);
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(20), nb.data.getImmunity(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                    } else {
                                                        nb.immunitySpent.put(((Player) sender).getUniqueId().toString(), Leaderboard.IMMUNITY.getStat(((Player) sender).getUniqueId()) + permanentCost);
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(20), Leaderboard.IMMUNITY.getStat(((Player) sender).getUniqueId()), (Player) sender));
                                                    }
                                                } else {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(6), permanentCost, (Player) sender));
                                                }

                                            } else {
                                                // ask to repeat
                                                nb.repeatBuyCommand2.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(30), permanentCost, (Player) sender));
                                            }
                                        } else {
                                            // already bought immunity
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(25), (Player) sender));
                                        }
                                    } else {
                                        if (args.length > 1) {
                                            double amount;
                                            try {
                                                amount = tryParse(args[1]);
                                            } catch (NumberFormatException ignored) {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                                return true;
                                            }
                                            if ((nb.repeatBuyCommand2.containsKey(((Player) sender).getUniqueId().toString()) && System.currentTimeMillis() - nb.repeatBuyCommand2.get(((Player) sender).getUniqueId().toString()) < 30000) || args.length > 2 && args[2].equalsIgnoreCase("--confirm")) {
                                                // try to find bounty and buy it
                                                nb.repeatBuyCommand2.remove(((Player) sender).getUniqueId().toString());
                                                if (checkBalance((Player) sender, amount)) {
                                                    NumberFormatting.doRemoveCommands((Player) sender, amount, new ArrayList<>());
                                                    // successfully bought scaling immunity - amount x scalingRatio
                                                    double immunity;
                                                    if (nb.SQL.isConnected()) {
                                                        nb.data.addData(((Player) sender).getUniqueId().toString(), 0, 0, 0, 0, amount, 0);
                                                        immunity = nb.data.getImmunity(((Player) sender).getUniqueId().toString());
                                                    } else {
                                                        nb.immunitySpent.put(((Player) sender).getUniqueId().toString(), Leaderboard.IMMUNITY.getStat(((Player) sender).getUniqueId()) + (amount));
                                                        immunity = Leaderboard.IMMUNITY.getStat(((Player) sender).getUniqueId());
                                                    }
                                                    if (immunityType == 2) {
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(21), immunity * scalingRatio, (Player) sender));
                                                    } else {
                                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(61).replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime((long) (immunity * timeImmunity * 1000L)))), immunity * timeImmunity, (Player) sender));
                                                        nb.immunityTimeTracker.put(((Player) sender).getUniqueId(), (long) (System.currentTimeMillis() + immunity * timeImmunity * 1000L));
                                                    }
                                                } else {
                                                    // broke
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(6), amount, (Player) sender));
                                                }
                                            } else {
                                                // ask to repeat
                                                nb.repeatBuyCommand2.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(30), amount, (Player) sender));
                                            }
                                        } else {
                                            // usage
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        }
                                    }
                                } else {
                                    // no permission
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                                }
                            } else {
                                // unknown command
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            }
                        } else {
                            sender.sendMessage("Only players can buy immunity!");
                        }
                    }
                } else if (args[0].equalsIgnoreCase("buy")) {
                    if (sender instanceof Player) {
                        if (buyBack) {
                            if (sender.hasPermission("notbounties.buyown")) {
                                if (nb.hasBounty((Player) sender)) {
                                    Bounty bounty = nb.getBounty((Player) sender);
                                    assert bounty != null;
                                    if ((nb.repeatBuyCommand.containsKey(((Player) sender).getUniqueId().toString()) && System.currentTimeMillis() - nb.repeatBuyCommand.get(((Player) sender).getUniqueId().toString()) < 30000) || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"))) {
                                        // try to find bounty and buy it
                                        nb.repeatBuyCommand.remove(((Player) sender).getUniqueId().toString());
                                        if (checkBalance((Player) sender, (bounty.getTotalBounty() * buyBackInterest))) {
                                            NumberFormatting.doRemoveCommands((Player) sender, (bounty.getTotalBounty() * buyBackInterest), new ArrayList<>());
                                            if (nb.SQL.isConnected()) {
                                                nb.data.removeBounty(bounty.getUUID());
                                            } else {
                                                nb.bountyList.remove(bounty);
                                            }
                                            reopenBountiesGUI();
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(14), sender.getName(), (Player) sender));
                                        } else {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(6), (bounty.getTotalBounty() * buyBackInterest), (Player) sender));
                                        }


                                    } else {
                                        // ask to repeat
                                        nb.repeatBuyCommand.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(17), (bounty.getTotalBounty() * buyBackInterest), (Player) sender));
                                    }
                                } else {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(8), sender.getName(), (Player) sender));
                                }
                            } else {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                            }
                        } else {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                        }
                    } else {
                        sender.sendMessage("You don't have a bounty!");
                    }
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (sender.hasPermission("notbounties.view")) {
                        if (args.length > 1) {
                            OfflinePlayer p = nb.getPlayer(args[1]);
                            if (p == null) {
                                if (sender instanceof Player)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[1], (Player) sender));
                                return true;
                            }
                            if (nb.hasBounty(p) && (!(sender instanceof Player) || Objects.requireNonNull(nb.getBounty(p)).getTotalBounty((Player) sender) > 0)) {
                                Bounty bounty = nb.getBounty(p);
                                assert bounty != null;
                                assert sender instanceof Player;
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(9), p.getName(), bounty.getTotalBounty((Player) sender), bounty.getLatestSetter(), (Player) sender));
                                for (Setter setters : bounty.getSetters()) {
                                    if (setters.canClaim((Player) sender))
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(10), setters.getName(), setters.getAmount(), setters.getTimeCreated(), (Player) sender));
                                }
                            } else {
                                if (sender instanceof Player)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(8), p.getName(), (Player) sender));
                            }
                        } else {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            sender.sendMessage(ChatColor.BLUE + "/bounty check (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Checks a bounty.");
                        }
                    } else {
                        if (sender instanceof Player)
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
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

                        nb.listBounties(sender, page - 1);
                    } else {
                        if (sender instanceof Player)
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("remove")) {
                    if (sender.hasPermission("notbounties.admin")) {
                        if (args.length > 1) {
                            Bounty toRemove = null;
                            List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
                            for (Bounty bounty : bountyList) {
                                if (bounty.getName().equalsIgnoreCase(args[1])) {
                                    toRemove = bounty;
                                    break;
                                }
                            }
                            if (toRemove != null) {
                                if (args.length == 2) {
                                    if (nb.SQL.isConnected()) {
                                        nb.data.removeBounty(toRemove.getUUID());
                                    } else {
                                        nb.bountyList.remove(toRemove);
                                    }

                                    // successfully removed
                                    if (sender instanceof Player) {
                                        Player player = Bukkit.getPlayer(toRemove.getUUID());
                                        if (player != null) {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), player));
                                        } else {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                        }
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
                                        if (actualRemove != null) {
                                            toRemove.removeBounty(actualRemove.getUuid());
                                            if (toRemove.getSetters().size() == 0) {
                                                if (nb.SQL.isConnected()) {
                                                    nb.data.removeBounty(toRemove.getUUID());
                                                } else {
                                                    nb.bountyList.remove(toRemove);
                                                }
                                            } else if (nb.SQL.isConnected()) {
                                                nb.data.removeSetter(toRemove.getUUID(), actualRemove.getUuid());
                                            }
                                            // reopen gui for everyone
                                            reopenBountiesGUI();
                                            // successfully removed
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(toRemove.getUUID());
                                                if (player != null) {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), player));
                                                } else {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(14), toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                                }
                                            }
                                        } else {
                                            // couldnt find setter
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(toRemove.getUUID());
                                                if (player != null) { // player then receiver
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(16), args[3], toRemove.getName(), player));
                                                } else {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(16), args[3], toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                                }
                                            }
                                        }
                                    } else {
                                        // usage
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                }
                            } else {
                                // could not find bounty
                                if (sender instanceof Player) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], (Player) sender));
                                }
                            }
                        } else {
                            // usage
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");

                        }
                    } else {
                        // no permission
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("edit")) {
                    if (sender.hasPermission("notbounties.admin")) {
                        if (args.length > 2) {
                            Bounty toEdit = null;
                            List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
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
                                        if (sender instanceof Player) {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                        }
                                        return true;
                                    }
                                    if (nb.SQL.isConnected()) {
                                        nb.data.editBounty(toEdit.getUUID(), new UUID(0, 0), amount - toEdit.getTotalBounty());
                                    } else {
                                        toEdit.addBounty(amount - toEdit.getTotalBounty(), new ArrayList<>());
                                    }
                                    // successfully edited bounty
                                    if (sender instanceof Player) {
                                        Player player = Bukkit.getPlayer(toEdit.getUUID());
                                        if (player != null) {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(15), toEdit.getName(), player));
                                        } else {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(15), toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                        }
                                    }
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
                                                if (sender instanceof Player) {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                                }
                                                return true;
                                            }
                                            if (nb.SQL.isConnected()) {
                                                nb.data.editBounty(toEdit.getUUID(), actualEdit.getUuid(), amount - toEdit.getTotalBounty());
                                            } else {
                                                toEdit.editBounty(actualEdit.getUuid(), amount);
                                            }

                                            // successfully edited bounty
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(toEdit.getUUID());
                                                if (player != null) {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(15), toEdit.getName(), player));
                                                } else {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(15), toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                                }
                                            }
                                        } else {
                                            // couldnt find setter
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(toEdit.getUUID());
                                                if (player != null) { // player then receiver
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(16), args[3], toEdit.getName(), player));
                                                } else {
                                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(16), args[3], toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                                }
                                            }
                                        }
                                    } else {
                                        // usage
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                }
                            } else {
                                // couldn't fimd bounty
                                if (sender instanceof Player) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], (Player) sender));
                                }
                            }
                        } else {
                            // usage
                            if (sender instanceof Player) {
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                            }
                        }
                    } else {
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("poster")) {
                    if (!(giveOwnMap || sender.hasPermission("notbounties.admin"))) {
                        // no permission
                        sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                        return true;
                    }
                    if (args.length == 1) {
                        // usage
                        if (sender instanceof Player) {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            sender.sendMessage(ChatColor.BLUE + "/bounty poster (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a poster of a player's bounty.");
                            if (sender.hasPermission("notbounties.admin"))
                                sender.sendMessage(ChatColor.BLUE + "/bounty poster (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a poster of a player's bounty.");
                        }
                        return true;
                    }
                    OfflinePlayer player = nb.getPlayer(args[1]);
                    if (!nb.hasBounty(player)) {
                        // couldn't find bounty
                        if (sender instanceof Player) {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], (Player) sender));
                            return true;
                        }
                    }
                    Bounty bounty = nb.getBounty(player);
                    assert bounty != null;
                    Player receiver;
                    if (args.length > 2) {
                        receiver = Bukkit.getPlayer(args[2]);
                        if (receiver == null) {
                            // can't find player
                            if (sender instanceof Player)
                                sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[2], (Player) sender));
                            return true;
                        }
                        if (sender instanceof Player)
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(67), args[2], bounty.getName(), player));
                    } else if (sender instanceof Player){
                        receiver = (Player) sender;
                    } else {
                        sender.sendMessage("You cant give yourself this item!");
                        return true;
                    }
                    try {
                        BountyMap.giveMap(receiver, bounty);
                    } catch (IOException e) {
                        Bukkit.getLogger().warning("Error trying to generate a map.");
                        e.printStackTrace();
                        return true;
                    }
                    receiver.sendMessage(parse(speakings.get(0) + speakings.get(68), bounty.getName(), player));
                } else if (args[0].equalsIgnoreCase("tracker")) {
                    // give a tracker that points toward a certain player with a bounty
                    if (tracker)
                        if (sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker"))) {
                            if (args.length > 1) {
                                Bounty tracking = null;
                                List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
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
                                    if (nb.trackedBounties.containsValue(tracking.getUUID().toString())) {
                                        // get already used number
                                        for (Map.Entry<Integer, String> entry : nb.trackedBounties.entrySet()) {
                                            if (entry.getValue().equals(tracking.getUUID().toString())) {
                                                i = entry.getKey();
                                                break;
                                            }
                                        }
                                    } else {
                                        // get new number
                                        while (nb.trackedBounties.containsKey(i)) {
                                            i++;
                                        }
                                        nb.trackedBounties.put(i, tracking.getUUID().toString());
                                    }

                                    lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + "" + ChatColor.UNDERLINE + "" + ChatColor.ITALIC + "@" + i);
                                    meta.setLore(lore);
                                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                    compass.setItemMeta(meta);
                                    compass.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                                    if (args.length > 2) {
                                        // /bounty tracker (player) (receiver)
                                        if (!sender.hasPermission("notbounties.admin")) {
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                                            return true;
                                        }
                                        // find player to give to
                                        Player receiver = Bukkit.getPlayer(args[2]);
                                        if (receiver != null) {
                                            NumberFormatting.givePlayer(receiver, compass, 1);
                                            // you have been given & you have received
                                            if (sender instanceof Player) {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(33), receiver.getName(), player.getName(), player));
                                            }
                                            receiver.sendMessage(parse(speakings.get(0) + speakings.get(34), player.getName(), player));
                                        } else {
                                            if (sender instanceof Player) {
                                                sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[2], (Player) sender));
                                            }
                                        }
                                    } else {
                                        if (sender instanceof Player) {
                                            NumberFormatting.givePlayer((Player) sender, compass, 1);
                                            // you have been given
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(34), player.getName(), player));
                                        } else {
                                            sender.sendMessage("You are not a player!");
                                        }
                                    }
                                } else {
                                    // couldn't find bounty
                                    if (sender instanceof Player) {
                                        sender.sendMessage(parse(speakings.get(0) + speakings.get(8), args[1], (Player) sender));
                                    }
                                }
                            } else {
                                // usage
                                if (sender instanceof Player) {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a compass that tracks a player with a bounty.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a compass that tracks a player with a bounty.");
                                }
                            }
                        } else {
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                        }
                } else {
                    if (sender.hasPermission("notbounties.set")) {
                        if (args.length > 1) {

                            // get whitelisted people
                            List<UUID> whitelist = (sender instanceof Player) && sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled ? nb.getPlayerWhitelist(((Player) sender).getUniqueId()) : new ArrayList<>();
                            if (args.length > 2 && sender.hasPermission("notbounties.whitelist")) {
                                whitelist.clear();
                                for (int i = 2; i < Math.min(args.length, 12); i++) {
                                    OfflinePlayer player = nb.getPlayer(args[i]);
                                    if (player == null) {
                                        // unknown player
                                        if (sender instanceof Player)
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[0], (Player) sender));
                                        return true;
                                    }
                                    whitelist.add(player.getUniqueId());
                                }
                            }
                            OfflinePlayer player = nb.getPlayer(args[0]);
                            if (player == null) {
                                // can't find player
                                if (sender instanceof Player)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(3), args[0], (Player) sender));
                                return true;
                            }
                            // check if max setters reached
                            if (maxSetters > -1) {
                                if (nb.hasBounty(player)) {
                                    Bounty bounty = nb.getBounty(player);
                                    assert bounty != null;
                                    if (bounty.getSetters().size() >= maxSetters) {
                                        if (sender instanceof Player)
                                            sender.sendMessage(parse(speakings.get(0) + speakings.get(65), args[0], (Player) sender));
                                        return true;
                                    }
                                }
                            }

                            // check if we can get the amount
                            double amount;
                            try {
                                amount = tryParse(args[1]);
                            } catch (NumberFormatException ignored) {
                                if (sender instanceof Player)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                return true;
                            }
                            if (amount < minBounty) {
                                if (sender instanceof Player)
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(23), minBounty, (Player) sender));
                                return true;
                            }
                            double total = amount + amount * bountyTax + whitelist.size() * bountyWhitelistCost;

                            // check if it is a player

                            if (sender instanceof Player) {
                                if (nb.gracePeriod.containsKey(player.getUniqueId().toString())) {
                                    long timeSinceDeath = System.currentTimeMillis() - nb.gracePeriod.get(player.getUniqueId().toString());
                                    if (timeSinceDeath < graceTime * 1000L) {
                                        // still in grace period
                                        long timeLeft = (graceTime * 1000L) - timeSinceDeath;

                                        String message = parse(speakings.get(0) + speakings.get(22).replaceAll("\\{time}",Matcher.quoteReplacement(NotBounties.formatTime(timeLeft))) , player.getName(), player);
                                        sender.sendMessage(message);
                                        return true;
                                    } else {
                                        nb.gracePeriod.remove(player.getUniqueId().toString());
                                    }
                                }
                                double immunitySpent = nb.SQL.isConnected() ? nb.data.getImmunity(player.getUniqueId().toString()) : Leaderboard.IMMUNITY.getStat(player.getUniqueId());
                                if ((player.isOnline() && Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immune")) || (!player.isOnline() && nb.immunePerms.contains(player.getUniqueId().toString())) || (immunityType == 1 && immunitySpent >= permanentCost)) {
                                    // has permanent immunity
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(18), player.getName(), immunitySpent, player));
                                    return true;
                                }
                                if (immunityType == 3 && nb.immunityTimeTracker.containsKey(player.getUniqueId())) {
                                    // has time immunity
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(62).replaceAll("\\{time}", Matcher.quoteReplacement(NotBounties.formatTime(nb.immunityTimeTracker.get(player.getUniqueId()) - System.currentTimeMillis()))), player.getName(), immunitySpent, player));
                                    return true;
                                }
                                if (amount <= immunitySpent && immunityType == 2) {
                                    // has scaling immunity
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(19), player.getName(), immunitySpent, player));
                                    return true;
                                }

                                /*if (!args[args.length-1].equalsIgnoreCase("--confirm"))  {
                                    GUI.openGUI((Player) sender, "confirm", 1);
                                    return true;
                                }*/

                                if (checkBalance((Player) sender, total)) {
                                    NumberFormatting.doRemoveCommands((Player) sender, total, new ArrayList<>());
                                    nb.addBounty((Player) sender, player, amount, whitelist);
                                    reopenBountiesGUI();
                                } else {
                                    sender.sendMessage(parse(speakings.get(0) + speakings.get(6), total, (Player) sender));
                                }

                            } else {
                                nb.addBounty(player, amount, whitelist);
                                reopenBountiesGUI();
                            }

                        } else {
                            //incorrect usage
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            sender.sendMessage(ChatColor.BLUE + "/bounty (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Adds a bounty to a player.");
                        }
                    } else {
                        if (sender instanceof Player)
                            sender.sendMessage(parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                }
            } else {
                // open gui
                GUI.openGUI((Player) sender, "bounty-gui", 1);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        List<String> tab = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("notbounties")) {
            if (args.length == 1) {
                tab.add("help");
                tab.add("bdc");
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
                    List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit") && sender.hasPermission("notbounties.admin"))) {
                    List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
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
                } else if (args[0].equalsIgnoreCase("tracker") && tracker && (sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker")))) {
                    List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
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
                } else if (args[0].equalsIgnoreCase("poster") && (sender.hasPermission("notbounties.admin") || giveOwnMap)) {
                    List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
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
                    List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
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
                if (args[0].equalsIgnoreCase("whitelist") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.whitelist")) {
                    for (UUID uuid : nb.getPlayerWhitelist(((Player) sender).getUniqueId())) {
                        tab.add(nb.getPlayerName(uuid));
                    }
                }
            }

            String typed = args[args.length - 1];
            tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            if (tab.isEmpty()) {
                if (args.length == 1 || args.length > 2) {
                    if (sender.hasPermission("notbounties.set")) {
                        for (Map.Entry<String, String> entry : nb.loggedPlayers.entrySet()) {
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
                        for (Map.Entry<String, String> entry : nb.loggedPlayers.entrySet()) {
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
                        for (Map.Entry<String, String> entry : nb.loggedPlayers.entrySet()) {
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
                    GUI.openGUI(player, gui.getType(), GUI.playerInfo.get(player.getUniqueId()).getPage());
            }
        }
    }


}

package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
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
import static me.jadenp.notbounties.ConfigOptions.*;

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

                    sender.sendMessage(ChatColor.BLUE + "/bounty help" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Shows this message.");
                    sender.sendMessage(ChatColor.BLUE + "/bounty bdc" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Toggles the bounty broadcast message.");
                    if (sender.hasPermission("notbounties.view")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty check (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Checks a bounty.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty list" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists all bounties.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty top (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists the top 10 players with the respective stats.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Opens bounty GUI.");
                    }
                    if (sender.hasPermission("notbounties.set")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Adds a bounty to a player with a " + (bountyTax * 100) + "% tax.");
                    }
                    if (sender.hasPermission("notbounties.buyown") & buyBack) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty buy" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buys your own bounty for " + buyBackInterest + "x the price.");
                    }
                    if (sender.hasPermission("notbounties.buyimmunity") && buyImmunity) {
                        if (permanentImmunity) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                        } else {
                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                        }
                    }
                    if (sender.hasPermission("notbounties.removeimmunity")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                    }
                    if (sender.hasPermission("notbounties.admin")) {
                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                        if (tracker) {
                            sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a compass that tracks a player with a bounty.");
                            sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a compass that tracks a player with a bounty.");
                        }
                        sender.sendMessage(ChatColor.BLUE + "/bounty reload" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Reloads the plugin.");
                    }

                    if (sender.hasPermission("notbounties.immune")) {
                        sender.sendMessage(ChatColor.DARK_PURPLE + "Note: You are immune to having bounties placed on you.");
                    }

                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                 ");
                } else if (args[0].equalsIgnoreCase("bdc")) {
                    if (sender instanceof Player){
                        if (nb.disableBroadcast.contains(((Player) sender).getUniqueId().toString())){
                            // enable
                            nb.disableBroadcast.remove(((Player) sender).getUniqueId().toString());
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(38), (Player) sender));
                        } else {
                            // disable
                            nb.disableBroadcast.add(((Player) sender).getUniqueId().toString());
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(39), (Player) sender));
                        }
                    } else {
                        sender.sendMessage("You can't disable bounty broadcast! (yet?) If you really want to disable, send a message in the discord!");
                    }
                } else if (args[0].equalsIgnoreCase("stat")) {
                    if (!sender.hasPermission("notbounties.view")){
                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
                        return true;
                    }
                    if (!(sender instanceof  Player)){
                        sender.sendMessage("Only players can use this command!");
                        return true;
                    }
                    if (args.length == 1){
                        int amount;
                        if (nb.SQL.isConnected()){
                            amount = nb.data.getClaimed(((Player) sender).getUniqueId().toString());
                        } else {
                            amount = nb.bountiesClaimed.get(((Player) sender).getUniqueId().toString());
                        }
                        sender.sendMessage(parseStats(speakings.get(0) + speakings.get(45), amount, false, ((Player) sender)));
                        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                   ");
                        return true;
                    }
                    if (args.length != 2){
                        // usage
                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("all")){
                        int amount;
                        if (nb.SQL.isConnected()){
                            amount = nb.data.getAllTime(((Player) sender).getUniqueId().toString());
                        } else {
                            amount = nb.allTimeBounty.get(((Player) sender).getUniqueId().toString());
                        }
                        sender.sendMessage(parseStats(speakings.get(0) + speakings.get(44), amount, true, ((Player) sender)));
                    } else if (args[1].equalsIgnoreCase("kills")){
                        int amount;
                        if (nb.SQL.isConnected()){
                            amount = nb.data.getClaimed(((Player) sender).getUniqueId().toString());
                        } else {
                            amount = nb.bountiesClaimed.get(((Player) sender).getUniqueId().toString());
                        }
                        sender.sendMessage(parseStats(speakings.get(0) + speakings.get(45), amount, false, ((Player) sender)));
                    } else if (args[1].equalsIgnoreCase("claimed")){
                        int amount;
                        if (nb.SQL.isConnected()){
                            amount = nb.data.getTotalClaimed(((Player) sender).getUniqueId().toString());
                        } else {
                            amount = nb.allClaimed.get(((Player) sender).getUniqueId().toString());
                        }
                        sender.sendMessage(parseStats(speakings.get(0) + speakings.get(46), amount, true, ((Player) sender)));
                    } else if (args[1].equalsIgnoreCase("deaths")){
                        int amount;
                        if (nb.SQL.isConnected()){
                            amount = nb.data.getReceived(((Player) sender).getUniqueId().toString());
                        } else {
                            amount = nb.bountiesReceived.get(((Player) sender).getUniqueId().toString());
                        }
                        sender.sendMessage(parseStats(speakings.get(0) + speakings.get(47), amount, false, ((Player) sender)));
                    } else if (args[1].equalsIgnoreCase("set")){
                        int amount;
                        if (nb.SQL.isConnected()){
                            amount = nb.data.getSet(((Player) sender).getUniqueId().toString());
                        } else {
                            amount = nb.bountiesSet.get(((Player) sender).getUniqueId().toString());
                        }
                        sender.sendMessage(parseStats(speakings.get(0) + speakings.get(48), amount, false, ((Player) sender)));
                    } else if (args[1].equalsIgnoreCase("immunity")){
                        int amount;
                        if (nb.SQL.isConnected()){
                            amount = nb.data.getImmunity(((Player) sender).getUniqueId().toString());
                        } else {
                            amount = nb.immunitySpent.get(((Player) sender).getUniqueId().toString());
                        }
                        sender.sendMessage(parseStats(speakings.get(0) + speakings.get(49), amount, true, ((Player) sender)));
                    }
                    else {
                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                        sender.sendMessage(ChatColor.BLUE + "/bounty stat (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "View your bounty stats.");
                    }
                } else if (args[0].equalsIgnoreCase("top")) {
                    if (sender.hasPermission("notbounties.view")) {
                        if (sender instanceof Player)
                            sender.sendMessage(nb.parse(speakings.get(37), (Player) sender));
                        else
                            sender.sendMessage(nb.parse(speakings.get(37), null));
                        if (args.length == 1){
                            Map<String, Integer> sorted;
                            if (nb.SQL.isConnected()){
                                sorted = sortByValue(nb.data.getAllClaimed());
                            } else {
                                sorted = sortByValue(nb.bountiesClaimed);
                            }

                            int i = 1;
                            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                                if (i < 11) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                    sender.sendMessage(parseBountyTopString(i, player.getName(), entry.getValue(), "", "", player));
                                    i++;
                                } else {
                                    break;
                                }
                            }
                            sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                   ");
                            return true;
                        }
                        if (args.length != 2){
                            if (sender instanceof Player)
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            else
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), null));
                            sender.sendMessage(ChatColor.BLUE + "/bounty top (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists the top 10 players with the respective stats.");
                            return true;
                        }

                         if (args[1].equalsIgnoreCase("all")){
                            Map<String, Integer> sorted;
                            if (nb.SQL.isConnected()){
                                sorted = sortByValue(nb.data.getAllAllTime());
                            } else {
                                sorted = sortByValue(nb.allTimeBounty);
                            }

                            int i = 1;
                            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                                if (i < 11) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                    sender.sendMessage(parseBountyTopString(i, player.getName(), entry.getValue(), currencyPrefix, currencySuffix, player));
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        } else if (args[1].equalsIgnoreCase("kills")){
                            Map<String, Integer> sorted;
                            if (nb.SQL.isConnected()){
                                sorted = sortByValue(nb.data.getAllClaimed());
                            } else {
                                sorted = sortByValue(nb.bountiesClaimed);
                            }

                            int i = 1;
                            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                                if (i < 11) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                    sender.sendMessage(parseBountyTopString(i, player.getName(), entry.getValue(), "", "", player));
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        } else if (args[1].equalsIgnoreCase("claimed")){
                            Map<String, Integer> sorted;
                            if (nb.SQL.isConnected()){
                                sorted = sortByValue(nb.data.getAllTotalClaimed());
                            } else {
                                sorted = sortByValue(nb.allClaimed);
                            }

                            int i = 1;
                            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                                if (i < 11) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                    sender.sendMessage(parseBountyTopString(i, player.getName(), entry.getValue(), currencyPrefix, currencySuffix, player));
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        } else if (args[1].equalsIgnoreCase("deaths")){
                            Map<String, Integer> sorted;
                            if (nb.SQL.isConnected()){
                                sorted = sortByValue(nb.data.getAllReceived());
                            } else {
                                sorted = sortByValue(nb.bountiesReceived);
                            }

                            int i = 1;
                            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                                if (i < 11) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                    sender.sendMessage(parseBountyTopString(i, player.getName(), entry.getValue(), "", "", player));
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        } else if (args[1].equalsIgnoreCase("set")){
                            Map<String, Integer> sorted;
                            if (nb.SQL.isConnected()){
                                sorted = sortByValue(nb.data.getAllSet());
                            } else {
                                sorted = sortByValue(nb.bountiesSet);
                            }

                            int i = 1;
                            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                                if (i < 11) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                    sender.sendMessage(parseBountyTopString(i, player.getName(), entry.getValue(), "", "", player));
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        } else if (args[1].equalsIgnoreCase("immunity")){
                            Map<String, Integer> sorted;
                            if (nb.SQL.isConnected()){
                                sorted = sortByValue(nb.data.getAllImmunity());
                            } else {
                                sorted = sortByValue(nb.immunitySpent);
                            }

                            int i = 1;
                            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                                if (i < 11) {
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                    sender.sendMessage(parseBountyTopString(i, player.getName(), entry.getValue(), currencyPrefix, currencySuffix, player));
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        }

                        else {
                            // usage
                            if (sender instanceof Player)
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            else
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), null));
                            sender.sendMessage(ChatColor.BLUE + "/bounty top (all/kills/claimed/deaths/set/immunity)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Lists the top 10 players with the respective stats.");
                        }


                        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                   ");
                    } else {
                        // no permission
                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
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
                            sender.sendMessage(nb.parse(speakings.get(0), (Player) sender) + ChatColor.GREEN + " Reloaded NotBounties version " + this.nb.getDescription().getVersion());
                        else
                            Bukkit.getLogger().info("[NotBounties] Reloaded NotBounties version " + this.nb.getDescription().getVersion());
                    } else {
                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("immunity")) {
                    if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
                        if (args.length == 2) {
                            // reg command
                            if (sender.hasPermission("notbounties.removeimmunity") || sender.hasPermission("notbounties.admin")) {
                                if (sender instanceof Player) {
                                    // remove immunity
                                    if (nb.SQL.isConnected()){
                                        if (nb.data.getImmunity(((Player) sender).getUniqueId().toString()) > 0){
                                            nb.data.setImmunity(((Player) sender).getUniqueId().toString(), 0);
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(26), (Player) sender));
                                        } else {
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(28), (Player) sender));
                                        }
                                    } else {
                                        if (nb.immunitySpent.get(((Player) sender).getUniqueId().toString()) > 0) {
                                            nb.immunitySpent.replace(((Player) sender).getUniqueId().toString(), 0);
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(26), (Player) sender));
                                        } else {
                                            // doesn't have immunity
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(28), (Player) sender));
                                        }
                                    }

                                } else {
                                    Bukkit.getLogger().info("You don't have immunity!");
                                }
                            } else {
                                // usage
                                if (sender instanceof Player)
                                    if (sender.hasPermission("notbounties.buyimmunity") && buyImmunity) {
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                        if (permanentImmunity) {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                        } else {
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        }
                                    } else {
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    }
                            }
                        } else if (args.length == 3) {
                            //admin command
                            if (sender.hasPermission("notbounties.admin")) {
                                Player p = Bukkit.getPlayer(args[2]);
                                if (p != null) {
                                    if (nb.SQL.isConnected()){
                                        if (nb.data.getImmunity(p.getUniqueId().toString()) > 0){
                                            nb.data.setImmunity(p.getUniqueId().toString(), 0);
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(27), p.getName(), p));
                                        } else {
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(29), p.getName(), p));
                                        }
                                    } else {
                                        if (nb.immunitySpent.get(p.getUniqueId().toString()) > 0) {
                                            nb.immunitySpent.replace(p.getUniqueId().toString(), 0);
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(27), p.getName(), p));
                                        } else {
                                            // doesn't have immunity
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(29), p.getName(), p));
                                        }
                                    }
                                } else {
                                    // go through logged players
                                    if (nb.loggedPlayers.containsKey(args[2].toLowerCase(Locale.ROOT))) {
                                        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(nb.loggedPlayers.get(args[2].toLowerCase(Locale.ROOT))));
                                        if (nb.SQL.isConnected()){
                                            if (nb.data.getImmunity(player.getUniqueId().toString()) > 0){
                                                nb.data.setImmunity(player.getUniqueId().toString(), 0);
                                                sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(27), player.getName(), null)));
                                            } else {
                                                sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(29), player.getName(), null)));
                                            }
                                        } else {
                                            if (nb.immunitySpent.get(player.getUniqueId().toString()) > 0) {
                                                nb.immunitySpent.replace(player.getUniqueId().toString(), 0);
                                                sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(27), player.getName(), null)));
                                            } else {
                                                // doesn't have immunity
                                                sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(29), player.getName(), null)));
                                            }
                                        }
                                    } else {
                                        // unknown player
                                        if (sender instanceof Player)
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(3), args[0], (Player) sender));
                                    }
                                }
                            } else {
                                if (sender.hasPermission("notbounties.removeimmunity")) {
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                                } else if (sender.hasPermission("notbounties.buyimmunity") && buyImmunity) {
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    if (permanentImmunity) {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                    } else {
                                        sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                    }
                                } else {
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                }
                            }
                        } else {
                            // usage
                            if (sender.hasPermission("notbounties.admin")) {
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from a player.");
                            } else if (sender.hasPermission("notbounties.removeimmunity")) {
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                sender.sendMessage(ChatColor.BLUE + "/bounty immunity remove" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes purchased immunity from yourself.");
                            } else if (sender.hasPermission("notbounties.buyimmunity") && buyImmunity) {
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                if (permanentImmunity) {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you for " + permanentCost + " currency.");
                                } else {
                                    sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                }
                            } else {
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            }
                        }
                    } else {
                        if (sender instanceof Player) {
                            if (buyImmunity) {
                                if (sender.hasPermission("notbounties.buyimmunity")) {
                                    if (permanentImmunity) {
                                        int immunitySpent = nb.SQL.isConnected() ? nb.data.getImmunity(((Player) sender).getUniqueId().toString()) : nb.immunitySpent.get(((Player) sender).getUniqueId().toString());
                                        if (immunitySpent < permanentCost && !sender.hasPermission("notbounties.immune")) {
                                            if (nb.repeatBuyCommand2.containsKey(((Player) sender).getUniqueId().toString())) {
                                                if (System.currentTimeMillis() - nb.repeatBuyCommand2.get(((Player) sender).getUniqueId().toString()) < 30000) {
                                                    // try to find bounty and buy it
                                                    nb.repeatBuyCommand2.remove(((Player) sender).getUniqueId().toString());
                                                    if (usingPapi) {
                                                        // check if papi is enabled - parse to check
                                                        if (papiEnabled) {
                                                            double balance;
                                                            try {
                                                                balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) sender, currency));
                                                            } catch (NumberFormatException ignored) {
                                                                Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                                return true;
                                                            }
                                                            if (balance >= permanentCost) {
                                                                nb.doRemoveCommands((Player) sender, permanentCost);
                                                                // successfully bought perm immunity
                                                                if (nb.SQL.isConnected()){
                                                                    nb.data.addData(((Player) sender).getUniqueId().toString(), 0,0,0,0,permanentCost, 0);
                                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(20), nb.data.getImmunity(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                                } else {
                                                                    nb.immunitySpent.replace(((Player) sender).getUniqueId().toString(), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()) + permanentCost);
                                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(20), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                                }
                                                                } else {
                                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), permanentCost, (Player) sender));
                                                            }
                                                        } else {
                                                            Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                                        }
                                                    } else {
                                                        if (nb.checkAmount((Player) sender, Material.valueOf(currency)) >= permanentCost) {
                                                            nb.removeItem((Player) sender, Material.valueOf(currency), permanentCost);
                                                            nb.doRemoveCommands((Player) sender, permanentCost);
                                                            // successfully bought perm immunity
                                                            if (nb.SQL.isConnected()){
                                                                nb.data.addData(((Player) sender).getUniqueId().toString(), 0,0,0,0,permanentCost, 0);
                                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(20), nb.data.getImmunity(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                            } else {
                                                                nb.immunitySpent.replace(((Player) sender).getUniqueId().toString(), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()) + permanentCost);
                                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(20), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                            }
                                                        } else {
                                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), permanentCost, (Player) sender));
                                                        }
                                                    }
                                                } else {
                                                    // ask to repeat
                                                    nb.repeatBuyCommand2.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(30), permanentCost, (Player) sender));
                                                }
                                            } else {
                                                // ask to repeat
                                                nb.repeatBuyCommand2.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(30), permanentCost, (Player) sender));
                                            }
                                        } else {
                                            // already bought immunity
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(25), (Player) sender));
                                        }
                                    } else {
                                        if (args.length > 1) {
                                            int amount;
                                            try {
                                                amount = Integer.parseInt(args[1]);
                                            } catch (NumberFormatException ignored) {
                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                                return true;
                                            }
                                            if (nb.repeatBuyCommand2.containsKey(((Player) sender).getUniqueId().toString())) {
                                                if (System.currentTimeMillis() - nb.repeatBuyCommand2.get(((Player) sender).getUniqueId().toString()) < 30000) {
                                                    // try to find bounty and buy it
                                                    nb.repeatBuyCommand2.remove(((Player) sender).getUniqueId().toString());
                                                    if (usingPapi) {
                                                        // check if papi is enabled - parse to check
                                                        if (papiEnabled) {
                                                            double balance;
                                                            try {
                                                                balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) sender, currency));
                                                            } catch (NumberFormatException ignored) {
                                                                Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                                return true;
                                                            }
                                                            if (balance >= (int) (amount * scalingRatio)) {
                                                                nb.doRemoveCommands((Player) sender, (int) (amount * scalingRatio));
                                                                // successfully bought scaling immunity - amount x scalingRatio
                                                                if (nb.SQL.isConnected()){
                                                                    nb.data.addData(((Player) sender).getUniqueId().toString(), 0,0,0,0, amount, 0);
                                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(20), nb.data.getImmunity(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                                } else {
                                                                    nb.immunitySpent.replace(((Player) sender).getUniqueId().toString(), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()) + (amount));
                                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(21), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                                }
                                                            } else {
                                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (amount * scalingRatio), (Player) sender));
                                                            }
                                                        } else {
                                                            Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                                        }
                                                    } else {
                                                        if (nb.checkAmount((Player) sender, Material.valueOf(currency)) >= amount * scalingRatio) {
                                                            nb.removeItem((Player) sender, Material.valueOf(currency), (int) (amount * scalingRatio));
                                                            nb.doRemoveCommands((Player) sender, (int) (amount * scalingRatio));
                                                            // successfully bought scaling immunity - amount x scalingRatio
                                                            if (nb.SQL.isConnected()){
                                                                nb.data.addData(((Player) sender).getUniqueId().toString(), 0,0,0,0,amount, 0);
                                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(20), nb.data.getImmunity(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                            } else {
                                                                nb.immunitySpent.replace(((Player) sender).getUniqueId().toString(), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()) + (amount));
                                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(21), nb.immunitySpent.get(((Player) sender).getUniqueId().toString()), (Player) sender));
                                                            }
                                                        } else {
                                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (amount * scalingRatio), (Player) sender));
                                                        }
                                                    }
                                                } else {
                                                    // ask to repeat
                                                    nb.repeatBuyCommand2.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(30), amount, (Player) sender));
                                                }
                                            } else {
                                                // ask to repeat
                                                nb.repeatBuyCommand2.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(30), amount, (Player) sender));
                                            }
                                        } else {
                                            // usage
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                            sender.sendMessage(ChatColor.BLUE + "/bounty immunity (price)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Buy immunity to bounties set on you under " + scalingRatio + "x the price you spend.");
                                        }
                                    }
                                } else {
                                    // no permission
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
                                }
                            } else {
                                // unknown command
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
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
                                    if (nb.repeatBuyCommand.containsKey(((Player) sender).getUniqueId().toString())) {
                                        if (System.currentTimeMillis() - nb.repeatBuyCommand.get(((Player) sender).getUniqueId().toString()) < 30000) {
                                            // try to find bounty and buy it
                                            nb.repeatBuyCommand.remove(((Player) sender).getUniqueId().toString());

                                            if (usingPapi) {
                                                // check if papi is enabled - parse to check
                                                if (papiEnabled) {
                                                    double balance;
                                                    try {
                                                        balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) sender, currency));
                                                    } catch (NumberFormatException ignored) {
                                                        Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                        return true;
                                                    }
                                                    if (balance >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                                        nb.doRemoveCommands((Player) sender, (int) (bounty.getTotalBounty() * buyBackInterest));
                                                        if (nb.SQL.isConnected()){
                                                            nb.data.removeBounty(bounty.getUUID());
                                                        } else {
                                                            nb.bountyList.remove(bounty);
                                                        }
                                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(14), sender.getName(), (Player) sender));
                                                    } else {
                                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) sender));
                                                    }
                                                } else {
                                                    Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                                }
                                            } else {
                                                if (nb.checkAmount((Player) sender, Material.valueOf(currency)) >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                                    nb.removeItem((Player) sender, Material.valueOf(currency), (int) (bounty.getTotalBounty() * buyBackInterest));
                                                    nb.doRemoveCommands((Player) sender, (int) (bounty.getTotalBounty() * buyBackInterest));
                                                    if (nb.SQL.isConnected()){
                                                        nb.data.removeBounty(bounty.getUUID());
                                                    } else {
                                                        nb.bountyList.remove(bounty);
                                                    }
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(14), sender.getName(), (Player) sender));
                                                } else {
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) sender));
                                                }
                                            }


                                        } else {
                                            // ask to repeat
                                            nb.repeatBuyCommand.replace(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(17), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) sender));
                                        }
                                    } else {
                                        // ask to repeat
                                        nb.repeatBuyCommand.put(((Player) sender).getUniqueId().toString(), System.currentTimeMillis());
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(17), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) sender));
                                    }
                                } else {
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(8), sender.getName(), (Player) sender));
                                }
                            } else {
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
                            }
                        } else {
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                        }
                    } else {
                        sender.sendMessage("You don't have a bounty!");
                    }
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (sender.hasPermission("notbounties.view")) {
                        if (args.length > 1) {
                            Player p = Bukkit.getPlayer(args[1]);
                            if (p != null) {
                                if (nb.hasBounty(p)) {
                                    Bounty bounty = nb.getBounty(p);
                                    assert bounty != null;
                                    if (sender instanceof Player) {
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(9), p.getName(), bounty.getTotalBounty(), (Player) sender));
                                        for (Setter setters : bounty.getSetters()) {
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(10), setters.getName(), setters.getAmount(), (Player) sender));
                                        }
                                    }
                                } else {
                                    if (sender instanceof Player)
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(8), p.getName(), (Player) sender));
                                }
                                return true;
                            }
                            // go through offline players
                            if (nb.loggedPlayers.containsKey(args[1].toLowerCase(Locale.ROOT))) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(nb.loggedPlayers.get(args[1].toLowerCase(Locale.ROOT))));
                                if (nb.hasBounty(player)) {
                                    Bounty bounty = nb.getBounty(player);
                                    assert bounty != null;
                                    if (sender instanceof Player) {
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(9), player.getName(), bounty.getTotalBounty(), (Player) sender));
                                        for (Setter setters : bounty.getSetters()) {
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(10), setters.getName(), setters.getAmount(), (Player) sender));
                                        }
                                    }
                                } else {
                                    if (sender instanceof Player)
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(8), player.getName(), (Player) sender));
                                }
                            } else {
                                if (sender instanceof Player)
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(3), args[1], (Player) sender));
                            }
                        } else {
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            sender.sendMessage(ChatColor.BLUE + "/bounty check (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Checks a bounty.");
                        }
                    } else {
                        if (sender instanceof Player)
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
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
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
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
                                    if (nb.SQL.isConnected()){
                                        nb.data.removeBounty(toRemove.getUUID());
                                    } else {
                                        nb.bountyList.remove(toRemove);
                                    }

                                    // successfully removed
                                    if (sender instanceof Player) {
                                        Player player = Bukkit.getPlayer(UUID.fromString(toRemove.getUUID()));
                                        if (player != null) {
                                            sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(14), toRemove.getName(), null)));
                                        } else {
                                            sender.sendMessage(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(toRemove.getUUID())), nb.parse(speakings.get(0) + speakings.get(14), toRemove.getName(), null)));
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
                                            toRemove.removeBounty(UUID.fromString(actualRemove.getUuid()));
                                            if (toRemove.getSetters().size() == 0) {
                                                if (nb.SQL.isConnected()){
                                                    nb.data.removeBounty(toRemove.getUUID());
                                                } else {
                                                    nb.bountyList.remove(toRemove);
                                                }
                                            } else if (nb.SQL.isConnected()){
                                                nb.data.removeSetter(toRemove.getUUID(), actualRemove.getUuid());
                                            }
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                                    if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                                                        nb.openGUI(player, Integer.parseInt(player.getOpenInventory().getTitle().substring(player.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                                    }
                                                }
                                            }
                                            // successfully removed
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(UUID.fromString(toRemove.getUUID()));
                                                if (player != null) {
                                                    sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(14), toRemove.getName(), null)));
                                                } else {
                                                    sender.sendMessage(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(toRemove.getUUID())), nb.parse(speakings.get(0) + speakings.get(14), toRemove.getName(), null)));
                                                }
                                            }
                                        } else {
                                            // couldnt find setter
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(UUID.fromString(toRemove.getUUID()));
                                                if (player != null) { // player then receiver
                                                    sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(16), args[3], toRemove.getName(), null)));
                                                } else {
                                                    sender.sendMessage(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(toRemove.getUUID())), nb.parse(speakings.get(0) + speakings.get(16), args[3], toRemove.getName(), null)));
                                                }
                                            }
                                        }
                                    } else {
                                        // usage
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");
                                }
                            } else {
                                // could not find bounty
                                if (sender instanceof Player) {
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(8), args[1], (Player) sender));
                                }
                            }
                        } else {
                            // usage
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes all bounties from a player.");
                            sender.sendMessage(ChatColor.BLUE + "/bounty remove (player) from (setter)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Removes a specific bounty put on a player.");

                        }
                    } else {
                        // no permission
                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
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
                                    int amount;
                                    try {
                                        amount = Integer.parseInt(args[2]);
                                    } catch (NumberFormatException ignored) {
                                        // unknown number - 2?
                                        if (sender instanceof Player) {
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                        }
                                        return true;
                                    }
                                    if (nb.SQL.isConnected()){
                                        nb.data.editBounty(toEdit.getUUID(), UUID.randomUUID().toString(), amount - toEdit.getTotalBounty());
                                    } else {
                                        toEdit.addBounty(amount - toEdit.getTotalBounty());
                                    }
                                    // successfully edited bounty
                                    if (sender instanceof Player) {
                                        Player player = Bukkit.getPlayer(UUID.fromString(toEdit.getUUID()));
                                        if (player != null) {
                                            sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(15), toEdit.getName(), null)));
                                        } else {
                                            sender.sendMessage(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(toEdit.getUUID())), nb.parse(speakings.get(0) + speakings.get(15), toEdit.getName(), null)));
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
                                            int amount;
                                            try {
                                                amount = Integer.parseInt(args[4]);
                                            } catch (NumberFormatException ignored) {
                                                // unknown number - 2?
                                                if (sender instanceof Player) {
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                                }
                                                return true;
                                            }
                                            if (nb.SQL.isConnected()){
                                                nb.data.editBounty(toEdit.getUUID(), actualEdit.getUuid(), amount - toEdit.getTotalBounty());
                                            } else {
                                                toEdit.editBounty(UUID.fromString(actualEdit.getUuid()), amount);
                                            }

                                            // successfully edited bounty
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(UUID.fromString(toEdit.getUUID()));
                                                if (player != null) {
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(15), toEdit.getName(), player));
                                                } else {
                                                    sender.sendMessage(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(toEdit.getUUID())), nb.parse(speakings.get(0) + speakings.get(15), toEdit.getName(), null)));
                                                }
                                            }
                                        } else {
                                            // couldnt find setter
                                            if (sender instanceof Player) {
                                                Player player = Bukkit.getPlayer(UUID.fromString(toEdit.getUUID()));
                                                if (player != null) { // player then receiver
                                                    sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(16), args[3], toEdit.getName(), null)));
                                                } else {
                                                    sender.sendMessage(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(toEdit.getUUID())), nb.parse(speakings.get(0) + speakings.get(16), args[3], toEdit.getName(), null)));
                                                }
                                            }
                                        }
                                    } else {
                                        // usage
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                        sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                    }
                                } else {
                                    // usage
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                                }
                            } else {
                                // couldn't fimd bounty
                                if (sender instanceof Player) {
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(8), args[1], (Player) sender));
                                }
                            }
                        } else {
                            // usage
                            if (sender instanceof Player) {
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a player's total bounty.");
                                sender.sendMessage(ChatColor.BLUE + "/bounty edit (player) from (setter) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Edits a specific bounty put on a player.");
                            }
                        }
                    } else {
                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                } else if (args[0].equalsIgnoreCase("tracker")) {
                    // give a tracker that points toward a certain player with a bounty
                    if (tracker)
                        if (sender.hasPermission("notbounties.admin")) {
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
                                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(tracking.getUUID()));
                                    ItemStack compass = new ItemStack(Material.COMPASS, 1);
                                    ItemMeta meta = compass.getItemMeta();
                                    assert meta != null;
                                    meta.setDisplayName(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(32), player.getName(), null)));
                                    ArrayList<String> lore = new ArrayList<>();
                                    for (String str : trackerLore) {
                                        lore.add(PlaceholderAPI.setPlaceholders(player, nb.parse(str, player.getName(), null)));
                                    }
                                    int i = 0;
                                    if (nb.trackedBounties.containsValue(tracking.getUUID())) {
                                        // get already used number
                                        for (Map.Entry<Integer, String> entry : nb.trackedBounties.entrySet()) {
                                            if (entry.getValue().equals(tracking.getUUID())) {
                                                i = entry.getKey();
                                                break;
                                            }
                                        }
                                    } else {
                                        // get new number
                                        while (nb.trackedBounties.containsKey(i)) {
                                            i++;
                                        }
                                        nb.trackedBounties.put(i, tracking.getUUID());
                                    }

                                    lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + "" + ChatColor.UNDERLINE + "" + ChatColor.ITALIC + "@" + i);
                                    meta.setLore(lore);
                                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                    compass.setItemMeta(meta);
                                    compass.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                                    if (args.length > 2) {
                                        // find player to give to
                                        Player receiver = Bukkit.getPlayer(args[2]);
                                        if (receiver != null) {
                                            nb.givePlayer(receiver, compass);
                                            // you have been given & you have received
                                            if (sender instanceof Player) {
                                                sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(33), receiver.getName(), player.getName(), null)));
                                            }
                                            receiver.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(34), player.getName(), null)));
                                        } else {
                                            if (sender instanceof Player) {
                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(3), args[2], (Player) sender));
                                            }
                                        }
                                    } else {
                                        if (sender instanceof Player) {
                                            nb.givePlayer((Player) sender, compass);
                                            // you have been given
                                            sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(34), player.getName(), null)));
                                        } else {
                                            sender.sendMessage("You are not a player!");
                                        }
                                    }
                                } else {
                                    // couldn't find bounty
                                    if (sender instanceof Player) {
                                        sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(8), args[1], (Player) sender));
                                    }
                                }
                            } else {
                                // usage
                                if (sender instanceof Player) {
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                                    sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives you a compass that tracks a player with a bounty.");
                                    sender.sendMessage(ChatColor.BLUE + "/bounty tracker (player) (receiver)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Gives receiver a compass that tracks a player with a bounty.");
                                }
                            }
                        } else {
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
                        }
                } else {
                    if (sender.hasPermission("notbounties.set")) {
                        if (args.length > 1) {
                            // check if we can get the amount
                            // put in
                            double amount;
                            try {
                                amount = Double.parseDouble(args[1]);
                            } catch (NumberFormatException ignored) {
                                if (sender instanceof Player)
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(1), (Player) sender));
                                return true;
                            }
                            if (amount < minBounty) {
                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(23), minBounty, (Player) sender));
                                return true;
                            }

                            // check if it is a player
                            Player p = Bukkit.getPlayer(args[0]);
                            if (p != null) {
                                if (sender instanceof Player) {
                                    if (nb.gracePeriod.containsKey(p.getUniqueId().toString())) {
                                        long timeSinceDeath = System.currentTimeMillis() - nb.gracePeriod.get(p.getUniqueId().toString());
                                        if (timeSinceDeath < graceTime * 1000L) {
                                            // still in grace period
                                            long timeLeft = (graceTime * 1000L) - timeSinceDeath;
                                            long hours = timeLeft / 3600000L;
                                            timeLeft = timeLeft % 3600000L;
                                            long minutes = timeLeft / 60000L;
                                            timeLeft = timeLeft % 60000L;
                                            long seconds = timeLeft / 1000L;
                                            String time = "";
                                            if (hours > 0) {
                                                time += hours + "h ";
                                            }
                                            if (minutes > 0) {
                                                time += minutes + "m ";
                                            }
                                            if (seconds > 0) {
                                                time += seconds + "s ";
                                            }
                                            String message = nb.parse(speakings.get(0) + speakings.get(22), p.getName(), p);
                                            while (message.contains("{time}")) {
                                                message = message.replace("{time}", time);
                                            }
                                            sender.sendMessage(message);
                                            return true;
                                        } else {
                                            nb.gracePeriod.remove(p.getUniqueId().toString());
                                        }
                                    }
                                    int immunitySpent = nb.SQL.isConnected() ? nb.data.getImmunity(p.getUniqueId().toString()) : nb.immunitySpent.get(p.getUniqueId().toString());
                                    if (!p.hasPermission("notbounties.immune") && ((amount > immunitySpent && !permanentImmunity) || (permanentImmunity && immunitySpent < permanentCost))) {
                                        if (usingPapi) {
                                            // check if papi is enabled - parse to check
                                            if (papiEnabled) {
                                                double balance;
                                                try {
                                                    balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) sender, currency));
                                                } catch (NumberFormatException ignored) {
                                                    Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                    return true;
                                                }
                                                if (balance >= (int) (amount + (amount * bountyTax))) {
                                                    nb.doRemoveCommands((Player) sender, (int) (amount + (amount * bountyTax)));
                                                    nb.addBounty((Player) sender, p, (int) amount);
                                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                                        if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                                            if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                                                                nb.openGUI(player, Integer.parseInt(player.getOpenInventory().getTitle().substring(player.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (amount + (amount * bountyTax)), (Player) sender));
                                                }
                                            } else {
                                                Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                            }

                                        } else {

                                            if (nb.checkAmount((Player) sender, Material.valueOf(currency)) >= (int) (amount + (amount * bountyTax))) {
                                                nb.removeItem((Player) sender, Material.valueOf(currency), (int) (amount + (amount * bountyTax)));
                                                nb.addBounty((Player) sender, p, (int) amount);
                                                nb.doRemoveCommands((Player) sender, (int) (amount + (amount * bountyTax)));
                                                for (Player player : Bukkit.getOnlinePlayers()) {
                                                    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                                        if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                                                            nb.openGUI(player, Integer.parseInt(player.getOpenInventory().getTitle().substring(player.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                                        }
                                                    }
                                                }
                                            } else {
                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (amount + (amount * bountyTax)), (Player) sender));
                                            }

                                        }
                                    } else {
                                        // has immunity
                                        if (permanentImmunity || p.hasPermission("notbounties.immune")) {
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(18), p.getName(), immunitySpent, p));
                                        } else {
                                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(19), p.getName(), immunitySpent, p));
                                        }
                                    }
                                } else {
                                    nb.addBounty(p, (int) amount);
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                            if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                                                nb.openGUI(player, Integer.parseInt(player.getOpenInventory().getTitle().substring(player.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                            }
                                        }
                                    }
                                }
                                return true;
                            }
                            // go through offline players
                            if (nb.loggedPlayers.containsKey(args[0].toLowerCase(Locale.ROOT))) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(nb.loggedPlayers.get(args[0].toLowerCase(Locale.ROOT))));
                                if (sender instanceof Player) {
                                    int immunitySpent = nb.SQL.isConnected() ? nb.data.getImmunity(player.getUniqueId().toString()) : nb.immunitySpent.get(player.getUniqueId().toString());
                                    if (!nb.immunePerms.contains(player.getUniqueId().toString()) && ((amount > immunitySpent && !permanentImmunity) || (permanentImmunity && immunitySpent < permanentCost))) {
                                        if (usingPapi) {
                                            // check if papi is enabled - parse - then remove w/ commands
                                            if (papiEnabled) {
                                                double balance;
                                                try {
                                                    balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) sender, currency));
                                                } catch (NumberFormatException ignored) {
                                                    Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                    return true;
                                                }
                                                if (balance >= (int) (amount + (amount * bountyTax))) {
                                                    nb.doRemoveCommands((Player) sender, (int) (amount + (amount * bountyTax)));
                                                    nb.addBounty((Player) sender, player, (int) amount);
                                                    for (Player player1 : Bukkit.getOnlinePlayers()) {
                                                        if (player1.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                                            if (player1.getOpenInventory().getTitle().contains(speakings.get(35))) {
                                                                nb.openGUI(player1, Integer.parseInt(player1.getOpenInventory().getTitle().substring(player1.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (amount + (amount *bountyTax)), (Player) sender));
                                                }
                                            } else {
                                                Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                            }
                                        } else {
                                            if (nb.checkAmount((Player) sender, Material.valueOf(currency)) >= (int) (amount + (amount * bountyTax))) {
                                                nb.removeItem((Player) sender, Material.valueOf(currency), (int) (amount + (amount * bountyTax)));
                                                nb.addBounty((Player) sender, player, (int) amount);
                                                for (Player player1 : Bukkit.getOnlinePlayers()) {
                                                    if (player1.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                                        if (player1.getOpenInventory().getTitle().contains(speakings.get(35))) {
                                                            nb.openGUI(player1, Integer.parseInt(player1.getOpenInventory().getTitle().substring(player1.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                                        }
                                                    }
                                                }
                                                nb.doRemoveCommands((Player) sender, (int) (amount + (amount * bountyTax)));
                                            } else {
                                                sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(6), (int) (amount + (amount * bountyTax)), (Player) sender));
                                            }
                                        }
                                    } else {
                                        // has immunity
                                        if (permanentImmunity || nb.immunePerms.contains(player.getUniqueId().toString())) {
                                            sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(18), player.getName(), immunitySpent, null)));
                                        } else {
                                            sender.sendMessage(PlaceholderAPI.setPlaceholders(player, nb.parse(speakings.get(0) + speakings.get(19), player.getName(), immunitySpent, null)));
                                        }
                                    }
                                } else {
                                    nb.addBounty(player, (int) amount);
                                    for (Player player1 : Bukkit.getOnlinePlayers()) {
                                        if (player1.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                            if (player1.getOpenInventory().getTitle().contains(speakings.get(35))) {
                                                nb.openGUI(player1, Integer.parseInt(player1.getOpenInventory().getTitle().substring(player1.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (sender instanceof Player)
                                    sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(3), args[0], (Player) sender));
                            }
                        } else {
                            //incorrect usage
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(24), (Player) sender));
                            sender.sendMessage(ChatColor.BLUE + "/bounty (player) (amount)" + ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "Adds a bounty to a player.");
                        }
                    } else {
                        if (sender instanceof Player)
                            sender.sendMessage(nb.parse(speakings.get(0) + speakings.get(5), (Player) sender));
                    }
                }
            } else {
                // open gui
                nb.openGUI((Player) sender, 0);
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
                    if (tracker)
                        tab.add("tracker");
                }
                if (buyBack) {
                    if (sender.hasPermission("notbounties.buyown")) {
                        tab.add("buy");
                    }
                }
                if (buyImmunity || sender.hasPermission("notbounties.admin")) {
                    if (sender.hasPermission("notbounties.buyimmunity")) {
                        tab.add("immunity");
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("check") && sender.hasPermission("notbounties.view")) {
                    List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit") || (args[0].equalsIgnoreCase("tracker")) && tracker) && sender.hasPermission("notbounties.admin")) {
                    List<Bounty> bountyList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.bountyList;
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if (args[0].equalsIgnoreCase("immunity") && (sender.hasPermission("notbounties.admin") || sender.hasPermission("notbounties.removeimmunity"))) {
                    tab.add("remove");
                } else if ((args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("stat")) && sender.hasPermission("notbounties.view")) {
                    tab.add("all");
                    tab.add("kills");
                    tab.add("claimed");
                    tab.add("deaths");
                    tab.add("set");
                    tab.add("immunity");
                }
            } else if (args.length == 3) {
                if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit")) && sender.hasPermission("notbounties.admin")) {
                    tab.add("from");
                } else if (args[0].equalsIgnoreCase("immunity") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.admin")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        tab.add(player.getName());
                    }
                } else if (args[0].equalsIgnoreCase("tracker") && tracker) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        tab.add(player.getName());
                    }
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
            String typed = args[args.length - 1];
            tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            if (tab.isEmpty()) {
                if (args.length == 1) {
                    if (sender.hasPermission("notbounties.set")) {
                        for (Map.Entry<String, String> entry : nb.loggedPlayers.entrySet()) {
                            tab.add(Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue())).getName());
                        }
                    }
                } else if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("immunity") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.admin")) {
                        for (Map.Entry<String, String> entry : nb.loggedPlayers.entrySet()) {
                            tab.add(Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue())).getName());
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

    // function to sort hashmap by values
    public static HashMap<String, Integer> sortByValue(Map<String, Integer> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        // put data from sorted list to hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public String parseBountyTopString(int rank, String playerName, int amount, String prefix, String suffix, OfflinePlayer player){
        String text = speakings.get(36);
        while (text.contains("{rank}")) {
            text = text.replace("{rank}", rank + "");
        }
        while (text.contains("{player}")) {
            text = text.replace("{player}", Objects.requireNonNull(playerName));
        }
        while (text.contains("{amount}")) {
            text = text.replace("{amount}", prefix + amount + suffix);
        }
        if (papiEnabled && player != null) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    public String parseStats(String text, int amount, boolean useCurrency, OfflinePlayer player){
        while (text.contains("{amount}")) {
            text = useCurrency ? text.replace("{amount}", currencyPrefix + amount + currencySuffix) : text.replace("{amount}", amount + "");
        }
        if (papiEnabled && player != null) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

}

package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.bountyEvents.BountyEditEvent;
import me.jadenp.notbounties.bountyEvents.BountyRemoveEvent;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.ui.map.BountyMap;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.challenges.ChallengeType;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.externalAPIs.LiteBansClass;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static me.jadenp.notbounties.ui.gui.GUI.openGUI;
import static me.jadenp.notbounties.ui.gui.GUI.reopenBountiesGUI;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.*;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class Commands implements CommandExecutor, TabCompleter {

    private static final Map<UUID, Long> giveOwnCooldown = new HashMap<>();
    private static final long cooldownTime = 3000;
    private static final String[] allowedPausedAliases = new String[]{"check", "help", "cleanEntities", "unpause", "pause", "debug", "top", "reload"};

    public Commands() {}

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("notbounties")) {
            boolean commandResult = executeCommand(sender, args);
            if (sender instanceof Player)
                if (commandResult) {
                    Prompt.successfulExecute(((Player) sender).getUniqueId());
                } else {
                    Prompt.failExecute(((Player) sender).getUniqueId());
                }
        }
        return true;
    }

    public boolean executeCommand(@NotNull CommandSender sender, String[] args) {
        if (NotBounties.isPaused()) {
            if (args.length > 0) {
                boolean allow = false;
                for (String str : allowedPausedAliases) {
                    if (str.equalsIgnoreCase(args[0])) {
                        allow = true;
                        break;
                    }
                }
                if (!allow) {
                    if (sender.hasPermission(NotBounties.getAdminPermission()))
                        sender.sendMessage(parse(prefix + paused, null));
                    return true;
                }
            }
        }
        boolean silent = false;
        for (int i = 0; i < args.length; i++) {
            // check if the argument has the -s
            if (args[i].equalsIgnoreCase("-s")) {
                silent = true;
            }
            // move everything backwards if a -s was found
            if (silent && i < args.length - 1) {
                args[i] = args[i + 1];
            }
        }
        if (silent) {
            // remove the last argument
            String[] tempArgs = args;
            args = new String[tempArgs.length - 1];
            System.arraycopy(tempArgs, 0, args, 0, tempArgs.length - 1);
        }
        Player parser = getParser(sender);
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help") && sender.hasPermission("notbounties.basic")) {
                int page;
                if (args.length == 1) {
                    page = 1;
                } else {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (IllegalArgumentException e) {
                        page = 1;
                    }
                }
                LanguageOptions.sendHelpMessage(sender, page);
            } else if (args[0].equalsIgnoreCase("challenges") && ChallengeManager.isEnabled()) {
                if (sender.hasPermission("notbounties.challenges") && sender instanceof Player) {
                    if (args.length == 1) {
                        // open GUI
                        openGUI(parser, "challenges", 1);
                            return true;
                    } else {
                        if (args[1].equalsIgnoreCase("claim")) {
                            if (args.length == 2) {
                                // iterate through all options to see if they can claim one
                                for (int i = 0; i < ChallengeManager.getConcurrentChallenges(); i++) {
                                    if (i == ChallengeManager.getConcurrentChallenges() - 1 || ChallengeManager.canClaim(parser, i)) {
                                        ChallengeManager.tryClaim(parser, i);
                                    }
                                }
                            } else {
                                // grab specified challenge
                                try {
                                    int challengeIndex = (int) (NumberFormatting.tryParse(args[2]) - 1);
                                    ChallengeManager.tryClaim(parser, challengeIndex);
                                    return true;
                                } catch (NumberFormatException e) {
                                    // unknown command
                                    if (!silent) {
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                        sender.sendMessage(parse(prefix + helpChallenges, parser));
                                    }
                                    return false;
                                }
                            }
                        } else if (args[1].equalsIgnoreCase("check")) {
                            if (args.length == 2) {
                                // send all challenges
                                sender.sendMessage("");
                                for (int i = 0; i < ChallengeManager.getConcurrentChallenges(); i++) {
                                    for (String text : ChallengeManager.getDisplayText(parser, i)) {
                                        sender.sendMessage(text);
                                    }
                                    sender.sendMessage("");
                                }
                            } else {
                                // get specific challenge
                                try {
                                    int challengeIndex = (int) (NumberFormatting.tryParse(args[2]) - 1);
                                    sender.sendMessage("");
                                    for (String text : ChallengeManager.getDisplayText(parser, challengeIndex)) {
                                        sender.sendMessage(text);
                                    }
                                    sender.sendMessage("");
                                    return true;
                                } catch (NumberFormatException e) {
                                    // unknown command
                                    if (!silent) {
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                        sender.sendMessage(parse(prefix + helpChallenges, parser));
                                    }
                                    return false;
                                }
                            }
                        } else {
                            // unknown command
                            if (!silent) {
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sender.sendMessage(parse(prefix + helpChallenges, parser));
                            }
                            return false;
                        }
                    }
                } else {
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                }
                return false;
            } else if (args[0].equalsIgnoreCase("update-notification")) {
                if (!sender.hasPermission(NotBounties.getAdminPermission())) {
                    if (!silent)
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                    return false;
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
                    if (!silent)
                        sender.sendMessage(parse(prefix + ChatColor.YELLOW + "The update notification is now " + ChatColor.GREEN + "enabled" + ChatColor.YELLOW + ".", parser));
                } else {
                    if (!silent)
                        sender.sendMessage(parse(prefix + ChatColor.YELLOW + "The update notification is now " + ChatColor.RED + "disabled" + ChatColor.YELLOW + ".", parser));
                }
            } else if (args[0].equalsIgnoreCase("cleanEntities")) {
                if (!sender.hasPermission(NotBounties.getAdminPermission())) {
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
                if (!(sender instanceof Player)) {
                    if (!silent)
                        sender.sendMessage("Only players can use this command!");
                    return false;
                }
                double radius;
                if (args.length == 1) {
                    radius = 10;
                } else {
                    try {
                        radius = Double.parseDouble(args[1]);
                    } catch (NumberFormatException e) {
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownNumber, parser));
                        return false;
                    }
                }
                RemovePersistentEntitiesEvent.cleanAsync(parser.getNearbyEntities(radius, radius, radius), sender);
                return true;
            } else if (args[0].equalsIgnoreCase("pause") && sender.hasPermission(NotBounties.getAdminPermission())) {
                if (NotBounties.isPaused()) {
                    sender.sendMessage(LanguageOptions.parse(prefix + ChatColor.RED + "NotBounties is already paused.", parser));
                    return false;
                } else {
                    NotBounties.setPaused(true);
                    sender.sendMessage(parse(prefix + ChatColor.RED + "NotBounties is now paused. Players will only be able to view previous bounties. Run " + ChatColor.GREEN + "/" + pluginBountyCommands.get(0) + " unpause " + ChatColor.RED + " to use all features again.", parser));
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("unpause") && sender.hasPermission(NotBounties.getAdminPermission())) {
                if (NotBounties.isPaused()) {
                    NotBounties.setPaused(false);
                    sender.sendMessage(parse(prefix + ChatColor.GREEN + "NotBounties is no longer paused.", parser));
                    return true;
                } else {
                    sender.sendMessage(parse(prefix + ChatColor.GREEN + "NotBounties is already unpaused.", parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission(NotBounties.getAdminPermission())) {
                if (args.length == 1) {
                    try {
                        NotBounties.getInstance().sendDebug(sender);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    boolean newValue;
                    newValue = args[1].equalsIgnoreCase("enable");
                    if (newValue != debug) {
                        debug = newValue;
                        if (debug)
                            if (!silent)
                                sender.sendMessage(parse(prefix + ChatColor.GREEN + "Debug messages will now be sent in console.", parser));
                            else if (!silent)
                                sender.sendMessage(parse(prefix + ChatColor.RED + "Debug messages will no longer be sent in console.", parser));
                    } else {
                        if (!silent)
                            sender.sendMessage(parse(prefix + ChatColor.YELLOW + "Debug mode is already set this way.", parser));
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("board") && sender.hasPermission(NotBounties.getAdminPermission())) {
                if (!(sender instanceof Player)) {
                    if (!silent)
                        sender.sendMessage("Only players can use this command!");
                    return false;
                }
                int rank = 0;
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("clear")) {
                        if (!silent)
                            sender.sendMessage(parse(prefix + ChatColor.DARK_RED + "Removed " + bountyBoards.size() + " bounty boards.", parser));
                        removeBountyBoard();

                        return true;
                    }
                    if (args[1].equalsIgnoreCase("remove")) {
                        if (!silent)
                            sender.sendMessage(parse(prefix + ChatColor.RED + "Right click the bounty board to remove.", parser));
                        NotBounties.boardSetup.put(parser.getUniqueId(), -1);
                        return true;
                    }
                    try {
                        rank = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        if (!silent)
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
                if (!silent)
                    sender.sendMessage(parse(prefix + ChatColor.DARK_AQUA + ChatColor.BOLD + "<Rank " + rank + "> " + ChatColor.AQUA + "Punch a block to place the bounty board on.", parser));
                NotBounties.boardSetup.put(parser.getUniqueId(), rank);
                return true;
            } else if (args[0].equalsIgnoreCase("tutorial") && sender.hasPermission("notbounties.basic")) {
                Tutorial.onCommand(sender, args);
                return true;
            } else if (args[0].equalsIgnoreCase("whitelist") && bountyWhitelistEnabled) {
                if (sender instanceof Player) {
                    if (!sender.hasPermission("notbounties.whitelist")) {
                        if (!silent)
                            sender.sendMessage(parse(prefix + noPermission, parser));
                        return false;
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
                        if (!silent)
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
                        if (!silent)
                            sender.sendMessage(parse(prefix + whitelistedPlayers + names, parser));
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("toggle")) {
                        if (args.length == 2 && enableBlacklist) {
                            // toggle
                            if (getPlayerWhitelist(((Player) sender).getUniqueId()).toggleBlacklist()) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + blacklistToggle, parser));
                            } else {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + whitelistToggle, parser));
                            }
                        } else {
                            List<String> toggleOptions = List.of("blacklist", "false", "off", "whitelist", "true", "on");
                            if (toggleOptions.contains(args[2].toLowerCase())) {
                                // set to specific type
                                boolean blacklist =
                                        args[2].equalsIgnoreCase("blacklist")
                                                || args[2].equalsIgnoreCase("false")
                                                || args[2].equalsIgnoreCase("off");
                                boolean change = getPlayerWhitelist(((Player) sender).getUniqueId()).setBlacklist(blacklist);
                                // command is silent if there is no change
                                if (change)
                                    if (blacklist) {
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + blacklistToggle, parser));
                                    } else {
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + whitelistToggle, parser));
                                    }
                            } else {
                                // try to find player
                                OfflinePlayer player = NotBounties.getPlayer(args[2]);
                                if (player == null) {
                                    // unknown player
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + unknownPlayer, args[2], parser));
                                    return false;
                                }
                                Whitelist whitelist = getPlayerWhitelist(((Player) sender).getUniqueId());
                                if (whitelist.getList().remove(player.getUniqueId())) {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + whitelistChange, parser));
                                } else if (whitelist.getList().size() < 10) {
                                    whitelist.getList().add(player.getUniqueId());
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + whitelistChange, parser));
                                } else {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + whitelistMax, parser));
                                }
                            }
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
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownPlayer, args[i], parser));
                                return false;
                            }
                            if (!whitelist.contains(player.getUniqueId()))
                                whitelist.add(player.getUniqueId());
                        }

                        if (args[1].equalsIgnoreCase("add")) {
                            whitelist.stream().filter(uuid -> !previousWhitelist.contains(uuid)).forEach(previousWhitelist::add);
                            while (previousWhitelist.size() > 10)
                                previousWhitelist.remove(10);
                            getPlayerWhitelist(parser.getUniqueId()).setList(previousWhitelist);
                            if (!silent)
                                sender.sendMessage(parse(prefix + whitelistChange, parser));
                            if (previousWhitelist.size() == 10)
                                if (!silent)
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
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownPlayer, builder.toString(), parser));
                                return true;
                            }
                            getPlayerWhitelist(parser.getUniqueId()).setList(previousWhitelist);
                            if (!silent)
                                sender.sendMessage(parse(prefix + whitelistChange, parser));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("set")) {
                            getPlayerWhitelist(parser.getUniqueId()).setList(whitelist);
                            if (!silent)
                                sender.sendMessage(parse(prefix + whitelistChange, parser));
                            return true;
                        }
                    }
                    // usage
                    if (!silent) {
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sendHelpMessage(sender, helpWhitelist);
                    }
                    return false;
                } else {
                    if (!silent)
                        sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command.");
                }
                return false;
            } else if (args[0].equalsIgnoreCase("currency")) {
                if (!sender.hasPermission(NotBounties.getAdminPermission())) {
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
                CurrencySetup.onCommand(sender, args);
                return true;
            } else if (args[0].equalsIgnoreCase("set")) {
                if (sender.hasPermission("notbounties.set")) {
                    if (sender instanceof Player) {
                        if (args.length > 1)
                            openGUI(parser, "set-bounty", 1, args[1]);
                        else
                            openGUI(parser, "set-bounty", 1);
                    } else {
                        if (!silent)
                            sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command.");
                        return false;
                    }
                    return true;
                } else {
                    // no permission
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("bdc") && sender.hasPermission("notbounties.basic")) {
                if (sender instanceof Player) {
                    if (args.length > 1) {
                        boolean broadcastMode = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("enable");
                        if (broadcastMode) {
                            // enable
                            NotBounties.disableBroadcast.remove((parser).getUniqueId());
                            if (!silent)
                                sender.sendMessage(parse(prefix + enableBroadcast, parser));
                        } else {
                            // disable
                            if (!NotBounties.disableBroadcast.contains((parser).getUniqueId()))
                                NotBounties.disableBroadcast.add(parser.getUniqueId());
                            if (!silent)
                                sender.sendMessage(parse(prefix + LanguageOptions.disableBroadcast, parser));
                        }
                    } else {
                        if (NotBounties.disableBroadcast.contains((parser).getUniqueId())) {
                            // enable
                            NotBounties.disableBroadcast.remove((parser).getUniqueId());
                            if (!silent)
                                sender.sendMessage(parse(prefix + enableBroadcast, parser));
                        } else {
                            // disable
                            NotBounties.disableBroadcast.add((parser).getUniqueId());
                            if (!silent)
                                sender.sendMessage(parse(prefix + LanguageOptions.disableBroadcast, parser));
                        }
                    }
                } else {
                    if (!silent)
                        sender.sendMessage("You can't disable bounty broadcast! (yet?) If you really want to disable, send a message in the discord!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("stat")) {
                if (!sender.hasPermission("notbounties.view")) {
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
                if (args.length == 1) {
                    Leaderboard.ALL.displayStats(parser, false);
                    return true;
                }
                if (args.length > 3 && !sender.hasPermission(NotBounties.getAdminPermission()) || args.length > 5) {
                    // usage
                    if (!silent)
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                    sendHelpMessage(sender, helpView);
                    return false;
                }
                Leaderboard leaderboard;
                try {
                    leaderboard = Leaderboard.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    // more usage
                    if (!silent) {
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sendHelpMessage(sender, helpView);
                    }
                    return false;
                }

                if (args.length == 2) {
                    if (!(sender instanceof Player)) {
                        if (!silent)
                            sender.sendMessage("Only players can use this command!");
                        return false;
                    }
                    leaderboard.displayStats(parser, false);
                } else {
                    OfflinePlayer player = getPlayer(args[2]);
                    if (player == null) {
                        // unknown player
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownPlayer, args[2], parser));
                        return false;
                    }
                    if (args.length == 3) {
                        leaderboard.displayStats(player, parser, true);
                        return true;
                    }
                    // admin part to edit or setValue
                    if (!sender.hasPermission(NotBounties.getAdminPermission())) {
                        if (!silent)
                            sender.sendMessage(parse(prefix + noPermission, parser));
                        return false;
                    }
                    if (args.length != 5 || (!args[3].equalsIgnoreCase("edit") && !args[3].equalsIgnoreCase("setValue"))) {
                        // usage
                        if (!silent) {
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sendHelpMessage(sender, helpAdmin);
                        }
                        return false;
                    }
                    // true = edit, false = setValue
                    boolean edit = args[3].equalsIgnoreCase("edit");
                    // value change or new value
                    double value;
                    try {
                        value = NumberFormatting.tryParse(args[4]);
                    } catch (NumberFormatException e) {
                        // usage
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sendHelpMessage(sender, helpAdmin);
                        return false;
                    }
                    if (!leaderboard.isMoney())
                        value = (long) value;
                    if (!edit)
                        value = value - leaderboard.getStat(player.getUniqueId());
                    // value is now the to-be-updated value
                    DataManager.changeStat(player.getUniqueId(), leaderboard, value, true);
                    if (!silent)
                        sender.sendMessage(parse(prefix + updateStat.replace("{leaderboard}", (leaderboard.toString())), getPlayerName(player.getUniqueId()), leaderboard.getStat(player.getUniqueId()), parser));
                }

                return true;
            } else if (args[0].equalsIgnoreCase("top")) {
                if (sender.hasPermission("notbounties.view")) {
                    Leaderboard leaderboard;
                    if (args.length > 1) {
                        try {
                            leaderboard = Leaderboard.valueOf(args[1].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sendHelpMessage(sender, helpView);
                            return false;
                        }
                    } else {
                        leaderboard = Leaderboard.ALL;
                    }
                    if (args.length > 2 && args[2].equalsIgnoreCase("list") || !(sender instanceof Player)) {
                        leaderboard.displayTopStat(sender, 10);
                    } else {
                        openGUI(parser, "leaderboard", 1, leaderboard);
                    }
                } else {
                    // no permission
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission(NotBounties.getAdminPermission())) {
                    try {
                        NotBounties.getInstance().loadConfig();
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(e.toString());
                    }
                    reopenBountiesGUI();
                    if (!silent)
                        sender.sendMessage(parse(prefix, parser) + ChatColor.GREEN + "Reloaded NotBounties version " + NotBounties.getInstance().getDescription().getVersion());
                } else {
                    if (!silent)
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                    return false;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("immunity")) {
                if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
                    if (args.length == 2) {
                        // reg command
                        if (sender.hasPermission("notbounties.removeimmunity") || sender.hasPermission(NotBounties.getAdminPermission())) {
                            if (sender instanceof Player) {
                                // remove immunity
                                if (Immunity.removeImmunity(parser.getUniqueId())) {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + removedImmunity, sender.getName(), (parser)));
                                    return true;
                                } else {
                                    // doesn't have immunity
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + noImmunity, sender.getName(), (parser)));
                                    return false;
                                }
                            } else {
                                Bukkit.getLogger().info("You don't have immunity!");
                                return false;
                            }
                        } else {
                            // usage
                            if (sender instanceof Player)
                                if (sender.hasPermission("notbounties.buyimmunity") && Immunity.immunityType != Immunity.ImmunityType.DISABLE) {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    switch (Immunity.immunityType) {
                                        case PERMANENT:
                                            sendHelpMessage(sender, helpBuyImmunityPermanent);
                                            break;
                                        case SCALING:
                                            sendHelpMessage(sender, helpBuyImmunityScaling);
                                            break;
                                        case TIME:
                                            sendHelpMessage(sender, helpBuyImmunityTime);
                                            break;
                                    }
                                } else {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                }
                            return false;
                        }
                    } else if (args.length == 3) {
                        //admin command
                        if (sender.hasPermission(NotBounties.getAdminPermission())) {
                            OfflinePlayer p = getPlayer(args[2]);
                            if (p == null) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownPlayer, args[0], parser));
                                return false;
                            }
                            if (Immunity.removeImmunity(p.getUniqueId())) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + removedOtherImmunity, p.getName(), p));
                                return true;
                            } else {
                                // doesn't have immunity
                                if (!silent)
                                    sender.sendMessage(parse(prefix + noImmunityOther, p.getName(), p));
                                return false;
                            }
                        } else {
                            if (sender.hasPermission("notbounties.removeimmunity")) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sendHelpMessage(sender, helpRemoveImmunity);
                            } else if (sender.hasPermission("notbounties.buyimmunity") && Immunity.immunityType != Immunity.ImmunityType.DISABLE) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                switch (Immunity.immunityType) {
                                    case PERMANENT:
                                        sendHelpMessage(sender, helpBuyImmunityPermanent);
                                        break;
                                    case SCALING:
                                        sendHelpMessage(sender, helpBuyImmunityScaling);
                                        break;
                                    case TIME:
                                        sendHelpMessage(sender, helpBuyImmunityTime);
                                        break;
                                }
                            } else {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                            }
                            return false;
                        }
                    } else {
                        // usage
                        if (sender.hasPermission(NotBounties.getAdminPermission())) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sendHelpMessage(sender, helpAdmin);
                        } else if (sender.hasPermission("notbounties.removeimmunity")) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sendHelpMessage(sender, helpRemoveImmunity);
                        } else if (sender.hasPermission("notbounties.buyimmunity") && Immunity.immunityType != Immunity.ImmunityType.DISABLE) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            switch (Immunity.immunityType) {
                                case PERMANENT:
                                    sendHelpMessage(sender, helpBuyImmunityPermanent);
                                    break;
                                case SCALING:
                                    sendHelpMessage(sender, helpBuyImmunityScaling);
                                    break;
                                case TIME:
                                    sendHelpMessage(sender, helpBuyImmunityTime);
                                    break;
                            }
                        } else {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                        }
                        return false;
                    }
                } else {
                    if (sender instanceof Player) {
                        if (Immunity.immunityType != Immunity.ImmunityType.DISABLE) {
                            if (sender.hasPermission("notbounties.buyimmunity")) {
                                if (Immunity.immunityType == Immunity.ImmunityType.PERMANENT) {
                                    double immunitySpent = Immunity.getImmunity(Objects.requireNonNull(parser).getUniqueId());
                                    if (immunitySpent < Immunity.getPermanentCost() && !sender.hasPermission("notbounties.immune")) {
                                        if ((repeatBuyCommand2.containsKey((parser).getUniqueId().toString()) && System.currentTimeMillis() - repeatBuyCommand2.get((parser).getUniqueId().toString()) < 30000) || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"))) {
                                            // try to find bounty and buy it
                                            repeatBuyCommand2.remove((parser).getUniqueId().toString());
                                            if (checkBalance(parser, Immunity.getPermanentCost())) {
                                                NumberFormatting.doRemoveCommands(parser, Immunity.getPermanentCost(), new ArrayList<>());
                                                // successfully bought perm immunity
                                                ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.PURCHASE_IMMUNITY, Immunity.getPermanentCost());
                                                DataManager.changeStat((parser).getUniqueId(), Leaderboard.IMMUNITY, Immunity.getPermanentCost(), false);
                                                if (!silent)
                                                    sender.sendMessage(parse(prefix + buyPermanentImmunity, Leaderboard.IMMUNITY.getStat((parser).getUniqueId()), parser));
                                                return true;
                                            } else {
                                                if (!silent)
                                                    sender.sendMessage(parse(prefix + broke, Immunity.getPermanentCost(), parser));
                                                return false;
                                            }

                                        } else {
                                            // ask to repeat
                                            repeatBuyCommand2.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + repeatCommandImmunity, Immunity.getPermanentCost(), parser));
                                            return true;
                                        }
                                    } else {
                                        // already bought immunity
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + alreadyBoughtPerm, parser));
                                        return false;
                                    }
                                } else {
                                    // scaling immunity or time immunity
                                    if (args.length > 1) {
                                        double amount;
                                        try {
                                            amount = tryParse(args[1]);
                                        } catch (NumberFormatException ignored) {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + unknownNumber, parser));
                                            return false;
                                        }
                                        if ((repeatBuyCommand2.containsKey((parser).getUniqueId().toString()) && System.currentTimeMillis() - repeatBuyCommand2.get((parser).getUniqueId().toString()) < 30000) || args.length > 2 && args[2].equalsIgnoreCase("--confirm")) {
                                            // try to find bounty and buy it
                                            repeatBuyCommand2.remove((parser).getUniqueId().toString());
                                            if (checkBalance(parser, amount)) {
                                                NumberFormatting.doRemoveCommands(parser, amount, new ArrayList<>());
                                                // successfully bought scaling immunity - amount x scalingRatio
                                                Immunity.addImmunity(parser.getUniqueId(), amount);
                                                ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.PURCHASE_IMMUNITY, amount);
                                                if (Immunity.immunityType == Immunity.ImmunityType.SCALING) {
                                                    if (!silent)
                                                        sender.sendMessage(parse(prefix + buyScalingImmunity, Immunity.getImmunity(parser.getUniqueId()) * Immunity.getScalingRatio(), parser));
                                                } else {
                                                    if (!silent)
                                                        sender.sendMessage(parse(prefix + buyTimeImmunity.replace("{time}", (LocalTime.formatTime(Immunity.getTimeImmunity(parser), LocalTime.TimeFormat.RELATIVE))), Immunity.getImmunity(parser.getUniqueId()) * Immunity.getTime(), parser));
                                                }
                                                return true;
                                            } else {
                                                // broke
                                                if (!silent)
                                                    sender.sendMessage(parse(prefix + broke, amount, parser));
                                                return false;
                                            }
                                        } else {
                                            // ask to repeat
                                            repeatBuyCommand2.put((parser).getUniqueId().toString(), System.currentTimeMillis());
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + repeatCommandImmunity, amount, parser));
                                            return true;
                                        }
                                    } else {
                                        // usage
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                                        if (Immunity.immunityType == Immunity.ImmunityType.SCALING)
                                            sendHelpMessage(sender, helpBuyImmunityScaling);
                                        else if (Immunity.immunityType == Immunity.ImmunityType.TIME)
                                            sendHelpMessage(sender, helpBuyImmunityTime);
                                        return false;
                                    }
                                }
                            } else {
                                // no permission
                                if (!silent)
                                    sender.sendMessage(parse(prefix + noPermission, parser));
                                return false;
                            }
                        } else {
                            // unknown command
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            return false;
                        }
                    } else {
                        if (!silent)
                            sender.sendMessage("Only players can buy immunity!");
                        return false;
                    }
                }
            } else if (args[0].equalsIgnoreCase("buy")) {
                if (sender instanceof Player) {
                    if (buyBack) {
                        if (sender.hasPermission("notbounties.buyown")) {
                            if (hasBounty(parser.getUniqueId())) {
                                Bounty bounty = getBounty(parser.getUniqueId());
                                assert bounty != null;
                                if ((repeatBuyCommand.containsKey((parser).getUniqueId().toString()) && System.currentTimeMillis() - repeatBuyCommand.get((parser).getUniqueId().toString()) < 30000) || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"))) {
                                    // try to find bounty and buy it
                                    repeatBuyCommand.remove((parser).getUniqueId().toString());
                                    if (checkBalance(parser, (bounty.getTotalDisplayBounty() * buyBackInterest))) {
                                        Bounty bought = new Bounty(bounty);
                                        BountyRemoveEvent event = new BountyRemoveEvent(sender, true, bought);
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (event.isCancelled())
                                            return true;
                                        NumberFormatting.doRemoveCommands(parser, (bounty.getTotalDisplayBounty() * buyBackInterest), new ArrayList<>());
                                        if (bountyItemsBuyItem)
                                            NumberFormatting.givePlayer(parser, bounty.getTotalItemBounty(), false);
                                        BountyManager.removeBounty(bounty.getUUID());
                                        ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.BUY_OWN, 1);
                                        reopenBountiesGUI();
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + successRemoveBounty, sender.getName(), parser));
                                        return true;
                                    } else {
                                        // broke
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + broke, (bounty.getTotalDisplayBounty() * buyBackInterest), parser));
                                        return false;
                                    }
                                } else {
                                    // open gui
                                    repeatBuyCommand.put(parser.getUniqueId().toString(), System.currentTimeMillis());
                                    GUI.openGUI(parser, "confirm", 1, parser.getUniqueId(), (bounty.getTotalDisplayBounty() * buyBackInterest));
                                    return true;
                                }
                            } else {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + noBounty, sender.getName(), parser));
                                return false;
                            }
                        } else {
                            if (!silent)
                                sender.sendMessage(parse(prefix + noPermission, parser));
                            return false;
                        }
                    } else {
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                        return false;
                    }
                } else {
                    if (!silent)
                        sender.sendMessage("You don't have a bounty!");
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("check")) {
                if (sender.hasPermission("notbounties.view")) {
                    if (args.length > 1) {
                        OfflinePlayer p = getPlayer(args[1]);
                        if (p == null) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownPlayer, args[1], parser));
                            return false;
                        }
                        if (!hasBounty(p.getUniqueId())) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + noBounty, p.getName(), parser));
                            return false;
                        }
                        Bounty bounty = getBounty(p.getUniqueId());
                        assert bounty != null;
                        double bountyAmount = showWhitelistedBounties || sender.hasPermission(NotBounties.getAdminPermission()) || !(sender instanceof Player) ? bounty.getTotalDisplayBounty() : bounty.getTotalDisplayBounty(parser);
                        if (bountyAmount == 0) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + noBounty, p.getName(), parser));
                            return false;
                        }

                        if (args.length > 2) {
                            if (args[2].equalsIgnoreCase("list")) {
                                if (silent)
                                    return true;
                                sender.sendMessage(parse(prefix + checkBounty, p.getName(), bountyAmount, bounty.getLatestSetter(), LocalTime.TimeFormat.PLAYER, parser));
                                for (Setter setters : bounty.getSetters()) {
                                    if (showWhitelistedBounties || sender.hasPermission(NotBounties.getAdminPermission()) || !(sender instanceof Player) || setters.canClaim(parser)) {
                                        if (listSetter.contains("{items}") && !setters.getItems().isEmpty()) {
                                            BaseComponent[] components = new BaseComponent[setters.getItems().size() + 2];
                                            components[0] = new TextComponent(parse(prefix + listSetter.substring(0, listSetter.indexOf("{items}")), setters.getName(), setters.getDisplayAmount(), setters.getTimeCreated(), LocalTime.TimeFormat.PLAYER, parser));
                                            BaseComponent[] itemComponents = NumberFormatting.listHoverItems(setters.getItems(), 'x');
                                            System.arraycopy(itemComponents, 0, components, 1, itemComponents.length);
                                            components[components.length - 1] = new TextComponent(parse(listSetter.substring(listSetter.indexOf("{items}") + 7), setters.getName(), setters.getDisplayAmount(), setters.getTimeCreated(), LocalTime.TimeFormat.PLAYER, parser));
                                            sender.spigot().sendMessage(components);
                                        } else {
                                            sender.sendMessage(parse(prefix + listSetter.replace("{items}", ""), setters.getName(), setters.getDisplayAmount(), setters.getTimeCreated(), LocalTime.TimeFormat.PLAYER, parser));
                                        }
                                        if (!setters.canClaim(parser))
                                            notWhitelistedLore.stream().filter(s -> !s.isEmpty()).map(s -> parse(s, p)).forEach(sender::sendMessage);
                                    }
                                }
                                return true;
                            } else {
                                if (!silent) {
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    sendHelpMessage(sender, helpView);
                                }
                                return false;
                            }
                        } else {
                            // open gui
                            openGUI(parser, "view-bounty", 1, bounty.getUUID());
                            return true;
                        }
                    } else {
                        if (!silent) {
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sendHelpMessage(sender, helpView);
                        }
                        return false;
                    }
                } else {
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
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
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (sender.hasPermission(NotBounties.getAdminPermission())) {
                    if (args.length > 1) {
                        OfflinePlayer bountyPlayer = NotBounties.getPlayer(args[1]);
                        if (bountyPlayer == null) {
                            if (!silent)
                                sender.sendMessage(LanguageOptions.parse(prefix + unknownPlayer, args[1], parser));
                            return false;
                        }
                        Bounty toRemove = DataManager.getGuarrenteedBounty(bountyPlayer.getUniqueId());
                        if (toRemove != null) {
                            if (args.length == 2) {
                                Bounty bounty = new Bounty(toRemove);
                                BountyRemoveEvent event = new BountyRemoveEvent(sender, false, bounty);
                                Bukkit.getPluginManager().callEvent(event);
                                if (event.isCancelled())
                                    return true;
                                BountyManager.removeBounty(toRemove.getUUID());
                                refundBounty(toRemove);
                                // successfully removed
                                if (parser != null && parser.getUniqueId().equals(toRemove.getUUID()))
                                    ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.BUY_OWN, 1);
                                NotBounties.removeWantedTag(toRemove.getUUID());
                                Player player = Bukkit.getPlayer(toRemove.getUUID());
                                if (player != null) {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), player));
                                } else {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                }
                                return true;
                            } else if (args.length == 4) {
                                if (args[2].equalsIgnoreCase("from")) {
                                    final String setterName = args[3];
                                    List<Setter> actualSetters = toRemove.getSetters().stream().filter(setter -> setter.getName().equalsIgnoreCase(setterName) || setter.getUuid().toString().equalsIgnoreCase(setterName)).toList();

                                    if (!actualSetters.isEmpty()) {
                                        Bounty bounty = new Bounty(toRemove.getUUID(), actualSetters, toRemove.getName());
                                        BountyRemoveEvent event = new BountyRemoveEvent(sender, false, bounty);
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (event.isCancelled())
                                            return true;
                                        BountyManager.removeSetters(toRemove.getUUID(), actualSetters);

                                        refundBounty(bounty);
                                        // reopen gui for everyone
                                        reopenBountiesGUI();
                                        // successfully removed
                                        if (toRemove.getTotalDisplayBounty() < minWanted)
                                            NotBounties.removeWantedTag(toRemove.getUUID());

                                        Player player = Bukkit.getPlayer(toRemove.getUUID());
                                        if (player != null) {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), player));
                                        } else {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                        }
                                        return true;
                                    } else {
                                        // couldn't find setter
                                        Player player = Bukkit.getPlayer(toRemove.getUUID());
                                        if (player != null) { // player then receiver
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + noSetter, toRemove.getName(), args[3], player));
                                        } else {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + noSetter, toRemove.getName(), args[3], Bukkit.getOfflinePlayer(toRemove.getUUID())));
                                        }
                                        return false;
                                    }

                                } else {
                                    // usage
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    sendHelpMessage(sender, helpAdmin);
                                    return false;
                                }
                            } else {
                                // usage
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sendHelpMessage(sender, helpAdmin);
                                return false;
                            }
                        } else {
                            // could not find bounty
                            if (!silent)
                                sender.sendMessage(parse(prefix + noBounty, args[1], parser));
                            return false;
                        }
                    } else {
                        // usage
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sendHelpMessage(sender, helpAdmin);
                        return false;
                    }
                } else if (sender.hasPermission("notbounties.removeset") && sender instanceof Player) {
                    if (args.length != 2) {
                        // usage
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sendHelpMessage(sender, helpRemoveSet);
                        return false;
                    }
                    Bounty toRemove = null;
                    List<Bounty> bountyList = BountyManager.getAllBounties(-1);
                    for (Bounty bounty : bountyList) {
                        if (bounty.getName().equalsIgnoreCase(args[1])) {
                            toRemove = bounty;
                            break;
                        }
                    }
                    if (toRemove == null) {
                        // could not find bounty
                        if (!silent)
                            sender.sendMessage(parse(prefix + noBounty, args[1], parser));
                        return false;
                    }
                    Setter actualRemove = null;
                    for (Setter setter : toRemove.getSetters()) {
                        if (setter.getUuid().equals(parser.getUniqueId())) {
                            actualRemove = setter;
                            break;
                        }
                    }

                    if (actualRemove == null) {
                        // couldnt find setter
                        Player player = Bukkit.getPlayer(toRemove.getUUID());
                        if (player != null) { // player then receiver
                            if (!silent)
                                sender.sendMessage(parse(prefix + noSetter, toRemove.getName(), sender.getName(), player));
                        } else {
                            if (!silent)
                                sender.sendMessage(parse(prefix + noSetter, toRemove.getName(), sender.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                        }
                        return false;
                    }
                    Bounty bounty = new Bounty(toRemove.getUUID(), Collections.singletonList(actualRemove), toRemove.getName());
                    BountyRemoveEvent event = new BountyRemoveEvent(sender, false, bounty);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled())
                        return true;
                    toRemove.removeBounty(actualRemove.getUuid());
                    if (toRemove.getSetters().isEmpty()) {
                        BountyManager.removeBounty(toRemove.getUUID());
                    }
                    refundSetter(actualRemove);
                    // reopen gui for everyone
                    reopenBountiesGUI();
                    // successfully removed
                    if (toRemove.getTotalDisplayBounty() < minWanted)
                        NotBounties.removeWantedTag(toRemove.getUUID());

                    Player player = Bukkit.getPlayer(toRemove.getUUID());
                    if (player != null) {
                        if (!silent)
                            sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), player));
                    } else {
                        if (!silent)
                            sender.sendMessage(parse(prefix + successRemoveBounty, toRemove.getName(), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                    }
                    return true;
                } else {
                    // no permission
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("edit")) {
                if (sender.hasPermission(NotBounties.getAdminPermission())) {
                    if (args.length > 2) {
                        Bounty toEdit = null;
                        List<Bounty> bountyList = BountyManager.getAllBounties(-1);
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
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + unknownNumber, parser));

                                    return false;
                                }
                                Bounty before = new Bounty(toEdit);
                                Bounty after = new Bounty(toEdit);
                                after.addBounty(amount - toEdit.getTotalDisplayBounty(), new ArrayList<>(), new Whitelist(new ArrayList<>(), false));
                                BountyEditEvent event = new BountyEditEvent(sender, before, after);
                                Bukkit.getPluginManager().callEvent(event);
                                if (event.isCancelled())
                                    return true;
                                if (BountyManager.editBounty(toEdit, null, amount)) {
                                    // successfully edited bounty
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + successEditBounty, toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                } else {
                                    // unsuccessful edit
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + noBounty, toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                }
                                return true;
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
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + unknownNumber, parser));
                                            return false;
                                        }
                                        Bounty before = new Bounty(toEdit);
                                        Bounty after = new Bounty(toEdit);
                                        after.editBounty(actualEdit.getUuid(), amount);
                                        BountyEditEvent event = new BountyEditEvent(sender, before, after);
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (event.isCancelled())
                                            return true;

                                        if (BountyManager.editBounty(toEdit, actualEdit.getUuid(), amount)) {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + successEditBounty, toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                        } else {
                                            // unsuccessful edit
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + noBounty, toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                        }
                                        return true;
                                    } else {
                                        // couldnt find setter
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + noSetter, args[3], toEdit.getName(), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                        return false;
                                    }
                                } else {
                                    // usage
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                                    sendHelpMessage(sender, helpAdmin);
                                    return false;
                                }
                            } else {
                                // usage
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownCommand, parser));
                                sendHelpMessage(sender, helpAdmin);
                                return false;
                            }
                        } else {
                            // couldn't find bounty
                            if (!silent)
                                sender.sendMessage(parse(prefix + noBounty, args[1], parser));
                            return false;

                        }
                    } else {
                        // usage
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sendHelpMessage(sender, helpAdmin);
                        return false;
                    }
                } else {
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("poster")) {
                if (!(giveOwnMap || sender.hasPermission(NotBounties.getAdminPermission()))) {
                    // no permission
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
                if (!sender.hasPermission(NotBounties.getAdminPermission()) && sender instanceof Player) {
                    if (giveOwnCooldown.containsKey(parser.getUniqueId()) && giveOwnCooldown.get(parser.getUniqueId()) > System.currentTimeMillis()) {
                        // cooldown
                        if (!silent)
                            sender.sendMessage(parse(prefix + waitCommand, giveOwnCooldown.get(parser.getUniqueId()) - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE, parser));
                        return false;
                    }
                    giveOwnCooldown.put(parser.getUniqueId(), System.currentTimeMillis() + cooldownTime);
                }
                if (args.length == 1) {
                    // usage
                    if (!silent)
                        sender.sendMessage(parse(prefix + unknownCommand, parser));
                    sendHelpMessage(sender, helpPosterOwn);
                    if (sender.hasPermission(NotBounties.getAdminPermission()))
                        sendHelpMessage(sender, helpPosterOther);

                    return false;
                }
                OfflinePlayer player = getPlayer(args[1]);
                if (player == null) {
                    // unknown player
                    if (!silent)
                        sender.sendMessage(parse(prefix + unknownPlayer, args[1], parser));
                    return false;
                }
                Bounty bounty = getBounty(player.getUniqueId());
                String playerName = bounty != null ? bounty.getName() : NotBounties.getPlayerName(player.getUniqueId());
                Player receiver;
                if (args.length > 2) {
                    receiver = Bukkit.getPlayer(args[2]);
                    if (receiver == null) {
                        // can't find player
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownPlayer, args[2], parser));
                        return false;
                    }
                    if (!silent)
                        sender.sendMessage(parse(prefix + mapGive, args[2], playerName, player));
                } else if (sender instanceof Player) {
                    receiver = parser;
                } else {
                    if (!silent)
                        sender.sendMessage("You cant give yourself this item!");
                    return false;
                }
                ItemStack mapItem = bounty != null ? BountyMap.getMap(bounty) : BountyMap.getMap(player.getUniqueId(), playerName, 0, System.currentTimeMillis());
                NumberFormatting.givePlayer(receiver, mapItem, 1);
                receiver.sendMessage(parse(prefix + mapReceive, playerName, player));
                return true;
            } else if (args[0].equalsIgnoreCase("tracker")) {
                // give a tracker that points toward a certain player with a bounty
                if (BountyTracker.isEnabled())
                    // admins can do everything -                         Give own or write empty settings with the tracker perm
                    if (sender.hasPermission(NotBounties.getAdminPermission()) || ((BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers()) && sender.hasPermission("notbounties.tracker"))) {
                        if (!sender.hasPermission(NotBounties.getAdminPermission()) && sender instanceof Player) {
                            if (giveOwnCooldown.containsKey(parser.getUniqueId()) && giveOwnCooldown.get(parser.getUniqueId()) > System.currentTimeMillis()) {
                                // cooldown
                                if (!silent)
                                    sender.sendMessage(parse(prefix + waitCommand, giveOwnCooldown.get(parser.getUniqueId()) - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE, parser));
                                return false;
                            }
                            giveOwnCooldown.put(parser.getUniqueId(), System.currentTimeMillis() + cooldownTime);
                        }
                        if (args.length > 1) {
                            OfflinePlayer player = NotBounties.getPlayer(args[1]);
                            boolean giveEmpty = args[1].equalsIgnoreCase("empty");
                            if (player == null && !giveEmpty) {
                                // unknown player
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownPlayer, args[0], parser));
                                return false;
                            }

                            if (giveEmpty || hasBounty(player.getUniqueId())) {
                                if (!giveEmpty && getBounty(player.getUniqueId()).getTotalDisplayBounty() < BountyTracker.getMinBounty()) {
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + LanguageOptions.minBounty, BountyTracker.getMinBounty(),  parser));
                                    return false;
                                }

                                if (args.length > 2) {
                                    // /bounty tracker (player) (receiver)
                                    if (!sender.hasPermission(NotBounties.getAdminPermission())) {
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + noPermission, parser));
                                        return false;
                                    }
                                    // find player to give to
                                    Player receiver = Bukkit.getPlayer(args[2]);
                                    if (receiver != null) {
                                        ItemStack tracker = giveEmpty ? BountyTracker.getEmptyTracker() : BountyTracker.getTracker(player.getUniqueId());
                                        NumberFormatting.givePlayer(receiver, tracker, 1);
                                        // you have been given & you have received
                                        String tracked = giveEmpty ? ChatColor.RED + "X" : player.getName();
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + trackerGive, receiver.getName(), tracked, player));

                                        if (giveEmpty) {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + trackerReceiveEmpty, player));
                                        } else {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + trackerReceive, player.getName(), player));
                                        }
                                        return true;
                                    } else {
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + unknownPlayer, args[2], parser));
                                        return false;
                                    }
                                } else {
                                    // /bounty tracker (player)
                                    if (sender instanceof Player) {
                                        if (!sender.hasPermission("notbounties.tracker") && !sender.hasPermission(NotBounties.getAdminPermission())) {
                                            // no permission
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + noPermission, parser));
                                            return false;
                                        }
                                        ItemStack tracker = giveEmpty ? BountyTracker.getEmptyTracker() : BountyTracker.getTracker(player.getUniqueId());
                                        // check if player is holding an empty compass
                                        if (BountyTracker.isWriteEmptyTrackers() && BountyTracker.getTrackerID(parser.getInventory().getItemInMainHand()) == -1) {
                                            // has empty tracker in hand
                                            BountyTracker.removeEmptyTracker(parser, true); // remove 1 empty tracker
                                            NumberFormatting.givePlayer(parser, tracker, 1); // give new tracker
                                        } else if (BountyTracker.isGiveOwnTracker() || sender.hasPermission(NotBounties.getAdminPermission())) {
                                            // does not have an empty tracker
                                            NumberFormatting.givePlayer(parser, tracker, 1); // give new tracker
                                        } else {
                                            if (BountyTracker.isWriteEmptyTrackers()) {
                                                // Does not have an empty tracker
                                                if (!silent)
                                                    sender.sendMessage(parse(prefix + noEmptyTracker, parser));
                                            } else {
                                                // no permission
                                                if (!silent)
                                                    sender.sendMessage(parse(prefix + noPermission, parser));
                                            }
                                            return false;
                                        }
                                        // you have been given
                                        if (giveEmpty) {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + trackerReceiveEmpty, player));
                                        } else {
                                            if (!silent)
                                                sender.sendMessage(parse(prefix + trackerReceive, player.getName(), player));
                                        }
                                        return true;
                                    } else {
                                        if (!silent)
                                            sender.sendMessage("You are not a player!");
                                        return false;
                                    }
                                }
                            } else {
                                // couldn't find bounty
                                if (!silent)
                                    sender.sendMessage(parse(prefix + noBounty, NotBounties.getPlayerName(player.getUniqueId()), parser));
                                return false;
                            }
                        } else {
                            // usage
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sendHelpMessage(sender, helpTrackerOwn);
                            if (sender.hasPermission(NotBounties.getAdminPermission()))
                                sendHelpMessage(sender, helpTrackerOther);
                            return false;
                        }
                    } else {
                        if (!silent)
                            sender.sendMessage(parse(prefix + noPermission, parser));
                        return false;
                    }
                return true;
            } else {
                if (sender.hasPermission("notbounties.set")) {
                    OfflinePlayer player = getPlayer(args[0]);
                    if (player == null) {
                        // can't find player
                        if (args.length == 1) {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownCommand, parser));
                            sendHelpMessage(sender, helpSet);
                        } else {
                            if (!silent)
                                sender.sendMessage(parse(prefix + unknownPlayer, args[0], parser));
                        }
                        return false;
                    }
                    if (!selfSetting && sender instanceof Player && ((Player) sender).getUniqueId().equals(player.getUniqueId())) {
                        // own bounty
                        if (!silent)
                            sender.sendMessage(parse(prefix + selfSetDeny, parser));
                        return false;
                    }
                    if (args.length == 1) {
                        if (sender instanceof Player) {
                            if (bountyItemsDefaultGUI) {
                                openGUI(parser, "bounty-item-select", 1, player.getUniqueId());
                            } else {
                                openGUI(parser, "select-price", ConfigOptions.minBounty, player.getUniqueId().toString());
                            }
                        }
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
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownPlayer, args[i], parser));
                                return false;
                            }
                            newWhitelist.add(p.getUniqueId());
                        }
                        whitelist.setList(newWhitelist);
                    }

                    // check if max setters reached
                    if (ConfigOptions.maxSetters > -1) {
                        if (hasBounty(player.getUniqueId())) {
                            Bounty bounty = getBounty(player.getUniqueId());
                            assert bounty != null;
                            if (bounty.getSetters().size() >= ConfigOptions.maxSetters) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + LanguageOptions.maxSetters, args[0], parser));
                                return false;
                            }
                        }
                    }


                    // check if we can get the amount
                    double amount = 0;
                    Map<Material, Integer> requestedItems = new HashMap<>();
                    List<ItemStack> items = new ArrayList<>();
                    boolean usingGUI = false;
                    if (bountyItemMode == BountyItemMode.ALLOW || bountyItemMode == BountyItemMode.EXCLUSIVE) {
                        // check if the input is an item
                        // /bounty (player) material:amount,apple:4,gold_ingot,diamond:1
                        String[] commaSeparated = args[1].split(",");
                        for (String entry : commaSeparated) {
                            // try to get the material
                            Material material;
                            String materialString = entry.contains(":") ? entry.substring(0, entry.indexOf(":")) : entry;
                            try {
                                material = Material.valueOf(materialString.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                if (commaSeparated.length > 1 || entry.contains(":")) {
                                    // probably an attempt for a material
                                    if (!silent)
                                        sender.sendMessage(parse(prefix + unknownMaterial.replace("{material}", (materialString)), parser));
                                    return false;
                                }
                                continue;
                            }
                            int materialAmount;
                            try {
                                materialAmount = entry.contains(":") ? Integer.parseInt(entry.substring(entry.indexOf(":") + 1)) : 1;
                                if (materialAmount < 1)
                                    throw new NumberFormatException("Number needs to be positive!");
                            } catch (NumberFormatException e) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownNumber, parser));
                                return false;
                            }
                            if (requestedItems.containsKey(material))
                                requestedItems.replace(material, requestedItems.get(material) + materialAmount);
                            else
                                requestedItems.put(material, materialAmount);
                        }
                        if (!requestedItems.isEmpty()) {
                            // definitely items
                            // get materials from inventory
                            if (sender instanceof Player) {
                                // check if they have the bounty-item-select GUI open
                                if (GUI.playerInfo.containsKey(((Player) sender).getUniqueId())) {
                                    PlayerGUInfo info = GUI.playerInfo.get(((Player) sender).getUniqueId());
                                    if (info.guiType().equals("bounty-item-select")) {
                                        usingGUI = true;
                                        // get items from data
                                        for (int i = 1; i < info.data().length; i++) {
                                            try {
                                                ItemStack[] contents = SerializeInventory.itemStackArrayFromBase64(info.data()[i].toString());
                                                for (ItemStack item : contents)
                                                    if (item != null)
                                                        items.add(item);
                                            } catch (IOException e) {
                                                Bukkit.getLogger().warning("[NotBounties] Could not deserialize items: " + info.data()[i].toString());
                                                Bukkit.getLogger().warning(e.toString());
                                            }
                                        }
                                    }
                                }
                                if (!usingGUI) {
                                    ItemStack[] contents = ((Player) sender).getInventory().getContents();
                                    for (ItemStack content : contents) {
                                        if (content == null)
                                            // empty slot
                                            continue;
                                        if (!requestedItems.containsKey(content.getType()))
                                            // not an item that was requested
                                            continue;
                                        int itemAmount = requestedItems.get(content.getType());
                                        ItemStack itemToAdd = content.clone();
                                        // contents[i] doesn't need to be updated because the player's inventory won't be updated
                                        if (content.getAmount() > itemAmount) {
                                            itemToAdd.setAmount(itemAmount);
                                            requestedItems.remove(content.getType());
                                        } else if (content.getAmount() < itemAmount) {
                                            itemAmount -= content.getAmount();
                                            requestedItems.replace(content.getType(), itemAmount);
                                        } else {
                                            requestedItems.remove(content.getType());
                                        }
                                        items.add(itemToAdd);
                                        if (requestedItems.isEmpty())
                                            break;
                                    }
                                    // check if all items were obtained - only send broke message if an item is missing completely
                                    // remove any found items from requested items
                                    items.stream().map(ItemStack::getType).forEach(requestedItems::remove);
                                    if (!requestedItems.isEmpty()) {
                                        // there are missing items
                                        // iterate through requested items to get missing items and their amounts and add to string builder
                                        StringBuilder brokeAmount = new StringBuilder();
                                        for (Map.Entry<Material, Integer> missingItems : requestedItems.entrySet()) {
                                            if (brokeAmount.length() != 0)
                                                brokeAmount.append(",");
                                            brokeAmount.append(missingItems.getKey().name()).append("x").append(missingItems.getValue());
                                        }
                                        // send message
                                        if (!silent)
                                            sender.sendMessage(parse(prefix + broke.replace("{amount}", (brokeAmount.toString())), parser));
                                        return false;
                                    }
                                    // don't update inventory until the bounty will be set
                                }
                            } else {
                                // add default items because console
                                for (Map.Entry<Material, Integer> item : requestedItems.entrySet()) {
                                    items.add(new ItemStack(item.getKey(), item.getValue()));
                                }
                            }
                            // get value from items
                            amount = NumberFormatting.getTotalValue(items);
                        }
                    }

                    if (bountyItemMode != BountyItemMode.EXCLUSIVE) {
                        if (items.isEmpty())
                            try {
                                amount = tryParse(args[1]);
                            } catch (NumberFormatException ignored) {
                                if (!silent)
                                    sender.sendMessage(parse(prefix + unknownNumber, parser));
                                return false;
                            }
                    } else if (items.isEmpty()) {
                        // exclusive mode is enabled but player didn't specify any items
                        // unknown command
                        if (!silent)
                            sender.sendMessage(parse(prefix + unknownCommand, parser));
                        sendHelpMessage(sender, helpSet);
                        return true;
                    }

                    if (amount < ConfigOptions.minBounty) {
                        if (!silent)
                            sender.sendMessage(parse(prefix + LanguageOptions.minBounty, ConfigOptions.minBounty, parser));
                        return false;
                    }
                    // total cost to place this bounty in currency
                    double total = amount * bountyTax + whitelist.getList().size() * bountyWhitelistCost;
                    if (items.isEmpty())
                        total += amount;  // this includes the bounty if no items are being set

                    // check if it is a player

                    if (sender instanceof Player) {
                        // check for immunity
                        boolean usingImmunity = false;
                        switch (Immunity.getAppliedImmunity(player, amount)) {
                            case GRACE_PERIOD:
                                if (!silent)
                                    sender.sendMessage(parse(prefix + LanguageOptions.gracePeriod.replace("{time}", (LocalTime.formatTime(Immunity.getGracePeriod(player.getUniqueId()), LocalTime.TimeFormat.RELATIVE))), player.getName(), player));
                                usingImmunity = true;
                                break;
                            case PERMANENT:
                                if (bountyItemsOverrideImmunity && !items.isEmpty())
                                    break;
                                if (!silent)
                                    sender.sendMessage(parse(prefix + permanentImmunity, player.getName(), Immunity.getImmunity(player.getUniqueId()), player));
                                usingImmunity = true;
                                break;
                            case SCALING:
                                if (bountyItemsOverrideImmunity && !items.isEmpty())
                                    break;
                                if (!silent)
                                    sender.sendMessage(parse(prefix + scalingImmunity, player.getName(), Immunity.getImmunity(player.getUniqueId()), player));
                                usingImmunity = true;
                                break;
                            case TIME:
                                if (bountyItemsOverrideImmunity && !items.isEmpty())
                                    break;
                                if (!silent)
                                    sender.sendMessage(parse(prefix + LanguageOptions.timeImmunity.replace("{time}", (LocalTime.formatTime(Immunity.getTimeImmunity(player), LocalTime.TimeFormat.RELATIVE))), player.getName(), Immunity.getImmunity(player.getUniqueId()), player));
                                usingImmunity = true;
                                break;
                            default:
                                // Not using immunity
                                break;
                        }
                        if (usingImmunity) {
                            if (usingGUI)
                                GUI.safeCloseGUI(parser, false);
                            return false;
                        }

                        if (checkBalance(parser, total)) {
                            if (!args[args.length - 1].equalsIgnoreCase("--confirm") && confirmation) {
                                // if there are items, open set-bounty-item gui
                                if (!items.isEmpty()) {
                                    // format items to take up multiple pages if necessary
                                    String[] serializedItems = NumberFormatting.serializeItems(new ArrayList<>(items), GUI.getGUI("bounty-item-select").getPlayerSlots().size() - 1);
                                    // take items from player if not using the GUI
                                    if (!usingGUI) {
                                        NumberFormatting.removeItems(parser, new ArrayList<>(items), true);
                                    }
                                    // open gui
                                    openGUI(parser, "bounty-item-select", 1, player.getUniqueId(), serializedItems);
                                } else {
                                    openGUI(parser, "confirm-bounty", (long) amount, player.getUniqueId(), (long) amount);
                                }
                                return true;
                            }
                            double finalAmount = amount;
                            double finalTotal = total;
                            boolean finalUsingGUI = usingGUI;
                            boolean finalSilent = silent;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // async ban check
                                    if (!player.isOnline() && removeBannedPlayers && (player.isBanned() || (liteBansEnabled && !(new LiteBansClass().isPlayerNotBanned(player.getUniqueId()))))) {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                // has permanent immunity
                                                if (!finalSilent)
                                                    sender.sendMessage(parse(prefix + permanentImmunity, player.getName(), Immunity.getImmunity(player.getUniqueId()), player));
                                            }
                                        }.runTask(NotBounties.getInstance());
                                    } else {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                if (!items.isEmpty()) {
                                                    // check to see if player is still online after the async check
                                                    if (!parser.isOnline())
                                                        return;
                                                    String result = "";
                                                    if (finalUsingGUI && GUI.playerInfo.containsKey(parser.getUniqueId()) && GUI.playerInfo.get(parser.getUniqueId()).guiType().equals("bounty-item-select")) {
                                                        if (manualEconomy != ManualEconomy.AUTOMATIC) {
                                                            // give items back
                                                            GUI.safeCloseGUI(parser, false);
                                                        } else {
                                                            // erase data from gui
                                                            GUI.playerInfo.remove(parser.getUniqueId());
                                                            parser.getOpenInventory().close();
                                                        }
                                                    } else {
                                                        result = NumberFormatting.removeItems(parser, new ArrayList<>(items), manualEconomy == ManualEconomy.AUTOMATIC);
                                                    }

                                                    if (!result.isEmpty()) {
                                                        // didn't have all the items
                                                        if (!finalSilent)
                                                            sender.sendMessage(parse(prefix + broke.replace("{amount}", result), parser));
                                                        return;
                                                    }
                                                }
                                                if (manualEconomy != ManualEconomy.PARTIAL)
                                                    NumberFormatting.doRemoveCommands(parser, finalTotal, new ArrayList<>());
                                                addBounty(parser, player, finalAmount, items, whitelist);
                                                reopenBountiesGUI();
                                            }
                                        }.runTask(NotBounties.getInstance());
                                    }
                                }
                            }.runTaskAsynchronously(NotBounties.getInstance());
                        } else {
                            if (!silent)
                                sender.sendMessage(parse(prefix + broke, total, parser));
                            return false;
                        }

                    } else {
                        addBounty(player, amount, items, whitelist);
                        reopenBountiesGUI();
                    }

                } else {
                    if (!silent)
                        sender.sendMessage(parse(prefix + noPermission, parser));
                    return false;
                }
            }
        } else {
            // open gui
            if (sender instanceof Player) {
                if (sender.hasPermission("notbounties.view")) {
                    openGUI(parser, "bounty-gui", 1);
                } else {
                    if (!silent)
                        sender.sendMessage(parse(noPermission, parser));
                    return false;
                }
            } else {
                if (sender.hasPermission(NotBounties.getAdminPermission())) {
                    try {
                        NotBounties.getInstance().sendDebug(sender);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        boolean silent = false;
        for (int i = 0; i < args.length; i++) {
            // check if the argument has the -s
            if (args[i].equalsIgnoreCase("-s")) {
                silent = true;
            }
            // move everything backwards if a -s was found
            if (silent && i < args.length - 1) {
                args[i] = args[i + 1];
            }
        }
        if (silent) {
            // make sure length > 1
            if (args.length == 1)
                return new ArrayList<>();
            // remove the last argument
            String[] tempArgs = args;
            args = new String[tempArgs.length - 1];
            System.arraycopy(tempArgs, 0, args, 0, tempArgs.length - 1);
        }
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
                        if (!selfSetting && sender instanceof Player && ((Player) sender).getUniqueId().equals(p.getUniqueId()))
                            continue;
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
                if (sender.hasPermission(NotBounties.getAdminPermission())) {
                    tab.add("remove");
                    tab.add("edit");
                    tab.add("reload");
                    tab.add("currency");
                    tab.add("debug");
                    tab.add("board");
                    tab.add("cleanEntities");
                } else if (sender.hasPermission("notbounties.removeset")) {
                    tab.add("remove");
                }
                if (sender.hasPermission(NotBounties.getAdminPermission()) || giveOwnMap)
                    tab.add("poster");
                if ((sender.hasPermission(NotBounties.getAdminPermission()) || ((BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers()) && sender.hasPermission("notbounties.tracker"))) && BountyTracker.isEnabled())
                    tab.add("tracker");
                if (sender.hasPermission("notbounties.buyown") && buyBack) {
                    tab.add("buy");
                }
                if (Immunity.immunityType != Immunity.ImmunityType.DISABLE || sender.hasPermission(NotBounties.getAdminPermission())) {
                    if (sender.hasPermission("notbounties.buyimmunity")) {
                        tab.add("immunity");
                    }
                }
                if (sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled) {
                    tab.add("whitelist");
                }
                if (sender.hasPermission("notbounties.challenges") && ChallengeManager.isEnabled())
                    tab.add("challenges");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("check") && sender.hasPermission("notbounties.view")) {
                    List<Bounty> bountyList = BountyManager.getPublicBounties(-1);
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit") && sender.hasPermission(NotBounties.getAdminPermission()))) {
                    List<Bounty> bountyList = BountyManager.getAllBounties(-1);
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if (args[0].equalsIgnoreCase("immunity")) {
                    if ((sender.hasPermission(NotBounties.getAdminPermission()) || sender.hasPermission("notbounties.removeimmunity")))
                        tab.add("remove");
                    if ((sender.hasPermission(NotBounties.getAdminPermission()) || sender.hasPermission("notbounties.buyimmunity")) && Immunity.immunityType == Immunity.ImmunityType.PERMANENT)
                        tab.add("--confirm");
                } else if ((args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("stat")) && sender.hasPermission("notbounties.view")) {
                    tab.add("all");
                    tab.add("kills");
                    tab.add("claimed");
                    tab.add("deaths");
                    tab.add("set");
                    tab.add("immunity");
                    tab.add("current");
                } else if (args[0].equalsIgnoreCase("tracker") && BountyTracker.isEnabled() && (sender.hasPermission(NotBounties.getAdminPermission()) ||
                        ((BountyTracker.isGiveOwnTracker() || (BountyTracker.isWriteEmptyTrackers() && sender instanceof Player && BountyTracker.getTrackerID(((Player) sender).getInventory().getItemInMainHand()) == -1)) && sender.hasPermission("notbounties.tracker")))) {

                    List<Bounty> bountyList = sender.hasPermission(NotBounties.getAdminPermission()) ? BountyManager.getAllBounties(-1) : BountyManager.getPublicBounties(-1);
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                    if (sender.hasPermission(NotBounties.getAdminPermission()))
                        tab.add("empty");
                } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("notbounties.set")) {
                    tab.add("offline");
                } else if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled) {
                    tab.add("add");
                    tab.add("remove");
                    tab.add("set");
                    tab.add("offline");
                    tab.add("reset");
                    tab.add("view");
                    tab.add("toggle");
                } else if (args[0].equalsIgnoreCase("poster") && (sender.hasPermission(NotBounties.getAdminPermission()) || giveOwnMap)) {
                    List<Bounty> bountyList = sender.hasPermission(NotBounties.getAdminPermission()) ? BountyManager.getAllBounties(-1) : BountyManager.getPublicBounties(-1);
                    for (Bounty bounty : bountyList) {
                        tab.add(bounty.getName());
                    }
                } else if (args[0].equalsIgnoreCase("board") && sender.hasPermission(NotBounties.getAdminPermission())) {
                    tab.add("clear");
                    tab.add("remove");
                } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission(NotBounties.getAdminPermission())) {
                    if (debug)
                        tab.add("disable");
                    else
                        tab.add("enable");
                } else if (args[0].equalsIgnoreCase("bdc") && sender.hasPermission("notbounties.basic")) {
                    tab.add("enable");
                    tab.add("disable");
                } else if (args[0].equalsIgnoreCase("challenges") && sender.hasPermission("notbounties.challenges") && ChallengeManager.isEnabled()) {
                    tab.add("claim");
                    tab.add("check");
                } else if (!args[1].isEmpty() && sender instanceof Player && bountyItemMode != BountyItemMode.DENY && tabCompleteItems) {
                    // They have started typing
                    Player player = (Player) sender;
                    try {
                        NumberFormatting.tryParse(args[1].substring(0, 1));
                    } catch (NumberFormatException e) {
                        // not a number - check if player has typed in a material
                        String currentMaterial = getCurrentMaterial(args[1]);
                        try {
                            Material material = Material.valueOf(currentMaterial.toUpperCase());
                            // valid material
                            if (args[1].charAt(args[1].length() - 1) == ':') {
                                // add their inventory amount as this material
                                int amount = getInventoryAmount(player, material);
                                tab.add(args[1] + amount);
                            } else {
                                // tab complete colon or comma
                                if (args[1].lastIndexOf(":") > args[1].lastIndexOf(currentMaterial)) {
                                    // colon after currentMaterial
                                    tab.add(args[1] + ",");
                                } else {
                                    // add their inventory amount as this material
                                    int amount = getInventoryAmount(player, material);
                                    tab.add(args[1] + ":" + amount);
                                }
                            }
                        } catch (IllegalArgumentException e2) {
                            // not a material - try to tab complete from inv
                            ItemStack[] contents = player.getInventory().getContents();
                            for (ItemStack item : contents) {
                                if (item != null && item.getType().name().startsWith(currentMaterial.toUpperCase())) {
                                    String addition = args[1].substring(0, args[1].length() - currentMaterial.length()) + item.getType().name();
                                    if (!tab.contains(addition))
                                        tab.add(addition);
                                }
                            }
                        }
                    }
                }
            } else if (args.length == 3) {
                if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit")) && sender.hasPermission(NotBounties.getAdminPermission())) {
                    tab.add("from");
                } else if (args[0].equalsIgnoreCase("immunity")) {
                    if (args[1].equalsIgnoreCase("remove") && sender.hasPermission(NotBounties.getAdminPermission())) {
                        for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                            String name = NotBounties.getPlayerName(p.getUniqueId());
                            if (name.length() < 25)
                                tab.add(name);
                        }
                    }
                    if ((sender.hasPermission(NotBounties.getAdminPermission()) || sender.hasPermission("notbounties.buyimmunity")) && (Immunity.immunityType == Immunity.ImmunityType.SCALING || Immunity.immunityType == Immunity.ImmunityType.TIME))
                        tab.add("--confirm");
                } else if (args[0].equalsIgnoreCase("tracker") && BountyTracker.isEnabled()) {
                    for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                        String name = NotBounties.getPlayerName(p.getUniqueId());
                        if (name.length() < 25)
                            tab.add(name);
                    }
                } else if (args[0].equalsIgnoreCase("top") && sender.hasPermission("notbounties.view")) {
                    tab.add("list");
                } else if (args[0].equalsIgnoreCase("check") && sender.hasPermission("notbounties.view")) {
                    tab.add("list");
                } else if (args[0].equalsIgnoreCase("stat") && sender.hasPermission("notbounties.view")) {
                    for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                        String name = NotBounties.getPlayerName(p.getUniqueId());
                        if (name.length() < 25)
                            tab.add(name);
                    }
                } else if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled && enableBlacklist) {
                    tab.add("blacklist");
                    tab.add("whitelist");
                } else if (args[0].equalsIgnoreCase("challenges") && ChallengeManager.isEnabled()) {
                    for (int i = 0; i < ChallengeManager.getConcurrentChallenges(); i++) {
                        tab.add((i + 1) + "");
                    }
                }
            } else if (args.length == 4) {
                if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit")) && sender.hasPermission(NotBounties.getAdminPermission())) {
                    List<Bounty> bountyList = BountyManager.getAllBounties(-1);
                    for (Bounty bounty1 : bountyList) {
                        if (bounty1.getName().equalsIgnoreCase(args[1])) {
                            for (Setter setter : bounty1.getSetters()) {
                                tab.add(setter.getName());
                            }
                            break;
                        }
                    }
                } else if (args[0].equalsIgnoreCase("stat") && sender.hasPermission(NotBounties.getAdminPermission())) {
                    tab.add("edit");
                    tab.add("setValue");
                }
            }
            if (args.length > 2) {
                boolean isPlayer = NotBounties.getPlayer(args[0]) != null;
                if ((sender.hasPermission("notbounties.set") && isPlayer) || (args[0].equalsIgnoreCase("whitelist") && !args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.whitelist")))
                    for (OfflinePlayer p : NotBounties.getNetworkPlayers()) {
                        String name = NotBounties.getPlayerName(p.getUniqueId());
                        if (name.length() < 25)
                            tab.add(name);
                    }
                if (sender.hasPermission("notbounties.set") && isPlayer && confirmation)
                    tab.add("--confirm");
                if (args[0].equalsIgnoreCase("whitelist") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.whitelist")) {
                    for (UUID uuid : getPlayerWhitelist(((Player) sender).getUniqueId()).getList()) {
                        String name = getPlayerName(uuid);
                        if (name.length() < 25)
                            tab.add(name);
                    }
                }
            }

            String typed = args[args.length - 1];
            tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            if (tab.isEmpty()) {
                if (args.length == 1 || args.length > 2) {
                    if (sender.hasPermission("notbounties.set")) {
                        for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                            if (!selfSetting && sender instanceof Player && ((Player) sender).getUniqueId().equals(entry.getValue()))
                                continue;
                            String name = Bukkit.getOfflinePlayer(entry.getValue()).getName();
                            if (name != null)
                                tab.add(name);
                            else
                                tab.add(entry.getKey());
                        }
                    }
                }
                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("immunity") && args[1].equalsIgnoreCase("remove") && sender.hasPermission(NotBounties.getAdminPermission())) {
                        for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                            String name = Bukkit.getOfflinePlayer(entry.getValue()).getName();
                            if (name != null)
                                tab.add(name);
                            else
                                tab.add(entry.getKey());
                        }
                    } else if (args[0].equalsIgnoreCase("stat") && sender.hasPermission("notbounties.view")) {
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

    private static int getInventoryAmount(Player player, Material material) {
        ItemStack[] contents = player.getInventory().getContents();
        int amount = 0;
        for (ItemStack item : contents) {
            if (item != null && item.getType() == material) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    @NotNull
    private static String getCurrentMaterial(String arg) {
        String currentMaterial;
        if (arg.contains(":")) {
            if (arg.substring(arg.indexOf(":") + 1).contains(":")) {
                // at least 2 colons
                // check if comma after last colon
                if (arg.substring(arg.lastIndexOf(":")).contains(",")) {
                    // everything after last comma
                    currentMaterial = arg.substring(arg.lastIndexOf(",") + 1);
                } else if (arg.contains(",")) {
                    // everything between the last comma and colon
                    currentMaterial = arg.substring(arg.lastIndexOf(",") + 1, arg.lastIndexOf(":"));
                } else {
                    // wrong formatting
                    currentMaterial = arg.substring(0, arg.indexOf(":"));
                }
            } else {
                // check if there is a comma
                if (arg.contains(",")) {
                    // everything after comma
                    currentMaterial = arg.substring(arg.indexOf(",") + 1);
                } else {
                    // everything to colon
                    currentMaterial = arg.substring(0, arg.indexOf(":"));
                }
            }
        } else {
            currentMaterial = arg;
        }
        return currentMaterial;
    }

    public static Player getParser(CommandSender sender) {
        if (sender instanceof Player)
            return (Player) sender;
        return null;
    }
}

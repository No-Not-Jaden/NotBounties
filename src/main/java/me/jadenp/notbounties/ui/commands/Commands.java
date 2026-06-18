package me.jadenp.notbounties.ui.commands;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.bounty_events.BountyEditEvent;
import me.jadenp.notbounties.bounty_events.BountyRemoveEvent;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.settings.display.BountyHunt;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.money.ExcludedItemException;
import me.jadenp.notbounties.features.settings.money.NotEnoughCurrencyException;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.CustomItem;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyMap;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.challenges.ChallengeType;
import me.jadenp.notbounties.features.*;
import me.jadenp.notbounties.features.settings.auto_bounties.Prompt;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.features.settings.money.NumberFormatting.*;
import static me.jadenp.notbounties.ui.gui.GUI.*;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.features.LanguageOptions.*;

// This is a very old file with lots of spaghetti code. You have been warned

public class Commands implements CommandExecutor, TabCompleter {

    private static final Map<UUID, Long> giveOwnCooldown = new HashMap<>();
    private static final long GIVE_OWN_COOLDOWN_MS = 3000;
    private static final PosterCommandHandler posterCommandHandler = new PosterCommandHandler(Commands::failUnknownPlayer);
    private static final TrackerCommandHandler trackerCommandHandler = new TrackerCommandHandler(Commands::failUnknownPlayer);
    private static final ImmunityCommandHandler immunityCommandHandler = new ImmunityCommandHandler(Commands::failUnknownPlayer, Commands::executeCommand);
    private static final String[] allowedPausedAliases = new String[]{"check", "help", "cleanEntities", "unpause", "pause", "debug", "top", "reload"};
    private static final Map<String, Long> repeatBuyBountyCommand = new HashMap<>();

    public Commands() {}

    static boolean applyGiveOwnCooldown(CommandSender sender, boolean forcePermission, boolean adminPermission, boolean silent) {
        return (forcePermission || adminPermission) || !(sender instanceof Player player) || Commands.failGiveOwnWait(sender, silent, player);
    }

    static boolean failGiveOwnWait(CommandSender sender, boolean silent, Player player) {
        if (Commands.giveOwnCooldown.containsKey(player.getUniqueId()) && Commands.giveOwnCooldown.get(player.getUniqueId()) > System.currentTimeMillis()) {
            if (!silent)
                sender.sendMessage(parse(getPrefix() + getMessage("wait-command"), Commands.giveOwnCooldown.get(player.getUniqueId()) - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE, player));
            return false;
        }
        Commands.giveOwnCooldown.put(player.getUniqueId(), System.currentTimeMillis() + Commands.GIVE_OWN_COOLDOWN_MS);
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("notbounties")) {
            boolean adminPermission = sender.hasPermission(NotBounties.getAdminPermission());
            boolean commandResult = executeCommand(sender, args, false, adminPermission);
            if (sender instanceof Player player) {
                if (commandResult) {
                    Prompt.successfulExecute(player.getUniqueId());
                } else {
                    Prompt.failExecute(player.getUniqueId());
                }
            }
        } else if (command.getName().equalsIgnoreCase("notbountiesadmin") && sender.hasPermission(NotBounties.getAdminPermission())) {
            if (args.length > 0) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player != null) {
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, newArgs.length);
                    boolean commandResult = executeCommand(player, newArgs, true, true);
                    if (commandResult) {
                        Prompt.successfulExecute(player.getUniqueId());
                    } else {
                        Prompt.failExecute(player.getUniqueId());
                    }
                } else {
                    sender.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("unknown-player"), null));
                }
            } else {
                sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), null));
                LanguageOptions.sendHelpMessage(sender, getListMessage("help.admin"));
            }
        }
        return true;
    }

    public static boolean executeCommand(@NotNull CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission) {
        if (NotBounties.isPaused() && args.length > 0) {
            boolean allow = false;
            for (String str : allowedPausedAliases) {
                if (str.equalsIgnoreCase(args[0])) {
                    allow = true;
                    break;
                }
            }
            if (!allow) {
                if (forcePermission || adminPermission)
                    sender.sendMessage(parse(getPrefix() + getMessage("paused"), null));
                return true;
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
            if (args[0].equalsIgnoreCase("help") && (forcePermission || sender.hasPermission("notbounties.basic"))) {
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
            } else if (args[0].equalsIgnoreCase("item") && (forcePermission || adminPermission) && sender instanceof Player player) {
                // /bounty item (item)
                if (args.length > 1) {
                    CustomItem item = GUI.getCustomItems().get(args[1]);
                    if (item != null) {
                        player.getInventory().addItem(item.getFormattedItem(player, null, "bounty-gui"));
                        if (!silent) {
                            player.sendMessage(parse(getPrefix() + ChatColor.YELLOW + "You have been given the custom item.", parser));
                            player.sendMessage(parse(getPrefix() + ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/data get entity @s SelectedItem " + ChatColor.YELLOW + "while holding the item to view its components.", parser));
                        }
                        return true;
                    } else {
                        // unknown item
                        if (!silent)
                            player.sendMessage(parse(getPrefix() + ChatColor.RED + "Unknown custom item \"" + args[1] + "\". (case-sensitive)", parser));
                        return false;
                    }
                } else {
                    // usage
                    if (!silent) {
                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                        LanguageOptions.sendHelpMessage(sender, getListMessage("help.admin"));
                    }
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("sort") && (forcePermission || sender.hasPermission("notbounties.sort"))) {
                // /bounty sort (gui-type) #
                // sort type is 0-3 for bounty-gui and 1-4 for the rest
                if (args.length > 2) {
                    GUIOptions guiOptions = getGUI(args[1].toLowerCase());
                    if (guiOptions == null) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-gui").replace("{gui}", args[1]), parser));
                        return false;
                    }
                    boolean indexFrom0 = args[1].equalsIgnoreCase("bounty-gui");
                    UUID uuid;
                    if (args.length > 3 && forcePermission) {
                        uuid = LoggedPlayers.getPlayer(args[3]);
                        if (uuid == null) {
                            failUnknownPlayer(sender, args[3], silent);
                            return false;
                        }
                    } else if (sender instanceof Player player) {
                        uuid = player.getUniqueId();
                    } else {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                        return false;
                    }
                    Player player = Bukkit.getPlayer(uuid);
                    if (args[2].equalsIgnoreCase("toggle")) {
                        PlayerData playerData = DataManager.getPlayerData(uuid);
                        int newSortType = playerData.getGUISortType(args[1].toLowerCase()) + 1;
                        if (indexFrom0) {
                            if (newSortType > 3) {
                                newSortType = 0;
                            }
                        } else {
                            if (newSortType > 4) {
                                newSortType = 1;
                            }
                        }
                        if (player != null) {
                            player.sendMessage(parse(getPrefix() + getMessage("sort-change").replace("{gui}", args[1]).replace("{sort_type}", String.valueOf(newSortType)).replace("{sort_type_name}", GUI.parseSortType(args[1].toLowerCase(), newSortType)), player));
                        }
                        playerData.setGUISortType(args[1].toLowerCase(), newSortType);
                        return true;
                    }
                    int sortType;
                    try {
                        sortType = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        if (!silent) {
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));
                            LanguageOptions.sendHelpMessage(sender, getListMessage("help.sort"));
                        }
                        return false;
                    }
                    if (sortType < 0 || sortType > 4) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("invalid-range").replace("{range}", "1-4"), parser));
                        return false;
                    }
                    if (player != null && !silent)
                        player.sendMessage(parse(getPrefix() + getMessage("sort-change").replace("{gui}", args[1]).replace("{sort_type}", String.valueOf(sortType)), player));

                    DataManager.getPlayerData(uuid).setGUISortType(args[1].toLowerCase(), sortType);
                    return true;
                } else {
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("hunt") && BountyHunt.isEnabled()) {
                if (forcePermission || sender.hasPermission("notbounties.hunt")) {
                    return BountyHunt.executeHuntCommand(sender, args, silent, forcePermission, parser);
                } else {
                    // no permission
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("challenges") && ChallengeManager.isEnabled()) {
                if ((forcePermission || sender.hasPermission("notbounties.challenges")) && sender instanceof Player) {
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
                                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                        LanguageOptions.sendHelpMessage(sender, getListMessage("help.challenges"));
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
                                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                        LanguageOptions.sendHelpMessage(sender, getListMessage("help.challenges"));
                                    }
                                    return false;
                                }
                            }
                        } else {
                            // unknown command
                            if (!silent) {
                                sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                LanguageOptions.sendHelpMessage(sender, getListMessage("help.challenges"));
                            }
                            return false;
                        }
                    }
                } else {
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                }
                return false;
            } else if (args[0].equalsIgnoreCase("update-notification") && (forcePermission || adminPermission)) {
                if (args.length == 1) {
                    if (ConfigOptions.getUpdateNotification().equalsIgnoreCase("true")) {
                        ConfigOptions.setUpdateNotification("false");
                    } else {
                        ConfigOptions.setUpdateNotification("true");
                    }
                } else {
                    if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("enable")) {
                        ConfigOptions.setUpdateNotification("true");
                    } else if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("disable")) {
                        ConfigOptions.setUpdateNotification("false");
                    } else {
                        ConfigOptions.setUpdateNotification(args[1]);
                    }
                }
                if (!silent) {
                    if (ConfigOptions.getUpdateNotification().equalsIgnoreCase("true")) {
                        sender.sendMessage(parse(getPrefix() + ChatColor.YELLOW + "The update notification is now " + ChatColor.GREEN + "enabled" + ChatColor.YELLOW + ".", parser));
                    } else if (ConfigOptions.getUpdateNotification().equalsIgnoreCase("false")) {
                        sender.sendMessage(parse(getPrefix() + ChatColor.YELLOW + "The update notification is now " + ChatColor.RED + "disabled" + ChatColor.YELLOW + ".", parser));
                    } else {
                        sender.sendMessage(parse(getPrefix() + ChatColor.YELLOW + "The update notification is now skipping the version " + ChatColor.GOLD + ConfigOptions.getUpdateNotification() + ChatColor.YELLOW + ".", parser));
                    }
                }
            } else if (args[0].equalsIgnoreCase("skin") && (forcePermission || adminPermission)) {
                if (args.length > 1) {
                    UUID uuid = LoggedPlayers.getPlayer(args[1]);
                    if (uuid == null) {
                        failUnknownPlayer(sender, args[1], silent);
                        return false;
                    }
                    if (SkinManager.isSkinLoaded(uuid)) {
                        PlayerSkin playerSkin = SkinManager.getSkin(uuid);
                        TextComponent textComponent = new TextComponent("Texture URL: " + playerSkin.url());
                        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, playerSkin.url()));
                        textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Click to copy")));
                        sender.spigot().sendMessage(textComponent);
                        TextComponent idComponent = new TextComponent("Texture ID: " + playerSkin.id());
                        idComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, playerSkin.id()));
                        idComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Click to copy")));
                        sender.spigot().sendMessage(idComponent);
                        return true;
                    } else {
                        sender.sendMessage("Skin is loading! Please enter the command again.");
                        return false;
                    }
                } else {
                    sender.sendMessage("/bounty skin (player name/uuid)");
                    return false;
                }

            } else if (args[0].equalsIgnoreCase("cleanEntities") && (forcePermission || sender.hasPermission("notbounties.cleanentities"))) {
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
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));
                        return false;
                    }
                }
                RemovePersistentEntitiesEvent.cleanAsync(parser.getNearbyEntities(radius, radius, radius), sender);
                return true;
            } else if (args[0].equalsIgnoreCase("pause") && (forcePermission || adminPermission)) {
                if (NotBounties.isPaused()) {
                    sender.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.RED + "NotBounties is already paused.", parser));
                    return false;
                } else {
                    NotBounties.setPaused(true);
                    sender.sendMessage(parse(getPrefix() + ChatColor.RED + "NotBounties is now paused. Players will only be able to view previous bounties. Run " + ChatColor.GREEN + "/" + ConfigOptions.getPluginBountyCommands().get(0) + " unpause " + ChatColor.RED + " to use all features again.", parser));
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("unpause") && (forcePermission || adminPermission)) {
                if (NotBounties.isPaused()) {
                    NotBounties.setPaused(false);
                    sender.sendMessage(parse(getPrefix() + ChatColor.GREEN + "NotBounties is no longer paused.", parser));
                    return true;
                } else {
                    sender.sendMessage(parse(getPrefix() + ChatColor.GREEN + "NotBounties is already unpaused.", parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("debug") && (forcePermission || adminPermission)) {
                if (args.length == 1) {
                    NotBounties.getInstance().sendDebug(sender);
                } else {
                    UUID uuid = LoggedPlayers.getPlayer(args[1]);
                    if (uuid != null) {
                        sender.sendMessage(DataManager.getPlayerData(uuid).toString());
                        Bounty bounty = BountyManager.getBounty(uuid);
                        if (bounty != null) {
                            sender.sendMessage(bounty.toString());
                        }
                        return true;
                    }
                    boolean newValue;
                    newValue = args[1].equalsIgnoreCase("enable");
                    if (newValue != NotBounties.isDebug()) {
                        NotBounties.setDebug(newValue);
                        if (NotBounties.isDebug()) {
                            if (!silent) {
                                sender.sendMessage(parse(getPrefix() + ChatColor.GREEN + "Debug messages will now be sent in console.", parser));
                            }
                        } else if (!silent) {
                            sender.sendMessage(parse(getPrefix() + ChatColor.RED + "Debug messages will no longer be sent in console.", parser));
                        }
                    } else {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + ChatColor.YELLOW + "Debug mode is already set this way.", parser));
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("board") && (forcePermission || adminPermission)) {
                if (!(sender instanceof Player)) {
                    if (!silent)
                        sender.sendMessage("Only players can use this command!");
                    return false;
                }
                int rank = 0;
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("clear")) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + ChatColor.DARK_RED + "Removed " + BountyBoard.getBountyBoards().size() + " bounty boards.", parser));
                        BountyBoard.clearBoard();

                        return true;
                    }
                    if (args[1].equalsIgnoreCase("remove")) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + ChatColor.RED + "Right click the bounty board to remove.", parser));
                        BountyBoard.getBoardSetup().put(parser.getUniqueId(), -1);
                        return true;
                    }
                    try {
                        rank = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));
                    }
                } else {
                    // get highest rank
                    for (BountyBoard board : BountyBoard.getBountyBoards()) {
                        if (board.getRank() > rank)
                            rank = board.getRank();
                    }
                    rank += 1;
                }
                if (rank < 1)
                    rank = 1;
                if (!silent)
                    sender.sendMessage(parse(getPrefix() + ChatColor.DARK_AQUA + ChatColor.BOLD + "<Rank " + rank + "> " + ChatColor.AQUA + "Punch a block to place the bounty board on.", parser));
                BountyBoard.getBoardSetup().put(parser.getUniqueId(), rank);
                return true;
            } else if (args[0].equalsIgnoreCase("tutorial") && (forcePermission || sender.hasPermission("notbounties.basic.tutorial"))) {
                Tutorial.onCommand(sender, args);
                return true;
            } else if (args[0].equalsIgnoreCase("whitelist") && Whitelist.isEnabled() && (forcePermission || sender.hasPermission("notbounties.whitelist"))) {
                if (sender instanceof Player player) {
                    if (args.length == 1) {
                        openGUI(parser, "set-whitelist", 1);
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("offline")) {
                        openGUI(parser, "set-whitelist", 1, "offline");
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("reset")) {
                        DataManager.getPlayerData(player.getUniqueId()).getWhitelist().setList(new TreeSet<>());
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("whitelist-reset"), parser));
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("view")) {
                        Set<UUID> whitelist = DataManager.getPlayerData(player.getUniqueId()).getWhitelist().getList();
                        StringBuilder names = new StringBuilder(" ");
                        for (UUID uuid : whitelist) {
                            names.append(LoggedPlayers.getPlayerName(uuid)).append(", ");
                        }
                        if (names.length() > 1) {
                            names.replace(names.length() - 2, names.length() - 1, "");
                        } else {
                            names.append("<none>");
                        }
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("whitelisted-players") + names, parser));
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("toggle")) {
                        if (args.length == 2) {
                            // toggle
                            if (Whitelist.isAllowTogglingWhitelist()) {
                                if (DataManager.getPlayerData(player.getUniqueId()).getWhitelist().toggleBlacklist()) {
                                    if (!silent)
                                        sender.sendMessage(parse(getPrefix() + getMessage("blacklist-toggle"), parser));
                                } else {
                                    if (!silent)
                                        sender.sendMessage(parse(getPrefix() + getMessage("whitelist-toggle"), parser));
                                }
                            } else {
                                // unknown command
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                return false;
                            }
                        } else {
                            List<String> toggleOptions = List.of("blacklist", "false", "off", "whitelist", "true", "on");
                            if (toggleOptions.contains(args[2].toLowerCase())) {
                                // set to specific type
                                boolean blacklist =
                                        args[2].equalsIgnoreCase("blacklist")
                                                || args[2].equalsIgnoreCase("false")
                                                || args[2].equalsIgnoreCase("off");
                                boolean change = DataManager.getPlayerData(player.getUniqueId()).getWhitelist().setBlacklist(blacklist);
                                // command is silent if there is no change
                                if (change)
                                    if (blacklist) {
                                        if (!silent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("blacklist-toggle"), parser));
                                    } else {
                                        if (!silent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("whitelist-toggle"), parser));
                                    }
                            } else {
                                // try to find player
                                UUID playerUUID = LoggedPlayers.getPlayer(args[2]);
                                if (playerUUID == null) {
                                    // unknown player
                                    failUnknownPlayer(sender, args[2], silent);
                                    return false;
                                }
                                Whitelist whitelist = DataManager.getPlayerData(player.getUniqueId()).getWhitelist();
                                if (whitelist.getList().remove(playerUUID)) {
                                    if (!silent)
                                        sender.sendMessage(parse(getPrefix() + getMessage("whitelist-change"), parser));
                                } else if (whitelist.getList().size() < 10) {
                                    whitelist.getList().add(playerUUID);
                                    if (!silent)
                                        sender.sendMessage(parse(getPrefix() + getMessage("whitelist-change"), parser));
                                } else {
                                    if (!silent)
                                        sender.sendMessage(parse(getPrefix() + getMessage("whitelist-max"), parser));
                                }
                            }
                        }
                        return true;
                    }
                    if (args.length > 2) {
                        Whitelist playerWhitelist = DataManager.getPlayerData(player.getUniqueId()).getWhitelist();
                        SortedSet<UUID> whitelist = new TreeSet<>();
                        SortedSet<UUID> previousWhitelist = playerWhitelist.getList();
                        for (int i = 2; i < Math.min(args.length, 12); i++) {
                            UUID playerUUID = LoggedPlayers.getPlayer(args[i]);
                            if (playerUUID == null) {
                                // unknown player
                                failUnknownPlayer(sender, args[i], silent);
                                return false;
                            }
                            whitelist.add(playerUUID);
                        }

                        if (args[1].equalsIgnoreCase("add")) {
                            whitelist.stream().filter(uuid -> !previousWhitelist.contains(uuid)).forEach(previousWhitelist::add);
                            while (previousWhitelist.size() > 10)
                                previousWhitelist.remove(previousWhitelist.last());
                            // don't need to set the player's whitelist to previousWhitelist because it is already a reference to it
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("whitelist-change"), parser));
                            if (previousWhitelist.size() == 10 && !silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("whitelist-max"), parser));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("remove")) {
                            if (!previousWhitelist.removeIf(whitelist::contains)) {
                                // nobody removed
                                StringBuilder builder = new StringBuilder(args[2]);
                                for (int i = 3; i < args.length; i++) {
                                    builder.append(", ").append(args[i]);
                                }
                                failUnknownPlayer(sender, builder.toString(), silent);
                                return true;
                            }
                            // don't need to set the player's whitelist to previousWhitelist because it is already a reference to it
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("whitelist-change"), parser));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("set")) {
                            playerWhitelist.setList(whitelist);
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("whitelist-change"), parser));
                            return true;
                        }
                    }
                    // usage
                    if (!silent) {
                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                        sendHelpMessage(sender, getListMessage("help.whitelist"));
                    }
                    return false;
                } else {
                    if (!silent)
                        sender.sendMessage(getPrefix() + ChatColor.RED + "Only players can use this command.");
                }
                return false;
            } else if (args[0].equalsIgnoreCase("currency") && (forcePermission || adminPermission)) {
                CurrencySetup.onCommand(sender, args);
                return true;
            } else if (args[0].equalsIgnoreCase("set")) {
                if ((forcePermission || sender.hasPermission("notbounties.set"))) {
                    if (sender instanceof Player) {
                        if (args.length > 1)
                            openGUI(parser, "set-bounty", 1, args[1]);
                        else
                            openGUI(parser, "set-bounty", 1);
                    } else {
                        if (!silent)
                            sender.sendMessage(getPrefix() + ChatColor.RED + "Only players can use this command.");
                        return false;
                    }
                    return true;
                } else {
                    // no permission
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
            } else if ((args[0].equalsIgnoreCase("bdc") || args[0].equalsIgnoreCase("broadcast")) && (forcePermission || sender.hasPermission("notbounties.basic"))) {
                if (sender instanceof Player player) {
                    PlayerData playerData = DataManager.getPlayerData(player.getUniqueId());
                    if (args.length > 1) {
                        // added arguments
                        if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("enable")) {
                            // enable
                            playerData.setBroadcastSettings(PlayerData.BroadcastSettings.EXTENDED);
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("enable-broadcast"), parser));
                        } else if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("disable")) {
                            // disable
                            playerData.setBroadcastSettings(PlayerData.BroadcastSettings.DISABLE);
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("disable-broadcast"), parser));
                        } else {
                            try {
                                PlayerData.BroadcastSettings broadcastSettings = PlayerData.BroadcastSettings.valueOf(args[1].toUpperCase());
                                playerData.setBroadcastSettings(broadcastSettings);
                                if (!silent) {
                                    if (broadcastSettings == PlayerData.BroadcastSettings.DISABLE) {
                                        // disable
                                        sender.sendMessage(parse(getPrefix() + getMessage("disable-broadcast"), parser));
                                    } else {
                                        // enabled
                                        sender.sendMessage(parse(getPrefix() + getMessage("enable-broadcast"), parser));
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("unknown-command"), parser));
                                return false;
                            }
                        }
                    } else {
                        switch (playerData.getBroadcastSettings()) {
                            case EXTENDED -> {
                                playerData.setBroadcastSettings(PlayerData.BroadcastSettings.SHORT);
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + getMessage("enable-broadcast"), parser));
                            }
                            case SHORT -> {
                                playerData.setBroadcastSettings(PlayerData.BroadcastSettings.DISABLE);
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("disable-broadcast"), parser));
                            }
                            case DISABLE -> {
                                playerData.setBroadcastSettings(PlayerData.BroadcastSettings.EXTENDED);
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + getMessage("enable-broadcast"), parser));
                            }
                        }
                    }
                } else {
                    if (!silent)
                        sender.sendMessage("You can't disable bounty broadcast! (yet?) If you really want to disable, send a message in the discord!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("stat")) {
                if (!(forcePermission || sender.hasPermission("notbounties.stats"))) {
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
                if (args.length == 1) {
                    Leaderboard.ALL.displayStats(parser, false);
                    return true;
                }
                if (args.length > 3 && !(forcePermission || adminPermission) || args.length > 5) {
                    // usage
                    if (!silent) {
                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                        sendHelpMessage(sender, getListMessage("help.stats"));
                    }
                    return false;
                }
                Leaderboard leaderboard;
                try {
                    leaderboard = Leaderboard.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    // more usage
                    if (!silent) {
                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                        sendHelpMessage(sender, getListMessage("help.stats"));
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
                    UUID playerUUID = LoggedPlayers.getPlayer(args[2]);
                    if (playerUUID == null) {
                        // unknown player
                        failUnknownPlayer(sender, args[2], silent);
                        return false;
                    }
                    if (args.length == 3) {
                        leaderboard.displayStats(Bukkit.getOfflinePlayer(playerUUID), parser, true);
                        return true;
                    }
                    // admin part to edit or setValue
                    if (!(forcePermission || adminPermission)) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                        return false;
                    }
                    if (args.length != 5 || (!args[3].equalsIgnoreCase("edit") && !args[3].equalsIgnoreCase("setValue"))) {
                        // usage
                        if (!silent) {
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                            sendHelpMessage(sender, getListMessage("help.admin"));
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
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                        sendHelpMessage(sender, getListMessage("help.admin"));
                        return false;
                    }
                    if (!leaderboard.isMoney())
                        value = (long) value;
                    if (!edit)
                        value = value - leaderboard.getStat(playerUUID);
                    // value is now the value to be added to the stat
                    if (leaderboard == Leaderboard.IMMUNITY) {
                        ImmunityManager.addImmunity(playerUUID, value);
                    } else {
                        DataManager.changeStat(playerUUID, leaderboard, value);
                    }
                    if (!silent) {
                        String amountString = leaderboard.getFormattedStat(playerUUID);
                        sender.sendMessage(parse(getPrefix() + getMessage("update-stat")
                                .replace("{leaderboard_name}", leaderboard.getDisplayName())
                                .replace("{leaderboard}", (leaderboard.toString()))
                                .replace("{amount}", amountString), Bukkit.getOfflinePlayer(playerUUID)));
                    }
                }

                return true;
            } else if (args[0].equalsIgnoreCase("top")) {
                if ((forcePermission || sender.hasPermission("notbounties.stats"))) {
                    Leaderboard leaderboard;
                    if (args.length > 1) {
                        try {
                            leaderboard = Leaderboard.valueOf(args[1].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            if (!silent) {
                                sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                sendHelpMessage(sender, getListMessage("help.stats"));
                            }
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
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if ((forcePermission || adminPermission)) {
                    try {
                        NotBounties.getInstance().loadConfig();
                    } catch (IOException e) {
                        sender.sendMessage(ChatColor.RED + e.toString());
                    }
                    reopenBountiesGUI();
                    if (!silent)
                        sender.sendMessage(parse(getPrefix(), parser) + ChatColor.GREEN + "Reloaded NotBounties version " + NotBounties.getInstance().getDescription().getVersion());
                } else {
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                    return false;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("immunity")) {
                return immunityCommandHandler.handle(sender, args, forcePermission, adminPermission, silent, parser);
            } else if (args[0].equalsIgnoreCase("buy")) {
                if (sender instanceof Player) {
                    if (ConfigOptions.getMoney().isBuyOwn()) {
                        if (forcePermission || sender.hasPermission("notbounties.buyown")) {
                            if (hasBounty(parser.getUniqueId())) {
                                Bounty bounty = getBounty(parser.getUniqueId());
                                assert bounty != null;
                                if ((repeatBuyBountyCommand.containsKey((parser).getUniqueId().toString()) && System.currentTimeMillis() - repeatBuyBountyCommand.get((parser).getUniqueId().toString()) < 30000) || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"))) {
                                    // try to find bounty and buy it
                                    repeatBuyBountyCommand.remove((parser).getUniqueId().toString());
                                    if (checkBalance(parser, (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply()))) {
                                        Bounty bought = new Bounty(bounty);
                                        BountyRemoveEvent event = new BountyRemoveEvent(sender, true, bought);
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (event.isCancelled())
                                            return true;
                                        try {
                                            NumberFormatting.doRemoveCommands(parser, (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply()), new ArrayList<>());
                                            if (NumberFormatting.isBountyItemsBuyItem()) {
                                                NumberFormatting.givePlayer(parser, bounty.getTotalItemBounty(), false);
                                            } else {
                                                for (Setter setter : bounty.getSetters()) {
                                                    if (!setter.getItems().isEmpty())
                                                        BountyManager.refundPlayer(setter.getUuid(), 0, setter.getItems(), LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-remove"), parser));
                                                }
                                            }
                                            BountyManager.removeBounty(bounty.getUUID());
                                            ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.BUY_OWN, 1);
                                            reopenBountiesGUI();
                                            if (!silent)
                                                sender.sendMessage(parse(getPrefix() + getMessage("success-remove-bounty"), parser));
                                            return true;
                                        } catch (NotEnoughCurrencyException e) {
                                            // broke
                                            if (!silent)
                                                sender.sendMessage(parse(getPrefix() + getMessage("broke"), (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply()), parser));
                                            return false;
                                        }
                                    } else {
                                        // broke
                                        if (!silent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("broke"), (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply()), parser));
                                        return false;
                                    }
                                } else {
                                    // open gui
                                    repeatBuyBountyCommand.put(parser.getUniqueId().toString(), System.currentTimeMillis());
                                    GUI.openGUI(parser, "confirm", 1, parser.getUniqueId(), (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply()));
                                    return true;
                                }
                            } else {
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + getMessage("no-bounty"), parser));
                                return false;
                            }
                        } else {
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                            return false;
                        }
                    } else {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                        return false;
                    }
                } else {
                    if (!silent)
                        sender.sendMessage("You don't have a bounty!");
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("check")) {
                if (forcePermission || sender.hasPermission("notbounties.view")) {
                    if (args.length > 1) {
                        UUID pUUID = LoggedPlayers.getPlayer(args[1]);
                        if (pUUID == null) {
                            failUnknownPlayer(sender, args[1], silent);
                            return false;
                        }
                        if (!hasBounty(pUUID)) {
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("no-bounty"), pUUID, parser));
                            return false;
                        }
                        Bounty bounty = getBounty(pUUID);
                        assert bounty != null;
                        double bountyAmount = Whitelist.isShowWhitelistedBounties() || adminPermission || !(sender instanceof Player) ? bounty.getTotalDisplayBounty() : bounty.getTotalDisplayBounty(parser);
                        if (bountyAmount == 0) {
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("no-bounty"), pUUID, parser));
                            return false;
                        }

                        if (args.length > 2) {
                            if (args[2].equalsIgnoreCase("list")) {
                                if (silent)
                                    return true;
                                OfflinePlayer p = Bukkit.getOfflinePlayer(pUUID);
                                sender.sendMessage(parse(getPrefix() + getMessage("check-bounty"), bountyAmount, bounty.getLatestUpdate(), LocalTime.TimeFormat.PLAYER, p));
                                for (Setter setters : bounty.getSetters()) {
                                    if (Whitelist.isShowWhitelistedBounties() || adminPermission || !(sender instanceof Player) || setters.canClaim(parser)) {
                                        if (getMessage("list-setter").contains("{items}") && !setters.getItems().isEmpty()) {
                                            BaseComponent[] components = new BaseComponent[setters.getItems().size() + 2];
                                            components[0] = LanguageOptions.getTextComponent(parse(getPrefix() + getMessage("list-setter").substring(0, getMessage("list-setter").indexOf("{items}")), setters.getDisplayAmount(), setters.getTimeCreated(), LocalTime.TimeFormat.PLAYER, Bukkit.getOfflinePlayer(setters.getUuid())));
                                            BaseComponent[] itemComponents = NumberFormatting.listHoverItems(setters.getItems(), 'x');
                                            System.arraycopy(itemComponents, 0, components, 1, itemComponents.length);
                                            components[components.length - 1] = LanguageOptions.getTextComponent(parse(getMessage("list-setter").substring(getMessage("list-setter").indexOf("{items}") + 7), setters.getDisplayAmount(), setters.getTimeCreated(), LocalTime.TimeFormat.PLAYER, Bukkit.getOfflinePlayer(setters.getUuid())));
                                            sender.spigot().sendMessage(components);
                                        } else {
                                            sender.sendMessage(parse(getPrefix() + getMessage("list-setter").replace("{items}", ""), setters.getDisplayAmount(), setters.getTimeCreated(), LocalTime.TimeFormat.PLAYER, Bukkit.getOfflinePlayer(setters.getUuid())));
                                        }
                                        if (!setters.canClaim(parser))
                                            getListMessage("not-whitelisted").stream().filter(s -> !s.isEmpty()).map(s -> parse(s, p)).forEach(sender::sendMessage);
                                    }
                                }
                                return true;
                            } else {
                                if (!silent) {
                                    sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                    sendHelpMessage(sender, getListMessage("help.view"));
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
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                            sendHelpMessage(sender, getListMessage("help.view"));
                        }
                        return false;
                    }
                } else {
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                if (forcePermission || sender.hasPermission("notbounties.view")) {
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
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (adminPermission) {
                    if (args.length > 1) {
                        UUID bountyUUID = LoggedPlayers.getPlayer(args[1]);
                        if (bountyUUID == null) {
                            failUnknownPlayer(sender, args[1], silent);
                            return false;
                        }
                        Bounty toRemove = DataManager.getGuarrenteedBounty(bountyUUID);
                        if (toRemove != null) {
                            if (args.length == 2) {
                                Bounty bounty = new Bounty(toRemove);
                                BountyRemoveEvent event = new BountyRemoveEvent(sender, false, bounty);
                                Bukkit.getPluginManager().callEvent(event);
                                if (event.isCancelled())
                                    return true;
                                BountyManager.removeBounty(toRemove.getUUID());
                                refundBounty(toRemove, LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-remove"), Bukkit.getOfflinePlayer(bountyUUID)));
                                // successfully removed
                                if (parser != null && parser.getUniqueId().equals(toRemove.getUUID()))
                                    ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.BUY_OWN, 1);
                                WantedTags.removeWantedTag(toRemove.getUUID());
                                OfflinePlayer player = Bukkit.getOfflinePlayer(toRemove.getUUID());
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + getMessage("success-remove-bounty"), player));
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
                                        DataManager.removeSetters(getBounty(toRemove.getUUID()), actualSetters);

                                        refundBounty(bounty, LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-remove"), Bukkit.getOfflinePlayer(bountyUUID)));
                                        // reopen gui for everyone
                                        reopenBountiesGUI();
                                        // successfully removed
                                        if (toRemove.getTotalDisplayBounty() < WantedTags.getMinWanted())
                                            WantedTags.removeWantedTag(toRemove.getUUID());

                                        OfflinePlayer player = Bukkit.getOfflinePlayer(toRemove.getUUID());
                                        if (!silent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("success-remove-bounty"), player));
                                        return true;
                                    } else {
                                        // couldn't find setter
                                        OfflinePlayer player = Bukkit.getOfflinePlayer(toRemove.getUUID());
                                        if (!silent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("no-setter").replace("{player}", args[3]).replace("{receiver}", args[3]), player));
                                        return false;
                                    }

                                } else {
                                    // usage
                                    if (!silent) {
                                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                        sendHelpMessage(sender, getListMessage("help.admin"));
                                    }
                                    return false;
                                }
                            } else {
                                // usage
                                if (!silent) {
                                    sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                    sendHelpMessage(sender, getListMessage("help.admin"));
                                }
                                return false;
                            }
                        } else {
                            // could not find bounty
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("no-bounty"), bountyUUID, parser));
                            return false;
                        }
                    } else {
                        // usage
                        if (!silent) {
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                            sendHelpMessage(sender, getListMessage("help.admin"));
                        }
                        return false;
                    }
                } else if (forcePermission || sender.hasPermission("notbounties.removeset") && sender instanceof Player) {
                    if (args.length != 2) {
                        // usage
                        if (!silent) {
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                            sendHelpMessage(sender, getListMessage("help.remove-set"));
                        }
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
                            sender.sendMessage(parse(getPrefix() + getMessage("no-bounty").replace("{player}", args[1]).replace("{receiver}", args[1]), parser));
                        return false;
                    }
                    UUID senderUUID = parser != null ? parser.getUniqueId() : DataManager.GLOBAL_SERVER_ID;
                    Setter actualRemove = null;
                    for (Setter setter : toRemove.getSetters()) {
                        if (setter.getUuid().equals(senderUUID)) {
                            actualRemove = setter;
                            break;
                        }
                    }

                    if (actualRemove == null) {
                        // couldnt find setter
                        OfflinePlayer player = Bukkit.getOfflinePlayer(toRemove.getUUID());
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("no-setter"), senderUUID, player));
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
                    refundSetter(actualRemove, LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-remove"), Bukkit.getOfflinePlayer(toRemove.getUUID())));
                    // reopen gui for everyone
                    reopenBountiesGUI();
                    // successfully removed
                    if (toRemove.getTotalDisplayBounty() < WantedTags.getMinWanted())
                        WantedTags.removeWantedTag(toRemove.getUUID());

                    OfflinePlayer player = Bukkit.getOfflinePlayer(toRemove.getUUID());
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("success-remove-bounty"), player));
                    return true;
                } else {
                    // no permission
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("edit")) {
                if (adminPermission) {
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
                                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));

                                    return false;
                                }
                                Bounty before = new Bounty(toEdit);
                                Bounty after = new Bounty(toEdit);
                                after.addBounty(amount - toEdit.getTotalDisplayBounty(), new ArrayList<>(), new Whitelist(new TreeSet<>(), false));
                                BountyEditEvent event = new BountyEditEvent(sender, before, after);
                                Bukkit.getPluginManager().callEvent(event);
                                if (event.isCancelled())
                                    return true;
                                if (BountyManager.editBounty(toEdit, null, amount)) {
                                    // successfully edited bounty
                                    if (!silent)
                                        sender.sendMessage(parse(getPrefix() + getMessage("success-edit-bounty"), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                } else {
                                    // unsuccessful edit
                                    if (!silent)
                                        sender.sendMessage(parse(getPrefix() + getMessage("no-bounty"), Bukkit.getOfflinePlayer(toEdit.getUUID())));
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
                                                sender.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));
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
                                                sender.sendMessage(parse(getPrefix() + getMessage("success-edit-bounty"), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                        } else {
                                            // unsuccessful edit
                                            if (!silent)
                                                sender.sendMessage(parse(getPrefix() + getMessage("no-bounty"), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                        }
                                        return true;
                                    } else {
                                        // couldnt find setter
                                        if (!silent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("no-setter").replace("{player}", args[3]).replace("{receiver}", args[3]), Bukkit.getOfflinePlayer(toEdit.getUUID())));
                                        return false;
                                    }
                                } else {
                                    // usage
                                    if (!silent) {
                                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                        sendHelpMessage(sender, getListMessage("help.admin"));
                                    }
                                    return false;
                                }
                            } else {
                                // usage
                                if (!silent) {
                                    sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                    sendHelpMessage(sender, getListMessage("help.admin"));
                                }
                                return false;
                            }
                        } else {
                            // couldn't find bounty
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("no-bounty").replace("{player}", args[1]).replace("{receiver}", args[1]), parser));
                            return false;

                        }
                    } else {
                        // usage
                        if (!silent) {
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                            sendHelpMessage(sender, getListMessage("help.admin"));
                        }
                        return false;
                    }
                } else {
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("poster") && BountyMap.isEnabled()) {
                return posterCommandHandler.handle(sender, args, forcePermission, adminPermission, silent, parser);
            } else if (args[0].equalsIgnoreCase("tracker")) {
                return trackerCommandHandler.handle(sender, args, forcePermission, adminPermission, silent, parser);
            } else {
                if (forcePermission || sender.hasPermission("notbounties.set")) {
                    UUID playerUUID = LoggedPlayers.getPlayer(args[0]);
                    if (playerUUID == null || (!ConfigOptions.isOfflineSet() && !NotBounties.getNetworkPlayers().containsKey(playerUUID))) {
                        // can't find player
                        if (args.length == 1) {
                            if (!silent) {
                                sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                                sendHelpMessage(sender, getListMessage("help.set"));
                            }
                        } else {
                            failUnknownPlayer(sender, args[0], silent);
                        }
                        return false;
                    }
                    if (ImmunityManager.getBountyCooldown() > 0 && sender instanceof Player player1) {
                        // bounty cooldown
                        long sinceLastSet = System.currentTimeMillis() - DataManager.getPlayerData(player1.getUniqueId()).getBountyCooldown();
                        if (sinceLastSet < ImmunityManager.getBountyCooldown() * 1000L) {
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("bounty-set-cooldown"), ImmunityManager.getBountyCooldown() * 1000L - sinceLastSet, LocalTime.TimeFormat.RELATIVE, parser));
                            return false;
                        }
                    }
                    if (BountyTracker.isTrackingExemptEnabled() && !BountyTracker.isTrackingExemptAllowBountySetting() && sender instanceof Player player2 && DataManager.getPlayerData(player2.getUniqueId()).isTrackingExempt()) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("tracker-exempt-bounty"), parser));
                        return false;
                    }

                    if (!ConfigOptions.isSelfSetting() && sender instanceof Player p && p.getUniqueId().equals(playerUUID)) {
                        // own bounty
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + getMessage("self-set-deny"), parser));
                        return false;
                    }
                    if (args.length == 1) {
                        if (sender instanceof Player) {
                            if (NumberFormatting.isBountyItemsDefaultGUI()) {
                                openGUI(parser, "bounty-item-select", 1, playerUUID);
                            } else {
                                openGUI(parser, "select-price", (long) ConfigOptions.getMoney().getMinBounty(), playerUUID.toString());
                            }
                        }
                        return true;
                    }

                    // get whitelisted people
                    Whitelist whitelist = (sender instanceof Player player) && (forcePermission || sender.hasPermission("notbounties.whitelist")) && Whitelist.isEnabled() ? DataManager.getPlayerData(player.getUniqueId()).getWhitelist() : new Whitelist(new TreeSet<>(), false);
                    if (args.length > 3 && (forcePermission || sender.hasPermission("notbounties.whitelist"))) {
                        SortedSet<UUID> newWhitelist = new TreeSet<>();
                        for (int i = 3; i < Math.min(args.length, 13); i++) {
                            if (args[i].equalsIgnoreCase("--confirm"))
                                continue;
                            UUID p = LoggedPlayers.getPlayer(args[i]);
                            if (p == null) {
                                // unknown player
                                failUnknownPlayer(sender, args[i], silent);
                                return false;
                            }
                            newWhitelist.add(p);
                        }
                        whitelist.setList(newWhitelist);
                    }

                    // check if max setters reached or max bounty
                    double currentBounty = 0;
                    if (ConfigOptions.getMaxSetters() > -1 || ConfigOptions.getMoney().getMaxBounty() > 0) {
                        if (hasBounty(playerUUID)) {
                            Bounty bounty = getBounty(playerUUID);
                            assert bounty != null;
                            currentBounty = bounty.getTotalDisplayBounty();
                            if (ConfigOptions.getMaxSetters() > -1 && bounty.getSetters().size() >= ConfigOptions.getMaxSetters()) {
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("max-setters"), playerUUID, parser));
                                return false;
                            } else if (ConfigOptions.getMoney().getMaxBounty() > 0 && bounty.getTotalDisplayBounty() > ConfigOptions.getMoney().getMaxBounty()) {
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("max-bounty"), playerUUID, ConfigOptions.getMoney().getMaxBounty(), parser));
                                return false;
                            }

                        }
                    }


                    // check if we can get the amount
                    double amount = 0;
                    Map<Material, Integer> requestedItems = new EnumMap<>(Material.class);
                    List<ItemStack> items = new ArrayList<>();
                    boolean usingGUI = false;
                    if (NumberFormatting.getBountyItemMode() == BountyItemMode.ALLOW || NumberFormatting.getBountyItemMode() == BountyItemMode.EXCLUSIVE) {
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
                                        sender.sendMessage(parse(getPrefix() + getMessage("unknown-material").replace("{material}", (materialString)), parser));
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
                                    sender.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));
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
                            if (sender instanceof Player player) {
                                // check if they have the bounty-item-select GUI open
                                if (GUI.playerInfo.containsKey(player.getUniqueId())) {
                                    PlayerGUInfo info = GUI.playerInfo.get(player.getUniqueId());
                                    if (info.guiType().equals("bounty-item-select")) {
                                        usingGUI = true;
                                        // get items from data
                                        if (info.data().length > 1 && info.data()[1] instanceof ItemStack[][] allItems) {
                                            // add all items to items arraylist
                                            Arrays.stream(allItems).forEach(itemStacks -> Arrays.stream(itemStacks).filter(Objects::nonNull).forEach(items::add));
                                        }
                                    }
                                }
                                if (!usingGUI) {
                                    ItemStack[] contents = player.getInventory().getContents();
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
                                            if (!brokeAmount.isEmpty())
                                                brokeAmount.append(",");
                                            brokeAmount.append(missingItems.getKey().name()).append("x").append(missingItems.getValue());
                                        }
                                        // send message
                                        if (!silent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("broke").replace("{amount}", (brokeAmount.toString())), parser));
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
                            try {
                                amount = NumberFormatting.getTotalValue(items);
                            } catch (ExcludedItemException e) {
                                sender.sendMessage(parse(getPrefix() + getMessage("excluded-bounty-item").replace("{material}", e.getMessage()), parser));
                                if (usingGUI)
                                    GUI.safeCloseGUI(parser, false);
                                return false;
                            }
                        }
                    }

                    if (NumberFormatting.getBountyItemMode() != BountyItemMode.EXCLUSIVE) {
                        if (items.isEmpty())
                            try {
                                amount = tryParse(args[1]);
                            } catch (NumberFormatException ignored) {
                                if (!silent)
                                    sender.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));
                                return false;
                            }
                    } else if (items.isEmpty()) {
                        // exclusive mode is enabled but player didn't specify any items
                        // unknown command
                        if (!silent) {
                            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                            sendHelpMessage(sender, getListMessage("help.set"));
                        }
                        return true;
                    }

                    if (amount < ConfigOptions.getMoney().getMinBounty() && (items.isEmpty() || NumberFormatting.getBountyItemsUseItemValues() != ItemValueMode.DISABLE)) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("min-bounty"), ConfigOptions.getMoney().getMinBounty(), parser));
                        return false;
                    }
                    if (ConfigOptions.getMoney().getMaxBounty() > 0 && amount + currentBounty > ConfigOptions.getMoney().getMaxBounty()) {
                        if (!silent)
                            sender.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("max-bounty"), ConfigOptions.getMoney().getMaxBounty(), parser));
                        return false;
                    }
                    // total cost to place this bounty in currency
                    double total = amount * ConfigOptions.getMoney().getBountyTax() + whitelist.getList().size() * Whitelist.getCost();
                    if (items.isEmpty())
                        total += amount;  // this includes the bounty if no items are being set

                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                    // check if it is a player
                    if (sender instanceof Player) {
                        // check for immunity
                        if (checkAndNotifyImmunity(sender, amount, silent, player, items)) {
                            if (usingGUI)
                                GUI.safeCloseGUI(parser, false);
                            return false;
                        }

                        if (checkBalance(parser, total)) {
                            if (!args[args.length - 1].equalsIgnoreCase("--confirm") && ConfigOptions.isBountyConfirmation()) {
                                // if there are items, open set-bounty-item gui
                                if (!items.isEmpty()) {
                                    // format items to take up multiple pages if necessary
                                    ItemStack[][] serializedItems = NumberFormatting.separateItems(new ArrayList<>(items), GUI.getMaxBountyItemSlots());

                                    // take items from player if not using the GUI
                                    if (!usingGUI) {
                                        NumberFormatting.removeItems(parser, new ArrayList<>(items), true);
                                    }
                                    // open gui
                                    openGUI(parser, "bounty-item-select", 1, playerUUID, serializedItems);
                                } else {
                                    openGUI(parser, "confirm-bounty", (long) amount, playerUUID, (long) amount);
                                }
                                return true;
                            }
                            double finalAmount = amount;
                            double finalTotal = total;
                            boolean finalUsingGUI = usingGUI;
                            boolean finalSilent = silent;
                            boolean finalSilent1 = silent;
                            NotBounties.getServerImplementation().async().runNow(() -> {
                                // async ban check
                                if (!player.isOnline() && ConfigOptions.isRemoveBannedPlayers() && BanChecker.isPlayerBanned(player)) {
                                    NotBounties.getServerImplementation().global().run(() -> {
                                        // has permanent immunity
                                        if (!finalSilent)
                                            sender.sendMessage(parse(getPrefix() + getMessage("permanent-immunity"), ImmunityManager.getImmunity(playerUUID), player));
                                    });
                                } else {
                                    NotBounties.getServerImplementation().global().run(() -> {
                                        if (!items.isEmpty()) {
                                            // check to see if player is still online after the async check
                                            if (!parser.isOnline())
                                                return;
                                            String result = "";
                                            if (finalUsingGUI && GUI.playerInfo.containsKey(parser.getUniqueId()) && GUI.playerInfo.get(parser.getUniqueId()).guiType().equals("bounty-item-select")) {
                                                if (NumberFormatting.getManualEconomy() != ManualEconomy.AUTOMATIC) {
                                                    // give items back
                                                    GUI.safeCloseGUI(parser, false);
                                                } else {
                                                    // erase data from gui
                                                    GUI.playerInfo.remove(parser.getUniqueId());
                                                    parser.closeInventory();
                                                }
                                            } else {
                                                result = NumberFormatting.removeItems(parser, new ArrayList<>(items), NumberFormatting.getManualEconomy() == ManualEconomy.AUTOMATIC);
                                            }

                                            if (!result.isEmpty()) {
                                                // didn't have all the items
                                                if (!finalSilent)
                                                    sender.sendMessage(parse(getPrefix() + getMessage("broke").replace("{amount}", result), parser));
                                                return;
                                            }
                                        }
                                        try {
                                            if (NumberFormatting.getManualEconomy() != ManualEconomy.PARTIAL)
                                                NumberFormatting.doRemoveCommands(parser, finalTotal, new ArrayList<>());
                                            addBounty(parser, player, finalAmount, items, whitelist);
                                            reopenBountiesGUI();
                                        } catch (NotEnoughCurrencyException e) {
                                            if (!finalSilent1)
                                                sender.sendMessage(parse(getPrefix() + getMessage("broke"), finalTotal, parser));
                                        }
                                    });
                                }
                            });
                        } else {
                            if (!silent)
                                sender.sendMessage(parse(getPrefix() + getMessage("broke"), total, parser));
                            return false;
                        }

                    } else {
                        addBounty(player, amount, items, whitelist);
                        reopenBountiesGUI();
                    }

                } else {
                    if (!silent)
                        sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
                    return false;
                }
            }
        } else {
            // open gui
            if (sender instanceof Player) {
                if ((forcePermission || sender.hasPermission("notbounties.view"))) {
                    openGUI(parser, "bounty-gui", 1);
                } else {
                    if (!silent)
                        sender.sendMessage(parse(getMessage("no-permission"), parser));
                    return false;
                }
            } else {
                if (adminPermission) {
                    NotBounties.getInstance().sendDebug(sender);
                }
            }
        }
        return true;
    }

    public static boolean checkAndNotifyImmunity(@NotNull CommandSender sender, double amount, boolean silent, OfflinePlayer player, List<ItemStack> items) {
        switch (ImmunityManager.getAppliedImmunity(player.getUniqueId(), amount)) {
            case GRACE_PERIOD:
                if (!silent)
                    sender.sendMessage(parse(getPrefix()
                            + LanguageOptions.getMessage("grace-period")
                            .replace("{time}", (LocalTime.formatTime(
                                    ImmunityManager.getGracePeriod(player.getUniqueId())
                                    , LocalTime.TimeFormat.RELATIVE))), player));
                break;
            case NEW_PLAYER:
                long immunityMS = (long) ((ImmunityManager.getNewPlayerImmunity() - ((double) player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20)) * 1000L);
                if (!silent)
                    sender.sendMessage(parse(getPrefix()
                            + LanguageOptions.getMessage("new-player-immunity")
                            .replace("{time}", (LocalTime.formatTime(
                                    immunityMS,
                                    LocalTime.TimeFormat.RELATIVE))), player));
                if (immunityMS <= 0) {
                    // does not have new player immunity anymore
                    DataManager.getPlayerData(player.getUniqueId()).setNewPlayer(false);
                    Bukkit.getLogger().info("immunity changed");
                    return false;
                }
                break;
            case PERMANENT:
                if (NumberFormatting.isBountyItemsOverrideImmunity() && !items.isEmpty())
                    break;
                if (!silent)
                    sender.sendMessage(parse(getPrefix() + getMessage("permanent-immunity"), ImmunityManager.getImmunity(player.getUniqueId()), player));
                break;
            case SCALING:
                if (NumberFormatting.isBountyItemsOverrideImmunity() && !items.isEmpty())
                    break;
                if (!silent)
                    sender.sendMessage(parse(getPrefix() + getMessage("scaling-immunity"), ImmunityManager.getImmunity(player.getUniqueId()), player));
                break;
            case TIME:
                if (NumberFormatting.isBountyItemsOverrideImmunity() && !items.isEmpty())
                    break;
                if (!silent)
                    sender.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("time-immunity").replace("{time}", (LocalTime.formatTime(ImmunityManager.getTimeImmunity(player.getUniqueId()), LocalTime.TimeFormat.RELATIVE))), ImmunityManager.getImmunity(player.getUniqueId()), player));
                break;
            default:
                // Not using immunity
                return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> tab = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("notbounties") || command.getName().equalsIgnoreCase("notbountiesadmin")) {
            boolean adminPermission = sender.hasPermission(NotBounties.getAdminPermission());
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
            if (command.getName().equalsIgnoreCase("notbountiesadmin")) {
                if (adminPermission) {
                    if (args.length == 1) {
                        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                    } else {
                        String[] tempArgs = args;
                        args = new String[tempArgs.length - 1];
                        System.arraycopy(tempArgs, 1, args, 0, tempArgs.length - 1);
                    }
                } else {
                    return tab;
                }
            }
            posterCommandHandler.tabComplete(sender, args, tab, adminPermission);
            trackerCommandHandler.tabComplete(sender, args, tab, adminPermission);
            immunityCommandHandler.tabComplete(sender, args, tab, adminPermission);
            if (args.length == 1) {
                if (sender.hasPermission("notbounties.basic")) {
                    tab.add("help");
                    tab.add("bdc");
                    tab.add("broadcast");
                }
                if (BountyHunt.isEnabled() && (sender.hasPermission("notbounties.hunt.start") || sender.hasPermission("notbounties.hunt.participate")))
                    tab.add("hunt");
                if (sender.hasPermission("notbounties.basic.tutorial")) {
                    tab.add("tutorial");
                }
                if (sender.hasPermission("notbounties.set")) {
                    for (Map.Entry<UUID, String> entry : NotBounties.getNetworkPlayers().entrySet()) {
                        if (!ConfigOptions.isSelfSetting() && sender instanceof Player player && player.getUniqueId().equals(entry.getKey()))
                            continue;
                        tab.add(entry.getValue());
                    }
                    tab.add("set");
                }
                if (sender.hasPermission("notbounties.view")) {
                    tab.add("check");
                    tab.add("list");
                }
                if (sender.hasPermission("notbounties.stats")) {
                    tab.add("top");
                    tab.add("stat");
                }
                if (sender.hasPermission("notbounties.cleanentities")) {
                    tab.add("cleanEntities");
                }
                if (sender.hasPermission("notbounties.sort")) {
                    tab.add("sort");
                }
                if (adminPermission) {
                    tab.add("remove");
                    tab.add("edit");
                    tab.add("reload");
                    tab.add("currency");
                    tab.add("debug");
                    tab.add("board");
                    tab.add("skin");
                    tab.add("item");
                    if (NotBounties.isPaused()) {
                        tab.add("unpause");
                    } else {
                        tab.add("pause");
                    }
                } else if (sender.hasPermission("notbounties.removeset")) {
                    tab.add("remove");
                }
                if (sender.hasPermission("notbounties.buyown") && ConfigOptions.getMoney().isBuyOwn()) {
                    tab.add("buy");
                }
                if (sender.hasPermission("notbounties.whitelist") && Whitelist.isEnabled()) {
                    tab.add("whitelist");
                }
                if (sender.hasPermission("notbounties.challenges") && ChallengeManager.isEnabled())
                    tab.add("challenges");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("check") && sender.hasPermission("notbounties.view")) {
                    List<Bounty> bountyList = BountyManager.getPublicBounties(-1);
                    if (bountyList.size() <= ConfigOptions.getMaxTabCompletePlayers()) {
                        for (Bounty bounty : bountyList) {
                            tab.add(bounty.getName());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("sort") && sender.hasPermission("notbounties.sort")) {
                    tab.addAll(GUI.getGUITypes());
                } else if (args[0].equalsIgnoreCase("hunt") && BountyHunt.isEnabled()) {
                    if (sender.hasPermission("notbounties.hunt.start")) {
                        List<Bounty> bountyList = BountyManager.getPublicBounties(-1);
                        if (bountyList.size() <= ConfigOptions.getMaxTabCompletePlayers()) {
                            for (Bounty bounty : bountyList) {
                                tab.add(bounty.getName());
                            }
                        }
                    }
                    if (sender.hasPermission("notbounties.hunt.participate") && sender instanceof Player player) {
                        tab.add("list");
                        int participatingHunts = BountyHunt.getParticipatingHunts(player.getUniqueId()).size();
                        if (participatingHunts > 0)
                            tab.add("leave");
                        if ((participatingHunts < BountyHunt.getMaxJoinableHunts() || BountyHunt.getMaxJoinableHunts() <= 0) && !BountyHunt.getHunts().isEmpty())
                            tab.add("join");
                    }
                    if (adminPermission) {
                        tab.add("cancel");
                    }
                } else if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit") && adminPermission)) {
                    List<Bounty> bountyList = BountyManager.getAllBounties(-1);
                    if (bountyList.size() <= ConfigOptions.getMaxTabCompletePlayers()) {
                        for (Bounty bounty : bountyList) {
                            tab.add(bounty.getName());
                        }
                    }
                } else if ((args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("stat")) && sender.hasPermission("notbounties.stats")) {
                    tab.add("all");
                    tab.add("kills");
                    tab.add("claimed");
                    tab.add("deaths");
                    tab.add("set");
                    tab.add("immunity");
                    tab.add("current");
                } else if (args[0].equalsIgnoreCase("item") && adminPermission) {
                        tab.addAll(getCustomItems().keySet());
                } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("notbounties.set")) {
                    tab.add("offline");
                } else if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist") && Whitelist.isEnabled()) {
                    tab.add("add");
                    tab.add("remove");
                    tab.add("set");
                    tab.add("offline");
                    tab.add("reset");
                    tab.add("view");
                    tab.add("toggle");
                } else if (args[0].equalsIgnoreCase("board") && adminPermission) {
                    tab.add("clear");
                    tab.add("remove");
                } else if (args[0].equalsIgnoreCase("debug") && adminPermission) {
                    if (NotBounties.isDebug())
                        tab.add("disable");
                    else
                        tab.add("enable");
                } else if (args[0].equalsIgnoreCase("bdc") && sender.hasPermission("notbounties.basic")) {
                    tab.add("disable");
                    tab.add("extended");
                    tab.add("short");
                } else if (args[0].equalsIgnoreCase("challenges") && sender.hasPermission("notbounties.challenges") && ChallengeManager.isEnabled()) {
                    tab.add("claim");
                    tab.add("check");
                } else if (!args[1].isEmpty() && tab.isEmpty() && sender instanceof Player player && NumberFormatting.getBountyItemMode() != BountyItemMode.DENY && NumberFormatting.isTabCompleteItems()) {
                    // They have started typing
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
                if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit")) && adminPermission) {
                    tab.add("from");
                } else if (args[0].equalsIgnoreCase("sort") && sender.hasPermission("notbounties.sort")) {
                    tab.add("toggle");
                } else if (args[0].equalsIgnoreCase("hunt") && BountyHunt.isEnabled()) {
                    if (args[1].equalsIgnoreCase("cancel") && adminPermission) {
                        for (BountyHunt hunt : BountyHunt.getHunts()) {
                            tab.add(LoggedPlayers.getPlayerName(hunt.getHuntedPlayer()));
                        }
                    }
                    if (sender.hasPermission("notbounties.hunt.participate") && sender instanceof Player player) {
                        if (args[1].equalsIgnoreCase("leave")) {
                            for (BountyHunt bountyHunt : BountyHunt.getParticipatingHunts(player.getUniqueId())) {
                                tab.add(LoggedPlayers.getPlayerName(bountyHunt.getHuntedPlayer()));
                            }
                        } else if (args[1].equalsIgnoreCase("list")) {
                            for (BountyHunt bountyHunt : BountyHunt.getHunts()) {
                                tab.add(LoggedPlayers.getPlayerName(bountyHunt.getHuntedPlayer()));
                            }
                        } else if (args[1].equalsIgnoreCase("join")) {
                            Set<UUID> participatingHunts = BountyHunt.getParticipatingHunts(player.getUniqueId()).stream().map(bountyHunt -> bountyHunt.getHuntedPlayer().getUniqueId()).collect(Collectors.toSet());
                            for (BountyHunt bountyHunt : BountyHunt.getHunts()) {
                                if (bountyHunt.getHuntedPlayer().getUniqueId() != player.getUniqueId() && !participatingHunts.contains(bountyHunt.getHuntedPlayer().getUniqueId())) {
                                    tab.add(LoggedPlayers.getPlayerName(bountyHunt.getHuntedPlayer()));
                                }
                            }
                        }
                    }
                } else if (args[0].equalsIgnoreCase("top") && sender.hasPermission("notbounties.view")) {
                    tab.add("list");
                } else if (args[0].equalsIgnoreCase("check") && sender.hasPermission("notbounties.view")) {
                    tab.add("list");
                } else if (args[0].equalsIgnoreCase("stat") && sender.hasPermission("notbounties.stats")) {
                    for (Map.Entry<UUID, String> entry : NotBounties.getNetworkPlayers().entrySet()) {
                        if (entry.getValue().length() < 25)
                            tab.add(entry.getValue());
                    }
                } else if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist") && Whitelist.isEnabled() && Whitelist.isAllowTogglingWhitelist() && args[1].equalsIgnoreCase("toggle")) {
                    tab.add("blacklist");
                    tab.add("whitelist");
                } else if (args[0].equalsIgnoreCase("challenges") && ChallengeManager.isEnabled()) {
                    for (int i = 0; i < ChallengeManager.getConcurrentChallenges(); i++) {
                        tab.add((i + 1) + "");
                    }
                }
            } else if (args.length == 4) {
                if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit")) && adminPermission) {
                    List<Bounty> bountyList = BountyManager.getAllBounties(-1);
                    if (bountyList.size() <= ConfigOptions.getMaxTabCompletePlayers()) {
                        for (Bounty bounty1 : bountyList) {
                            if (bounty1.getName().equalsIgnoreCase(args[1])) {
                                for (Setter setter : bounty1.getSetters()) {
                                    tab.add(setter.getName());
                                }
                                break;
                            }
                        }
                    }
                } else if (args[0].equalsIgnoreCase("stat") && adminPermission) {
                    tab.add("edit");
                    tab.add("setValue");
                }
            }
            if (args.length > 2) {
                boolean isPlayer = LoggedPlayers.isLogged(args[0]);
                if ((sender.hasPermission("notbounties.set") && isPlayer) || (args[0].equalsIgnoreCase("whitelist") && !args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.whitelist")))
                    for (Map.Entry<UUID, String> entry : NotBounties.getNetworkPlayers().entrySet()) {
                        if (entry.getValue().length() < 25)
                            tab.add(entry.getValue());
                    }
                if (sender.hasPermission("notbounties.set") && isPlayer && ConfigOptions.isBountyConfirmation())
                    tab.add("--confirm");
                if (args[0].equalsIgnoreCase("whitelist") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("notbounties.whitelist") && sender instanceof Player player) {
                    for (UUID uuid : DataManager.getPlayerData(player.getUniqueId()).getWhitelist().getList()) {
                        String name = LoggedPlayers.getPlayerName(uuid);
                        if (name.length() < 25)
                            tab.add(name);
                    }
                }
            }

            String typed = args[args.length - 1];
            tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            if (tab.isEmpty() && LoggedPlayers.getLoggedPlayers().size() <= ConfigOptions.getMaxTabCompletePlayers()) {
                immunityCommandHandler.addEmptyTabFallback(args, tab, adminPermission);
                if (args.length == 1 || args.length > 2) {
                    if (sender.hasPermission("notbounties.set") && ConfigOptions.isOfflineSet()) {
                        for (Map.Entry<UUID, String> entry : LoggedPlayers.getLoggedPlayers().entrySet()) {
                            if (!ConfigOptions.isSelfSetting() && sender instanceof Player player && player.getUniqueId().equals(entry.getKey()))
                                continue;
                            tab.add(entry.getValue());
                        }
                    }
                }
                if (args.length == 3) {
                    if ((args[0].equalsIgnoreCase("stat") && sender.hasPermission("notbounties.stats"))) {
                        tab.addAll(LoggedPlayers.getLoggedPlayers().values());
                    }
                }
                if (args.length > 2) {
                    if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("notbounties.whitelist")) {
                        tab.addAll(LoggedPlayers.getLoggedPlayers().values());
                    }
                }
                tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            }

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
        if (sender instanceof Player player)
            return player;
        return null;
    }

    private static void failUnknownPlayer(CommandSender sender, String player, boolean silent) {
        if (!silent)
            sender.sendMessage(parse(getPrefix() + getMessage("unknown-player").replace("{player}", player).replace("{receiver}", player), getParser(sender)));
    }

    @FunctionalInterface
    interface UnknownPlayerHandler {
        void failUnknownPlayer(CommandSender sender, String player, boolean silent);
    }
}

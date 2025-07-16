package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ActionCommands;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.money.NotEnoughCurrencyException;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.Commands;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.*;

import static me.jadenp.notbounties.features.LanguageOptions.*;
import static me.jadenp.notbounties.features.LanguageOptions.getListMessage;

// admin command to extend a hunt time, or cancel a hunt
// tab complete duration
// tab complete leave/join/list for players -
// don't need to specify player when leaving a bounty hunt if you only have one -
// use proper capitalization in messages -
// cannot use GUI
// view hunts GUI
public class BountyHunt {
    // config options
    private static boolean enabled;
    private static double costPerMinute;
    private static int minimumMinutes;
    private static boolean addCostToBounty;
    private static boolean giveTrackers;
    private static boolean removeOldTrackers;
    private static boolean pauseOtherBounties;
    private static String bossBarText;
    private static boolean bossBarEnabled;
    private static BarColor bossBarColor;
    private static int maxConcurrentHunts;
    private static int maxJoinableHunts;
    private static List<String> commands;

    private static List<BountyHunt> hunts = new ArrayList<>();
    private static NamespacedKey huntKey = null;

    public static void loadSavedHunts(List<BountyHunt> hunts) {
        BountyHunt.hunts = hunts;
    }

    public static void loadConfiguration(ConfigurationSection config) {
        if (huntKey == null)
            huntKey = new NamespacedKey(NotBounties.getInstance(), "hunt");

        enabled = config.getBoolean("enabled", false);
        costPerMinute = config.getDouble("cost-per-minute", 0);
        minimumMinutes = config.getInt("minimum-minutes", 0);
        addCostToBounty = config.getBoolean("add-cost-to-bounty", true);
        giveTrackers = config.getBoolean("give-trackers", true);
        removeOldTrackers = config.getBoolean("remove-old-trackers", true);
        pauseOtherBounties = config.getBoolean("pause-other-bounties", false);
        commands = config.getStringList("commands");

        boolean oldBossBarEnabled = bossBarText == null ? config.getBoolean("bossbar.enabled") : bossBarEnabled;
        bossBarEnabled = config.getBoolean("bossbar.enabled");
        if (bossBarEnabled && !oldBossBarEnabled) {
            // add players to bossbar in active hunts
            for (BountyHunt hunt : hunts) {
                hunt.enableBossBar();
            }
        } else if (!bossBarEnabled && oldBossBarEnabled) {
            // remove players from bossbar in active hunts
            for (BountyHunt hunt : hunts) {
                hunt.disableBossBar();
            }
        }

        bossBarColor = BarColor.valueOf(config.getString("bar.color", "RED").toUpperCase());
        bossBarText = config.getString("bossbar.text");

        maxConcurrentHunts = config.getInt("max-concurrent-hunts", 0);
        maxJoinableHunts = config.getInt("max-joinable-hunts", 0);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double getCostPerMinute() {
        return costPerMinute;
    }

    public static int getMinimumMinutes() {
        return minimumMinutes;
    }

    public static boolean isPauseOtherBounties() {
        return pauseOtherBounties;
    }

    public static boolean isAddCostToBounty() {
        return addCostToBounty;
    }

    public static boolean isRemoveOldTrackers() {
        return removeOldTrackers;
    }

    public static NamespacedKey getHuntKey() {
        return huntKey;
    }

    public static List<BountyHunt> getHunts() {
        return hunts;
    }

    public static boolean isHunted(Player player) {
        for (BountyHunt hunt : hunts) {
            if (hunt.getHuntedPlayer().getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public static void endHunt(UUID uuid) {
        ListIterator<BountyHunt> huntListIterator = hunts.listIterator();
        while (huntListIterator.hasNext()) {
            BountyHunt hunt = huntListIterator.next();
            if (hunt.getHuntedPlayer().getUniqueId().equals(uuid)) {
                hunt.endHunt();
                huntListIterator.remove();
            }
        }
    }

    public static void login(Player player) {
        for (BountyHunt hunt : hunts) {
            hunt.onJoin(player);
        }
    }

    public static void logout(Player player) {
        for (BountyHunt hunt : hunts) {
            hunt.onQuit(player);
        }
    }

    public static void updateBossBars() {
        for (BountyHunt hunt : hunts) {
            hunt.updateBossBar();
        }
    }

    public static void claimBounty(Player receiver, Player killer) {
        ListIterator<BountyHunt> huntListIterator = hunts.listIterator();
        while (huntListIterator.hasNext()) {
            BountyHunt hunt = huntListIterator.next();
            // check if player is being hunted
            if (hunt.getHuntedPlayer().getUniqueId().equals(receiver.getUniqueId())) {
                // end hunt, run commands
                hunt.endHunt();
                ActionCommands.executeCommands(receiver, killer, commands);
                huntListIterator.remove();
            }
        }
    }

    public static @Nullable BountyHunt getHunt(UUID huntedUUID) {
        for (BountyHunt hunt : hunts)
            if (hunt.getHuntedPlayer().getUniqueId().equals(huntedUUID))
                return hunt;
        return null;
    }


    /**
     * Get the hunt that the uuid is participating in.
     * @param participatingUUID The uuid of the player participating in a hunt.
     * @return The list of hunts that the player is participating in.
     */
    public static List<BountyHunt> getParticipatingHunts(UUID participatingUUID) {
        List<BountyHunt> participatingHunts = new ArrayList<>();
        for (BountyHunt hunt : hunts) {
            if (hunt.isParticipating(participatingUUID))
                participatingHunts.add(hunt);
        }
        return participatingHunts;
    }

    // bounty hunt ...
    public static boolean executeHuntCommand(CommandSender sender, String[] args, boolean silent, boolean adminPermission, Player parser) {
        if (args.length < 1) {
            return failUnknownCommand(sender, silent, parser, "help.hunt-start");
        }

        // Handle single-argument command (start GUI for hunt)
        if (args.length == 1) {
            if (!hasPermission(sender, adminPermission, "notbounties.hunt.start")) {
                return failNoPermission(sender, silent, parser);
            }
            if (!(sender instanceof Player player)) {
                return failUnknownCommand(sender, silent, parser, "help.hunt-start");
            }
            GUI.openGUI(player, "bounty-hunt-player", 1);
            return true;
        }

        // Check permission for all multi-argument commands
        if (!hasPermission(sender, adminPermission, "notbounties.hunt.participate")) {
            return failNoPermission(sender, silent, parser);
        }

        String subCommand = args[1].toLowerCase();

        return switch (subCommand) {
            case "join" -> handleJoinCommand(sender, args, silent, parser);
            case "leave" -> handleLeaveCommand(sender, args, silent, parser);
            case "list" -> handleListCommand(sender, args, silent, parser);
            default -> handleStartNewHunt(sender, args, silent, adminPermission, parser);
        };
    }

// =======================
// Command Handlers
// =======================

    private static boolean handleJoinCommand(CommandSender sender, String[] args, boolean silent, Player parser) {
        if (!(sender instanceof Player player)) {
            return failOnlyPlayers(sender, silent);
        }

        if (getParticipatingHunts(player.getUniqueId()).size() >= maxJoinableHunts && maxJoinableHunts > 0) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("max-hunts-joined"), parser));
        }

        if (args.length <= 2) {
            return failUnknownCommand(sender, silent, parser, "help.hunt-participate");
        }

        UUID uuid = LoggedPlayers.getPlayer(args[2]);
        if (uuid == null) {
            return failUnknownPlayer(sender, silent, args[2], parser);
        }

        if (uuid.equals(player.getUniqueId())) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("own-hunt"), args[2], parser));
        }

        BountyHunt hunt = getHunt(uuid);
        if (hunt == null) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("no-hunt-found"), LoggedPlayers.getPlayerName(uuid), parser));
        }

        hunt.addParticipatingPlayer(player);
        sender.sendMessage(parse(getPrefix() + getMessage("hunt-join"), LoggedPlayers.getPlayerName(uuid), parser));
        return true;
    }

    private static boolean handleLeaveCommand(CommandSender sender, String[] args, boolean silent, Player parser) {
        if (!(sender instanceof Player player)) {
            return failOnlyPlayers(sender, silent);
        }

        if (args.length <= 2) {
            List<BountyHunt> participatingHunts = getParticipatingHunts(player.getUniqueId());
            if (participatingHunts.size() == 1) {
                participatingHunts.get(0).removeParticipatingPlayer(player);
                sender.sendMessage(parse(getPrefix() + getMessage("hunt-leave"), LoggedPlayers.getPlayerName(participatingHunts.get(0).getHuntedPlayer().getUniqueId()), parser));
                return true;
            }
            return failUnknownCommand(sender, silent, parser, "help.hunt-participate");
        }

        UUID uuid = LoggedPlayers.getPlayer(args[2]);
        if (uuid == null) {
            return failUnknownPlayer(sender, silent, args[2], parser);
        }

        BountyHunt hunt = getHunt(uuid);
        if (hunt == null) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("no-hunt-found"), LoggedPlayers.getPlayerName(uuid), parser));
        }

        if (!hunt.isParticipating(player.getUniqueId())) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("not-in-hunt"), LoggedPlayers.getPlayerName(hunt.getHuntedPlayer().getUniqueId()), parser));
        }

        hunt.removeParticipatingPlayer(player);
        sender.sendMessage(parse(getPrefix() + getMessage("hunt-leave"), LoggedPlayers.getPlayerName(hunt.getHuntedPlayer().getUniqueId()), parser));
        return true;
    }

    private static boolean handleListCommand(CommandSender sender, String[] args, boolean silent, Player parser) {
        if (args.length == 2) {
            // List all hunts
            sender.sendMessage(parse(getPrefix() + getMessage("list-hunts"), parser));
            hunts.forEach(hunt -> sender.sendMessage(ChatColor.RED + LoggedPlayers.getPlayerName(hunt.getHuntedPlayer().getUniqueId())));
            sender.sendMessage("");
            return true;
        }

        UUID uuid = LoggedPlayers.getPlayer(args[2]);
        if (uuid == null) {
            return failUnknownPlayer(sender, silent, args[2], parser);
        }

        BountyHunt hunt = getHunt(uuid);
        if (hunt == null) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("no-hunt-found"), LoggedPlayers.getPlayerName(uuid), parser));
        }

        sender.sendMessage(parse(getPrefix() + getMessage("list-hunt-players"), LoggedPlayers.getPlayerName(uuid), parser));
        hunt.getParticipatingPlayers().forEach(player -> sender.sendMessage(ChatColor.YELLOW + LoggedPlayers.getPlayerName(player.getUniqueId())));
        sender.sendMessage("");
        return true;
    }

    private static boolean handleStartNewHunt(CommandSender sender, String[] args, boolean silent, boolean adminPermission, Player parser) {
        if (hunts.size() >= maxConcurrentHunts && maxConcurrentHunts > 0) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("max-hunts-active"), parser));
        }

        if (!hasPermission(sender, adminPermission, "notbounties.hunt.start")) {
            return failNoPermission(sender, silent, parser);
        }

        UUID uuid = LoggedPlayers.getPlayer(args[1]);
        if (uuid == null) {
            return failUnknownPlayer(sender, silent, args[1], parser);
        }

        if (args.length == 2) {
            if (sender instanceof Player player) {
                GUI.openGUI(player, "bounty-hunt-time", minimumMinutes, uuid);
                return true;
            } else {
                return failUnknownCommand(sender, silent, parser, "help.hunt-start");
            }
        }

        // Parse duration
        long time;
        try {
            time = Integer.parseInt(args[2]) * 60 * 1000L;
        } catch (NumberFormatException e) {
            try {
                time = Duration.parse("+PT" + args[2]).toMillis();
            } catch (DateTimeParseException e1) {
                return failMessage(sender,  silent, parse(getPrefix() + getMessage("unknown-number"), parser));
            }
        }
        if (time / (60 * 1000L) < minimumMinutes) {
            return failMessage(sender, silent, parse(getPrefix() + getMessage("minimum-duration").replace("{time}", String.valueOf(minimumMinutes)), parser));
        }
        double cost = time / (60 * 1000.0) * costPerMinute;

        OfflinePlayer huntedPlayer = Bukkit.getOfflinePlayer(uuid);
        if (Commands.checkAndNotifyImmunity(sender, cost, silent, huntedPlayer, Collections.emptyList())) {
            // has immunity
            return false;
        }

        if (sender instanceof Player player) {
            if (!NumberFormatting.checkBalance(player, cost)) {
                return failBroke(sender, silent, cost, parser);
            }
            try {
                NumberFormatting.doRemoveCommands(player, cost, Collections.emptyList());
            } catch (NotEnoughCurrencyException e) {
                return failBroke(sender, silent, cost, parser);
            }
        }
        if (addCostToBounty) {
            if (sender instanceof Player player) {
                BountyManager.addBounty(player, huntedPlayer, cost * (1-ConfigOptions.getMoney().getBountyTax()), Collections.emptyList(), new Whitelist(new TreeSet<>(), false));
            } else {
                BountyManager.addBounty(huntedPlayer, cost, Collections.emptyList(), new Whitelist(new TreeSet<>(), false));
            }

        }
        for (BountyHunt hunt :  hunts) {
            if (hunt.getHuntedPlayer().getUniqueId().equals(uuid)) {
                // already has a hunt
                hunt.extendHunt(parser, time);
                sender.sendMessage(parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("extend-hunt").replace("{time}", LocalTime.formatTime(hunt.getEndTime() - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE)), args[1], parser));
                return true;
            }
        }
        hunts.add(new BountyHunt(parser, huntedPlayer, time));
        return true;
    }

// =======================
// Helper Methods
// =======================

    private static boolean hasPermission(CommandSender sender, boolean adminPermission, String permission) {
        return adminPermission || sender.hasPermission(permission);
    }

    private static boolean failUnknownCommand(CommandSender sender, boolean silent, Player parser, String helpMessageKey) {
        if (!silent) {
            sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
            LanguageOptions.sendHelpMessage(sender, getListMessage(helpMessageKey));
        }
        return false;
    }

    private static boolean failNoPermission(CommandSender sender, boolean silent, Player parser) {
        if (!silent) {
            sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
        }
        return false;
    }

    private static boolean failBroke(CommandSender sender, boolean silent, double requiredAmount, Player parser) {
        if (!silent) {
            sender.sendMessage(parse(getPrefix() + getMessage("broke"), requiredAmount, parser));
        }
        return false;
    }

    private static boolean failUnknownPlayer(CommandSender sender, boolean silent, String playerName, Player parser) {
        if (!silent) {
            sender.sendMessage(parse(getPrefix() + getMessage("unknown-player"), playerName, parser));
        }
        return false;
    }

    private static boolean failOnlyPlayers(CommandSender sender, boolean silent) {
        if (!silent) {
            sender.sendMessage("Only players can use this command");
        }
        return false;
    }

    private static boolean failMessage(CommandSender sender, boolean silent, String message) {
        if (!silent) {
            sender.sendMessage(message);
        }
        return false;
    }

    private final OfflinePlayer huntedPlayer;
    private final OfflinePlayer setter;
    private long endTime;
    private final long startTime;
    private final BossBar bossBar;
    private final Set<Player> participatingPlayers = new HashSet<>();
    private final Set<UUID> givenTrackers;

    /**
     * Creating this object starts a bounty hunt for a player.
     * @param setter Player who started the hunt. Null for console.
     * @param huntedPlayer Player who will be hunted.
     * @param time The time in milliseconds for the hunt.
     */
    public BountyHunt(@Nullable Player setter, OfflinePlayer huntedPlayer, long time) {
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + time;
        this.huntedPlayer = huntedPlayer;
        this.setter = setter;
        this.bossBar = Bukkit.createBossBar(parseTitle(), bossBarColor, BarStyle.SOLID);
        this.givenTrackers = new HashSet<>();
        if (setter != null)
            addParticipatingPlayer(setter);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (setter != null && setter.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            sendBroadcast(player);
        }
        Bukkit.getConsoleSender().sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("hunt-broadcast").replace("{time}", LocalTime.formatTime(endTime - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE)), setter, huntedPlayer));
    }


    public BountyHunt(@Nullable UUID setterUUID, @NotNull UUID huntedUUID, long durationLeft, long totalTime, Set<UUID> givenTracker) {
        if (setterUUID != null)
            this.setter = Bukkit.getOfflinePlayer(setterUUID);
        else
            this.setter = null;
        this.huntedPlayer = Bukkit.getOfflinePlayer(huntedUUID);
        this.startTime = System.currentTimeMillis() - totalTime + durationLeft;
        this.endTime = startTime + totalTime;
        this.bossBar = Bukkit.createBossBar("...", BarColor.RED, BarStyle.SOLID);
        this.givenTrackers = givenTracker;
    }

    public void extendHunt(@Nullable Player setter, long time) {
        if (setter != null)
            if (!isParticipating(setter.getUniqueId()))
                addParticipatingPlayer(setter);
        this.endTime += time;
    }

    public void endHunt() {
        participatingPlayers.clear();
        bossBar.removeAll();
        // send hunt ended msg
        String msg = LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("hunt-end"), setter, huntedPlayer);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (
                    player.getUniqueId().equals(huntedPlayer.getUniqueId())
                    || (setter != null && player.getUniqueId().equals(setter.getUniqueId()))
                    || DataManager.getPlayerData(player.getUniqueId()).getBroadcastSettings() != PlayerData.BroadcastSettings.DISABLE
            ) {
                player.sendMessage(msg);
            }
        }
    }

    private void sendBroadcast(Player player) {
        if (
                player.getUniqueId().equals(huntedPlayer.getUniqueId())
                || (setter != null && player.getUniqueId().equals(setter.getUniqueId()))
                || DataManager.getPlayerData(player.getUniqueId()).getBroadcastSettings() != PlayerData.BroadcastSettings.DISABLE
        ) {
            player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("hunt-broadcast").replace("{time}", LocalTime.formatTime(endTime - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE)), setter, huntedPlayer));
        }
    }

    private String parseTitle() {
        return LanguageOptions.parse(bossBarText, endTime - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE, huntedPlayer);
    }

    public void addParticipatingPlayer(Player player) {
        participatingPlayers.add(player);
        if (bossBarEnabled) {
            bossBar.addPlayer(player);
        }
        if (giveTrackers && BountyTracker.isEnabled() && !givenTrackers.contains(player.getUniqueId())) {
            ItemStack tracker = BountyTracker.getTracker(huntedPlayer.getUniqueId());
            ItemMeta meta = tracker.getItemMeta();
            assert meta != null;
            meta.getPersistentDataContainer().set(huntKey, PersistentDataType.STRING, player.getUniqueId().toString());
            tracker.setItemMeta(meta);
            NumberFormatting.givePlayer(player, tracker, 1);
            givenTrackers.add(player.getUniqueId());
        }
    }

    public void removeParticipatingPlayer(Player player) {
        participatingPlayers.remove(player);
        if (bossBarEnabled) {
            bossBar.removePlayer(player);
        }
        BountyTracker.removeTracker(player);
    }

    public boolean isParticipating(UUID uuid) {
        for (Player player : participatingPlayers) {
            if (player.getUniqueId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public OfflinePlayer getHuntedPlayer() {
        return huntedPlayer;
    }

    public Set<Player> getParticipatingPlayers() {
        return participatingPlayers;
    }

    public void updateBossBar() {
        if (System.currentTimeMillis() > endTime) {
            BountyHunt.endHunt(huntedPlayer.getUniqueId());
            return;
        }
        if (participatingPlayers.isEmpty() || bossBar.getPlayers().isEmpty()) {
            return;
        }
        bossBar.setColor(bossBarColor);
        bossBar.setTitle(parseTitle());
        bossBar.setProgress(1 - ((double) (System.currentTimeMillis() - startTime) / (endTime - startTime)));
    }

    public void enableBossBar() {
        for (Player player : participatingPlayers) {
            if (player.isOnline()) {
                bossBar.addPlayer(player);
            }
        }
        updateBossBar();
    }

    public void disableBossBar() {
        for (Player player : participatingPlayers) {
            if (player.isOnline()) {
                bossBar.removePlayer(player);
            }
        }
    }

    public void onJoin(Player player) {
        if (participatingPlayers.contains(player) && bossBarEnabled) {
            bossBar.addPlayer(player);
        } else {
            // send the broadcast message about hunt
            sendBroadcast(player);
        }
    }

    public void onQuit(Player player) {
        if (participatingPlayers.contains(player) && bossBarEnabled) {
            bossBar.removePlayer(player);
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public OfflinePlayer getSetter() {
        return setter;
    }

    public Set<UUID> getGivenTrackers() {
        return givenTrackers;
    }

    public static int getMaxJoinableHunts() {
        return maxJoinableHunts;
    }
}

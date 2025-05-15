package me.jadenp.notbounties;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.google.common.io.Files;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.Commands;
import me.jadenp.notbounties.ui.Events;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.ui.map.BountyBoardTypeAdapter;
import me.jadenp.notbounties.ui.map.BountyMap;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.utils.challenges.ChallengeListener;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.configuration.auto_bounties.MurderBounties;
import me.jadenp.notbounties.utils.configuration.auto_bounties.RandomBounties;
import me.jadenp.notbounties.utils.configuration.auto_bounties.TimedBounties;
import me.jadenp.notbounties.utils.configuration.webhook.WebhookOptions;
import me.jadenp.notbounties.utils.external_api.*;
import me.jadenp.notbounties.utils.external_api.bedrock.FloodGateClass;
import me.jadenp.notbounties.utils.external_api.bedrock.GeyserMCClass;
import me.jadenp.notbounties.utils.external_api.worldguard.WorldGuardClass;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.vaultEnabled;

/**
 * Folia
 * Team bounties
 * Bungee support.
 * Better SQL and Redis config with connection string and address options to replace others.
 * allow other image sizes for bounty poster & center them
 * Redo vouchers with persistent data, give items, & reward delay
 */
public final class NotBounties extends JavaPlugin {

    private static NotBounties instance;
    private static String latestVersion;
    private static boolean updateAvailable = false;

    public static final String SESSION_KEY = UUID.randomUUID().toString();
    private static NamespacedKey namespacedKey;
    private static int serverVersion = 20;
    private static int serverSubVersion = 0;
    private static boolean debug = false;
    private static boolean paused = false;
    private static Events events;
    private boolean started = false;
    private static ServerImplementation serverImplementation;

    public static NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public static void setNamespacedKey(NamespacedKey namespacedKey) {
        NotBounties.namespacedKey = namespacedKey;
    }

    private static void setInstance(NotBounties instance) {
        NotBounties.instance = instance;
    }

    public static void setEvents(Events events) {
        NotBounties.events = events;
    }

    @Override
    public void onLoad() {
        setInstance(this);
        // register api flags
        ConfigOptions.setWorldGuardEnabled(getServer().getPluginManager().getPlugin("WorldGuard") != null);
        if (ConfigOptions.isWorldGuardEnabled()) {
            WorldGuardClass.registerFlags();
        }
        if (getServer().getPluginManager().getPlugin("Lands") != null)
            new LandsClass().registerClaimFlag();
    }


    @Override
    public void onEnable() {
        setServerImplementation(new FoliaCompatibility(this).getServerImplementation());
        Random random = new Random(System.currentTimeMillis());
        // Plugin startup logic
        if (random.nextInt(10) == 3) {
            // 10% chance to receive this Easter egg
            String display = """
                                      \s
                    ████████████████████████████████████████████████████████████████████
                    █▄─▀█▄─▄█─▄▄─█─▄─▄─█▄─▄─▀█─▄▄─█▄─██─▄█▄─▀█▄─▄█─▄─▄─█▄─▄█▄─▄▄─█─▄▄▄▄█
                    ██─█▄▀─██─██─███─████─▄─▀█─██─██─██─███─█▄▀─████─████─███─▄█▀█▄▄▄▄─█
                    ▀▄▄▄▀▀▄▄▀▄▄▄▄▀▀▄▄▄▀▀▄▄▄▄▀▀▄▄▄▄▀▀▄▄▄▄▀▀▄▄▄▀▀▄▄▀▀▄▄▄▀▀▄▄▄▀▄▄▄▄▄▀▄▄▄▄▄▀""";
            Bukkit.getLogger().info(display);
        }

        // load commands and events
        Commands commands = new Commands();
        Objects.requireNonNull(this.getCommand("notbounties")).setExecutor(commands);
        Objects.requireNonNull(this.getCommand("notbountiesadmin")).setExecutor(commands);
        setEvents(new Events());
        Bukkit.getServer().getPluginManager().registerEvents(events, this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new CurrencySetup(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyMap(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PVPRestrictions(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new WebhookOptions(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Prompt(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyTracker(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ChallengeListener(), this);

        this.saveDefaultConfig();


        BountyMap.initialize();
        setNamespacedKey(new NamespacedKey(this, "bounty-entity"));

        readVersion();

        Bukkit.getServer().getPluginManager().registerEvents(new RemovePersistentEntitiesEvent(), this);

        try {
            loadConfig();
            if (ChallengeManager.isEnabled()) {
                ChallengeManager.readChallengeData();
            }
            DataManager.loadData(this);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[NotBounties] Failed to read player data!");
            Bukkit.getLogger().severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stack -> Bukkit.getLogger().severe("       at " + stack.toString()));
        }

        if (sendBStats) {
            int pluginId = 20776;
            Metrics metrics = new Metrics(this, pluginId);
            metrics.addCustomChart(new Metrics.SingleLineChart("active_bounties", new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return BountyManager.getAllBounties(-1).size();
                }
            }));
        }

        // clean out posters that aren't supposed to be there
        if (!saveTemplates) {
            BountyMap.cleanPosters();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BountyExpansion().register();
        }

        // load skins for bounties
        for (Bounty bounty : BountyManager.getAllBounties(-1))
            SkinManager.saveSkin(bounty.getUUID());

        // force login players that are already on the server - this will happen if the plugin is loaded without a restart
        for (Player player : Bukkit.getOnlinePlayers()) {
            Events.login(player);
        }

        if (ConfigOptions.isWorldGuardEnabled())
            WorldGuardClass.registerHandlers();

        // update checker
        if (!ConfigOptions.getUpdateNotification().equalsIgnoreCase("false")) {
            new UpdateChecker(this, 104484).getVersion(version -> {
                latestVersion = version;
                if (getDescription().getVersion().contains("dev"))
                    return;
                if (getDescription().getVersion().equals(version))
                    return;
                // split the numbers
                String[] versionSplit = version.split("\\.");
                String[] currentSplit = getDescription().getVersion().split("\\.");
                // convert to integers
                int[] versionNumbers = new int[versionSplit.length];
                for (int i = 0; i < versionSplit.length; i++) {
                    try {
                        versionNumbers[i] = Integer.parseInt(versionSplit[i]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                int[] currentNumbers = new int[currentSplit.length];
                for (int i = 0; i < currentNumbers.length; i++) {
                    try {
                        currentNumbers[i] = Integer.parseInt(currentSplit[i]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                for (int i = 0; i < currentNumbers.length; i++) {
                    if (currentNumbers[i] < versionNumbers[i]) {
                        updateAvailable = true;
                        break;
                    }
                }
                if (!updateAvailable && currentNumbers.length < versionNumbers.length)
                    updateAvailable = true;
                if (updateAvailable && !version.equals(ConfigOptions.getUpdateNotification())) {
                    Bukkit.getLogger().info("[NotBounties] " + ChatColor.stripColor(parse(getMessage("update-notification").replace("{current}", NotBounties.getInstance().getDescription().getVersion()).replace("{latest}", version), null)));
                }
            });
        }

        // check permission immunity every 5 mins
        getServerImplementation().global().runAtFixedRate(Immunity::checkOnlinePermissionImmunity, 3611, 3600);

        // make bounty tracker work & big bounty particle & time immunity
        getServerImplementation().global().runAtFixedRate(() -> {
            if (paused)
                return;
            // update bounty tracker
            BountyTracker.update();

            // big bounty particle
            BigBounty.displayParticle();

            Immunity.update();
            RandomBounties.update();
            TimedBounties.update();

            PVPRestrictions.checkCombatExpiry();
            ChallengeManager.checkChallengeChange();

            BountyBoard.update();
        }, 100, 40);
        // auto save bounties & do some ram cleaning
        getServerImplementation().async().runAtFixedRate(() -> {
            if (paused)
                return;
            MurderBounties.cleanPlayerKills();
            SkinManager.removeOldData();
            RemovePersistentEntitiesEvent.checkRemovedEntities();

            try {
                save();
            } catch (IOException e) {
                Bukkit.getLogger().severe("[NotBounties] Error autosaving saving data!");
                Bukkit.getLogger().severe(e.toString());
            }
        }, autoSaveInterval * 60 * 20L + 69, autoSaveInterval * 60 * 20L);


        // this needs to be in a 5-minute interval cuz that's the lowest time specified in the config for expiration
        getServerImplementation().async().runAtFixedRate(BountyExpire::removeExpiredBounties, 5 * 60 * 20L + 2007, 5 * 60 * 20L);

        // Check for banned players
        //         * Runs every hour and will check a few players at a time
        //         * Every player will be guaranteed to be checked after 12 hours
        getServerImplementation().global().runAtFixedRate(new Runnable() {
            List<Bounty> bountyListCopy = new ArrayList<>();
            int playersPerRun = 1;
            @Override
            public void run() {
                if (!removeBannedPlayers || paused)
                    return;
                if (bountyListCopy.isEmpty()) {
                    bountyListCopy = BountyManager.getAllBounties(-1);
                    playersPerRun = bountyListCopy.size() / 12 + 1;
                }
                for (int i = 0; i < playersPerRun; i++) {
                    if (i >= bountyListCopy.size())
                        break;
                    Bounty bounty = bountyListCopy.get(i);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(bounty.getUUID());
                    getServerImplementation().async().runNow(() -> {
                        if (player.isBanned() || (liteBansEnabled && !(new LiteBansClass().isPlayerNotBanned(bounty.getUUID())))) {
                            BountyManager.removeBounty(bounty.getUUID());
                        }
                    });
                }
                if (playersPerRun > 0) {
                    if (bountyListCopy.size() > playersPerRun)
                        bountyListCopy.subList(0, playersPerRun).clear();
                    else
                        bountyListCopy.clear();
                }
            }
        }, 101, 72006);

        // wanted text
        getServerImplementation().global().runAtFixedRate(new Runnable() {
            static final int MAX_UPDATE_TIME = 10;
            int lastUpdateTime = 0; // time it took for the stands to update last
            long lastRunTime = 0;
            @Override
            public void run() {
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    if (lastUpdateTime > MAX_UPDATE_TIME) {
                        //    (the amount of ms since last update)      (2 x the amount of ms last update took)
                        if (System.currentTimeMillis() - lastRunTime < (lastUpdateTime - MAX_UPDATE_TIME) * 2L) {
                            return;
                        }
                    }
                    long startTime = System.currentTimeMillis();
                    WantedTags.update();
                    lastUpdateTime = (int) (System.currentTimeMillis() - startTime);
                    if (lastUpdateTime > MAX_UPDATE_TIME) {
                        lastRunTime = System.currentTimeMillis();
                        if (debug)
                            Bukkit.getLogger().info("[NotBountiesDebug] Took " + lastUpdateTime + "ms to update wanted tags. Pausing for a few updates.");
                    }
                }
            }
        }, 20, 1);

        // plugin was enabled successfully
        started = true;
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static NotBounties getInstance() {
        return instance;
    }


    private void save() throws IOException {
        File dataDirectory = new File(this.getDataFolder() + File.separator + "data");
        if (dataDirectory.mkdir())
            NotBounties.debugMessage("Created new data directory", false);
        // save bounties
        File bountiesFile = new File(dataDirectory + File.separator + "bounties.json");
        if (bountiesFile.createNewFile()) {
            NotBounties.debugMessage("Created a new bounties.json file.", false);
        }
        try (JsonWriter writer = new JsonWriter(new FileWriter(bountiesFile))) {
            writer.beginArray();
            BountyTypeAdapter adapter = new BountyTypeAdapter();
            for (Bounty bounty : DataManager.getLocalBounties()) {
                adapter.write(writer, bounty);
            }
            writer.endArray();
        }

        // save stats
        File statsFile = new File(dataDirectory + File.separator + "player_stats.json");
        if (statsFile.createNewFile()) {
            NotBounties.debugMessage("Created a new player_stats.json file.", false);
        }
        try (JsonWriter writer = new JsonWriter(new FileWriter(statsFile))) {
            writer.beginArray();
            PlayerStatAdapter adapter = new PlayerStatAdapter();
            for (Map.Entry<UUID, PlayerStat> entry : DataManager.getLocalStats()) {
                writer.beginObject();
                writer.name("uuid").value(entry.getKey().toString());
                writer.name("stats");
                adapter.write(writer, entry.getValue());
                writer.endObject();
            }
            writer.endArray();
        }

        // save player data
        File playerDataFile = new File(dataDirectory + File.separator + "player_data.json");
        if (playerDataFile.createNewFile()) {
            NotBounties.debugMessage("Created a new player_data.json file.", false);
        }

        try (JsonWriter writer = new JsonWriter(new FileWriter(playerDataFile))) {
            writer.beginObject();
            writer.name("players");
            writer.beginArray();
            PlayerDataAdapter adapter = new PlayerDataAdapter();
            for (Map.Entry<UUID, PlayerData> entry : DataManager.getPlayerDataMap().entrySet()) {
                writer.beginObject();
                writer.name("uuid").value(entry.getKey().toString());
                writer.name("data");
                adapter.write(writer, entry.getValue());
                writer.endObject();
            }
            writer.endArray();

            writer.name("trackedBounties");
            writer.beginArray();
            for (Map.Entry<Integer, UUID> entry : BountyTracker.getTrackedBounties().entrySet()) {
                writer.beginObject();
                writer.name("id").value(entry.getKey());
                writer.name("uuid").value(entry.getValue().toString());
                writer.endObject();
            }
            writer.endArray();

            writer.name("databaseSyncTimes");
            writer.beginArray();
            for (AsyncDatabaseWrapper database : DataManager.getDatabases()) {
                if (!database.isPermDatabase() && System.currentTimeMillis() - database.getLastSync() < DataManager.CONNECTION_REMEMBRANCE_MS) {
                    writer.beginObject();
                    writer.name("name").value(database.getName());
                    writer.name("time").value(database.getLastSync());
                    writer.endObject();
                }
            }
            writer.endArray();

            if (RandomBounties.isEnabled())
                writer.name("nextRandomBounty").value(RandomBounties.getNextRandomBounty());

            if (TimedBounties.isEnabled()) {
                writer.name("nextTimedBounties");
                writer.beginArray();
                for (Map.Entry<UUID, Long> entry : TimedBounties.getNextBounties().entrySet()) {
                    writer.beginObject();
                    writer.name("uuid").value(entry.getKey().toString());
                    writer.name("time").value(entry.getValue());
                    writer.endObject();
                }
                writer.endArray();
            }

            writer.name("bountyBoards");
            writer.beginArray();
            BountyBoardTypeAdapter bountyBoardTypeAdapter = new BountyBoardTypeAdapter();
            for (BountyBoard board : BountyBoard.getBountyBoards()) {
                bountyBoardTypeAdapter.write(writer, board);
            }
            writer.endArray();

            writer.name("nextChallengeChange").value(ChallengeManager.getNextChallengeChange());
            writer.name("serverID").value(DataManager.getDatabaseServerID(false).toString());
            writer.name("paused").value(NotBounties.isPaused());

            List<String> wantedTagLocations = DataManager.locationListToStringList(WantedTags.getLastLocations());
            if (!wantedTagLocations.isEmpty()) {
                writer.name("wantedTagLocations");
                writer.beginArray();
                for (String loc : wantedTagLocations)
                    writer.value(loc);
                writer.endArray();
            }
            writer.endObject();
        }

        saveBackup(bountiesFile);
        saveBackup(statsFile);
        saveBackup(playerDataFile);
    }

    private static void readVersion() {
        try {
            // get the text version - ex: 1.20.3
            String fullServerVersion = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf("-"));
            fullServerVersion = fullServerVersion.substring(2); // remove the '1.' in the version
            if (fullServerVersion.contains(".")) {
                // get the subversion - ex: 3
                serverSubVersion = Integer.parseInt(fullServerVersion.substring(fullServerVersion.indexOf(".") + 1));
                fullServerVersion = fullServerVersion.substring(0, fullServerVersion.indexOf(".")); // remove the subversion
            }
            serverVersion = Integer.parseInt(fullServerVersion);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning("[NotBounties] Could not get the server version. Some features may not function properly.");
            serverVersion = 20;
            serverSubVersion = 0;
        }
    }

    private void saveBackup(File file) throws IOException {
        if (!bountyBackups) {
            return;
        }
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd");
        File backupDirectory = new File(this.getDataFolder() + File.separator + "backups");
        File todayDirectory = new File(backupDirectory + File.separator + simpleDateFormat.format(date));
        File today = new File(todayDirectory + File.separator + file.getName());
        if (backupDirectory.mkdir()) {
            Bukkit.getLogger().info("[NotBounties] Created backup directory.");
        }

        if (todayDirectory.mkdir()) {
            // delete old backups
            deleteOldBackups(backupDirectory, simpleDateFormat);
        }
        // try to create a daily backup
        if (today.createNewFile()) {
            Files.copy(file, today);
        }

    }

    private void deleteOldBackups(File backupDirectory, SimpleDateFormat simpleDateFormat) {
        File[] files = backupDirectory.listFiles();
        if (files == null)
            return;

        Map<File, Long> fileDates = getFileDates(files, simpleDateFormat);
        Map<File, Long> sortedMap =
                fileDates.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));
        // find out which files need to be deleted
        final long maxWeeklyBackup = 1000L * 60 * 60 * 24 * 7 * 3;
        final long maxDailyBackup = 1000L * 60 * 60 * 24 * 7;
        int weeklyBackups = 0;
        long lastWeeklyBackup = 0;
        List<File> pendingDeletion = new ArrayList<>();
        for (Map.Entry<File, Long> entry : sortedMap.entrySet()) {
            long timeSinceCreation = System.currentTimeMillis() - entry.getValue();
            if (timeSinceCreation > maxWeeklyBackup) {
                // too long ago to be useful
                pendingDeletion.add(entry.getKey());
                continue;
            }
            if (timeSinceCreation > maxDailyBackup) {
                // been over 7 days since this backup was created
                if (weeklyBackups < 3 || timeSinceCreation - lastWeeklyBackup >= maxDailyBackup - (1000L * 60 * 60 * 24)) { // 6 days
                    // only keep 3 weekly backups
                    weeklyBackups++;
                    lastWeeklyBackup = timeSinceCreation;
                } else {
                    // delete
                    pendingDeletion.add(entry.getKey());
                }
            }
        }
        deleteDirectories(pendingDeletion);
    }

    private void deleteDirectories(List<File> pendingDeletion) {
        for (File directory : pendingDeletion) {
            try {
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    java.nio.file.Files.delete(file.toPath());
                }
                java.nio.file.Files.delete(directory.toPath());
            } catch (IOException e) {
                Bukkit.getLogger().warning("[NotBounties] Could not delete old backups.");
                Bukkit.getLogger().warning(e.toString());
            }
        }
    }

    private Map<File, Long> getFileDates(File[] files, SimpleDateFormat simpleDateFormat) {
        Map<File, Long> fileDates = new HashMap<>();
        for (File value : files) {
            try {
                if (value.isDirectory())
                    fileDates.put(value, simpleDateFormat.parse(value.getName()).getTime());
            } catch (ParseException ignored) {
                // file not in correct format
            }
        }

        return fileDates;
    }

    public void loadConfig() throws IOException {
        // close gui

        ConfigOptions.reloadOptions();

        if (!NotBounties.isPaused()) {
            // check players to display particles
            getServerImplementation().global().runDelayed(BigBounty::refreshParticlePlayers, 40);

        }


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (ConfigOptions.isMmoLibEnabled())
            MMOLibClass.removeAllModifiers();
        SkinManager.shutdown();
        DataManager.shutdown();
        if (!started)
            // Plugin failed to start.
            // Returning, so save data isn't overwritten.
            return;

        // close all GUIs
        for (Player player : Bukkit.getOnlinePlayers()) {
            GUI.safeCloseGUI(player, true);
        }
        // save data

        try {
            save();
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error saving data!");
            Bukkit.getLogger().severe(e.toString());
        }

        // remove wanted tags
        WantedTags.disableWantedTags();
        BountyBoard.clearBoard();

    }


    public void sendDebug(CommandSender sender) {
        sender.sendMessage(parse(getPrefix() + ChatColor.WHITE + "NotBounties debug info:", null));
        long numConnected = DataManager.getDatabases().stream().filter(AsyncDatabaseWrapper::isConnected).count();
        String connected = numConnected > 0 ? ChatColor.GREEN + "" + numConnected : ChatColor.RED + "" + numConnected;
        String numConfigured = ChatColor.WHITE + "" + DataManager.getDatabases().size();
        int bounties = BountyManager.getAllBounties(-1).size();
        sender.sendMessage(ChatColor.GOLD + "Databases > " + ChatColor.YELLOW + "Configured: " + numConfigured
                + ChatColor.YELLOW + " Connected: " + connected);
        sender.sendMessage(ChatColor.GOLD + "General > "
                + ChatColor.YELLOW + "Author: " + ChatColor.GRAY + "Not_Jaden"
                + ChatColor.YELLOW + " Current Plugin Version: " + ChatColor.WHITE + getDescription().getVersion()
                + ChatColor.YELLOW + " Latest Plugin Version: " + ChatColor.WHITE + getLatestVersion()
                + ChatColor.YELLOW + " Server Version: " + ChatColor.WHITE
                + "1." + serverVersion + "." + serverSubVersion
                + ChatColor.YELLOW + " Debug Mode: " + ChatColor.WHITE + debug
                + ChatColor.YELLOW + " Online Mode: " + ChatColor.WHITE + Bukkit.getOnlineMode());
        sender.sendMessage(ChatColor.GOLD + "Stats > " + ChatColor.YELLOW + "Bounties: " + ChatColor.WHITE + bounties
                + ChatColor.YELLOW + " Tracked Bounties: " + ChatColor.WHITE + BountyTracker.getTrackedBounties().size()
                + ChatColor.YELLOW + " Bounty Boards: " + ChatColor.WHITE + BountyBoard.getBountyBoards().size());
        String vault = vaultEnabled ? ChatColor.GREEN + "Vault" : ChatColor.RED + "Vault";
        String papi = papiEnabled ? ChatColor.GREEN + "PlaceholderAPI" : ChatColor.RED + "PlaceholderAPI";
        String hdb = HDBEnabled ? ChatColor.GREEN + "HeadDataBase" : ChatColor.RED + "HeadDataBase";
        String liteBans = liteBansEnabled ? ChatColor.GREEN + "LiteBans" : ChatColor.RED + "LiteBans";
        String skinsRestorer = skinsRestorerEnabled
                ? ChatColor.GREEN + "SkinsRestorer" : ChatColor.RED + "SkinsRestorer";
        String betterTeams = BountyClaimRequirements.isBetterTeamsEnabled()
                ? ChatColor.GREEN + "BetterTeams" : ChatColor.RED + "BetterTeams";
        String townyAdvanced = BountyClaimRequirements.isTownyAdvancedEnabled()
                ? ChatColor.GREEN + "TownyAdvanced" : ChatColor.RED + "TownyAdvanced";
        String floodgate = floodgateEnabled ? ChatColor.GREEN + "Floodgate" : ChatColor.RED + "Floodgate";
        String geyser = geyserEnabled ? ChatColor.GREEN + "GeyserMC" : ChatColor.RED + "GeyserMC";
        String kingdoms = BountyClaimRequirements.isKingdomsXEnabled()
                ? ChatColor.GREEN + "Kingdoms" : ChatColor.RED + "Kingdoms";
        String lands = BountyClaimRequirements.isLandsEnabled() ? ChatColor.GREEN + "Lands" : ChatColor.RED + "Lands";
        String worldGuard = ConfigOptions.isWorldGuardEnabled()
                ? ChatColor.GREEN + "WorldGuard" : ChatColor.RED + "WorldGuard";
        String superiorSkyblock2 = BountyClaimRequirements.isSuperiorSkyblockEnabled()
                ? ChatColor.GREEN + "SuperiorSkyblock2" : ChatColor.RED + "SuperiorSkyblock2";
        String mMOLib = ConfigOptions.isMmoLibEnabled() ? ChatColor.GREEN + "MMOLib" : ChatColor.RED + "MMOLib";
        String simpleClans = BountyClaimRequirements.isSimpleClansEnabled()
                ? ChatColor.GREEN + "SimpleClans" : ChatColor.RED + "SimpleClans";
        String factions = BountyClaimRequirements.isFactionsEnabled()
                ? ChatColor.GREEN + "Factions" : ChatColor.RED + "Factions";
        String duels = ConfigOptions.isDuelsEnabled() ? ChatColor.GREEN + "Duels" : ChatColor.RED + "Duels";
        sender.sendMessage(ChatColor.GOLD + "Plugin Hooks > " + ChatColor.GRAY + "["
                + vault + ChatColor.GRAY + "|"
                + papi + ChatColor.GRAY + "|"
                + hdb + ChatColor.GRAY + "|"
                + liteBans + ChatColor.GRAY + "|"
                + skinsRestorer + ChatColor.GRAY + "|"
                + betterTeams + ChatColor.GRAY + "|"
                + townyAdvanced + ChatColor.GRAY + "|"
                + geyser + ChatColor.GRAY + "|"
                + floodgate + ChatColor.GRAY + "|"
                + kingdoms + ChatColor.GRAY + "|"
                + lands + ChatColor.GRAY + "|"
                + worldGuard + ChatColor.GRAY + "|"
                + superiorSkyblock2 + ChatColor.GRAY + "|"
                + mMOLib + ChatColor.GRAY + "|"
                + simpleClans + ChatColor.GRAY + "|"
                + factions + ChatColor.GRAY + "|"
                + duels + ChatColor.GRAY + "]");
        sender.sendMessage(ChatColor.GRAY + "Reloading the plugin will refresh connections.");
        TextComponent discord = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(114, 137, 218))
                + "Support Discord: " + ChatColor.GRAY + ChatColor.UNDERLINE + "https://discord.gg/zEsUzwYEx7");
        discord.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/zEsUzwYEx7"));
        TextComponent spigot = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(240, 149, 45))
                + "Spigot: " + ChatColor.GRAY + ChatColor.UNDERLINE + "https://www.spigotmc.org/resources/notbounties.104484/");
        spigot.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/notbounties.104484/"));
        TextComponent github = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(230, 237, 243))
                + "Github: " + ChatColor.GRAY + ChatColor.UNDERLINE + "https://github.com/No-Not-Jaden/NotBounties");
        github.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/No-Not-Jaden/NotBounties"));
        sender.spigot().sendMessage(discord);
        sender.spigot().sendMessage(spigot);
        sender.spigot().sendMessage(github);
        sender.sendMessage("");
    }



    public static Map<UUID, String> getNetworkPlayers() {
        return DataManager.getNetworkPlayers();
    }

    public static boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    /**
     * Returns if the server version is above the specified version
     *
     * @param majorVersion Major version of the server. In 1.20.4, the major version is 20
     * @param subVersion   Sub version of the server. In 1.20.4, the sub version is 4
     * @return True if the current server version is higher than the specified one
     */
    public static boolean isAboveVersion(int majorVersion, int subVersion) {
        return serverVersion > majorVersion || (majorVersion == serverVersion && subVersion < serverSubVersion);
    }

    public static void debugMessage(String message, boolean warning) {
        if (!debug)
            return;
        message = "[NotBountiesDebug] " + message;
        NotBounties notBounties = NotBounties.getInstance();
        if (notBounties.isEnabled()) {
            String finalMessage = message;
            getServerImplementation().global().run(() -> consoleMessage(finalMessage, warning));
        } else {
            consoleMessage(message, warning);
        }
    }

    public static boolean isPaused() {
        return paused;
    }

    public static void setPaused(boolean paused) {
        if (paused) {
            for (BountyBoard board : BountyBoard.getBountyBoards())
                board.remove();
            // close all GUIs
            for (Player player : Bukkit.getOnlinePlayers()) {
                GUI.safeCloseGUI(player, true);
            }
            // remove wanted tags
            WantedTags.disableWantedTags();
            for (Player player : Bukkit.getOnlinePlayers())
                events.onQuit(new PlayerQuitEvent(player, ""));
            NotBounties.paused = true;
        } else {
            NotBounties.paused = false;
            for (Player player : Bukkit.getOnlinePlayers())
                events.onJoin(new PlayerJoinEvent(player, ""));
        }
    }

    private static void consoleMessage(String message, boolean warning) {
        if (warning)
            Bukkit.getLogger().warning(message);
        else
            Bukkit.getLogger().info(message);
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            if (ConfigOptions.floodgateEnabled) {
                FloodGateClass floodGateClass = new FloodGateClass();
                return floodGateClass.isBedrockPlayer(uuid);
            } else if (ConfigOptions.geyserEnabled) {
                GeyserMCClass geyserMCClass = new GeyserMCClass();
                return geyserMCClass.isBedrockPlayer(uuid);
            }
        }
        String first = uuid.toString().substring(0, 18);
        return first.equals("00000000-0000-0000");
    }

    public static String getXuid(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            if (ConfigOptions.floodgateEnabled) {
                FloodGateClass floodGateClass = new FloodGateClass();
                if (floodGateClass.isBedrockPlayer(uuid)) {
                    return floodGateClass.getXuid(uuid);
                }
            } else if (ConfigOptions.geyserEnabled) {
                GeyserMCClass geyserMCClass = new GeyserMCClass();
                if (geyserMCClass.isBedrockPlayer(uuid)) {
                    return geyserMCClass.getXuid(uuid);
                }
            }
        }
        if (isBedrockPlayer(uuid)) {
            String last = uuid.toString().replace("-", "").substring(16);
            try {
                return Long.parseLong(last, 16) + "";
            } catch (NumberFormatException ignored) {
                // Not parsable hex
            }
        }
        return "";
    }

    private static final String ADMIN_PERMISSION = "notbounties.admin";

    public static String getAdminPermission() {
        return ADMIN_PERMISSION;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean debug) {
        NotBounties.debug = debug;
    }

    public static int getServerVersion() {
        return serverVersion;
    }

    public static ServerImplementation getServerImplementation() {
        return serverImplementation;
    }

    private static void setServerImplementation(ServerImplementation serverImplementation) {
        NotBounties.serverImplementation = serverImplementation;
    }
}

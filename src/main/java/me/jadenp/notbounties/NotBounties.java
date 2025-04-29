package me.jadenp.notbounties;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.google.common.io.Files;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.ui.*;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.map.*;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.utils.challenges.ChallengeListener;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.configuration.auto_bounties.*;
import me.jadenp.notbounties.utils.configuration.webhook.WebhookOptions;
import me.jadenp.notbounties.utils.external_api.*;
import me.jadenp.notbounties.utils.external_api.bedrock.*;
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

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.vaultEnabled;

public final class NotBounties extends JavaPlugin {
    // Singleton instance
    private static volatile NotBounties instance;
    private static volatile String latestVersion;
    private static volatile boolean updateAvailable = false;

    // Constants
    public static final String SESSION_KEY = UUID.randomUUID().toString();
    private static final String ADMIN_PERMISSION = "notbounties.admin";
    private static final int MAX_WANTED_TAG_UPDATE_MS = 10;

    // Configuration
    private static NamespacedKey namespacedKey;
    private static int serverVersion = 20;
    private static int serverSubVersion = 0;
    private static volatile boolean debug = false;
    private static volatile boolean paused = false;
    private static Events events;
    private boolean started = false;
    private static ServerImplementation serverImplementation;

    // Initialize
    @Override
    public void onLoad() {
        setInstance(this);
        ConfigOptions.setWorldGuardEnabled(getServer().getPluginManager().getPlugin("WorldGuard") != null);
        if (ConfigOptions.isWorldGuardEnabled()) {
            WorldGuardClass.registerFlags();
        }
        if (getServer().getPluginManager().getPlugin("Lands") != null) {
            new LandsClass().registerClaimFlag();
        }
    }

    @Override
    public void onEnable() {
        setServerImplementation(new FoliaCompatibility(this).getServerImplementation());
        
        // Easter egg
        if (new Random().nextInt(10) == 3) {
            Bukkit.getLogger().info("""
                ████████████████████████████████████████████████████████████████████
                █▄─▀█▄─▄█─▄▄─█─▄─▄─█▄─▄─▀█─▄▄─█▄─██─▄█▄─▀█▄─▄█─▄─▄─█▄─▄█▄─▄▄─█─▄▄▄▄█
                ██─█▄▀─██─██─███─████─▄─▀█─██─██─██─███─█▄▀─████─████─███─▄█▀█▄▄▄▄─█
                ▀▄▄▄▀▀▄▄▀▄▄▄▄▀▀▄▄▄▀▀▄▄▄▄▀▀▄▄▄▄▀▀▄▄▄▄▀▀▄▄▄▀▀▄▄▀▀▄▄▄▀▀▄▄▄▀▄▄▄▄▄▀▄▄▄▄▄▀""");
        }

        // Register commands and events
        Commands commands = new Commands();
        Objects.requireNonNull(getCommand("notbounties")).setExecutor(commands);
        Objects.requireNonNull(getCommand("notbountiesadmin")).setExecutor(commands);
        
        setEvents(new Events());
        registerEvents();

        // Initialize core components
        saveDefaultConfig();
        BountyMap.initialize();
        setNamespacedKey(new NamespacedKey(this, "bounty-entity"));
        readVersion();

        // Load data
        try {
            loadConfig();
            if (ChallengeManager.isEnabled()) {
                ChallengeManager.readChallengeData();
            }
            DataManager.loadData(this);
        } catch (IOException e) {
            logSevere("Failed to read player data!", e);
        }

        // Metrics
        if (sendBStats) {
            setupMetrics();
        }

        // Cleanup
        if (!saveTemplates) {
            BountyMap.cleanPosters();
        }

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BountyExpansion().register();
        }

        // Load skins
        loadBountySkins();

        // Force login existing players
        Bukkit.getOnlinePlayers().forEach(Events::login);

        // WorldGuard
        if (ConfigOptions.isWorldGuardEnabled()) {
            WorldGuardClass.registerHandlers();
        }

        // Update checker
        setupUpdateChecker();

        // Scheduled tasks
        setupScheduledTasks();

        started = true;
    }

    @Override
    public void onDisable() {
        if (ConfigOptions.isMmoLibEnabled()) {
            MMOLibClass.removeAllModifiers();
        }
        
        SkinManager.shutdown();
        DataManager.shutdown();
        
        if (!started) return;

        // Close GUIs
        Bukkit.getOnlinePlayers().forEach(player -> GUI.safeCloseGUI(player, true));

        // Save data
        try {
            save();
        } catch (IOException e) {
            logSevere("Error saving data!", e);
        }

        // Cleanup
        WantedTags.disableWantedTags();
        BountyBoard.clearBoard();
    }

    // Helper methods
    private void registerEvents() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(events, this);
        pm.registerEvents(new GUI(), this);
        pm.registerEvents(new CurrencySetup(), this);
        pm.registerEvents(new BountyMap(), this);
        pm.registerEvents(new PVPRestrictions(), this);
        pm.registerEvents(new WebhookOptions(), this);
        pm.registerEvents(new Prompt(), this);
        pm.registerEvents(new BountyTracker(), this);
        pm.registerEvents(new ChallengeListener(), this);
        pm.registerEvents(new RemovePersistentEntitiesEvent(), this);
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 20776);
        metrics.addCustomChart(new Metrics.SingleLineChart("active_bounties", 
            () -> BountyManager.getAllBounties(-1).size()));
    }

    private void loadBountySkins() {
        BountyManager.getAllBounties(-1).parallelStream()
            .forEach(bounty -> SkinManager.saveSkin(bounty.getUUID()));
    }

    private void setupUpdateChecker() {
        if (ConfigOptions.getUpdateNotification().equalsIgnoreCase("false")) return;
        
        new UpdateChecker(this, 104484).getVersion(version -> {
            latestVersion = version;
            if (getDescription().getVersion().contains("dev") || 
                getDescription().getVersion().equals(version)) {
                return;
            }

            updateAvailable = isNewerVersion(version);
            if (updateAvailable && !version.equals(ConfigOptions.getUpdateNotification())) {
                Bukkit.getLogger().info("[NotBounties] " + ChatColor.stripColor(
                    parse(getMessage("update-notification")
                        .replace("{current}", getDescription().getVersion())
                        .replace("{latest}", version), null)));
            }
        });
    }

    private boolean isNewerVersion(String newVersion) {
        String[] newParts = newVersion.split("\\.");
        String[] currentParts = getDescription().getVersion().split("\\.");
        
        for (int i = 0; i < Math.min(newParts.length, currentParts.length); i++) {
            try {
                int newNum = Integer.parseInt(newParts[i]);
                int currentNum = Integer.parseInt(currentParts[i]);
                if (newNum > currentNum) return true;
                if (newNum < currentNum) return false;
            } catch (NumberFormatException ignored) {}
        }
        return newParts.length > currentParts.length;
    }

    private void setupScheduledTasks() {
        // Main task (40 ticks = 2 seconds)
        getServerImplementation().global().runAtFixedRate(() -> {
            if (paused) return;
            
            BountyTracker.update();
            BigBounty.displayParticle();
            Immunity.update();
            RandomBounties.update();
            TimedBounties.update();
            PVPRestrictions.checkCombatExpiry();
            ChallengeManager.checkChallengeChange();
            BountyBoard.update();
        }, 100, 40);

        // Auto-save task
        getServerImplementation().async().runAtFixedRate(() -> {
            if (paused) return;
            
            MurderBounties.cleanPlayerKills();
            SkinManager.removeOldData();
            RemovePersistentEntitiesEvent.checkRemovedEntities();
            
            try {
                save();
            } catch (IOException e) {
                logSevere("Error autosaving data!", e);
            }
        }, autoSaveInterval * 60 * 20L + 69, autoSaveInterval * 60 * 20L);

        // Bounty expiration check
        getServerImplementation().async().runAtFixedRate(
            BountyExpire::removeExpiredBounties, 
            5 * 60 * 20L + 2007, 
            5 * 60 * 20L);

        // Banned players check
        getServerImplementation().global().runAtFixedRate(new BannedPlayerChecker(), 
            101, 72006);

        // Wanted tags update
        getServerImplementation().global().runAtFixedRate(new WantedTagUpdater(), 
            20, 1);
    }

    private class BannedPlayerChecker implements Runnable {
        private List<Bounty> bountyListCopy = new ArrayList<>();
        private int playersPerRun = 1;

        @Override
        public void run() {
            if (!removeBannedPlayers || paused) return;
            
            if (bountyListCopy.isEmpty()) {
                bountyListCopy = BountyManager.getAllBounties(-1);
                playersPerRun = Math.max(1, bountyListCopy.size() / 12);
            }

            Iterator<Bounty> iterator = bountyListCopy.iterator();
            while (iterator.hasNext() && playersPerRun-- > 0) {
                Bounty bounty = iterator.next();
                OfflinePlayer player = Bukkit.getOfflinePlayer(bounty.getUUID());
                if (player == null) continue;
                
                getServerImplementation().async().runNow(() -> {
                    if (player.isBanned() || 
                        (liteBansEnabled && !new LiteBansClass().isPlayerNotBanned(bounty.getUUID()))) {
                        BountyManager.removeBounty(bounty.getUUID());
                    }
                });
                iterator.remove();
            }
        }
    }

    private class WantedTagUpdater implements Runnable {
        private int lastUpdateTime = 0;
        private long lastRunTime = 0;

        @Override
        public void run() {
            if (Bukkit.getOnlinePlayers().isEmpty()) return;
            
            if (lastUpdateTime > MAX_WANTED_TAG_UPDATE_MS && 
                System.currentTimeMillis() - lastRunTime < (lastUpdateTime - MAX_WANTED_TAG_UPDATE_MS) * 2L) {
                return;
            }

            long startTime = System.currentTimeMillis();
            WantedTags.update();
            lastUpdateTime = (int) (System.currentTimeMillis() - startTime);
            
            if (lastUpdateTime > MAX_WANTED_TAG_UPDATE_MS) {
                lastRunTime = System.currentTimeMillis();
                if (debug) {
                    debugMessage("Took " + lastUpdateTime + "ms to update wanted tags. Pausing for a few updates.", false);
                }
            }
        }
    }

    // Data saving
    public void save() throws IOException {
        File dataDirectory = new File(getDataFolder(), "data");
        if (dataDirectory.mkdir()) {
            debugMessage("Created new data directory", false);
        }

        saveBounties(dataDirectory);
        saveStats(dataDirectory);
        savePlayerData(dataDirectory);
    }

    private void saveBounties(File dataDirectory) throws IOException {
        File bountiesFile = new File(dataDirectory, "bounties.json");
        if (bountiesFile.createNewFile()) {
            debugMessage("Created a new bounties.json file.", false);
        }

        try (JsonWriter writer = new JsonWriter(new FileWriter(bountiesFile))) {
            writer.beginArray();
            BountyTypeAdapter adapter = new BountyTypeAdapter();
            DataManager.getLocalBounties().forEach(bounty -> adapter.write(writer, bounty));
            writer.endArray();
        }
    }

    private void saveStats(File dataDirectory) throws IOException {
        File statsFile = new File(dataDirectory, "player_stats.json");
        if (statsFile.createNewFile()) {
            debugMessage("Created a new player_stats.json file.", false);
        }

        try (JsonWriter writer = new JsonWriter(new FileWriter(statsFile))) {
            writer.beginArray();
            PlayerStatAdapter adapter = new PlayerStatAdapter();
            DataManager.getLocalStats().forEach(entry -> {
                writer.beginObject();
                writer.name("uuid").value(entry.getKey().toString());
                writer.name("stats");
                adapter.write(writer, entry.getValue());
                writer.endObject();
            });
            writer.endArray();
        }
    }

    private void savePlayerData(File dataDirectory) throws IOException {
        File playerDataFile = new File(dataDirectory, "player_data.json");
        if (playerDataFile.createNewFile()) {
            debugMessage("Created a new player_data.json file.", false);
        }

        try (JsonWriter writer = new JsonWriter(new FileWriter(playerDataFile))) {
            writer.beginObject();
            
            // Players
            writer.name("players");
            writer.beginArray();
            PlayerDataAdapter playerAdapter = new PlayerDataAdapter();
            DataManager.getPlayerDataMap().forEach((uuid, data) -> {
                writer.beginObject();
                writer.name("uuid").value(uuid.toString());
                writer.name("data");
                playerAdapter.write(writer, data);
                writer.endObject();
            });
            writer.endArray();

            // Tracked bounties
            writer.name("trackedBounties");
            writer.beginArray();
            BountyTracker.getTrackedBounties().forEach((id, uuid) -> {
                writer.beginObject();
                writer.name("id").value(id);
                writer.name("uuid").value(uuid.toString());
                writer.endObject();
            });
            writer.endArray();

            // Database sync times
            writer.name("databaseSyncTimes");
            writer.beginArray();
            DataManager.getDatabases().stream()
                .filter(db -> !db.isPermDatabase() && 
                    System.currentTimeMillis() - db.getLastSync() < DataManager.CONNECTION_REMEMBRANCE_MS)
                .forEach(db -> {
                    writer.beginObject();
                    writer.name("name").value(db.getName());
                    writer.name("time").value(db.getLastSync());
                    writer.endObject();
                });
            writer.endArray();

            // Random bounties
            if (RandomBounties.isEnabled()) {
                writer.name("nextRandomBounty").value(RandomBounties.getNextRandomBounty());
            }

            // Timed bounties
            if (TimedBounties.isEnabled()) {
                writer.name("nextTimedBounties");
                writer.beginArray();
                TimedBounties.getNextBounties().forEach((uuid, time) -> {
                    writer.beginObject();
                    writer.name("uuid").value(uuid.toString());
                    writer.name("time").value(time);
                    writer.endObject();
                });
                writer.endArray();
            }

            // Bounty boards
            writer.name("bountyBoards");
            writer.beginArray();
            BountyBoardTypeAdapter boardAdapter = new BountyBoardTypeAdapter();
            BountyBoard.getBountyBoards().forEach(board -> boardAdapter.write(writer, board));
            writer.endArray();

            // Misc
            writer.name("nextChallengeChange").value(ChallengeManager.getNextChallengeChange());
            writer.name("serverID").value(DataManager.getDatabaseServerID(false).toString());
            writer.name("paused").value(isPaused());

            // Wanted tags
            List<String> wantedTagLocations = DataManager.locationListToStringList(WantedTags.getLocations());
            if (!wantedTagLocations.isEmpty()) {
                writer.name("wantedTagLocations");
                writer.beginArray();
                wantedTagLocations.forEach(writer::value);
                writer.endArray();
            }

            writer.endObject();
        }

        // Backups
        saveBackup(new File(dataDirectory, "bounties.json"));
        saveBackup(new File(dataDirectory, "player_stats.json"));
        saveBackup(new File(dataDirectory, "player_data.json"));
    }

    private void saveBackup(File file) throws IOException {
        if (!bountyBackups) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
        File backupDir = new File(getDataFolder(), "backups");
        File todayDir = new File(backupDir, dateFormat.format(new Date()));

        if (backupDir.mkdir()) {
            Bukkit.getLogger().info("[NotBounties] Created backup directory.");
        }

        if (todayDir.mkdir()) {
            cleanOldBackups(backupDir, dateFormat);
        }

        Files.copy(file, new File(todayDir, file.getName()));
    }

    private void cleanOldBackups(File backupDir, SimpleDateFormat dateFormat) {
        try {
            File[] backupFolders = backupDir.listFiles();
            if (backupFolders == null) return;

            // Sort by date (oldest first)
            Arrays.sort(backupFolders, Comparator.comparing(f -> {
                try {
                    return dateFormat.parse(f.getName()).getTime();
                } catch (ParseException e) {
                    return Long.MAX_VALUE;
                }
            }));

            // Keep only the 3 most recent weekly backups
            for (int i = 0; i < backupFolders.length - 3; i++) {
                deleteDirectory(backupFolders[i]);
            }
        } catch (Exception e) {
            debugMessage("Failed to clean old backups: " + e.getMessage(), true);
        }
    }

    private void deleteDirectory(File dir) {
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (!file.delete()) {
                    debugMessage("Failed to delete backup file: " + file, true);
                }
            }
        }
        if (!dir.delete()) {
            debugMessage("Failed to delete backup directory: " + dir, true);
        }
    }

    // Utility methods
    private static void readVersion() {
        try {
            String version = Bukkit.getBukkitVersion().split("-")[0].substring(2);
            String[] parts = version.split("\\.");
            serverVersion = Integer.parseInt(parts[0]);
            serverSubVersion = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NotBounties] Could not parse server version. Defaulting to 1.20.");
            serverVersion = 20;
            serverSubVersion = 0;
        }
    }

    public static boolean isVanished(Player player) {
        return player.getMetadata("vanished").stream().anyMatch(MetadataValue::asBoolean);
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (uuid == null) return false;
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (ConfigOptions.floodgateEnabled) {
                return new FloodGateClass().isBedrockPlayer(uuid);
            }
            if (ConfigOptions.geyserEnabled) {
                return new GeyserMCClass().isBedrockPlayer(uuid);
            }
        }
        return uuid.toString().startsWith("00000000-0000-0000");
    }

    public static String getXuid(UUID uuid) {
        if (uuid == null) return "";
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (ConfigOptions.floodgateEnabled) {
                FloodGateClass floodgate = new FloodGateClass();
                if (floodgate.isBedrockPlayer(uuid)) {
                    return floodgate.getXuid(uuid);
                }
            }
            if (ConfigOptions.geyserEnabled) {
                GeyserMCClass geyser = new GeyserMCClass();
                if (geyser.isBedrockPlayer(uuid)) {
                    return geyser.getXuid(uuid);
                }
            }
        }
        
        if (isBedrockPlayer(uuid)) {
            try {
                return Long.parseLong(uuid.toString().replace("-", "").substring(16), 16) + "";
            } catch (NumberFormatException ignored) {}
        }
        return "";
    }

    public static void debugMessage(String message, boolean warning) {
        if (!debug) return;
        
        String formatted = "[NotBountiesDebug] " + message;
        if (getInstance().isEnabled()) {
            getServerImplementation().global().run(() -> consoleMessage(formatted, warning));
        } else {
            consoleMessage(formatted, warning);
        }
    }

    private static void consoleMessage(String message, boolean warning) {
        if (warning) {
            Bukkit.getLogger().warning(message);
        } else {
            Bukkit.getLogger().info(message);
        }
    }

    private void logSevere(String message, Exception e) {
        Bukkit.getLogger().severe("[NotBounties] " + message);
        Bukkit.getLogger().severe(e.toString());
        Arrays.stream(e.getStackTrace())
              .limit(5)
              .forEach(stack -> Bukkit.getLogger().severe("       at " + stack));
    }

    // Getters and setters
    public static NotBounties getInstance() {
        return instance;
    }

    private static void setInstance(NotBounties instance) {
        NotBounties.instance = instance;
    }

    public static NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public static void setNamespacedKey(NamespacedKey key) {
        namespacedKey = key;
    }

    public static void setEvents(Events events) {
        NotBounties.events = events;
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static void setPaused(boolean paused) {
        NotBounties.paused = paused;
        if (paused) {
            // Pause logic
            BountyBoard.getBountyBoards().forEach(BountyBoard::remove);
            Bukkit.getOnlinePlayers().forEach(player -> GUI.safeCloseGUI(player, true));
            WantedTags.disableWantedTags();
            Bukkit.getOnlinePlayers().forEach(player -> events.onQuit(new PlayerQuitEvent(player, "")));
        } else {
            // Resume logic
            Bukkit.getOnlinePlayers().forEach(player -> events.onJoin(new PlayerJoinEvent(player, "")));
        }
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

    private static void setServerImplementation(ServerImplementation impl) {
        serverImplementation = impl;
    }

    public static String getAdminPermission() {
        return ADMIN_PERMISSION;
    }

    public static Map<UUID, String> getNetworkPlayers() {
        return DataManager.getNetworkPlayers();
    }

    public static boolean isAboveVersion(int major, int sub) {
        return serverVersion > major || (serverVersion == major && serverSubVersion >= sub);
    }
}

package me.jadenp.notbounties.utils;

import com.cjcrafter.foliascheduler.TaskImplementation;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.*;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.data.player_data.*;
import me.jadenp.notbounties.features.settings.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.features.settings.databases.LocalData;
import me.jadenp.notbounties.features.settings.databases.NotBountiesDatabase;
import me.jadenp.notbounties.features.settings.databases.TempDatabase;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyDatabase;
import me.jadenp.notbounties.features.settings.databases.redis.RedisConnection;
import me.jadenp.notbounties.features.settings.databases.sql.MySQL;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.settings.auto_bounties.BigBounty;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.auto_bounties.RandomBounties;
import me.jadenp.notbounties.features.settings.auto_bounties.TimedBounties;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.integrations.external_api.MMOLibClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import static me.jadenp.notbounties.features.LanguageOptions.*;

public class DataManager {

    private DataManager(){}

    private static final List<AsyncDatabaseWrapper> databases = new ArrayList<>();

    private static LocalData localData; // locally stored bounties and stats
    private static UUID databaseServerID = null;
    public static final long CONNECTION_REMEMBRANCE_MS = (long) 2.592e+8; // how long before databases stop storing changes if no connection was made (3 days)
    public static final UUID GLOBAL_SERVER_ID = new UUID(0,0);
    private static final long MIN_DATABASE_SYNC_INTERVAL_MS = 1000; // for reading the priority database for getting local data on save
    private static long lastPriorityDatabaseSync = 0;
    private static TaskImplementation<Void> autoReconnectTask = null;

    private static Plugin plugin;

    public static void loadData(Plugin plugin) throws IOException {
        DataManager.plugin = plugin;
        localData = new LocalData();
        loadOldData();
        // load modern data
        SaveManager.read(plugin);


    }



    protected static void setDatabaseServerID(UUID databaseServerID) {
        DataManager.databaseServerID = databaseServerID;
    }



    public static LocalData getLocalData() {
        return localData;
    }

    public static void connectProxy(List<Bounty> bounties, Map<UUID, PlayerStat> playerStatMap, List<PlayerData> playerDataMap) {
        // turn local data into proxy database
        NotBounties.getServerImplementation().async().runNow(task -> {
            for (AsyncDatabaseWrapper database : databases) {
                if (database.getDatabase() instanceof ProxyDatabase) {
                    syncDatabase(database.getDatabase(), bounties, playerStatMap, playerDataMap);
                }
            }
        });

    }

    /**
     * Load the database configuration.
     * @param configuration Configuration section holding the database configs.
     * @param plugin Plugin that is loading the configuration.
     */
    public static void loadDatabaseConfig(ConfigurationSection configuration, Plugin plugin) {
        if (autoReconnectTask != null)
            autoReconnectTask.cancel();
        if (configuration.isSet("auto-connect-interval")) {
            long autoConnectInterval = configuration.getLong("auto-connect-interval");
            if (autoConnectInterval > 0) {
                autoReconnectTask = NotBounties.getServerImplementation().async().runAtFixedRate(DataManager::tryDatabaseConnections, 120, autoConnectInterval * 20);
            }
        }
        for (String databaseName : configuration.getKeys(false)) {
            if (databaseName.equals("auto-connect-interval"))
                continue;
            boolean newDatabase = true;
            for (AsyncDatabaseWrapper database : databases) {
                if (database.getName().equals(databaseName)) {
                    database.reloadConfig();
                    newDatabase = false;
                    break;
                }
            }
            if (newDatabase) {
                try {
                    loadNewDatabaseConfig(configuration, databaseName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(e.toString());
                }
            }
        }
        Collections.sort(databases);
        tryDatabaseConnections();

    }

    /**
     * Load a new database config into the system.
     * @param configuration Database configuration. The name of the database should be at the top of this
     *                      configuration section.
     * @param databaseName Name of the database.
     * @throws IllegalArgumentException If the database type specified is not defined.
     */
    private static void loadNewDatabaseConfig(ConfigurationSection configuration, String databaseName) throws IllegalArgumentException {
        String type = configuration.isSet(databaseName + ".type") ? configuration.getString(databaseName + ".type") : "You need to set your database type for: " + databaseName;
        assert type != null; // isSet() assures that type != null
        NotBountiesDatabase database;
        try {
            switch (type.toUpperCase()) {
                case "SQL" -> database = new MySQL(NotBounties.getInstance(), databaseName);
                case "REDIS" -> database = new RedisConnection(NotBounties.getInstance(), databaseName);
                case "PROXY" -> database = new ProxyDatabase(NotBounties.getInstance(), databaseName);
                default -> throw new IllegalArgumentException("Unknown database type for " + databaseName + ": " + type);

            }
            AsyncDatabaseWrapper asyncDatabaseWrapper = new AsyncDatabaseWrapper(database);
            SaveManager.loadSyncTime(asyncDatabaseWrapper);
            asyncDatabaseWrapper.reloadConfig();
            databases.add(asyncDatabaseWrapper);
        } catch (NoClassDefFoundError e) {
            // Couldn't load a dependency.
            // This will be thrown if unable to use Spigot's library loader
            NotBounties.debugMessage("One or more dependencies could not be downloaded to use the database: " + databaseName + " (" + type + ")", true);
        }
    }

    /**
     * Loads bounties from the bounties.yml file.
     * To be removed 1/8/2026
     */
    @Deprecated(since = "1.22.0", forRemoval = true)
    private static void loadOldData() {
        File bounties = new File(NotBounties.getInstance().getDataFolder() + File.separator + "bounties.yml");
        if (!bounties.exists())
            return;
        List<Bounty> bountyList = new ArrayList<>();
        Map<UUID, PlayerStat> stats = new HashMap<>();
        // create bounties file if one doesn't exist
        try {
                // get existing bounties file
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(bounties);
                if (configuration.isSet("server-id"))
                    databaseServerID = UUID.fromString(Objects.requireNonNull(configuration.getString("server-id")));
                else
                    databaseServerID = UUID.randomUUID();
                // add all previously logged on players to a map
                if (configuration.isConfigurationSection("logged-players"))
                    LoggedPlayers.readOldConfiguration(Objects.requireNonNull(configuration.getConfigurationSection("logged-players")));
                Map<UUID, Boolean[]> immunityMap = new HashMap<>();
                if (configuration.isSet("immune-permissions")) {
                    for (String str :configuration.getStringList("immune-permissions")) {
                        UUID uuid = UUID.fromString(str);
                        if (immunityMap.containsKey(uuid))
                            immunityMap.get(uuid)[0] = true;
                        else
                            immunityMap.put(uuid, new Boolean[]{true, false, false, false});
                    }
                }
            if (configuration.isSet("immunity-murder")) {
                for (String str :configuration.getStringList("immunity-murder")) {
                    UUID uuid = UUID.fromString(str);
                    if (immunityMap.containsKey(uuid))
                        immunityMap.get(uuid)[1] = true;
                    else
                        immunityMap.put(uuid, new Boolean[]{false, true, false, false});
                }
            }
            if (configuration.isSet("immunity-random")) {
                for (String str :configuration.getStringList("immunity-random")) {
                    UUID uuid = UUID.fromString(str);
                    if (immunityMap.containsKey(uuid))
                        immunityMap.get(uuid)[2] = true;
                    else
                        immunityMap.put(uuid, new Boolean[]{false, false, true, false});
                }
            }
            if (configuration.isSet("immunity-timed")) {
                for (String str :configuration.getStringList("immunity-timed")) {
                    UUID uuid = UUID.fromString(str);
                    if (immunityMap.containsKey(uuid))
                        immunityMap.get(uuid)[3] = true;
                    else
                        immunityMap.put(uuid, new Boolean[]{false, false, false, true});
                }
            }
            for (Map.Entry<UUID, Boolean[]> entry : immunityMap.entrySet()) {
                PlayerData playerData = localData.getPlayerData(entry.getKey());
                playerData.setGeneralImmunity(entry.getValue()[0]);
                playerData.setMurderImmunity(entry.getValue()[1]);
                playerData.setRandomImmunity(entry.getValue()[2]);
                playerData.setTimedImmunity(entry.getValue()[3]);
            }
                // go through bounties in file
                int i = 0;
                while (configuration.getString("bounties." + i + ".uuid") != null) {
                    List<Setter> setters = new ArrayList<>();
                    int l = 0;
                    while (configuration.getString("bounties." + i + "." + l + ".uuid") != null) {
                        List<String> whitelistUUIDs = new ArrayList<>();
                        if (configuration.isSet("bounties." + i + "." + l + ".whitelist"))
                            whitelistUUIDs = configuration.getStringList("bounties." + i + "." + l + ".whitelist");
                        boolean blacklist = configuration.isSet("bounties." + i + "." + l + ".blacklist") && configuration.getBoolean("bounties." + i + "." + l + ".blacklist");

                        SortedSet<UUID> convertedUUIDs = new TreeSet<>();
                        for (String uuid : whitelistUUIDs)
                            convertedUUIDs.add(UUID.fromString(uuid));
                        // check for old CONSOLE UUID
                        UUID setterUUID = Objects.requireNonNull(configuration.getString("bounties." + i + "." + l + ".uuid")).equalsIgnoreCase("CONSOLE") ? new UUID(0, 0) : UUID.fromString(Objects.requireNonNull(configuration.getString("bounties." + i + "." + l + ".uuid")));
                        // check for no playtime
                        long playTime = configuration.isSet("bounties." + i + "." + l + ".playtime") ? configuration.getLong("bounties." + i + "." + l + ".playtime") : 0;
                        ArrayList<ItemStack> items = configuration.isString("bounties." + i + "." + l + ".items") ? new ArrayList<>(List.of(SerializeInventory.itemStackArrayFromBase64(configuration.getString("bounties." + i + "." + l + ".items")))) : new ArrayList<>();
                        Setter setter = new Setter(configuration.getString("bounties." + i + "." + l + ".name"), setterUUID, configuration.getDouble("bounties." + i + "." + l + ".amount"), items, configuration.getLong("bounties." + i + "." + l + ".time-created"), configuration.getBoolean("bounties." + i + "." + l + ".notified"), new Whitelist(convertedUUIDs, blacklist), playTime);
                        setters.add(setter);
                        l++;
                    }
                    if (!setters.isEmpty()) {
                        bountyList.add(new Bounty(UUID.fromString(Objects.requireNonNull(configuration.getString("bounties." + i + ".uuid"))), setters, configuration.getString("bounties." + i + ".name")));
                    }
                    i++;
                }
                // new version vvv
                Map<UUID, Long> timedBounties = new HashMap<>();
                if (configuration.isConfigurationSection("data"))
                    for (String uuidString : Objects.requireNonNull(configuration.getConfigurationSection("data")).getKeys(false)) {
                        UUID uuid = UUID.fromString(uuidString);
                        PlayerData playerData = localData.getPlayerData(uuid);
                        // old data protection
                        if (uuidString.length() < 10 || uuid.equals(DataManager.GLOBAL_SERVER_ID))
                            continue;
                        Double[] stat = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
                        if (configuration.isSet("data." + uuid + ".kills"))
                            stat[0] = (double) configuration.getLong("data." + uuid + ".kills");
                        if (configuration.isSet("data." + uuid + ".set"))
                            stat[1] = (double) configuration.getLong("data." + uuid + ".set");
                        if (configuration.isSet("data." + uuid + ".deaths"))
                            stat[2] = (double) configuration.getLong("data." + uuid + ".deaths");
                        if (configuration.isSet("data." + uuid + ".all-time"))
                            stat[3] = configuration.getDouble("data." + uuid + ".all-time");
                        if (configuration.isSet("data." + uuid + ".immunity"))
                            stat[4] = configuration.getDouble("data." + uuid + ".immunity");
                        if (configuration.isSet("data." + uuid + ".all-claimed"))
                            stat[5] = configuration.getDouble("data." + uuid + ".all-claimed");
                        stats.put(uuid, new PlayerStat(stat[0].longValue(), stat[1].longValue(), stat[2].longValue(), stat[3], stat[4], stat[5], databaseServerID));
                        if (configuration.isSet("data." + uuid + ".next-bounty"))
                            timedBounties.put(uuid, configuration.getLong("data." + uuid + ".next-bounty"));
                        if (Whitelist.isVariableWhitelist() && configuration.isSet("data." + uuid + ".whitelist"))
                            try {
                                localData.getPlayerData(uuid).setWhitelist(new Whitelist(new TreeSet<>(configuration.getStringList("data." + uuid + ".whitelist").stream().map(UUID::fromString).toList()), configuration.getBoolean("data." + uuid + ".blacklist")));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Failed to get whitelisted uuids from: " + uuid + "\nThis list will be overwritten in 5 minutes");
                            }
                        if (configuration.isSet("data." + uuid + ".refund"))
                            playerData.addRefund(new AmountRefund(configuration.getDouble("data." + uuid + ".refund"), null));
                        if (configuration.isSet("data." + uuid + ".refund-items"))
                            try {
                                playerData.addRefund(new ItemRefund(Arrays.asList(SerializeInventory.itemStackArrayFromBase64(configuration.getString("data." + uuid + ".refund-items"))), null));
                            } catch (IOException e) {
                                plugin.getLogger().warning("Unable to load item refund for player using this encoded data: " + configuration.getString("data." + uuid + ".refund-items"));
                                plugin.getLogger().warning(e.toString());
                            }
                        if (configuration.isSet("data." + uuid + ".time-zone"))
                            LocalTime.addTimeZone(uuid, configuration.getString("data." + uuid + ".time-zone"));
                        if (ConfigOptions.getBountyCooldown() > 0 && configuration.isSet("data." + uuid + ".last-set"))
                            playerData.setBountyCooldown(configuration.getLong("data." + uuid + ".last-set"));
                    }
                if (!timedBounties.isEmpty())
                    TimedBounties.setNextBounties(timedBounties);
                if (configuration.isSet("disable-broadcast"))
                    configuration.getStringList("disable-broadcast").forEach(s -> localData.getPlayerData(UUID.fromString(s)).setBroadcastSettings(PlayerData.BroadcastSettings.DISABLE));

                i = 0;
                while (configuration.getString("head-rewards." + i + ".setter") != null) {
                    try {
                        PlayerData playerData = localData.getPlayerData(UUID.fromString(Objects.requireNonNull(configuration.getString("head-rewards." + i + ".setter"))));
                        for (String str : configuration.getStringList("head-rewards." + i + ".uuid")) {
                            playerData.addRefund(RewardHead.decodeRewardHead(str));
                        }
                    } catch (IllegalArgumentException | NullPointerException e) {
                        plugin.getLogger().warning("Invalid UUID for head reward #" + i);
                    }
                    i++;
                }
                BiMap<Integer, UUID> trackedBounties = HashBiMap.create();
                i = 0;
                while (configuration.getString("tracked-bounties." + i + ".uuid") != null) {
                    try {
                        UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString("tracked-bounties." + i + ".uuid")));
                        trackedBounties.put(configuration.getInt("tracked-bounties." + i + ".number"), uuid);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Could not convert tracked string to uuid: " + configuration.getString("tracked-bounties." + i + ".uuid"));
                    }
                    i++;
                }
                BountyTracker.setTrackedBounties(trackedBounties);

                if (configuration.isSet("next-random-bounty"))
                    RandomBounties.setNextRandomBounty(configuration.getLong("next-random-bounty"));
                if (!RandomBounties.isEnabled()) {
                    RandomBounties.setNextRandomBounty(0);
                } else if (RandomBounties.getNextRandomBounty() == 0) {
                    RandomBounties.setNextRandomBounty();
                }
                if (configuration.isConfigurationSection("bounty-boards"))
                    for (String str : Objects.requireNonNull(configuration.getConfigurationSection("bounty-boards")).getKeys(false)) {
                        Location location;
                        if (configuration.isLocation("bounty-boards." + str + ".location"))
                            location = configuration.getLocation("bounty-boards." + str + ".location");
                        else
                            location = deserializeLocation(Objects.requireNonNull(configuration.getConfigurationSection("bounty-boards." + str + ".location")));
                        if (location == null)
                            continue;
                        BountyBoard.addBountyBoard(new BountyBoard(location, BlockFace.valueOf(configuration.getString("bounty-boards." + str + ".direction")), configuration.getInt("bounty-boards." + str + ".rank")));
                    }
                if (configuration.isSet("next-challenge-change")) {
                    ChallengeManager.setNextChallengeChange(configuration.getLong("next-challenge-change"));
                } else {
                    ChallengeManager.setNextChallengeChange(1);
                }
                if (configuration.isBoolean("paused"))
                    NotBounties.setPaused(configuration.getBoolean("paused"));
                // load database sync times
                if (configuration.isConfigurationSection("database-sync-times")) {
                    for (String key : Objects.requireNonNull(configuration.getConfigurationSection("database-sync-times")).getKeys(false)) {
                        for (NotBountiesDatabase database : databases) {
                            if (database.getName().equals(key)) {
                                database.setLastSync(configuration.getLong("database-sync-times." + key));
                            }
                        }
                    }
                }
                if (configuration.isList("wanted-tags")) {
                    List<Location> locations = stringListToLocationList(configuration.getStringList("wanted-tags"));
                    NotBounties.getServerImplementation().global().runDelayed(task -> {
                        RemovePersistentEntitiesEvent.cleanChunks(locations);
                    }, 100);
                }

                // delete old file
                java.nio.file.Files.delete(bounties.toPath());

        } catch (IOException e) {
            plugin.getLogger().severe("Error loading saved data!");
            plugin.getLogger().severe(e.toString());
        }
        localData.addBounty(bountyList);
        localData.addStats(stats);
    }

    /**
     * Get the server ID that was set in the config for the SQL Database.
     * @param newData If the serverID is for new data generation.
     * @return The set server ID for NotBounties.
     */
    public static UUID getDatabaseServerID(boolean newData) {
        if (newData && isPermDatabaseConnected())
            return GLOBAL_SERVER_ID;
        if (databaseServerID == null) {
            plugin.getLogger().info("Generating new database ID.");
            databaseServerID = UUID.randomUUID();
        }
        return databaseServerID;
    }

    /**
     * Get players on the network
     * @return All players online in the SQL database, or all server players if the database isn't connected
     */
    public static Map<UUID, String> getNetworkPlayers() {
        for (AsyncDatabaseWrapper database : databases) {
            if (database.isConnected())
                return database.getOnlinePlayers();
        }
        return localData.getOnlinePlayers();
    }


    public static double getStat(UUID uuid, Leaderboard leaderboard) {
        if (leaderboard == Leaderboard.CURRENT) {
            Bounty bounty = getBounty(uuid);
            if (bounty != null)
                return bounty.getTotalDisplayBounty();
            else
                return 0;
        }
        Map<UUID, PlayerStat> stats = getAllStats();
        if (stats.containsKey(uuid)) {
            return stats.get(uuid).leaderboardType(leaderboard);
        } else {
            return 0;
        }
    }

    public static List<AsyncDatabaseWrapper> getDatabases() {
        return databases;
    }

    public static Map<UUID, PlayerStat> getAllStats() {
        return localData.getAllStats();
    }

    public static List<String> locationListToStringList(List<Location> locations) {
        List<String> stringList = new ArrayList<>(locations.size());
        for (Location location : locations) {
            if (location == null || location.getWorld() == null)
                continue;
            stringList.add(location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getWorld().getUID());
        }
        return stringList;
    }

    public static List<Location> stringListToLocationList(List<String> stringList) {
        List<Location> locations = new ArrayList<>(stringList.size());
        for (String str : stringList) {
            String[] split = str.split(",");
            if (split.length != 4)
                continue;
            try {
                locations.add(new Location(Bukkit.getWorld(UUID.fromString(split[3])), Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2])));
            } catch (IllegalArgumentException ignored) {
                // couldn't parse location from string
            }
        }
        return locations;
    }

    /**
     * Change stats of the player. Current stat cannot be changed here.
     * @param uuid UUID of the player.
     * @param leaderboard Stat to change.
     * @param change The amount the stat should change by.
     */
    public static void changeStat(UUID uuid, Leaderboard leaderboard, double change) {
        PlayerStat statChange = PlayerStat.fromLeaderboard(leaderboard, change);
        changeStats(uuid, statChange);
    }

    public static void changeStats(UUID uuid, PlayerStat changes) {
        localData.addStats(uuid, changes);
        for (AsyncDatabaseWrapper database : databases) {
            database.addStats(uuid, changes);
        }
    }

    private static @Nullable Location deserializeLocation(ConfigurationSection configuration) {
        String worldUUID = configuration.getString("world");
        if (worldUUID == null)
            return null;
        double x = configuration.getDouble("x");
        double y = configuration.getDouble("y");
        double z = configuration.getDouble("z");
        double pitch = configuration.getDouble("pitch");
        double yaw = configuration.getDouble("yaw");
        try {
            World world = Bukkit.getWorld(UUID.fromString(worldUUID));
            if (world == null)
                return null;
            return new Location(world, x, y, z, (float) pitch, (float) yaw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static @Nullable Bounty getBounty(UUID receiver) {
        for (Bounty bounty : localData.getAllBounties(-1)) {
            if (bounty.getUUID().equals(receiver)) {
                return bounty;
            }
        }
        return null;
    }

    public static boolean hasBounty(UUID receiver) {
            return getBounty(receiver) != null;
    }

    /**
     * Get a copy of all bounties on the server.
     * @param sortType
     *   <p>-1 : unsorted</p>
     *   <p> 0 : newer bounties at top</p>
     *   <p> 1 : older bounties at top</p>
     *   <p> 2 : more expensive bounties at top</p>
     *   <p> 3 : less expensive bounties at top</p>
     * @return A copy of all bounties on the server.
     */
    public static List<Bounty> getAllBounties(int sortType) {
        return sortBounties(sortType);
    }

    private static void readPriorityDatabase() {
        if (System.currentTimeMillis() - lastPriorityDatabaseSync < MIN_DATABASE_SYNC_INTERVAL_MS)
            // recently read the database
            return;
        lastPriorityDatabaseSync = System.currentTimeMillis();
        // load data from priority database
        // store bounties and stats with a server id
        for (AsyncDatabaseWrapper database : databases) {
            if (database.isConnected()) {
                // highest priority database
                database.readDatabaseData(true);
                break;
            }
        }
    }
    /**
     * Get the stats that are only stored locally. This will load sql data if possible.
     * @return Stats to be stored locally.
     */
    public static Set<Map.Entry<UUID, PlayerStat>> getLocalStats(){
        return getAllStats().entrySet().stream().filter(entry -> entry.getValue().serverID().equals(databaseServerID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Get a list of bounties that are only stored locally. This will load sql data if possible.
     * @return Bounties to be stored locally.
     */
    public static Set<Bounty> getLocalBounties() {
        return getAllBounties(-1).stream().filter(bounty -> bounty.getServerID().equals(databaseServerID)).collect(Collectors.toUnmodifiableSet());
    }

    public static Set<PlayerData> getLocalPlayerData() {
        return getAllPlayerData().stream().filter(playerData -> playerData.getServerID().equals(databaseServerID)).collect(Collectors.toUnmodifiableSet());
    }

    public static List<Bounty> sortBounties(int sortType) {
        return localData.getAllBounties(sortType);
    }

    /**
     * Remove a bounty from the active bounties
     * @param uuid UUID of the player to remove
     */
    public static void deleteBounty(UUID uuid) {
        for (AsyncDatabaseWrapper database : databases) {
            database.removeBounty(uuid);
        }
        localData.removeBounty(uuid);
    }

    /**
     * Shuts down database connections
     */
    public static void shutdown() {
        for (AsyncDatabaseWrapper database : databases) {
            database.shutdown();
        }
    }

    /**
     * Returns a bounty that is guaranteed to be active. This checks all connected databases
     * @param uuid UUID of the bountied player
     * @return The bounty for the player or null if the player doesn't have one
     */
    public static @Nullable Bounty getGuarrenteedBounty(UUID uuid) {
        // most up-to-date database
        for (AsyncDatabaseWrapper database : databases) {
            if (database.isConnected()) {
                try {
                    Bounty bounty = database.getBounty(uuid);
                    // compare inconsistency and update both databases
                    Bounty localBounty = localData.getBounty(uuid);
                    // check if the bounties are different
                    if ((bounty != null && !bounty.equals(localBounty)) || (localBounty != null && !localBounty.equals(bounty))) {
                        // update data storage
                        Bounty consistentBounty = bounty != null ? Inconsistent.compareInconsistentObjects(bounty, localBounty, database.getLastSync(), database.getName())
                                : Inconsistent.compareInconsistentObjects(localBounty, bounty, database.getLastSync(), database.getName());
                        localData.replaceBounty(uuid, consistentBounty);
                        database.replaceBounty(uuid, consistentBounty);
                        return consistentBounty;
                    }
                    return bounty;
                } catch (IOException e) {
                    // database not connected
                }
            }
        }
        return localData.getBounty(uuid);
    }

    /**
     * Edit a bounty
     * @param bounty Bounty to be edited
     * @param setterUUID UUID of the setter to be edited
     * @param change The amount the bounty should change
     * @return The new bounty or null if the bounty is not active
     */
    public static Bounty editBounty(@NotNull Bounty bounty, @Nullable UUID setterUUID, double change) {
        Setter lastSetter = null;
        if (setterUUID != null) {
            // edit a specific setter
            ListIterator<Setter> setterListIterator = bounty.getSetters().listIterator();
            while (setterListIterator.hasNext()) {
                if (change == 0)
                    break;
                Setter setter = setterListIterator.next();
                if (setter.getUuid().equals(setterUUID)) {
                    if (change < 0) {
                        // amount could go negative with change
                        // remove if change causes that and there are no items
                        if (setter.getDisplayAmount() + change < 0 && setter.getItems().isEmpty()) {
                            setterListIterator.remove();
                            change += setter.getDisplayAmount();
                            lastSetter = new Setter(setter.getName(), setterUUID, 0, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), 0);
                        } else if (setter.getAmount() > 0) {
                            // update amount (minimum 0)
                            double newAmount = Math.min(0, setter.getAmount() + change);
                            change+= setter.getAmount() - newAmount; // update amount needed to be changed
                            Setter newSetter = new Setter(setter.getName(), setterUUID, newAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() - (setter.getAmount() - newAmount));
                            setterListIterator.set(newSetter);
                            lastSetter = newSetter;
                        }
                    } else {
                        // setter amount won't go negative
                        // update setter amount
                        Setter newSetter = new Setter(setter.getName(), setterUUID, setter.getAmount() + change, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + change);
                        change = 0;
                        setterListIterator.set(newSetter);
                        lastSetter = newSetter;
                    }

                }
            }
        }
        if (change != 0) {
            // did not complete the desired amount change
            if (lastSetter == null) {
                // either no setter uuid was specified, or the specified uuid was not in the bounty
                // add a new setter
                bounty.addBounty(change, new ArrayList<>(), new Whitelist(new TreeSet<>(), false));
            } else {
                // edit last setter with the remaining change
                // this may cause the amount to be negative
                bounty.getSetters().remove(lastSetter);
                bounty.getSetters().add(new Setter(lastSetter.getName(), setterUUID, lastSetter.getAmount() + change, lastSetter.getItems(), lastSetter.getTimeCreated(), lastSetter.isNotified(), lastSetter.getWhitelist(), lastSetter.getReceiverPlaytime(), lastSetter.getDisplayAmount() + change));
            }
        }
        for (AsyncDatabaseWrapper database : databases) {
            database.replaceBounty(bounty.getUUID(), bounty);
        }
        localData.replaceBounty(bounty.getUUID(), bounty);
        return bounty;
    }

    /**
     * Checks if the player has any new bounties to be notified of and sends the offlineBounty message and updates their big bounty status.
     * @param player Player to notify.
     */
    public static void notifyBounty(Player player,  Bounty bounty) {
        if (bounty != null) {
            double addedAmount = 0;
            for (Setter setter : bounty.getSetters()) {
                if (!setter.isNotified()) {
                    player.sendMessage(parse(getPrefix() + getMessage("offline-bounty"), setter.getDisplayAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    setter.setNotified(true);
                    addedAmount += setter.getDisplayAmount();
                }
            }
            if (addedAmount > 0) {
                for (AsyncDatabaseWrapper database : databases) {
                    database.notifyBounty(player.getUniqueId());
                }
                localData.notifyBounty(player.getUniqueId());
                BigBounty.setBounty(player, bounty, addedAmount);
            }

            if (bounty.getTotalDisplayBounty() > BigBounty.getThreshold()) {
                BigBounty.addParticle(player.getUniqueId());
            }
        }
    }

    public static void login(@NotNull Player player) {
        for (AsyncDatabaseWrapper database : databases)
            database.login(player.getUniqueId(), player.getName());
        localData.login(player.getUniqueId(), player.getName());
    }

    public static void logout(@NotNull Player player) {
        for (AsyncDatabaseWrapper database : databases)
            database.logout(player.getUniqueId());
        localData.logout(player.getUniqueId());
    }

    public static PlayerData getPlayerData(@NotNull UUID uuid) {
        return localData.getPlayerData(uuid);
    }

    public static void handleRefund(PlayerData playerData) {
        // this is probably being called in an async thread
        NotBounties.getServerImplementation().global().run(() -> {
            // check if a refund can be processed (player online and refunds present)
            Player player = Bukkit.getPlayer(playerData.getUuid());
            if (player != null && !playerData.getRefund().isEmpty()) {
                NotBounties.debugMessage("Giving " + playerData.getPlayerName() + " a refund.", false);
                // copy and clear the refund to avoid concurrent modification
                List<OnlineRefund> refunds = new ArrayList<>(playerData.getRefund());
                // update the reference to the local data
                PlayerData localPlayerData = localData.getPlayerData(playerData.getUuid());
                localPlayerData.clearRefund();
                localPlayerData.setLastSeen(System.currentTimeMillis());
                for (OnlineRefund refund : refunds)
                    refund.giveRefund(player);
                // re-sync player data with the most up-to-date database
                NotBounties.getServerImplementation().async().runNow(() -> syncPlayerData(playerData.getUuid(), null));
            }
        });

    }

    /**
     * Synchronizes the data of a player across all connected databases and the local data store.
     * If discrepancies are found between the data, they will be resolved based on consistency checks.
     * The resolved or retrieved player data can be passed to the provided consumer, if applicable.
     *
     * @apiNote This method accesses databases. Use in an asynchronous thread.
     * @param uuid The unique identifier of the player whose data is being synchronized.
     * @param consumer An optional callback to handle the resolved or retrieved {@code PlayerData}. Can be null.
     */
    public static synchronized void syncPlayerData(UUID uuid, @Nullable Consumer<PlayerData> consumer) {
        // most up-to-date database
        for (AsyncDatabaseWrapper database : databases) {
            if (database.isConnected()) {
                try {
                    PlayerData playerData = database.getPlayerData(uuid);

                    PlayerData localPlayerData = localData.getPlayerData(uuid);

                    if ((playerData != null && !playerData.equals(localPlayerData)) || (localPlayerData != null && !localPlayerData.equals(playerData))) {
                        // update data storage
                        PlayerData consistentPlayerData = playerData != null ? Inconsistent.compareInconsistentObjects(playerData, localPlayerData, database.getLastSync(), database.getName())
                                : Inconsistent.compareInconsistentObjects(localPlayerData, null, database.getLastSync(), database.getName());
                        if (consistentPlayerData == null) {
                            consistentPlayerData = localPlayerData;
                        }
                        long now = System.currentTimeMillis();
                        consistentPlayerData.setLastSyncOverride(database.getName(), now);
                        localData.updatePlayerData(consistentPlayerData);
                        database.updatePlayerData(consistentPlayerData);
                        if (consumer != null)
                            consumer.accept(consistentPlayerData);
                        return;
                    }
                    // both are the same object
                    if (consumer != null)
                        consumer.accept(localPlayerData);
                    return;
                } catch (IOException e) {
                    // database not connected
                }
            }
        }
        if (consumer != null)
            consumer.accept(localData.getPlayerData(uuid));
    }

    public static List<PlayerData> getAllPlayerData() {
        try {
            return localData.getPlayerData();
        } catch (IOException e) {
            // comes from the NotBountiesDatabase interface
            // will not be thrown with LocalData implementation
            return new ArrayList<>();
        }
    }

    /**
     * A utility to modify both parameters to remove any setters that are similar.
     * For any setter with the same uuid and whitelist, amounts will be canceled out, and similar items will be removed.
     * The order of the parameters doesn't matter.
     */
   public static void removeSimilarSetters(List<Setter> masterSetterList, List<Setter> setterList) {
        // iterate through setters
        ListIterator<Setter> masterSetters = masterSetterList.listIterator();
        while (masterSetters.hasNext()) {
            Setter masterSetter = masterSetters.next();
            ListIterator<Setter> setters = setterList.listIterator();
            while (setters.hasNext()) {
                Setter setter = setters.next();
                // if master setters match, remove matching amount and items from both lists
                if (setter.getUuid().equals(masterSetter.getUuid()) && setter.getWhitelist().equals(masterSetter.getWhitelist())) {
                    double newSetterAmount = 0;
                    double newMasterAmount = masterSetter.getAmount() - setter.getAmount();
                    if (newMasterAmount < 0) {
                        // trying to remove too much from this setter
                        newSetterAmount = -1 * newMasterAmount;
                        newMasterAmount = 0;
                    }
                    removeSimilarItems(masterSetter.getItems(), setter.getItems());
                    // if setter is empty, remove
                    if (newSetterAmount == 0 && setter.getItems().isEmpty()) {
                        // empty setter
                        setters.remove();
                    } else {
                        // replace with new setter
                        setters.set(new Setter(setter.getName(), setter.getUuid(), newSetterAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), 0)); // display bounty doesn't matter because this setter will no longer exist outside this loop
                    }
                    if (newMasterAmount == 0 && masterSetter.getItems().isEmpty()) {
                        // empty setter
                        masterSetters.remove();
                        break;
                    } else {
                        // replace with new setter
                        masterSetter = new Setter(masterSetter.getName(), masterSetter.getUuid(), newMasterAmount, masterSetter.getItems(), masterSetter.getTimeCreated(), masterSetter.isNotified(), masterSetter.getWhitelist(), masterSetter.getReceiverPlaytime());
                        masterSetters.set(masterSetter);
                    }
                }
            }
        }
    }

    private static void removeSimilarItems(List<ItemStack> masterItemsList, List<ItemStack> setterItemsList) {
        ListIterator<ItemStack> masterItems = masterItemsList.listIterator();
        while (masterItems.hasNext()) {
            ItemStack masterItem = masterItems.next();
            ListIterator<ItemStack> setterItems = setterItemsList.listIterator();
            while (setterItems.hasNext()) {
                ItemStack setterItem = setterItems.next();
                if (masterItem.isSimilar(setterItem)) {
                    int setterAmount = 0;
                    int masterAmount = masterItem.getAmount() - setterItem.getAmount();
                    if (masterAmount < 0) {
                        setterAmount = -1 * masterAmount;
                        masterAmount = 0;
                    }
                    if (setterAmount == 0) {
                        setterItems.remove();
                    } else {
                        setterItem.setAmount(setterAmount);
                        setterItems.set(setterItem);
                    }
                    if (masterAmount == 0) {
                        masterItems.remove();
                        break;
                    } else {
                        masterItem.setAmount(masterAmount);
                        masterItems.set(masterItem);
                    }
                }
            }
        }
    }

    /**
     * Removes setters from all databases if the bounty contains them.
     * @param bounty Bounty that contains the setters.
     * @param setters Setters to be removed.
     */
    public static void removeSetters(@NotNull Bounty bounty, List<Setter> setters) {
        if (setters.isEmpty())
            return;
        // create copies of objects so the originals aren't modified
        Bounty bountyCopy = new Bounty(bounty);
        List<Setter> originalSetters = new ArrayList<>(bounty.getSetters());
        List<Setter> settersToRemove = new ArrayList<>(setters);
        // remove similar setters in the lists
        // any setters that weren't on the bounty are left in the settersToRemove list
        DataManager.removeSimilarSetters(bountyCopy.getSetters(), settersToRemove);
        // modify features that depend on a bounty change (this should probably be extracted to a method)
        if (bountyCopy.getTotalBounty() < BigBounty.getThreshold())
            BigBounty.removeParticle(bounty.getUUID());
        if (bountyCopy.getTotalDisplayBounty() < WantedTags.getMinWanted()) {
            // remove bounty tag
            NotBounties.getServerImplementation().global().run(() -> WantedTags.removeWantedTag(bounty.getUUID()));
            NotBounties.debugMessage("Removed wanted tag.", false);
        }
        if (ConfigOptions.getIntegrations().isMmoLibEnabled()) {
            Player player = Bukkit.getPlayer(bountyCopy.getUUID());
            if (player != null) {
                MMOLibClass.removeStats(player);
                MMOLibClass.addStats(player, bountyCopy.getTotalDisplayBounty());
            }
        }
        // update the bounty in the databases
        if (bountyCopy.getSetters().isEmpty()) {
            // no more setters left in the bounty - delete from databases
            deleteBounty(bounty.getUUID());
            BountyTracker.stopTracking(bounty.getUUID());
            for (Player p : Bukkit.getOnlinePlayers()) {
                BountyTracker.removeTracker(p);
            }
        } else {
            // there are setters remaining
            // check if the setter amounts were modified in the removeSimilarSetters method
            boolean allMatch = true;
            for (Setter setter : bountyCopy.getSetters()) {
                if (!originalSetters.contains(setter)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                // the individual amounts were not modified, so these setters can just be removed from the databases
                Bounty removedBounty = new Bounty(bounty.getUUID(), setters, bounty.getName(), bounty.getServerID());
                // setters that remain were unmodified.
                for (AsyncDatabaseWrapper database : databases)
                    database.removeBounty(removedBounty);
                localData.removeBounty(removedBounty);
            } else {
                // Setters that remain had their amounts modified from their original values.
                // The bounty has to be replaced with the new setter amounts.
                for (AsyncDatabaseWrapper database : databases)
                    database.replaceBounty(bounty.getUUID(), bountyCopy);
                localData.replaceBounty(bounty.getUUID(), bountyCopy);
            }

        }
    }

    /**
     * Inserts a bounty into the sorted bountyList
     * @return The new bounty. This bounty will be the combination of all bounties on the same person.
     */
    public static Bounty insertBounty(@Nullable Player setter, @NotNull OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        Bounty newBounty = setter == null ? new Bounty(receiver, amount, items, whitelist) : new Bounty(setter, receiver, amount, items, whitelist);

        for (AsyncDatabaseWrapper database : databases) {
            database.addBounty(newBounty);
        }
        return localData.addBounty(newBounty);


    }

    /**
     * Adds a bounty to the active bounties.
     * @param bounty Bounty to be added
     */
    public static void addBounty(Bounty bounty) {
        for (AsyncDatabaseWrapper database : databases) {
            database.addBounty(bounty);
        }
        localData.addBounty(bounty);
    }

    public static boolean isPermDatabaseConnected() {
        for (AsyncDatabaseWrapper database : databases) {
            if (database.isPermDatabase() && database.isConnected()) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable AsyncDatabaseWrapper getDatabase(@NotNull String name) {
        for (AsyncDatabaseWrapper database : databases) {
            if (database.getName().equals(name))
                return database;
        }
        return null;
    }

    /**
     * Called when a database connects.
     * A database could connect when:
     * - The server first starts. This is probably not the only server connected to the database.
     * - Lost connection to the database. Perm data would be saved, Temp data maybe not.
     * - The server restarted with an active temp database. Perm database will fall into the category above.
     * @param database Database that connected.
     */
    public static void databaseConnect(NotBountiesDatabase database) {
        // hasConnectedBefore will be false if this is the first time connecting
        if (NotBounties.getInstance().isEnabled()) {
            if (Bukkit.isPrimaryThread()) {
                NotBounties.getServerImplementation().async().runNow(task -> {
                    getAndSyncDatabase(database);
                });
            } else {
                getAndSyncDatabase(database);
            }
        }
    }

    public static void getAndSyncDatabase(NotBountiesDatabase database) {
        List<Bounty> databaseBounties;
        Map<UUID, PlayerStat> databaseStats;
        List<PlayerData> playerDataMap;

        try {
            databaseBounties = database.getAllBounties(2);
            databaseStats = database.getAllStats();
            playerDataMap = database.getPlayerData();
        } catch (IOException e) {
            plugin.getLogger().warning("Error while trying to connect to " + database.getName() + " (bad connection)");
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> NotBounties.debugMessage(stackTraceElement.toString(), false));
            AsyncDatabaseWrapper databaseWrapper = getDatabase(database.getName());
            if (databaseWrapper != null) {
                databaseWrapper.disconnect();
            } else {
                database.disconnect();
            }
            return;
        }

        syncDatabase(database, databaseBounties, databaseStats, playerDataMap);
    }

    private static <T extends Comparable<T>> void insertIntoSortedList(List<T> list, T element) {
        int index = Collections.binarySearch(list, element);
        if (index < 0) {
            index = -index - 1;
        }
        list.add(index, element);
    }

    private static synchronized void syncDatabase(NotBountiesDatabase database, List<Bounty> databaseBounties, Map<UUID, PlayerStat> databaseStats, List<PlayerData> databasePlayerData) {
        NotBounties.debugMessage("Synchronizing database \"" + database.getName() + "\" with " + databaseBounties.size() + " bounties and "  + databaseStats.size() + " stat records.", false);
        AsyncDatabaseWrapper databaseWrapper = null;
        for (AsyncDatabaseWrapper asyncDatabaseWrapper : databases) {
            if (asyncDatabaseWrapper.getName().equals(database.getName())) {
                databaseWrapper = asyncDatabaseWrapper;
                break;
            }
        }
        if (databaseWrapper == null) {
            // unknown database
            plugin.getLogger().warning("Unknown database \"" + database.getName() + "\" tried to connect!");
            return;
        }

        List<Bounty> localBounties = getAllBounties(2);
        Map<UUID, PlayerStat> localStats = getAllStats();
        List<PlayerData> localPlayerData = getAllPlayerData();

        long lastSyncTime = database.getLastSync();// get last sync time
        database.setLastSync(System.currentTimeMillis());

        // <TODO> delete or archive this essay </TODO>
        // goals:
        // When connecting to a database for the first time, local data should be added to the database

        // sync with empty temp, then sync with perm and get a bunch of older stuff, sync with temp again
        // solutions:
        // on first connect, add to every other connected database instantly - maybe not1
        // stats? -
        // when connecting to a perm database for the first time, all data changed on local should be changed on any connected temp databases if the temp database doesn't have global stuff already - what if changes already occured from another server? - only change if the temp database hasn't connected to a global
        // add last reset time to temp database, no, cant tell if data was removed
        // on temp connect, if a perm database is connected and the temp database doesnt have any global data, set temp database to all local data + temp changes since last sync
        // only write to lower priority databases, syncing with the highest priority-

        // theory:
        // when a new database connects, it may add old data that other databases do not have. This old data is less than the sync time of the other databases, so it will dissapear on sync.
        // data added to local on a sync should also be added to other connected databases iff it isn't there already
        // idealy, the databases should all be consistent, but temp databases may not, assume the others are.
        // localdata clears on restart if perm is connected
        // sync times are only saved for temp databases

        // problems:
        // data may be desynchronized on different databases, Inconsistent will work for perm databases, but temp databases may lose data after sync
        // when a database syncs, adding a new item, and then the server syncs with another database that also has this new item, the item was updated after both last syncs - inconsistent will fix this
        // syncing a database on first connect, local data is empty, db has data, but older than lastSync - last sync is not saved for perm databases
        // why is last sync saved for temp databases? Because we store the temp database data locally when we restart
        // database connects with old data that needs to be added to other databases that are already connected, the databases may or may not already have the data - already have? inconsistent will merge. doesn't have? can't tell if it was deleted or just old data that needs to be added. With more than 1 perm database, one cannot go down before a restart

        // chosen:
        // when a perm database is connected for the first time, (lasySync=0 or firstConnect), scan for any connected temp databases and add the bounties added to local
        // on temp connect, if a perm database is connected and the temp database doesnt have any global data, set temp database to all local data + temp changes since last sync
        // get consistent list of player data, then add any missing values back to the list, then update all on temp database
        // get local bounties added and database removed to the database remove local removed from the database, add database added to local bounties

        // add all server-id data if haven't connected to this temp database before
        // data with global server id is in a perm database already
        // if the temp database is empty, add all data because it hasn't received anything from the perm database
        if (database instanceof TempDatabase tempDatabase) {
            if (tempDatabase.hasServerData()) {
                // regular data
                syncConsistentData(database, databaseBounties, databaseStats, databasePlayerData, localBounties, lastSyncTime, databaseWrapper, localPlayerData, localStats);
            } else {
                if (tempDatabase.isEmpty()) {
                    // add all data
                    databaseWrapper.addBounty(localBounties);
                    databaseWrapper.addStats(localStats);
                    databaseWrapper.addPlayerData(localPlayerData);
                } else {
                    // add any data from the database with a non-global-id to local data
                    for (Bounty bounty : databaseBounties) {
                        if (!bounty.getServerID().equals(DataManager.GLOBAL_SERVER_ID)) {
                            localData.addBounty(bounty);
                            insertIntoSortedList(localBounties, bounty);
                        }
                    }
                    for (PlayerData playerData : databasePlayerData) {
                        if (!playerData.getServerID().equals(DataManager.GLOBAL_SERVER_ID)) {
                            localData.updatePlayerData(playerData);
                            insertIntoSortedList(localPlayerData, playerData);
                        }
                    }

                    // add data with server-id to the database - any global-id data will be added in the next step
                    for (Bounty bounty : localBounties) {
                        if (DataManager.databaseServerID.equals(bounty.getServerID())) {
                            databaseWrapper.addBounty(bounty);
                            insertIntoSortedList(databaseBounties, bounty);
                        }
                    }
                    for (PlayerData playerData : localPlayerData) {
                        if (DataManager.databaseServerID.equals(playerData.getServerID())) {
                            databaseWrapper.updatePlayerData(playerData);
                            insertIntoSortedList(databasePlayerData, playerData);
                        }
                    }

                    // sync inconsistent data (for global-id elements)
                    syncConsistentData(database, databaseBounties, databaseStats, databasePlayerData, localBounties, lastSyncTime, databaseWrapper, localPlayerData, localStats);
                }
                tempDatabase.addServerData();
            }
        } else {
            syncConsistentData(database, databaseBounties, databaseStats, databasePlayerData, localBounties, lastSyncTime, databaseWrapper, localPlayerData, localStats);

        }

    }

    // prob rename
    private static void syncConsistentData(NotBountiesDatabase database, List<Bounty> databaseBounties, Map<UUID, PlayerStat> databaseStats, List<PlayerData> databasePlayerData, List<Bounty> localBounties, long lastSyncTime, AsyncDatabaseWrapper databaseWrapper, List<PlayerData> localPlayerData, Map<UUID, PlayerStat> localStats) {
        // regular sync
        List<Bounty>[] dataChanges = Inconsistent.getAsynchronousObjects(localBounties, databaseBounties, lastSyncTime, database.getName());
        // these bounties should be added/removed to local data
        List<Bounty> databaseAdded = dataChanges[0];
        List<Bounty> databaseRemoved = dataChanges[1];
        // these bounties should be added/removed to the local data
        List<Bounty> localAdded = dataChanges[2];
        List<Bounty> localRemoved = dataChanges[3];
        // apply consistency changes
        localData.addBounty(databaseAdded);
        localData.removeBounty(databaseRemoved);
        databaseWrapper.addBounty(localAdded);
        databaseWrapper.removeBounty(localRemoved);

        NotBounties.debugMessage("Since last sync: databaseAdded=" + databaseAdded.size() + " databaseRemoved=" + databaseRemoved.size() + " localAdded=" + localAdded.size() + " localRemoved=" + localRemoved.size(), false);

        // log any unknown names in the database bounties
        for (Bounty bounty : databaseAdded) {
            if (LoggedPlayers.isMissing(bounty.getUUID()) && !bounty.getUUID().equals(DataManager.GLOBAL_SERVER_ID)) {
                LoggedPlayers.logPlayer(bounty.getName(), bounty.getUUID());
            }
        }

        // push stat changes
        Map<UUID, PlayerStat> pushedChanges = databaseWrapper.getStatChanges(); // these are the stats that have been added to the local data since the last sync or server start
        Map<UUID, PlayerStat> localStatsCopy = new HashMap<>(localStats);
        // remove any stats without the server-id
        localStats.entrySet().removeIf(entry -> !entry.getValue().serverID().equals(DataManager.databaseServerID));
        // add any pushed changes to the local stats
        for (Map.Entry<UUID, PlayerStat> entry : pushedChanges.entrySet()) {
            if (!localStats.containsKey(entry.getKey())) {
                localStats.put(entry.getKey(), entry.getValue());
            }
        }
        // localStats now has all server-bound data (only this server has it)

        // add changes to database
        databaseWrapper.addStats(localStats);
        Map<UUID, PlayerStat> databaseStatCopy = new HashMap<>(databaseStats);
        // add changes to the local data
        for (Map.Entry<UUID, PlayerStat> entry : localStats.entrySet()) {
            if (databaseStats.containsKey(entry.getKey()))
                databaseStats.replace(entry.getKey(), databaseStats.get(entry.getKey()).combineStats(entry.getValue()));
            else
                databaseStats.put(entry.getKey(), entry.getValue());
        }
        localData.setStats(databaseStats);

        // sync player data
        syncTimes(localPlayerData, databasePlayerData);

        List<PlayerData> syncedPlayerData = Inconsistent.compareInconsistentLists(localPlayerData, new ArrayList<>(databasePlayerData), lastSyncTime, database.getName(), false);

        // apply consistency changes
        localData.addPlayerData(syncedPlayerData);
        databaseWrapper.addPlayerData(syncedPlayerData);

        NotBounties.debugMessage("Player Data synced:" + syncedPlayerData.size() , false);

        // check for any online refunds
        if (NotBounties.getInstance().isEnabled()) {
            NotBounties.getServerImplementation().global().run(() -> {
                for (PlayerData playerData : syncedPlayerData) {
                    if (playerData.hasRefund() && Bukkit.getPlayer(playerData.getUuid()) != null) {
                        handleRefund(playerData);
                    }
                }
            });
        }

        if (database.isPermDatabase()) {
            // remove local server ID from local data
            localData.syncPermData();
            if (lastSyncTime == 0) {
                // first time connecting since restart
                // check if there are any temp databases to load global data to
                for (AsyncDatabaseWrapper asyncDatabaseWrapper : databases) {
                    if (asyncDatabaseWrapper.isConnected() && !asyncDatabaseWrapper.isPermDatabase() && !((TempDatabase) asyncDatabaseWrapper.getDatabase()).isGlobal()) {
                        asyncDatabaseWrapper.addBounty(databaseAdded);
                        asyncDatabaseWrapper.addPlayerData(syncedPlayerData);
                        asyncDatabaseWrapper.addStats(databaseStatCopy);
                        ((TempDatabase) asyncDatabaseWrapper.getDatabase()).setGlobal();
                    }
                }
            }
        } else if (isPermDatabaseConnected()) {
            // temp database just connected
            if (!((TempDatabase) database).isGlobal()) {
                // add back removed data because it was probably just received from the last perm data connection
                databaseWrapper.addBounty(databaseRemoved);
                // sync missed data
                List<PlayerData> missedPlayerData = new LinkedList<>();
                for (PlayerData playerData : localPlayerData) {
                    boolean found = false;
                    for (PlayerData dbPlayerData : databasePlayerData) {
                        if (dbPlayerData.getUuid().equals(playerData.getUuid())) {
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        continue;
                    for (PlayerData sPlayerData : syncedPlayerData) {
                        if (sPlayerData.getUuid().equals(playerData.getUuid())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        missedPlayerData.add(playerData);
                }
                databaseWrapper.addPlayerData(missedPlayerData);
                databaseWrapper.addStats(localStatsCopy);

                ((TempDatabase) database).setGlobal();
            }
        }
    }

    /**
     * Synchronize matching objects in 2 lists that hold time in PlayerData excluding the lastSeen variable.
     * @param playerDataList1 A sorted player data list.
     * @param playerDataList2 A sorted player data list.
     */
    private static void syncTimes(List<PlayerData> playerDataList1, List<PlayerData> playerDataList2) {
        // lists are sorted
        for (int i = 0; i < playerDataList1.size(); i++) {
            PlayerData playerData1 = playerDataList1.get(i);
            PlayerData playerData2 = null;
            // find matching player data in the other list
            for (int j = i; j < playerDataList2.size(); j++) {
                if (playerData1.getID().equals(playerDataList2.get(j).getID())) {
                    playerData2 = playerDataList2.get(i);
                    break;
                }
            }
            if (playerData2 != null) {
                // sync times - not lastSeen because that is used to sync other options later
                long lastClaim = Math.max(playerData1.getLastClaim(), playerData2.getLastClaim());
                playerData1.setLastClaim(lastClaim);
                playerData2.setLastClaim(lastClaim);
                long bountyCooldown = Math.max(playerData1.getBountyCooldown(), playerData2.getBountyCooldown());
                playerData1.setBountyCooldown(bountyCooldown);
                playerData2.setBountyCooldown(bountyCooldown);
            }
        }
    }


    private static void tryDatabaseConnections() {
        for (AsyncDatabaseWrapper database : databases) {
            if (!database.isConnected())
                database.connect(true);
        }
    }
}

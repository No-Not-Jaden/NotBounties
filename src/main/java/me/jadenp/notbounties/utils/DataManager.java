package me.jadenp.notbounties.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import me.jadenp.notbounties.*;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.features.settings.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.features.settings.databases.LocalData;
import me.jadenp.notbounties.features.settings.databases.NotBountiesDatabase;
import me.jadenp.notbounties.features.settings.databases.TempDatabase;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyDatabase;
import me.jadenp.notbounties.features.settings.databases.redis.RedisConnection;
import me.jadenp.notbounties.features.settings.databases.sql.MySQL;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyBoardTypeAdapter;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.settings.auto_bounties.BigBounty;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.data.RewardHead;
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
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


import static me.jadenp.notbounties.features.LanguageOptions.*;

public class DataManager {

    private DataManager(){}

    private static final List<AsyncDatabaseWrapper> databases = new ArrayList<>();

    private static LocalData localData; // locally stored bounties and stats
    private static UUID databaseServerID = null;
    public static final long CONNECTION_REMEMBRANCE_MS = (long) 2.592e+8; // how long before databases stop storing changes if no connection was made (3 days)
    public static final UUID GLOBAL_SERVER_ID = new UUID(0,0);

    private static final Map<UUID, PlayerData> playerDataMap = Collections.synchronizedMap(new HashMap<>());
    private static Plugin plugin;

    public static void loadData() throws IOException {
        localData = new LocalData();
        loadOldData();
        readPlayerData();
        readBounties();
        readStats();
        // load player data for immunity
        // currently this is just for time immunity
        ImmunityManager.loadPlayerData();

    }

    private static void readStats() throws IOException {
        File statsFile = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "player_stats.json");
        if (!statsFile.exists())
            return;
        try (JsonReader reader = new JsonReader(new FileReader(statsFile))) {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return;
            }
            reader.beginArray();
            PlayerStatAdapter adapter = new PlayerStatAdapter();
            while (reader.hasNext()) {
                reader.beginObject();
                UUID uuid = null;
                PlayerStat playerStat = null;
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("uuid"))
                        uuid = UUID.fromString(reader.nextString());
                    else if (name.equals("stats"))
                        playerStat = adapter.read(reader);
                }
                reader.endObject();
                localData.addStats(uuid, playerStat);
            }
            reader.endArray();
        }
    }

    private static void readBounties() throws IOException {
        File bountiesFile = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "bounties.json");
        if (!bountiesFile.exists())
            return;
        try (JsonReader reader = new JsonReader(new FileReader(bountiesFile))) {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return;
            }
            reader.beginArray();
            BountyTypeAdapter adapter = new BountyTypeAdapter();
            while (reader.hasNext()) {
                localData.addBounty(adapter.read(reader));
            }
            reader.endArray();
        }
    }

    private static void readPlayerData() throws IOException {
        ChallengeManager.setNextChallengeChange(1); // prepare new challenges if the last challenge change wasn't read
        File playerDataFile = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "player_data.json");
        if (!playerDataFile.exists())
            return;
        try (JsonReader reader = new JsonReader(new FileReader(playerDataFile))) {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return;
            }
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "players" -> playerDataMap.putAll(readPlayers(reader));
                    case "trackedBounties" -> BountyTracker.setTrackedBounties(readTrackedBounties(reader));
                    case "databaseSyncTimes" -> {
                        Map<String, Long> syncTimes = readDatabaseSyncTimes(reader);
                        for (AsyncDatabaseWrapper databaseWrapper : databases) {
                            if (syncTimes.containsKey(databaseWrapper.getName()))
                                databaseWrapper.setLastSync(syncTimes.get(databaseWrapper.getName()));
                        }
                    }
                    case "nextRandomBounty" -> {
                        RandomBounties.setNextRandomBounty(reader.nextLong());
                        if (!RandomBounties.isEnabled()) {
                            RandomBounties.setNextRandomBounty(0);
                        } else if (RandomBounties.getNextRandomBounty() == 0) {
                            RandomBounties.setNextRandomBounty();
                        }
                    }
                    case "nextTimedBounties" -> TimedBounties.setNextBounties(readTimedBounties(reader));
                    case "bountyBoards" -> BountyBoard.addBountyBoards(readBountyBoards(reader));
                    case "nextChallengeChange" -> ChallengeManager.setNextChallengeChange(reader.nextLong());
                    case "serverID" -> databaseServerID = UUID.fromString(reader.nextString());
                    case "paused" -> NotBounties.setPaused(reader.nextBoolean());
                    case "wantedTagLocations" -> readWantedTagLocations(reader);
                    default -> {
                        // unexpected name
                    }
                }
            }
            reader.endObject();
        }

        // tell LoggedPlayers that it can read all the player names and store them in an easy to read hashmap
        LoggedPlayers.loadPlayerData();
    }

    private static void readWantedTagLocations(JsonReader reader) throws IOException {
        List<Location> locations = getWantedTagLocations(reader);
        if (!locations.isEmpty()) {
            NotBounties.getServerImplementation().global().runDelayed(task -> {
                RemovePersistentEntitiesEvent.cleanChunks(locations);
            }, 100);
        }
    }

    private static List<Location> getWantedTagLocations(JsonReader reader) throws IOException {
        List<String> stringList = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            stringList.add(reader.nextString());
        }
        reader.endArray();

        return stringListToLocationList(stringList);
    }

    private static List<BountyBoard> readBountyBoards(JsonReader reader) throws IOException {
        List<BountyBoard> bountyBoards = new LinkedList<>();
        reader.beginArray();
        BountyBoardTypeAdapter bountyBoardTypeAdapter = new BountyBoardTypeAdapter();
        while (reader.hasNext()) {
            BountyBoard bountyBoard = bountyBoardTypeAdapter.read(reader);
            if (bountyBoard != null)
                bountyBoards.add(bountyBoard);
            else
                plugin.getLogger().info("Could not load a saved bounty board. (Location does not exist)");
        }
        reader.endArray();

        return bountyBoards;
    }

    private static Map<String, Long> readDatabaseSyncTimes(JsonReader reader) throws IOException {
        Map<String, Long> syncMap = new HashMap<>();
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            String dbName = null;
            long time = 0;
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name" -> dbName = reader.nextString();
                    case "time" -> time = reader.nextLong();
                    default -> {
                        // unexpected name
                    }
                }
            }
            reader.endObject();
            syncMap.put(dbName, time);
        }
        reader.endArray();

        return syncMap;
    }

    private static BiMap<Integer, UUID> readTrackedBounties(JsonReader reader) throws IOException {
        BiMap<Integer, UUID> map = HashBiMap.create();
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            int num = -404;
            UUID uuid = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("id"))
                    num = reader.nextInt();
                else if (name.equals("uuid"))
                    uuid = UUID.fromString(reader.nextString());
            }
            reader.endObject();
            map.put(num, uuid);
        }
        reader.endArray();

        return map;
    }

    private static Map<UUID, Long> readTimedBounties(JsonReader reader) throws IOException {
        Map<UUID, Long> timedBounties = new HashMap<>();
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            UUID uuid = null;
            long time = 0;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("time"))
                    time = reader.nextLong();
                else if (name.equals("uuid"))
                    uuid = UUID.fromString(reader.nextString());
            }
            reader.endObject();
            timedBounties.put(uuid, time);
        }
        reader.endArray();

        return timedBounties;
    }

    private static Map<UUID, PlayerData> readPlayers(JsonReader reader) throws IOException {
        Map<UUID, PlayerData> data = new HashMap<>();
        reader.beginArray();
        PlayerDataAdapter adapter = new PlayerDataAdapter();
        while (reader.hasNext()) {
            reader.beginObject();
            UUID uuid = null;
            PlayerData playerData = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("uuid"))
                    uuid = UUID.fromString(reader.nextString());
                else if (name.equals("data"))
                    playerData = adapter.read(reader);
            }
            reader.endObject();
            data.put(uuid, playerData);
        }
        reader.endArray();

        return data;
    }

    public static Map<UUID, PlayerData> getPlayerDataMap() {
        Map<UUID, PlayerData> data;
        synchronized (playerDataMap) {
            data = new HashMap<>(playerDataMap);
        }
        return data;
    }

    public static LocalData getLocalData() {
        return localData;
    }

    public static void connectProxy(List<Bounty> bounties, Map<UUID, PlayerStat> playerStatMap) {
        // turn local data into proxy database
        NotBounties.getServerImplementation().async().runNow(task -> {
            for (AsyncDatabaseWrapper database : databases) {
                if (database.getDatabase() instanceof ProxyDatabase) {
                    syncDatabase(database.getDatabase(), bounties, playerStatMap);
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
        DataManager.plugin = plugin;
        for (String databaseName : configuration.getKeys(false)) {
            boolean newDatabase = true;
            for (NotBountiesDatabase database : databases) {
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
                default -> {
                    throw new IllegalArgumentException("Unknown database type for " + databaseName + ": " + type);
                }
            }
            databases.add(new AsyncDatabaseWrapper(database));
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
                PlayerData playerData = getPlayerData(entry.getKey());
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

                        List<UUID> convertedUUIDs = new ArrayList<>();
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
                        PlayerData playerData = DataManager.getPlayerData(uuid);
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
                                getPlayerData(uuid).setWhitelist(new Whitelist(configuration.getStringList("data." + uuid + ".whitelist").stream().map(UUID::fromString).collect(Collectors.toList()), configuration.getBoolean("data." + uuid + ".blacklist")));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Failed to get whitelisted uuids from: " + uuid + "\nThis list will be overwritten in 5 minutes");
                            }
                        if (configuration.isSet("data." + uuid + ".refund"))
                            playerData.addRefund(configuration.getDouble("data." + uuid + ".refund"));
                        if (configuration.isSet("data." + uuid + ".refund-items"))
                            try {
                                playerData.addRefund(Arrays.asList(SerializeInventory.itemStackArrayFromBase64(configuration.getString("data." + uuid + ".refund-items"))));
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
                    configuration.getStringList("disable-broadcast").forEach(s -> getPlayerData(UUID.fromString(s)).setDisableBroadcast(true));

                i = 0;
                while (configuration.getString("head-rewards." + i + ".setter") != null) {
                    try {
                        List<RewardHead> rewardHeads = new ArrayList<>();
                        for (String str : configuration.getStringList("head-rewards." + i + ".uuid")) {
                            rewardHeads.add(RewardHead.decodeRewardHead(str));
                        }
                        DataManager.getPlayerData(UUID.fromString(Objects.requireNonNull(configuration.getString("head-rewards." + i + ".setter")))).addRewardHeads(rewardHeads);
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

    public static PlayerData getPlayerData(UUID uuid) {
        if (uuid.equals(GLOBAL_SERVER_ID))
            return new PlayerData();
        return playerDataMap.computeIfAbsent(uuid, k -> new PlayerData());
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
        localData.addStats(uuid, statChange);
        for (AsyncDatabaseWrapper database : databases) {
            database.addStats(uuid, statChange);
        }
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
        // load data from priority database
        // store bounties & stats with a server id
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
        readPriorityDatabase();

        return getAllStats().entrySet().stream().filter(entry -> entry.getValue().serverID().equals(databaseServerID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Get a list of bounties that are only stored locally. This will load sql data if possible.
     * @return Bounties to be stored locally.
     */
    public static Set<Bounty> getLocalBounties() {
        readPriorityDatabase();
        return getAllBounties(-1).stream().filter(bounty -> bounty.getServerID().equals(databaseServerID)).collect(Collectors.toUnmodifiableSet());
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
        for (AsyncDatabaseWrapper database : databases)
            database.shutdown();
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
                    localData.replaceBounty(uuid, bounty);
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
                bounty.addBounty(change, new ArrayList<>(), new Whitelist(new ArrayList<>(), false));
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
    public static void notifyBounty(Player player) {
        Bounty bounty = getBounty(player.getUniqueId());
        if (bounty != null) {
            double addedAmount = 0;
            for (Setter setter : bounty.getSetters()) {
                if (!setter.isNotified()) {
                    player.sendMessage(parse(getPrefix() + getMessage("offline-bounty"), setter.getDisplayAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    setter.setNotified(true);
                    addedAmount += setter.getAmount();
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

    private static boolean isPermDatabaseConnected() {
        for (AsyncDatabaseWrapper database : databases) {
            if (database.isPermDatabase() && database.isConnected())
                return true;
        }
        return false;
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
        NotBounties.getServerImplementation().async().runNow(task -> {
            List<Bounty> databaseBounties;
            Map<UUID, PlayerStat> databaseStats;

            try {
                databaseBounties = database.getAllBounties(2);
                databaseStats = database.getAllStats();
            } catch (IOException e) {
                plugin.getLogger().warning("Incomplete connection to " + database.getName());
                return;
            }

            syncDatabase(database, databaseBounties, databaseStats);
        });
    }

    private static void syncDatabase(NotBountiesDatabase database, List<Bounty> databaseBounties, Map<UUID, PlayerStat> databaseStats) {
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

        if (database instanceof TempDatabase tempDatabase && isPermDatabaseConnected()) {
            tempDatabase.replaceWithDefaultServerID();
        } else if (!(database instanceof TempDatabase)) {
            // remove local server ID from local data
            localData.syncPermData();
            // remove server ID of connected temp databases
            for (AsyncDatabaseWrapper otherDatabase : databases) {
                if (otherDatabase.isConnected() && otherDatabase.getDatabase() instanceof TempDatabase tempDatabase) {
                    tempDatabase.replaceWithDefaultServerID();
                }
            }
        }


        long lastSyncTime = database.getLastSync();// get last sync time
        database.setLastSync(System.currentTimeMillis());

        List<Bounty>[] dataChanges = Inconsistent.getAsyncronousObjects(localBounties, databaseBounties, lastSyncTime);
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

        if (lastSyncTime == 0) {
            // never sunk
            // doesn't have local data
            // combine both local and database stats
            databaseWrapper.addStats(localStats);
            localData.addStats(databaseStats);
        } else {
            // database has connected before

            if (databaseStats.isEmpty()) {
                // nothing in the database yet
                // if the database restarted, this will add old stats in
                // add all local stats
                databaseWrapper.addStats(localStats);
            } else {

                // check if this server's stats are in the db already
                // if the database is a TempDatabase, their bounties will contain the local server id if the data is in the db already
                // if the database isn't a TempDatabase, all local data with a local server id needs to be migrated
                if (database instanceof TempDatabase tempDatabase && tempDatabase.getStoredServerIds().contains(DataManager.getDatabaseServerID(false))) {
                    // db is a temp database that has this server's data
                    // push stat changes since last connect
                    Map<UUID, PlayerStat> pushedChanges = databaseWrapper.getStatChanges();
                    databaseWrapper.addStats(pushedChanges);
                } else {
                    // db is a temp database that hasn't received this server's data before or a perm database
                    // migrate local stats with the local server-id
                    // push stat changes that weren't in the migrated local stats
                    Map<UUID, PlayerStat> pushedChanges = databaseWrapper.getStatChanges();
                    // remove any stats without the server-id
                    localStats.entrySet().removeIf(entry -> !entry.getValue().serverID().equals(DataManager.databaseServerID));
                    // add any pushed changes to the local stats
                    for (Map.Entry<UUID, PlayerStat> entry : pushedChanges.entrySet()) {
                        if (!localStats.containsKey(entry.getKey())) {
                            localStats.put(entry.getKey(), entry.getValue());
                        }
                    }
                    // add changes to database
                    databaseWrapper.addStats(localStats);
                    // add changes to the local data
                    for (Map.Entry<UUID, PlayerStat> entry : localStats.entrySet()) {
                        if (databaseStats.containsKey(entry.getKey()))
                            databaseStats.replace(entry.getKey(), databaseStats.get(entry.getKey()).combineStats(entry.getValue()));
                        else
                            databaseStats.put(entry.getKey(), entry.getValue());
                    }
                    localData.setStats(databaseStats);
                }
            }
        }
        // log any unknown names in the database bounties
        for (Bounty bounty : databaseAdded) {
            if (!LoggedPlayers.isLogged(bounty.getUUID()) && !bounty.getUUID().equals(DataManager.GLOBAL_SERVER_ID)) {
                LoggedPlayers.logPlayer(bounty.getName(), bounty.getUUID());
            }
        }
    }



    private static void tryDatabaseConnections() {
        for (NotBountiesDatabase database : databases) {
            if (!database.isConnected())
                database.connect();
        }
    }
}

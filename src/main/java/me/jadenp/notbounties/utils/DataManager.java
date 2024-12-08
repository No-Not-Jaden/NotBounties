package me.jadenp.notbounties.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.*;
import me.jadenp.notbounties.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.databases.LocalData;
import me.jadenp.notbounties.databases.NotBountiesDatabase;
import me.jadenp.notbounties.databases.TempDatabase;
import me.jadenp.notbounties.databases.redis.RedisConnection;
import me.jadenp.notbounties.databases.sql.MySQL;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.BigBounty;
import me.jadenp.notbounties.utils.configuration.Immunity;
import me.jadenp.notbounties.utils.configuration.RewardHead;
import me.jadenp.notbounties.utils.configuration.autoBounties.RandomBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.NotBounties.bountyBoards;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class DataManager {

    private static final List<AsyncDatabaseWrapper> databases = new ArrayList<>();

    private static LocalData localData; // locally stored bounties and stats
    private static UUID databaseServerID = UUID.randomUUID();
    public static final long CONNECTION_REMEMBRANCE_MS = (long) 2.592e+8; // how long before databases stop storing changes if no connection was made (3 days)
    public static final UUID GLOBAL_SERVER_ID = new UUID(0,0);
    
    private DataManager(){}

    public static LocalData getLocalData() {
        return localData;
    }

    public static void loadDatabaseConfig(ConfigurationSection configuration) {
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
                String type = configuration.isSet(databaseName + ".type") ? configuration.getString(databaseName + ".type") : "You need to set your database type for: " + databaseName;
                assert type != null; // isSet() assures that type != null
                NotBountiesDatabase database;
                try {
                    switch (type.toUpperCase()) {
                        case "SQL" -> database = new MySQL(NotBounties.getInstance(), databaseName);
                        case "REDIS" -> database = new RedisConnection(NotBounties.getInstance(), databaseName);
                        default -> {
                            Bukkit.getLogger().warning(() -> "[NotBounties] Unknown database type for " + databaseName + ": " + type);
                            continue;
                        }
                    }
                    databases.add(new AsyncDatabaseWrapper(database));
                } catch (NoClassDefFoundError e) {
                    // Couldn't load a dependency.
                    // This will be thrown if unable to use Spigot's library loader
                    NotBounties.debugMessage("One or more dependencies could not be downloaded to use the database: " + databaseName + " (" + type + ")", true);
                }
            }
        }
        Collections.sort(databases);
        tryDatabaseConnections();
    }

    /**
     * Loads bounties from the
     */
    public static void loadBounties(){
        localData = new LocalData(); // initialize local data storage
        File bounties = new File(NotBounties.getInstance().getDataFolder() + File.separator + "bounties.yml");
        List<Bounty> bountyList = new ArrayList<>();
        Map<UUID, PlayerStat> stats = new HashMap<>();
        // create bounties file if one doesn't exist
        try {
            if (bounties.createNewFile()) {
                Bukkit.getLogger().info("[NotBounties] Created new storage file.");
            } else {
                // get existing bounties file
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(bounties);
                if (configuration.isSet("server-id"))
                    databaseServerID = UUID.fromString(Objects.requireNonNull(configuration.getString("server-id")));
                else
                    databaseServerID = UUID.randomUUID();
                // add all previously logged on players to a map
                int i = 0;
                while (configuration.getString("logged-players." + i + ".name") != null) {
                    loggedPlayers.put(Objects.requireNonNull(configuration.getString("logged-players." + i + ".name")).toLowerCase(Locale.ROOT), UUID.fromString(Objects.requireNonNull(configuration.getString("logged-players." + i + ".uuid"))));
                    i++;
                }
                immunePerms = configuration.isSet("immune-permissions") ? configuration.getStringList("immune-permissions") : new ArrayList<>();
                autoImmuneMurderPerms = configuration.isSet("immunity-murder") ? configuration.getStringList("immunity-murder") : new ArrayList<>();
                autoImmuneRandomPerms = configuration.isSet("immunity-random") ? configuration.getStringList("immunity-random") : new ArrayList<>();
                autoImmuneTimedPerms = configuration.isSet("immunity-timed") ? configuration.getStringList("immunity-timed") : new ArrayList<>();
                // go through bounties in file
                i = 0;
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
                        if (variableWhitelist && configuration.isSet("data." + uuid + ".whitelist"))
                            try {
                                playerWhitelist.put(uuid, new Whitelist(configuration.getStringList("data." + uuid + ".whitelist").stream().map(UUID::fromString).collect(Collectors.toList()), configuration.getBoolean("data." + uuid + ".blacklist")));
                            } catch (IllegalArgumentException e) {
                                Bukkit.getLogger().warning("Failed to get whitelisted uuids from: " + uuid + "\nThis list will be overwritten in 5 minutes");
                            }
                        if (configuration.isSet("data." + uuid + ".refund"))
                            BountyManager.refundedBounties.put(uuid, configuration.getDouble("data." + uuid + ".refund"));
                        if (configuration.isSet("data." + uuid + ".refund-items"))
                            try {
                                BountyManager.refundedItems.put(uuid, new ArrayList<>(Arrays.asList(SerializeInventory.itemStackArrayFromBase64(configuration.getString("data." + uuid + ".refund-items")))));
                            } catch (IOException e) {
                                Bukkit.getLogger().warning("[NotBounties] Unable to load item refund for player using this encoded data: " + configuration.getString("data." + uuid + ".refund-items"));
                                Bukkit.getLogger().warning(e.toString());
                            }
                        if (configuration.isSet("data." + uuid + ".time-zone"))
                            LocalTime.addTimeZone(uuid, configuration.getString("data." + uuid + ".time-zone"));
                        if (bountyCooldown > 0 && configuration.isSet("data." + uuid + ".last-set"))
                            BountyManager.bountyCooldowns.put(uuid, configuration.getLong("data." + uuid + ".last-set"));
                    }
                if (!timedBounties.isEmpty())
                    TimedBounties.setNextBounties(timedBounties);
                if (configuration.isSet("disable-broadcast"))
                    configuration.getStringList("disable-broadcast").forEach(s -> NotBounties.disableBroadcast.add(UUID.fromString(s)));

                i = 0;
                while (configuration.getString("head-rewards." + i + ".setter") != null) {
                    try {
                        List<RewardHead> rewardHeads = new ArrayList<>();
                        for (String str : configuration.getStringList("head-rewards." + i + ".uuid")) {
                            rewardHeads.add(RewardHead.decodeRewardHead(str));
                        }
                        BountyManager.headRewards.put(UUID.fromString(Objects.requireNonNull(configuration.getString("head-rewards." + i + ".setter"))), rewardHeads);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        Bukkit.getLogger().warning("Invalid UUID for head reward #" + i);
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
                        Bukkit.getLogger().warning("[NotBounties] Could not convert tracked string to uuid: " + configuration.getString("tracked-bounties." + i + ".uuid"));
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
                        bountyBoards.add(new BountyBoard(location, BlockFace.valueOf(configuration.getString("bounty-boards." + str + ".direction")), configuration.getInt("bounty-boards." + str + ".rank")));
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
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("[NotBounties] Error loading saved data!");
            Bukkit.getLogger().severe(e.toString());
        }
        localData.addBounty(bountyList);
        localData.addStats(stats);
        // load player data for immunity
        // currently this is just for time immunity
        Immunity.loadPlayerData();
    }

    /**
     * Get the server ID that was set in the config for the SQL Database.
     * @param newData If the serverID is for new data generation.
     * @return The set server ID for NotBounties.
     */
    public static UUID getDatabaseServerID(boolean newData) {
        if (newData && isPermDatabaseConnected())
            return GLOBAL_SERVER_ID;
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
                            lastSetter = setter;
                        } else if (setter.getAmount() > 0) {
                            // update amount (minimum 0)
                            double newAmount = Math.min(0, setter.getAmount() + change);
                            change+= setter.getAmount() - newAmount; // update amount needed to be changed
                            Setter newSetter = new Setter(setter.getName(), setterUUID, newAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() - (setter.getAmount() - newAmount));
                            setterListIterator.set(newSetter);
                            lastSetter = setter;
                        }
                    } else {
                        // setter amount won't go negative
                        // update setter amount
                        Setter newSetter = new Setter(setter.getName(), setterUUID, setter.getAmount() + change, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + change);
                        change = 0;
                        setterListIterator.set(newSetter);
                        lastSetter = setter;
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
            }
            BigBounty.setBounty(player, bounty, addedAmount);
            if (bounty.getTotalDisplayBounty() > BigBounty.getThreshold()) {
                displayParticle.add(player.getUniqueId());
            }
        }
    }

    public static void login(@NotNull Player player) {
        for (AsyncDatabaseWrapper database : databases)
            database.login(player.getUniqueId(), player.getName());
    }

    public static void logout(@NotNull Player player) {
        for (AsyncDatabaseWrapper database : databases)
            database.logout(player.getUniqueId());
    }

    /**
     * A utility to modify both parameters to remove any setters that are similar.
     * For any setter with the same uuid and whitelist, amounts will be canceled out, and similar items will be removed.
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
                        masterSetters.set(new Setter(masterSetter.getName(), masterSetter.getUuid(), newMasterAmount, masterSetter.getItems(), masterSetter.getTimeCreated(), masterSetter.isNotified(), masterSetter.getWhitelist(), masterSetter.getReceiverPlaytime(), 0));
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
     * Removes setters from all databases.
     * @param bounty Bounty that contains the setters.
     * @param setters Setters to be removed.
     */
    public static void removeSetters(@NotNull Bounty bounty, List<Setter> setters) {
        bounty.getSetters().removeIf(setters::contains);
        if (bounty.getSetters().isEmpty()) {
            deleteBounty(bounty.getUUID());
        } else {
            Bounty removedBounty = new Bounty(bounty.getUUID(), setters, bounty.getName(), bounty.getServerID());
            for (AsyncDatabaseWrapper database : databases)
                database.removeBounty(removedBounty);
            localData.removeBounty(removedBounty);
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
     * @param database
     */
    public static void databaseConnect(NotBountiesDatabase database) {
        // hasConnectedBefore will be false if this is the first time connecting
        new BukkitRunnable() {
            @Override
            public void run() {
                AsyncDatabaseWrapper databaseWrapper = null;
                for (AsyncDatabaseWrapper asyncDatabaseWrapper : databases) {
                    if (asyncDatabaseWrapper.getName().equals(database.getName())) {
                        databaseWrapper = asyncDatabaseWrapper;
                        break;
                    }
                }
                if (databaseWrapper == null) {
                    // unknown database
                    Bukkit.getLogger().warning("[NotBounties] Unknown database \"" + database.getName() + "\" tried to connect!");
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

                List<Bounty> databaseBounties;
                Map<UUID, PlayerStat> databaseStats;

                try {
                    databaseBounties = database.getAllBounties(2);
                    databaseStats = database.getAllStats();
                } catch (IOException e) {
                    Bukkit.getLogger().warning("[NotBounties] Incomplete connection to " + database.getName());
                    return;
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
                database.addBounty(localAdded);
                database.removeBounty(localRemoved);

                NotBounties.debugMessage("Since last sync: databaseAdded=" + databaseAdded.size() + " databaseRemoved=" + databaseRemoved.size() + " localAdded=" + localAdded.size() + " localRemoved=" + localRemoved.size(), false);

                if (lastSyncTime == 0) {
                    // never sunk
                    // doesn't have local data
                    // combine both local and database stats
                    database.addStats(localStats);
                    localData.addStats(databaseStats);
                } else {
                    // database has connected before

                    if (databaseStats.isEmpty()) {
                        // nothing in the database yet
                        // if the database restarted, this will add old stats in
                        // add all local stats
                        database.addStats(localStats);
                    } else {

                        // check if this server's stats are in the db already
                        // if the database is a TempDatabase, their bounties will contain the local server id if the data is in the db already
                        // if the database isn't a TempDatabase, all local data with a local server id needs to be migrated
                        if (database instanceof TempDatabase tempDatabase && tempDatabase.getStoredServerIds().contains(DataManager.getDatabaseServerID(false))) {
                            // db is a temp database that has this server's data
                            // push stat changes since last connect
                            Map<UUID, PlayerStat> pushedChanges = databaseWrapper.getStatChanges();
                            database.addStats(pushedChanges);
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
                            database.addStats(localStats);
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
                databaseAdded.stream().filter(bounty -> !loggedPlayers.containsKey(bounty.getName()) && !bounty.getUUID().equals(DataManager.GLOBAL_SERVER_ID)).forEach(bounty -> loggedPlayers.put(bounty.getName(), bounty.getUUID()));
            }
        }.runTaskAsynchronously(NotBounties.getInstance());
    }

    private static void tryDatabaseConnections() {
        for (NotBountiesDatabase database : databases) {
            if (!database.isConnected())
                database.connect();
        }
    }
}

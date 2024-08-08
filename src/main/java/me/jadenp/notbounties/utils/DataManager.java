package me.jadenp.notbounties.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.sql.MySQL;
import me.jadenp.notbounties.sql.SQLGetter;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.BigBounty;
import me.jadenp.notbounties.utils.configuration.BountyExpire;
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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.NotBounties.bountyBoards;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.autoConnect;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class DataManager {
    private static MySQL sql;
    private static SQLGetter data;
    private static final Map<UUID, Double[]> stats = new HashMap<>();
    private static final Map<UUID, Double[]> statChanges = new HashMap<>();
    private static final List<Bounty> activeBounties = new LinkedList<>(); // Collections.synchronizedList(new LinkedList<>()); // stored in descending order
    // changes to be made to the database on reconnect
    // only added bounties will be saved if the server restarts
    private static final List<BountyChange> queuedChanges = new ArrayList<>();

    private static BukkitTask autoConnectTask = null;
    private static boolean firstConnect = true;

    private static long lastSQLLoad = 0;

    private DataManager(){}

    public static long getLastSQLLoad() {
        return lastSQLLoad;
    }

    /**
     * Loads bounties from the
     */
    public static void loadBounties(){
        loadDatabase();
        File bounties = new File(NotBounties.getInstance().getDataFolder() + File.separator + "bounties.yml");
        List<Bounty> bountyList = new ArrayList<>();
        // create bounties file if one doesn't exist
        try {
            if (bounties.createNewFile()) {
                Bukkit.getLogger().info("[NotBounties] Created new storage file.");
            } else {
                // get existing bounties file
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(bounties);
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
                // go through player logs - old version
                i = 0;
                while (configuration.isSet("data." + i + ".uuid")) {
                    UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString("data." + i + ".uuid")));
                    Double[] stat = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
                    if (configuration.isSet("data." + i + ".claimed"))
                        stat[0] = (double) configuration.getLong("data." + i + ".claimed");
                    if (configuration.isSet("data." + i + ".set"))
                        stat[1] = (double) configuration.getLong("data." + i + ".set");
                    if (configuration.isSet("data." + i + ".received"))
                        stat[2] = (double) configuration.getLong("data." + i + ".received");
                    if (configuration.isSet("data." + i + ".all-time")) {
                        stat[3] = configuration.getDouble("data." + i + ".all-time");
                    } else {
                        for (Bounty bounty : bountyList) {
                            // if they have a bounty already
                            if (bounty.getUUID().equals(uuid)) {
                                stat[3] = bounty.getTotalDisplayBounty();
                                Bukkit.getLogger().info("Missing all time bounty for " + bounty.getName() + ". Setting as current bounty.");
                                break;
                            }
                        }
                    }
                    if (configuration.isSet("data." + i + ".immunity"))
                        stat[4] = configuration.getDouble("data." + i + ".immunity");
                    if (configuration.isSet("data." + i + ".all-claimed"))
                        stat[5] = configuration.getDouble("data." + i + ".all-claimed");
                    stats.put(uuid, stat);
                    if (configuration.isSet("data." + i + ".broadcast")) {
                        NotBounties.disableBroadcast.add(uuid);
                    }
                    i++;
                }
                // end old version ^^^
                // new version vvv
                Map<UUID, Long> timedBounties = new HashMap<>();
                if (configuration.isConfigurationSection("data"))
                    for (String uuidString : Objects.requireNonNull(configuration.getConfigurationSection("data")).getKeys(false)) {
                        UUID uuid = UUID.fromString(uuidString);
                        // old data protection
                        if (uuidString.length() < 10)
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
                        stats.put(uuid, stat);
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
                            rewardHeads.add(decodeRewardHead(str));
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
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("[NotBounties] Error loading saved data!");
            Bukkit.getLogger().severe(e.toString());
        }
        activeBounties.addAll(bountyList);
        tryToConnect();
        sortActiveBounties();
    }

    /**
     * Check if NotBounties is connected to an SQL database
     * @return True if NotBounties is connected to an SQL database
     */
    public static boolean isSQLConnected() {
        return sql.isConnected();
    }

    /**
     * Get the SQL database type
     * @return The SQL database type or N/A if a database isn't connected
     */
    public static String getSQLDatabaseType() {
        try {
            return sql.getDatabaseType();
        } catch (SQLException e) {
            return "N/A";
        }
    }

    /**
     * Get the server ID that was set in the config for the SQL Database.
     * @return The set server ID for NotBounties.
     */
    public static int getServerID() {
        return sql.getServerID();
    }

    /**
     * Get players on the network
     * @return All players online in the SQL database, or all server players if the database isn't connected
     */
    public static List<OfflinePlayer> getNetworkPlayers() {
        if (sql.isConnected())
            return data.getOnlinePlayers();
        return Bukkit.getOnlinePlayers().stream().filter(player -> !isVanished(player)).collect(Collectors.toList());
    }

    /**
     *
     */
    public static void startAutoConnect() {
        if (autoConnectTask != null) {
            autoConnectTask.cancel();
        }
        try {
            if (!firstConnect && (sql.isConnected() || autoConnect)) {
                sql.reconnect();
                loadSQLData(false);
            }
        } catch (SQLException ignored) {
            // error connecting
            // errors are expected if autoConnect is enabled and a database isn't set up
        }
        if (firstConnect) {
            firstConnect = false;
        }
        if (autoConnect) {
            autoConnectTask = new BukkitRunnable() {
                @Override
                public void run() {
                    tryToConnect();
                }
            }.runTaskTimer(NotBounties.getInstance(), 600, 600);
        }

    }

    public static double getStat(UUID uuid, Leaderboard leaderboard) {
        if (leaderboard == Leaderboard.CURRENT) {
            Bounty bounty = getBounty(uuid);
            if (bounty != null)
                return bounty.getTotalDisplayBounty();
            else
                return 0;
        }
        if (stats.containsKey(uuid)) {
            Double[] stat = stats.get(uuid);
            switch (leaderboard) {
                case KILLS -> {
                    return stat[0];
                }
                case SET -> {
                    return stat[1];
                }
                case DEATHS -> {
                    return stat[2];
                }
                case ALL -> {
                    return stat[3];
                }
                case IMMUNITY -> {
                    return stat[4];
                }
                case CLAIMED -> {
                    return stat[5];
                }
                default -> {
                    return 0;
                }
            }
        }
        return 0;
    }

   public static Map<UUID, Double[]> getAllStats() {
        return stats;
   }

    /**
     * Change stats of the player. Current stat cannot be changed here.
     * @param uuid UUID of the player.
     * @param leaderboard Stat to change.
     * @param change The amount the stat should change by.
     */
    public static synchronized void changeStat(UUID uuid, Leaderboard leaderboard, double change, boolean commit) {
        Double[] stat = stats.containsKey(uuid) ? stats.get(uuid) : new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
        Double[] prevChanges = statChanges.containsKey(uuid) && (!commit || !sql.isConnected()) ? statChanges.get(uuid) : new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
        switch (leaderboard) {
            case KILLS -> {
                stat[0]+= change;
                prevChanges[0] += change;
            }
            case SET -> {
                stat[1]+= change;
                prevChanges[1]+=change;
            }
            case DEATHS -> {
                stat[2]+= change;
                prevChanges[2]+=change;
            }
            case ALL -> {
                stat[3]+= change;
                prevChanges[3]+=change;
            }
            case IMMUNITY -> {
                stat[4]+= change;
                prevChanges[4]+=change;
            }
            case CLAIMED -> {
                stat[5]+= change;
                prevChanges[5]+=change;
            }
            default -> {
                // current stat can't be changed with this method because it is based on their current bounty
            }
        }
        stats.put(uuid, stat);
        if (!commit || !sql.isConnected())
            statChanges.put(uuid, prevChanges);
        else
            data.addData(uuid, prevChanges);
    }

    /**
     * Sorts the active bounties
     */
    private static void sortActiveBounties() {
            activeBounties.sort(Comparator.reverseOrder());
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

    public static void loadDatabase(){
        sql = new MySQL(NotBounties.getInstance());
        data = new SQLGetter(sql);

    }

    public static @Nullable Bounty getBounty(UUID receiver) {
        for (Bounty bounty : activeBounties) {
            if (bounty.getUUID().equals(receiver))
                return bounty;
        }
        return null;
    }

    public static boolean hasBounty(UUID receiver) {
            return getBounty(receiver) != null;
    }

    /**
     * Replaces a stored bounty without updating the databases
     * @param uuid uuid of the bounty to be replaced
     * @param bounty new bounty, or null to remove
     */
    private static void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        for (int i = 0; i < activeBounties.size(); i++) {
            if (activeBounties.get(i).getUUID().equals(uuid)) {
                if (bounty != null)
                    activeBounties.set(i, bounty);
                else
                    activeBounties.remove(i);
                break;
            }
        }
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
        // Check if the SQL object has been loaded
        // If not, no bounties should be loaded locally either
        if (sql == null)
            return new ArrayList<>();
        return sortBounties(sortType);
    }

    /**
     * Get the stats that are only stored locally. This will load sql data if possible.
     * @return Stats to be stored locally.
     */
    public static Map<UUID, Double[]> getLocalStats(){
        if (sql.isConnected()) {
            loadSQLData(true);
        }
        if (sql.hasConnectedBefore()) {
            // only return stats from stat changes
            return statChanges;
        } else {
            return stats;
        }
    }

    /**
     * Get a list of bounties that are only stored locally. This will load sql data if possible.
     * @return Bounties to be stored locally.
     */
    public static List<Bounty> getLocalBounties() {
        if (sql.isConnected()) {
            loadSQLData(true);
        }
        if (sql.hasConnectedBefore()) {
            // only returned bounties created from queued changes
            // this will only preserve added bounties and other changes if they were to a recently added bounty
            // queuedChanges is likely to be empty unless the database lost connection
            List<Bounty> createdBounties = new ArrayList<>();
            for (BountyChange bountyChange : queuedChanges) {
                switch (bountyChange.changeType()) {
                    case ADD_BOUNTY -> createdBounties.add((Bounty) bountyChange.change());
                    case DELETE_BOUNTY -> {
                        Bounty toRemove = (Bounty) bountyChange.change();
                        ListIterator<Bounty> bountyListIterator = createdBounties.listIterator();
                        while (bountyListIterator.hasNext()) {
                            Bounty createdBounty = bountyListIterator.next();
                            if (createdBounty.getUUID().equals(toRemove.getUUID())) {
                                deleteSetters(createdBounty, toRemove.getSetters());
                                if (createdBounty.getSetters().isEmpty())
                                    bountyListIterator.remove();
                            }
                        }
                    }
                    case EDIT_BOUNTY -> {
                        Bounty toEdit = (Bounty) bountyChange.change();
                        Setter original = toEdit.getSetters().get(0);
                        Setter change = toEdit.getSetters().get(1);
                        boolean madeEdit = false;
                        for (Bounty createdBounty : createdBounties) {
                            if (createdBounty.getUUID().equals(toEdit.getUUID())) {
                                createdBounty.editBounty(original.getUuid(), change.getAmount());
                                madeEdit = true;
                                break;
                            }
                        }
                        // could not find bounty to edit in queue
                        if (!madeEdit)
                            createdBounties.add(new Bounty(toEdit.getUUID(), new ArrayList<>(List.of(new Setter(original.getName(), original.getUuid(), original.getAmount() + change.getAmount(), original.getItems(), original.getTimeCreated(), original.isNotified(), original.getWhitelist(), original.getReceiverPlaytime(), original.getDisplayAmount() + change.getAmount()))), toEdit.getName()));
                    }
                    case NOTIFY -> {
                        // 0: bounty uuid
                        // 1: setter uuid
                        UUID[] uuids = (UUID[]) bountyChange.change();
                        for (Bounty createdBounty : createdBounties) {
                            if (createdBounty.getUUID().equals(uuids[0])) {
                                for (Setter setter : createdBounty.getSetters()) {
                                    if (setter.getUuid().equals(uuids[1]))
                                        setter.setNotified(true);
                                }
                            }
                        }
                    }
                }
            }
            return createdBounties;
        } else {
            // return all active bounties
            return activeBounties;
        }
    }

    public static List<Bounty> sortBounties(int sortType) {
        // how bounties are sorted
        List<Bounty> sortedList;
        sortedList = new ArrayList<>(activeBounties);
        if (sortType == -1)
            return sortedList;
        if (sortType == 2)
            return sortedList;
        if (sortType == 3) {
            Collections.reverse(sortedList);
            return sortedList;
        }
        if (sortedList.isEmpty())
            return sortedList;
        Bounty temp;
        for (int i = 0; i < sortedList.size(); i++) {
            for (int j = i + 1; j < sortedList.size(); j++) {
                if ((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                        (sortedList.get(i).getSetters().get(0).getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 1)){// newest bounties at top
                    temp = sortedList.get(i);
                    sortedList.set(i, sortedList.get(j));
                    sortedList.set(j, temp);
                }
            }
        }
        return sortedList;
    }

    /**
     * Remove a bounty from the active bounties
     * @param uuid UUID of the player to remove
     */
    public static void deleteBounty(UUID uuid) {
        Bounty removed = null;
        ListIterator<Bounty> bountyListIterator = activeBounties.listIterator();
        while (bountyListIterator.hasNext()) {
            Bounty bounty = bountyListIterator.next();
            if (bounty.getUUID().equals(uuid)) {
                bountyListIterator.remove();
                removed = bounty;
                break;
            }
        }
        if (removed != null) {
            if (!sql.isConnected() || !data.removeBounty(uuid))
                queuedChanges.add(new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, removed));
        }
    }

    /**
     * Returns a bounty that is guaranteed to be active. This checks all connected databases for consistency
     * @param uuid UUID of the bountied player
     * @return The bounty for the player or null if the player doesn't have one
     */
    public static @Nullable Bounty getGuarrenteedBounty(UUID uuid) {
        if (sql.isConnected()) {
            // grab bounty from sql database & apply queued changes
            List<BountyChange> completedChanges = new ArrayList<>();
            Bounty sqlBounty = data.getBounty(uuid);
            synchronized (queuedChanges) {
                ListIterator<BountyChange> changesIterator = queuedChanges.listIterator();
                while (changesIterator.hasNext()) {
                    BountyChange bountyChange = changesIterator.next();
                    switch (bountyChange.changeType()) {
                        case ADD_BOUNTY -> {
                            Bounty toAdd = (Bounty) bountyChange.change();
                            if (toAdd.getUUID().equals(uuid)) {
                                if (sqlBounty != null) {
                                    for (Setter setter : toAdd.getSetters())
                                        sqlBounty.addBounty(setter);
                                } else {
                                    sqlBounty = new Bounty(toAdd);
                                }
                                completedChanges.add(bountyChange);
                                changesIterator.remove();
                            }
                        }
                        case EDIT_BOUNTY -> {
                            Bounty toEdit = (Bounty) bountyChange.change();
                            Setter original = toEdit.getSetters().get(0);
                            Setter change = toEdit.getSetters().get(1);
                            if (toEdit.getUUID().equals(uuid)) {
                                if (sqlBounty != null) {
                                    sqlBounty.editBounty(original.getUuid(), change.getAmount());
                                } else {
                                    sqlBounty = new Bounty(toEdit.getUUID(), new ArrayList<>(List.of(new Setter(original.getName(), original.getUuid(), original.getAmount() + change.getAmount(), original.getItems(), original.getTimeCreated(), original.isNotified(), original.getWhitelist(), original.getReceiverPlaytime(), original.getDisplayAmount() + change.getAmount()))), toEdit.getName());
                                }
                                completedChanges.add(bountyChange);
                                changesIterator.remove();
                            }
                        }
                        case DELETE_BOUNTY -> {
                            Bounty toDelete = (Bounty) bountyChange.change();
                            if (toDelete.getUUID().equals(uuid) && sqlBounty != null) {
                                deleteSetters(sqlBounty, toDelete.getSetters());
                                // remove bounty if setter list is empty
                                if (sqlBounty.getSetters().isEmpty())
                                    sqlBounty = null;
                                completedChanges.add(bountyChange);
                                changesIterator.remove();
                            }
                        }
                        case NOTIFY -> {
                            // 0: bounty uuid
                            // 1: setter uuid
                            UUID[] uuids = (UUID[]) bountyChange.change();
                            if (uuids[0].equals(uuid) && sqlBounty != null) {
                                for (Setter setter : sqlBounty.getSetters()) {
                                    if (setter.getUuid().equals(uuids[1]) && setter.getTimeCreated() < lastSQLLoad) {
                                        setter.setNotified(true);
                                    }
                                }
                                completedChanges.add(bountyChange);
                                changesIterator.remove();
                            }
                        }
                    }
                }
            }
            // apply changes to the database
            if (!data.pushChanges(completedChanges, new HashMap<>(), lastSQLLoad))
                queuedChanges.addAll(completedChanges);
            replaceBounty(uuid, sqlBounty);
            return sqlBounty;
        } else {
            return getBounty(uuid);
        }
    }

    /**
     * Removes setters from a bounty. This only checks if the setters uuid matches and the time was before lastSQLLoad.
     * This does not commit the change to any database.
     * @param original Original bounty to modify.
     * @param toRemove Setters to remove
     */
    private static void deleteSetters(Bounty original, List<Setter> toRemove) {
        ListIterator<Setter> setterListIterator = original.getSetters().listIterator();
        while (setterListIterator.hasNext()) {
            Setter setter = setterListIterator.next();
            // remove setter if any setter uuids match and they were created before last sql load
            for (Setter toRemoveSetter : toRemove) {
                if (setter.getUuid().equals(toRemoveSetter.getUuid()) && setter.getTimeCreated() < lastSQLLoad && setter.getWhitelist().equals(toRemoveSetter.getWhitelist())) {
                    setterListIterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * Edit a bounty
     * @param bounty Bounty to be edited
     * @param setterUUID UUID of the setter to be edited
     * @param change The amount the bounty should change
     * @return The new bounty or null if the bounty is not active
     */
    public static Bounty editBounty(@NotNull Bounty bounty, @Nullable UUID setterUUID, double change) {
        if (!activeBounties.contains(bounty)) {
            bounty = getBounty(bounty.getUUID());
            if (bounty == null)
                return null;
        }
        List<BountyChange> changes = new ArrayList<>();
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
                            changes.add(new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, new Bounty(bounty.getUUID(), new ArrayList<>(List.of(setter)), bounty.getName())));
                            lastSetter = setter;
                        } else if (setter.getAmount() > 0) {
                            // update amount (minimum 0)
                            double newAmount = Math.min(0, setter.getAmount() + change);
                            change+= setter.getAmount() - newAmount; // update amount needed to be changed
                            Setter newSetter = new Setter(setter.getName(), setterUUID, newAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() - (setter.getAmount() - newAmount));
                            setterListIterator.set(newSetter);
                            changes.add(new BountyChange(BountyChange.ChangeType.EDIT_BOUNTY, new Bounty(bounty.getUUID(), new ArrayList<>(List.of(setter, new Setter(consoleName, new UUID(0,0), setter.getAmount() - newAmount, new ArrayList<>(), System.currentTimeMillis(), true, new Whitelist(new ArrayList<>(), false), BountyExpire.getTimePlayed(bounty.getUUID())))), bounty.getName())));
                            lastSetter = setter;
                        }
                    } else {
                        // setter amount won't go negative
                        // update setter amount
                        Setter newSetter = new Setter(setter.getName(), setterUUID, setter.getAmount() + change, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + change);
                        changes.add(new BountyChange(BountyChange.ChangeType.EDIT_BOUNTY, new Bounty(bounty.getUUID(), new ArrayList<>(List.of(setter, new Setter(consoleName, new UUID(0,0), change, new ArrayList<>(), System.currentTimeMillis(), true, new Whitelist(new ArrayList<>(), false), BountyExpire.getTimePlayed(bounty.getUUID())))), bounty.getName())));
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
                changes.add(new BountyChange(BountyChange.ChangeType.ADD_BOUNTY, new Bounty(bounty.getUUID(), new ArrayList<>(List.of(bounty.getLastSetter())), bounty.getName())));
            } else {
                // edit last setter with the remaining change
                // this may cause the amount to be negative
                bounty.getSetters().remove(lastSetter);
                bounty.getSetters().add(new Setter(lastSetter.getName(), setterUUID, lastSetter.getAmount() + change, lastSetter.getItems(), lastSetter.getTimeCreated(), lastSetter.isNotified(), lastSetter.getWhitelist(), lastSetter.getReceiverPlaytime(), lastSetter.getDisplayAmount() + change));
                // get the previous changes to be made and update them
                BountyChange bountyChange = changes.remove(changes.size()-1);
                Setter previousSetter = bountyChange.changeType() == BountyChange.ChangeType.DELETE_BOUNTY ? ((Bounty) bountyChange.change()).getLastSetter() : ((Bounty) bountyChange.change()).getSetters().get(0);
                double prevChange = bountyChange.changeType() == BountyChange.ChangeType.DELETE_BOUNTY ? previousSetter.getDisplayAmount() : ((Bounty) bountyChange.change()).getSetters().get(1).getDisplayAmount();
                changes.add(new BountyChange(BountyChange.ChangeType.EDIT_BOUNTY, new Bounty(bounty.getUUID(), new ArrayList<>(List.of(previousSetter, new Setter(consoleName, new UUID(0,0), change + prevChange, new ArrayList<>(), System.currentTimeMillis(), true, new Whitelist(new ArrayList<>(), false), BountyExpire.getTimePlayed(bounty.getUUID())))), bounty.getName())));
            }
        }
        queuedChanges.addAll(changes);
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
                    player.sendMessage(parse(prefix + offlineBounty, setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    setter.setNotified(true);
                    addedAmount += setter.getAmount();
                    if (!sql.isConnected())
                        queuedChanges.add(new BountyChange(BountyChange.ChangeType.NOTIFY, new UUID[]{player.getUniqueId(), setter.getUuid()})); // add notification changes to queue
                }
            }
            if (addedAmount > 0 && sql.isConnected()) {
                data.notifyPlayer(player.getUniqueId(), lastSQLLoad);
            }
            BigBounty.setBounty(player, bounty, addedAmount);
            if (bounty.getTotalDisplayBounty() > BigBounty.getThreshold()) {
                displayParticle.add(player.getUniqueId());
            }
        }
    }

    public static void login(@NotNull Player player) {
        if (sql.isConnected()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    data.login(player);
                }
            }.runTaskAsynchronously(NotBounties.getInstance());
        }
    }

    public static void logout(@NotNull Player player) {
        if (sql.isConnected()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    data.logout(player);
                }
            }.runTaskAsynchronously(NotBounties.getInstance());
        }
    }

    public static void removeSetters(@NotNull Bounty bounty, List<Setter> setters) {
        if (!activeBounties.contains(bounty)) {
            // get the stored bounty
            bounty = getBounty(bounty.getUUID());
            if (bounty == null)
                return;
        }
        bounty.getSetters().removeIf(setters::contains);
        if (bounty.getSetters().isEmpty()) {
            deleteBounty(bounty.getUUID());
        } else {
            Bounty removedBounty = new Bounty(bounty.getUUID(), setters, bounty.getName());
            if (!sql.isConnected() || !data.removeBounty(removedBounty))
                queuedChanges.add(new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, removedBounty));
        }
    }

    /**
     * Inserts a bounty into the sorted bountyList
     * @return The new bounty. This bounty will be the combination of all bounties on the same person.
     */
    public static Bounty insertBounty(@Nullable Player setter, @NotNull OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        Bounty prevBounty;
        Bounty newBounty = setter == null ? new Bounty(receiver, amount, items, whitelist) : new Bounty(setter, receiver, amount, items, whitelist);
        if (sql.isConnected()) {
            data.addBounty(newBounty);
        } else {
            queuedChanges.add(new BountyChange(BountyChange.ChangeType.ADD_BOUNTY, new Bounty(newBounty)));
        }


            prevBounty = getBounty(receiver.getUniqueId());
            if (prevBounty == null) {
                // insert new bounty for this player
                int index = Collections.binarySearch(activeBounties, newBounty, Collections.reverseOrder());
                if (index < 0) {
                    index = -index - 1;
                }
                activeBounties.add(index, newBounty);
                prevBounty = newBounty;
            } else {
                // combine with previous bounty
                if (setter == null) {
                    prevBounty.addBounty(amount, items, whitelist);
                } else {
                    prevBounty.addBounty(new Setter(setter.getName(), setter.getUniqueId(), amount, items, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
                }
                sortActiveBounties();
            }

        return prevBounty;
    }

    public static void expireSQLBounties(boolean offlineTracking) {
        if (sql.isConnected()) {
            if (offlineTracking)
                data.removeOldBounties();
            else
                data.removeOldPlaytimeBounties();
        }
    }

    /**
     * Adds a bounty to the active bounties.
     * @param bounty Bounty to be added
     */
    public static void addBounty(Bounty bounty) {
        if (sql.isConnected()) {
            data.addBounty(bounty);
        } else {
            queuedChanges.add(new BountyChange(BountyChange.ChangeType.ADD_BOUNTY, new Bounty(bounty)));
        }
        Bounty prevBounty = getBounty(bounty.getUUID());
        if (prevBounty == null) {
            // insert new bounty for this player
            int index = Collections.binarySearch(activeBounties, bounty, Collections.reverseOrder());
            if (index < 0) {
                index = -index - 1;
            }
            activeBounties.add(index, bounty);
        } else {
            // combine with previous bounty
            for (Setter setter : bounty.getSetters())
                prevBounty.addBounty(setter);
            sortActiveBounties();
        }
    }

    /**
     * First, loads all the bounties on the SQL database.
     * Next, queued changes are edited on the loaded bounties.
     * Then, queued changes are pushed to the database.
     * This way, the bounties can be obtained immediately.
     * Stats are also updated from the database
     */
    private static void loadSQLData(boolean sync) {
        if (sql.isConnected() && System.currentTimeMillis() - lastSQLLoad > 20000L) {
            lastSQLLoad = System.currentTimeMillis();
            if (sync) {
                updateSQLData();
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateSQLData();
                    }
                }.runTaskAsynchronously(NotBounties.getInstance());
            }
        }
    }

    private static void updateSQLData(){
        // check if sql is connected and last load was more than 5 seconds ago

            NotBounties.debugMessage("Sending SQL Request", false);
            List<BountyChange> bountyChanges = new ArrayList<>(queuedChanges); // grab a copy of queued changes
            queuedChanges.clear(); // clear the list
            List<Bounty> sqlBounties = data.getTopBounties(-1); // not sorted
            for (BountyChange bountyChange : bountyChanges) {
                switch (bountyChange.changeType()) {
                    case NOTIFY:
                        // 0: bounty uuid
                        // 1: setter uuid
                        UUID[] uuids = (UUID[]) bountyChange.change();
                        // uuids must match and set time must be before lastSQLLoad
                        for (Bounty bounty : sqlBounties) {
                            if (bounty.getUUID().equals(uuids[0])) {
                                for (Setter setter : bounty.getSetters()) {
                                    if (setter.getUuid().equals(uuids[1]) && setter.getTimeCreated() < lastSQLLoad) {
                                        setter.setNotified(true);
                                    }
                                }
                            }
                        }
                        break;
                    case DELETE_BOUNTY:
                        // bounty to be deleted
                        Bounty toRemove = (Bounty) bountyChange.change();
                        // iterate through all bounties
                        ListIterator<Bounty> bountyListIterator = sqlBounties.listIterator();
                        while (bountyListIterator.hasNext()) {
                            Bounty bounty = bountyListIterator.next();
                            // check if bounty uuids match
                            if (bounty.getUUID().equals(toRemove.getUUID())) {
                                // iterate through setters
                                ListIterator<Setter> setterListIterator = bounty.getSetters().listIterator();
                                while (setterListIterator.hasNext()) {
                                    Setter setter = setterListIterator.next();
                                    // remove setter if any setter uuids match and they were created before last sql load
                                    for (Setter toRemoveSetter : toRemove.getSetters()) {
                                        if (setter.getUuid().equals(toRemoveSetter.getUuid()) && setter.getTimeCreated() < lastSQLLoad)
                                            setterListIterator.remove();
                                    }
                                }
                                // remove bounty if setter list is empty
                                if (bounty.getSetters().isEmpty())
                                    bountyListIterator.remove();
                            }
                        }
                        break;
                    case ADD_BOUNTY:
                        Bounty toAdd = (Bounty) bountyChange.change();
                        sqlBounties.add(toAdd);
                        break;
                    case EDIT_BOUNTY:
                        Bounty toEdit = (Bounty) bountyChange.change();
                        Setter prevSetter = toEdit.getSetters().get(0);
                        Setter change = toEdit.getSetters().get(1);
                        boolean madeEdit = false;
                        // find bounty
                        for (Bounty bounty : sqlBounties) {
                            if (bounty.getUUID().equals(toEdit.getUUID())) {
                                // same bounty - check for setter match
                                ListIterator<Setter> setterListIterator = bounty.getSetters().listIterator();
                                while (setterListIterator.hasNext()) {
                                    Setter setter = setterListIterator.next();
                                    if (setter.getUuid().equals(prevSetter.getUuid()) && setter.getTimeCreated() == prevSetter.getTimeCreated()) {
                                        double newAmount = setter.getAmount() + change.getAmount();
                                        Setter newSetter = new Setter(setter.getName(), setter.getUuid(), newAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + prevSetter.getAmount());
                                        setterListIterator.set(newSetter);
                                        madeEdit = true;
                                        break;
                                    }
                                }
                                if (!madeEdit) {
                                    // did not find a matching setter - create new one
                                    bounty.getSetters().add(new Setter(prevSetter.getName(), prevSetter.getUuid(), change.getAmount(), new ArrayList<>(), prevSetter.getTimeCreated(), prevSetter.isNotified(), prevSetter.getWhitelist(), prevSetter.getReceiverPlaytime()));
                                }
                                break;
                            }
                        }
                        break;

                }
            }
            // sort bounties in descending order
            sqlBounties.sort(Comparator.reverseOrder());
            activeBounties.clear();
            for (Bounty bounty : sqlBounties) {
                activeBounties.add(bounty);
                if (!loggedPlayers.containsValue(bounty.getUUID()))
                    loggedPlayers.put(bounty.getName().toLowerCase(Locale.ROOT), bounty.getUUID());
            }

            // update stats
            Map<UUID, Double[]> queuedStatChanges = new HashMap<>(statChanges);
            statChanges.clear();

            Map<UUID, Double[]> sqlStats = data.getAllStats();
            // apply changes to received stats
            for (Map.Entry<UUID, Double[]> entry : queuedStatChanges.entrySet()) {
                Double[] stat = sqlStats.containsKey(entry.getKey()) ? sqlStats.get(entry.getKey()) : new Double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
                for (int i = 0; i < stat.length; i++) {
                    stat[i] += entry.getValue()[i];
                }
                sqlStats.put(entry.getKey(), stat);
            }
            stats.clear();
            stats.putAll(sqlStats);

            // push changes to the database
            if (!data.pushChanges(bountyChanges, queuedStatChanges, lastSQLLoad)) {
                // add changes back if push failed
                queuedChanges.addAll(bountyChanges);
                statChanges.putAll(queuedStatChanges);
            }
    }


    /**
     * This method attempts to connect to the MySQL database.
     * If a connection is successful, local storage will be migrated
     *
     * @return true if a connection was successful
     */
    public static boolean tryToConnect() {
        if (!sql.isConnected()) {
            try {
                sql.connect();
            } catch (SQLException e) {
                //e.printStackTrace();
                return false;
            }
            if (sql.isConnected()) {
                if (!sql.hasConnectedBefore()) {
                    // first time connection
                    // migrate local data
                    Bukkit.getLogger().info("[NotBounties] Database is connected!");
                    data.createTable();
                    data.createDataTable();
                    data.createOnlinePlayerTable();
                    if (!stats.isEmpty()) {
                        // add entries to database
                            statChanges.clear();
                            for (Map.Entry<UUID, Double[]> entry : stats.entrySet()) {
                                Double[] values = entry.getValue();
                                data.addData(String.valueOf(entry.getKey()), values[0].longValue(), values[1].longValue(), values[2].longValue(), values[3], values[4], values[5]);
                            }
                            stats.clear();

                    }
                        if (!activeBounties.isEmpty()) {
                            Bukkit.getLogger().info("[NotBounties] Migrating local storage to database");
                            // add entries to database

                            queuedChanges.clear(); // all queued changes are in the current migrating bounties
                            for (Bounty bounty : activeBounties) {
                                if (bounty.getTotalDisplayBounty() != 0) {
                                    for (Setter setter : bounty.getSetters()) {
                                        data.addBounty(bounty, setter);
                                    }
                                }
                            }

                        }


                    // load bounties & stats from database
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            loadSQLData(false);
                        }
                    }.runTaskLater(NotBounties.getInstance(), 100L); // 5 seconds

                    // remove data with all stats of 0
                    int cleanedRows = data.removeExtraData();
                    if (cleanedRows > 0)
                        Bukkit.getLogger().info(() -> "[NotBounties] Cleared up " + cleanedRows + " unused rows in the database!");

                    data.refreshOnlinePlayers(); // add current online players on this server to the database
                } else {
                    // reconnected
                    // migrate queuedChanges
                    loadSQLData(false);
                }
            } else {
                return false;
            }
        }
        return true;
    }
}

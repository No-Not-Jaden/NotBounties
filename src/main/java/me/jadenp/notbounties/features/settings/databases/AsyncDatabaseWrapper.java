package me.jadenp.notbounties.features.settings.databases;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.Inconsistent;
import me.jadenp.notbounties.data.PlayerStat;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * This is a wrapper for a NotBounties database that makes adding data asynchronous.
 * When a database is connected, new data is always added right away. Ex: When a bounty is set.
 * Data in the database is always accurate while the database is connected, but losing connection for a while can cause
 * concurrency issues, which is why DataManager has to sync them.
 * <TODO>Move reconnect to here instead of NotBountiesDatabase and implementations</TODO>
 */
public class AsyncDatabaseWrapper extends NotBountiesDatabase {

    private static final Map<String, ActiveAsyncTask> taskList = new HashMap<>();

    /**
     * Gets a delay for an async task as to not overlap any other tasks with the same period.
     * @param name Name of the database to create the task for.
     * @param period The length in milliseconds between task execution.
     * @return The time in milliseconds when the task should start
     */
    private static synchronized long getSafeStartTime(String name, long period) {
        // a sorted list of all the start delays for the tasks that have synchronous periods
        List<Long> samePeriods = taskList.entrySet().stream().filter(e -> !e.getKey().equals(name) && (e.getValue().period() % period == 0 || period % e.getValue().period() == 0)).map(e -> e.getValue().start() % period).sorted().toList();
        if (samePeriods.isEmpty()) {
            // no other tasks with same period
            taskList.put(name, new ActiveAsyncTask(System.currentTimeMillis() + period, period));
            return System.currentTimeMillis() + period;
        }
        long[] startPoints = new long[samePeriods.size() + 2];
        // add points outside the valid bounds (0-period) to represent that the list wraps around
        startPoints[0] = -period + samePeriods.get(samePeriods.size()-1);
        startPoints[startPoints.length-1] = period + samePeriods.get(0);
        // find the maximum distance between points
        long maxDistance = 0;
        long maxMidpoint = 0;
        for (int i = 0; i < startPoints.length-1; i++) {
            long distance = startPoints[i+1] - startPoints[i];
            if (distance > maxDistance && startPoints[i] + distance / 2 > 0 && startPoints[i] + distance / 2 < period) {
                // greater distance & midpoint is within bounds
                maxDistance = distance;
                maxMidpoint = startPoints[i] + distance / 2;
            }
        }
        // the amount of time in ms of when to start the task
        long start = System.currentTimeMillis() - System.currentTimeMillis() % period + maxMidpoint + period;
        taskList.put(name, new ActiveAsyncTask(start, period));
        return start;
    }

    private final NotBountiesDatabase database;
    private TaskImplementation<Void> asyncUpdateTask = null;
    private static final long MIN_UPDATE_INTERVAL = 20000L; // the minimum amount of time between database updates
    private int refreshInterval;
    private long lastOnlinePlayerRequest = 0;
    private final Map<UUID, PlayerStat> statChanges = new HashMap<>();
    private Map<UUID, String> onlinePlayers = new HashMap<>();
    private static final long CONNECTION_TEST_INTERVAL = 500L; // the minimum amount of time between connection tests
    private long lastConnectionTest = 0;
    private boolean lastConnection = false;
    private long lastConnectionAttempt = 0;

    public AsyncDatabaseWrapper(NotBountiesDatabase database) {
        this.database = database;
    }

    private void setAsyncUpdateTask() {
        refreshInterval = database.getRefreshInterval();
        if (asyncUpdateTask != null)
            asyncUpdateTask.cancel();
        if (refreshInterval <= 0)
            // fast database - data should be read directly from the database when needed
            return;
        long startTime = getSafeStartTime(database.getName(), refreshInterval * 1000L);
        asyncUpdateTask = NotBounties.getServerImplementation().async().runAtFixedRate(() -> readDatabaseData(true), (startTime - System.currentTimeMillis()) / 50L, refreshInterval * 20L);
    }

    /**
     * Cancels the async update task and sets it to a null value
     */
    private void stopUpdating() {
        if (asyncUpdateTask != null)
            asyncUpdateTask.cancel();
        taskList.remove(database.getName());
        asyncUpdateTask = null;
    }

    /**
     * Reads the data from the database and updates the local data.
     * @param sync Whether the read should be synchronous or asynchronous.
     */
    public void readDatabaseData(boolean sync) {
        if (isConnected() && System.currentTimeMillis() - database.getLastSync() > Math.min(refreshInterval * 1000L, MIN_UPDATE_INTERVAL)) {
            if (sync) {
                DataManager.getAndSyncDatabase(database);
            } else {
                NotBounties.getServerImplementation().async().runNow(() -> DataManager.getAndSyncDatabase(database));
            }
        }

    }

    public void disconnect() {
        lastConnection = false;
        if (isConnected()) {
            database.disconnect();
            Bukkit.getLogger().warning(() -> "Disconnected from " + database.getName() + ".");
        }
    }

    public NotBountiesDatabase getDatabase() {
        return database;
    }


    /**
     * Adds stats to the statChanges queue
     * @param uuid UUID of the player that the changes are for
     * @param stats Changes in the player's stats
     */
    @Override
    public void addStats(UUID uuid, PlayerStat stats) {
        if (!isConnected()) {
            if (System.currentTimeMillis() - getLastSync() < DataManager.CONNECTION_REMEMBRANCE_MS) {
                if (statChanges.containsKey(uuid)) {
                    statChanges.replace(uuid, statChanges.get(uuid).combineStats(stats));
                } else {
                    statChanges.put(uuid, stats);
                }
            }
            return;
        }
        if (NotBounties.getInstance().isEnabled()) {
            NotBounties.getServerImplementation().async().runNow(() -> {
                if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                    stats.setServerID(DataManager.GLOBAL_SERVER_ID);
                database.addStats(uuid, stats);
            });
        } else {
            if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                stats.setServerID(DataManager.GLOBAL_SERVER_ID);
            database.addStats(uuid, stats);
        }

    }

    @Override
    public @NotNull PlayerStat getStats(UUID uuid) throws IOException {
        return database.getStats(uuid);
    }

    @Override
    public Map<UUID, PlayerStat> getAllStats() throws IOException {
        return database.getAllStats();
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        if (!playerStats.isEmpty()) {
            if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
                NotBounties.getServerImplementation().async().runNow(() -> {
                    if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                        playerStats.forEach((k, v) -> v.setServerID(DataManager.GLOBAL_SERVER_ID));
                    database.addStats(playerStats);
                    statChanges.clear();
                });
            } else {
                if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                    playerStats.forEach((k, v) -> v.setServerID(DataManager.GLOBAL_SERVER_ID));
                database.addStats(playerStats);
                statChanges.clear();
            }

        }
    }

    @Override
    public void addBounty(List<Bounty> bounties) {
        if (!bounties.isEmpty()) {
            if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
                NotBounties.getServerImplementation().async().runNow(() -> {
                    if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                        bounties.forEach(bounty -> bounty.setServerID(DataManager.GLOBAL_SERVER_ID));
                    database.addBounty(bounties);
                });
            } else {
                if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                    bounties.forEach(bounty -> bounty.setServerID(DataManager.GLOBAL_SERVER_ID));
                database.addBounty(bounties);
            }
        }
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {
        if (!bounties.isEmpty()) {
            if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
                NotBounties.getServerImplementation().async().runNow(() -> database.removeBounty(bounties));
            } else {
                database.removeBounty(bounties);
            }
        }
    }

    /**
     * Adds a bounty to the database.
     * @param bounty Bounty to be added
     * @return The bounty that was provided. It will be updated with new setters asynchronously
     */
    @Override
    public Bounty addBounty(@NotNull Bounty bounty) {
        if (isConnected()) {
            if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
                NotBounties.getServerImplementation().async().runNow(() -> syncAddBounty(bounty));
            } else {
                syncAddBounty(bounty);
            }
        }

        return bounty;
    }

    private void syncAddBounty(@NotNull Bounty bounty) {
        try {
            if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                bounty.setServerID(DataManager.GLOBAL_SERVER_ID);
            Bounty newbounty = database.addBounty(bounty);
            if (newbounty != null && !newbounty.equals(bounty)) {
                bounty.getSetters().clear();
                bounty.getSetters().addAll(newbounty.getSetters());
            }

        } catch (IOException e) {
            disconnect();
        }
    }

    @Override
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> database.replaceBounty(uuid, bounty));
        } else {
            database.replaceBounty(uuid, bounty);
        }

    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) throws IOException{
        try {
            return database.getBounty(uuid);
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }

    @Override
    public void removeBounty(UUID uuid) {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> database.removeBounty(uuid));
        } else {
            database.removeBounty(uuid);
        }

    }

    @Override
    public void removeBounty(Bounty bounty) {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> database.removeBounty(bounty));
        } else {
            database.removeBounty(bounty);
        }

    }

    @Override
    public List<Bounty> getAllBounties(int sortType) {
        try {
            return database.getAllBounties(sortType);
        } catch (IOException e) {
            disconnect();
            return DataManager.getLocalData().getAllBounties(sortType);
        }
    }

    public Map<UUID, PlayerStat> getStatChanges() {
        return statChanges;
    }

    @Override
    public String getName() {
        return database.getName();
    }

    @Override
    public boolean isConnected() {
        if (System.currentTimeMillis() - lastConnectionTest > CONNECTION_TEST_INTERVAL) {
            lastConnectionTest = System.currentTimeMillis();
            try {
                lastConnection = database.isConnected();
            } catch (NoClassDefFoundError e) {
                // Couldn't load a dependency.
                // This will be thrown if unable to use Spigot's library loader
                NotBounties.debugMessage("One or more dependencies could not be downloaded to use the database: " + database.getName(), true);
                lastConnection = false;
            }
        }
        return lastConnection;
    }


    /**
     * Attempts to reconnect to the database
     * @return True if the connection was successful
     */
    @Override
    public boolean connect(boolean syncData) {
        if (System.currentTimeMillis() - lastConnectionAttempt < CONNECTION_TEST_INTERVAL) {
            return isConnected();
        }
        lastConnectionAttempt = System.currentTimeMillis();
        try {
            boolean success = database.connect(syncData);
            if (!success) {
                // database is no longer connected
                // stop attempting to receive data
                stopUpdating();
            } else {
                // restart update task
                setAsyncUpdateTask();
                Bukkit.getLogger().info("[NotBounties] Connected to " + database.getName() + "!");
            }
            return success;
        } catch (NoClassDefFoundError e) {
            // Couldn't load a dependency.
            // This will be thrown if unable to use Spigot's library loader
            NotBounties.debugMessage("One or more dependencies could not be downloaded to use the database: " + database.getName(), true);
        }
        return false;
    }

    @Override
    public boolean hasConnectedBefore() {
        return database.hasConnectedBefore();
    }

    @Override
    public int getRefreshInterval() {
        return database.getRefreshInterval();
    }

    @Override
    public long getLastSync() {
        return database.getLastSync();
    }

    @Override
    public void setLastSync(long lastSync) {
        database.setLastSync(lastSync);
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() {
        if (System.currentTimeMillis() - lastOnlinePlayerRequest > MIN_UPDATE_INTERVAL) {
            lastOnlinePlayerRequest = System.currentTimeMillis();
            try {
                onlinePlayers = database.getOnlinePlayers();
            } catch (IOException e) {
                onlinePlayers.clear();
                disconnect();
            }
        }
        Map<UUID, String> currentPlayers = new HashMap<>(onlinePlayers);
        Bukkit.getOnlinePlayers().forEach(player -> currentPlayers.put(player.getUniqueId(), player.getName()));
        return currentPlayers;
    }

    @Override
    public void updatePlayerData(PlayerData playerData) {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> {
                if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                    playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
                database.updatePlayerData(playerData);
            });
        } else {
            if (isPermDatabase() || DataManager.isPermDatabaseConnected())
                playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
            database.updatePlayerData(playerData);
        }
    }

    @Override
    public PlayerData getPlayerData(@NotNull UUID uuid) throws IOException {
        return database.getPlayerData(uuid);
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataMap) {

        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> {
                if (isPermDatabase() || DataManager.isPermDatabaseConnected()) {
                    for (PlayerData playerData : playerDataMap) {
                        playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
                    }
                }
                database.addPlayerData(playerDataMap);
            });
        } else {
            if (isPermDatabase() || DataManager.isPermDatabaseConnected()) {
                for (PlayerData playerData : playerDataMap) {
                    playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
                }
            }
            database.addPlayerData(playerDataMap);
        }
    }

    @Override
    public List<PlayerData> getPlayerData() throws IOException {
        return database.getPlayerData();
    }

    @Override
    public int getPriority() {
        return database.getPriority();
    }

    @Override
    public boolean reloadConfig() {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> {
                if (database.reloadConfig())
                    connect(false);
            });
        } else {
            if (database.reloadConfig()) {
                return connect(false);
            }
        }
        return false;
    }

    @Override
    public void shutdown() {
        database.shutdown();
    }

    @Override
    public void notifyBounty(UUID uuid) {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> database.notifyBounty(uuid));
        } else {
            database.notifyBounty(uuid);
        }

    }

    @Override
    public void login(UUID uuid, String playerName) {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> database.login(uuid, playerName));
        } else {
            database.login(uuid, playerName);
        }
    }

    @Override
    public void logout(UUID uuid) {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> database.logout(uuid));
        } else {
            database.logout(uuid);
        }
    }

    public boolean isPermDatabase() {
        return database.isPermDatabase();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AsyncDatabaseWrapper asyncDatabaseWrapper && asyncDatabaseWrapper.getDatabase().equals(database);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), database, asyncUpdateTask, refreshInterval, lastOnlinePlayerRequest, statChanges, onlinePlayers, lastConnectionTest, lastConnection);
    }
}

record ActiveAsyncTask(long start, long period) { }

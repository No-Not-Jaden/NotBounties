package me.jadenp.notbounties.features.settings.databases;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * This is a wrapper for a NotBounties database that makes adding data asynchronous.
 * When a database is connected, new data is always added right away. Ex: When a bounty is set.
 * Data in the database is always accurate while the database is connected, but losing connection for a while can cause
 * concurrency issues, which is why DataManager has to sync them.
 */
public class AsyncDatabaseWrapper extends NotBountiesDatabase {

    private final NotBountiesDatabase database;
    private static final long MIN_UPDATE_INTERVAL = 20000L; // the minimum amount of time between database updates
    private long lastOnlinePlayerRequest = 0;
    private final Map<UUID, PlayerStat> statChanges = new HashMap<>();
    private Map<UUID, String> onlinePlayers = new HashMap<>();
    private static final long CONNECTION_TEST_INTERVAL = 500L; // the minimum amount of time between connection tests
    private long lastConnectionTest = 0;
    private boolean lastConnection = false;
    private long lastConnectionAttempt = 0;

    public AsyncDatabaseWrapper(NotBountiesDatabase database) {
        super(database, "Async");
        this.database = database;
    }


    public void disconnect() {
        lastConnection = false;
        if (isConnected()) {
            database.disconnect();
            Bukkit.getLogger().warning(() -> "Disconnected from " + database.getName() + ".");
        }
    }

    @Override
    public @Nullable NotBountiesDatabase getWrappedDatabase() {
        return database;
    }

    @FunctionalInterface
    private interface AddDataOperation {
        void run() throws DatabaseConnectionException;
    }

    private void executeVoid(AddDataOperation operation) throws DatabaseConnectionException {
        if (NotBounties.getInstance().isEnabled() && Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(operation::run);
        } else {
            operation.run();
        }
    }


    @Override
    public void setAllBroadcastSetting(PlayerData.BroadcastSettings broadcastSetting) {
        executeVoid(() -> database.setAllBroadcastSetting(broadcastSetting));
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
        executeVoid(() -> {
            stats.setServerID(DataManager.GLOBAL_SERVER_ID);
            database.addStats(uuid, stats);
        });

    }

    @Override
    public @NotNull PlayerStat getStats(UUID uuid) throws DatabaseConnectionException {
        return database.getStats(uuid);
    }

    @Override
    public Map<UUID, PlayerStat> getStats(Leaderboard sortStat, StatSortType sortType, UUID lastUUID, Object lastVal, int limit) throws DatabaseConnectionException {
        return database.getStats(sortStat, sortType, lastUUID, lastVal, limit);
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        if (!playerStats.isEmpty()) {
            executeVoid(() -> {
                playerStats.forEach((k, v) -> v.setServerID(DataManager.GLOBAL_SERVER_ID));
                database.addStats(playerStats);
                statChanges.clear(); // why is this cleared here?
            });

        }
    }

    @Override
    public void addBounty(List<Bounty> bounties) {
        if (!bounties.isEmpty()) {
            executeVoid(() -> {
                bounties.forEach(bounty -> bounty.setServerID(DataManager.GLOBAL_SERVER_ID));
                database.addBounty(bounties);
            });
        }
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {
        if (!bounties.isEmpty()) {
            executeVoid(() -> database.removeBounty(bounties));
        }
    }

    /**
     * Adds a bounty to the database.
     * @param bounty Bounty to be added
     * @return The bounty that was provided. It will be updated with new setters asynchronously
     */
    @Override
    public Bounty addBounty(@NotNull Bounty bounty) {
        executeVoid(() -> {
            bounty.setServerID(DataManager.GLOBAL_SERVER_ID);
            Bounty newbounty = database.addBounty(bounty);
            if (newbounty != null && !newbounty.equals(bounty)) {
                bounty.getSetters().clear();
                bounty.getSetters().addAll(newbounty.getSetters());
            }
        });
        return bounty;
    }

    @Override
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        executeVoid(() -> database.replaceBounty(uuid, bounty));
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) throws DatabaseConnectionException{
        return database.getBounty(uuid);
    }

    @Override
    public void removeBounty(UUID uuid) {
        executeVoid(() -> database.removeBounty(uuid));
    }

    @Override
    public void removeBounty(Bounty bounty) {
        executeVoid(() -> database.removeBounty(bounty));
    }

    @Override
    public List<Bounty> getBounties(BountySortType sortType, UUID lastUUID, Object lastVal, int limit) throws DatabaseConnectionException {
        return database.getBounties(sortType, lastUUID, lastVal, limit);
    }

    public Map<UUID, PlayerStat> getStatChanges() {
        return statChanges;
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
            if (success) {
                logger.info("Connected to " + database.getName() + "!");
            }
            return success;
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
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
            } catch (DatabaseConnectionException e) {
                onlinePlayers.clear();
            }
        }
        Map<UUID, String> currentPlayers = new HashMap<>(onlinePlayers);
        Bukkit.getOnlinePlayers().forEach(player -> currentPlayers.put(player.getUniqueId(), player.getName()));
        return currentPlayers;
    }

    @Override
    public void updatePlayerData(PlayerData playerData) {
        if (playerData.getPlayerName() == null) {
            return;
        }
        executeVoid(() -> {
            playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
            database.updatePlayerData(playerData);
        });
    }

    @Override
    public PlayerData getPlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {
        return database.getPlayerData(uuid);
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataMap) {
        executeVoid(() -> {
            for (PlayerData playerData : playerDataMap) {
                playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
            }
            database.addPlayerData(playerDataMap);
        });
    }

    @Override
    public List<PlayerData> getPlayerData(PlayerSortType sortType, UUID lastUUID, Object lastVal, int limit) throws DatabaseConnectionException {
        return database.getPlayerData(sortType, lastUUID, lastVal, limit);
    }

    @Override
    public int getPriority() {
        return database.getPriority();
    }

    @Override
    public boolean reloadConfig() {
        executeVoid(() -> {
            if (database.reloadConfig())
                connect(false);
        });
        return false;
    }

    @Override
    public synchronized void shutdown() {
        database.shutdown();
    }

    @Override
    public void notifyBounty(UUID uuid) {
        executeVoid(() -> database.notifyBounty(uuid));
    }

    @Override
    public void login(UUID uuid, String playerName) {
        executeVoid(() -> database.login(uuid, playerName));
    }

    @Override
    public void logout(UUID uuid) {
        executeVoid(() -> database.logout(uuid));
    }

    @Override
    public List<OnlineRefund<?>> getAndRemoveRefunds(UUID uuid) throws DatabaseConnectionException {
        return database.getAndRemoveRefunds(uuid);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AsyncDatabaseWrapper asyncDatabaseWrapper && asyncDatabaseWrapper.getDatabase().equals(database);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), database, lastOnlinePlayerRequest, statChanges, onlinePlayers, lastConnectionTest, lastConnection);
    }
}
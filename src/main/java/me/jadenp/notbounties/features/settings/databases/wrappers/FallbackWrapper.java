package me.jadenp.notbounties.features.settings.databases.wrappers;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.settings.databases.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Updates to the database update the fallback database and delete the out-of-date data from the current database.
 * Queries to the database request from the current database and pull from the fallback if it is missing.
 * It may be possible for data to mismatch if the fallbackDB.isConnected() check succeeds, but the update call after that fails.
 */
public class FallbackWrapper extends NotBountiesDatabase {

    /**
     * The database to request data from if data does not exist or there is an error retrieving it.
     * This database is also used for updating data.
     * A null value means there is no fallback.
     */
    private NotBountiesDatabase fallbackDB = null;
    private final NotBountiesDatabase database;
    private static final String FALLBACK_CONNECTION_ERR = "Fallback database is not connected";

    private void executeUpdateOp(VoidDatabaseOperation dbOp, VoidDatabaseOperation fallbackOp) throws DatabaseConnectionException {
        if (fallbackDB != null) {
            if (fallbackDB.isConnected()) {
                dbOp.run();
                fallbackOp.run();
            } else {
                throw new DatabaseConnectionException(FALLBACK_CONNECTION_ERR);
            }
        } else {
            dbOp.run();
        }
    }

    @FunctionalInterface
    private interface DatabaseSyncOperation<T> {
        void run(T newData) throws DatabaseConnectionException;
    }

    private <T> T executeQueryOp(DatabaseOperation<T> dbOp, DatabaseSyncOperation<T> dbUpdateOp, DatabaseOperation<T> fallbackOp) throws DatabaseConnectionException {
        try {
            T result = dbOp.run();
            if (result == null)
                throw new DatabaseConnectionException("Data not found");
            return result;
        } catch (DatabaseConnectionException e) {
            if (fallbackDB != null) {
                T result = fallbackOp.run();
                try {
                    if (result != null)
                        dbUpdateOp.run(result);
                } catch (DatabaseConnectionException ignored) {
                    // failed to set stats - could be disconnected from the database
                }
                return result;
            } else {
                throw e;
            }
        }
    }

    public FallbackWrapper(NotBountiesDatabase database) {
        super(database, "Fallback");
        this.database = database;
    }

    public void setFallbackDB(NotBountiesDatabase fallbackDB) {
        this.fallbackDB = fallbackDB;
    }

    @Override
    public @Nullable NotBountiesDatabase getWrappedDatabase() {
        return database;
    }


    @Override
    public void setAllBroadcastSetting(PlayerData.BroadcastSettings broadcastSetting) throws DatabaseConnectionException {
        if (fallbackDB != null && fallbackDB.isConnected()) {
            fallbackDB.setAllBroadcastSetting(broadcastSetting);
        }
        database.setAllBroadcastSetting(broadcastSetting);
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.addStats(uuid, stats), () -> fallbackDB.addStats(uuid, stats));
    }

    @Override
    public @Nullable PlayerStat getStats(UUID uuid) throws DatabaseConnectionException {
        return executeQueryOp(() -> database.getStats(uuid), newData -> database.setStats(uuid, newData), () -> fallbackDB.getStats(uuid));
    }

    @Override
    public Map<UUID, PlayerStat> getStats(Leaderboard sortStat, StatSortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        // always use the fallback for leaderboard lookups
        if (fallbackDB != null) {
            return fallbackDB.getStats(sortStat, sortType, offset, limit, excludedPlayers);
        }
        return database.getStats(sortStat, sortType, offset, limit, excludedPlayers);
    }

    @Override
    public long getStatRank(UUID uuid, Leaderboard sortStat, StatSortType sortType, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        if (fallbackDB != null) {
            return fallbackDB.getStatRank(uuid, sortStat, sortType, excludedPlayers);
        }
        return database.getStatRank(uuid, sortStat, sortType, excludedPlayers);
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.addStats(playerStats), () -> fallbackDB.addStats(playerStats));
    }

    @Override
    public void deleteStats(UUID uuid) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.deleteStats(uuid), () -> fallbackDB.deleteStats(uuid));
    }

    @Override
    public void setStats(UUID uuid, PlayerStat stat) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.setStats(uuid, stat), () -> fallbackDB.setStats(uuid, stat));
    }

    @Override
    public void addBounty(List<Bounty> bounties) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.addBounty(bounties), () -> fallbackDB.addBounty(bounties));
    }

    @Override
    public void removeBounty(List<Bounty> bounties) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.removeBounty(bounties), () -> fallbackDB.removeBounty(bounties));
    }

    @Override
    public void addBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.addBounty(bounty), () -> fallbackDB.addBounty(bounty));
    }

    @Override
    public void setBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.setBounty(bounty), () -> fallbackDB.setBounty(bounty));
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) throws DatabaseConnectionException {
        return executeQueryOp(() -> database.getBounty(uuid), database::setBounty, () -> fallbackDB.getBounty(uuid));
    }

    @Override
    public void removeBounty(UUID uuid) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.removeBounty(uuid), () -> fallbackDB.removeBounty(uuid));
    }

    @Override
    public void removeBounty(Bounty bounty) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.removeBounty(bounty), () -> fallbackDB.removeBounty(bounty));
    }

    @Override
    public List<Bounty> getBounties(BountySortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        // allways use fallback for leaderboards
        if (fallbackDB != null) {
            return fallbackDB.getBounties(sortType, offset, limit, excludedPlayers);
        }
        return database.getBounties(sortType, offset, limit, excludedPlayers);
    }

    @Override
    public long getBountyRank(UUID uuid, BountySortType sortType, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        if (fallbackDB != null) {
            return fallbackDB.getBountyRank(uuid, sortType, excludedPlayers);
        }
        return database.getBountyRank(uuid, sortType, excludedPlayers);
    }

    @Override
    public long getNumBounties() throws DatabaseConnectionException {
        if (fallbackDB != null) {
            return fallbackDB.getNumBounties();
        }
        return database.getNumBounties();
    }

    @Override
    public long getNumUniqueBounties() throws DatabaseConnectionException {
        if (fallbackDB != null) {
            return fallbackDB.getNumUniqueBounties();
        }
        return database.getNumUniqueBounties();
    }

    @Override
    public List<ItemStack> getBountyItems(int bountyId) throws DatabaseConnectionException {
        return executeQueryOp(() -> database.getBountyItems(bountyId), newData -> database.setBountyItems(bountyId, newData), () -> fallbackDB.getBountyItems(bountyId));
    }

    @Override
    public List<ItemStack> getRefundItems(int refundId) throws DatabaseConnectionException {
        return executeQueryOp(() -> database.getRefundItems(refundId), newData -> database.setRefundItems(refundId, newData), () -> fallbackDB.getRefundItems(refundId));
    }

    @Override
    public void setBountyItems(int bountyId, @NotNull List<ItemStack> items) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.setBountyItems(bountyId, items), () -> fallbackDB.setBountyItems(bountyId, items));
    }

    @Override
    public void setRefundItems(int refundId, @NotNull List<ItemStack> items) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.setRefundItems(refundId, items), () -> fallbackDB.setRefundItems(refundId, items));
    }

    @Override
    public boolean isConnected() {
        return database.isConnected();
    }

    @Override
    public boolean connect(boolean syncData) {
        return database.connect(syncData);
    }

    @Override
    public void disconnect() {
        database.disconnect();
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws DatabaseConnectionException {
        Map<UUID, String> onlinePlayers = database.getOnlinePlayers();
        if (onlinePlayers.isEmpty() && fallbackDB != null) {
            onlinePlayers = fallbackDB.getOnlinePlayers();
            for (UUID uuid : onlinePlayers.keySet()) {
                database.updatePlayerData(fallbackDB.getPlayerData(uuid));
            }
        }
        return onlinePlayers;
    }

    @Override
    public void updatePlayerData(PlayerData playerData) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.updatePlayerData(playerData), () -> fallbackDB.updatePlayerData(playerData));
    }

    @Override
    public PlayerData getPlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {
        return executeQueryOp(() -> database.getPlayerData(uuid), database::updatePlayerData, () -> fallbackDB.getPlayerData(uuid));
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataMap) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.addPlayerData(playerDataMap), () -> fallbackDB.addPlayerData(playerDataMap));
    }

    @Override
    public List<PlayerData> getPlayerData(PlayerSortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        // allways use fallback for leaderboard
        if (fallbackDB != null) {
            return fallbackDB.getPlayerData(sortType, offset, limit, excludedPlayers);
        }
        return database.getPlayerData(sortType, offset, limit, excludedPlayers);
    }

    @Override
    public void deletePlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.deletePlayerData(uuid), () -> fallbackDB.deletePlayerData(uuid));
    }

    @Override
    public long getNumPlayers() throws DatabaseConnectionException {
        if (fallbackDB != null) {
            return fallbackDB.getNumPlayers();
        }
        return database.getNumPlayers();
    }

    @Override
    public void notifyBounty(UUID uuid) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.notifyBounty(uuid), () -> fallbackDB.notifyBounty(uuid));
    }

    @Override
    public void login(UUID uuid, String playerName) throws DatabaseConnectionException {
        if (fallbackDB != null) {
            fallbackDB.login(uuid, playerName);
            database.updatePlayerData(fallbackDB.getPlayerData(uuid));
        } else {
            database.login(uuid, playerName);
        }

    }

    @Override
    public void logout(UUID uuid) throws DatabaseConnectionException {
        if (fallbackDB != null) {
            fallbackDB.logout(uuid);
            database.updatePlayerData(fallbackDB.getPlayerData(uuid));
        } else {
            database.logout(uuid);
        }
    }

    @Override
    public List<OnlineRefund<?>> getAndRemoveRefunds(UUID uuid) throws DatabaseConnectionException {
        List<OnlineRefund<?>> refunds = database.getAndRemoveRefunds(uuid);
        if (fallbackDB != null) {
            refunds = fallbackDB.getAndRemoveRefunds(uuid);
        }
        return refunds;
    }

    @Override
    public void addRefunds(UUID uuid, List<OnlineRefund<?>> refunds) throws DatabaseConnectionException {
        executeUpdateOp(() -> database.addRefunds(uuid, refunds), () -> fallbackDB.addRefunds(uuid, refunds));
    }

    @Override
    public synchronized void shutdown() {
        database.shutdown();
        if (fallbackDB != null) {
            shutdown();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FallbackWrapper that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(fallbackDB, that.fallbackDB) && Objects.equals(database, that.database);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fallbackDB, database);
    }
}

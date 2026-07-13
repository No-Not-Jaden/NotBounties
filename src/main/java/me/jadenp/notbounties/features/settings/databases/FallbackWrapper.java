package me.jadenp.notbounties.features.settings.databases;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Updates to the database update the fallback database and delete the out-of-date data from the current database.
 * Queries to the database request from the current database and pull from the fallback if it is missing.
 * TODO: Expire values after read
 */
public class FallbackWrapper extends NotBountiesDatabase {

    /**
     * The database to request data from if data does not exist or there is an error retrieving it.
     * This database is also used for updating data.
     * A null value means there is no fallback.
     */
    private NotBountiesDatabase fallbackDB = null;
    private final NotBountiesDatabase database;

    public FallbackWrapper(NotBountiesDatabase database) {
        super(database, "Fallback");
        this.database = database;
    }

    public FallbackWrapper(NotBountiesDatabase database, NotBountiesDatabase fallbackDB) {
        this(database);
        this.fallbackDB = fallbackDB;
    }

    public NotBountiesDatabase getFallbackDB() {
        return fallbackDB;
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
        if (fallbackDB != null && fallbackDB.isConnected()) {
            fallbackDB.addStats(uuid, stats);
        }
        database.deleteStats(uuid);
    }

    @Override
    public @Nullable PlayerStat getStats(UUID uuid) throws DatabaseConnectionException {
        try {
            PlayerStat stats = database.getStats(uuid);
            if (stats == null)
                throw new DatabaseConnectionException("Data not found");
            return stats;
        } catch (DatabaseConnectionException e) {
            if (fallbackDB != null && fallbackDB.isConnected()) {
                PlayerStat stat = fallbackDB.getStats(uuid);
                try {
                    if (stat != null)
                        database.setStats(uuid, stat);
                } catch (DatabaseConnectionException ignored) {
                    // failed to set stats - could be disconnected from the database
                }
                return stat;
            } else {
                throw e;
            }
        }
    }

    @Override
    public Map<UUID, PlayerStat> getStats(Leaderboard sortStat, StatSortType sortType, UUID lastUUID, Object lastVal, int limit) throws DatabaseConnectionException {
        return Map.of();
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) throws DatabaseConnectionException {

    }

    @Override
    public void addBounty(List<Bounty> bounties) throws DatabaseConnectionException {

    }

    @Override
    public void removeBounty(List<Bounty> bounties) throws DatabaseConnectionException {

    }

    @Override
    public Bounty addBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        return null;
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) throws DatabaseConnectionException {
        return null;
    }

    @Override
    public void removeBounty(UUID uuid) throws DatabaseConnectionException {

    }

    @Override
    public void removeBounty(Bounty bounty) throws DatabaseConnectionException {

    }

    @Override
    public List<Bounty> getBounties(BountySortType sortType, UUID lastUUID, Object lastVal, int limit) throws DatabaseConnectionException {
        return List.of();
    }

    @Override
    public List<ItemStack> getBountyItems(int bountyId) throws DatabaseConnectionException {
        return List.of();
    }

    @Override
    public List<ItemStack> getRefundItems(int refundId) throws DatabaseConnectionException {
        return List.of();
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean connect(boolean syncData) {
        return false;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws DatabaseConnectionException {
        return Map.of();
    }

    @Override
    public void updatePlayerData(PlayerData playerData) throws DatabaseConnectionException {

    }

    @Override
    public PlayerData getPlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {
        return null;
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataMap) throws DatabaseConnectionException {

    }

    @Override
    public List<PlayerData> getPlayerData(PlayerSortType sortType, UUID lastUUID, Object lastVal, int limit) throws DatabaseConnectionException {
        return List.of();
    }

    @Override
    public void notifyBounty(UUID uuid) throws DatabaseConnectionException {

    }

    @Override
    public void login(UUID uuid, String playerName) throws DatabaseConnectionException {

    }

    @Override
    public void logout(UUID uuid) throws DatabaseConnectionException {

    }

    @Override
    public List<OnlineRefund<?>> getAndRemoveRefunds(UUID uuid) throws DatabaseConnectionException {
        return List.of();
    }
}

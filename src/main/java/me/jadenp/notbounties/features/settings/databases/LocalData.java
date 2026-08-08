package me.jadenp.notbounties.features.settings.databases;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.features.settings.databases.wrappers.NotBountiesDatabase;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class LocalData extends NotBountiesDatabase {
    // elements must expire quickly to be up to date with other servers
    private final Cache<UUID, Bounty> bountyCache;
    private final Cache<UUID, PlayerStat> statCache;
    private final Cache<UUID, PlayerData> playerDataCache;

    public LocalData(Plugin plugin, long maxCacheSize, long refreshInterval) {
        super(plugin, "LocalData");
        bountyCache = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
                .expireAfterAccess(refreshInterval, TimeUnit.SECONDS).build();
        statCache = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
                .expireAfterAccess(refreshInterval, TimeUnit.SECONDS).build();
        playerDataCache = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
                .expireAfterAccess(refreshInterval, TimeUnit.SECONDS).build();
    }

    @Override
    public void setAllBroadcastSetting(PlayerData.BroadcastSettings broadcastSetting) {
        for (PlayerData playerData : playerDataCache.asMap().values()) {
            playerData.setBroadcastSettings(broadcastSetting);
        }
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) {
        PlayerStat playerStat = statCache.getIfPresent(uuid);
        if (playerStat != null)
            stats = stats.combineStats(playerStat);
        statCache.put(uuid, stats);
    }

    @Override
    public @Nullable PlayerStat getStats(UUID uuid) {
        return statCache.getIfPresent(uuid);
    }

    @Override
    public Map<UUID, PlayerStat> getStats(Leaderboard sortStat, StatSortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        throw new DatabaseConnectionException("Leaderboard lookup not allowed locally.");
    }

    public Map<UUID, PlayerStat> getCachedStats() {
        return statCache.asMap();
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet()) {
            addStats(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void deleteStats(UUID uuid) throws DatabaseConnectionException {
        statCache.invalidate(uuid);
    }

    @Override
    public void setStats(UUID uuid, PlayerStat stat) throws DatabaseConnectionException {
        statCache.put(uuid, stat);
    }

    @Override
    public void addBounty(List<Bounty> bounties) {
        // called on database synchronizations
        for (Bounty bounty : bounties) {
            addBounty(bounty);
        }
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {
        for (Bounty bounty : bounties)
            removeBounty(bounty);
    }

    @Override
    public void addBounty(@NotNull Bounty bounty) {
        Bounty prevBounty = getBounty(bounty.getUUID());
        if (prevBounty == null) {
            // insert a new bounty for this player
            bountyCache.put(bounty.getUUID(), bounty);
        } else {
            // combine with previous bounty
            for (Setter setter : bounty.getSetters()) {
                prevBounty.addBounty(setter);
            }
            bountyCache.put(bounty.getUUID(), prevBounty);
        }
    }

    @Override
    public void setBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        bountyCache.put(bounty.getUUID(), bounty);
    }

    @Override
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        if (bounty != null) {
            bountyCache.put(uuid, bounty);
        } else {
            bountyCache.invalidate(uuid);
        }
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) {
        return bountyCache.getIfPresent(uuid);
    }

    @Override
    public void removeBounty(Bounty bounty) {
        Bounty prevBounty = getBounty(bounty.getUUID());
        if (prevBounty != null) {
            removeSimilarSetters(prevBounty.getSetters(), new ArrayList<>(bounty.getSetters()));
            if (prevBounty.getSetters().isEmpty()) {
                // no setters remain
                bountyCache.invalidate(prevBounty.getUUID());
            }
        }
    }

    @Override
    public List<Bounty> getBounties(BountySortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        throw new DatabaseConnectionException("Leaderboard lookup not allowed locally.");
    }

    public List<Bounty> getCachedBounties() {
        return new ArrayList<>(bountyCache.asMap().values());
    }

    @Override
    public long getNumBounties() throws DatabaseConnectionException {
        long numBounties = 0;
        for (Map.Entry<UUID, Bounty> entry : bountyCache.asMap().entrySet()) {
            numBounties += entry.getValue().getSetters().size();
        }
        return numBounties;
    }

    @Override
    public long getNumUniqueBounties() throws DatabaseConnectionException {
        return bountyCache.size();
    }

    @Override
    public List<ItemStack> getBountyItems(int bountyId) throws DatabaseConnectionException {
        throw new DatabaseConnectionException("Item lookup not allowed locally.");
    }

    @Override
    public List<ItemStack> getRefundItems(int refundId) throws DatabaseConnectionException {
        throw new DatabaseConnectionException("Item lookup not allowed locally.");
    }

    @Override
    public void setBountyItems(int bountyId, @NotNull List<ItemStack> items) throws DatabaseConnectionException {
        // find bounty with id (if exists) and update
        for (Bounty bounty : bountyCache.asMap().values()) {
            for (Setter setter : bounty.getSetters()) {
                Optional<Integer> bId = setter.getBountyId();
                if (bId.isPresent() && bId.get() == bountyId) {
                    setter.setItems(items);
                    break;
                }
            }
        }
    }

    @Override
    public void setRefundItems(int refundId, @NotNull List<ItemStack> items) throws DatabaseConnectionException {
        // refund items are not cached because they are removed as soon as they are queried.
    }

    /**
     * A utility to remove all setters that have the same uuid and time created in both lists.
     * Setters are removed in both lists.
     */
    private static void removeSimilarSetters(List<Setter> masterSetterList, List<Setter> setterList) {
        // iterate through setters
        ListIterator<Setter> masterSetters = masterSetterList.listIterator();
        while (masterSetters.hasNext()) {
            Setter masterSetter = masterSetters.next();
            ListIterator<Setter> setters = setterList.listIterator();
            while (setters.hasNext()) {
                Setter setter = setters.next();
                if (setter.getUuid().equals(masterSetter.getUuid()) && setter.getTimeCreated() == masterSetter.getTimeCreated()) {
                    setters.remove();
                    masterSetters.remove();
                    break;
                }
            }
        }
    }


    @Override
    public void removeBounty(UUID uuid) {
        bountyCache.invalidate(uuid);
    }

    @Override
    public String getName() {
        return "Local Data";
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean connect(boolean syncData) {
        return true;
    }

    @Override
    public void disconnect() {
        // cannot disconnect from local data
    }

    @Override
    public boolean hasConnectedBefore() {
        return true;
    }

    @Override
    public long getLastSync() {
        return 0;
    }

    @Override
    public void setLastSync(long lastSync) {
        // Always synced
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() {
        // Not tracking players on other servers
        return Collections.emptyMap();
    }

    @Override
    public void updatePlayerData(PlayerData playerData) {
        if (playerData.getPlayerName() == null)
            return;
        playerDataCache.put(playerData.getUuid(), playerData);
    }

    public PlayerData getPlayerData(@NotNull UUID uuid) {
        PlayerData playerData;
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID)) {
            playerData = new PlayerData();
        } else {
            playerData = playerDataCache.getIfPresent(uuid);
            if (playerData == null) {
                playerData = new PlayerData();
                playerData.setUuid(uuid);
                playerData.setServerID(Databases.getDatabaseServerID());
                return playerData;
            }
        }
        if (playerData.getUuid() == null) {
            playerData.setUuid(uuid);
        }
        return playerData;
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataMap) {
        for (PlayerData playerData : playerDataMap) {
            updatePlayerData(playerData);
        }
    }

    @Override
    public List<PlayerData> getPlayerData(PlayerSortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        throw new DatabaseConnectionException("Leaderboard lookup not allowed locally.");
    }

    public List<PlayerData> getCachedPlayerData() {
        return new ArrayList<>(playerDataCache.asMap().values());
    }

    @Override
    public void deletePlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {
        playerDataCache.invalidate(uuid);
    }

    @Override
    public long getNumPlayers() throws DatabaseConnectionException {
        return playerDataCache.size();
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean reloadConfig() {
        // no configuration for this database
        return false;
    }

    @Override
    public synchronized ConfigurationSection readConfig() {
        // no configuration for local data.
        return null;
    }

    @Override
    public synchronized void shutdown() {
        // no shutdown operations
    }

    @Override
    public void notifyBounty(UUID uuid) {
        Bounty bounty = getBounty(uuid);
        if (bounty != null) {
            bounty.notifyBounty();
        }
    }

    @Override
    public void login(UUID uuid, String playerName) {
        // This data is local, so bukkit methods can be used to retrieve status
    }

    @Override
    public void logout(UUID uuid) {
        // This data is local, so bukkit methods can be used to retrieve status
    }

    @Override
    public List<OnlineRefund<?>> getAndRemoveRefunds(UUID uuid) throws DatabaseConnectionException {
        throw new DatabaseConnectionException("Refund lookup not allowed locally.");
    }

    @Override
    public void addRefunds(UUID uuid, List<OnlineRefund<?>> refunds) throws DatabaseConnectionException {
        // refunds are not cached
    }

    @Override
    public @Nullable NotBountiesDatabase getWrappedDatabase() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LocalData localData = (LocalData) o;
        return Objects.equals(bountyCache, localData.bountyCache) && Objects.equals(statCache, localData.statCache) && Objects.equals(playerDataCache, localData.playerDataCache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bountyCache, statCache, playerDataCache);
    }
}

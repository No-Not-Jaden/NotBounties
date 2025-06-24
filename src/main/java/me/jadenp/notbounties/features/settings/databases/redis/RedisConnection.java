package me.jadenp.notbounties.features.settings.databases.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StaticCredentialsProvider;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.settings.databases.NotBountiesDatabase;
import me.jadenp.notbounties.features.settings.databases.TempDatabase;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * Data is stored in Redis which is basically a string hashmap
 */
public class RedisConnection extends NotBountiesDatabase implements TempDatabase {
    private RedisClient redis = null;
    private StatefulRedisConnection<String, String> connection = null;
    private RedisCommands<String, String> data = null;
    private static ClientResources sharedResources = null;

    private String username = "username";
    private String password = "pass";
    private String host = "localhost";
    private int port = 3306;
    private int databaseNumber = -1;
    private boolean ssl = false;

    private static final String BOUNTIES_KEY = "bounties";
    private static final String STATS_KEY = "stats";
    private static final String ONLINE_PLAYERS_KEY = "players";
    private static final String PLAYER_DATA_KEY = "playerData";
    private static final String SERVER_ID_KEY = "serverIDs";

    private static synchronized ClientResources getClientResources() {
        if (sharedResources == null) {
            sharedResources = DefaultClientResources.builder()
                    .build();
        }
        return sharedResources;
    }

    public RedisConnection(Plugin plugin, String name) {
        super(plugin, name);

    }

    @Override
    protected ConfigurationSection readConfig() {
        ConfigurationSection configuration = super.readConfig();
        if (configuration == null)
            return null;
        host = configuration.isSet( "host") ? configuration.getString( "host") : "localhost";
        port = configuration.isSet( "port") ? configuration.getInt( "port") : 3306;
        databaseNumber = configuration.isSet( "database-number") ? configuration.getInt( "database-number") : -1;
        username = configuration.isSet( "user") ? configuration.getString( "user") : "user";
        password = configuration.isSet( "password") ? configuration.getString( "password") : "";
        ssl = configuration.isSet( "use-ssl") && configuration.getBoolean( "use-ssl");
        return configuration;
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws IOException {
        Map<UUID, String> players = new HashMap<>();
        if (isConnected()) {
            try {
                getData().hgetall(ONLINE_PLAYERS_KEY).forEach((key, value) -> players.put(UUID.fromString(key), value.substring(value.indexOf(":") + 1)));
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    return getOnlinePlayers();
            }
            return players;
        }
        throw notConnectedException;
    }

    private RedisCommands<String, String> getData() throws RedisConnectionException {
        if (!isConnected())
            throw new RedisConnectionException("Redis connection is not open");
        if (data == null) {
            data = connection.sync();
        }
        return data;

    }

    private RedisURI constructURI() {
        RedisURI uri = new RedisURI();

        uri.setSsl(ssl);
        uri.setHost(host);
        if (port != -1)
            uri.setPort(port);
        if (databaseNumber != -1)
            uri.setDatabase(databaseNumber);
        if (password != null && username != null && !password.isEmpty() && !username.isEmpty()) {
            uri.setCredentialsProvider(new StaticCredentialsProvider(username, password.toCharArray()));
        }
        return uri;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(host, port, databaseNumber, username, password, ssl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RedisConnection that = (RedisConnection) o;
        return port == that.port && databaseNumber == that.databaseNumber && ssl == that.ssl
                && Objects.equals(redis, that.redis) && Objects.equals(connection, that.connection)
                && Objects.equals(data, that.data) && Objects.equals(username, that.username)
                && Objects.equals(password, that.password) && Objects.equals(host, that.host);
    }

    /**
     * Connects or reconnects to the database.
     */
    public synchronized boolean connect(boolean syncData) {
        if (isConnected())
            disconnect();
        redis = RedisClient.create(getClientResources(), constructURI());
        return makeConnection(syncData);
    }

    public synchronized void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } finally {
                connection = null;
            }
        }
        if (redis != null) {
            try {
                redis.shutdown();
            } finally {
                redis = null;
                data = null;
            }
        }

    }

    private boolean makeConnection(boolean syncData) {
        if (redis == null)
            return false;
        try {
            connection = redis.connect();
            data = connection.sync();
            if (syncData) {
                DataManager.databaseConnect(this);
            }
            hasConnected = true;
            return true;
        } catch (RedisConnectionException e) {
            // can't connect
            // could be that the default values weren't changed
            return false;
        }
    }

    @Override
    public boolean hasServerData() {
        if (isConnected()) {
            try {
                String ids = getData().get(SERVER_ID_KEY);
                if (ids == null)
                    return false;
                return ids.contains(DataManager.getDatabaseServerID(false).toString());
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    return hasServerData();
            }
        }
        return true;
    }

    @Override
    public void addServerData() {
        if (isConnected()) {
            String ids = getServerIDs();
            if (!ids.contains(DataManager.getDatabaseServerID(false).toString())) {
                getData().set(SERVER_ID_KEY, ids + "," + DataManager.getDatabaseServerID(false).toString());
            }
        }
    }

    private String getServerIDs() {
        try {
            String ids = getData().get(SERVER_ID_KEY);
            if (ids == null)
                ids = "";
            return ids;
        } catch (RedisConnectionException e) {
            if (reconnect(e))
                return getServerIDs();
            return "";
        }
    }

    @Override
    public boolean isEmpty() {
        if (isConnected()) {
            return getServerIDs().isEmpty();
        }
        return false;
    }

    @Override
    public boolean isGlobal() {
        if (isConnected()) {
            return getServerIDs().contains(DataManager.GLOBAL_SERVER_ID.toString());
        }
        return true;
    }

    @Override
    public void setGlobal() {
        if (isConnected()) {
            String ids = getServerIDs();
            if (!ids.contains(DataManager.GLOBAL_SERVER_ID.toString())) {
                getData().set(SERVER_ID_KEY, ids + "," + DataManager.GLOBAL_SERVER_ID);
            }
        }
    }

    public boolean isConnected() {
        return (connection != null && connection.isOpen());
    }

    @Override
    public void shutdown() {
        try {
            disconnect();
        } finally {
            if (sharedResources != null) {
                try {
                    sharedResources.shutdown();
                } finally {
                    sharedResources = null;
                }
            }

        }

    }

    @Override
    public void notifyBounty(UUID uuid) {
        if (isConnected()) {
            try {
                Bounty bounty = getBounty(uuid);
                if (bounty != null) {
                    bounty.notifyBounty();
                    replaceBounty(uuid, bounty);
                }
            } catch (IOException e) {
                // database not connected
            }
        }
    }

    @Override
    public void login(UUID uuid, String playerName) {
        if (isConnected()) {
            try {
                getData().hset(ONLINE_PLAYERS_KEY, uuid.toString(), DataManager.getDatabaseServerID(false).toString() + ":" + playerName);
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    login(uuid, playerName);
            }
        }
    }

    @Override
    public void logout(UUID uuid) {
        if (isConnected()) {
            try {
                String currentServerID = getData().hget(ONLINE_PLAYERS_KEY, uuid.toString());
                if (currentServerID != null && currentServerID.substring(0, currentServerID.indexOf(":")).equals(DataManager.getDatabaseServerID(false).toString())) {
                    getData().hdel(ONLINE_PLAYERS_KEY, uuid.toString());
                }
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    logout(uuid);
            }
        }
    }


    /**
     * Add stats of a player to the redis database
     * @param uuid UUID of the player
     * @param stats The stats to add
     */
    public void addStats(UUID uuid, PlayerStat stats) {
        try {
            PlayerStat previousStats = getStats(uuid);
            getData().hset(STATS_KEY, uuid.toString(), previousStats.combineStats(stats).toJson().toString());
        } catch (IOException e) {
            // database not connected
        } catch (RedisConnectionException e) {
            if (reconnect(e))
                addStats(uuid, stats);
        }
    }

    /**
     * Get stats of a player
     * @param uuid UUID of the player
     * @return A 6 element array of the player's recorded stats
     */
    public @NotNull PlayerStat getStats(UUID uuid) throws IOException {
        if (isConnected()) {
            try {
                String jsonResult = getData().hget(STATS_KEY, uuid.toString());
                if (jsonResult != null)
                    return new PlayerStat(jsonResult);
                return new PlayerStat(0, 0, 0, 0, 0, 0, DataManager.GLOBAL_SERVER_ID);
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    return getStats(uuid);
            }
        }
        throw notConnectedException;
    }

    /**
     * Get all the stats in the redis database
     * @return All recorded player stats
     */
    public Map<UUID, PlayerStat> getAllStats() throws IOException {
        if (!isConnected())
            throw notConnectedException;
        Map<UUID, PlayerStat> stats = new HashMap<>();
        try {
            for (Map.Entry<String, String> entry : getData().hgetall(STATS_KEY).entrySet()) {
                stats.put(UUID.fromString(entry.getKey()), new PlayerStat(entry.getValue()));
            }
        } catch (RedisConnectionException e) {
            if (reconnect(e))
                return getAllStats();
        }
        return stats;
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        if (isConnected() && !playerStats.isEmpty()) {
            for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet()) {
                addStats(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void addBounty(List<Bounty> bounties) {
        try {
            for (Bounty bounty : bounties)
                addBounty(bounty);
        } catch (IOException e) {
            // database not connected
        }
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {
        for (Bounty bounty : bounties)
            removeBounty(bounty);
    }

    /**
     * Add a bounty to the redis database
     *
     * @param bounty Bounty to be added
     * @return The bounty after it was added to the existing bounty of the player.
     */
    public Bounty addBounty(@NotNull Bounty bounty) throws IOException {
        if (!isConnected()) {
            throw notConnectedException;
        }
        Bounty prevBounty = getBounty(bounty.getUUID());
        if (prevBounty != null) {
            prevBounty.getSetters().addAll(bounty.getSetters());
        } else {
            prevBounty = bounty;
        }
        try {
            getData().hset(BOUNTIES_KEY, bounty.getUUID().toString(), prevBounty.toJson().toString());
        } catch (RedisConnectionException e) {
            if (reconnect(e))
                getData().hset(BOUNTIES_KEY, bounty.getUUID().toString(), prevBounty.toJson().toString());
        }
        return prevBounty;
    }

    /**
     * Replaces a bounty in the redis database
     * @param uuid   UUID of the bounty to be replaced
     * @param bounty Replacement bounty. A null value will remove the bounty.
     */
    @Override
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        if (isConnected()) {
            try {
                if (bounty != null)
                    getData().hset(BOUNTIES_KEY, uuid.toString(), bounty.toJson().toString());
                else
                    removeBounty(uuid);
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    replaceBounty(uuid, bounty);
            }
        }
    }

    public @Nullable Bounty getBounty(UUID uuid) throws IOException {
        if (isConnected()) {
            try {
                String jsonBounty = getData().hget(BOUNTIES_KEY, uuid.toString());
                if (jsonBounty != null) {
                    return new Bounty(jsonBounty);
                }
                return null;
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    return getBounty(uuid);
            }
        }
        throw notConnectedException;
    }

    /**
     * Remove bounty from the redis database
     * @param bounty Bounty to be removed
     */
    public void removeBounty(Bounty bounty) {
        if (isConnected()) {
            try {
                Bounty currentBounty = getBounty(bounty.getUUID());
                if (currentBounty == null)
                    // bounty doesn't exist
                    return;
                // delete if uuid and time created match in any setter from the two bounties
                currentBounty.getSetters().removeIf(setter -> bounty.getSetters().stream().anyMatch(toDeleteSetter -> setter.getUuid().equals(toDeleteSetter.getUuid()) && setter.getTimeCreated() == toDeleteSetter.getTimeCreated()));
                if (currentBounty.getSetters().isEmpty()) {
                    // bounty should be deleted
                    removeBounty(bounty.getUUID());
                } else {
                    // replace bounty
                    getData().hset(BOUNTIES_KEY, bounty.getUUID().toString(), currentBounty.toJson().toString());
                }
            } catch (IOException e) {
                // database not connected
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    removeBounty(bounty);
            }
        }
    }

    /**
     * Remove a player's bounty from the database
     * @param uuid UUID of the player
     */
    public void removeBounty(UUID uuid) {
        if (!isConnected())
            return;
        try {
            getData().hdel(BOUNTIES_KEY, uuid.toString());
        } catch (RedisConnectionException e) {
            if (reconnect(e))
                removeBounty(uuid);
        }
    }

    /**
     * Get all the bounties in the redis database
     * @param sortType How the returned list should be sorted
     * @return A list of all the bounties in the redis database
     */
    public List<Bounty> getAllBounties(int sortType) throws IOException{
        if (!isConnected())
            throw notConnectedException;
        List<Bounty> bounties = new ArrayList<>();
        try {
            for (Map.Entry<String, String> entry : getData().hgetall(BOUNTIES_KEY).entrySet()) {
                bounties.add(new Bounty(entry.getValue()));
            }

            if (sortType == -1)
                return bounties;
            if (sortType == 2) {
                bounties.sort(Comparator.reverseOrder());
                return bounties;
            }
            if (sortType == 3) {
                Collections.sort(bounties);
                return bounties;
            }
            // sort in other ways
            Bounty temp;
            for (int i = 0; i < bounties.size(); i++) {
                for (int j = i + 1; j < bounties.size(); j++) {
                    if (!bounties.get(i).getSetters().isEmpty()
                            && ((bounties.get(i).getSetters().get(0).getTimeCreated() > bounties.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                            (bounties.get(i).getSetters().get(0).getTimeCreated() < bounties.get(j).getSetters().get(0).getTimeCreated() && sortType == 1))) { // newest bounties at top
                        temp = bounties.get(i);
                        bounties.set(i, bounties.get(j));
                        bounties.set(j, temp);
                    }
                }
            }
        } catch (RedisConnectionException e) {
            if (reconnect(e))
                return getAllBounties(sortType);
        }

        return bounties;
    }

    @Override
    public void updatePlayerData(PlayerData playerData) {
        if (isConnected()) {
            try {
                getData().hset(PLAYER_DATA_KEY, playerData.getUuid().toString(), playerData.toJson().toString());
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    updatePlayerData(playerData);
            }
        }
    }

    @Override
    public PlayerData getPlayerData(@NotNull UUID uuid) throws IOException {
        if (isConnected()) {
            try {
                String jsonResponse = getData().hget(PLAYER_DATA_KEY, uuid.toString());
                if (jsonResponse != null) {
                    return PlayerData.fromJson(jsonResponse);
                } else {
                    return null;
                }
            } catch (RedisConnectionException e) {
                if (reconnect(e))
                    return getPlayerData(uuid);
            }
        }
        throw notConnectedException;
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataMap) {
        if (isConnected() && !playerDataMap.isEmpty()) {
            for (PlayerData playerData : playerDataMap) {
                updatePlayerData(playerData);
            }
        }
    }

    @Override
    public List<PlayerData> getPlayerData() throws IOException {
        if (!isConnected())
            throw notConnectedException;
        try {
            return getData().hgetall(PLAYER_DATA_KEY).values().stream().map(PlayerData::fromJson).sorted().toList();
        } catch (RedisConnectionException e) {
            if (reconnect(e))
                return getPlayerData();
            throw notConnectedException;
        }
    }

    @Override
    public boolean isPermDatabase() {
        return false;
    }
}

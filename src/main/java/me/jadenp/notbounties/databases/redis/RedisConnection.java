package me.jadenp.notbounties.databases.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StaticCredentialsProvider;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.databases.NotBountiesDatabase;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.PlayerStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * Data is stored in Redis which is basically a string hashmap
 */
public class RedisConnection implements NotBountiesDatabase {
    private RedisClient redis = null;
    private StatefulRedisConnection<String, String> connection = null;
    private RedisCommands<String, String> data = null;
    private long failedConnectionTimeout = 0;
    private final Plugin plugin;
    private boolean connectedBefore = false;
    private int refreshInterval = 300;
    private long lastSync = 0;

    private final String name;
    private String username = "username";
    private String password = "pass";
    private String host = "localhost";
    private int port = 3306;
    private int databaseNumber = -1;
    private boolean ssl = false;
    private int priority;

    private static final String BOUNTIES_KEY = "bounties";
    private static final String STATS_KEY = "stats";
    private static final String SERVER_IDS_KEY = "servers";
    private static final String ONLINE_PLAYERS_KEY = "players";

    public RedisConnection(Plugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;

        readConfig();

        redis = RedisClient.create(constructURI());
    }

    private void readConfig() {
        ConfigurationSection configuration = plugin.getConfig().getConfigurationSection("databases." + name);
        if (configuration == null)
            return;
        host = configuration.isSet( "host") ? configuration.getString( "host") : "localhost";
        port = configuration.isSet( "port") ? configuration.getInt( "port") : 3306;
        databaseNumber = configuration.isSet( "database-number") ? configuration.getInt( "database-number") : -1;
        username = configuration.isSet( "user") ? configuration.getString( "user") : "user";
        password = configuration.isSet( "password") ? configuration.getString( "password") : "";
        ssl = configuration.isSet( "use-ssl") && configuration.getBoolean( "use-ssl");
        refreshInterval = configuration.isSet("refresh-interval") ? configuration.getInt("refresh-interval") : 300;
        priority = configuration.isSet("priority") ? configuration.getInt("priority") : 0;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public long getLastSync() {
        return lastSync;
    }

    @Override
    public void setLastSync(long lastSync) {
        this.lastSync = lastSync;
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws IOException {
        Map<UUID, String> players = new HashMap<>();
        if (getData() != null) {
            getData().hgetall(ONLINE_PLAYERS_KEY).forEach((key, value) -> players.put(UUID.fromString(key), value.substring(value.indexOf(":") + 1)));
            return players;
        }
        throw new IOException("Database is not connected!");
    }

    @Override
    public int getPriority() {
        return priority;
    }

    private RedisCommands<String, String> getData() {
        if (data == null && isConnected()) {
            data = connection.sync();
            return data;
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

    private long getConfigHash() {
        return (long) host.hashCode() + port + databaseNumber + username.hashCode() + password.hashCode() + (ssl ? 1 : 0) + refreshInterval + priority;
    }

    @Override
    public void reloadConfig() {
        long oldHash = getConfigHash();
        readConfig();
        if (oldHash != getConfigHash())
            connect();
    }

    /**
     * Connects or reconnects to the database.
     */
    public boolean connect() {
        if (connection != null)
            connection.closeAsync();
        if (redis != null)
            redis.shutdownAsync();
        redis = RedisClient.create(constructURI());
        return makeConnection();
    }

    public void disconnect() {
        if (getData() != null) {
            connection.close();
            data = null;
        }
    }

    private boolean makeConnection() {
        if (redis == null)
            return false;
        try {
            connection = redis.connect();
            if (data == null)
                data = connection.sync();
            DataManager.databaseConnect(this);
            connectedBefore = true;
            return true;
        } catch (RedisConnectionException e) {
            // can't connect
            // could be that the default values weren't changed
            return false;
        }
    }

    public boolean isConnected() {
        if (System.currentTimeMillis() - failedConnectionTimeout < 5000L)
            return false;
        if (connection == null || !connection.isOpen()) {
            failedConnectionTimeout = System.currentTimeMillis();
            if (hasConnectedBefore() && NotBounties.getInstance().isEnabled())
                connect();
            return false;
        }
        return true;
    }

    /**
     * Check if the database has ever been connected with this plugin instance.
     * The database doesn't need to be currently connected to return true.
     * @return True if the redis database has been connected.
     */
    public boolean hasConnectedBefore() {
        return connectedBefore;
    }

    public void shutdown() {
        if (connection != null)
            connection.close();
        if (redis != null)
            redis.shutdown();
    }

    @Override
    public void notifyBounty(UUID uuid) {
        if (getData() != null) {
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
        if (getData() != null) {
            getData().hset(ONLINE_PLAYERS_KEY, uuid.toString(), DataManager.getDatabaseServerID(false).toString() + ":" + playerName);
        }
    }

    @Override
    public void logout(UUID uuid) {
        if (getData() != null) {
            String currentServerID = getData().hget(ONLINE_PLAYERS_KEY, uuid.toString());
            if (currentServerID != null && currentServerID.substring(0, currentServerID.indexOf(":")).equals(DataManager.getDatabaseServerID(false).toString())) {
                getData().hdel(ONLINE_PLAYERS_KEY, uuid.toString());
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
        }
    }

    /**
     * Get stats of a player
     * @param uuid UUID of the player
     * @return A 6 element array of the player's recorded stats
     */
    public @NotNull PlayerStat getStats(UUID uuid) throws IOException {
        if (getData() != null) {
            String jsonResult = getData().hget(STATS_KEY, uuid.toString());
            if (jsonResult != null)
                return new PlayerStat(jsonResult);
            return new PlayerStat(0,0,0,0,0,0, DataManager.GLOBAL_SERVER_ID);
        }
        throw new IOException("Database is not connected!");
    }

    /**
     * Get all the stats in the redis database
     * @return All recorded player stats
     */
    public Map<UUID, PlayerStat> getAllStats() throws IOException {
        if (getData() == null)
            throw new IOException("Database is not connected!");
        Map<UUID, PlayerStat> stats = new HashMap<>();
        for (Map.Entry<String, String> entry : getData().hgetall(STATS_KEY).entrySet()) {
            stats.put(UUID.fromString(entry.getKey()), new PlayerStat(entry.getValue()));
        }
        return stats;
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        if (getData() != null) {
            try {
                for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet()) {
                    PlayerStat previousStats = getStats(entry.getKey());
                    getData().hset(STATS_KEY, entry.getKey().toString(), previousStats.combineStats(entry.getValue()).toJson().toString());
                }
            } catch (IOException ignored) {
                // database isn't connected
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
        if (getData() == null) {
            throw new IOException("Database is not connected!");
        }
        Bounty prevBounty = getBounty(bounty.getUUID());
        if (prevBounty != null) {
            prevBounty.getSetters().addAll(bounty.getSetters());
        } else {
            prevBounty = bounty;
        }
        getData().hset(BOUNTIES_KEY, bounty.getUUID().toString(), prevBounty.toJson().toString());
        return prevBounty;
    }

    /**
     * Replaces a bounty in the redis database
     * @param uuid   UUID of the bounty to be replaced
     * @param bounty Replacement bounty. A null value will remove the bounty.
     */
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        if (getData() != null) {
            if (bounty != null)
                getData().hset(BOUNTIES_KEY, uuid.toString(), bounty.toJson().toString());
            else
                removeBounty(uuid);
        }
    }

    public @Nullable Bounty getBounty(UUID uuid) throws IOException {
        if (getData() != null) {
            String jsonBounty = getData().hget(BOUNTIES_KEY, uuid.toString());
            if (jsonBounty != null) {
                return new Bounty(jsonBounty);
            }
            return null;
        }
        throw new IOException("Database is not connected!");
    }

    /**
     * Remove bounty from the redis database
     * @param bounty Bounty to be removed
     */
    public void removeBounty(Bounty bounty) {
        if (getData() != null) {
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
            }
        }
    }

    /**
     * Remove a player's bounty from the database
     * @param uuid UUID of the player
     */
    public void removeBounty(UUID uuid) {
        if (getData() == null)
            return;
        getData().hdel(BOUNTIES_KEY, uuid.toString());
    }

    /**
     * Get all the bounties in the redis database
     * @param sortType How the returned list should be sorted
     * @return A list of all the bounties in the redis database
     */
    public List<Bounty> getAllBounties(int sortType) throws IOException{
        if (getData() == null)
            throw new IOException("Database is not connected!");
        List<Bounty> bounties = new ArrayList<>();
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
                if ((bounties.get(i).getSetters().get(0).getTimeCreated() > bounties.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                        (bounties.get(i).getSetters().get(0).getTimeCreated() < bounties.get(j).getSetters().get(0).getTimeCreated() && sortType == 1)) { // newest bounties at top
                    temp = bounties.get(i);
                    bounties.set(i, bounties.get(j));
                    bounties.set(j, temp);
                }
            }
        }

        return bounties;
    }


    @Override
    public String getName() {
        return name;
    }
}

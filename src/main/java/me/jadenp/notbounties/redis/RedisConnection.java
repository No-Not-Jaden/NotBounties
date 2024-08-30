package me.jadenp.notbounties.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StaticCredentialsProvider;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class RedisConnection {
    private RedisClient redis = null;
    private StatefulRedisConnection<String, String> connection = null;
    private RedisCommands<String, String> data = null;
    private long failedConnectionTimeout = 0;
    private final Plugin plugin;

    private String username = "username";
    private String password = "pass";
    private String host = "localhost";
    private int port = 3306;
    private int databaseNumber = -1;
    private boolean ssl = false;
    private boolean autoConnect = false;

    public RedisConnection(Plugin plugin) {
        this.plugin = plugin;
        ConfigurationSection configuration = plugin.getConfig().getConfigurationSection("redis");
        if (configuration == null)
            return;

        this.username = configuration.getString("user");
        this.password = configuration.getString("password");
        this.host = configuration.getString("host");
        this.port = configuration.getInt("port");
        this.databaseNumber = configuration.getInt("database-number");
        this.ssl = configuration.getBoolean("ssl");
        this.autoConnect = configuration.getBoolean("auto-connect");

        redis = RedisClient.create(constructURI());

        connect(true);
    }

    public RedisCommands<String, String> getData() {
        if (data == null && isConnected())
            data = connection.sync();
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

    /**
     * Loads configuration data and connects to Redis.
     * If the configuration data doesn't change and redis is already connected, nothing will happen.
     */
    public void reconnect() {
        ConfigurationSection configuration = plugin.getConfig().getConfigurationSection("redis");
        if (configuration == null)
            return;
        // get new values
        String newUsername = configuration.getString("user");
        String newPassword = configuration.getString("password");
        String newHost = configuration.getString("host");
        int newPort = configuration.getInt("port");
        int newDatabaseNumber = configuration.getInt("database-number");
        boolean newSsl = configuration.getBoolean("ssl");
        autoConnect = configuration.getBoolean("auto-connect");
        // compare values
        if (!username.equals(newUsername) || !password.equals(newPassword) || !host.equals(newHost) || port != newPort || databaseNumber != newDatabaseNumber || newSsl != ssl) {
            username = newUsername;
            password = newPassword;
            host = newHost;
            port = newPort;
            databaseNumber = newDatabaseNumber;
            ssl = newSsl;
            if (connection != null)
                connection.closeAsync();
            if (redis != null)
                redis.shutdownAsync();
            redis = RedisClient.create(constructURI());
            connect(false);
        }
    }

    private void connect(boolean sync) {
        if (redis == null)
            return;
        if (sync) {
            try {
                connection = redis.connect();
            } catch (RedisConnectionException e) {
                // can't connect
                // could be that the default values weren't changed
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    connection = redis.connect();
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    public boolean isConnected() {
        if (System.currentTimeMillis() - failedConnectionTimeout < 5000L)
            return false;
        if (connection == null || !connection.isOpen()) {
            failedConnectionTimeout = System.currentTimeMillis();
            if (autoConnect)
                connect(false);
            return false;
        }
        return true;
    }

    public void shutdown() {
        if (connection != null)
            connection.close();
        if (redis != null)
            redis.shutdown();
    }
}

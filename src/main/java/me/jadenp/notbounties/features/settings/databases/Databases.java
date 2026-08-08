package me.jadenp.notbounties.features.settings.databases;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxySettings;
import me.jadenp.notbounties.features.settings.databases.sql.SQLDatabase;
import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import me.jadenp.notbounties.features.settings.databases.wrappers.AsyncDatabaseWrapper;
import me.jadenp.notbounties.features.settings.databases.wrappers.FallbackWrapper;
import me.jadenp.notbounties.features.settings.databases.wrappers.NotBountiesDatabase;
import me.jadenp.notbounties.features.settings.databases.wrappers.ReconnectWrapper;
import me.jadenp.notbounties.utils.SaveManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class Databases extends ResourceConfiguration {

    private static UUID databaseServerID = null;
    private final List<AsyncDatabaseWrapper> configuredDatabases = new ArrayList<>();
    private TaskImplementation<Void> autoReconnectTask = null;

    private void setAutoReconnectTask(long autoConnectInterval) {
        if (autoConnectInterval > 0) {
            autoReconnectTask = NotBounties.getServerImplementation().async().runAtFixedRate(this::tryDatabaseConnections, 120, autoConnectInterval * 20);
        }
    }

    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        long maxLocalCacheSize;
        long refreshInterval;
        if (autoReconnectTask != null)
            autoReconnectTask.cancel();
        long autoConnectInterval = config.getLong("auto-connect-interval", 0);
        setAutoReconnectTask(autoConnectInterval);

        maxLocalCacheSize = Math.max(config.getLong("max-local-cache-size", 200), 10);
        refreshInterval = config.getLong("refresh-interval", 0);
        if (configuredDatabases.isEmpty())
            configuredDatabases.add(new AsyncDatabaseWrapper(new FallbackWrapper(new LocalData(plugin, maxLocalCacheSize, refreshInterval))));

        for (String databaseName : config.getKeys(false)) {
            if (!config.isConfigurationSection(databaseName))
                continue;
            boolean newDatabase = true;
            for (AsyncDatabaseWrapper database : configuredDatabases) {
                if (database.getConfigurationName().equals(databaseName)) {
                    database.reloadConfig();
                    newDatabase = false;
                    break;
                }
            }
            if (newDatabase) {
                try {
                    loadNewDatabaseConfig(config, databaseName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(e.toString());
                }
            }
        }
        if (configuredDatabases.isEmpty())
            return;
        Collections.sort(configuredDatabases); // local data will be first in the list
        // set fallback order
        for (int i = 0; i < configuredDatabases.size(); i++) {
            FallbackWrapper fallbackWrapper = configuredDatabases.get(i).getDatabase(FallbackWrapper.class);
            if (fallbackWrapper != null) {
                if (i == configuredDatabases.size() - 1) {
                    // the lowest priority database does not have a fallback
                    fallbackWrapper.setFallbackDB(null);
                } else {
                    fallbackWrapper.setFallbackDB(configuredDatabases.get(i+1));
                }
            }

        }

        tryDatabaseConnections();
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[]{""};
    } // means all sections

    @Override
    protected String getPath() {
        return "settings/databases.yml";
    }

    /**
     * Load a new database config into the system.
     * @param configuration Database configuration. The name of the database should be at the top of this
     *                      configuration section.
     * @param databaseName Name of the database.
     * @throws IllegalArgumentException If the database type specified is not defined.
     */
    private void loadNewDatabaseConfig(ConfigurationSection configuration, String databaseName) throws IllegalArgumentException {
        String type = configuration.getString(databaseName + ".type","unknown");
        NotBountiesDatabase database;
        try {
            database = new SQLDatabase(NotBounties.getInstance(), databaseName);
            if (type.equalsIgnoreCase("PROXY")) {
                ProxySettings.readConfig(configuration);
            }
            AsyncDatabaseWrapper asyncDatabaseWrapper = new AsyncDatabaseWrapper(new FallbackWrapper(new ReconnectWrapper(database)));
            SaveManager.loadSyncTime(asyncDatabaseWrapper);
            asyncDatabaseWrapper.reloadConfig();
            configuredDatabases.add(asyncDatabaseWrapper);
        } catch (NoClassDefFoundError e) {
            // Couldn't load a dependency.
            // This will be thrown if unable to use Spigot's library loader
            NotBounties.debugMessage("One or more dependencies could not be downloaded to use the database: " + databaseName + " (" + type + ")", true);
        }
    }

    /**
     * Get the server ID for this server.
     * @return The server ID for this server.
     */
    public static UUID getDatabaseServerID() {
        if (databaseServerID == null) {
            NotBounties.getInstance().getLogger().info("Generating new database ID.");
            databaseServerID = UUID.randomUUID();
        }
        return databaseServerID;
    }

    public static void setDatabaseServerID(UUID databaseServerID) {
        Databases.databaseServerID = databaseServerID;
    }

    private void tryDatabaseConnections() {
        for (NotBountiesDatabase database : configuredDatabases) {
            if (!database.isConnected())
                database.connect(true);
        }
    }

    /**
     * Get the configured databases for NotBounties sorted by priority in descending order.
     * @return All configured databases.
     */
    public List<AsyncDatabaseWrapper> getConfiguredDatabases() {
        return configuredDatabases;
    }
}

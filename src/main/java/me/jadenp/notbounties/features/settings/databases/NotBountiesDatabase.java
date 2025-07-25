package me.jadenp.notbounties.features.settings.databases;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.features.ConfigOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Databases will be loaded first with the call of the constructor. Configuration should be loaded here
 * Later, databases will be called to connect.
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public abstract class NotBountiesDatabase implements Comparable<NotBountiesDatabase>{
    private final String name;
    private final Plugin plugin;
    private int priority = 0;
    private int refreshInterval = 0;
    private long lastSync = 0;
    protected boolean hasConnected = false;
    private long nextReconnectAttempt;
    private int reconnectAttempts;

    protected static final IOException notConnectedException = new IOException("Database is not connected!");

    protected NotBountiesDatabase(Plugin plugin, String name) {
        this.name = name;
        this.plugin = plugin;

        nextReconnectAttempt = System.currentTimeMillis();
        reconnectAttempts = 0;

        readConfig();
    }

    protected NotBountiesDatabase() {
        this.name = "Pseudo";
        this.plugin = null;

        nextReconnectAttempt = System.currentTimeMillis();
        reconnectAttempts = 0;
    }

    /**
     * Add stats of a player to the database
     * @param uuid UUID of the player
     * @param stats The stats to add
     */
    public abstract void addStats(UUID uuid, PlayerStat stats);

    /**
     * Get stats of a player.
     * @param uuid UUID of the player
     * @return The stats of the player
     * @throws IOException When the database isn't connected.
     */
    public abstract @NotNull PlayerStat getStats(UUID uuid) throws IOException;

    /**
     * Get all the stats in the database
     * @return All recorded player stats
     * @throws IOException When the database isn't connected.
     */
    public abstract Map<UUID, PlayerStat> getAllStats() throws IOException;

    /**
     * Adds multiple stats to the database.
     * @apiNote This is used to synchronize multiple databases
     * @param playerStats Stats to be added to the database
     */
    public abstract void addStats(Map<UUID, PlayerStat> playerStats);

    /**
     * Adds multiple bounties to the database
     * @apiNote This is used to synchronize multiple databases.
     * @param bounties Bounties to be added to the database.
     */
    public abstract void addBounty(List<Bounty> bounties);

    /**
     * Removes multiple bounties from the database.
     * @apiNote This is used to synchronize multiple databases.
     * @param bounties Bounties to be removed.
     */
    public abstract void removeBounty(List<Bounty> bounties);

    /**
     * Add a bounty to the database
     * @param bounty Bounty to be added
     * @return A bounty that is the combination of all the bounties on the same person which includes the supplied bounty.
     * @throws IOException When the database isn't connected.
     */
    public abstract Bounty addBounty(@NotNull Bounty bounty) throws IOException;

    /**
     * Replaces a bounty in the database
     * @param uuid   UUID of the bounty to be replaced
     * @param bounty Replacement bounty. A null value will remove the bounty.
     */
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        try {
            removeBounty(uuid);
            if (bounty != null)
                addBounty(bounty);
        } catch (IOException ignored) {
            // couldn't replace bounty because database isn't connected
        }
    }

    /**
     * Get a bounty from the database.
     * @param uuid UUID of the player the bounty is set on.
     * @return A stored bounty, or null if no bounty exists.
     * @throws IOException When the database isn't connected.
     */
    public abstract @Nullable Bounty getBounty(UUID uuid) throws IOException;

    /**
     * Remove a player's bounty from the database.
     * @param uuid UUID of the player.
     */
    public abstract void removeBounty(UUID uuid);

    /**
     * Remove a specific bounty from the database.
     * @param bounty Bounty to be removed.
     */
    public abstract void removeBounty(Bounty bounty);

    /**
     * Get all the bounties in the database
     * @param sortType How the returned list should be sorted
     *                 <p>-1  = Not sorted</p>
     *                 <p> 0  = Oldest bounties first</p>
     *                 <p> 1  = Newest bounties first</p>
     *                 <p> 2  = Most expensive bounties first</p>
     *                 <p> 3  = Least expensive bounties first</p>
     * @return A list of all the bounties in the redis database
     * @throws IOException When the database isn't connected.
     */
    public abstract List<Bounty> getAllBounties(int sortType) throws IOException;

    /**
     * Get a configurable name for this database.
     * @return A user-inputted name.
     */
    public String getName() {
        return name;
    }

    /**
     * Check if this database is currently connected.
     * This should be used before accessing any of the data in the database.
     * @return True if the database is connected to the plugin.
     */
    public abstract boolean isConnected();

    /**
     * Attempts to reconnect to the database.
     * @return True if the connection was successful.
     */
    public abstract boolean connect(boolean syncData);

    /**
     * Disconnects the server from the database.
     */
    public abstract void disconnect();

    /**
     * Check if the database has ever been connected with this plugin instance.
     * The database doesn't need to be currently connected to return true.
     * @return True if the redis database has been connected before.
     */
    public boolean hasConnectedBefore() {
        return hasConnected;
    }

    /**
     * Get the refresh interval of the database.
     * This is how many seconds between updating locally stored data with cloud data.
     * @return The database refresh interval in seconds.
     */
    public int getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * Get the last time that data was read from this database.
     * @return A time in milliseconds.
     */
    public long getLastSync() {
        return lastSync;
    }

    /**
     * Set the last time the database was synced.
     * This will be used at startup to write the time from a local file.
     * @param lastSync The time in milliseconds when the server read information from this database.
     */
    public void setLastSync(long lastSync) {
        this.lastSync = lastSync;
    }

    /**
     * Get the players on servers connected to the database.
     * @return A Map of player's uuids and usernames.
     * @throws IOException When the database isn't connected.
     */
    public abstract Map<UUID, String> getOnlinePlayers() throws IOException;

    /**
     * Update the player data for a player in the database.
     * @param playerData New player data information.
     */
    public abstract void updatePlayerData(PlayerData playerData);

    /**
     * Get the player data for a player.
     * @param uuid UUID of the player.
     * @return The player data of the player.
     * @throws IOException When the database isn't connected.
     */
    public abstract PlayerData getPlayerData(@NotNull UUID uuid) throws IOException;

    /**
     * Add player data to the database. Existing player data with the same UUID will be overwritten.
     * @param playerDataMap Map of player data to add.
     */
    public abstract void addPlayerData(List<PlayerData> playerDataMap);

    /**
     * Get the player data in the database.
     * @return The player data in the database, sorted by UUID in ascending order.
     * @throws IOException When the database isn't connected.
     */
    public abstract List<PlayerData> getPlayerData() throws IOException;

    /**
     * Get the priority of the database.
     * @return The priority of the database set in the config.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Reload the database config
     * @return True if there was a change in the configuration.
     */
    public boolean reloadConfig() {
        long oldHash = hashCode();
        readConfig();
        return oldHash != hashCode();

    }

    /**
     * Read and load the configuration for this database.
     * @return The configuration section that the database read from.
     */
    protected synchronized ConfigurationSection readConfig() {
        YamlConfiguration config;
        try {
            config = ConfigOptions.getDatabases().getConfig();
        } catch (IOException e) {
            // YAML formatting errors
            return null;
        }
        ConfigurationSection configSection = config.getConfigurationSection(name);
        if (configSection == null)
            return null;
        refreshInterval = configSection.getInt("refresh-interval", 0);
        priority = configSection.getInt("priority", 0);
        return configSection;
    }

    /**
     * Shut down the connection to the database.
     * Called when the plugin is disabled.
     */
    public synchronized void shutdown() {
        disconnect();
    }

    /**
     * Record that a player was notified.
     * @param uuid UUID of the player that was notified.
     */
    public abstract void notifyBounty(UUID uuid);

    /**
     * Record that a player logged in.
     * @param uuid UUID of the player that logged in.
     * @param playerName Username of the player that logged in.
     */
    public abstract void login(UUID uuid, String playerName);

    /**
     * Record a player that logged out.
     * @param uuid UUID of the player that logged out.
     */
    public abstract void logout(UUID uuid);

    /**
     * Check if the connected database is reliable and the data will persist.
     * @return True if the database is permanent.
     */
    public abstract boolean isPermDatabase();

    protected synchronized boolean reconnect(Exception e) {
        NotBounties.debugMessage(e.toString(), true);
        Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> NotBounties.debugMessage(stackTraceElement.toString(), true));
        if (System.currentTimeMillis() > nextReconnectAttempt) {
            reconnectAttempts++;
            disconnect();
            if (reconnectAttempts >= 3) {
                reconnectAttempts = 0;
                nextReconnectAttempt = System.currentTimeMillis() + 5000L;
            }
            if (NotBounties.getInstance().isEnabled()) {
                if (!connect(false)) {
                    if (reconnectAttempts < 2)
                        NotBounties.getServerImplementation().async().runDelayed(() -> connect(true), 20L);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(@NotNull NotBountiesDatabase o) {
        return o.getPriority() - getPriority();
    }

    // only hash config options
    @Override
    public int hashCode() {
        return Objects.hash(name, priority, refreshInterval);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NotBountiesDatabase that = (NotBountiesDatabase) o;
        return priority == that.priority && refreshInterval == that.refreshInterval && lastSync == that.lastSync && hasConnected == that.hasConnected && nextReconnectAttempt == that.nextReconnectAttempt && reconnectAttempts == that.reconnectAttempts && Objects.equals(name, that.name) && Objects.equals(plugin, that.plugin);
    }
}

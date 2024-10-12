package me.jadenp.notbounties.databases;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.utils.PlayerStat;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Databases will be loaded first with the call of the constructor. Configuration should be loaded here
 * Later, databases will be called to connect
 */
public interface NotBountiesDatabase extends Comparable<NotBountiesDatabase>{

    /**
     * Add stats of a player to the database
     * @param uuid UUID of the player
     * @param stats The stats to add
     */
    void addStats(UUID uuid, PlayerStat stats);

    /**
     * Get stats of a player.
     * @param uuid UUID of the player
     * @return The stats of the player
     * @throws IOException When the database isn't connected.
     */
    @NotNull PlayerStat getStats(UUID uuid) throws IOException;

    /**
     * Get all the stats in the database
     * @return All recorded player stats
     * @throws IOException When the database isn't connected.
     */
    Map<UUID, PlayerStat> getAllStats() throws IOException;

    /**
     * Adds multiple stats to the database.
     * @apiNote This is used to synchronize multiple databases
     * @param playerStats Stats to be added to the database
     */
    void addStats(Map<UUID, PlayerStat> playerStats);

    /**
     * Adds multiple bounties to the database
     * @apiNote This is used to synchronize multiple databases.
     * @param bounties Bounties to be added to the database.
     */
    void addBounty(List<Bounty> bounties);

    /**
     * Removes multiple bounties from the database.
     * @apiNote This is used to synchronize multiple databases.
     * @param bounties Bounties to be removed.
     */
    void removeBounty(List<Bounty> bounties);

    /**
     * Add a bounty to the database
     * @param bounty Bounty to be added
     * @return A bounty that is the combination of all the bounties on the same person which includes the supplied bounty.
     * @throws IOException When the database isn't connected.
     */
    Bounty addBounty(@NotNull Bounty bounty) throws IOException;

    /**
     * Replaces a bounty in the database
     * @param uuid   UUID of the bounty to be replaced
     * @param bounty Replacement bounty. A null value will remove the bounty.
     */
    default void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
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
    @Nullable Bounty getBounty(UUID uuid) throws IOException;

    /**
     * Remove a player's bounty from the database.
     * @param uuid UUID of the player.
     */
    void removeBounty(UUID uuid);

    /**
     * Remove a specific bounty from the database.
     * @param bounty Bounty to be removed.
     */
    void removeBounty(Bounty bounty);

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
    List<Bounty> getAllBounties(int sortType) throws IOException;

    /**
     * Get a configurable name for this database
     * @return A user-inputted name
     */
    String getName();

    /**
     * Check if this database is currently connected.
     * This should be used before accessing any of the data in the database.
     * @return True if the database is connected to the plugin.
     */
    boolean isConnected();

    /**
     * Attempts to reconnect to the database.
     * @return True if the connection was successful.
     */
    boolean connect();

    /**
     * Disconnects the server from the database.
     */
    void disconnect();

    /**
     * Check if the database has ever been connected with this plugin instance.
     * The database doesn't need to be currently connected to return true.
     * @return True if the redis database has been connected before.
     */
    boolean hasConnectedBefore();

    /**
     * Get the refresh interval of the database.
     * This is how many seconds between updating locally stored data with cloud data.
     * @return The database refresh interval in seconds.
     */
    int getRefreshInterval();

    /**
     * Get the last time that data was read from this database.
     * @return A time in milliseconds.
     */
    long getLastSync();

    /**
     * Set the last time the database was synced.
     * This will be used at startup to write the time from a local file.
     * @param lastSync The time in milliseconds when the server read information from this database.
     */
    void setLastSync(long lastSync);

    /**
     * Get the players on servers connected to the database.
     * @return A Map of player's uuids and usernames.
     * @throws IOException When the database isn't connected.
     */
    Map<UUID, String> getOnlinePlayers() throws IOException;

    /**
     * Get the priority of the database.
     * @return The priority of the database set in the config.
     */
    int getPriority();

    /**
     * Reload the database config
     */
    void reloadConfig();

    /**
     * Shut down the connection to the database
     */
    void shutdown();

    /**
     * Record that a player was notified.
     * @param uuid UUID of the player that was notified.
     */
    void notifyBounty(UUID uuid);

    /**
     * Record that a player logged in.
     * @param uuid UUID of the player that logged in.
     * @param playerName Username of the player that logged in.
     */
    void login(UUID uuid, String playerName);

    /**
     * Record a player that logged out.
     * @param uuid UUID of the player that logged out.
     */
    void logout(UUID uuid);

    @Override
    default int compareTo(@NotNull NotBountiesDatabase o) {
        return o.getPriority() - getPriority();
    }
}

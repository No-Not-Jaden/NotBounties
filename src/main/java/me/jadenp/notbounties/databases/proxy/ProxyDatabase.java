package me.jadenp.notbounties.databases.proxy;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.databases.NotBountiesDatabase;
import me.jadenp.notbounties.utils.BountyChange;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * The proxy database works slightly differently than the others:
 * - A player must be online to receive or send data
 * - When a player joins, the server will receive the database
 * - When data is updated on other servers, it will be immediately sent over to this server
 * Database synchronizations shouldn't need to occur apart from when a player joins.
 * Therefor, getting data here will only return the local data.
 * Another way that this is different is that there can only be 1 proxy connected, so statics can be used.
 */
public class ProxyDatabase extends NotBountiesDatabase {

    private static boolean enabled = false;
    private int numOnlinePlayers;
    private boolean hasConnected = false;
    private static Map<UUID, String> databaseOnlinePlayers = new HashMap<>();
    private static long lastOnlinePlayersCheck = 0;
    private static boolean databaseSynchronization = false;
    private static boolean skins = false;
    private static boolean registeredListener = false;


    public ProxyDatabase(Plugin plugin, String name) {
        super(plugin, name);
    }

    public static void setDatabaseOnlinePlayers(Map<UUID, String> databaseOnlinePlayers) {
        ProxyDatabase.databaseOnlinePlayers = databaseOnlinePlayers;
    }

    private static void requestPlayerList() {
        if (System.currentTimeMillis() - lastOnlinePlayersCheck > 20000) {
            ProxyMessaging.requestPlayerList();
            lastOnlinePlayersCheck = System.currentTimeMillis();
        }
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) {
        ProxyMessaging.sendStatUpdate(Collections.singletonMap(uuid, stats));
    }

    @Override
    public @NotNull PlayerStat getStats(UUID uuid) {
        return DataManager.getLocalData().getStats(uuid);
    }

    @Override
    public Map<UUID, PlayerStat> getAllStats() {
        return DataManager.getLocalData().getAllStats();
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        ProxyMessaging.sendStatUpdate(playerStats);

    }

    @Override
    public void addBounty(List<Bounty> bounties) {
        List<BountyChange> changes = bounties.stream().map(
                bounty -> new BountyChange(BountyChange.ChangeType.ADD_BOUNTY, bounty)).toList();
        ProxyMessaging.sendBountyUpdate(changes);
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {

        List<BountyChange> changes = bounties.stream().map(
                bounty -> new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, bounty)).toList();
        ProxyMessaging.sendBountyUpdate(changes);
    }

    @Override
    public Bounty addBounty(@NotNull Bounty bounty) {

            BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.ADD_BOUNTY, bounty);
        ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
        return bounty;
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) {
        return DataManager.getLocalData().getBounty(uuid);
    }

    @Override
    public void removeBounty(UUID uuid) {
        BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, new Bounty(uuid, Collections.emptyList(), ""));
        ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
    }

    @Override
    public void removeBounty(Bounty bounty) {
            BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, bounty);
        ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
    }

    @Override
    public List<Bounty> getAllBounties(int sortType) {
        return DataManager.getLocalData().getAllBounties(sortType);
    }

    @Override
    public boolean isConnected() {
        return enabled && databaseSynchronization && ProxyMessaging.hasConnectedBefore() && numOnlinePlayers > 0;
    }

    @Override
    public boolean connect() {
        // if this returns false, it will stop the update task
        return ProxyMessaging.hasConnectedBefore();
    }

    @Override
    public void disconnect() {
        // cannot disconnect from the proxy
    }

    @Override
    public boolean hasConnectedBefore() {
        return ProxyMessaging.hasConnectedBefore();
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws IOException {
        requestPlayerList();
        return databaseOnlinePlayers;
    }

    @Override
    protected ConfigurationSection readConfig() {
        ConfigurationSection configuration = super.readConfig();
        if (configuration == null)
            return null;
        setEnabled(configuration.getBoolean("enabled"));
        setDatabaseSynchronization(configuration.getBoolean("database-sync"));
        setSkins(configuration.getBoolean("skins"));
        if (enabled && !registeredListener) {
            // Proxy messaging is enabled, but the listeners haven't been registered.
            // Register plugin message listeners.
            Plugin plugin = NotBounties.getInstance();
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "notbounties:main");
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "notbounties:main", new ProxyMessaging());
            setRegisteredListener(true);
        } else if (!enabled && registeredListener) {
            // Proxy messaging is disabled, but listeners are registered.
            // Unregister message listeners.
            Plugin plugin = NotBounties.getInstance();
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
            setRegisteredListener(false);
        }
        return configuration;
    }

    private static void setEnabled(boolean enabled) {
        ProxyDatabase.enabled = enabled;
    }

    private static void setDatabaseSynchronization(boolean databaseSynchronization) {
        ProxyDatabase.databaseSynchronization = databaseSynchronization;
    }

    private static void setSkins(boolean skins) {
        ProxyDatabase.skins = skins;
    }

    public static boolean areSkinRequestsEnabled() {
        return enabled && skins;
    }

    private static void setRegisteredListener(boolean registeredListener) {
        ProxyDatabase.registeredListener = registeredListener;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(enabled, databaseSynchronization, skins);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ProxyDatabase that = (ProxyDatabase) o;
        return numOnlinePlayers == that.numOnlinePlayers && hasConnected == that.hasConnected;
    }

    @Override
    public void notifyBounty(UUID uuid) {
        BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.NOTIFY, getBounty(uuid));
        ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
    }

    @Override
    public void replaceBounty(UUID uuid, Bounty bounty) {
        BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.REPLACE_BOUNTY, bounty);
        ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
    }

    @Override
    public int getRefreshInterval() {
        return 1000000;
    }

    @Override
    public void login(UUID uuid, String playerName) {
        numOnlinePlayers++;
        databaseOnlinePlayers.put(uuid, playerName);
    }

    @Override
    public void logout(UUID uuid) {
        numOnlinePlayers--;
        databaseOnlinePlayers.remove(uuid);
        if (numOnlinePlayers == 0)
            hasConnected = false;
    }
}

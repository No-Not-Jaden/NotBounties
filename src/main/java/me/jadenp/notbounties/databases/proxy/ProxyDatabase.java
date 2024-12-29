package me.jadenp.notbounties.databases.proxy;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.databases.NotBountiesDatabase;
import me.jadenp.notbounties.utils.BountyChange;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.PlayerStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/** <TODO> save last sync time, override extra methods</TODO>
 * The proxy database works slightly differently than the others:
 * - A player must be online to receive or send data
 * - When a player joins, the server will receive the database
 * - When data is updated on other servers, it will be immediately sent over to this server
 * Database synchronizations shouldn't need to occur apart from when a player joins.
 * Therefor, getting data here will only return the local data.
 */
public class ProxyDatabase extends NotBountiesDatabase {

    private boolean enabled = false;
    private int numOnlinePlayers = 0;
    private boolean hasConnected = false;
    private List<BountyChange> bountyChanges = new LinkedList<>();
    private Map<UUID, PlayerStat> statChanges = new HashMap<>();
    private static Map<UUID, String> databaseOnlinePlayers = new HashMap<>();
    private static long lastOnlinePlayersCheck = 0;


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

    private void checkConnection() {
        if (isConnected() && !hasConnected) {
            requestPlayerList();
            hasConnected = true;
            ProxyMessaging.sendBountyUpdate(bountyChanges);
            ProxyMessaging.sendStatUpdate(statChanges);
            bountyChanges.clear();
            statChanges.clear();
        }
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) {
        if (enabled) {
            if (hasConnected) {
                // send stats through proxy
                ProxyMessaging.sendStatUpdate(Collections.singletonMap(uuid, stats));
            } else {
                // store change
                if (statChanges.containsKey(uuid))
                    statChanges.replace(uuid, statChanges.get(uuid).combineStats(stats));
                else
                    statChanges.put(uuid, stats);
                checkConnection();
            }
        }
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
        if (enabled) {
            if (hasConnected) {
                // send stats through proxy
                ProxyMessaging.sendStatUpdate(playerStats);
            } else {
                // store change
                for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet()) {
                    if (statChanges.containsKey(entry.getKey()))
                        statChanges.replace(entry.getKey(), statChanges.get(entry.getKey()).combineStats(entry.getValue()));
                    else
                        statChanges.put(entry.getKey(), entry.getValue());
                }
                checkConnection();
            }
        }
    }



    @Override
    public void addBounty(List<Bounty> bounties) {
        if (enabled) {
            List<BountyChange> changes = bounties.stream().map(
                    bounty -> new BountyChange(BountyChange.ChangeType.ADD_BOUNTY, bounty)).toList();
            if (hasConnected) {
                ProxyMessaging.sendBountyUpdate(changes);
            } else {
                bountyChanges.addAll(changes);
            }
            checkConnection();
        }
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {
        if (enabled) {
            List<BountyChange> changes = bounties.stream().map(
                    bounty -> new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, bounty)).toList();
            if (hasConnected) {
                ProxyMessaging.sendBountyUpdate(changes);
            } else {
                bountyChanges.addAll(changes);
            }
            checkConnection();
        }
    }

    @Override
    public Bounty addBounty(@NotNull Bounty bounty) {
        if (enabled) {
            BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.ADD_BOUNTY, bounty);
            if (hasConnected) {
                ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
            } else {
                bountyChanges.add(bountyChange);
            }
            checkConnection();
        }
        return bounty;
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) {
        return DataManager.getLocalData().getBounty(uuid);
    }

    @Override
    public void removeBounty(UUID uuid) {
        if (enabled) {
            BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, getBounty(uuid));
            if (hasConnected) {
                ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
            } else {
                bountyChanges.add(bountyChange);
            }
            checkConnection();
        }
    }

    @Override
    public void removeBounty(Bounty bounty) {
        if (enabled) {
            BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.DELETE_BOUNTY, bounty);
            if (hasConnected) {
                ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
            } else {
                bountyChanges.add(bountyChange);
            }
            checkConnection();
        }
    }

    @Override
    public List<Bounty> getAllBounties(int sortType) {
        return DataManager.getLocalData().getAllBounties(sortType);
    }

    @Override
    public boolean isConnected() {
        // once connected, always connected
        return enabled && ProxyMessaging.hasConnectedBefore() && numOnlinePlayers > 0;
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
        enabled = configuration.getBoolean("enabled");
        return configuration;
    }

    @Override
    protected long getConfigHash() {
        return super.getConfigHash() + Objects.hash(enabled);
    }

    @Override
    public void notifyBounty(UUID uuid) {
        if (enabled) {
            BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.NOTIFY, getBounty(uuid));
            if (hasConnected) {
                ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
            } else {
                bountyChanges.add(bountyChange);
            }
            checkConnection();
        }
    }

    @Override
    public void replaceBounty(UUID uuid, Bounty bounty) {
        if (enabled) {
            BountyChange bountyChange = new BountyChange(BountyChange.ChangeType.REPLACE_BOUNTY, bounty);
            if (hasConnected) {
                ProxyMessaging.sendBountyUpdate(Collections.singletonList(bountyChange));
            } else {
                bountyChanges.add(bountyChange);
            }
            checkConnection();
        }
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
package me.jadenp.notbounties.databases;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.isVanished;

public class LocalData extends NotBountiesDatabase {
    protected final List<Bounty> activeBounties;
    /**
     * The bounties for online players.
     */
    private final Map<UUID, Bounty> onlineBounties = new HashMap<>();
    protected final Map<UUID, PlayerStat> playerStats;

    public LocalData() {
        super(null, "LocalData");
        activeBounties = Collections.synchronizedList(new LinkedList<>());
        playerStats = Collections.synchronizedMap(new HashMap<>());
    }

    protected LocalData(List<Bounty> activeBounties, Map<UUID, PlayerStat> playerStats) {
        super(null, "LocalData");
        this.activeBounties = activeBounties;
        this.playerStats = playerStats;
    }

    private void sortActiveBounties() {
        activeBounties.sort(Comparator.reverseOrder());
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) {
        if (!playerStats.containsKey(uuid)) {
            // player not in changes yet
            this.playerStats.put(uuid, stats);
        } else {
            // add stats to the master values
            this.playerStats.replace(uuid, this.playerStats.get(uuid).combineStats(stats));
        }
    }

    @Override
    public @NotNull PlayerStat getStats(UUID uuid) {
        return playerStats.get(uuid);
    }

    @Override
    public Map<UUID, PlayerStat> getAllStats() {
        Map<UUID, PlayerStat> snapshot;
        synchronized (playerStats) {
            snapshot = new HashMap<>(playerStats);
        }
        return snapshot;
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet()) {
            addStats(entry.getKey(), entry.getValue());
        }
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

    public void setStats(Map<UUID, PlayerStat> stats) {
        playerStats.clear();
        playerStats.putAll(stats);
    }

    @Override
    public Bounty addBounty(@NotNull Bounty bounty) {
        Bounty prevBounty = getBounty(bounty.getUUID());
        if (prevBounty == null) {
            // insert new bounty for this player
            int index = activeBounties.size();
            for (int i = 0; i < activeBounties.size(); i++) {
                if (activeBounties.get(i).compareTo(bounty) < 0) {
                    index = i;
                    break;
                }
            }
            activeBounties.add(index, bounty);
            prevBounty = bounty;
        } else {
            // combine with previous bounty
            for (Setter setter : bounty.getSetters()) {
                prevBounty.addBounty(setter);
            }
            sortActiveBounties();
        }
        if (getOnlinePlayers().containsKey(bounty.getUUID())) {
            onlineBounties.put(bounty.getUUID(), bounty);
        }
        return prevBounty;
    }

    @Override
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        for (int i = 0; i < activeBounties.size(); i++) {
            if (activeBounties.get(i).getUUID().equals(uuid)) {
                if (bounty != null) {
                    activeBounties.set(i, bounty);
                    sortActiveBounties();
                    onlineBounties.put(uuid, bounty);
                } else {
                    onlineBounties.remove(uuid);
                    activeBounties.remove(i);
                }
                break;
            }
        }
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) {
        if (onlineBounties.containsKey(uuid) && onlineBounties.get(uuid) != null)
            return onlineBounties.get(uuid);
        for (Bounty bounty : activeBounties) {
            if (bounty.getUUID().equals(uuid))
                return bounty;
        }
        return null;
    }

    @Override
    public void removeBounty(Bounty bounty) {
        for (int i = 0; i < activeBounties.size(); i++) {
            if (activeBounties.get(i).getUUID().equals(bounty.getUUID())) {
                Bounty bountyCopy = new Bounty(activeBounties.get(i));
                removeSimilarSetters(bountyCopy.getSetters(), bounty.getSetters());
                // if no master setters are left over, remove bounty entirely from active bounties
                if (bountyCopy.getSetters().isEmpty()) {
                    activeBounties.remove(i);
                } else {
                    activeBounties.set(i, bountyCopy);
                    sortActiveBounties();
                }
                break;
            }
        }

        // bounty.getSetters() contains the leftover setters that couldn't be removed
    }

    /**
     * A utility to remove all setters that have the same uuid and time created in both lists
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
        onlineBounties.remove(uuid);
        synchronized (activeBounties) {
            ListIterator<Bounty> bountyListIterator = activeBounties.listIterator();
            while (bountyListIterator.hasNext()) {
                Bounty bounty = bountyListIterator.next();
                if (bounty.getUUID().equals(uuid)) {
                    bountyListIterator.remove();
                    return;
                }
            }
        }

    }

    @Override
    public List<Bounty> getAllBounties(int sortType) {
        List<Bounty> sortedList;
        sortedList = new ArrayList<>(activeBounties);
        if (sortType == -1)
            return sortedList;
        if (sortType == 2)
            return sortedList;
        if (sortType == 3) {
            Collections.reverse(sortedList);
            return sortedList;
        }
        if (sortedList.isEmpty())
            return sortedList;
        Bounty temp;
        for (int i = 0; i < sortedList.size(); i++) {
            for (int j = i + 1; j < sortedList.size(); j++) {
                if (!sortedList.get(i).getSetters().isEmpty()
                        && ((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                        (sortedList.get(i).getLatestUpdate() < sortedList.get(j).getLatestUpdate() && sortType == 1))) {// newest bounties at top
                    temp = sortedList.get(i);
                    sortedList.set(i, sortedList.get(j));
                    sortedList.set(j, temp);
                }
            }
        }
        return sortedList;
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
    public boolean connect() {
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
    public int getRefreshInterval() {
        return 0;
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
        return Bukkit.getOnlinePlayers().stream().filter(player -> !isVanished(player)).collect(Collectors.toMap(Entity::getUniqueId, Player::getName, (a, b) -> b));
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void reloadConfig() {
        // no configuration for this database
    }

    @Override
    public ConfigurationSection readConfig() {
        // no configuration for local data.
        return null;
    }

    @Override
    public void shutdown() {
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
        Bounty bounty = getBounty(uuid);
        if (bounty != null)
            onlineBounties.put(uuid, bounty);
    }

    @Override
    public void logout(UUID uuid) {
        // This data is local, so bukkit methods can be used to retrieve status
        onlineBounties.remove(uuid);
    }

    /**
     * Replaces the server IDs that match with the local server with the global ID.
     */
    public void syncPermData() {
        for (Bounty bounty : activeBounties)
                bounty.setServerID(DataManager.GLOBAL_SERVER_ID);
        for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet())
                entry.getValue().setServerID(DataManager.GLOBAL_SERVER_ID);
    }

}

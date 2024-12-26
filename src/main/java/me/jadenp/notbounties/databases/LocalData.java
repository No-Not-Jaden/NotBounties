package me.jadenp.notbounties.databases;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.PlayerStat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.isVanished;

public class LocalData implements NotBountiesDatabase {
    private final List<Bounty> activeBounties =  Collections.synchronizedList(new LinkedList<>());
    private final Map<UUID, PlayerStat> playerStats = Collections.synchronizedMap(new HashMap<>());

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
            int index = Collections.binarySearch(activeBounties, bounty, Collections.reverseOrder());
            if (index < 0) {
                index = -index - 1;
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
        return prevBounty;
    }

    @Override
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        for (int i = 0; i < activeBounties.size(); i++) {
            if (activeBounties.get(i).getUUID().equals(uuid)) {
                if (bounty != null)
                    activeBounties.set(i, bounty);
                else
                    activeBounties.remove(i);
                break;
            }
        }
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) {
        for (Bounty bounty : activeBounties) {
            if (bounty.getUUID().equals(uuid))
                return bounty;
        }
        return null;
    }

    @Override
    public void removeBounty(Bounty bounty) {
        Bounty masterBounty = getBounty(bounty.getUUID());
        if (masterBounty == null)
            return;
        synchronized (masterBounty) {
            DataManager.removeSimilarSetters(masterBounty.getSetters(), bounty.getSetters());
        }
        // if no master setters are left over, remove bounty entirely from active bounties
        if (masterBounty.getSetters().isEmpty()) {
            removeBounty(masterBounty.getUUID());
        }

        // bounty.getSetters() contains the leftover setters that couldn't be removed
    }


    @Override
    public void removeBounty(UUID uuid) {
        ListIterator<Bounty> bountyListIterator = activeBounties.listIterator();
        while (bountyListIterator.hasNext()) {
            Bounty bounty = bountyListIterator.next();
            if (bounty.getUUID().equals(uuid)) {
                bountyListIterator.remove();
                return;
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
                if ((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                        (sortedList.get(i).getSetters().get(0).getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 1)){// newest bounties at top
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
    }

    @Override
    public void logout(UUID uuid) {
        // This data is local, so bukkit methods can be used to retrieve status
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

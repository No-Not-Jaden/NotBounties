package me.jadenp.notbounties.features.settings.databases;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.PlayerStat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A temporary database is one that doesn't save data long term. Whether that is from a restart or reconnect.
 * Data from temporary databases need to be stored locally still, but they can be used to communicate between servers while connected.
 */
public abstract class TempDatabase extends NotBountiesDatabase{


    /**
     * Gets the bounties that are saved on a specified server.
     * @param serverID ID of the server that the bounties are saved on.
     * @return All the bounties that have the serverID in their data.
     */
    public List<Bounty> getServerBounties(UUID serverID) throws IOException {
        List<Bounty> bounties = getAllBounties(-1);
        bounties.removeIf(bounty -> bounty.getServerID() != serverID);
        return bounties;
    }

    /**
     * Gets the stats that are saved on a specified server.
     * @param serverID ID of the server that the stats are saved on.
     * @return All the stats that have the serverID Vin their data.
     */
    public Map<UUID, PlayerStat> getServerStats(UUID serverID) throws IOException{
        Map<UUID, PlayerStat> stats = getAllStats();
        stats.entrySet().removeIf(entry -> entry.getValue().serverID() != serverID);
        return stats;
    }

    /**
     * Get the ids of the servers that have data stored in this database.
     * @return A list of server IDs that have connected to this database.
     */
    public abstract Set<UUID> getStoredServerIds();

    /**
     * Replaces all server IDs the global server ID.
     */
    public abstract void replaceWithDefaultServerID();

}

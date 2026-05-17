package me.jadenp.notbounties.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.utils.DataManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class PlayerStat {
    private static final Gson gson;
    private final long bountiesClaimed;
    private final long bountiesSet;
    private final long bountiesReceived;
    private final double bountyAllTime;
    private final double immunity;
    private final double bountyClaimAmount;
    private UUID serverID;

    public PlayerStat(long bountiesClaimed, long bountiesSet, long bountiesReceived, double bountyAllTime, double immunity, double bountyClaimAmount, UUID serverID) {
        this.bountiesClaimed = bountiesClaimed;
        this.bountiesSet = bountiesSet;
        this.bountiesReceived = bountiesReceived;
        this.bountyAllTime = bountyAllTime;
        this.immunity = immunity;
        this.bountyClaimAmount = bountyClaimAmount;
        this.serverID = serverID;
    }

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Setter.class, new PlayerStatAdapter());
        gson = builder.create();
    }

    public PlayerStat(long bountiesClaimed, long bountiesSet, long bountiesReceived, double bountyAllTime, double immunity, double bountyClaimAmount) {
        this(bountiesClaimed, bountiesSet, bountiesReceived, bountyAllTime, immunity, bountyClaimAmount, DataManager.getDatabaseServerID(true));
    }

    public PlayerStat(PlayerStat playerStat) {
        this(playerStat.kills(), playerStat.set(), playerStat.deaths(), playerStat.all(), playerStat.immunity(), playerStat.claimed(), playerStat.serverID());
    }

    public PlayerStat(String json) {
        this(gson.fromJson(json, PlayerStat.class));
    }

    public PlayerStat(JsonReader jsonReader) throws IOException {
        this(new PlayerStatAdapter().read(jsonReader));
    }

    public static PlayerStat fromLeaderboard(Leaderboard leaderboard, double amount) {
        switch (leaderboard) {
            case KILLS -> {
                return new PlayerStat((long) amount, 0, 0, 0, 0, 0, DataManager.getDatabaseServerID(true));
            }
            case SET -> {
                return new PlayerStat(0, (long) amount, 0, 0, 0, 0, DataManager.getDatabaseServerID(true));
            }
            case DEATHS -> {
                return new PlayerStat(0, 0, (long) amount, 0, 0, 0, DataManager.getDatabaseServerID(true));
            }
            case ALL -> {
                return new PlayerStat(0, 0, 0, amount, 0, 0, DataManager.getDatabaseServerID(true));
            }
            case IMMUNITY -> {
                return new PlayerStat(0, 0, 0, 0, amount, 0, DataManager.getDatabaseServerID(true));
            }
            case CLAIMED -> {
                return new PlayerStat(0, 0, 0, 0, 0, amount, DataManager.getDatabaseServerID(true));
            }
            default -> {
                return new PlayerStat(0, 0, 0, 0, 0, 0, DataManager.getDatabaseServerID(true));
            }
        }
    }

    /**
     * Combines 2 player stats together.
     * The server ID is kept from this object.
     *
     * @param playerStat The player stat to combine to.
     * @return The combination of the stats in this object, and the stats in the playerStat parameter.
     */
    public @NotNull PlayerStat combineStats(PlayerStat playerStat) {
        return new PlayerStat(bountiesClaimed + playerStat.kills(), bountiesSet + playerStat.set(), bountiesReceived + playerStat.deaths(), bountyAllTime + playerStat.all(), immunity + playerStat.immunity(), bountyClaimAmount + playerStat.claimed(), serverID);
    }

    public JsonObject toJson() {
        return (JsonObject) gson.toJsonTree(this);
    }

    public double leaderboardType(Leaderboard leaderboard) {
        switch (leaderboard) {
            case KILLS -> {
                return bountiesClaimed;
            }
            case SET -> {
                return bountiesSet;
            }
            case DEATHS -> {
                return bountiesReceived;
            }
            case ALL -> {
                return bountyAllTime;
            }
            case IMMUNITY -> {
                return immunity;
            }
            case CLAIMED -> {
                return bountyClaimAmount;
            }
            default -> {
                return 0;
            }
        }
    }

    public long kills() {
        return bountiesClaimed;
    }

    public long set() {
        return bountiesSet;
    }

    public long deaths() {
        return bountiesReceived;
    }

    public double all() {
        return bountyAllTime;
    }

    public double immunity() {
        return immunity;
    }

    public double claimed() {
        return bountyClaimAmount;
    }

    public UUID serverID() {
        return serverID;
    }

    public void setServerID(UUID serverID) {
        this.serverID = serverID;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlayerStat) obj;
        return this.bountiesClaimed == that.bountiesClaimed &&
                this.bountiesSet == that.bountiesSet &&
                this.bountiesReceived == that.bountiesReceived &&
                Double.doubleToLongBits(this.bountyAllTime) == Double.doubleToLongBits(that.bountyAllTime) &&
                Double.doubleToLongBits(this.immunity) == Double.doubleToLongBits(that.immunity) &&
                Double.doubleToLongBits(this.bountyClaimAmount) == Double.doubleToLongBits(that.bountyClaimAmount) &&
                Objects.equals(this.serverID, that.serverID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bountiesClaimed, bountiesSet, bountiesReceived, bountyAllTime, immunity, bountyClaimAmount, serverID);
    }

    @Override
    public String toString() {
        return "PlayerStat[" +
                "kills=" + bountiesClaimed + ", " +
                "set=" + bountiesSet + ", " +
                "deaths=" + bountiesReceived + ", " +
                "all=" + bountyAllTime + ", " +
                "immunity=" + immunity + ", " +
                "claimed=" + bountyClaimAmount + ", " +
                "serverID=" + serverID + ']';
    }

}

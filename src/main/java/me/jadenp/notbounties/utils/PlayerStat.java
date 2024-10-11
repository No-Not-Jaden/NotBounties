package me.jadenp.notbounties.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public final class PlayerStat {
    private static final Gson gson;
    private final long kills;
    private final long set;
    private final long deaths;
    private final double all;
    private final double immunity;
    private final double claimed;
    private UUID serverID;

    public PlayerStat(long kills, long set, long deaths, double all, double immunity, double claimed, UUID serverID) {
        this.kills = kills;
        this.set = set;
        this.deaths = deaths;
        this.all = all;
        this.immunity = immunity;
        this.claimed = claimed;
        this.serverID = serverID;
    }

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Setter.class, new PlayerStatAdapter());
        gson = builder.create();
    }

    public PlayerStat(long kills, long set, long deaths, double all, double immunity, double claimed) {
        this(kills, set, deaths, all, immunity, claimed, DataManager.getDatabaseServerID(true));
    }

    public PlayerStat(PlayerStat playerStat) {
        this(playerStat.kills(), playerStat.set(), playerStat.deaths(), playerStat.all(), playerStat.immunity(), playerStat.claimed(), playerStat.serverID());
    }

    public PlayerStat(String json) {
        this(gson.fromJson(json, PlayerStat.class));
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
        return new PlayerStat(kills + playerStat.kills(), set + playerStat.set(), deaths + playerStat.deaths(), all + playerStat.all(), immunity + playerStat.immunity(), claimed + playerStat.claimed(), serverID);
    }

    public JsonObject toJson() {
        return (JsonObject) gson.toJsonTree(this);
    }

    public double leaderboardType(Leaderboard leaderboard) {
        switch (leaderboard) {
            case KILLS -> {
                return kills;
            }
            case SET -> {
                return set;
            }
            case DEATHS -> {
                return deaths;
            }
            case ALL -> {
                return all;
            }
            case IMMUNITY -> {
                return immunity;
            }
            case CLAIMED -> {
                return claimed;
            }
            default -> {
                return 0;
            }
        }
    }

    public long kills() {
        return kills;
    }

    public long set() {
        return set;
    }

    public long deaths() {
        return deaths;
    }

    public double all() {
        return all;
    }

    public double immunity() {
        return immunity;
    }

    public double claimed() {
        return claimed;
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
        return this.kills == that.kills &&
                this.set == that.set &&
                this.deaths == that.deaths &&
                Double.doubleToLongBits(this.all) == Double.doubleToLongBits(that.all) &&
                Double.doubleToLongBits(this.immunity) == Double.doubleToLongBits(that.immunity) &&
                Double.doubleToLongBits(this.claimed) == Double.doubleToLongBits(that.claimed) &&
                Objects.equals(this.serverID, that.serverID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kills, set, deaths, all, immunity, claimed, serverID);
    }

    @Override
    public String toString() {
        return "PlayerStat[" +
                "kills=" + kills + ", " +
                "set=" + set + ", " +
                "deaths=" + deaths + ", " +
                "all=" + all + ", " +
                "immunity=" + immunity + ", " +
                "claimed=" + claimed + ", " +
                "serverID=" + serverID + ']';
    }

}

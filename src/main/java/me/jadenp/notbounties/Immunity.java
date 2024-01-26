package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class Immunity {
    public enum ImmunityType {
        DISABLE, PERMANENT, SCALING, TIME, GRACE_PERIOD
    }
    private static ImmunityType immunityType;
    private static boolean timeOfflineTracking;
    private static double time;

    private static double permanentCost;
    private static double scalingRatio;
    private static long gracePeriod;
    private static final Map<UUID, Long> immunityTimeTracker = new HashMap<>();
    private static final Map<UUID, Long> gracePeriodTracker = new HashMap<>();
    public static void loadConfiguration(ConfigurationSection configuration) {

        try {
            immunityType = ImmunityType.valueOf(Objects.requireNonNull(configuration.getString("type")).toUpperCase());
        } catch (IllegalArgumentException e) {
            immunityType = ImmunityType.DISABLE;
            Bukkit.getLogger().warning("[NotBounties] Immunity type is not set to a proper value!");
        }
        timeOfflineTracking = configuration.getBoolean("time-immunity.offline-tracking");
        time = configuration.getDouble("time-immunity.seconds");
        permanentCost = configuration.getDouble("permanent-immunity.cost");
        scalingRatio = configuration.getDouble("scaling-immunity.ratio");
        gracePeriod = configuration.getLong("grace-period");

        // add immunity that isn't in time tracker - this should only do anything when immunity is switched to time immunity
        if (immunityType == ImmunityType.TIME) {
            Map<UUID, Double> immunity = getImmunity();
            for (Map.Entry<UUID, Double> entry : immunity.entrySet()) {
                if (!immunityTimeTracker.containsKey(entry.getKey()))
                    if (Bukkit.getOfflinePlayer(entry.getKey()).isOnline() || timeOfflineTracking)
                        immunityTimeTracker.put(entry.getKey(), (long) ((entry.getValue() * time * 1000) + System.currentTimeMillis()));
                    else
                        immunityTimeTracker.put(entry.getKey(), (long) ((entry.getValue() * time * 1000)));
            }
        }

    }

    private static Map<UUID, Double> getImmunity() {
        return BountyManager.SQL.isConnected() ? BountyManager.data.getTopStats(Leaderboard.IMMUNITY, new ArrayList<>(), -1, -1) : BountyManager.immunitySpent;
    }

    public static double getImmunity(UUID uuid) {
        Map<UUID, Double> immunity = getImmunity();
        return immunity.containsKey(uuid) ? immunity.get(uuid) : 0;
    }

    public static long getTimeImmunity(OfflinePlayer player) {
        if (!hasTimeImmunity(player))
            return 0;
        if (player.isOnline() || timeOfflineTracking)
            return immunityTimeTracker.get(player.getUniqueId()) - System.currentTimeMillis();
        return immunityTimeTracker.get(player.getUniqueId());
    }

    /**
     * Get the player immunity from a bounty set
     * @param receiver The player to check immunity for
     * @param amount The amount of currency the bounty will be set for
     * @return The immunity type preventing the bounty or ImmunityType.DISABLE if there is none
     */
    public static ImmunityType getPlayerImmunity(OfflinePlayer receiver, double amount) {
        if (gracePeriodTracker.containsKey(receiver.getUniqueId()) && gracePeriodTracker.get(receiver.getUniqueId()) > System.currentTimeMillis())
            return ImmunityType.GRACE_PERIOD;
        switch (immunityType) {
            case TIME:
                if (hasTimeImmunity(receiver))
                    return ImmunityType.TIME;
                break;
            case SCALING:
                if (getImmunity(receiver.getUniqueId()) * scalingRatio >= amount)
                    return ImmunityType.SCALING;
                break;
            case PERMANENT:
                if (getImmunity(receiver.getUniqueId()) >= permanentCost)
                    return ImmunityType.PERMANENT;
                break;
        }
        return ImmunityType.DISABLE;
    }

    private static boolean hasTimeImmunity(OfflinePlayer player) {
        if ((player.isOnline() || timeOfflineTracking) && immunityTimeTracker.containsKey(player.getUniqueId()) && immunityTimeTracker.get(player.getUniqueId()) > System.currentTimeMillis()) {
            return true;
        }
        return !player.isOnline() && !timeOfflineTracking && immunityTimeTracker.containsKey(player.getUniqueId()) && immunityTimeTracker.get(player.getUniqueId()) > 0;
    }

    public static void login() {

    }

    public static void logout(){

    }


}

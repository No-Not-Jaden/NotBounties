package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

import static me.jadenp.notbounties.NotBounties.immunePerms;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class Immunity {
    public enum ImmunityType {
        /**
         * No immunity or immunity is disabled
         */
        DISABLE,
        /**
         * Permanent immunity - bought or with the notbounties.immune permission
         */
        PERMANENT,
        /**
         * Scaling immunity - every x currency spent covers x*ratio worth of bounties
         */
        SCALING,
        /**
         * Time immunity - tick, tock. Your time counts down every second.
         */
        TIME,
        /**
         * Grace period immunity - a bounty was just claimed on this person
         */
        GRACE_PERIOD,
        /**
         * Auto bounty murder immunity - bounties can't be placed on this person for murdering another player
         */
        MURDER,
        /**
         * Auto bounty random immunity - random auto bounties can't be placed on this person
         */
        RANDOM,
        /**
         * Auto bounty timed immunity - timed auto bounties can't be placed on this person
         */
        TIMED
    }
    public static ImmunityType immunityType;
    private static boolean timeOfflineTracking = false;
    private static double time;

    private static double permanentCost;
    private static double scalingRatio;
    private static long gracePeriod;
    private static Map<UUID, Long> immunityTimeTracker = new HashMap<>();
    private static final Map<UUID, Long> gracePeriodTracker = new HashMap<>();
    public static void loadConfiguration(ConfigurationSection configuration) {

        try {
            immunityType = ImmunityType.valueOf(Objects.requireNonNull(configuration.getString("type")).toUpperCase());
        } catch (IllegalArgumentException e) {
            immunityType = ImmunityType.DISABLE;
            Bukkit.getLogger().warning("[NotBounties] Immunity type is not set to a proper value!");
        }

        time = configuration.getDouble("time-immunity.seconds");
        permanentCost = configuration.getDouble("permanent-immunity.cost");
        scalingRatio = configuration.getDouble("scaling-immunity.ratio");
        gracePeriod = configuration.getLong("grace-period");

        if (immunityType == ImmunityType.TIME) {
            // convert saved times if necessary
            boolean updatedOfflineTracking = configuration.getBoolean("time-immunity.offline-tracking");
            if (!timeOfflineTracking && updatedOfflineTracking) {
                // convert to global time
                Map<UUID, Long> updatedNextBounties = new HashMap<>();
                for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
                    // only convert if the player is offline, otherwise the value should already be in global time
                    if (!Bukkit.getOfflinePlayer(entry.getKey()).isOnline())
                        updatedNextBounties.put(entry.getKey(), entry.getValue() + System.currentTimeMillis());
                }
                immunityTimeTracker = updatedNextBounties;
            } else if (timeOfflineTracking && !updatedOfflineTracking) {
                // convert to local time
                Map<UUID, Long> updatedNextBounties = new HashMap<>();
                for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
                    if (!Bukkit.getOfflinePlayer(entry.getKey()).isOnline())
                        updatedNextBounties.put(entry.getKey(), entry.getValue() - System.currentTimeMillis());
                }
                immunityTimeTracker = updatedNextBounties;
            }
            timeOfflineTracking = updatedOfflineTracking;
            // add immunity that isn't in time tracker - this should only do anything when immunity is switched to time immunity or the server is starting
            Map<UUID, Double> immunity = getImmunity();
            for (Map.Entry<UUID, Double> entry : immunity.entrySet()) {
                if (!immunityTimeTracker.containsKey(entry.getKey()))
                    if (Bukkit.getOfflinePlayer(entry.getKey()).isOnline() || timeOfflineTracking)
                        immunityTimeTracker.put(entry.getKey(), (long) ((entry.getValue() * time * 1000) + System.currentTimeMillis()));
                    else
                        immunityTimeTracker.put(entry.getKey(), (long) ((entry.getValue() * time * 1000)));
            }
        } else {
            immunityTimeTracker.clear();
        }

    }

    /**
     * Removes immunity of a player
     * @param uuid UUID of the player
     * @return true if the player had immunity
     */
    public static boolean removeImmunity(UUID uuid) {
        if (getImmunity(uuid) == 0)
            return false;
        DataManager.changeStat(uuid, Leaderboard.IMMUNITY, DataManager.getStat(uuid, Leaderboard.IMMUNITY) * -1);
        if (immunityType == ImmunityType.TIME)
            immunityTimeTracker.remove(uuid);
        return true;
    }

    public static double getScalingRatio() {
        return scalingRatio;
    }

    public static double getTime() {
        return time;
    }

    public static double getPermanentCost() {
        return permanentCost;
    }

    public static void startGracePeriod(Player player) {
        gracePeriodTracker.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private static long lastSQLUpdate = 0;
    public static void update() {
        if (immunityType != ImmunityType.TIME)
            return;

        // iterate through time tracker to find any expired immunity.
        List<UUID> expiredImmunity = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
            if ((Bukkit.getOfflinePlayer(entry.getKey()).isOnline() || timeOfflineTracking) && System.currentTimeMillis() > entry.getValue()) {
                expiredImmunity.add(entry.getKey());
            } else if (!(Bukkit.getOfflinePlayer(entry.getKey()).isOnline() || timeOfflineTracking) && entry.getValue() <= 0) {
                expiredImmunity.add(entry.getKey());
            } else {
                double immunity = Bukkit.getOfflinePlayer(entry.getKey()).isOnline() || timeOfflineTracking ? (entry.getValue() - System.currentTimeMillis()) / 1000.0D / time : (double) (entry.getValue()) / 1000 / time;
                DataManager.changeStat(entry.getKey(), Leaderboard.IMMUNITY, immunity - DataManager.getStat(entry.getKey(), Leaderboard.IMMUNITY));
            }
        }
        for (UUID uuid : expiredImmunity) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.isOnline())
                Objects.requireNonNull(player.getPlayer()).sendMessage(parse(prefix + immunityExpire, player));
            immunityTimeTracker.remove(uuid);
            DataManager.changeStat(uuid, Leaderboard.IMMUNITY, DataManager.getStat(uuid, Leaderboard.IMMUNITY) * -1);
        }

    }


    public static void addImmunity(UUID uuid, double amount) {
        DataManager.changeStat(uuid, Leaderboard.IMMUNITY, amount);
        if (immunityType == ImmunityType.TIME)
            if (immunityTimeTracker.containsKey(uuid)) {
                immunityTimeTracker.replace(uuid, (long) (immunityTimeTracker.get(uuid) + amount * time * 1000L));
            } else {
                if (Bukkit.getOfflinePlayer(uuid).isOnline() || !timeOfflineTracking)
                    immunityTimeTracker.put(uuid, (long) (amount * time * 1000L + System.currentTimeMillis()));
                else
                    immunityTimeTracker.put(uuid, (long) (amount * time * 1000L));
            }
    }

    public static void setImmunity(UUID uuid, double amount) {
        DataManager.changeStat(uuid, Leaderboard.IMMUNITY, amount - DataManager.getStat(uuid, Leaderboard.IMMUNITY));
        if (immunityType == ImmunityType.TIME) {
            if (Bukkit.getOfflinePlayer(uuid).isOnline() || !timeOfflineTracking) {
                immunityTimeTracker.put(uuid, (long) (amount * time * 1000L + System.currentTimeMillis()));
            } else {
                immunityTimeTracker.put(uuid, (long) (amount * time * 1000L));
            }
        }
    }

    private static Map<UUID, Double> getImmunity() {
        return Leaderboard.IMMUNITY.getStatMap();
    }

    public static long getGracePeriod(UUID uuid) {
        if (gracePeriodTracker.containsKey(uuid)) {
            long timeSinceDeath = System.currentTimeMillis() - gracePeriodTracker.get(uuid);
            if (timeSinceDeath < gracePeriod * 1000L) {
                // still in grace period
                return (gracePeriod * 1000L) - timeSinceDeath;
            } else {
                gracePeriodTracker.remove(uuid);
            }
        }
        return 0;
    }

    public static double getImmunity(UUID uuid) {
        return Leaderboard.IMMUNITY.getStat(uuid);
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
    public static ImmunityType getAppliedImmunity(OfflinePlayer receiver, double amount) {
        if (getGracePeriod(receiver.getUniqueId()) > 0)
            return ImmunityType.GRACE_PERIOD;
        if (receiver.getUniqueId().equals(DataManager.GLOBAL_SERVER_ID) || (receiver.isOnline() && Objects.requireNonNull(receiver.getPlayer()).hasPermission("notbounties.immune")) || (!receiver.isOnline() && immunePerms.contains(receiver.getUniqueId().toString()))) {
            return ImmunityType.PERMANENT;
        }
        switch (immunityType) {
            case TIME:
                if (hasTimeImmunity(receiver))
                    return ImmunityType.TIME;
                break;
            case SCALING:
                if (getImmunity(receiver.getUniqueId()) * scalingRatio >= amount && amount != 0)
                    return ImmunityType.SCALING;
                break;
            case PERMANENT:
                if (getImmunity(receiver.getUniqueId()) >= permanentCost)
                    return ImmunityType.PERMANENT;
                break;
            default:
                return ImmunityType.DISABLE;
        }
        return ImmunityType.DISABLE;
    }

    private static boolean hasTimeImmunity(OfflinePlayer player) {
        if ((player.isOnline() || timeOfflineTracking) && immunityTimeTracker.containsKey(player.getUniqueId()) && immunityTimeTracker.get(player.getUniqueId()) > System.currentTimeMillis()) {
            return true;
        }
        return !player.isOnline() && !timeOfflineTracking && immunityTimeTracker.containsKey(player.getUniqueId()) && immunityTimeTracker.get(player.getUniqueId()) > 0;
    }

    public static void login(Player player) {
        if (immunityType == ImmunityType.TIME && !timeOfflineTracking && immunityTimeTracker.containsKey(player.getUniqueId())) {
            // change storage type from time until expire to time of expire
            immunityTimeTracker.replace(player.getUniqueId(), immunityTimeTracker.get(player.getUniqueId()) + System.currentTimeMillis());
        }
    }

    public static void logout(Player player){
        if (immunityType == ImmunityType.TIME && !timeOfflineTracking && immunityTimeTracker.containsKey(player.getUniqueId())) {
            // change storage type from time to expire to time until expire
            immunityTimeTracker.replace(player.getUniqueId(), immunityTimeTracker.get(player.getUniqueId()) - System.currentTimeMillis());
        }
    }


}

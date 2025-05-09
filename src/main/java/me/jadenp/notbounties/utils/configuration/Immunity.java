package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.PlayerData;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

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
         * New player immunity - this player is new to the server, so bounties can't be set on them.
         */
        NEW_PLAYER,
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

    /**
     * Current immunity type set in the configuration. This can be DISABLE, PERMANENT, SCALING, or TIME.
     */
    private static ImmunityType immunityType;
    /**
     * Whether time immunity ticks down when the player is offline.
     */
    private static boolean timeOfflineTracking = false;
    /**
     * The amount of time given per currency spent on immunity.
     */
    private static double time;
    /**
     * The permanent cost of immunity.
     */
    private static double permanentCost;
    /**
     * The ratio of currency spent to immunity strength.
     */
    private static double scalingRatio;
    /**
     * The time in seconds that a player has immunity for after a bounty is claimed on them.
     */
    private static long gracePeriod;
    /**
     * The time in seconds that a new player must have in playtime before a bounty can be set on them.
     */
    private static long newPlayerImmunity;
    /**
     * Whether the notbounties.immune permission and other permission nodes gives players immunity
     */
    private static boolean permissionImmunity;
    /**
     * Time immunity tracking. This will either display at what time in milliseconds that the immunity expires,
     * or how many milliseconds the player has left in their immunity.
     */
    private static Map<UUID, Long> immunityTimeTracker = new HashMap<>();
    private static Set<UUID> onlinePlayers = new CopyOnWriteArraySet<>();
    /**
     * The time at which the grace period started for players.
     */
    private static final Map<UUID, Long> gracePeriodTracker = new HashMap<>();

    /**
     * Load the immunity configuration.
     * @param configuration The immunity configuration section in the config.yml file.
     */
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
        newPlayerImmunity = configuration.getLong("new-player-immunity");
        permissionImmunity = configuration.getBoolean("permission-immunity");

        if (immunityType == ImmunityType.TIME) {
            // convert saved times if necessary
            boolean updatedOfflineTracking = configuration.getBoolean("time-immunity.offline-tracking");
            if (!timeOfflineTracking && updatedOfflineTracking) {
                // convert to global time
                Map<UUID, Long> updatedNextBounties = new HashMap<>();
                for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
                    // only convert if the player is offline, otherwise the value should already be in global time
                    if (!onlinePlayers.contains(entry.getKey()))
                        updatedNextBounties.put(entry.getKey(), entry.getValue() + System.currentTimeMillis());
                }
                immunityTimeTracker = updatedNextBounties;
            } else if (timeOfflineTracking && !updatedOfflineTracking) {
                // convert to local time
                Map<UUID, Long> updatedNextBounties = new HashMap<>();
                for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
                    if (!onlinePlayers.contains(entry.getKey()))
                        updatedNextBounties.put(entry.getKey(), entry.getValue() - System.currentTimeMillis());
                }
                immunityTimeTracker = updatedNextBounties;
            }
            timeOfflineTracking = updatedOfflineTracking;
        } else {
            immunityTimeTracker.clear();
        }

    }

    public static void loadPlayerData() {
        // add immunity that isn't in time tracker - this should only do anything when immunity is switched to time immunity or the server is starting
        Map<UUID, Double> immunity = getImmunity();
        if (immunityType == ImmunityType.TIME) {
            for (Map.Entry<UUID, Double> entry : immunity.entrySet()) {
                if (entry.getValue() > 0 && !immunityTimeTracker.containsKey(entry.getKey())) {
                    if (onlinePlayers.contains(entry.getKey()) || timeOfflineTracking) {
                        immunityTimeTracker.put(entry.getKey(), (long) ((entry.getValue() * time * 1000) + System.currentTimeMillis()));
                    } else {
                        immunityTimeTracker.put(entry.getKey(), (long) (entry.getValue() * time * 1000));
                    }
                }
            }
        }
        // check playtime
        for (UUID uuid : DataManager.getPlayerDataMap().keySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            DataManager.getPlayerData(uuid).setNewPlayer(player.getStatistic(Statistic.PLAY_ONE_MINUTE) < newPlayerImmunity);
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

    public static void update() {
        if (immunityType != ImmunityType.TIME)
            return;

        // iterate through time tracker to find any expired immunity.
        List<UUID> expiredImmunity = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
            if ((onlinePlayers.contains(entry.getKey()) || timeOfflineTracking) && System.currentTimeMillis() > entry.getValue()) {
                expiredImmunity.add(entry.getKey());
            } else if (!(onlinePlayers.contains(entry.getKey()) || timeOfflineTracking) && entry.getValue() <= 0) {
                expiredImmunity.add(entry.getKey());
            } else {
                double immunity = onlinePlayers.contains(entry.getKey()) || timeOfflineTracking ? (entry.getValue() - System.currentTimeMillis()) / 1000.0D / time : (double) (entry.getValue()) / 1000 / time;
                DataManager.changeStat(entry.getKey(), Leaderboard.IMMUNITY, immunity - DataManager.getStat(entry.getKey(), Leaderboard.IMMUNITY));
            }
        }
        for (UUID uuid : expiredImmunity) {
            if (onlinePlayers.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null)
                    player.sendMessage(parse(getPrefix() + getMessage("immunity-expire"), player));
            }
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
                if (onlinePlayers.contains(uuid) || !timeOfflineTracking)
                    immunityTimeTracker.put(uuid, (long) (amount * time * 1000L + System.currentTimeMillis()));
                else
                    immunityTimeTracker.put(uuid, (long) (amount * time * 1000L));
            }
    }

    public static void setImmunity(UUID uuid, double amount) {
        DataManager.changeStat(uuid, Leaderboard.IMMUNITY, amount - DataManager.getStat(uuid, Leaderboard.IMMUNITY));
        if (immunityType == ImmunityType.TIME) {
            if (onlinePlayers.contains(uuid) || !timeOfflineTracking) {
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

    public static long getTimeImmunity(UUID uuid) {
        if (!hasTimeImmunity(uuid))
            return 0;
        if (onlinePlayers.contains(uuid) || timeOfflineTracking)
            return immunityTimeTracker.get(uuid) - System.currentTimeMillis();
        return immunityTimeTracker.get(uuid);
    }

    /**
     * Get the player immunity from a bounty set
     * @param uuid The player to check immunity for
     * @param amount The amount of currency the bounty will be set for
     * @return The immunity type preventing the bounty or ImmunityType.DISABLE if there is none
     */
    public static ImmunityType getAppliedImmunity(UUID uuid, double amount) {
        // check for grace period
        if (getGracePeriod(uuid) > 0)
            return ImmunityType.GRACE_PERIOD;
        // check for permanent immunity
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID)
                || (permissionImmunity && DataManager.getPlayerData(uuid).hasGeneralImmunity())) {
            return ImmunityType.PERMANENT;
        }
        // check for new player immunity
        if (DataManager.getPlayerData(uuid).isNewPlayer()) return ImmunityType.NEW_PLAYER;
        // check for bought immunity
        switch (immunityType) {
            case TIME:
                if (hasTimeImmunity(uuid))
                    return ImmunityType.TIME;
                break;
            case SCALING:
                if (getImmunity(uuid) * scalingRatio >= amount && amount != 0)
                    return ImmunityType.SCALING;
                break;
            case PERMANENT:
                if (getImmunity(uuid) >= permanentCost)
                    return ImmunityType.PERMANENT;
                break;
            default:
                return ImmunityType.DISABLE;
        }
        return ImmunityType.DISABLE;
    }

    private static boolean hasTimeImmunity(UUID uuid) {
        if ((onlinePlayers.contains(uuid) || timeOfflineTracking) && immunityTimeTracker.containsKey(uuid) && immunityTimeTracker.get(uuid) > System.currentTimeMillis()) {
            return true;
        }
        return !onlinePlayers.contains(uuid) && !timeOfflineTracking && immunityTimeTracker.containsKey(uuid) && immunityTimeTracker.get(uuid) > 0;
    }

    public static void login(Player player) {
        onlinePlayers.add(player.getUniqueId());
        if (immunityType == ImmunityType.TIME && !timeOfflineTracking && immunityTimeTracker.containsKey(player.getUniqueId())) {
            // change storage type from time until expire to time of expire
            immunityTimeTracker.replace(player.getUniqueId(), immunityTimeTracker.get(player.getUniqueId()) + System.currentTimeMillis());
        }
        checkPermissionImmunity(player);
    }

    public static void logout(Player player){
        onlinePlayers.remove(player.getUniqueId());
        if (immunityType == ImmunityType.TIME && !timeOfflineTracking && immunityTimeTracker.containsKey(player.getUniqueId())) {
            // change storage type from time to expire to time until expire
            immunityTimeTracker.replace(player.getUniqueId(), immunityTimeTracker.get(player.getUniqueId()) - System.currentTimeMillis());
        }
        checkPermissionImmunity(player);
    }

    public static ImmunityType getImmunityType() {
        return immunityType;
    }

    public static boolean isPermissionImmunity() {
        return permissionImmunity;
    }

    public static long getNewPlayerImmunity() {
        return newPlayerImmunity;
    }

    /**
     * Check permission immunity for a player and store the results in their playerdata.
     * @param player Player to check the immunity for.
     */
    public static void checkPermissionImmunity(Player player) {
        PlayerData playerData = DataManager.getPlayerData(player.getUniqueId());
        playerData.setGeneralImmunity(player.hasPermission("notbounties.immune"));
        playerData.setTimedImmunity(player.hasPermission("notbounties.immunity.timed"));
        playerData.setRandomImmunity(player.hasPermission("notbounties.immunity.random"));
        playerData.setMurderImmunity(player.hasPermission("notbounties.immunity.murder"));
        playerData.setNewPlayer(player.getStatistic(Statistic.PLAY_ONE_MINUTE) < newPlayerImmunity);
    }

    /**
     * Check permission immunity for all online players. Occasionally checking permissions is useful,
     * so immunity queries can be done without having to retrieve a bukkit player.
     */
    public static void checkOnlinePermissionImmunity() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        NotBounties.getServerImplementation().async().runNow(() -> {
            Random random = new Random();
            for (Player player : players)
                if (random.nextInt(players.size()) < 50)
                    checkPermissionImmunity(player);
        });
    }

}

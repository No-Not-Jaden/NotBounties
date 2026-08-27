package me.jadenp.notbounties.features.settings.immunity;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import static me.jadenp.notbounties.features.LanguageOptions.*;

public class ImmunityManager {
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
    private static long newPlayerImmunity = -1;
    /**
     * Whether the notbounties.immune permission and other permission nodes gives players immunity
     */
    private static boolean permissionImmunity;
    /**
     * The time in seconds that a player must wait before they can set another bounty.
     */
    private static long bountyCooldown;
    /**
     * Time immunity tracking. This will either display at what time in milliseconds that the immunity expires,
     * or how many milliseconds the player has left in their immunity.
     */
    private static Map<UUID, Long> immunityTimeTracker = new HashMap<>();
    private static final Set<UUID> onlinePlayers = new CopyOnWriteArraySet<>();
    private static final Random random = new Random();

    /**
     * Load the immunity configuration.
     *
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
        permissionImmunity = configuration.getBoolean("permission-immunity");
        bountyCooldown = configuration.getLong("bounty-cooldown");

        newPlayerImmunity = configuration.getLong("new-player-immunity");

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
        if (immunityType == ImmunityType.TIME) {
            DataManager.iterateAllStats((uuid, playerStat) -> {
                if (playerStat.immunity() > 0 && !immunityTimeTracker.containsKey(uuid)) {
                    if (onlinePlayers.contains(uuid) || timeOfflineTracking) {
                        immunityTimeTracker.put(uuid, (long) ((playerStat.immunity() * time * 1000) + System.currentTimeMillis()));
                    } else {
                        immunityTimeTracker.put(uuid, (long) (playerStat.immunity() * time * 1000));
                    }
                }
            });
        }
    }

    /**
     * Removes immunity of a player
     *
     * @param uuid UUID of the player
     * @return A true future if the player had immunity
     */
    public static CompletableFuture<Boolean> removeImmunity(UUID uuid) {
        return DataManager.getStatAsync(uuid, Leaderboard.IMMUNITY).thenApply(immunity -> {
            if (immunity == 0)
                return false;
            DataManager.changeStat(uuid, Leaderboard.IMMUNITY, immunity * -1);
            if (immunityType == ImmunityType.TIME)
                immunityTimeTracker.remove(uuid);
            return true;
        });
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
        DataManager.getPlayerDataAsync(player.getUniqueId()).thenAccept(playerData -> playerData.setLastClaim(System.currentTimeMillis()));
    }

    public static void update() {
        if (immunityType != ImmunityType.TIME)
            return;
        // TODO: Check if immunity was removed on another server -> immunity value = 0
        // iterate through time tracker to find any expired immunity.
        List<UUID> expiredImmunity = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
            if ((onlinePlayers.contains(entry.getKey()) || timeOfflineTracking) && System.currentTimeMillis() > entry.getValue()) {
                expiredImmunity.add(entry.getKey());
            } else if (!(onlinePlayers.contains(entry.getKey()) || timeOfflineTracking) && entry.getValue() <= 0) {
                expiredImmunity.add(entry.getKey());
            } else {
                double immunity = onlinePlayers.contains(entry.getKey()) || timeOfflineTracking ? (entry.getValue() - System.currentTimeMillis()) / 1000.0D / time : (double) (entry.getValue()) / 1000 / time;
                DataManager.getStatAsync(entry.getKey(), Leaderboard.IMMUNITY).thenAccept(immunityValue -> DataManager.changeStat(entry.getKey(), Leaderboard.IMMUNITY, immunity - immunityValue));

            }
        }
        for (UUID uuid : expiredImmunity) {
            if (onlinePlayers.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null)
                    Messages.send(player, getPrefix() + getMessage("immunity-expire"), MessageContext.builder().receiver(player).build());
            }
            immunityTimeTracker.remove(uuid);
            DataManager.getStatAsync(uuid, Leaderboard.IMMUNITY).thenAccept(immunityValue -> DataManager.changeStat(uuid, Leaderboard.IMMUNITY, immunityValue * -1));
        }

    }

    public static long currencyToTime(double currency) {
        return (long) (currency * time * 1000L);
    }

    public static void addImmunity(UUID uuid, double amount) {
        DataManager.changeStat(uuid, Leaderboard.IMMUNITY, amount);
        if (immunityType == ImmunityType.TIME) {
            if (immunityTimeTracker.containsKey(uuid)) {
                immunityTimeTracker.replace(uuid, (long) (immunityTimeTracker.get(uuid) + amount * time * 1000L));
            } else {
                if (onlinePlayers.contains(uuid) || !timeOfflineTracking) {
                    immunityTimeTracker.put(uuid, (long) (amount * time * 1000L + System.currentTimeMillis()));
                } else {
                    immunityTimeTracker.put(uuid, (long) (amount * time * 1000L));
                }
            }
        }
    }

    public static void setImmunity(UUID uuid, double amount) {
        DataManager.getStatAsync(uuid, Leaderboard.IMMUNITY).thenAccept(immunityValue -> DataManager.changeStat(uuid, Leaderboard.IMMUNITY, amount - immunityValue));
        if (immunityType == ImmunityType.TIME) {
            if (onlinePlayers.contains(uuid) || !timeOfflineTracking) {
                immunityTimeTracker.put(uuid, (long) (amount * time * 1000L + System.currentTimeMillis()));
            } else {
                immunityTimeTracker.put(uuid, (long) (amount * time * 1000L));
            }
        }
    }


    public static CompletableFuture<Long> getGracePeriod(@NotNull UUID uuid) {
        return DataManager.getPlayerDataAsync(uuid).thenApply(playerData -> {
            long timeSinceDeath = System.currentTimeMillis() - playerData.getLastClaim();
            if (timeSinceDeath < gracePeriod * 1000L) {
                // still in grace period
                return (gracePeriod * 1000L) - timeSinceDeath;
            }
            return 0L;
        });
    }

    public static CompletableFuture<Double> getImmunity(UUID uuid) {
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
     *
     * @param uuid   The player to check immunity for
     * @param amount The amount of currency the bounty will be set for
     * @return The immunity type preventing the bounty or ImmunityType.DISABLE if there is none
     */
    public static CompletableFuture<ImmunityType> getAppliedImmunity(@NotNull UUID uuid, double amount) {
        CompletableFuture<Long> gracePeriod = getGracePeriod(uuid);
        CompletableFuture<Double> immunity = getImmunity(uuid);
        CompletableFuture<PlayerData> playerData = DataManager.getPlayerDataAsync(uuid);

        return CompletableFuture.allOf(gracePeriod, immunity, playerData).thenApply(v -> {
            // check for grace period
            if (gracePeriod.join() > 0)
                return ImmunityType.GRACE_PERIOD;
            // check for permanent immunity
            if (uuid.equals(DataManager.GLOBAL_SERVER_ID)
                    || (permissionImmunity && playerData.join().hasGeneralImmunity())) {
                return ImmunityType.PERMANENT;
            }
            // check for bought immunity
            switch (immunityType) {
                case TIME:
                    if (hasTimeImmunity(uuid))
                        return ImmunityType.TIME;
                    break;
                case SCALING:
                    if (immunity.join() * scalingRatio >= amount && amount != 0)
                        return ImmunityType.SCALING;
                    break;
                case PERMANENT:
                    if (immunity.join() >= permanentCost)
                        return ImmunityType.PERMANENT;
                    break;
                default:
                    return ImmunityType.DISABLE;
            }
            // check for new player immunity
            if (playerData.join().isNewPlayer()) return ImmunityType.NEW_PLAYER;

            return ImmunityType.DISABLE;
        });


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

    public static void logout(Player player) {
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

    public static void setNewPlayerImmunity(long newPlayerImmunity) {
        ImmunityManager.newPlayerImmunity = newPlayerImmunity;
    }

    /**
     * Check permission immunity for a player and store the results in their playerdata.
     *
     * @param player Player to check the immunity for.
     */
    public static void checkPermissionImmunity(Player player) {
        DataManager.getPlayerDataAsync(player.getUniqueId()).thenAccept(playerData -> {
            playerData.setGeneralImmunity(player.hasPermission("notbounties.immune"));
            playerData.setTimedImmunity(player.hasPermission("notbounties.immunity.timed"));
            playerData.setRandomImmunity(player.hasPermission("notbounties.immunity.random"));
            playerData.setMurderImmunity(player.hasPermission("notbounties.immunity.murder"));
            DataManager.updatePlayerData(playerData);
        });
    }

    /**
     * Check permission immunity for all online players. Occasionally checking permissions is useful,
     * so immunity queries can be done without having to retrieve a bukkit player.
     */
    public static void checkOnlinePermissionImmunity() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        NotBounties.getServerImplementation().async().runNow(() -> {
            for (Player player : players)
                if (random.nextInt(players.size()) < 50)
                    checkPermissionImmunity(player);
        });
    }

    public static long getBountyCooldown() {
        return bountyCooldown;
    }


    public static CompletableFuture<Boolean> hasPermissionImmunity(OfflinePlayer player, String permission, Function<PlayerData, Boolean> dataFunction) {
        if (!ImmunityManager.isPermissionImmunity())
            return CompletableFuture.completedFuture(false);

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer != null) {
            TaskImplementation<Boolean> task = NotBounties.getServerImplementation().entity(Objects.requireNonNull(player.getPlayer()))
                    .run((Function<TaskImplementation<Boolean>, Boolean>) t -> Objects.requireNonNull(player.getPlayer()).hasPermission(permission));
            if (task != null) {
                return task.asFuture().thenApply(TaskImplementation::getCallback);
            }

        }
        // wait for prev check
        return DataManager.getPlayerDataAsync(player.getUniqueId()).thenApply(dataFunction);
    }

    public static CompletableFuture<Boolean> checkAndNotifyImmunity(@NotNull CommandSender sender, double amount, boolean silent, OfflinePlayer player, List<ItemStack> items) {
        return ImmunityManager.getAppliedImmunity(player.getUniqueId(), amount).thenApply(immunityType -> {

            switch (immunityType) {
                case ImmunityType.GRACE_PERIOD:
                    if (!silent) {
                        ImmunityManager.getGracePeriod(player.getUniqueId()).thenAccept(gracePeriod -> {
                            Messages.send(sender, getPrefix()
                                    + LanguageOptions.getMessage("grace-period")
                                    .replace("{time}", (LocalTime.formatTime(
                                            gracePeriod
                                            , LocalTime.TimeFormat.RELATIVE))), MessageContext.builder().receiver(player).build());
                        });
                    }
                    break;
                case NEW_PLAYER:
                    long immunityMS = (long) ((ImmunityManager.getNewPlayerImmunity() - ((double) player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20)) * 1000L);
                    if (!silent)
                        Messages.send(sender, getPrefix()
                                + LanguageOptions.getMessage("new-player-immunity")
                                .replace("{time}", (LocalTime.formatTime(
                                        immunityMS,
                                        LocalTime.TimeFormat.RELATIVE))), MessageContext.builder().receiver(player).build());
                    break;
                case PERMANENT:
                    if (NumberFormatting.isBountyItemsOverrideImmunity() && !items.isEmpty())
                        break;
                    if (!silent)
                        Messages.send(sender, getPrefix() + getMessage("permanent-immunity"), MessageContext.builder().receiver(player).bounty(ImmunityManager.getImmunity(player.getUniqueId()).join()).build());
                    break;
                case SCALING:
                    if (NumberFormatting.isBountyItemsOverrideImmunity() && !items.isEmpty())
                        break;
                    if (!silent)
                        Messages.send(sender, getPrefix() + getMessage("scaling-immunity"), MessageContext.builder().receiver(player).bounty(ImmunityManager.getImmunity(player.getUniqueId()).join()).build());
                    break;
                case TIME:
                    if (NumberFormatting.isBountyItemsOverrideImmunity() && !items.isEmpty())
                        break;
                    if (!silent)
                        Messages.send(sender, getPrefix() + LanguageOptions.getMessage("time-immunity").replace("{time}", (LocalTime.formatTime(ImmunityManager.getTimeImmunity(player.getUniqueId()), LocalTime.TimeFormat.RELATIVE))), MessageContext.builder().receiver(player).bounty(ImmunityManager.getImmunity(player.getUniqueId()).join()).build());
                    break;
                default:
                    // Not using immunity
                    return false;
            }
            return true;
        });
    }
}

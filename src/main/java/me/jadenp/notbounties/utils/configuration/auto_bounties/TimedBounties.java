package me.jadenp.notbounties.utils.configuration.auto_bounties;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.Immunity;
import me.jadenp.notbounties.utils.external_api.LiteBansClass;
import me.jadenp.notbounties.data.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

import static me.jadenp.notbounties.NotBounties.isVanished;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.liteBansEnabled;

public class TimedBounties {

    private TimedBounties(){}

    /**
     * The amount of time between bounties in seconds.
     */
    private static long time;
    /**
     * The amount of currency that the bounty should increase.
     */
    private static double bountyIncrease;
    /**
     * Whether the timer for the next bounty should be reset when the player dies.
     */
    private static boolean resetOnDeath;
    /**
     * The maximum bounty the player can have with timed bounties.
     * 0 means no maximum.
     */
    private static double maxBounty;
    /**
     * Whether offline players should have bounties set on them.
     */
    private static boolean offlineTracking = false;
    /**
     * When the next bounty should be set for every player.
     * When offlineTracking is...
     *   true  -> The time in milliseconds when the next bounty should be set is always stored as the value.
     *   false -> The time in milliseconds until the bounty should be set is stored when the player is offline.
     * Online players will ALWAYS have the value be the time when the next bounty should be set.
     */
    private static Map<UUID, Long> nextBounties = new HashMap<>();

    public static void loadConfiguration(ConfigurationSection timedBounties) {
        time  = timedBounties.getInt("time");
        resetOnDeath = timedBounties.getBoolean("reset-on-death");
        bountyIncrease = timedBounties.getDouble("bounty-increase");
        maxBounty = timedBounties.getDouble("max-bounty");
        boolean updatedOfflineTracking = timedBounties.getBoolean("offline-tracking");
        if (!offlineTracking && updatedOfflineTracking) {
            // convert to global time
            Map<UUID, Long> updatedNextBounties = new HashMap<>();
            for (Map.Entry<UUID, Long> entry : nextBounties.entrySet()) {
                // only convert if the player is offline, otherwise the value should already be in global time
                if (!Bukkit.getOfflinePlayer(entry.getKey()).isOnline())
                    updatedNextBounties.put(entry.getKey(), entry.getValue() + System.currentTimeMillis());
            }
            nextBounties = updatedNextBounties;
        } else if (offlineTracking && !updatedOfflineTracking) {
            // convert to local time
            Map<UUID, Long> updatedNextBounties = new HashMap<>();
            for (Map.Entry<UUID, Long> entry : nextBounties.entrySet()) {
                if (!Bukkit.getOfflinePlayer(entry.getKey()).isOnline())
                    updatedNextBounties.put(entry.getKey(), entry.getValue() - System.currentTimeMillis());
            }
            nextBounties = updatedNextBounties;
        }
        offlineTracking = updatedOfflineTracking;
        if (time == 0 || bountyIncrease == 0) {
            nextBounties.clear();
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!nextBounties.containsKey(player.getUniqueId()))
                    nextBounties.put(player.getUniqueId(), System.currentTimeMillis() + time * 1000);
            }
        }
    }

    public static boolean isEnabled() {
        return time != 0 && bountyIncrease != 0;
    }

    public static void update() {
        if (!isEnabled())
            return;
        Map<UUID, Long> nextBountiesCopy = Map.copyOf(nextBounties);
        for (Map.Entry<UUID, Long> entry : nextBountiesCopy.entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            if (!offlineTracking && !player.isOnline())
                continue;
            if (System.currentTimeMillis() > entry.getValue()) {
                if (player.isOnline() && isVanished(Objects.requireNonNull(player.getPlayer())))
                    continue;
                // set bounty
                TaskImplementation<Boolean> checkBounty = NotBounties.getServerImplementation().async().runNow(task -> player.isBanned() || (liteBansEnabled && !new LiteBansClass().isPlayerNotBanned(player.getUniqueId())));
                checkBounty.asFuture().thenRun(() -> NotBounties.getServerImplementation().global().run(nextTask -> {
                    if (Boolean.TRUE.equals(checkBounty.getCallback())) {
                        if (player.isOnline() || offlineTracking)
                            nextBounties.replace(entry.getKey(), System.currentTimeMillis() + time * 1000);
                        else nextBounties.replace(entry.getKey(), time * 1000);
                        if (!hasBounty(player.getUniqueId()) || !isMaxed(Objects.requireNonNull(getBounty(player.getUniqueId())).getTotalDisplayBounty())) {
                            // check immunity
                            if ((ConfigOptions.autoBountyOverrideImmunity || Immunity.getAppliedImmunity(player, bountyIncrease) == Immunity.ImmunityType.DISABLE) && !hasImmunity(player))
                                addBounty(player, bountyIncrease, new ArrayList<>(), new Whitelist(new ArrayList<>(), false));
                        }
                    } else {
                        nextBounties.remove(entry.getKey());
                    }
                }));
            }
        }
    }

    public static boolean isMaxed(double totalBounty) {
        return !(maxBounty == 0 || totalBounty < maxBounty);
    }

    public static void login(Player player) {
        if (time != 0 && bountyIncrease != 0) {
            if (nextBounties.containsKey(player.getUniqueId())) {
                if (!offlineTracking)
                    nextBounties.replace(player.getUniqueId(), nextBounties.get(player.getUniqueId()) + System.currentTimeMillis());
            } else {
                nextBounties.put(player.getUniqueId(), System.currentTimeMillis() + time * 1000);
            }
        }
    }

    public static void onDeath(Player player) {
        if (resetOnDeath && time != 0 && bountyIncrease != 0)
            nextBounties.put(player.getUniqueId(), System.currentTimeMillis() + time * 1000);
    }

    public static void logout(Player player) {
        if (time != 0 && bountyIncrease != 0 && !offlineTracking && nextBounties.containsKey(player.getUniqueId()))
            nextBounties.replace(player.getUniqueId(), nextBounties.get(player.getUniqueId()) - System.currentTimeMillis());
        DataManager.getPlayerData(player.getUniqueId()).setTimedImmunity(player.hasPermission("notbounties.immunity.timed"));
    }
    public static void setNextBounties(Map<UUID, Long> nextBounties) {
        TimedBounties.nextBounties = nextBounties;
    }

    public static Map<UUID, Long> getNextBounties() {
        Map<UUID, Long> timeUntilNextBounty = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : nextBounties.entrySet()) {
            if (offlineTracking || Bukkit.getOfflinePlayer(entry.getKey()).isOnline())
                timeUntilNextBounty.put(entry.getKey(), entry.getValue() - System.currentTimeMillis());
            else
                timeUntilNextBounty.put(entry.getKey(), entry.getValue());
        }
        return timeUntilNextBounty;
    }

    public static long getUntilNextBounty(UUID uuid) {
        if (nextBounties.containsKey(uuid)) {
            if (!offlineTracking && !Bukkit.getOfflinePlayer(uuid).isOnline())
                return nextBounties.get(uuid);
            return nextBounties.get(uuid) - System.currentTimeMillis();
        }
        return -1;
    }

    private static boolean hasImmunity(OfflinePlayer player) {
        if (!Immunity.isPermissionImmunity())
            return false;
        if (player.isOnline())
            return Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immunity.timed");
        return DataManager.getPlayerData(player.getUniqueId()).hasTimedImmunity();
    }
}

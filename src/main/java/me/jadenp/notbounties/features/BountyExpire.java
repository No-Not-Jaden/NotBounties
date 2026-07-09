package me.jadenp.notbounties.features;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.features.LanguageOptions.*;


public class BountyExpire {
    private static long timeMillis;
    private static boolean proportional;
    private static boolean extendExpiration;
    private static boolean offlineTracking;
    private static boolean rewardReceiver;
    private static final Map<UUID, Long> playTimes = new HashMap<>();
    private static final Map<UUID, Long> logonTimes = new HashMap<>();
    private static final long NON_EXPIRING_MILLIS = 365L * 24 * 60 * 60 * 1000;
    private static TaskImplementation<Void> expirationCheckTask;
    private static final long EXPIRATION_CHECK_INTERVAL_MS = 5 * 60 * 1000L; // 5 mins
    private static final Pattern DURATION_PART_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(ms|milliseconds?|s|sec(?:onds?)?|m|mins?|minutes?|h|hrs?|hours?|d|days?|w|weeks?)");

    private BountyExpire(){}

    public static void loadConfiguration(ConfigurationSection configuration) {
        Object configuredTime = configuration.get("time");
        timeMillis = parseDurationToMillis(configuredTime);
        offlineTracking = configuration.getBoolean("offline-tracking");
        rewardReceiver = configuration.getBoolean("reward-receiver");
        proportional = configuration.getBoolean("proportional");
        extendExpiration = configuration.getBoolean("extend-expiration");
        if (expirationCheckTask == null) {
            expirationCheckTask = NotBounties.getServerImplementation().async().runAtFixedRate(() -> BountyExpire.removeExpiredBounties(), EXPIRATION_CHECK_INTERVAL_MS / 50 + 2007, EXPIRATION_CHECK_INTERVAL_MS / 50);
        }
    }

    public static void login(Player player) {
        logonTimes.put(player.getUniqueId(), System.currentTimeMillis());
        playTimes.put(player.getUniqueId(), 0L);
    }

    public static void logout(Player player) {
        if (!logonTimes.containsKey(player.getUniqueId())) {
            NotBounties.debugMessage("No recorded login time for " + player.getName(), false);
            return;
        }
        long timePlayed = System.currentTimeMillis() - logonTimes.get(player.getUniqueId());
        if (playTimes.containsKey(player.getUniqueId())) {
            playTimes.replace(player.getUniqueId(), playTimes.get(player.getUniqueId()) + timePlayed);
        } else {
            playTimes.put(player.getUniqueId(), timePlayed);
        }
        logonTimes.remove(player.getUniqueId());
    }

    public static long getTimePlayed(UUID uuid) {
        if (!playTimes.containsKey(uuid))
            return 0;
        if (logonTimes.containsKey(uuid))
            return playTimes.get(uuid) + (System.currentTimeMillis() - logonTimes.get(uuid));
        return playTimes.get(uuid);
    }
    public static boolean isExpired(Bounty bounty, Setter setter) {
        // the time that matters
        // offlineTracking = true -> time since the bounty was created
        // offlineTracking = false -> change in playtime of receiver since the bounty was set
        long compareTime = getCompareTime(bounty, setter);
        return isExpired(compareTime, bounty, setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID));
    }

    public static long getExpireTime(Bounty bounty, Setter setter) {
        // the time that matters
        // offlineTracking = true -> time since the bounty was created
        // offlineTracking = false -> change in playtime of receiver since the bounty was set
        long compareTime = getCompareTime(bounty, setter);
        return getExpireTime(compareTime, bounty, setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID));
    }

    public static long getLowestExpireTime(Bounty bounty) {
        if (bounty == null)
            return 0;
        if (bounty.getSetters().isEmpty()) {
            DataManager.getLocalData().removeBounty(bounty.getUUID());
            return 0;
        }
        long lowestTime = getExpireTime(bounty, bounty.getSetters().get(0));
        for (int i = 1; i < bounty.getSetters().size(); i++) {
            long expireTime = getExpireTime(bounty, bounty.getSetters().get(i));
            if (expireTime < lowestTime)
                lowestTime = expireTime;
        }
        return lowestTime;
    }

    public static long getHighestExpireTime(Bounty bounty) {
        if (bounty == null)
            return 0;
        if (bounty.getSetters().isEmpty()) {
            DataManager.getLocalData().removeBounty(bounty.getUUID());
            return 0;
        }
        long highestTime = getExpireTime(bounty, bounty.getSetters().get(0));
        for (int i = 1; i < bounty.getSetters().size(); i++) {
            long expireTime = getExpireTime(bounty, bounty.getSetters().get(i));
            if (expireTime > highestTime)
                highestTime = expireTime;
        }
        return highestTime;
    }

    /**
     * Checks if a time is longer than the bounty expire time
     * @param compareTime Time to compare
     * @param autoBounty Whether the bounty should follow auto bounty expire time
     * @return true if the compare time is larger than the expire time
     */
    private static boolean isExpired(long compareTime, Bounty bounty, boolean autoBounty) {
        return getExpireTime(compareTime, bounty, autoBounty) <= 0;
    }

    /**
     * Get the amount of time left before the bounty expires
     * @param compareTime Time to compare
     * @param autoBounty Whether the bounty should follow auto bounty expire time
     * @return the time in milliseconds until the bounty expires, or 1 year if it won't
     */
    private static long getExpireTime(long compareTime, Bounty bounty, boolean autoBounty) {
        if (autoBounty && ConfigOptions.getAutoBounties().getExpireTimeMillis() != -1) {
            if (ConfigOptions.getAutoBounties().getExpireTimeMillis() < -1)
                return NON_EXPIRING_MILLIS; // 1 year (won't expire)
            return ConfigOptions.getAutoBounties().getExpireTimeMillis() - compareTime;
        }
        if (timeMillis <= 0)
            return NON_EXPIRING_MILLIS; // 1 year (won't expire)
        long expireLength = proportional ? (long) (timeMillis * Math.max(0, bounty.getTotalDisplayBounty())) : timeMillis;
        if (expireLength <= 0) {
            return NON_EXPIRING_MILLIS;
        }
        return expireLength - compareTime;
    }

    private static long getCompareTime(Bounty bounty, Setter setter) {
        if (offlineTracking) {
            long startTime = extendExpiration ? bounty.getLatestUpdate() : setter.getTimeCreated();
            return System.currentTimeMillis() - startTime;
        }
        long startPlaytime = setter.getReceiverPlaytime();
        if (extendExpiration) {
            Setter latestSetter = bounty.getLastSetter();
            if (latestSetter != null) {
                startPlaytime = latestSetter.getReceiverPlaytime();
            }
        }
        return getTimePlayed(bounty.getUUID()) - startPlaytime;
    }

    public static long parseDurationToMillis(Object rawTime) {
        if (rawTime instanceof Number number) {
            if (number.doubleValue() < 0) {
                return number.longValue();
            }
            return (long) (number.doubleValue() * 24D * 60 * 60 * 1000);
        }
        if (rawTime == null) {
            return -1;
        }
        String value = rawTime.toString().trim();
        if (value.isEmpty()) {
            return -1;
        }

        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0) {
                return (long) parsed;
            }
            return (long) (parsed * 24D * 60 * 60 * 1000);
        } catch (NumberFormatException ignored) {
            // not a numeric day value
        }

        String lowerValue = value.toLowerCase(Locale.ROOT);
        if (lowerValue.matches("-?\\d+:\\d+(?::\\d+)?")) {
            String[] sections = lowerValue.split(":");
            long hours = 0;
            long minutes;
            long seconds;
            if (sections.length == 3) {
                hours = Long.parseLong(sections[0]);
                minutes = Long.parseLong(sections[1]);
                seconds = Long.parseLong(sections[2]);
            } else {
                minutes = Long.parseLong(sections[0]);
                seconds = Long.parseLong(sections[1]);
            }
            return (hours * 3600 + minutes * 60 + seconds) * 1000;
        }

        String normalizedDuration = lowerValue.replaceAll("[\\s,_-]", "");
        Matcher matcher = DURATION_PART_PATTERN.matcher(normalizedDuration);
        long totalMillis = 0;
        int consumedChars = 0;
        while (matcher.find()) {
            double amount = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);
            totalMillis += (long) (amount * getUnitMillis(unit));
            consumedChars += matcher.group().length();
        }

        if (totalMillis > 0 && consumedChars == normalizedDuration.length()) {
            return totalMillis;
        }

        NotBounties.getInstance().getLogger().warning("Invalid bounty-expire.time value: '" + value + "'. Defaulting to -1.");
        return -1;
    }

    private static long getUnitMillis(String unit) {
        String normalized = unit.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ms", "millisecond", "milliseconds" -> 1;
            case "s", "sec", "second", "seconds" -> 1000L;
            case "m", "min", "mins", "minute", "minutes" -> 60 * 1000L;
            case "h", "hr", "hrs", "hour", "hours" -> 60 * 60 * 1000L;
            case "d", "day", "days" -> 24 * 60 * 60 * 1000L;
            case "w", "week", "weeks" -> 7 * 24 * 60 * 60 * 1000L;
            default -> 0;
        };
    }

    private static void removeExpiredBounties(List<Bounty> bounties) {
        // go through all the bounties and remove setters if it has been more than expire time
        int expiredBounties = 0;
        Map<Bounty, List<Setter>> settersToRemove = new HashMap<>();
        for (Bounty bounty : bounties) {
            List<Setter> expired = refundExpiredBounty(bounty);
            if (!expired.isEmpty()) {
                expiredBounties += expired.size();
                if (settersToRemove.containsKey(bounty)) {
                    settersToRemove.get(bounty).addAll(expired);
                } else {
                    settersToRemove.put(bounty, new ArrayList<>(expired));
                }
            }
        }
        for (Map.Entry<Bounty, List<Setter>> entry : settersToRemove.entrySet()) {
            DataManager.removeSetters(entry.getKey(), entry.getValue());
        }
        if (expiredBounties > 0) {
            NotBounties.debugMessage("Removed " + expiredBounties + " expired bounties", false);
        }

    }

    /**
     * Refund bounty setters that have expired.
     * @param bounty Bounty to refund setters for.
     * @return The list of setters that were refunded.
     */
    private static List<Setter> refundExpiredBounty(Bounty bounty) {
        List<Setter> expired = new ArrayList<>();
        long minExpireTime = NON_EXPIRING_MILLIS;
        for (Setter setter : bounty.getSetters()) {
            long expireTime = getExpireTime(bounty, setter);
            if (expireTime <= 0) {
                if (!setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID)) {
                    // check if setter is online
                    NotBounties.getServerImplementation().global().run(task -> {
                        Player player = Bukkit.getPlayer(setter.getUuid());
                        if (player != null) {
                            player.sendMessage(parse(getPrefix() + getMessage("expired-bounty"), setter.getDisplayAmount(), Bukkit.getOfflinePlayer(bounty.getUUID())));
                        }
                        if (rewardReceiver) {
                            refundPlayer(bounty.getUUID(), setter.getAmount(), setter.getItems(), null);
                        } else {
                            refundSetter(setter, LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-expire"), Bukkit.getOfflinePlayer(bounty.getUUID())));
                        }
                    });
                }
                expired.add(setter);
            } else if (expireTime <= EXPIRATION_CHECK_INTERVAL_MS) {
                minExpireTime = Math.min(minExpireTime, expireTime);
            }
        }
        if (minExpireTime < NON_EXPIRING_MILLIS) {
            // a setter in this bounty will expire soon
            final UUID bountyUUID = bounty.getUUID();
            NotBounties.getServerImplementation().async().runDelayed(() -> {
                Bounty futureBounty = BountyManager.getBounty(bountyUUID);
                if (futureBounty != null) {
                    List<Setter> setters = refundExpiredBounty(futureBounty);
                    DataManager.removeSetters(futureBounty, setters);
                }
            }, minExpireTime / 50 + 2);
        }
        return expired;
    }

    public static synchronized void removeExpiredBounties() {
        if (timeMillis <= 0 && ConfigOptions.getAutoBounties().getExpireTimeMillis() <= 0) {
            return;
        }
        removeExpiredBounties(DataManager.getAllBounties(-1));
    }

}


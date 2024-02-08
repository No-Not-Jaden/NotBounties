package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.ConfigOptions.autoBountyExpireTime;
import static me.jadenp.notbounties.utils.LanguageOptions.*;


public class BountyExpire {
    private static double time;
    private static boolean offlineTracking;
    private static final Map<UUID, Long> playTimes = new HashMap<>();
    private static final Map<UUID, Long> logonTimes = new HashMap<>();
    public static void loadConfiguration(ConfigurationSection configuration) {
        time = configuration.getDouble("time");
        offlineTracking = configuration.getBoolean("offline-tracking");
    }

    public static void login(Player player) {
        logonTimes.put(player.getUniqueId(), System.currentTimeMillis());
        playTimes.put(player.getUniqueId(), 0L);
    }

    public static void logout(Player player) {
        long timePlayed = System.currentTimeMillis() - logonTimes.get(player.getUniqueId());
        if (playTimes.containsKey(player.getUniqueId())) {
            playTimes.replace(player.getUniqueId(), playTimes.get(player.getUniqueId()) + timePlayed);
        } else {
            playTimes.put(player.getUniqueId(), timePlayed);
        }
    }

    public static long getTimePlayed(UUID uuid) {
        if (!playTimes.containsKey(uuid))
            return 0;
        if (logonTimes.containsKey(uuid))
            return playTimes.get(uuid) + (System.currentTimeMillis() - logonTimes.get(uuid));
        return playTimes.get(uuid);
    }
    public static boolean isExpired(UUID receiver, Setter setter) {
        // the time that matters
        // offlineTracking = true -> time since the bounty was created
        // offlineTracking = false -> change in playtime of receiver since the bounty was set
        long compareTime = offlineTracking ? System.currentTimeMillis() - setter.getTimeCreated() : getTimePlayed(receiver) - setter.getReceiverPlaytime();
        return isExpired(compareTime, setter.getUuid().equals(new UUID(0,0)));
    }

    /**
     * Checks if a time is longer than the bounty expire time
     * @param compareTime Time to compare
     * @param autoBounty Whether the bounty should follow auto bounty expire time
     * @return true if the compare time is larger than the expire time
     */
    private static boolean isExpired(long compareTime, boolean autoBounty) {
        if (autoBounty && autoBountyExpireTime != -1) {
            if (autoBountyExpireTime < -1)
                return false;
            return compareTime > autoBountyExpireTime * 1000L * 60 * 60 * 24;
        }
        if (time == -1)
            return false;
        return compareTime > time * 1000L * 60 * 60 * 24;
    }

    public static boolean removeExpiredBounties() {
        if (!(time > 0) && !(autoBountyExpireTime > 0)) {
            return false;
        }
        boolean change = false;
        // go through all the bounties and remove setters if it has been more than expire time
        if (SQL.isConnected()) {
            List<Setter> setters = offlineTracking ? data.removeOldBounties() : data.removeOldPlaytimeBounties();
            for (Setter setter : setters) {
                refundSetter(setter);
                change = true;
            }
        } else {
            ListIterator<Bounty> bountyIterator = bountyList.listIterator();
            while (bountyIterator.hasNext()) {
                Bounty bounty = bountyIterator.next();
                ListIterator<Setter> setterIterator = bounty.getSetters().listIterator();
                while (setterIterator.hasNext()) {
                    Setter setter = setterIterator.next();
                    if (isExpired(bounty.getUUID(), setter)) {
                        if (!setter.getUuid().equals(new UUID(0,0))) {
                            // check if setter is online
                            Player player = Bukkit.getPlayer(setter.getUuid());
                            if (player != null) {
                                player.sendMessage(parse(prefix + expiredBounty, bounty.getName(), setter.getAmount(), player));
                            }
                            refundSetter(setter);
                        }
                        setterIterator.remove();
                        change = true;
                    }
                }
                //bounty.getSetters().removeIf(setter -> System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire);
                // remove bounty if all the setters have been removed
                if (bounty.getSetters().isEmpty()) {
                    bountyIterator.remove();
                    if (NotBounties.wantedText.containsKey(bounty.getUUID())) {
                        NotBounties.wantedText.get(bounty.getUUID()).removeStand();
                        NotBounties.wantedText.remove(bounty.getUUID());
                    }
                }
            }
        }
        return change;
    }

    public static double getTime() {
        return time;
    }

}


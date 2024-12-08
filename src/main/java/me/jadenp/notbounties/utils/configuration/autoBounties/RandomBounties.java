package me.jadenp.notbounties.utils.configuration.autoBounties;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.Immunity;
import me.jadenp.notbounties.utils.externalAPIs.LiteBansClass;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.addBounty;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.liteBansEnabled;

public class RandomBounties {
    private static int randomBountyMinTime;
    private static int randomBountyMaxTime;
    private static double randomBountyMinPrice;
    private static double randomBountyMaxPrice;
    private static boolean randomBountyOfflineSet;
    private static long nextRandomBounty = 0;
    private static final Random random = new Random(System.currentTimeMillis());

    private RandomBounties(){}

    public static void loadConfiguration(ConfigurationSection randomBounties) {
        randomBountyMinTime = randomBounties.getInt("min-time");
        randomBountyMaxTime = randomBounties.getInt("max-time");
        randomBountyMinPrice = randomBounties.getDouble("min-price");
        randomBountyMaxPrice = randomBounties.getDouble("max-price");
        randomBountyOfflineSet = randomBounties.getBoolean("offline-set");

        // make sure amounts are in bounds
        if (randomBountyMaxTime < randomBountyMinTime)
            randomBountyMaxTime = randomBountyMinTime;
        if (randomBountyMaxPrice < randomBountyMinPrice)
            randomBountyMaxPrice = randomBountyMinPrice;

        // stop next random bounty if it is changed
        if (!isEnabled() && nextRandomBounty != 0)
            nextRandomBounty = 0;
        if (isEnabled() && nextRandomBounty == 0)
            setNextRandomBounty();
    }

    public static void update() {
        // random bounties
        if (randomBountyMinTime != 0 && nextRandomBounty != 0 && System.currentTimeMillis() > nextRandomBounty) {
            if (!randomBountyOfflineSet && NotBounties.getNetworkPlayers().isEmpty()) {
                setNextRandomBounty();
                return;
            }
            UUID uuid = randomBountyOfflineSet ? (UUID) NotBounties.loggedPlayers.values().toArray()[random.nextInt(NotBounties.loggedPlayers.values().size())] : (UUID) NotBounties.getNetworkPlayers().keySet().toArray()[random.nextInt(NotBounties.getNetworkPlayers().size())];
            if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
                // this shouldn't be possible, but it's an extra safety measure
                return;
            final double[] price = {randomBountyMinPrice + Math.random() * (randomBountyMaxPrice - randomBountyMinPrice)};
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                // check immunity
                if (!ConfigOptions.autoBountyOverrideImmunity && Immunity.getAppliedImmunity(player, price[0]) != Immunity.ImmunityType.DISABLE || hasImmunity(player))
                    return;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isBanned()) {
                            if (liteBansEnabled) {
                                if (new LiteBansClass().isPlayerNotBanned(player.getUniqueId())) {
                                    // back into sync thread
                                    if (!NumberFormatting.shouldUseDecimals())
                                        price[0] = (long) price[0];
                                    double finalPrice = price[0];
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            addBounty(player, finalPrice, new ArrayList<>(), new Whitelist(new ArrayList<>(), false));
                                        }
                                    }.runTask(NotBounties.getInstance());
                                }
                            } else {
                                if (!NumberFormatting.shouldUseDecimals())
                                    price[0] = (long) price[0];
                                double finalPrice = price[0];
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        addBounty(player, finalPrice, new ArrayList<>(), new Whitelist(new ArrayList<>(), false));
                                    }
                                }.runTask(NotBounties.getInstance());
                            }
                            setNextRandomBounty();
                        }
                    }
                }.runTaskAsynchronously(NotBounties.getInstance());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().info("[NotBounties] Invalid UUID of picked player for random bounty: " + uuid);
            }
        }
    }

    public static boolean isEnabled() {
        return randomBountyMinTime != 0;
    }

    public static long getNextRandomBounty() {
        return nextRandomBounty;
    }

    public static void setNextRandomBounty(long nextRandomBounty) {
        RandomBounties.nextRandomBounty = nextRandomBounty;
    }

    public static void setNextRandomBounty() {
        nextRandomBounty = System.currentTimeMillis() + randomBountyMinTime * 1000L + (random.nextInt(randomBountyMaxTime - randomBountyMinTime) * 1000L);
    }

    private static boolean hasImmunity(OfflinePlayer player) {
        if (player.isOnline())
            return Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immunity.random");
        return NotBounties.autoImmuneRandomPerms.contains(player.getUniqueId().toString());
    }
}

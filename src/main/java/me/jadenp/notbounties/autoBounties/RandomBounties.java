package me.jadenp.notbounties.autoBounties;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.LiteBansClass;
import me.jadenp.notbounties.utils.NumberFormatting;
import me.jadenp.notbounties.utils.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.UUID;

import static me.jadenp.notbounties.utils.BountyManager.addBounty;
import static me.jadenp.notbounties.utils.ConfigOptions.liteBansEnabled;

public class RandomBounties {
    private static int randomBountyMinTime;
    private static int randomBountyMaxTime;
    private static double randomBountyMinPrice;
    private static double randomBountyMaxPrice;
    private static boolean randomBountyOfflineSet;
    private static long nextRandomBounty = 0;
    public static void loadConfiguration(ConfigurationSection randomBounties) {
        randomBountyMinTime = randomBounties.getInt("min-time");
        randomBountyMaxTime = randomBounties.getInt("max-time");
        randomBountyMinPrice = randomBounties.getDouble("min-price");
        randomBountyMaxPrice = randomBounties.getDouble("max-price");
        randomBountyOfflineSet = randomBounties.getBoolean("offline-set");

        // stop next random bounty if it is changed
        if (!isRandomBountiesEnabled() && nextRandomBounty != 0)
            nextRandomBounty = 0;
        if (isRandomBountiesEnabled() && nextRandomBounty == 0)
            setNextRandomBounty();
    }

    public static void update() {
        // random bounties
        if (randomBountyMinTime != 0 && nextRandomBounty != 0 && System.currentTimeMillis() > nextRandomBounty) {
            if (!randomBountyOfflineSet && NotBounties.getNetworkPlayers().isEmpty()) {
                setNextRandomBounty();
                return;
            }
            UUID uuid = randomBountyOfflineSet ? (UUID) NotBounties.loggedPlayers.values().toArray()[(int) (Math.random() * NotBounties.loggedPlayers.values().size())] : ((OfflinePlayer) NotBounties.getNetworkPlayers().toArray()[(int) (Math.random() * NotBounties.getNetworkPlayers().size())]).getUniqueId();
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isBanned()) {
                            if (liteBansEnabled) {
                                if (new LiteBansClass().isPlayerNotBanned(player.getUniqueId())) {
                                    // back into sync thread
                                    double price = randomBountyMinPrice + Math.random() * (randomBountyMaxPrice - randomBountyMinPrice);
                                    if (NumberFormatting.shouldUseDecimals())
                                        price = (long) price;
                                    double finalPrice = price;
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            addBounty(player, finalPrice, new Whitelist(new ArrayList<>(), false));
                                        }
                                    }.runTask(NotBounties.getInstance());
                                }
                            } else {
                                double price = randomBountyMinPrice + Math.random() * (randomBountyMaxPrice - randomBountyMinPrice);
                                if (NumberFormatting.shouldUseDecimals())
                                    price = (long) price;
                                double finalPrice = price;
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        addBounty(player, finalPrice, new Whitelist(new ArrayList<>(), false));
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

    public static boolean isRandomBountiesEnabled() {
        return randomBountyMinTime != 0;
    }

    public static long getNextRandomBounty() {
        return nextRandomBounty;
    }

    public static void setNextRandomBounty(long nextRandomBounty) {
        RandomBounties.nextRandomBounty = nextRandomBounty;
    }

    public static void setNextRandomBounty() {
        nextRandomBounty = System.currentTimeMillis() + randomBountyMinTime * 1000L + (long) (Math.random() * (randomBountyMaxTime - randomBountyMinTime) * 1000L);
        //Bukkit.getLogger().info(nextRandomBounty + "");
    }
}

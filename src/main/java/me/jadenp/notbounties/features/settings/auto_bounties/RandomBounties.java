package me.jadenp.notbounties.features.settings.auto_bounties;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.external_api.LiteBansClass;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.data.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.function.Consumer;

import static me.jadenp.notbounties.utils.BountyManager.addBounty;

public class RandomBounties {
    /**
     * The minimum amount of time between random bounties.
     * 0 means disabled.
     */
    private static int randomBountyMinTime;
    /**
     * The maximum amount of time between random bounties.
     */
    private static int randomBountyMaxTime;
    /**
     * The minimum price of a random bounty.
     */
    private static double randomBountyMinPrice;
    /**
     * The maximum price of a random bounty.
     */
    private static double randomBountyMaxPrice;
    /**
     * Whether offline players can get random bounties set on them.
     */
    private static boolean randomBountyOfflineSet;
    /**
     * The time in milliseconds when the next random bounty should be set.
     */
    private static long nextRandomBounty = 0;
    /**
     * The random number generator used to generate random amounts.
     */
    private static final Random random = new Random();

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
            UUID uuid = randomBountyOfflineSet ? (UUID) LoggedPlayers.getLoggedPlayers().keySet().toArray()[random.nextInt(LoggedPlayers.getLoggedPlayers().size())] : (UUID) NotBounties.getNetworkPlayers().keySet().toArray()[random.nextInt(NotBounties.getNetworkPlayers().size())];
            if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
                // this shouldn't be possible, but it's an extra safety measure
                return;
            final double[] price = {randomBountyMinPrice + Math.random() * (randomBountyMaxPrice - randomBountyMinPrice)};
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                // check immunity
                if (!ConfigOptions.getAutoBounties().isOverrideImmunity() && ImmunityManager.getAppliedImmunity(player.getUniqueId(), price[0]) != ImmunityManager.ImmunityType.DISABLE || hasImmunity(player))
                    return;
                NotBounties.getServerImplementation().async().runNow(task -> {
                    if (!player.isBanned() && (!ConfigOptions.getIntegrations().isLiteBansEnabled() || new LiteBansClass().isPlayerNotBanned(player.getUniqueId()))) {
                        if (!NumberFormatting.shouldUseDecimals()) {
                            price[0] = (long) price[0];
                        }
                        double finalPrice = price[0];

                        NotBounties.getServerImplementation().global().run((Consumer<TaskImplementation<Void>>) task1 ->
                                addBounty(player, finalPrice, new ArrayList<>(), new Whitelist(new ArrayList<>(), false)));

                        setNextRandomBounty();
                    }
                });

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
        nextRandomBounty = System.currentTimeMillis() + randomBountyMinTime * 1000L + (random.nextInt(Math.abs(randomBountyMaxTime - randomBountyMinTime)) * 1000L);
    }

    private static boolean hasImmunity(OfflinePlayer player) {
        if (!ImmunityManager.isPermissionImmunity())
            return false;
        if (player.isOnline())
            return Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immunity.random");
        return DataManager.getPlayerData(player.getUniqueId()).hasRandomImmunity();
    }
}

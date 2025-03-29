package me.jadenp.notbounties.utils.external_api;

import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class DuelsClass {

    private DuelsClass(){}

    private static int teleportDelay;
    private static boolean enabled;
    private static boolean claimBounties;
    private static boolean murderBounty;
    private static boolean delayReward;
    private static final String PLUGIN_NAME = "Duels";

    /**
     * Read the duels config for their teleport delay.
     */
    public static void readConfig() {
        try {
            me.realized.duels.api.Duels duels = (me.realized.duels.api.Duels) Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (duels != null) {
                duels.reloadConfig();
                teleportDelay = duels.getConfig().getInt("duel.teleport-delay", 5);
            }
        } catch (NoClassDefFoundError | ClassCastException e) {
            com.meteordevelopments.duels.api.Duels duels = (com.meteordevelopments.duels.api.Duels) Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (duels != null) {
                duels.reloadConfig();
                teleportDelay = duels.getConfig().getInt("duel.teleport-delay", 5);
            }
        }

    }

    /**
     * Read the NotBounties config for integration settings.
     * @param config Duels configuration section.
     */
    public static void loadConfiguration(ConfigurationSection config) {
        enabled = config.getBoolean("enabled");
        claimBounties = config.getBoolean("claim-bounties");
        murderBounty = config.getBoolean("murder-bounty");
        delayReward = config.getBoolean("delay-reward");
    }

    public static int getTeleportDelay() {
        return teleportDelay;
    }

    public static boolean isInDuel(Player player) {
        try {
            me.realized.duels.api.Duels duels = (me.realized.duels.api.Duels) Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (duels != null) {
                return duels.getArenaManager().isInMatch(player);
            }
        } catch (NoClassDefFoundError | ClassCastException e) {
            com.meteordevelopments.duels.api.Duels duels = (com.meteordevelopments.duels.api.Duels) Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (duels != null) {
                return duels.getArenaManager().isInMatch(player);
            }
        }
        return false;
    }

    /**
     * Check if the Duels integration is enabled in the config.
     * {@link ConfigOptions#isDuelsEnabled()} should be used to check if the plugin is enabled.
     * @return True if the Duels integration is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isClaimBounties() {
        return claimBounties;
    }

    public static boolean isMurderBounty() {
        return murderBounty;
    }

    public static boolean isDelayReward() {
        return delayReward;
    }
}

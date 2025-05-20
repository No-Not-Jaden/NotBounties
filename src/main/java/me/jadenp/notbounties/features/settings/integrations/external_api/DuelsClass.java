package me.jadenp.notbounties.features.settings.integrations.external_api;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class DuelsClass {

    private static int teleportDelay;
    private boolean enabled;
    private boolean claimBounties;
    private boolean murderBounty;
    private boolean delayReward;
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
    public void loadConfiguration(ConfigurationSection config) {
        enabled = config.getBoolean("enabled");
        claimBounties = config.getBoolean("claim-bounties");
        murderBounty = config.getBoolean("murder-bounty");
        delayReward = config.getBoolean("delay-reward");
    }

    public int getTeleportDelay() {
        return teleportDelay;
    }

    public boolean isInDuel(Player player) {
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
     * @return True if the Duels integration is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isClaimBounties() {
        return claimBounties;
    }

    public boolean isMurderBounty() {
        return murderBounty;
    }

    public boolean isDelayReward() {
        return delayReward;
    }
}

package me.jadenp.notbounties.features.settings.integrations.external_api;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

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
            try {
                Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
                if (plugin != null) {
                    Method reloadConfigMethod = plugin.getClass().getMethod("reloadConfig");
                    reloadConfigMethod.invoke(plugin);

                    Method getConfigMethod = plugin.getClass().getMethod("getConfig");
                    Object config = getConfigMethod.invoke(plugin);

                    Method getIntMethod = config.getClass().getMethod("getInt", String.class, int.class);
                    teleportDelay = (int) getIntMethod.invoke(config, "duel.teleport-delay", 5);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
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
            // Use reflection for newer Duels API to avoid compile-time dependency
            try {
                Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
                if (plugin != null) {
                    Method getArenaManagerMethod = plugin.getClass().getMethod("getArenaManager");
                    Object arenaManager = getArenaManagerMethod.invoke(plugin);

                    Method isInMatchMethod = arenaManager.getClass().getMethod("isInMatch", Player.class);
                    return (boolean) isInMatchMethod.invoke(arenaManager, player);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
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

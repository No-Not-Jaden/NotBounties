package me.jadenp.notbounties.features.settings.databases.proxy;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.settings.databases.*;
import me.jadenp.notbounties.utils.BountyChange;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class ProxySettings {

    /**
     * Whether proxy communication should occur.
     */
    private static boolean enabled = false;
    private static Map<UUID, String> databaseOnlinePlayers = new HashMap<>();
    private static long lastOnlinePlayersCheck = 0;
    private static boolean databaseSynchronization = false;
    private static boolean skins = false;
    private static boolean registeredListener = false;

    public static boolean isDatabaseSynchronization() {
        return databaseSynchronization;
    }



    public static void setDatabaseOnlinePlayers(Map<UUID, String> databaseOnlinePlayers) {
        ProxySettings.databaseOnlinePlayers = databaseOnlinePlayers;
    }

    private static void requestPlayerList() {
        if (System.currentTimeMillis() - lastOnlinePlayersCheck > 20000) {
            ProxyMessaging.requestPlayerList();
            lastOnlinePlayersCheck = System.currentTimeMillis();
        }
    }

    public static boolean isConnected() {
        return enabled && databaseSynchronization && ProxyMessaging.hasConnectedBefore() && !DataManager.getLocalOnlinePlayers().isEmpty();
    }

    public Map<UUID, String> getOnlinePlayers() throws IOException {
        requestPlayerList();
        return databaseOnlinePlayers;
    }


    public static void readConfig(ConfigurationSection configuration) {
        enabled = configuration.getBoolean("enabled", false);
        databaseSynchronization = configuration.getBoolean("database-sync", false);
        skins = configuration.getBoolean("skins", true);
        if (enabled && !registeredListener) {
            // Proxy messaging is enabled, but the listeners haven't been registered.
            // Register plugin message listeners.
            Plugin plugin = NotBounties.getInstance();
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "notbounties:main");
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "notbounties:main", new ProxyMessaging());
            setRegisteredListener(true);
        } else if (!enabled && registeredListener) {
            // Proxy messaging is disabled, but listeners are registered.
            // Unregister message listeners.
            Plugin plugin = NotBounties.getInstance();
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
            setRegisteredListener(false);
        }
    }

    private static void setEnabled(boolean enabled) {
        ProxySettings.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static void setDatabaseSynchronization(boolean databaseSynchronization) {
        ProxySettings.databaseSynchronization = databaseSynchronization;
    }

    private static void setSkins(boolean skins) {
        ProxySettings.skins = skins;
    }

    public static boolean areSkinRequestsEnabled() {
        return enabled && skins;
    }

    private static void setRegisteredListener(boolean registeredListener) {
        ProxySettings.registeredListener = registeredListener;
    }





}

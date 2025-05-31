package me.jadenp.notbounties.features.settings.integrations;

import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import me.jadenp.notbounties.features.settings.integrations.external_api.*;
import me.jadenp.notbounties.features.settings.integrations.external_api.worldguard.WorldGuardClass;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class Integrations extends ResourceConfiguration {
    private boolean mmoLibEnabled;
    private boolean duelsEnabled;
    private final DuelsClass duelsClass = new DuelsClass();
    private SkinsRestorerClass skinsRestorerClass;
    private boolean papiEnabled;
    private boolean liteBansEnabled;
    private boolean geyserEnabled;
    private boolean skinsRestorerEnabled;
    private boolean floodgateEnabled;
    private boolean headDataBaseEnabled;
    private boolean worldGuardEnabled;
    private boolean packetEventsEnabled;

    public static void onLoad(Plugin plugin) {
        // register api flags
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) WorldGuardClass.registerFlags();
        if (plugin.getServer().getPluginManager().getPlugin("Lands") != null) new LandsClass().registerClaimFlag();
    }

    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        BountyClaimRequirements.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("teams")));

        mmoLibEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicLib");
        if (mmoLibEnabled)
            MMOLibClass.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("MMOLib")));

        duelsEnabled = Bukkit.getPluginManager().isPluginEnabled("Duels");
        if (duelsEnabled) {
            DuelsClass.readConfig();
            duelsClass.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("Duels")));
        }

        skinsRestorerEnabled = Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer");
        if (skinsRestorerEnabled) {
            if (config.getBoolean("override-skinsrestorer")) {
                Bukkit.getLogger().info("[NotBounties] NotBounties will be using its own methods to get player skins instead of SkinsRestorer.");
                skinsRestorerEnabled = false;
            } else {
                skinsRestorerClass = new SkinsRestorerClass();
            }
        }


        papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        liteBansEnabled = Bukkit.getPluginManager().isPluginEnabled("LiteBans");
        floodgateEnabled = Bukkit.getPluginManager().isPluginEnabled("floodgate");
        geyserEnabled = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot");
        headDataBaseEnabled = Bukkit.getPluginManager().isPluginEnabled("HeadDataBase");
        worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        packetEventsEnabled = Bukkit.getPluginManager().isPluginEnabled("packetevents");
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[0];
    }

    @Override
    protected String getPath() {
        return "settings/integrations.yml";
    }

    public boolean isMmoLibEnabled() {
        return mmoLibEnabled;
    }

    /**
     * Check if the Duels plugin is enabled on the server.
     * @return True if the Duels plugin is enabled.
     */
    public boolean isDuelsEnabled() {
        return duelsEnabled && duelsClass.isEnabled();
    }

    public DuelsClass getDuels() {
        return duelsClass;
    }

    public boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }

    public boolean isGeyserEnabled() {
        return geyserEnabled;
    }

    public boolean isLiteBansEnabled() {
        return liteBansEnabled;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }

    public boolean isSkinsRestorerEnabled() {
        return skinsRestorerEnabled;
    }

    public SkinsRestorerClass getSkinsRestorerClass() {
        return skinsRestorerClass;
    }

    public boolean isHeadDataBaseEnabled() {
        return headDataBaseEnabled;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public boolean isPacketEventsEnabled() {
        return packetEventsEnabled;
    }
}

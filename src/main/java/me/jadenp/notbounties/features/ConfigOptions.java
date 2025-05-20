package me.jadenp.notbounties.features;


import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.RewardHead;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.settings.display.Display;
import me.jadenp.notbounties.features.settings.immunity.Immunity;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.Integrations;
import me.jadenp.notbounties.features.settings.money.Money;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIClicks;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUI;
import me.jadenp.notbounties.features.settings.display.map.BountyMap;
import me.jadenp.notbounties.features.settings.auto_bounties.AutoBounties;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.settings.databases.Databases;
import me.jadenp.notbounties.features.webhook.WebhookOptions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.*;


public class ConfigOptions {
    private static final AutoBounties autoBounties = new AutoBounties();
    private static final Databases databases = new Databases();
    private static final Display display = new Display();
    private static final Immunity immunity = new Immunity();
    private static final Integrations integrations = new Integrations();
    private static final Money money = new Money();

    private static List<String> hiddenNames = new ArrayList<>();
    private static String updateNotification;
    private static boolean npcClaim;
    private static boolean worldFilter;
    private static List<String> worldFilterNames = new ArrayList<>();
    private static int maxSetters;
    private static DateFormat dateFormat;
    private static boolean bountyConfirmation;
    private static boolean removeBannedPlayers;
    private static boolean firstStart = true;
    private static boolean bountyBackups;
    private static boolean offlineSet;

    public enum ClaimOrder {
        BEFORE, REGULAR, AFTER
    }

    private static ClaimOrder claimOrder;
    private static boolean sendBStats;
    private static boolean selfSetting;
    private static boolean autoTimezone;
    private static boolean reducePageCalculations;
    private static boolean seePlayerList;
    private static boolean stealBounties;
    private static List<String> pluginBountyCommands;
    private static final String[] modifiableSections = new String[]{"number-formatting.divisions", "wanted-tag.level", "databases", "MMOLib"};
    private static long bountyCooldown;
    private static int maxTabCompletePlayers;
    private static int autoSaveInterval;
    private static boolean sameIPClaim;

    public static void reloadOptions() throws IOException {
        BountyMap.loadFont();
        NotBounties bounties = NotBounties.getInstance();

        bounties.reloadConfig();

        // create configuration sections for each external team plugin
        if (bounties.getConfig().isBoolean("teams.bt-claim")) {
            bounties.getConfig().set("teams.better-teams.team", bounties.getConfig().getBoolean("teams.bt-claim"));
            bounties.getConfig().set("teams.better-teams.ally", bounties.getConfig().getBoolean("teams.bt-allies"));
            bounties.getConfig().set("teams.towny-advanced.nation", bounties.getConfig().getBoolean("teams.towny-nation"));
            bounties.getConfig().set("teams.towny-advanced.town", bounties.getConfig().getBoolean("teams.towny-town"));
            bounties.getConfig().set("teams.towny-advanced.ally", bounties.getConfig().getBoolean("teams.towny-allies"));
            bounties.getConfig().set("teams.bt-claim", null);
            bounties.getConfig().set("teams.bt-allies", null);
            bounties.getConfig().set("teams.towny-nation", null);
            bounties.getConfig().set("teams.towny-town", null);
            bounties.getConfig().set("teams.towny-allies", null);
        }

        // move saber-factions to just factions
        if (bounties.getConfig().isSet("teams.saber-factions.faction")) {
            bounties.getConfig().set("teams.factions.faction", bounties.getConfig().getBoolean("teams.saber-factions.faction"));
            bounties.getConfig().set("teams.factions.ally", bounties.getConfig().getBoolean("teams.saber-factions.ally"));
            bounties.getConfig().set("teams.saber-factions.faction", null);
            bounties.getConfig().set("teams.saber-factions.ally", null);
        }

        // convert use-item-values to item-vales
        if (bounties.getConfig().isBoolean("currency.bounty-items.use-item-values")) {
            if (bounties.getConfig().getBoolean("currency.bounty-items.use-item-values")) {
                bounties.getConfig().set("currency.bounty-items.item-values", "FILE");
            } else {
                bounties.getConfig().set("currency.bounty-items.item-values", "DISABLE");
            }
            bounties.getConfig().set("currency.bounty-items.use-item-values", null);
        }

        // convert database to databases
        if (bounties.getConfig().isSet("database")) {
            bounties.getConfig().set("databases.example-sql.type", "SQL");
            bounties.getConfig().set("databases.example-sql.host", bounties.getConfig().getString("database.host"));
            bounties.getConfig().set("databases.example-sql.port", bounties.getConfig().getInt("database.port"));
            bounties.getConfig().set("databases.example-sql.database", bounties.getConfig().getString("database.database"));
            bounties.getConfig().set("databases.example-sql.user", bounties.getConfig().getString("database.user"));
            bounties.getConfig().set("databases.example-sql.password", bounties.getConfig().getString("database.password"));
            bounties.getConfig().set("databases.example-sql.ssl", bounties.getConfig().getBoolean("database.use-ssl"));
            bounties.getConfig().set("databases.example-sql.refresh-interval", 300);
            bounties.getConfig().set("databases.example-sql.priority", 1);
            bounties.getConfig().set("database", null);
        }

        // add extra options to the proxy database
        if (bounties.getConfig().isSet("databases.example-proxy.type") && !bounties.getConfig().isSet("databases.example-proxy.skins")) {
            bounties.getConfig().set("databases.example-proxy.skins", true);
            bounties.getConfig().set("databases.example-proxy.database-sync", true);
        }

        // set permission immunity to true if updating
        if (!bounties.getConfig().isSet("immunity.permission-immunity")) {
            bounties.getConfig().set("immunity.permission-immunity", true);
        }

        boolean saveChanges = true;
        if (bounties.getConfig().getKeys(true).size() <= 2) {
            saveChanges = false;
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the config.yml file. Fix the YAML formatting errors, or the plugin may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
        } else {
            migrate120Config(bounties);
        }



        // fill in any default options that aren't present
        if (NotBounties.getInstance().getResource("config.yml") != null) {
            bounties.getConfig().setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("config.yml")))));
            for (String key : Objects.requireNonNull(bounties.getConfig().getDefaults()).getKeys(true)) {
                if (Arrays.stream(modifiableSections).anyMatch(key::startsWith))
                    continue;
                if (!bounties.getConfig().isSet(key))
                    bounties.getConfig().set(key, bounties.getConfig().getDefaults().get(key));
            }
        }


        PVPRestrictions.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("pvp-restrictions")));
        BountyExpire.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("bounty-expire")));
        GUIClicks.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("bounty-gui-clicks")));
        Whitelist.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("bounty-whitelist")));
        RewardHead.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("reward-heads")));

        money.loadFile(bounties);
        immunity.loadFile(bounties);
        integrations.loadFile(bounties);
        autoBounties.loadFile(bounties);
        databases.loadFile(bounties);

        if (!firstStart) {
            ImmunityManager.loadPlayerData();
        }

        hiddenNames = bounties.getConfig().getStringList("hide-stats");
        updateNotification = bounties.getConfig().getString("update-notification");
        npcClaim = bounties.getConfig().getBoolean("npc-claim");
        worldFilter = bounties.getConfig().getBoolean("world-filter.whitelist");
        worldFilterNames = bounties.getConfig().getStringList("world-filter.worlds");
        maxSetters = bounties.getConfig().getInt("max-setters");
        bountyConfirmation = bounties.getConfig().getBoolean("confirmation");
        removeBannedPlayers = bounties.getConfig().getBoolean("remove-banned-players");
        try {
            claimOrder = ClaimOrder.valueOf(Objects.requireNonNull(bounties.getConfig().getString("claim-order")).toUpperCase());
        } catch (IllegalArgumentException e) {
            claimOrder = ClaimOrder.REGULAR;
            Bukkit.getLogger().warning("[NotBounties] claim-order is not set to a proper value!");
        }
        sendBStats = bounties.getConfig().getBoolean("send-bstats");
        autoTimezone = bounties.getConfig().getBoolean("auto-timezone");
        reducePageCalculations = bounties.getConfig().getBoolean("reduce-page-calculations");
        seePlayerList = bounties.getConfig().getBoolean("see-player-list");
        stealBounties = bounties.getConfig().getBoolean("steal-bounties");
        pluginBountyCommands = bounties.getConfig().getStringList("plugin-bounty-commands");
        if (pluginBountyCommands.isEmpty())
            pluginBountyCommands.add("notbounties");
        if (firstStart)
            registerAliases(pluginBountyCommands);
        bountyBackups = bounties.getConfig().getBoolean("bounty-backups");
        bountyCooldown = bounties.getConfig().getLong("bounty-cooldown");
        maxTabCompletePlayers = bounties.getConfig().getInt("max-tab-complete-players");
        autoSaveInterval = bounties.getConfig().getInt("auto-save-interval");
        offlineSet = bounties.getConfig().getBoolean("offline-set");
        sameIPClaim = bounties.getConfig().getBoolean("same-ip-claim");
        selfSetting = bounties.getConfig().getBoolean("self-setting");

        dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.getLocale());

        File guiFile = new File(bounties.getDataFolder() + File.separator + "gui.yml");
        if (!guiFile.exists()) {
            bounties.saveResource("gui.yml", false);
        }
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        if (guiConfig.getKeys(true).size() <= 2) {
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the gui.yml file. Fix the YAML formatting errors, or the GUI may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            if (NotBounties.getInstance().getResource("gui.yml") != null) {
                guiConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("gui.yml"))));
                GUI.loadConfiguration(guiConfig);
            }
        } else {
            GUI.loadConfiguration(guiConfig);
        }


        for (String key : guiConfig.getKeys(false)) {
            if (key.equals("custom-items"))
                continue;
            try {
                GUI.addGUI(new GUIOptions(Objects.requireNonNull(guiConfig.getConfigurationSection(key))), key);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Unknown GUI in gui.yml: \"" + key + "\"");
            }
        }

        if (saveChanges)
            bounties.saveConfig();
        LanguageOptions.reloadOptions();
        WebhookOptions.reloadOptions();
        SkinManager.refreshSkinRequests();
        ChallengeManager.reloadOptions();
        if (integrations.isFloodgateEnabled() || integrations.isGeyserEnabled())
            BedrockGUI.reloadOptions();

        display.loadFile(bounties); // bounty tracker needs to be loaded later
        firstStart = false;
    }

    private static void registerAliases(List<String> aliases) {
        aliases = new ArrayList<>(aliases);
        aliases.removeIf(s -> s.equalsIgnoreCase("notbounties")); // this is already the command name
        PluginCommand command = NotBounties.getInstance().getCommand("notbounties");
        if (command == null) {
            Bukkit.getLogger().warning("[NotBounties] Error finding bounty command to register aliases.");
            return;
        }
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            for (String alias : aliases) {
                commandMap.register(alias, "notbounties", command);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Bukkit.getLogger().warning("[NotBounties] Error adding command aliases");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    /**
     * Saves the default configuration section from a resource to a YamlConfiguration.
     * @param resourceName The resource to obtain the defaults from.
     * @param configuration The configuration to save the defaults to.
     * @param section The section to be saved.
     */
    public static void saveConfigurationSection(String resourceName, YamlConfiguration configuration, String section) {
        if (NotBounties.getInstance().getResource(resourceName) != null) {
            YamlConfiguration resourceConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource(resourceName))));
            if (resourceConfig.isConfigurationSection(section))
                for (String key : Objects.requireNonNull(resourceConfig.getConfigurationSection(section)).getKeys(true)) {
                    configuration.set(section + "." + key, resourceConfig.get(section + "." + key));
                }
        }
    }

    /**
     * Migrates a configuration section to a new file. Recommended to use with a fresh configuration in the new file.
     *
     * @param prevFile YamlConfiguration of the previous file.
     * @param prevPath Path to a key to migrate from the old file. Can be a configuration section.
     * @param newFile YamlConfiguration of the new file.
     * @param newPath Path to a key to migrate to the new file. Can be a configuration section.
     */
    private static void migrateConfigSection(YamlConfiguration prevFile, String prevPath, YamlConfiguration newFile, String newPath) {
        if (!prevFile.isSet(prevPath)) {
            // nothing set in the previous file, so don't migrate anything
            return;
        }

        if (Arrays.asList(modifiableSections).contains(prevPath)) {
            if (newPath.isEmpty()) {
                // delete everything
                for (String key : newFile.getKeys(false))
                    newFile.set(key, null);
            } else {
                newFile.set(newPath, null);
            }
        }


        if (prevFile.isConfigurationSection(prevPath)) {
            // erase any modifiable sub sections
            for (String key : Objects.requireNonNull(prevFile.getConfigurationSection(prevPath)).getKeys(true)) {
                if (Arrays.asList(modifiableSections).contains((prevPath + "." + key))) {
                    String name = newPath.isEmpty() ? key : newPath + "." + key;
                    newFile.set(name, null);
                }
            }
            // copy sections over
            for (String key : Objects.requireNonNull(prevFile.getConfigurationSection(prevPath)).getKeys(true)) {
                if (!prevFile.isConfigurationSection(prevPath + "." + key)) {
                    String name = newPath.isEmpty() ? key : newPath + "." + key;
                    newFile.set(name, prevFile.get(prevPath + "." + key));
                }
            }
        } else {
            if (!newPath.isEmpty()) newFile.set(newPath, prevFile.get(prevPath));
        }

        prevFile.set(prevPath, null);
    }

    /**
     * Migrate a configuration section from the config.yml file to a new resource file.
     *
     * @param plugin Plugin containing the config files.
     * @param resourcePath Path to the resource from the plugin's data folder.
     * @param pathMap Map of previous paths in the config file to new paths in the resource file.
     */
    private static void migrateConfigResource(Plugin plugin, String resourcePath, Map<String, String> pathMap) {
        // generate the new resource file if it doesn't exist
        File resourceFile = new File(plugin.getDataFolder() + File.separator + resourcePath);
        if (!resourceFile.exists()) {
            plugin.getLogger().info("Generated a new " + resourcePath + " file.");
            plugin.saveResource(resourcePath, false);
        }

        YamlConfiguration prevFile = (YamlConfiguration) plugin.getConfig();
        YamlConfiguration newFile = YamlConfiguration.loadConfiguration(resourceFile);
        // safety check for yaml errors
        if (newFile.getKeys(true).size() <= 2 || prevFile.getKeys(true).size() <= 2) {
            return;
        }

        // migrate paths
        for (Map.Entry<String, String> entry : pathMap.entrySet()) {
            migrateConfigSection(prevFile, entry.getKey(), newFile, entry.getValue());
        }

        // save
        try {
            newFile.save(resourceFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to migrate config to resource files.");
            plugin.getLogger().warning(e.toString());
        }
    }

    private static void migrate120Config(Plugin plugin) {
        File settingsFolder = new File(plugin.getDataFolder() + File.separator + "settings");
        if (settingsFolder.mkdir())
            plugin.getLogger().info("Created settings folder.");

        Map<String, String> pathMap = new HashMap<>();
        pathMap.put("console-bounty-name", "console-bounty-name");
        pathMap.put("auto-bounties", "");
        pathMap.put("trickle-bounties", "trickle-bounties");
        pathMap.put("big-bounties", "big-bounties");
        pathMap.put("bounty-claim-commands", "bounty-claim-commands");
        pathMap.put("bounty-set-commands", "bounty-set-commands");
        pathMap.put("blocked-bounty-commands", "blocked-bounty-commands");
        pathMap.put("prompts", "prompts");
        migrateConfigResource(plugin, "settings/auto-bounties.yml", pathMap);

        pathMap.clear();
        pathMap.put("databases", "");
        migrateConfigResource(plugin, "settings/databases.yml", pathMap);

        pathMap.clear();
        pathMap.put("bounty-posters", "bounty-posters");
        pathMap.put("bounty-board", "bounty-board");
        pathMap.put("bounty-tracker", "bounty-tracker");
        pathMap.put("wanted-tag", "wanted-tag");
        migrateConfigResource(plugin, "settings/display.yml", pathMap);

        pathMap.clear();
        pathMap.put("immunity", "");
        migrateConfigResource(plugin, "settings/immunity.yml", pathMap);

        pathMap.clear();
        pathMap.put("override-skinsrestorer", "override-skinsrestorer");
        pathMap.put("teams", "teams");
        pathMap.put("MMOLib", "MMOLib");
        pathMap.put("Duels", "Duels");
        migrateConfigResource(plugin, "settings/integrations.yml", pathMap);

        pathMap.clear();
        pathMap.put("minimum-bounty", "minimum-bounty");
        pathMap.put("maximum-bounty", "maximum-bounty");
        pathMap.put("minimum-broadcast", "minimum-broadcast");
        pathMap.put("bounty-tax", "bounty-tax");
        pathMap.put("death-tax", "death-tax");
        pathMap.put("currency", "currency");
        pathMap.put("number-formatting", "number-formatting");
        pathMap.put("redeem-reward-later.enabled", "redeem-reward-later.vouchers");
        pathMap.put("redeem-reward-later.voucher-per-setter", "redeem-reward-later.voucher-per-setter");
        pathMap.put("redeem-reward-later.setter-lore-addition", "redeem-reward-later.setter-lore-addition");
        pathMap.put("reward-delay", "redeem-reward-later.reward-delay");
        pathMap.put("buy-own-bounties", "buy-own-bounties");
        migrateConfigResource(plugin, "settings/money.yml", pathMap);
    }

    public static String getUpdateNotification() {
        return updateNotification;
    }

    public static void setUpdateNotification(String updateNotification) {
        ConfigOptions.updateNotification = updateNotification;
        NotBounties.getInstance().reloadConfig();
        if (NotBounties.getInstance().getConfig().getKeys(true).size() <= 2) {
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the config.yml file. Fix the YAML formatting errors, or the plugin may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            return;
        }
        NotBounties.getInstance().getConfig().set("update-notification", updateNotification);
        NotBounties.getInstance().saveConfig();
    }



    public static boolean isOfflineSet() {
        return offlineSet;
    }

    /**
     * Check if players with the same IP should be able to claim bounties on themselves.
     * @return If players can claim each other's bounties with the same IP.
     */
    public static boolean isSameIPClaim() {
        return sameIPClaim;
    }



    public static AutoBounties getAutoBounties() {
        return autoBounties;
    }

    public static Databases getDatabases() {
        return databases;
    }

    public static Display getDisplay() {
        return display;
    }

    public static Immunity getImmunity() {
        return immunity;
    }

    public static Integrations getIntegrations() {
        return integrations;
    }

    public static Money getMoney() {
        return money;
    }

    public static List<String> getPluginBountyCommands() {
        return pluginBountyCommands;
    }

    public static long getBountyCooldown() {
        return bountyCooldown;
    }

    public static List<String> getHiddenNames() {
        return hiddenNames;
    }

    public static boolean isStealBounties() {
        return stealBounties;
    }

    public static boolean isNpcClaim() {
        return npcClaim;
    }

    public static boolean isReducePageCalculations() {
        return reducePageCalculations;
    }

    public static boolean isSeePlayerList() {
        return seePlayerList;
    }

    public static boolean isBountyConfirmation() {
        return bountyConfirmation;
    }

    public static boolean isAutoTimezone() {
        return autoTimezone;
    }

    public static DateFormat getDateFormat() {
        return dateFormat;
    }

    public static boolean isSendBStats() {
        return sendBStats;
    }

    public static int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public static boolean isRemoveBannedPlayers() {
        return removeBannedPlayers;
    }

    public static boolean isBountyBackups() {
        return bountyBackups;
    }

    public static boolean isWorldFilter() {
        return worldFilter;
    }

    public static List<String> getWorldFilterNames() {
        return worldFilterNames;
    }

    public static int getMaxTabCompletePlayers() {
        return maxTabCompletePlayers;
    }

    public static ClaimOrder getClaimOrder() {
        return claimOrder;
    }

    public static boolean isSelfSetting() {
        return selfSetting;
    }

    public static int getMaxSetters() {
        return maxSetters;
    }
}

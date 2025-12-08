package me.jadenp.notbounties.features;


import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.RewardHead;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.settings.display.Display;
import me.jadenp.notbounties.features.settings.immunity.Immunity;
import me.jadenp.notbounties.features.settings.integrations.Integrations;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
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
import org.bukkit.command.CommandSender;
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
    private static int maxTabCompletePlayers;
    private static int autoSaveInterval;
    private static Plugin plugin;
    private static int defaultEntityTrackingRangePlayer;
    private static boolean hideInvisiblePlayers;
    private static boolean setterClaimOwn;
    private static boolean usePlcmdInGui;

    public static void reloadOptions(Plugin plugin) throws IOException {
        ConfigOptions.plugin = plugin;

        defaultEntityTrackingRangePlayer = Bukkit.getServer().spigot().getConfig().getInt("world-settings.default.entity-tracking-range.players", 48);

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        BountyMap.loadFont();


        // create configuration sections for each external team plugin
        if (plugin.getConfig().isBoolean("teams.bt-claim")) {
            plugin.getConfig().set("teams.better-teams.team", plugin.getConfig().getBoolean("teams.bt-claim"));
            plugin.getConfig().set("teams.better-teams.ally", plugin.getConfig().getBoolean("teams.bt-allies"));
            plugin.getConfig().set("teams.towny-advanced.nation", plugin.getConfig().getBoolean("teams.towny-nation"));
            plugin.getConfig().set("teams.towny-advanced.town", plugin.getConfig().getBoolean("teams.towny-town"));
            plugin.getConfig().set("teams.towny-advanced.ally", plugin.getConfig().getBoolean("teams.towny-allies"));
            plugin.getConfig().set("teams.bt-claim", null);
            plugin.getConfig().set("teams.bt-allies", null);
            plugin.getConfig().set("teams.towny-nation", null);
            plugin.getConfig().set("teams.towny-town", null);
            plugin.getConfig().set("teams.towny-allies", null);
        }

        // move saber-factions to just factions
        if (plugin.getConfig().isSet("teams.saber-factions.faction")) {
            plugin.getConfig().set("teams.factions.faction", plugin.getConfig().getBoolean("teams.saber-factions.faction"));
            plugin.getConfig().set("teams.factions.ally", plugin.getConfig().getBoolean("teams.saber-factions.ally"));
            plugin.getConfig().set("teams.saber-factions.faction", null);
            plugin.getConfig().set("teams.saber-factions.ally", null);
        }

        // convert use-item-values to item-vales
        if (plugin.getConfig().isBoolean("currency.bounty-items.use-item-values")) {
            if (plugin.getConfig().getBoolean("currency.bounty-items.use-item-values")) {
                plugin.getConfig().set("currency.bounty-items.item-values", "FILE");
            } else {
                plugin.getConfig().set("currency.bounty-items.item-values", "DISABLE");
            }
            plugin.getConfig().set("currency.bounty-items.use-item-values", null);
        }

        // convert database to databases
        if (plugin.getConfig().isSet("database")) {
            plugin.getConfig().set("databases.example-sql.type", "SQL");
            plugin.getConfig().set("databases.example-sql.host", plugin.getConfig().getString("database.host"));
            plugin.getConfig().set("databases.example-sql.port", plugin.getConfig().getInt("database.port"));
            plugin.getConfig().set("databases.example-sql.database", plugin.getConfig().getString("database.database"));
            plugin.getConfig().set("databases.example-sql.user", plugin.getConfig().getString("database.user"));
            plugin.getConfig().set("databases.example-sql.password", plugin.getConfig().getString("database.password"));
            plugin.getConfig().set("databases.example-sql.ssl", plugin.getConfig().getBoolean("database.use-ssl"));
            plugin.getConfig().set("databases.example-sql.refresh-interval", 300);
            plugin.getConfig().set("databases.example-sql.priority", 1);
            plugin.getConfig().set("database", null);
        }

        // add extra options to the proxy database
        if (plugin.getConfig().isSet("databases.example-proxy.type") && !plugin.getConfig().isSet("databases.example-proxy.skins")) {
            plugin.getConfig().set("databases.example-proxy.skins", true);
            plugin.getConfig().set("databases.example-proxy.database-sync", true);
        }

        if (plugin.getConfig().isSet("bounty-whitelist.enable-blacklist")) {
            plugin.getConfig().set("bounty-whitelist.allow-toggling-whitelist", plugin.getConfig().getBoolean("bounty-whitelist.enable-blacklist"));
            plugin.getConfig().set("bounty-whitelist.enable-blacklist", null);
        }

        boolean saveChanges = true;
        if (plugin.getConfig().getKeys(true).size() <= 2) {
            saveChanges = false;
            plugin.getLogger().severe("Loaded an empty configuration for the config.yml file. Fix the YAML formatting errors, or the plugin may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
        } else {
            migrate120Config();
        }



        // fill in any default options that aren't present
        if (NotBounties.getInstance().getResource("config.yml") != null) {
            plugin.getConfig().setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("config.yml")))));
            for (String key : Objects.requireNonNull(plugin.getConfig().getDefaults()).getKeys(true)) {
                if (Arrays.stream(modifiableSections).anyMatch(key::startsWith))
                    continue;
                if (!plugin.getConfig().isSet(key))
                    plugin.getConfig().set(key, plugin.getConfig().getDefaults().get(key));
            }
        }


        PVPRestrictions.loadConfiguration(Objects.requireNonNull(plugin.getConfig().getConfigurationSection("pvp-restrictions")));
        BountyExpire.loadConfiguration(Objects.requireNonNull(plugin.getConfig().getConfigurationSection("bounty-expire")));
        GUIClicks.loadConfiguration(Objects.requireNonNull(plugin.getConfig().getConfigurationSection("bounty-gui-clicks")));
        Whitelist.loadConfiguration(Objects.requireNonNull(plugin.getConfig().getConfigurationSection("bounty-whitelist")));
        RewardHead.loadConfiguration(Objects.requireNonNull(plugin.getConfig().getConfigurationSection("reward-heads")));

        money.loadFile(plugin);
        immunity.loadFile(plugin);
        integrations.loadFile(plugin);
        autoBounties.loadFile(plugin);
        databases.loadFile(plugin);

        hiddenNames = plugin.getConfig().getStringList("hide-stats");
        updateNotification = plugin.getConfig().getString("update-notification");
        npcClaim = plugin.getConfig().getBoolean("npc-claim");
        worldFilter = plugin.getConfig().getBoolean("world-filter.whitelist");
        worldFilterNames = plugin.getConfig().getStringList("world-filter.worlds");
        maxSetters = plugin.getConfig().getInt("max-setters");
        bountyConfirmation = plugin.getConfig().getBoolean("confirmation");
        removeBannedPlayers = plugin.getConfig().getBoolean("remove-banned-players");
        try {
            claimOrder = ClaimOrder.valueOf(Objects.requireNonNull(plugin.getConfig().getString("claim-order")).toUpperCase());
        } catch (IllegalArgumentException e) {
            claimOrder = ClaimOrder.REGULAR;
            plugin.getLogger().warning("claim-order is not set to a proper value!");
        }
        sendBStats = plugin.getConfig().getBoolean("send-bstats");
        autoTimezone = plugin.getConfig().getBoolean("auto-timezone");
        reducePageCalculations = plugin.getConfig().getBoolean("reduce-page-calculations");
        seePlayerList = plugin.getConfig().getBoolean("see-player-list");
        stealBounties = plugin.getConfig().getBoolean("steal-bounties");
        pluginBountyCommands = plugin.getConfig().getStringList("plugin-bounty-commands");
        if (pluginBountyCommands.isEmpty())
            pluginBountyCommands.add("notbounties");
        if (firstStart)
            registerAliases(pluginBountyCommands);
        bountyBackups = plugin.getConfig().getBoolean("bounty-backups");
        maxTabCompletePlayers = plugin.getConfig().getInt("max-tab-complete-players");
        autoSaveInterval = plugin.getConfig().getInt("auto-save-interval");
        offlineSet = plugin.getConfig().getBoolean("offline-set");
        selfSetting = plugin.getConfig().getBoolean("self-setting");
        hideInvisiblePlayers = plugin.getConfig().getBoolean("hide-invisible-players");
        setterClaimOwn = plugin.getConfig().getBoolean("setter-claim-own");
        usePlcmdInGui = plugin.getConfig().getBoolean("use-plcmd-in-gui");

        dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.getLocale());

        File guiFile = new File(plugin.getDataFolder() + File.separator + "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        if (guiConfig.getKeys(true).size() <= 2) {
            plugin.getLogger().severe("Loaded an empty configuration for the gui.yml file. Fix the YAML formatting errors, or the GUI may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
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
                plugin.getLogger().warning("Unknown GUI in gui.yml: \"" + key + "\"");
            }
        }

        if (saveChanges)
            plugin.saveConfig();
        LanguageOptions.reloadOptions();
        WebhookOptions.reloadOptions();
        SkinManager.refreshSkinRequests();
        ChallengeManager.reloadOptions();
        LocalTime.readAuthentication();
        if (integrations.isFloodgateEnabled() || integrations.isGeyserEnabled())
            BedrockGUI.reloadOptions();

        display.loadFile(plugin); // bounty tracker needs to be loaded later
        firstStart = false;
    }

    private static void registerAliases(List<String> aliases) {
        aliases = new ArrayList<>(aliases);
        aliases.removeIf(s -> s.equalsIgnoreCase("notbounties")); // this is already the command name
        PluginCommand command = NotBounties.getInstance().getCommand("notbounties");
        if (command == null) {
            plugin.getLogger().warning("Error finding bounty command to register aliases.");
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
            plugin.getLogger().warning("Error adding command aliases");
            plugin.getLogger().warning(e.toString());
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
     * @param resourcePath Path to the resource from the plugin's data folder.
     * @param pathMap Map of previous paths in the config file to new paths in the resource file.
     */
    private static void migrateConfigResource(String resourcePath, Map<String, String> pathMap) {
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

    private static void migrate120Config() {
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
        migrateConfigResource("settings/auto-bounties.yml", pathMap);

        pathMap.clear();
        pathMap.put("databases", "");
        migrateConfigResource("settings/databases.yml", pathMap);

        pathMap.clear();
        pathMap.put("bounty-posters", "bounty-posters");
        pathMap.put("bounty-board", "bounty-board");
        pathMap.put("bounty-tracker", "bounty-tracker");
        pathMap.put("wanted-tag", "wanted-tag");
        migrateConfigResource("settings/display.yml", pathMap);

        pathMap.clear();
        pathMap.put("immunity", "");
        pathMap.put("bounty-cooldown", "bounty-cooldown");
        migrateConfigResource("settings/immunity.yml", pathMap);

        pathMap.clear();
        pathMap.put("override-skinsrestorer", "override-skinsrestorer");
        pathMap.put("teams", "teams");
        pathMap.put("MMOLib", "MMOLib");
        pathMap.put("Duels", "Duels");
        pathMap.put("same-ip-claim", "teams.same-ip-claim");
        migrateConfigResource("settings/integrations.yml", pathMap);

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
        migrateConfigResource("settings/money.yml", pathMap);
    }

    public static String getUpdateNotification() {
        return updateNotification;
    }

    public static void setUpdateNotification(String updateNotification) {
        ConfigOptions.updateNotification = updateNotification;
        NotBounties.getInstance().reloadConfig();
        if (NotBounties.getInstance().getConfig().getKeys(true).size() <= 2) {
            plugin.getLogger().severe("Loaded an empty configuration for the config.yml file. Fix the YAML formatting errors, or the plugin may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            return;
        }
        NotBounties.getInstance().getConfig().set("update-notification", updateNotification);
        NotBounties.getInstance().saveConfig();
    }

    /**
     * Run a GUI command for a player
     * @param sender Player who is going to run the command
     * @param command Command to be ran without the /bounty in front of it
     */
    public static void runGUIPluginCommand(CommandSender sender, String command) {
        if (usePlcmdInGui) {
            Bukkit.dispatchCommand(sender, pluginBountyCommands.get(0) + " " + command);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "notbountiesadmin " + sender.getName() + " " + command);
        }
    }

    public static boolean isOfflineSet() {
        return offlineSet;
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

    public static int getDefaultEntityTrackingRangePlayer() {
        return defaultEntityTrackingRangePlayer;
    }

    public static boolean isHideInvisiblePlayers() {
        return hideInvisiblePlayers;
    }

    public static boolean isSetterClaimOwn() {
        return setterClaimOwn;
    }
}

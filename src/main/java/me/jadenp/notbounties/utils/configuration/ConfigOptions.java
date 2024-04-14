package me.jadenp.notbounties.utils.configuration;


import me.jadenp.notbounties.*;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUI;
import me.jadenp.notbounties.utils.BountyClaimRequirements;
import me.jadenp.notbounties.utils.configuration.autoBounties.MurderBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.RandomBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import me.jadenp.notbounties.ui.gui.CustomItem;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.map.BountyMap;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.webhook.WebhookOptions;
import me.jadenp.notbounties.utils.externalAPIs.SkinsRestorerClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;

public class ConfigOptions {
    public static boolean autoConnect;
    public static boolean migrateLocalData;
    public static boolean tracker;
    public static boolean giveOwnTracker;
    public static int trackerRemove;
    public static int trackerGlow;
    public static boolean trackerActionBar;
    public static boolean TABShowAlways;
    public static boolean TABPlayerName;
    public static boolean TABDistance;
    public static boolean TABPosition;
    public static boolean TABWorld;
    public static int minBroadcast;
    public static int minBounty;
    public static double bountyTax;
    public static boolean rewardHeadClaimed;
    public static boolean redeemRewardLater;
    public static boolean rewardHeadSetter;
    public static boolean buyBack;
    public static double buyBackInterest;
    public static boolean papiEnabled;
    public static List<String> hiddenNames = new ArrayList<>();
    public static boolean updateNotification;
    public static final Map<String, CustomItem> customItems = new HashMap<>();
    public static boolean npcClaim;
    public static double deathTax;
    public static boolean worldFilter;
    public static List<String> worldFilterNames = new ArrayList<>();
    public static double bountyWhitelistCost;
    public static boolean HDBEnabled;
    public static int maxSetters;

    public static DateFormat dateFormat;
    public static boolean giveOwnMap;
    public static boolean displayReward;
    public static String rewardText;
    public static boolean lockMap;
    public static boolean currencyWrap;
    public static boolean bountyWhitelistEnabled;
    public static int updateInterval;
    public static boolean confirmation;

    public static boolean showWhitelistedBounties;
    public static boolean variableWhitelist;

    public static String consoleName;
    public static boolean enableBlacklist;
    public static boolean liteBansEnabled;
    public static boolean removeBannedPlayers;
    public static boolean saveTemplates;
    public static String nameLine;
    public static boolean alwaysUpdate;
    public static boolean wanted;
    public static double wantedOffset;
    public static String wantedText;
    public static long wantedTextUpdateInterval;
    public static long wantedVisibilityUpdateInterval;
    public static double minWanted;
    public static boolean hideWantedWhenSneaking;
    public static LinkedHashMap<Integer, String> wantedLevels = new LinkedHashMap<>();
    public static int boardType;
    public static long boardUpdate;
    public static boolean boardGlow;
    public static boolean boardInvisible;
    public static boolean skinsRestorerEnabled;
    public static SkinsRestorerClass skinsRestorerClass;
    public static int boardStaggeredUpdate;

    public static String boardName;
    public static int updateName;
    public static boolean RRLVoucherPerSetter;
    public static String RRLSetterLoreAddition;

    public enum ClaimOrder {
        BEFORE, REGULAR, AFTER
    }

    public static ClaimOrder claimOrder;
    public static boolean geyserEnabled;
    public static boolean floodgateEnabled;
    public static boolean sendBStats;
    public static double autoBountyExpireTime;
    public static boolean autoBountyOverrideImmunity;
    public static boolean selfSetting;
    public static boolean autoTimezone;
    public static boolean reducePageCalculations;

    public static void reloadOptions() throws IOException {
        BountyMap.loadFont();
        NotBounties bounties = NotBounties.getInstance();

        papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        liteBansEnabled = Bukkit.getPluginManager().isPluginEnabled("LiteBans");
        skinsRestorerEnabled = Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer");
        geyserEnabled = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot");
        floodgateEnabled = Bukkit.getPluginManager().isPluginEnabled("floodgate");

        if (skinsRestorerEnabled)
            skinsRestorerClass = new SkinsRestorerClass();

        bounties.reloadConfig();

        if (bounties.getConfig().isString("currency")) {
            String prevOption = bounties.getConfig().getString("currency");
            bounties.getConfig().set("currency", null);
            bounties.getConfig().set("currency.object", prevOption);
            bounties.getConfig().set("currency.prefix", "");
            bounties.getConfig().set("currency.suffix", "");
        }

        if (bounties.getConfig().isBoolean("currency.add-single-currency"))
            if (bounties.getConfig().getBoolean("currency.add-single-currency"))
                bounties.getConfig().set("currency.add-single-currency", "first");
            else
                bounties.getConfig().set("currency.add-single-currency", "ratio");

        if (bounties.getConfig().isSet("add-currency-commands")) {
            bounties.getConfig().set("currency.add-commands", bounties.getConfig().getStringList("add-currency-commands"));
            bounties.getConfig().set("currency.remove-commands", bounties.getConfig().getStringList("remove-currency-commands"));
            bounties.getConfig().set("add-currency-commands", null);
            bounties.getConfig().set("remove-currency-commands", null);
        }
        if (bounties.getConfig().isBoolean("reward-heads")) {
            boolean prevOption = bounties.getConfig().getBoolean("reward-heads");
            bounties.getConfig().set("reward-heads", null);
            bounties.getConfig().set("reward-heads.setters", prevOption);
        }

        if (bounties.getConfig().isSet("bounty-whitelist-cost")) {
            bounties.getConfig().set("bounty-whitelist.cost", bounties.getConfig().getInt("bounty-whitelist-cost"));
            bounties.getConfig().set("bounty-whitelist-cost", null);
        }

        if (bounties.getConfig().isInt("number-formatting.type")) {
            switch (bounties.getConfig().getInt("number-formatting.type")) {
                case 0:
                    bounties.getConfig().set("number-formatting.pattern", "#.##");
                    bounties.getConfig().set("number-formatting.use-divisions", true);
                    break;
                case 1:
                    bounties.getConfig().set("number-formatting.use-divisions", false);
                    break;
                case 2:
                    bounties.getConfig().set("number-formatting.use-divisions", true);
                    break;
            }
            bounties.getConfig().set("number-formatting.type", null);
            bounties.getConfig().set("number-formatting.thousands", null);
            bounties.getConfig().set("number-formatting.divisions.decimals", null);
            bounties.getConfig().set("number-formatting.decimal-symbol", null);
            bounties.getConfig().set("currency.decimals", null);
        }
        if (bounties.getConfig().getBoolean("bounty-posters.clean-posters")) {
            BountyMap.cleanPosters();
            bounties.getConfig().set("bounty-posters.clean-posters", false);
        }
        if (bounties.getConfig().isBoolean("redeem-reward-later"))
            bounties.getConfig().set("redeem-reward-later.enabled", bounties.getConfig().getBoolean("redeem-reward-later"));
        if (bounties.getConfig().isSet("force-death-event")) {
            if (bounties.getConfig().getBoolean("force-death-event"))
                bounties.getConfig().set("claim-order", "BEFORE");
            else
                bounties.getConfig().set("claim-order", "REGULAR");
            bounties.getConfig().set("force-death-event", null);
        }
        if (bounties.getConfig().isBoolean("currency.manual-economy")) {
            if (bounties.getConfig().getBoolean("currency.manual-economy"))
                bounties.getConfig().set("currency.manual-economy", "MANUAL");
            else
                bounties.getConfig().set("currency.manual-economy", "AUTOMATIC");
        }
        if (bounties.getConfig().isSet("immunity.buy-immunity")) {
            if (bounties.getConfig().getBoolean("immunity.buy-immunity"))
                bounties.getConfig().set("immunity.type", "DISABLE");
            else if (bounties.getConfig().getBoolean("immunity.permanent-immunity.enabled"))
                bounties.getConfig().set("immunity.type", "PERMANENT");
            else bounties.getConfig().set("immunity.type", "SCALING");
            bounties.getConfig().set("immunity.buy-immunity", null);
            bounties.getConfig().set("immunity.permanent-immunity.enabled", null);
        }
        if (bounties.getConfig().isInt("immunity.type")) {
            switch (bounties.getConfig().getInt("immunity.type")) {
                case 0:
                    bounties.getConfig().set("immunity.type", "DISABLE");
                    break;
                case 1:
                    bounties.getConfig().set("immunity.type", "PERMANENT");
                    break;
                case 2:
                    bounties.getConfig().set("immunity.type", "SCALING");
                    break;
                case 3:
                    bounties.getConfig().set("immunity.type", "TIME");
                    break;
            }
        }

        // move auto-bounties to one configuration section
        if (bounties.getConfig().isConfigurationSection("murder-bounty") &&
                bounties.getConfig().isConfigurationSection("random-bounties") &&
                bounties.getConfig().isConfigurationSection("timed-bounties")) {
            bounties.getConfig().set("auto-bounties.murder-bounty.player-cooldown", bounties.getConfig().getInt("murder-bounty.player-cooldown"));
            bounties.getConfig().set("auto-bounties.murder-bounty.bounty-increase", bounties.getConfig().getDouble("murder-bounty.bounty-increase"));
            bounties.getConfig().set("auto-bounties.murder-bounty.exclude-claiming", bounties.getConfig().getBoolean("murder-bounty.exclude-claiming"));
            bounties.getConfig().set("auto-bounties.random-bounties.offline-set", bounties.getConfig().getBoolean("random-bounties.offline-set"));
            bounties.getConfig().set("auto-bounties.random-bounties.min-time", bounties.getConfig().getInt("random-bounties.min-time"));
            bounties.getConfig().set("auto-bounties.random-bounties.max-time", bounties.getConfig().getInt("random-bounties.max-time"));
            bounties.getConfig().set("auto-bounties.random-bounties.min-price", bounties.getConfig().getDouble("random-bounties.min-price"));
            bounties.getConfig().set("auto-bounties.random-bounties.max-price", bounties.getConfig().getDouble("random-bounties.max-price"));
            bounties.getConfig().set("auto-bounties.timed-bounties.time", bounties.getConfig().getInt("timed-bounties.time"));
            bounties.getConfig().set("auto-bounties.timed-bounties.bounty-increase", bounties.getConfig().getDouble("timed-bounties.bounty-increase"));
            bounties.getConfig().set("auto-bounties.timed-bounties.max-bounty", bounties.getConfig().getDouble("timed-bounties.max-bounty"));
            bounties.getConfig().set("auto-bounties.timed-bounties.reset-on-death", bounties.getConfig().getBoolean("timed-bounties.reset-on-death"));
            bounties.getConfig().set("auto-bounties.timed-bounties.offline-tracking", bounties.getConfig().getBoolean("timed-bounties.offline-tracking"));
            bounties.getConfig().set("timed-bounties", null);
            bounties.getConfig().set("random-bounties", null);
            bounties.getConfig().set("murder-bounty", null);
        }

        // move bounty-expire to a new configuration section
        if (bounties.getConfig().isInt("bounty-expire") || bounties.getConfig().isDouble("bounty-expire")) {
            bounties.getConfig().set("bounty-expire.time", bounties.getConfig().getDouble("bounty-expire"));
            bounties.getConfig().set("bounty-expire.offline-tracking", true);
        }

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

        boolean saveChanges = true;
        if (bounties.getConfig().getKeys(true).size() <= 2) {
            saveChanges = false;
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the config.yml file. Fix the YAML formatting errors, or the plugin may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
        }

        // fill in any default options that aren't present
        if (NotBounties.getInstance().getResource("config.yml") != null) {
            bounties.getConfig().setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("config.yml")))));
            for (String key : Objects.requireNonNull(bounties.getConfig().getDefaults()).getKeys(true)) {
                if (!bounties.getConfig().isSet(key))
                    bounties.getConfig().set(key, bounties.getConfig().getDefaults().get(key));
            }
        }

        NumberFormatting.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("currency")), bounties.getConfig().getConfigurationSection("number-formatting"));
        PVPRestrictions.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("pvp-restrictions")));
        MurderBounties.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("auto-bounties.murder-bounty")));
        RandomBounties.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("auto-bounties.random-bounties")));
        TimedBounties.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("auto-bounties.timed-bounties")));
        ActionCommands.loadConfiguration(bounties.getConfig().getStringList("bounty-claim-commands"), bounties.getConfig().getStringList("big-bounties.commands"));
        Immunity.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("immunity")));
        BountyExpire.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("bounty-expire")));
        BigBounty.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("big-bounties")));
        BountyClaimRequirements.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("teams")));
        Prompt.loadConfiguration(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("prompts")));

        rewardHeadSetter = bounties.getConfig().getBoolean("reward-heads.setters");
        rewardHeadClaimed = bounties.getConfig().getBoolean("reward-heads.claimed");
        buyBack = bounties.getConfig().getBoolean("buy-own-bounties.enabled");
        buyBackInterest = bounties.getConfig().getDouble("buy-own-bounties.cost-multiply");
        minBounty = bounties.getConfig().getInt("minimum-bounty");
        bountyTax = bounties.getConfig().getDouble("bounty-tax");
        tracker = bounties.getConfig().getBoolean("bounty-tracker.enabled");
        giveOwnTracker = bounties.getConfig().getBoolean("bounty-tracker.give-own");
        trackerRemove = bounties.getConfig().getInt("bounty-tracker.remove");
        trackerGlow = bounties.getConfig().getInt("bounty-tracker.glow");
        trackerActionBar = bounties.getConfig().getBoolean("bounty-tracker.action-bar.enabled");
        TABShowAlways = bounties.getConfig().getBoolean("bounty-tracker.action-bar.show-always");
        TABPlayerName = bounties.getConfig().getBoolean("bounty-tracker.action-bar.player-name");
        TABDistance = bounties.getConfig().getBoolean("bounty-tracker.action-bar.distance");
        TABPosition = bounties.getConfig().getBoolean("bounty-tracker.action-bar.position");
        TABWorld = bounties.getConfig().getBoolean("bounty-tracker.action-bar.world");
        redeemRewardLater = bounties.getConfig().getBoolean("redeem-reward-later.enabled");
        RRLVoucherPerSetter = bounties.getConfig().getBoolean("redeem-reward-later.voucher-per-setter");
        RRLSetterLoreAddition = bounties.getConfig().getString("redeem-reward-later.setter-lore-addition");
        minBroadcast = bounties.getConfig().getInt("minimum-broadcast");
        migrateLocalData = bounties.getConfig().getBoolean("database.migrate-local-data");
        autoConnect = bounties.getConfig().getBoolean("database.auto-connect");
        hiddenNames = bounties.getConfig().getStringList("hide-stats");
        updateNotification = bounties.getConfig().getBoolean("update-notification");
        npcClaim = bounties.getConfig().getBoolean("npc-claim");
        deathTax = bounties.getConfig().getDouble("death-tax");
        worldFilter = bounties.getConfig().getBoolean("world-filter.whitelist");
        worldFilterNames = bounties.getConfig().getStringList("world-filter.worlds");
        bountyWhitelistCost = bounties.getConfig().getDouble("bounty-whitelist.cost");
        bountyWhitelistEnabled = bounties.getConfig().getBoolean("bounty-whitelist.enabled");
        HDBEnabled = Bukkit.getPluginManager().isPluginEnabled("HeadDataBase");
        maxSetters = bounties.getConfig().getInt("max-setters");
        giveOwnMap = bounties.getConfig().getBoolean("bounty-posters.give-own");
        displayReward = bounties.getConfig().getBoolean("bounty-posters.display-reward");
        rewardText = bounties.getConfig().getString("bounty-posters.reward-text");
        lockMap = bounties.getConfig().getBoolean("bounty-posters.lock-maps");
        currencyWrap = bounties.getConfig().getBoolean("bounty-posters.currency-wrap");
        updateInterval = bounties.getConfig().getInt("bounty-posters.update-interval");
        confirmation = bounties.getConfig().getBoolean("confirmation");
        showWhitelistedBounties = bounties.getConfig().getBoolean("bounty-whitelist.show-all-bounty");
        variableWhitelist = bounties.getConfig().getBoolean("bounty-whitelist.variable-whitelist");
        consoleName = bounties.getConfig().getString("console-bounty-name");
        enableBlacklist = bounties.getConfig().getBoolean("bounty-whitelist.enable-blacklist");
        removeBannedPlayers = bounties.getConfig().getBoolean("remove-banned-players");
        saveTemplates = bounties.getConfig().getBoolean("bounty-posters.save-templates");
        nameLine = bounties.getConfig().getString("bounty-posters.name-line");
        alwaysUpdate = bounties.getConfig().getBoolean("bounty-posters.always-update");
        wanted = bounties.getConfig().getBoolean("wanted-tag.enabled");
        wantedOffset = bounties.getConfig().getDouble("wanted-tag.offset");
        wantedText = bounties.getConfig().getString("wanted-tag.text");
        minWanted = bounties.getConfig().getDouble("wanted-tag.min-bounty");
        hideWantedWhenSneaking = bounties.getConfig().getBoolean("wanted-tag.hide-when-sneaking");
        boardType = bounties.getConfig().getInt("bounty-board.type");
        boardUpdate = bounties.getConfig().getInt("bounty-board.update-interval");
        boardGlow = bounties.getConfig().getBoolean("bounty-board.glow");
        boardInvisible = bounties.getConfig().getBoolean("bounty-board.invisible");
        boardStaggeredUpdate = bounties.getConfig().getInt("bounty-board.staggered-update");
        boardName = bounties.getConfig().getString("bounty-board.item-name");
        updateName = bounties.getConfig().getInt("bounty-board.update-name");
        try {
            claimOrder = ClaimOrder.valueOf(Objects.requireNonNull(bounties.getConfig().getString("claim-order")).toUpperCase());
        } catch (IllegalArgumentException e) {
            claimOrder = ClaimOrder.REGULAR;
            Bukkit.getLogger().warning("[NotBounties] claim-order is not set to a proper value!");
        }
        sendBStats = bounties.getConfig().getBoolean("send-bstats");
        autoBountyExpireTime = bounties.getConfig().getDouble("auto-bounties.expire-time");
        autoBountyOverrideImmunity = bounties.getConfig().getBoolean("auto-bounties.override-immunity");
        autoTimezone = bounties.getConfig().getBoolean("auto-timezone");
        wantedTextUpdateInterval = bounties.getConfig().getLong("wanted-tag.text-update-interval");
        wantedVisibilityUpdateInterval = bounties.getConfig().getLong("wanted-tag.visibility-update-interval");
        reducePageCalculations = bounties.getConfig().getBoolean("reduce-page-calculations");

        wantedLevels.clear();
        for (String key : Objects.requireNonNull(bounties.getConfig().getConfigurationSection("wanted-tag.level")).getKeys(false)) {
            try {
                int amount = Integer.parseInt(key);
                String text = bounties.getConfig().getString("wanted-tag.level." + key);
                wantedLevels.put(amount, text);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().info("[NotBounties] Wanted tag level: \"" + key + "\" is not a whole number!");
            }
        }
        selfSetting = bounties.getConfig().getBoolean("self-setting");

        wantedLevels = sortByValue(wantedLevels);

        dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.locale);


        File guiFile = new File(bounties.getDataFolder() + File.separator + "gui.yml");
        if (!guiFile.exists()) {
            bounties.saveResource("gui.yml", false);
        }
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        if (guiConfig.getKeys(true).size() <= 2) {
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the gui.yml file. Fix the YAML formatting errors, or the GUI may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            if (NotBounties.getInstance().getResource("gui.yml") != null) {
                guiConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("gui.yml"))));
                loadGUI(guiConfig);
            }
        } else {
            loadGUI(guiConfig);
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
        if (geyserEnabled && floodgateEnabled)
            BedrockGUI.reloadOptions();
    }

    private static void loadGUI(YamlConfiguration guiConfig) throws IOException {
        NotBounties bounties = NotBounties.getInstance();
        boolean guiChanges = false;
        if (bounties.getConfig().isConfigurationSection("advanced-gui")) {
            // migrate everything to gui.yml
            guiConfig.set("bounty-gui", null);
            ConfigurationSection section = bounties.getConfig().getConfigurationSection("advanced-gui");
            assert section != null;
            guiConfig.set("bounty-gui.sort-type", section.get("sort-type"));
            guiConfig.set("bounty-gui.size", section.get("size"));
            for (String key : Objects.requireNonNull(section.getConfigurationSection("custom-items")).getKeys(false)) {
                guiConfig.set("custom-items." + key, section.get("custom-items." + key));
            }
            guiConfig.set("bounty-gui.player-slots", section.get("bounty-slots"));
            guiConfig.set("bounty-gui.remove-page-items", true);
            guiConfig.set("bounty-gui.head-lore", Arrays.asList("&7<&m                        &7>", "&4Bounty: &6%notbounties_bounty_formatted%", "&4&oKill this player to", "&4&oreceive this reward", "&7<&m                        &7>"));
            guiConfig.set("bounty-gui.head-name", "&4☠ &c&l{player} &4☠");

            guiConfig.set("bounty-gui.gui-name", "&d&lBounties &9&lPage");
            guiConfig.set("bounty-gui.add-page", true);

            if (LanguageOptions.getLanguageFile().exists()) {
                YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(LanguageOptions.getLanguageFile());
                if (languageConfig.isString("bounty-item-name") && languageConfig.isList("bounty-item-lore")) {
                    // convert {amount} to %notbounties_bounty_formatted%
                    String unformatted = languageConfig.getString("bounty-item-name");
                    assert unformatted != null;
                    unformatted = unformatted.replaceAll("\\{amount}", "%notbounties_bounty_formatted%");
                    guiConfig.set("bounty-gui.head-name", unformatted);

                    List<String> unformattedList = languageConfig.getStringList("bounty-item-lore");
                    unformattedList.replaceAll(s -> s.replaceAll("\\{amount}", "%notbounties_bounty_formatted%"));
                    guiConfig.set("bounty-gui.head-lore", unformattedList);
                    guiConfig.set("bounty-gui.gui-name", languageConfig.getString("gui-name"));
                    languageConfig.set("gui-name", null);
                    languageConfig.set("bounty-item-name", null);
                    languageConfig.set("bounty-item-lore", null);
                    languageConfig.save(LanguageOptions.getLanguageFile());
                }
            }

            for (String key : Objects.requireNonNull(section.getConfigurationSection("layout")).getKeys(false)) {
                guiConfig.set("bounty-gui.layout." + key, section.get("layout." + key));
            }

            bounties.getConfig().set("advanced-gui", null);
            guiChanges = true;
        }

        customItems.clear();


        if (!guiConfig.isSet("set-whitelist")) {
            guiConfig.set("set-whitelist.sort-type", 3);
            guiConfig.set("set-whitelist.size", 54);
            guiConfig.set("set-whitelist.gui-name", "&d&lSelect &7&lWhitelisted &9&lPlayers");
            guiConfig.set("set-whitelist.add-page", false);
            guiConfig.set("set-whitelist.remove-page-items", true);
            guiConfig.set("set-whitelist.player-slots", Collections.singletonList("0-44"));
            guiConfig.set("set-whitelist.head-name", "&e&l{player}");
            guiConfig.set("set-whitelist.head-lore", Arrays.asList("", "&6Immunity: {amount}", "&7Click to toggle whitelist", ""));
            guiConfig.set("set-whitelist.layout.1.item", "fill");
            guiConfig.set("set-whitelist.layout.1.slot", "45-53");
            guiConfig.set("set-whitelist.layout.2.item", "return");
            guiConfig.set("set-whitelist.layout.2.slot", "49");
            guiConfig.set("set-whitelist.layout.3.item", "next");
            guiConfig.set("set-whitelist.layout.3.slot", "53");
            guiConfig.set("set-whitelist.layout.4.item", "back");
            guiConfig.set("set-whitelist.layout.4.slot", "45");
            guiConfig.set("set-whitelist.layout.5.item", "add-offline-whitelist");
            guiConfig.set("set-whitelist.layout.5.slot", "51");
            guiConfig.set("set-whitelist.layout.6.item", "reset-whitelist");
            guiConfig.set("set-whitelist.layout.6.slot", "47");
            guiConfig.set("custom-items.add-offline-whitelist.material", "LEVER");
            guiConfig.set("custom-items.add-offline-whitelist.amount", 1);
            guiConfig.set("custom-items.add-offline-whitelist.name", "&7See all players");
            guiConfig.set("custom-items.add-offline-whitelist.commands", Collections.singletonList("[gui] set-whitelist 1 offline"));
            guiConfig.set("custom-items.reset-whitelist.material", "MILK_BUCKET");
            guiConfig.set("custom-items.reset-whitelist.amount", 1);
            guiConfig.set("custom-items.reset-whitelist.name", "&fReset whitelist");
            guiConfig.set("custom-items.reset-whitelist.commands", Arrays.asList("[p] bounty whitelist reset", "[gui] set-whitelist 1"));
            guiChanges = true;
        }
        if (!guiConfig.isSet("confirm-bounty.layout")) {
            guiConfig.set("confirm-bounty.sort-type", 1);
            guiConfig.set("confirm-bounty.size", 54);
            guiConfig.set("confirm-bounty.gui-name", "&6&lBounty Cost: &2{amount_tax}");
            guiConfig.set("confirm-bounty.add-page", false);
            guiConfig.set("confirm-bounty.remove-page-items", true);
            guiConfig.set("confirm-bounty.player-slots", Collections.singletonList("13"));
            guiConfig.set("confirm-bounty.head-name", "&e&lSet bounty of {amount}");
            guiConfig.set("confirm-bounty.head-lore", Arrays.asList("", "&7{player}", ""));
            guiConfig.set("confirm-bounty.layout.1.item", "fill");
            guiConfig.set("confirm-bounty.layout.1.slot", "0-53");
            guiConfig.set("confirm-bounty.layout.2.item", "return-select-price");
            guiConfig.set("confirm-bounty.layout.2.slot", "49");
            guiConfig.set("confirm-bounty.layout.3.item", "false");
            guiConfig.set("confirm-bounty.layout.3.slot", "19-21");
            guiConfig.set("confirm-bounty.layout.4.item", "false");
            guiConfig.set("confirm-bounty.layout.4.slot", "37-39");
            guiConfig.set("confirm-bounty.layout.5.item", "false");
            guiConfig.set("confirm-bounty.layout.5.slot", "28-30");
            guiConfig.set("confirm-bounty.layout.6.item", "yes-bounty");
            guiConfig.set("confirm-bounty.layout.6.slot", "23-25");
            guiConfig.set("confirm-bounty.layout.7.item", "yes-bounty");
            guiConfig.set("confirm-bounty.layout.7.slot", "32-34");
            guiConfig.set("confirm-bounty.layout.8.item", "yes-bounty");
            guiConfig.set("confirm-bounty.layout.8.slot", "41-43");
            guiConfig.set("custom-items.yes-bounty.material", "LIME_STAINED_GLASS_PANE");
            guiConfig.set("custom-items.yes-bounty.amount", 1);
            guiConfig.set("custom-items.yes-bounty.name", "&a&lYes");
            guiConfig.set("custom-items.yes-bounty.commands", Arrays.asList("[p] bounty {slot13} {page} --confirm", "[close]"));
            guiConfig.set("custom-items.return-set-bounty.material", "WHITE_BED");
            guiConfig.set("custom-items.return-set-bounty.amount", 1);
            guiConfig.set("custom-items.return-set-bounty.name", "&6&lReturn");
            guiConfig.set("custom-items.return-set-bounty.lore", Collections.singletonList("&7Return to player selection"));
            guiConfig.set("custom-items.return-set-bounty.commands", Collections.singletonList("[gui] set-bounty 1"));
            guiConfig.set("custom-items.return-select-price.material", "WHITE_BED");
            guiConfig.set("custom-items.return-select-price.amount", 1);
            guiConfig.set("custom-items.return-select-price.name", "&6&lReturn");
            guiConfig.set("custom-items.return-select-price.lore", Collections.singletonList("&7Return to price selection"));
            guiConfig.set("custom-items.return-select-price.commands", Collections.singletonList("[gui] select-price {page}"));
            guiChanges = true;
        }
        File guiFile = new File(bounties.getDataFolder() + File.separator + "gui.yml");
        if (guiChanges) {
            guiConfig.save(guiFile);
        }
        for (String key : Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items")).getKeys(false)) {
            CustomItem customItem = new CustomItem(Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items." + key)));
            customItems.put(key, customItem);
        }
    }


    public static LinkedHashMap<Integer, String> sortByValue(Map<Integer, String> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<Integer, String>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort(Map.Entry.comparingByKey());

        // put data from sorted list to hashmap
        LinkedHashMap<Integer, String> temp = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static String getWantedDisplayText(OfflinePlayer player) {
        if (!BountyManager.hasBounty(player))
            return "";
        Bounty bounty = BountyManager.getBounty(player);
        assert bounty != null;
        String levelReplace = "";
        for (Map.Entry<Integer, String> entry : wantedLevels.entrySet()) {
            if (entry.getKey() <= bounty.getTotalBounty()) {
                levelReplace = entry.getValue();
            } else {
                break;
            }
        }
        return LanguageOptions.parse(wantedText.replaceAll("\\{level}", Matcher.quoteReplacement(levelReplace)), bounty.getTotalBounty(), player);
    }
}

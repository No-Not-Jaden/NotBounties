package me.jadenp.notbounties.utils;


import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.gui.CustomItem;
import me.jadenp.notbounties.gui.GUI;
import me.jadenp.notbounties.gui.GUIOptions;
import me.jadenp.notbounties.map.BountyMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class ConfigOptions {
    public static boolean autoConnect;
    public static boolean migrateLocalData;
    public static List<String> bBountyCommands = new ArrayList<>();
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
    public static int bBountyThreshold;
    public static boolean bBountyParticle;
    public static int immunityType;
    public static double timeImmunity;
    public static int permanentCost;
    public static double scalingRatio;
    public static int graceTime;
    public static int minBounty;
    public static double bountyTax;
    public static boolean rewardHeadClaimed;
    public static boolean redeemRewardLater;
    public static boolean rewardHeadSetter;
    public static boolean buyBack;
    public static double buyBackInterest;
    public static List<String> buyBackLore;
    //public static boolean usingPapi;
    public static int bountyExpire;
    public static boolean papiEnabled;
    public static File language;
    public static List<String> trackerLore;
    public static List<String> voucherLore;
    public static List<String> speakings = new ArrayList<>();
    public static List<String> hiddenNames = new ArrayList<>();
    public static boolean updateNotification;
    public static Map<String, CustomItem> customItems = new HashMap<>();
    public static boolean npcClaim;
    public static double deathTax;
    public static boolean worldFilter;
    public static List<String> worldFilterNames = new ArrayList<>();
    public static double bountyWhitelistCost;
    public static boolean HDBEnabled;
    public static int maxSetters;
    public static List<String> notWhitelistedLore;
    public static List<String> mapLore;
    private static DateFormat dateFormat;
    public static boolean giveOwnMap;
    public static boolean displayReward;
    public static String rewardText;
    public static boolean lockMap;
    public static boolean currencyWrap;
    public static boolean bountyWhitelistEnabled;
    public static int updateInterval;
    public static boolean confirmation;
    public static List<String> adminEditLore;
    public static boolean showWhitelistedBounties;
    public static boolean variableWhitelist;
    public static List<String> whitelistNotify;
    public static List<String> whitelistLore;
    public static int murderCooldown;
    public static double murderBountyIncrease;
    public static boolean murderExcludeClaiming;
    public static String consoleName;

    public static void reloadOptions() throws IOException {
        BountyMap.loadFont();
        NotBounties bounties = NotBounties.getInstance();

        papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        language = new File(bounties.getDataFolder() + File.separator + "language.yml");

        bounties.reloadConfig();

        if (bounties.getConfig().isString("currency")) {
            String prevOption = bounties.getConfig().getString("currency");
            bounties.getConfig().set("currency", null);
            bounties.getConfig().set("currency.object", prevOption);
            bounties.getConfig().set("currency.prefix", "");
            bounties.getConfig().set("currency.suffix", "");
        }
        if (!bounties.getConfig().isSet("currency.prefix"))
            bounties.getConfig().set("currency.prefix", "&f");
        if (!bounties.getConfig().isSet("currency.suffix"))
            bounties.getConfig().set("currency.suffix", "&b◆");
        if (!bounties.getConfig().isSet("currency.add-single-currency"))
            bounties.getConfig().set("currency.add-single-currency", true);
        if (!bounties.getConfig().isSet("minimum-bounty"))
            bounties.getConfig().set("minimum-bounty", 1);
        if (!bounties.getConfig().isSet("bounty-tax"))
            bounties.getConfig().set("bounty-tax", 0.0);
        if (bounties.getConfig().isSet("add-currency-commands")) {
            bounties.getConfig().set("currency.add-commands", bounties.getConfig().getStringList("add-currency-commands"));
            bounties.getConfig().set("currency.remove-commands", bounties.getConfig().getStringList("remove-currency-commands"));
            bounties.getConfig().set("add-currency-commands", null);
            bounties.getConfig().set("remove-currency-commands", null);
        }
        if (!bounties.getConfig().isSet("currency.add-commands"))
            bounties.getConfig().set("currency.add-commands", new ArrayList<>());
        if (!bounties.getConfig().isSet("currency.remove-commands"))
            bounties.getConfig().set("currency.remove-commands", new ArrayList<>());
        if (!bounties.getConfig().isSet("bounty-expire")) bounties.getConfig().set("bounty-expire", -1);
        if (bounties.getConfig().isBoolean("reward-heads")) {
            boolean prevOption = bounties.getConfig().getBoolean("reward-heads");
            bounties.getConfig().set("reward-heads", null);
            bounties.getConfig().set("reward-heads.setters", prevOption);
        }
        if (!bounties.getConfig().isSet("reward-heads.setters"))
            bounties.getConfig().set("reward-heads.setters", false);
        if (!bounties.getConfig().isSet("reward-heads.claimed"))
            bounties.getConfig().set("reward-heads.claimed", false);
        if (!bounties.getConfig().isSet("buy-own-bounties.enabled"))
            bounties.getConfig().set("buy-own-bounties.enabled", false);
        if (!bounties.getConfig().isSet("buy-own-bounties.cost-multiply"))
            bounties.getConfig().set("buy-own-bounties.cost-multiply", 1.25);
        if (!bounties.getConfig().isSet("buy-own-bounties.lore-addition"))
            bounties.getConfig().set("buy-own-bounties.lore-addition", Collections.singletonList("&9Left Click &7to buy back for &a{amount}"));
        if (bounties.getConfig().isSet("immunity.buy-immunity")) {
            if (bounties.getConfig().getBoolean("immunity.buy-immunity")) bounties.getConfig().set("immunity.type", 0);
            else if (bounties.getConfig().getBoolean("immunity.permanent-immunity.enabled")) bounties.getConfig().set("immunity.type", 1);
            else bounties.getConfig().set("immunity.type", 2);
            bounties.getConfig().set("immunity.buy-immunity", null);
            bounties.getConfig().set("immunity.permanent-immunity.enabled", null);
        }
        if (!bounties.getConfig().isSet("immunity.type"))
            bounties.getConfig().set("immunity.type", 2);
        if (!bounties.getConfig().isSet("immunity.permanent-immunity.cost"))
            bounties.getConfig().set("immunity.permanent-immunity.cost", 128);
        if (!bounties.getConfig().isSet("immunity.scaling-immunity.ratio"))
            bounties.getConfig().set("immunity.scaling-immunity.ratio", 1.0);
        if (!bounties.getConfig().isSet("immunity.time-immunity.seconds"))
            bounties.getConfig().set("immunity.time-immunity.seconds", 3600);
        if (!bounties.getConfig().isSet("immunity.grace-period"))
            bounties.getConfig().set("immunity.grace-period", 10);
        if (!bounties.getConfig().isSet("bounty-tracker.enabled"))
            bounties.getConfig().set("bounty-tracker.enabled", true);
        if (!bounties.getConfig().isSet("bounty-tracker.give-own"))
            bounties.getConfig().set("bounty-tracker.give-own", false);
        if (!bounties.getConfig().isSet("bounty-tracker.remove"))
            bounties.getConfig().set("bounty-tracker.remove", 2);
        if (!bounties.getConfig().isSet("bounty-tracker.glow"))
            bounties.getConfig().set("bounty-tracker.glow", 10);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.enabled"))
            bounties.getConfig().set("bounty-tracker.action-bar.enabled", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.show-always"))
            bounties.getConfig().set("bounty-tracker.action-bar.show-always", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.player-name"))
            bounties.getConfig().set("bounty-tracker.action-bar.player-name", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.distance"))
            bounties.getConfig().set("bounty-tracker.action-bar.distance", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.position"))
            bounties.getConfig().set("bounty-tracker.action-bar.position", false);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.world"))
            bounties.getConfig().set("bounty-tracker.action-bar.world", false);
        if (!bounties.getConfig().isSet("redeem-reward-later"))
            bounties.getConfig().set("redeem-reward-later", false);
        if (!bounties.getConfig().isSet("minimum-broadcast"))
            bounties.getConfig().set("minimum-broadcast", 100);
        if (!bounties.getConfig().isSet("big-bounties.bounty-threshold"))
            bounties.getConfig().set("big-bounties.bounty-threshold", -1);
        if (!bounties.getConfig().isSet("big-bounties.particle"))
            bounties.getConfig().set("big-bounties.particle", true);
        if (!bounties.getConfig().isSet("big-bounties.commands"))
            bounties.getConfig().set("big-bounties.commands", new ArrayList<>(Collections.singletonList("execute run effect give {player} minecraft:glowing 10 0")));
        if (!bounties.getConfig().isSet("database.host"))
            bounties.getConfig().set("database.host", "localhost");
        if (!bounties.getConfig().isSet("database.port"))
            bounties.getConfig().set("database.port", "3306");
        if (!bounties.getConfig().isSet("database.database"))
            bounties.getConfig().set("database.database", "db");
        if (!bounties.getConfig().isSet("database.user"))
            bounties.getConfig().set("database.user", "username");
        if (!bounties.getConfig().isSet("database.password"))
            bounties.getConfig().set("database.password", "");
        if (!bounties.getConfig().isSet("database.use-ssl"))
            bounties.getConfig().set("database.use-ssl", false);
        if (!bounties.getConfig().isSet("database.migrate-local-data"))
            bounties.getConfig().set("database.migrate-local-data", true);
        if (!bounties.getConfig().isSet("database.auto-connect"))
            bounties.getConfig().set("database.auto-connect", false);
        if (!bounties.getConfig().isSet("hide-stats"))
            bounties.getConfig().set("hide-stats", new ArrayList<>());
        if (!bounties.getConfig().isSet("update-notification"))
            bounties.getConfig().set("update-notification", true);
        if (!bounties.getConfig().isSet("npc-claim"))
            bounties.getConfig().set("npc-claim", false);
        if (!bounties.getConfig().isSet("death-tax"))
            bounties.getConfig().set("death-tax", 0);
        if (!bounties.getConfig().isSet("world-filter")){
            bounties.getConfig().set("world-filter.whitelist", false);
            bounties.getConfig().set("world-filter.worlds", new ArrayList<>());
        }
        if (bounties.getConfig().isSet("bounty-whitelist-cost")) {
            bounties.getConfig().set("bounty-whitelist.cost", bounties.getConfig().getInt("bounty-whitelist-cost"));
            bounties.getConfig().set("bounty-whitelist-cost", null);
        }
        if (!bounties.getConfig().isSet("bounty-whitelist.cost"))
            bounties.getConfig().set("bounty-whitelist.cost", 10);
        if (!bounties.getConfig().isSet("bounty-whitelist.enabled"))
            bounties.getConfig().set("bounty-whitelist.enabled", true);
        if (!bounties.getConfig().isSet("bounty-whitelist.show-all-bounty"))
            bounties.getConfig().set("bounty-whitelist.show-all-bounty", false);
        if (!bounties.getConfig().isSet("bounty-whitelist.variable-whitelist"))
            bounties.getConfig().set("bounty-whitelist.variable-whitelist", false);

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
        if (!bounties.getConfig().isSet("number-formatting.use-divisions"))
            bounties.getConfig().set("number-formatting.use-divisions", true);
        if (!bounties.getConfig().isSet("number-formatting.pattern"))
            bounties.getConfig().set("number-formatting.pattern", "#,###.##");
        if (!bounties.getConfig().isSet("number-formatting.format-locale"))
            bounties.getConfig().set("number-formatting.format-locale", "en-US");
        if (!bounties.getConfig().isConfigurationSection("number-formatting.divisions"))
            bounties.getConfig().set("number-formatting.divisions.1000", "K");
        if (!bounties.getConfig().isSet("max-setters"))
            bounties.getConfig().set("max-setters", -1);
        if (!bounties.getConfig().isSet("bounty-posters.give-own"))
            bounties.getConfig().set("bounty-posters.give-own", false);
        if (!bounties.getConfig().isSet("bounty-posters.display-reward"))
            bounties.getConfig().set("bounty-posters.display-reward", true);
        if (!bounties.getConfig().isSet("bounty-posters.reward-text"))
            bounties.getConfig().set("bounty-posters.reward-text", "REWARD: ");
        if (!bounties.getConfig().isSet("bounty-posters.lock-maps"))
            bounties.getConfig().set("bounty-posters.lock-maps", false);
        if (!bounties.getConfig().isSet("bounty-posters.currency-wrap"))
            bounties.getConfig().set("bounty-posters.currency-wrap", false);
        if (!bounties.getConfig().isSet("bounty-posters.update-interval"))
            bounties.getConfig().set("bounty-posters.update-interval", 1000);
        if (!bounties.getConfig().isSet("confirmation"))
            bounties.getConfig().set("confirmation", true);
        if (!bounties.getConfig().isSet("murder-bounty.player-cooldown"))
            bounties.getConfig().set("murder-bounty.player-cooldown", 360);
        if (!bounties.getConfig().isSet("murder-bounty.bounty-increase"))
            bounties.getConfig().set("murder-bounty.bounty-increase", 0);
        if (!bounties.getConfig().isSet("murder-bounty.exclude-claiming"))
            bounties.getConfig().set("murder-bounty.exclude-claiming", true);
        if (!bounties.getConfig().isSet("console-bounty-name"))
            bounties.getConfig().set("console-bounty-name", "Sheriff");



        NumberFormatting.setCurrencyOptions(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("currency")), bounties.getConfig().getConfigurationSection("number-formatting"));


        bountyExpire = bounties.getConfig().getInt("bounty-expire");
        rewardHeadSetter = bounties.getConfig().getBoolean("reward-heads.setters");
        rewardHeadClaimed = bounties.getConfig().getBoolean("reward-heads.claimed");
        buyBack = bounties.getConfig().getBoolean("buy-own-bounties.enabled");
        buyBackInterest = bounties.getConfig().getDouble("buy-own-bounties.cost-multiply");
        immunityType = bounties.getConfig().getInt("immunity.type");
        timeImmunity = bounties.getConfig().getDouble("immunity.time-immunity.seconds");
        permanentCost = bounties.getConfig().getInt("immunity.permanent-immunity.cost");
        scalingRatio = bounties.getConfig().getDouble("immunity.scaling-immunity.ratio");
        graceTime = bounties.getConfig().getInt("immunity.grace-period");
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
        redeemRewardLater = bounties.getConfig().getBoolean("redeem-reward-later");
        minBroadcast = bounties.getConfig().getInt("minimum-broadcast");
        bBountyThreshold = bounties.getConfig().getInt("big-bounties.bounty-threshold");
        bBountyParticle = bounties.getConfig().getBoolean("big-bounties.particle");
        bBountyCommands = bounties.getConfig().getStringList("big-bounties.commands");
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
        murderCooldown = bounties.getConfig().getInt("murder-bounty.player-cooldown");
        murderBountyIncrease = bounties.getConfig().getDouble("murder-bounty.bounty-increase");
        consoleName = bounties.getConfig().getString("console-bounty-name");
        murderExcludeClaiming = bounties.getConfig().getBoolean("murder-bounty.exclude-claiming");


        dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.locale);

        // add immunity that isn't in time tracker - this should only do anything when immunity is switched to time immunity
        if (immunityType == 3) {
            Map<String, Double> immunity = NotBounties.getInstance().SQL.isConnected() ? NotBounties.getInstance().data.getTopStats(Leaderboard.IMMUNITY, new ArrayList<>(), -1, -1) : NotBounties.getInstance().immunitySpent;
            for (Map.Entry<String, Double> entry : immunity.entrySet()) {
                if (!NotBounties.getInstance().immunityTimeTracker.containsKey(UUID.fromString(entry.getKey())))
                    NotBounties.getInstance().immunityTimeTracker.put(UUID.fromString(entry.getKey()), (long) ((entry.getValue() * timeImmunity * 1000) + System.currentTimeMillis()));
            }
        }

        File guiFile = new File(bounties.getDataFolder() + File.separator + "gui.yml");
        if (!guiFile.exists()) {
            bounties.saveResource("gui.yml", false);
        }
        if (bounties.getConfig().isConfigurationSection("advanced-gui")) {
            // migrate everything to gui.yml
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(guiFile);
            configuration.set("bounty-gui", null);
            ConfigurationSection section = bounties.getConfig().getConfigurationSection("advanced-gui");
            assert section != null;
            configuration.set("bounty-gui.sort-type", section.get("sort-type"));
            configuration.set("bounty-gui.size", section.get("size"));
            for (String key : Objects.requireNonNull(section.getConfigurationSection("custom-items")).getKeys(false)) {
                configuration.set("custom-items." + key, section.get("custom-items." + key));
            }
            configuration.set("bounty-gui.player-slots", section.get("bounty-slots"));
            configuration.set("bounty-gui.remove-page-items", true);
            configuration.set("bounty-gui.head-lore", Arrays.asList("&7<&m                        &7>", "&4Bounty: &6%notbounties_bounty_formatted%", "&4&oKill this player to", "&4&oreceive this reward", "&7<&m                        &7>"));
            configuration.set("bounty-gui.head-name", "&4☠ &c&l{player} &4☠");

            configuration.set("bounty-gui.gui-name", "&d&lBounties &9&lPage");
            configuration.set("bounty-gui.add-page", true);

            if (language.exists()) {
                YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(language);
                if (languageConfig.isSet("bounty-item-name") && languageConfig.isSet("bounty-item-lore") && languageConfig.isSet("bounty-item-lore")) {
                    // convert {amount} to %notbounties_bounty_formatted%
                    String unformatted = languageConfig.getString("bounty-item-name");
                    assert unformatted != null;
                    unformatted = unformatted.replaceAll("\\{amount}", "%notbounties_bounty_formatted%");
                    configuration.set("bounty-gui.head-name", unformatted);

                    List<String> unformattedList = languageConfig.getStringList("bounty-item-lore");
                    unformattedList.replaceAll(s -> s.replaceAll("\\{amount}", "%notbounties_bounty_formatted%"));
                    configuration.set("bounty-gui.head-lore", unformattedList);
                    configuration.set("bounty-gui.gui-name", languageConfig.getString("gui-name"));
                    languageConfig.set("gui-name", null);
                    languageConfig.set("bounty-item-name", null);
                    languageConfig.set("bounty-item-lore", null);
                    languageConfig.save(language);
                }
            }

            for (String key : Objects.requireNonNull(section.getConfigurationSection("layout")).getKeys(false)) {
                configuration.set("bounty-gui.layout." + key, section.get("layout." + key));
            }

            bounties.getConfig().set("advanced-gui", null);
            configuration.save(guiFile);
        }

        customItems.clear();
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
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
        }
        for (String key : Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items")).getKeys(false)) {
            CustomItem customItem = new CustomItem(Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items." + key)));
            customItems.put(key, customItem);
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

        guiConfig.save(guiFile);


        if (!speakings.isEmpty())
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                    if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                        player.closeInventory();
                    }
                }
            }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(language);

        if (!configuration.isSet("prefix"))
            configuration.set("prefix", "&7[&9Not&dBounties&7] &8» &r");
        if (!configuration.isSet("unknown-number"))
            configuration.set("unknown-number", "&cUnknown number!");
        if (!configuration.isSet("bounty-success"))
            configuration.set("bounty-success", "&aBounty placed on &e{player}&a for &e{amount}&a!");
        if (!configuration.isSet("unknown-player"))
            configuration.set("unknown-player", "&cCould not find the player &4{player}&c!");
        if (!configuration.isSet("bounty-broadcast"))
            configuration.set("bounty-broadcast", "&e{player}&6 has placed a bounty of &f{amount}&6 on &e{receiver}&6! Total Bounty: &f{bounty}");
        if (!configuration.isSet("no-permission"))
            configuration.set("no-permission", "&cYou do not have permission to execute this command!");
        if (!configuration.isSet("broke"))
            configuration.set("broke", "&cYou do not have enough currency for this! &8Required: &7{amount}");
        if (!configuration.isSet("claim-bounty-broadcast"))
            configuration.set("claim-bounty-broadcast", "&e{player}&6 has claimed the bounty of &f{amount}&6 on &e{receiver}&6!");
        if (!configuration.isSet("no-bounty"))
            configuration.set("no-bounty", "&4{receiver} &cdoesn't have a bounty!");
        if (!configuration.isSet("check-bounty"))
            configuration.set("check-bounty", "&e{receiver}&a has a bounty of &e{amount}&a.");
        if (!configuration.isSet("list-setter"))
            configuration.set("list-setter", "&e{player} &7> &a{amount}");
        if (!configuration.isSet("list-total"))
            configuration.set("list-total", "&e{player} &7> &a{amount}");
        if (!configuration.isSet("offline-bounty"))
            configuration.set("offline-bounty", "&e{player}&6 has set a bounty on you while you were offline!");
        if (!configuration.isSet("success-remove-bounty"))
            configuration.set("success-remove-bounty", "&cSuccessfully removed &4{receiver}'s &cbounty.");
        if (!configuration.isSet("success-edit-bounty"))
            configuration.set("success-edit-bounty", "&cSuccessfully edited &4{receiver}'s &cbounty.");
        if (!configuration.isSet("no-setter"))
            configuration.set("no-setter", "&4{player} &chas not set a bounty on {receiver}");
        if (!configuration.isSet("repeat-command-bounty"))
            configuration.set("repeat-command-bounty", "&6Please type this command in again in the next 30 seconds to confirm buying your bounty for &e{amount}&6.");
        if (!configuration.isSet("repeat-command-immunity"))
            configuration.set("repeat-command-immunity", "&6Please type this command in again in the next 30 seconds to confirm buying immunity for &e{amount}&6.");
        if (!configuration.isSet("permanent-immunity"))
            configuration.set("permanent-immunity", "&6{player} &eis immune to bounties!");
        if (!configuration.isSet("scaling-immunity"))
            configuration.set("scaling-immunity", "&6{player} &eis immune to bounties less than &e{amount}&6.");
        if (!configuration.isSet("time-immunity"))
            configuration.set("time-immunity", "&6{player} &eis immune to bounties for &e{time}&6.");
        if (!configuration.isSet("buy-permanent-immunity"))
            configuration.set("buy-permanent-immunity", "&aYou have bought immunity from bounties.");
        if (!configuration.isSet("buy-scaling-immunity"))
            configuration.set("buy-scaling-immunity", "&aYou have bought immunity from bounties under the amount of &2{amount}&a.");
        if (!configuration.isSet("buy-time-immunity"))
            configuration.set("buy-time-immunity", "&aYou have bought immunity from bounties for &2{time}&a.");
        if (!configuration.isSet("grace-period"))
            configuration.set("grace-period", "&cA bounty had just been claimed on &4{player}&c. Please wait &4{time}&c until you try again.");
        if (!configuration.isSet("min-bounty"))
            configuration.set("min-bounty", "&cThe bounty must be at least &4{amount}&c.");
        if (!configuration.isSet("unknown-command"))
            configuration.set("unknown-command", "&dUse &9/bounty help &dfor a list of commands.");
        if (!configuration.isSet("already-bought-perm"))
            configuration.set("already-bought-perm", "&cYou already have permanent immunity!");
        if (!configuration.isSet("removed-immunity"))
            configuration.set("removed-immunity", "&aSuccessfully removed your immunity to bounties.");
        if (!configuration.isSet("removed-other-immunity"))
            configuration.set("removed-other-immunity", "&aSuccessfully removed &2{receiver}'s &aimmunity to bounties.");
        if (!configuration.isSet("no-immunity"))
            configuration.set("no-immunity", "&cYou do not have purchased immunity!");
        if (!configuration.isSet("no-immunity-other"))
            configuration.set("no-immunity-other", "&4{receiver} &cdoes not have purchased immunity!");
        if (!configuration.isSet("expired-bounty"))
            configuration.set("expired-bounty", "&eYour bounty on &6{player}&e has expired. You have been refunded &2{amount}&e.");
        if (!configuration.isSet("bounty-tracker-name"))
            configuration.set("bounty-tracker-name", "&eBounty Tracker: &6&l{player}");
        if (!configuration.isSet("bounty-tracker-lore"))
            configuration.set("bounty-tracker-lore", Arrays.asList("", "&7Follow this compass", "&7to find {player}", ""));
        if (!configuration.isSet("tracker-give"))
            configuration.set("tracker-give", "&eYou have given &6{receiver}&e a compass that tracks &6{player}&e.");
        if (!configuration.isSet("tracker-receive"))
            configuration.set("tracker-receive", "&eYou have been given a bounty tracker for &6{player}&e.");
        if (!configuration.isSet("tracked-notify"))
            configuration.set("tracked-notify", "&c&lYou are being tracked!");
        if (!configuration.isSet("bounty-top"))
            configuration.set("bounty-top", "&9&l{rank}. &d{player} &7> &a{amount}");
        if (!configuration.isSet("bounty-top-title"))
            configuration.set("bounty-top-title", "&7&m               &r &d&lBounties &9&lTop &7&m               ");
        if (!configuration.isSet("enable-broadcast"))
            configuration.set("enable-broadcast", "&eYou have &aenabled &ebounty broadcast!");
        if (!configuration.isSet("disable-broadcast"))
            configuration.set("disable-broadcast", "&eYou have &cdisabled &ebounty broadcast!");
        if (!configuration.isSet("bounty-voucher-name"))
            configuration.set("bounty-voucher-name", "&6{player}'s&e claimed bounty of &a{amount}&e.");
        if (!configuration.isSet("bounty-voucher-lore"))
            configuration.set("bounty-voucher-lore", Arrays.asList("", "&2Awarded to {receiver}", "&7Right click to redeem", "&7this player's bounty", ""));
        if (!configuration.isSet("redeem-voucher"))
            configuration.set("redeem-voucher", "&aSuccessfully redeemed voucher for {amount}!");
        if (!configuration.isSet("bounty-receiver"))
            configuration.set("bounty-receiver", "&4{player} &cset a bounty on you for &4{amount}&c! Total Bounty: &4{bounty}");
        if (!configuration.isSet("big-bounty"))
            configuration.set("big-bounty", "&eYour bounty is very impressive!");
        if (configuration.isSet("bounty-stat-all")) {
            configuration.set("bounty-stat.all.long", configuration.getString("bounty-stat-all"));
            configuration.set("bounty-stat.kills.long", configuration.getString("bounty-stat-kills"));
            configuration.set("bounty-stat.claimed.long", configuration.getString("bounty-stat-claimed"));
            configuration.set("bounty-stat.deaths.long", configuration.getString("bounty-stat-deaths"));
            configuration.set("bounty-stat.set.long", configuration.getString("bounty-stat-set"));
            configuration.set("bounty-stat.immunity.long", configuration.getString("bounty-stat-immunity"));
        }
        if (!configuration.isSet("bounty-stat.all.long"))
            configuration.set("bounty-stat.all.long", "&eYour all-time bounty is &2{amount}&e.");
        if (!configuration.isSet("bounty-stat.kills.long"))
            configuration.set("bounty-stat.kills.long", "&eYou have killed &6{amount}&e players with bounties.");
        if (!configuration.isSet("bounty-stat.claimed.long"))
            configuration.set("bounty-stat.claimed.long", "&eYou have claimed &2{amount}&e from bounties.");
        if (!configuration.isSet("bounty-stat.deaths.long"))
            configuration.set("bounty-stat.deaths.long", "&eYou have died &6{amount}&e times with a bounty.");
        if (!configuration.isSet("bounty-stat.set.long"))
            configuration.set("bounty-stat.set.long", "&eYou have set &6{amount}&e successful bounties.");
        if (!configuration.isSet("bounty-stat.immunity.long"))
            configuration.set("bounty-stat.immunity.long", "&eYou have spent &2{amount}&e on immunity.");
        if (!configuration.isSet("bounty-stat.all.short"))
            configuration.set("bounty-stat.all.short", "'&6All-time bounty: &e{amount}");
        if (!configuration.isSet("bounty-stat.kills.short"))
            configuration.set("bounty-stat.kills.short", "&6Bounty kills: &e{amount}");
        if (!configuration.isSet("bounty-stat.claimed.short"))
            configuration.set("bounty-stat.claimed.short", "&6Bounty rewards: &e{amount}");
        if (!configuration.isSet("bounty-stat.deaths.short"))
            configuration.set("bounty-stat.deaths.short", "&6Bounty deaths: &e{amount}");
        if (!configuration.isSet("bounty-stat.set.short"))
            configuration.set("bounty-stat.set.short", "&6Bounties set: &e{amount}");
        if (!configuration.isSet("bounty-stat.immunity.short"))
            configuration.set("bounty-stat.immunity.short", "&6Bounty immunity: &e{amount}");
        if (!configuration.isSet("whitelisted-players"))
            configuration.set("whitelisted-players", "&fYour whitelisted players:&7");
        if (!configuration.isSet("whitelist-max"))
            configuration.set("whitelist-max", "&cYou've reached the maximum amount of whitelisted players.");
        if (!configuration.isSet("whitelist-reset"))
            configuration.set("whitelist-reset", "&fYour whitelisted players have been reset.");
        if (!configuration.isSet("whitelist-change"))
            configuration.set("whitelist-change", "&eYour whitelisted players have been changed.");
        if (!configuration.isSet("whitelisted-lore"))
            configuration.set("whitelist-lore", Arrays.asList("&f&lThis player is whitelisted.", ""));
        if (!configuration.isSet("whitelist-notify"))
            configuration.set("whitelist-notify", Arrays.asList("&fYou are whitelisted to this bounty!", ""));
        if (!configuration.isSet("immunity-expire"))
            configuration.set("immunity-expire", "&cYour bounty immunity has expired!");
        if (!configuration.isSet("death-tax"))
            configuration.set("death-tax", "&cYou were killed because of a bounty. You lost &4{items}&c.");
        if (!configuration.isSet("max-setters"))
            configuration.set("max-setters", "&cThe maximum amount of setters for this player has been reached.");
        if (!configuration.isSet("not-whitelisted"))
            configuration.set("not-whitelisted", Collections.singletonList("&cPart of this bounty is whitelisted."));
        if (!configuration.isSet("map-name"))
            configuration.set("map-name", "&6&lWANTED: &f{player}");
        if (!configuration.isSet("map-lore"))
            configuration.set("map-lore", Arrays.asList("", "&6&oREWARD:", "&2{amount}", "&6&oAS OF {time}", ""));
        if (!configuration.isSet("map-give"))
            configuration.set("map-give", "&eYou have given &6{receiver}&e a bounty poster of {player}.");
        if (!configuration.isSet("map-receive"))
            configuration.set("map-receive", "&eYou have been given a bounty poster of &6{player}&e.");
        if (bounties.getConfig().isSet("buy-own-bounties.lore-addition")) {
            List<String> bbLore;
            if (bounties.getConfig().isList("buy-own-bounties.lore-addition")) {
                bbLore = bounties.getConfig().getStringList("buy-own-bounties.lore-addition");
            } else {
                bbLore = Collections.singletonList(bounties.getConfig().getString("buy-own-bounties.lore-addition"));
            }
            configuration.set("buy-back-lore", bbLore);
            bounties.getConfig().set("buy-own-bounties.lore-addition", null);
        }
        if (!configuration.isSet("admin-edit-lore"))
            configuration.set("admin-edit-lore", Arrays.asList("&cLeft Click &7to Remove", "&eRight Click &7to Edit", ""));
        if (configuration.isString("whitelist-lore"))
            configuration.set("whitelist-lore", Arrays.asList(configuration.getString("whitelist-lore"), ""));
        if (configuration.isString("whitelist-notify"))
            configuration.set("whitelist-notify", Arrays.asList(configuration.getString("whitelist-notify"), ""));
        if (!configuration.isSet("murder"))
            configuration.set("murder", "&cYour bounty has been increased for murdering &4{player}&c!");

        bounties.saveConfig();
        configuration.save(language);

        // 0 prefix
        speakings.add(color(Objects.requireNonNull(configuration.getString("prefix"))));
        // 1 unknown-number
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-number"))));
        // 2 bounty-success
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-success"))));
        // 3 unknown-player
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-player"))));
        // 4 bounty-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-broadcast"))));
        // 5 no-permission
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-permission"))));
        // 6 broke
        speakings.add(color(Objects.requireNonNull(configuration.getString("broke"))));
        // 7 claim-bounty-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("claim-bounty-broadcast"))));
        // 8 no-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-bounty"))));
        // 9 check-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("check-bounty"))));
        // 10 list-setter
        speakings.add(color(Objects.requireNonNull(configuration.getString("list-setter"))));
        // 11 list-total
        speakings.add(color(Objects.requireNonNull(configuration.getString("list-total"))));
        // 12 offline-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("offline-bounty"))));
        // 13 whitelisted-players
        speakings.add(color(Objects.requireNonNull(configuration.getString("whitelisted-players"))));
        // 14 success-remove-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("success-remove-bounty"))));
        // 15 success-edit-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("success-edit-bounty"))));
        // 16 no-setter
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-setter"))));
        // 17 repeat-command-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("repeat-command-bounty"))));
        // 18 permanent-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("permanent-immunity"))));
        // 19 scaling-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("scaling-immunity"))));
        // 20 buy-permanent-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("buy-permanent-immunity"))));
        // 21 buy-scaling-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("buy-scaling-immunity"))));
        // 22 grace-period
        speakings.add(color(Objects.requireNonNull(configuration.getString("grace-period"))));
        // 23 min-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("min-bounty"))));
        // 24 unknown-command
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-command"))));
        // 25 already-bought-perm
        speakings.add(color(Objects.requireNonNull(configuration.getString("already-bought-perm"))));
        // 26 removed-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("removed-immunity"))));
        // 27 removed-other-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("removed-other-immunity"))));
        // 28 no-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-immunity"))));
        // 29 no-immunity-other
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-immunity-other"))));
        // 30 repeat-command-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("repeat-command-immunity"))));
        // 31 expired-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("expired-bounty"))));
        // 32 bounty-tracker-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-tracker-name"))));
        // 33 tracker-give
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracker-give"))));
        // 34 tracker-receive
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracker-receive"))));
        // 35 tracked-notify
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracked-notify"))));
        // 36 bounty-top
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-top"))));
        // 37 bounty-top-title
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-top-title"))));
        // 38 enable-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("enable-broadcast"))));
        // 39 disable-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("disable-broadcast"))));
        // 40 bounty-voucher-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-voucher-name"))));
        // 41 redeem-voucher
        speakings.add(color(Objects.requireNonNull(configuration.getString("redeem-voucher"))));
        // 42 bounty-receiver
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-receiver"))));
        // 43 big-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("big-bounty"))));
        // bounty-stat
        // 44 all.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.all.long"))));
        // 45 kills.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.kills.long"))));
        // 46 claimed.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.claimed.long"))));
        // 47 deaths.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.deaths.long"))));
        // 48 set.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.set.long"))));
        // 49 immunity.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.immunity.long"))));
        // 50 all.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.all.short"))));
        // 51 kills.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.kills.short"))));
        // 52 claimed.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.claimed.short"))));
        // 53 deaths.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.deaths.short"))));
        // 54 set.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.set.short"))));
        // 55 immunity.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.immunity.short"))));
        // 56 whitelist-max
        speakings.add(color(Objects.requireNonNull(configuration.getString("whitelist-max"))));
        // 57 whitelist-reset
        speakings.add(color(Objects.requireNonNull(configuration.getString("whitelist-reset"))));
        // 58 whitelist-change
        speakings.add(color(Objects.requireNonNull(configuration.getString("whitelist-change"))));
        // 59 murder
        speakings.add(color(Objects.requireNonNull(configuration.getString("murder"))));
        // 60 immunity-expire
        speakings.add(color(Objects.requireNonNull(configuration.getString("immunity-expire"))));
        // 61 buy-time-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("buy-time-immunity"))));
        // 62 time-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("time-immunity"))));
        // 63
        speakings.add("");
        // 64 death-tax
        speakings.add(color(Objects.requireNonNull(configuration.getString("death-tax"))));
        // 65 max-setters
        speakings.add(color(Objects.requireNonNull(configuration.getString("max-setters"))));
        // 66 map-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("map-name"))));
        // 67 map-give
        speakings.add(color(Objects.requireNonNull(configuration.getString("map-give"))));
        // 68 map-receive
        speakings.add(color(Objects.requireNonNull(configuration.getString("map-receive"))));

        voucherLore = new ArrayList<>();
        configuration.getStringList("bounty-voucher-lore").forEach(str -> voucherLore.add(color(str)));
        trackerLore = new ArrayList<>();
        configuration.getStringList("bounty-tracker-lore").forEach(str -> trackerLore.add(color(str)));
        notWhitelistedLore = new ArrayList<>();
        configuration.getStringList("not-whitelisted").forEach(str -> notWhitelistedLore.add(color(str)));
        mapLore = new ArrayList<>();
        configuration.getStringList("map-lore").forEach(lore -> mapLore.add(color(lore)));
        buyBackLore = new ArrayList<>();
        configuration.getStringList("buy-back-lore").forEach(lore -> buyBackLore.add(color(lore)));
        adminEditLore = new ArrayList<>();
        configuration.getStringList("admin-edit-lore").forEach(lore -> adminEditLore.add(color(lore)));
        whitelistNotify = new ArrayList<>();
        configuration.getStringList("whitelist-notify").forEach(lore -> whitelistNotify.add(color(lore)));
        whitelistLore = new ArrayList<>();
        configuration.getStringList("whitelist-lore").forEach(lore -> whitelistLore.add(color(lore)));
    }


    public static String color(String str) {
        str = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', str);
        return translateHexColorCodes("&#", "", str);
    }

    public static String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }

    public static String parse(String str, String player, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }

    public static String parse(String str, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }
    public static String parse(String str, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", dateFormat.format(time));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }

    public static String parse(String str, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }

    public static String parse(String str, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }
    public static String parse(String str, String player, double amount, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(time)));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }

    public static String parse(String str, String player, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }

    public static String parse(String str, String player, double amount, double bounty, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(time)));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }


    public static String parse(String str, String sender, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }

    public static String parse(String str, String sender, String player, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }

        return str;
    }


    public static String parse(String str, String sender, String player, double amount, double totalBounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            return new PlaceholderAPIClass().parse(receiver, str);
        }
        return str;
    }


}

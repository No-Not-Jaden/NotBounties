package me.jadenp.notbounties.utils;


import me.jadenp.notbounties.Bounty;
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
import java.io.InputStreamReader;
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
    public static final List<String> speakings = new ArrayList<>();
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
    public static int randomBountyMinTime;
    public static int randomBountyMaxTime;
    public static double randomBountyMinPrice;
    public static double randomBountyMaxPrice;
    public static boolean enableBlacklist;
    public static List<String> blacklistLore;
    public static boolean liteBansEnabled;
    public static boolean removeBannedPlayers;
    public static boolean saveTemplates;
    public static String nameLine;
    public static boolean alwaysUpdate;
    public static List<String> bountyClaimCommands = new ArrayList<>();
    public static boolean wanted;
    public static double wantedOffset;
    public static String wantedText;
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
    public static boolean scoreboardTeamClaim;
    public static boolean btClaim;
    public static boolean btAllies;
    public static boolean betterTeamsEnabled;
    public static String teamsPlaceholder;
    public static List<String> rewardHeadLore;
    public static String boardName;
    public static int updateName;
    public static boolean townyAdvancedEnabled;
    public static boolean townyNation;
    public static boolean townyTown;
    public static boolean townyAllies;
    public static boolean RRLVoucherPerSetter;
    public static String RRLSetterLoreAddition;
    public static boolean randomBountyOfflineSet;

    public static void reloadOptions() throws IOException {
        BountyMap.loadFont();
        NotBounties bounties = NotBounties.getInstance();

        papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        liteBansEnabled = Bukkit.getPluginManager().isPluginEnabled("LiteBans");
        skinsRestorerEnabled = Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer");
        betterTeamsEnabled = Bukkit.getPluginManager().isPluginEnabled("BetterTeams");
        townyAdvancedEnabled = Bukkit.getPluginManager().isPluginEnabled("Towny");
        language = new File(bounties.getDataFolder() + File.separator + "language.yml");

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
        if (bounties.getConfig().isSet("immunity.buy-immunity")) {
            if (bounties.getConfig().getBoolean("immunity.buy-immunity")) bounties.getConfig().set("immunity.type", 0);
            else if (bounties.getConfig().getBoolean("immunity.permanent-immunity.enabled")) bounties.getConfig().set("immunity.type", 1);
            else bounties.getConfig().set("immunity.type", 2);
            bounties.getConfig().set("immunity.buy-immunity", null);
            bounties.getConfig().set("immunity.permanent-immunity.enabled", null);
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


        // fill in any missing default settings
        for (String key : Objects.requireNonNull(bounties.getConfig().getDefaults()).getKeys(true)) {
            // Bukkit.getLogger().info("[key] " + key);
            if (!bounties.getConfig().isSet(key)) {
                //Bukkit.getLogger().info("Not set -> " + config.getDefaults().get(key));
                bounties.getConfig().set(key, bounties.getConfig().getDefaults().get(key));
            }
        }

        NumberFormatting.setCurrencyOptions(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("currency")), bounties.getConfig().getConfigurationSection("number-formatting"));
        PVPRestrictions.setPVPRestrictions(Objects.requireNonNull(bounties.getConfig().getConfigurationSection("pvp-restrictions")));

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
        redeemRewardLater = bounties.getConfig().getBoolean("redeem-reward-later.enabled");
        RRLVoucherPerSetter = bounties.getConfig().getBoolean("redeem-reward-later.voucher-per-setter");
        RRLSetterLoreAddition = bounties.getConfig().getString("redeem-reward-later.setter-lore-addition");
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
        randomBountyMinTime = bounties.getConfig().getInt("random-bounties.min-time");
        randomBountyMaxTime = bounties.getConfig().getInt("random-bounties.max-time");
        randomBountyMinPrice = bounties.getConfig().getDouble("random-bounties.min-price");
        randomBountyMaxPrice = bounties.getConfig().getDouble("random-bounties.max-price");
        enableBlacklist = bounties.getConfig().getBoolean("bounty-whitelist.enable-blacklist");
        removeBannedPlayers = bounties.getConfig().getBoolean("remove-banned-players");
        saveTemplates = bounties.getConfig().getBoolean("bounty-posters.save-templates");
        nameLine = bounties.getConfig().getString("bounty-posters.name-line");
        alwaysUpdate = bounties.getConfig().getBoolean("bounty-posters.always-update");
        bountyClaimCommands = bounties.getConfig().getStringList("bounty-claim-commands");
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
        scoreboardTeamClaim = bounties.getConfig().getBoolean("teams.scoreboard-claim");
        btClaim = bounties.getConfig().getBoolean("teams.bt-claim");
        btAllies = bounties.getConfig().getBoolean("teams.bt-allies");
        teamsPlaceholder = bounties.getConfig().getString("teams.placeholder");
        boardName = bounties.getConfig().getString("bounty-board.item-name");
        updateName = bounties.getConfig().getInt("bounty-board.update-name");
        townyNation = bounties.getConfig().getBoolean("teams.towny-nation");
        townyTown = bounties.getConfig().getBoolean("teams.towny-town");
        townyAllies = bounties.getConfig().getBoolean("teams.towny-allies");
        randomBountyOfflineSet = bounties.getConfig().getBoolean("random-bounties.offline-set");

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

        wantedLevels = sortByValue(wantedLevels);

        dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.locale);

        // add immunity that isn't in time tracker - this should only do anything when immunity is switched to time immunity
        if (immunityType == 3) {
            Map<String, Double> immunity = BountyManager.SQL.isConnected() ? BountyManager.data.getTopStats(Leaderboard.IMMUNITY, new ArrayList<>(), -1, -1) : BountyManager.immunitySpent;
            for (Map.Entry<String, Double> entry : immunity.entrySet()) {
                if (!NotBounties.immunityTimeTracker.containsKey(UUID.fromString(entry.getKey())))
                    NotBounties.immunityTimeTracker.put(UUID.fromString(entry.getKey()), (long) ((entry.getValue() * timeImmunity * 1000) + System.currentTimeMillis()));
            }
        }

        // stop next random bounty if it is changed
        if (randomBountyMinTime == 0 && NotBounties.nextRandomBounty != 0)
            NotBounties.nextRandomBounty = 0;
        if (randomBountyMinTime != 0 && NotBounties.nextRandomBounty == 0)
            NotBounties.setNextRandomBounty();

        File guiFile = new File(bounties.getDataFolder() + File.separator + "gui.yml");
        if (!guiFile.exists()) {
            bounties.saveResource("gui.yml", false);
        }
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
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

            if (language.exists()) {
                YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(language);
                if (languageConfig.isSet("bounty-item-name") && languageConfig.isSet("bounty-item-lore") && languageConfig.isSet("bounty-item-lore")) {
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
                    languageConfig.save(language);
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
            if (guiChanges) {
                guiConfig.save(guiFile);
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



        if (!speakings.isEmpty())
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                    if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                        player.closeInventory();
                    }
                }
            }


        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(language);
        if (configuration.isSet("bounty-stat-all")) {
            configuration.set("bounty-stat.all.long", configuration.getString("bounty-stat-all"));
            configuration.set("bounty-stat.kills.long", configuration.getString("bounty-stat-kills"));
            configuration.set("bounty-stat.claimed.long", configuration.getString("bounty-stat-claimed"));
            configuration.set("bounty-stat.deaths.long", configuration.getString("bounty-stat-deaths"));
            configuration.set("bounty-stat.set.long", configuration.getString("bounty-stat-set"));
            configuration.set("bounty-stat.immunity.long", configuration.getString("bounty-stat-immunity"));
            configuration.set("bounty-stat-all", null);
        }
        if (bounties.getConfig().isSet("buy-own-bounties.lore-addition") && !configuration.isSet("buy-back-lore")) {
            List<String> bbLore;
            if (bounties.getConfig().isList("buy-own-bounties.lore-addition")) {
                bbLore = bounties.getConfig().getStringList("buy-own-bounties.lore-addition");
            } else {
                bbLore = Collections.singletonList(bounties.getConfig().getString("buy-own-bounties.lore-addition"));
            }
            configuration.set("buy-back-lore", bbLore);
            bounties.getConfig().set("buy-own-bounties.lore-addition", null);
        }

        // fill in any default options that aren't present
        configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("language.yml")))));
        for (String key : Objects.requireNonNull(configuration.getDefaults()).getKeys(true)) {
            if (!configuration.isSet(key))
                configuration.set(key, configuration.getDefaults().get(key));
        }

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
        // 63 whitelist-toggle
        speakings.add(color(Objects.requireNonNull(configuration.getString("whitelist-toggle"))));
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
        // 69 blacklist-toggle
        speakings.add(color(Objects.requireNonNull(configuration.getString("blacklist-toggle"))));
        // 70 reward-head-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("reward-head-name"))));

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
        blacklistLore = new ArrayList<>();
        configuration.getStringList("blacklist-lore").forEach(lore -> blacklistLore.add(color(lore)));
        rewardHeadLore = new ArrayList<>();
        configuration.getStringList("reward-head-lore").forEach(lore -> rewardHeadLore.add(color(lore)));
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
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }
    public static String parse(String str, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", dateFormat.format(time));
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (papiEnabled && receiver != null) {
            str = color(new PlaceholderAPIClass().parse(receiver, str));
        }
        return color(str);
    }

    public static String parse(String str, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }
    public static String parse(String str, String player, double amount, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(time)));
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String player, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String player, double amount, double bounty, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(time)));
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }


    public static String parse(String str, String sender, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String sender, String player, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }

        return color(str);
    }


    public static String parse(String str, String sender, String player, double amount, double totalBounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(dateFormat.format(new Date())));
        if (papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
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

    public static String getWantedDisplayText(OfflinePlayer player){
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
        return parse(wantedText.replaceAll("\\{level}", Matcher.quoteReplacement(levelReplace)), bounty.getTotalBounty(), player);
    }
}

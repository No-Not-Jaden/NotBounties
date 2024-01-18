package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class LanguageOptions {
    public static String prefix;
    public static String unknownNumber;
    public static String bountySuccess;
    public static String unknownPlayer;
    public static String bountyBroadcast;
    public static String noPermission;
    public static String broke;
    public static String claimBountyBroadcast;
    public static String noBounty;
    public static String checkBounty;
    public static String listSetter;
    public static String listTotal;
    public static String offlineBounty;
    public static String whitelistedPlayers;
    public static String successRemoveBounty;
    public static String successEditBounty;
    public static String noSetter;
    public static String repeatCommandBounty;
    public static String permanentImmunity;
    public static String scalingImmunity;
    public static String buyPermanentImmunity;
    public static String buyScalingImmunity;
    public static String gracePeriod;
    public static String minBounty;
    public static String unknownCommand;
    public static String alreadyBoughtPerm;
    public static String removedImmunity;
    public static String removedOtherImmunity;
    public static String noImmunity;
    public static String noImmunityOther;
    public static String repeatCommandImmunity;
    public static String expiredBounty;
    public static String bountyTrackerName;
    public static String trackerGive;
    public static String trackerReceive;
    public static String trackedNotify;
    public static String bountyTop;
    public static String bountyTopTitle;
    public static String enableBroadcast;
    public static String disableBroadcast;
    public static String bountyVoucherName;
    public static String redeemVoucher;
    public static String bountyReceiver;
    public static String bigBounty;
    public static String bountyStatDeathsLong;
    public static String bountyStatKillsLong;
    public static String bountyStatClaimedLong;
    public static String bountyStatSetLong;
    public static String bountyStatImmunityLong;
    public static String bountyStatAllLong;
    public static String bountyStatAllShort;
    public static String bountyStatKillsShort;
    public static String bountyStatClaimedShort;
    public static String bountyStatImmunityShort;
    public static String bountyStatDeathsShort;
    public static String bountyStatSetShort;
    public static String whitelistMax;
    public static String whitelistReset;
    public static String whitelistChange;
    public static String murder;
    public static String immunityExpire;
    public static String buyTimeImmunity;
    public static String timeImmunity;
    public static String whitelistToggle;
    public static String deathTax;
    public static String maxSetters;
    public static String mapName;
    public static String mapGive;
    public static String mapReceive;
    public static String blacklistToggle;
    public static String rewardHeadName;
    public static String helpTitle;
    public static List<String> trackerLore;
    public static List<String> voucherLore;
    public static List<String> notWhitelistedLore;
    public static List<String> mapLore;
    public static List<String> whitelistNotify;
    public static List<String> whitelistLore;
    public static List<String> adminEditLore;
    public static List<String> blacklistLore;
    public static List<String> rewardHeadLore;
    public static List<String> buyBackLore;
    private static List<String> helpBasic;
    private static List<String> helpView;
    private static List<String> helpSet;
    private static List<String> helpWhitelist;
    private static List<String> helpBlacklist;
    private static List<String> helpBuyOwn;
    private static List<String> helpBuyImmunityPermanent;
    private static List<String> helpBuyImmunityScaling;
    private static List<String> helpBuyImmunityTime;
    private static List<String> helpRemoveImmunity;
    private static List<String> helpTrackerOwn;
    private static List<String> helpTrackerOther;
    private static List<String> helpAdmin;
    private static List<String> helpImmune;
    private static List<String> helpPosterOwn;
    private static List<String> helpPosterOther;

    public static File getLanguageFile() {
        return new File(NotBounties.getInstance().getDataFolder() + File.separator + "language.yml");
    }

    public static void reloadOptions() throws IOException {
        NotBounties bounties = NotBounties.getInstance();
        File language = getLanguageFile();

        // create language file if it doesn't exist
        if (!language.exists()) {
            bounties.saveResource("language.yml", false);
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
        configuration.save(language);
        bounties.saveConfig();

        prefix = configuration.getString("prefix");
        unknownNumber = configuration.getString("unknown-number");
        bountySuccess = configuration.getString("bounty-success");
        unknownPlayer = configuration.getString("unknown-player");
        bountyBroadcast = configuration.getString("bounty-broadcast");
        noPermission = configuration.getString("no-permission");
        broke = configuration.getString("broke");
        claimBountyBroadcast = configuration.getString("claim-bounty-broadcast");
        noBounty = configuration.getString("no-bounty");
        checkBounty = configuration.getString("check-bounty");
        listSetter = configuration.getString("list-setter");
        listTotal = configuration.getString("list-total");
        offlineBounty = configuration.getString("offline-bounty");
        whitelistedPlayers = configuration.getString("whitelisted-players");
        successRemoveBounty = configuration.getString("success-remove-bounty");
        successEditBounty = configuration.getString("success-edit-bounty");
        noSetter = configuration.getString("no-setter");
        repeatCommandBounty = configuration.getString("repeat-command-bounty");
        permanentImmunity = configuration.getString("permanent-immunity");
        scalingImmunity = configuration.getString("scaling-immunity");
        buyPermanentImmunity = configuration.getString("buy-permanent-immunity");
        buyScalingImmunity = configuration.getString("buy-scaling-immunity");
        gracePeriod = configuration.getString("grace-period");
        minBounty = configuration.getString("min-bounty");
        unknownCommand = configuration.getString("unknown-command");
        alreadyBoughtPerm = configuration.getString("already-bought-perm");
        removedImmunity = configuration.getString("removed-immunity");
        removedOtherImmunity = configuration.getString("removed-other-immunity");
        noImmunity = configuration.getString("no-immunity");
        noImmunityOther = configuration.getString("no-immunity-other");
        repeatCommandImmunity = configuration.getString("repeat-command-immunity");
        expiredBounty = configuration.getString("expired-bounty");
        bountyTrackerName = configuration.getString("bounty-tracker-name");
        trackerGive = configuration.getString("tracker-give");
        trackerReceive = configuration.getString("tracker-receive");
        trackedNotify = configuration.getString("tracked-notify");
        bountyTop = configuration.getString("bounty-top");
        bountyTopTitle = configuration.getString("bounty-top-title");
        enableBroadcast = configuration.getString("enable-broadcast");
        disableBroadcast = configuration.getString("disable-broadcast");
        bountyVoucherName = configuration.getString("bounty-voucher-name");
        redeemVoucher = configuration.getString("redeem-voucher");
        bountyReceiver = configuration.getString("bounty-receiver");
        bigBounty = configuration.getString("big-bounty");
        bountyStatAllLong = configuration.getString("bounty-stat.all.long");
        bountyStatKillsLong = configuration.getString("bounty-stat.kills.long");
        bountyStatClaimedLong = configuration.getString("bounty-stat.claimed.long");
        bountyStatDeathsLong = configuration.getString("bounty-stat.deaths.long");
        bountyStatSetLong = configuration.getString("bounty-stat.set.long");
        bountyStatImmunityLong = configuration.getString("bounty-stat.immunity.long");
        bountyStatAllShort = configuration.getString("bounty-stat.all.short");
        bountyStatKillsShort = configuration.getString("bounty-stat.kills.short");
        bountyStatClaimedShort = configuration.getString("bounty-stat.claimed.short");
        bountyStatDeathsShort = configuration.getString("bounty-stat.deaths.short");
        bountyStatSetShort = configuration.getString("bounty-stat.set.short");
        bountyStatImmunityShort = configuration.getString("bounty-stat.immunity.short");
        whitelistMax = configuration.getString("whitelist-max");
        whitelistReset = configuration.getString("whitelist-reset");
        whitelistChange = configuration.getString("whitelist-change");
        murder = configuration.getString("murder");
        immunityExpire = configuration.getString("immunity-expire");
        buyTimeImmunity = configuration.getString("buy-time-immunity");
        timeImmunity = configuration.getString("time-immunity");
        whitelistToggle = configuration.getString("whitelist-toggle");
        deathTax = configuration.getString("death-tax");
        maxSetters = configuration.getString("max-setters");
        mapName = configuration.getString("map-name");
        mapGive = configuration.getString("map-give");
        mapReceive = configuration.getString("map-receive");
        blacklistToggle = configuration.getString("blacklist-toggle");
        rewardHeadName = configuration.getString("reward-head-name");
        helpTitle = configuration.getString("help.title");

        voucherLore = configuration.getStringList("bounty-voucher-lore");
        trackerLore = configuration.getStringList("bounty-tracker-lore");
        notWhitelistedLore = configuration.getStringList("not-whitelisted");
        mapLore = configuration.getStringList("map-lore");
        buyBackLore = configuration.getStringList("buy-back-lore");
        adminEditLore = configuration.getStringList("admin-edit-lore");
        whitelistNotify = configuration.getStringList("whitelist-notify");
        whitelistLore = configuration.getStringList("whitelist-lore");
        blacklistLore = configuration.getStringList("blacklist-lore");
        rewardHeadLore = configuration.getStringList("blacklist-lore");
        helpBasic = configuration.getStringList("help.basic");
        helpSet = configuration.getStringList("help.set");
        helpView = configuration.getStringList("help.view");
        helpWhitelist = configuration.getStringList("help.whitelist");
        helpBlacklist = configuration.getStringList("help.blacklist");
        helpBuyOwn = configuration.getStringList("help.buy-own");
        helpBuyImmunityPermanent = configuration.getStringList("help.buy-immunity.permanent");
        helpBuyImmunityScaling = configuration.getStringList("help.buy-immunity.scaling");
        helpBuyImmunityTime = configuration.getStringList("help.buy-immunity.time");
        helpRemoveImmunity = configuration.getStringList("help.remove-immunity");
        helpTrackerOwn = configuration.getStringList("help.tracker-own");
        helpTrackerOther = configuration.getStringList("help.tracker-other");
        helpAdmin = configuration.getStringList("help.admin");
        helpImmune = configuration.getStringList("help.immune");
        helpPosterOwn = configuration.getStringList("help.poster-own");
        helpPosterOther = configuration.getStringList("help.poster-other");
    }

    public static void sendHelpMessage(CommandSender sender) {
        Player parser = sender instanceof Player ? (Player) sender : null;
        sender.sendMessage(parse(prefix + helpTitle, parser));
        sendHelpMessage(sender, helpBasic);
        if (sender.hasPermission("notbounties.view")) {
            sendHelpMessage(sender, helpView);
        }
        if (sender.hasPermission("notbounties.set")) {
            sendHelpMessage(sender, helpSet);
        }
        if (sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled) {
            sendHelpMessage(sender, helpWhitelist);
            if (enableBlacklist) {
                sendHelpMessage(sender, helpBlacklist);
            }
        }
        if (sender.hasPermission("notbounties.buyown") & buyBack) {
            sendHelpMessage(sender, helpBuyOwn);
        }
        if (sender.hasPermission("notbounties.buyimmunity") && immunityType > 0) {
            if (immunityType == 1) {
                sendHelpMessage(sender, helpBuyImmunityPermanent);
            } else if (immunityType == 2) {
                sendHelpMessage(sender, helpBuyImmunityScaling);
            } else if (immunityType == 3) {
                sendHelpMessage(sender, helpBuyImmunityTime);
            }
        }
        if (sender.hasPermission("notbounties.removeimmunity")) {
            sendHelpMessage(sender, helpRemoveImmunity);
        }
        if (sender.hasPermission("notbounties.admin") || giveOwnMap) {
            sendHelpMessage(sender, helpPosterOwn);
            if (sender.hasPermission("notbounties.admin"))
                sendHelpMessage(sender, helpPosterOther);
        }
        if (tracker)
            if (sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker"))) {
                sendHelpMessage(sender, helpTrackerOwn);
                if (sender.hasPermission("notbounties.admin"))
                    sendHelpMessage(sender, helpTrackerOther);
            }
        if (sender.hasPermission("notbounties.admin")) {
            sendHelpMessage(sender, helpAdmin);
        }

        if (sender.hasPermission("notbounties.immune")) {
            sendHelpMessage(sender, helpImmune);
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                 ");
    }

    private static void sendHelpMessage(CommandSender sender, List<String> message) {
        Player parser = sender instanceof Player ? (Player) sender : null;
        for (String str : message) {
            str = str.replaceAll("\\{whitelist}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(ConfigOptions.bountyWhitelistCost) + NumberFormatting.currencySuffix));
            str = str.replaceAll("\\{tax}", Matcher.quoteReplacement(NumberFormatting.formatNumber(ConfigOptions.bountyTax * 100)));
            str = str.replaceAll("\\{buy_back_interest}", Matcher.quoteReplacement(NumberFormatting.formatNumber(ConfigOptions.buyBackInterest)));
            str = str.replaceAll("\\{permanent_cost}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(ConfigOptions.permanentCost) + NumberFormatting.currencySuffix));
            str = str.replaceAll("\\{scaling_ratio}", Matcher.quoteReplacement(NumberFormatting.formatNumber(ConfigOptions.scalingRatio)));
            str = str.replaceAll("\\{time_immunity}", Matcher.quoteReplacement(NumberFormatting.formatNumber(ConfigOptions.timeImmunity)));
            sender.sendMessage(parse(str, parser));
        }
    }

    public static String parse(String str, String player, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", ConfigOptions.dateFormat.format(time));
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (receiver != null && receiver.getName() != null) {
            str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
            str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
        }
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = color(new PlaceholderAPIClass().parse(receiver, str));
        }
        return color(str);
    }

    public static String parse(String str, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String player, double amount, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(time)));
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String player, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String player, double amount, double bounty, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(time)));
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String sender, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, String sender, String player, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }

        return color(str);
    }

    public static String parse(String str, String sender, String player, double amount, double totalBounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.currencySuffix));
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String color(String str) {
        str = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', str);
        return translateHexColorCodes("&#", "", str);
    }

    public static String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
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
}
package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.Whitelist;
import me.jadenp.notbounties.utils.externalAPIs.PlaceholderAPIClass;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
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
    public static String updateStat;
    public static String selfSetDeny;
    public static String entityRemove;

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
    public static List<String> helpBasic;
    public static List<String> helpView;
    public static List<String> helpSet;
    public static List<String> helpWhitelist;
    public static List<String> helpBlacklist;
    public static List<String> helpBuyOwn;
    public static List<String> helpBuyImmunityPermanent;
    public static List<String> helpBuyImmunityScaling;
    public static List<String> helpBuyImmunityTime;
    public static List<String> helpRemoveImmunity;
    public static List<String> helpTrackerOwn;
    public static List<String> helpTrackerOther;
    public static List<String> helpAdmin;
    public static List<String> helpImmune;
    public static List<String> helpPosterOwn;
    public static List<String> helpPosterOther;
    public static List<String> helpRemoveSet;
    public static String nextPage;
    public static String previousPage;

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
        if (NotBounties.getInstance().getResource("language.yml") != null) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("language.yml")))));
            for (String key : Objects.requireNonNull(configuration.getDefaults()).getKeys(true)) {
                if (!configuration.isSet(key))
                    configuration.set(key, configuration.getDefaults().get(key));
            }
            configuration.save(language);
        }
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
        updateStat = configuration.getString("update-stat");
        selfSetDeny = configuration.getString("self-set-deny");
        entityRemove = configuration.getString("entity-remove");

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
        helpRemoveSet = configuration.getStringList("help.remove-set");
        previousPage = configuration.getString("help.previous-page");
        nextPage = configuration.getString("help.next-page");
    }

    private static int getAdjustedPage(CommandSender sender, int page) {
        if (page == 2 && !sender.hasPermission("notbounties.view"))
            page++;
        if (page == 3 && !sender.hasPermission("notbounties.set"))
            page++;
        if (page == 4 && !(sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled))
            page++;
        if (page == 5 && !(sender.hasPermission("notbounties.buyown") && buyBack) && !(sender.hasPermission("notbounties.buyimmunity") && Immunity.immunityType != Immunity.ImmunityType.DISABLE))
            page++;
        if (page == 6 && !sender.hasPermission("notbounties.removeimmunity") && !(sender.hasPermission("notbounties.removeset") && !sender.hasPermission("notbounties.admin")))
            page++;
        if (page == 7 && !(sender.hasPermission("notbounties.admin") || (giveOwnTracker && sender.hasPermission("notbounties.tracker"))) && !(sender.hasPermission("notbounties.admin") || giveOwnMap))
            page++;
        if (page == 8 && !sender.hasPermission("notbounties.admin"))
            page++;
        return page;
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
        if (sender.hasPermission("notbounties.buyimmunity") && Immunity.immunityType != Immunity.ImmunityType.DISABLE) {
            switch (Immunity.immunityType) {
                case PERMANENT:
                    sendHelpMessage(sender, helpBuyImmunityPermanent);
                    break;
                case SCALING:
                    sendHelpMessage(sender, helpBuyImmunityScaling);
                    break;
                case TIME:
                    sendHelpMessage(sender, helpBuyImmunityTime);
                    break;
            }
        }
        if (sender.hasPermission("notbounties.removeimmunity")) {
            sendHelpMessage(sender, helpRemoveImmunity);
        }
        if (sender.hasPermission("notbounties.removeset") && !sender.hasPermission("notbounties.admin")) {
            sendHelpMessage(sender, helpRemoveSet);
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

    public static void sendHelpMessage(CommandSender sender, int page) {
        if (!(sender instanceof Player)) {
            sendHelpMessage(sender);
            return;
        }
        Player parser = (Player) sender;
        sender.sendMessage(parse(helpTitle, parser));
        page = getAdjustedPage(sender, page);


        switch (page) {
            case 1:
                // basic
                sendHelpMessage(sender, helpBasic);
                if (sender.hasPermission("notbounties.immune")) {
                    sendHelpMessage(sender, helpImmune);
                }
                break;
            case 2:
                // view
                sendHelpMessage(sender, helpView);
                break;
            case 3:
                // set
                sendHelpMessage(sender, helpSet);
                break;
            case 4:
                // whitelist
                sendHelpMessage(sender, helpWhitelist);
                if (enableBlacklist)
                    sendHelpMessage(sender, helpBlacklist);
                break;
            case 5:
                // buy
                if (sender.hasPermission("notbounties.buyown") & buyBack) {
                    sendHelpMessage(sender, helpBuyOwn);
                }
                if (sender.hasPermission("notbounties.buyimmunity") && Immunity.immunityType != Immunity.ImmunityType.DISABLE) {
                    switch (Immunity.immunityType) {
                        case PERMANENT:
                            sendHelpMessage(sender, helpBuyImmunityPermanent);
                            break;
                        case SCALING:
                            sendHelpMessage(sender, helpBuyImmunityScaling);
                            break;
                        case TIME:
                            sendHelpMessage(sender, helpBuyImmunityTime);
                            break;
                    }
                }
                break;
            case 6:
                // remove
                if (sender.hasPermission("notbounties.removeimmunity"))
                    sendHelpMessage(sender, helpRemoveImmunity);
                if (sender.hasPermission("notbounties.removeset") && !sender.hasPermission("notbounties.admin"))
                    sendHelpMessage(sender, helpRemoveSet);
                break;
            case 7:
                // item
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
                break;
            case 8:
                // admin
                sendHelpMessage(sender, helpAdmin);
                break;
        }

        sendPageLine(sender, page);
    }

    public static void sendPageLine(CommandSender sender, int currentPage) {
        int previousPage = currentPage-1;
        int calculatedPrevPage = getAdjustedPage(sender, previousPage);
        while (previousPage > 0 && calculatedPrevPage >= currentPage) {
            previousPage--;
            calculatedPrevPage = getAdjustedPage(sender, previousPage);
        }
        int nextPage = getAdjustedPage(sender, currentPage + 1);
        // end points are 0 and 9 (no next page or previous page)
        TextComponent space = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "     ");
        TextComponent back = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋘⋘⋘");
        TextComponent middle = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "             " + ChatColor.GRAY + "[" + currentPage + "]" + ChatColor.STRIKETHROUGH + "              ");
        TextComponent next = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋙⋙⋙");
        back.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(LanguageOptions.previousPage, null))));
        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(LanguageOptions.nextPage, null))));
        back.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties help " + previousPage));
        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties help " + nextPage));
        BaseComponent[] baseComponents = new BaseComponent[]{space, space, back, middle, next, space, space};
        if (previousPage == 0)
            baseComponents[2] = space;
        if (nextPage == 9)
            baseComponents[4] = space;
        sender.spigot().sendMessage(baseComponents);
    }

    public static void sendHelpMessage(CommandSender sender, List<String> message) {
        Player parser = sender instanceof Player ? (Player) sender : null;
        for (String str : message) {
            str = str.replaceAll("\\{whitelist}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(ConfigOptions.bountyWhitelistCost) + NumberFormatting.currencySuffix));
            str = str.replaceAll("\\{tax}", Matcher.quoteReplacement(NumberFormatting.formatNumber(ConfigOptions.bountyTax * 100)));
            str = str.replaceAll("\\{buy_back_interest}", Matcher.quoteReplacement(NumberFormatting.formatNumber(ConfigOptions.buyBackInterest)));
            str = str.replaceAll("\\{permanent_cost}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(Immunity.getPermanentCost()) + NumberFormatting.currencySuffix));
            str = str.replaceAll("\\{scaling_ratio}", Matcher.quoteReplacement(NumberFormatting.formatNumber(Immunity.getScalingRatio())));
            str = str.replaceAll("\\{time_immunity}", Matcher.quoteReplacement(formatTime((long) (Immunity.getTime() * 1000L))));
            sender.sendMessage(parse(str, parser));
        }
    }

    public static String parse(String str, String player, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(player));
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        return parse(str, receiver);
    }

    public static String parse(String str, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(new Date())));
        str = str.replaceAll("\\{min_bounty}", Matcher.quoteReplacement(NumberFormatting.getValue(ConfigOptions.minBounty)));
        str = str.replaceAll("\\{c_prefix}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix));
        str = str.replaceAll("\\{c_suffix}", Matcher.quoteReplacement(NumberFormatting.currencySuffix));

        if (receiver != null) {
            Bounty bounty = BountyManager.getBounty(receiver);
            if (bounty != null) {
                str = str.replaceAll("\\{min_expire}", Matcher.quoteReplacement(formatTime(BountyExpire.getLowestExpireTime(bounty))));
                str = str.replaceAll("\\{max_expire}", Matcher.quoteReplacement(formatTime(BountyExpire.getHighestExpireTime(bounty))));
            } else {
                str = str.replaceAll("\\{min_expire}", "");
                str = str.replaceAll("\\{max_expire}", "");
            }
            if (receiver.getName() != null) {
                str = str.replaceAll("\\{player}", Matcher.quoteReplacement(receiver.getName()));
                str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(receiver.getName()));
            }
            str = str.replaceAll("\\{balance}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(NumberFormatting.getBalance(receiver)) + NumberFormatting.currencySuffix));
            Whitelist whitelist = NotBounties.getPlayerWhitelist(receiver.getUniqueId());
            str = str.replaceAll("\\{whitelist}", Matcher.quoteReplacement(whitelist.toString()));
            String mode = whitelist.isBlacklist() ? "Blacklist" : "Whitelist";
            str = str.replaceAll("\\{mode}", Matcher.quoteReplacement(mode));
            // {whitelist2} turns into the name of the second player in the receiver's whitelist
            while (str.contains("{whitelist") && str.substring(str.indexOf("{whitelist")).contains("}")) {
                int num;
                String stringValue = str.substring(str.indexOf("{whitelist") + 10, str.indexOf("{whitelist") + str.substring(str.indexOf("{whitelist")).indexOf(">}"));
                try {
                    num = Integer.parseInt(stringValue);
                } catch (NumberFormatException e) {
                    str = str.replace("{whitelist" + stringValue + "}", "<Error>");
                    continue;
                }
                if (num < 1)
                    num = 1;
                if (whitelist.getList().size() > num)
                    str = str.replace("{whitelist" + stringValue + "}", "");
                else
                    str = str.replace("{whitelist" + stringValue + "}", NotBounties.getPlayerName(whitelist.getList().get(num-1)));
            }
        }
        if (ConfigOptions.papiEnabled && receiver != null) {
            str = new PlaceholderAPIClass().parse(receiver, str);
        }
        return color(str);
    }

    public static String parse(String str, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", ConfigOptions.dateFormat.format(time));
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(time + ""));
        return parse(str, receiver);
    }

    public static String parse(String str, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        return parse(str, receiver);
    }

    public static String parse(String str, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{player}", Matcher.quoteReplacement(player));
        return parse(str,amount,receiver);
    }

    public static String parse(String str, String player, double amount, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(time)));
        return parse(str, player, amount, receiver);
    }

    public static String parse(String str, String player, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        return parse(str, player, amount, receiver);
    }

    public static String parse(String str, String player, double amount, double bounty, long time, OfflinePlayer receiver) {
        str = str.replaceAll("\\{time}", Matcher.quoteReplacement(ConfigOptions.dateFormat.format(time)));
        return parse(str, player, amount, bounty, receiver);
    }

    public static String parse(String str, String sender, String player, double amount, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        return parse(str, player, amount, receiver);
    }

    public static String parse(String str, String sender, String player, OfflinePlayer receiver) {
        str = str.replaceAll("\\{receiver}", Matcher.quoteReplacement(sender));
        return parse(str, player, receiver);
    }

    public static String parse(String str, String sender, String player, double amount, double totalBounty, OfflinePlayer receiver) {
        str = str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.currencySuffix));
        return parse(str, sender, player, amount, receiver);
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

    public static String formatTime(long ms) {
        long days = (long) (ms / (8.64 * Math.pow(10,7)));
        ms = (long) (ms % (8.64 * Math.pow(10,7)));
        long hours = ms / 3600000L;
        ms = ms % 3600000L;
        long minutes = ms / 60000L;
        ms = ms % 60000L;
        long seconds = ms / 1000L;
        String time = "";
        if (days > 0) time += days + "d ";
        if (hours > 0) time += hours + "h ";
        if (minutes > 0) time += minutes + "m ";
        if (seconds > 0) time += seconds + "s";
        if (time.isEmpty())
            return "0s";
        return time;
    }
}

package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.Whitelist;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import me.jadenp.notbounties.utils.externalAPIs.PlaceholderAPIClass;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.externalAPIs.LocalTime.formatTime;
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
    public static String promptCancel;
    public static String promptExpire;
    public static String combatTag;
    public static String combatSafe;
    public static String waitCommand;
    public static String emptyTrackerName;
    public static String trackerOffline;
    public static String trackerNoPermission;
    public static String trackerReceiveEmpty;
    public static String unknownMaterial;
    public static String noEmptyTracker;
    public static String stolenBounty;
    public static String stolenBountyBroadcast;
    public static String challengeCompletion;
    public static String challengeGUIClaim;
    public static String challengeChatClaim;
    public static String challengeClaimDeny;
    public static String trickleBounty;
    public static String bedrockOpenGUI;
    public static String paused;
    public static String playerPrefix;
    public static String playerSuffix;
    public static String bountySetCooldown;

    public static List<String> emptyTrackerLore;
    public static List<String> giveTrackerLore;
    public static List<String> givePosterLore;
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
    public static List<String> helpChallenges;
    public static List<String> viewBountyLore;
    public static List<String> removeBountyLore;
    public static List<String> editBountyLore;
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
        boolean saveChanges = true;
        if (configuration.getKeys(true).size() <= 2) {
            saveChanges = false;
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the language.yml file. Fix the YAML formatting errors, or the messages may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
        }
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
            bounties.saveConfig();
        }

        // fill in any default options that aren't present
        if (NotBounties.getInstance().getResource("language.yml") != null) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("language.yml")))));
            for (String key : Objects.requireNonNull(configuration.getDefaults()).getKeys(true)) {
                if (!configuration.isSet(key))
                    configuration.set(key, configuration.getDefaults().get(key));
            }
            if (saveChanges)
                configuration.save(language);
        }


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
        promptCancel = configuration.getString("prompt-cancel");
        promptExpire = configuration.getString("prompt-expire");
        combatTag = configuration.getString("combat-tag");
        combatSafe = configuration.getString("combat-safe");
        waitCommand = configuration.getString("wait-command");
        emptyTrackerName = configuration.getString("empty-tracker-name");
        trackerOffline = configuration.getString("tracker-offline");
        trackerNoPermission = configuration.getString("tracker-no-permission");
        trackerReceiveEmpty = configuration.getString("tracker-receive-empty");
        unknownMaterial = configuration.getString("unknown-material");
        noEmptyTracker = configuration.getString("no-empty-tracker");
        stolenBounty = configuration.getString("stolen-bounty");
        stolenBountyBroadcast = configuration.getString("stolen-bounty-broadcast");
        challengeCompletion = configuration.getString("challenge-completion");
        challengeGUIClaim = configuration.getString("challenge-gui-claim");
        challengeChatClaim = configuration.getString("challenge-chat-claim");
        challengeClaimDeny = configuration.getString("challenge-claim-deny");
        trickleBounty = configuration.getString("trickle-bounty");
        bedrockOpenGUI = configuration.getString("bedrock-open-gui");
        paused = configuration.getString("paused");
        playerPrefix = configuration.getString("player-prefix");
        playerSuffix = configuration.getString("player-suffix");
        bountySetCooldown = configuration.getString("bounty-set-cooldown");

        emptyTrackerLore = configuration.getStringList("empty-tracker-lore");
        giveTrackerLore = configuration.getStringList("give-tracker-lore");
        givePosterLore = configuration.getStringList("give-poster-lore");
        voucherLore = configuration.getStringList("bounty-voucher-lore");
        trackerLore = configuration.getStringList("bounty-tracker-lore");
        notWhitelistedLore = configuration.getStringList("not-whitelisted");
        mapLore = configuration.getStringList("map-lore");
        buyBackLore = configuration.getStringList("buy-back-lore");
        adminEditLore = configuration.getStringList("admin-edit-lore");
        whitelistNotify = configuration.getStringList("whitelist-notify");
        whitelistLore = configuration.getStringList("whitelist-lore");
        blacklistLore = configuration.getStringList("blacklist-lore");
        rewardHeadLore = configuration.getStringList("reward-head-lore");
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
        helpChallenges = configuration.getStringList("help.challenges");
        viewBountyLore = configuration.getStringList("view-bounty-lore");
        editBountyLore = configuration.getStringList("edit-bounty-lore");
        removeBountyLore = configuration.getStringList("remove-bounty-lore");
        previousPage = configuration.getString("help.previous-page");
        nextPage = configuration.getString("help.next-page");
    }

    private static int getAdjustedPage(CommandSender sender, int page) {
        if (page < 1)
            page = 1;
        if (page == 2 && !sender.hasPermission("notbounties.view"))
            page++;
        if (page == 3 && !sender.hasPermission("notbounties.set"))
            page++;
        if (page == 4 && !(sender.hasPermission("notbounties.whitelist") && bountyWhitelistEnabled))
            page++;
        if (page == 5 && !(sender.hasPermission("notbounties.buyown") && buyBack) && !(sender.hasPermission("notbounties.buyimmunity") && Immunity.immunityType != Immunity.ImmunityType.DISABLE))
            page++;
        if (page == 6 && !sender.hasPermission("notbounties.removeimmunity") && !(sender.hasPermission("notbounties.removeset") && !sender.hasPermission(NotBounties.getAdminPermission())))
            page++;
        if (page == 7 && !(sender.hasPermission(NotBounties.getAdminPermission()) || (BountyTracker.isGiveOwnTracker() && sender.hasPermission("notbounties.tracker"))) && !(sender.hasPermission(NotBounties.getAdminPermission()) || giveOwnMap))
            page++;
        if (page == 8 && (!sender.hasPermission("notbounties.challenges") || !ChallengeManager.isEnabled()))
            page++;
        if (page == 9 && !sender.hasPermission(NotBounties.getAdminPermission()))
            page++;
        if (page >= 10)
            page = 1;
        return page;
    }

    public static void sendHelpMessage(CommandSender sender) {
        Player parser = sender instanceof Player player ? player : null;
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
        if (sender.hasPermission("notbounties.removeset") && !sender.hasPermission(NotBounties.getAdminPermission())) {
            sendHelpMessage(sender, helpRemoveSet);
        }
        if (sender.hasPermission(NotBounties.getAdminPermission()) || giveOwnMap) {
            sendHelpMessage(sender, helpPosterOwn);
            if (sender.hasPermission(NotBounties.getAdminPermission()))
                sendHelpMessage(sender, helpPosterOther);
        }
        if (BountyTracker.isEnabled())
            if (sender.hasPermission(NotBounties.getAdminPermission()) || (BountyTracker.isGiveOwnTracker() && sender.hasPermission("notbounties.tracker"))) {
                sendHelpMessage(sender, helpTrackerOwn);
                if (sender.hasPermission(NotBounties.getAdminPermission()))
                    sendHelpMessage(sender, helpTrackerOther);
            }
        if (sender.hasPermission("notbounties.challenges") && ChallengeManager.isEnabled()) {
            sendHelpMessage(sender, helpChallenges);
        }
        if (sender.hasPermission(NotBounties.getAdminPermission())) {
            sendHelpMessage(sender, helpAdmin);
        }

        if (sender.hasPermission("notbounties.immune")) {
            sendHelpMessage(sender, helpImmune);
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                 ");
    }

    public static void sendHelpMessage(CommandSender sender, int page) {
        if (!(sender instanceof Player parser)) {
            sendHelpMessage(sender);
            return;
        }
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
                if (sender.hasPermission("notbounties.removeset") && !sender.hasPermission(NotBounties.getAdminPermission()))
                    sendHelpMessage(sender, helpRemoveSet);
                break;
            case 7:
                // item
                if (sender.hasPermission(NotBounties.getAdminPermission()) || giveOwnMap) {
                    sendHelpMessage(sender, helpPosterOwn);
                    if (sender.hasPermission(NotBounties.getAdminPermission()))
                        sendHelpMessage(sender, helpPosterOther);
                }
                if (BountyTracker.isEnabled())
                    if (sender.hasPermission(NotBounties.getAdminPermission()) || (BountyTracker.isGiveOwnTracker() && sender.hasPermission("notbounties.tracker"))) {
                        sendHelpMessage(sender, helpTrackerOwn);
                        if (sender.hasPermission(NotBounties.getAdminPermission()))
                            sendHelpMessage(sender, helpTrackerOther);
                    }
                break;
            case 8:
                sendHelpMessage(sender, helpChallenges);
                break;
            case 9:
                // admin
                sendHelpMessage(sender, helpAdmin);
                break;
            default:
                sender.sendMessage("You're not supposed to be here...");
                sender.sendMessage("Join the discord! https://discord.gg/zEsUzwYEx7");
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
        back.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " help " + previousPage));
        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " help " + nextPage));
        BaseComponent[] baseComponents = new BaseComponent[]{space, space, back, middle, next, space, space};
        if (previousPage <= 0)
            baseComponents[2] = space;
        if (nextPage >= 9 || nextPage <= previousPage)
            baseComponents[4] = space;
        sender.spigot().sendMessage(baseComponents);
    }

    public static void sendHelpMessage(CommandSender sender, List<String> message) {
        Player parser = sender instanceof Player ? (Player) sender : null;
        for (String str : message) {
            str = str.replace("{whitelist}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(ConfigOptions.bountyWhitelistCost) + NumberFormatting.currencySuffix));
            str = str.replace("{tax}", (NumberFormatting.formatNumber(ConfigOptions.bountyTax * 100)));
            str = str.replace("{buy_back_interest}", (NumberFormatting.formatNumber(ConfigOptions.buyBackInterest)));
            str = str.replace("{permanent_cost}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(Immunity.getPermanentCost()) + NumberFormatting.currencySuffix));
            str = str.replace("{scaling_ratio}", (NumberFormatting.formatNumber(Immunity.getScalingRatio())));
            str = str.replace("{time_immunity}", (formatTime((long) (Immunity.getTime() * 1000L), LocalTime.TimeFormat.RELATIVE)));
            sender.sendMessage(parse(str, parser));
        }
    }

    /**
     * Will not add the player prefix or player suffix
     * Mainly for unknown player
     */
    public static String parse(String str, String player, OfflinePlayer receiver) {
        str = str.replace("{receiver}", (player));
        str = str.replace("{player}", (player));
        return parse(str, receiver);
    }

    public static String parse(String str, OfflinePlayer receiver) {
        if (str.contains("{time}")) {
            String timeString = formatTime(System.currentTimeMillis(), LocalTime.TimeFormat.PLAYER, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        str = str.replace("{next_challenges}", formatTime(ChallengeManager.getNextChallengeChange() - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE));
        str = str.replace("{min_bounty}", (NumberFormatting.getValue(ConfigOptions.minBounty)));
        str = str.replace("{c_prefix}", (NumberFormatting.currencyPrefix));
        str = str.replace("{c_suffix}", (NumberFormatting.currencySuffix));

        if (receiver != null) {
            Bounty bounty = BountyManager.getBounty(receiver.getUniqueId());
            if (bounty != null) {
                str = str.replace("{min_expire}", (formatTime(BountyExpire.getLowestExpireTime(bounty), LocalTime.TimeFormat.RELATIVE)));
                str = str.replace("{max_expire}", (formatTime(BountyExpire.getHighestExpireTime(bounty), LocalTime.TimeFormat.RELATIVE)));
                str = str.replace("{bounty}", NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalDisplayBounty()) + NumberFormatting.currencySuffix);
                str = str.replace("{bounty_value}", NumberFormatting.getValue(bounty.getTotalDisplayBounty()) );
            } else {
                str = str.replace("{min_expire}", "");
                str = str.replace("{max_expire}", "");
            }
            if (receiver.getName() != null) {
                str = str.replace("{player}", playerPrefix + receiver.getName() + playerSuffix);
                str = str.replace("{receiver}", playerPrefix + receiver.getName() + playerSuffix);
            } else {
                str = str.replace("{player}", playerPrefix + NotBounties.getPlayerName(receiver.getUniqueId()) + playerPrefix);
                str = str.replace("{receiver}", playerPrefix + NotBounties.getPlayerName(receiver.getUniqueId()) + playerSuffix);
            }
            if (str.contains("{balance}"))
                str = str.replace("{balance}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(NumberFormatting.getBalance(receiver)) + NumberFormatting.currencySuffix));
            Whitelist whitelist = NotBounties.getPlayerWhitelist(receiver.getUniqueId());
            str = str.replace("{whitelist}", (whitelist.toString()));
            String mode = whitelist.isBlacklist() ? "Blacklist" : "Whitelist";
            str = str.replace("{mode}", mode);
            mode = whitelist.isBlacklist() ? "false" : "true";
            str = str.replace("{mode_raw}", mode);
            String notification = NotBounties.disableBroadcast.contains(receiver.getUniqueId()) ? "false" : "true";
            str = str.replace("{notification}", notification);
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

    public static String parse(String str, long time, LocalTime.TimeFormat format, OfflinePlayer receiver) {
        if (str.contains("{time}")) {
            String timeString = formatTime(time, format, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        str = str.replace("{amount}", (time + ""));
        return parse(str, receiver);
    }

    public static String parse(String str, double amount, OfflinePlayer receiver) {
        str = str.replace("{amount}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        return parse(str, receiver);
    }

    public static String parse(String str, String player, double amount, OfflinePlayer receiver) {
        str = str.replace("{player}", (player));
        str = str.replace("{receiver}", (player));
        return parse(str,amount,receiver);
    }

    public static String parse(String str, double amount, long time, LocalTime.TimeFormat format, OfflinePlayer receiver) {
        if (str.contains("{time}")) {
            String timeString = formatTime(time, format, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        return parse(str, amount, receiver);
    }

    /**
     * This does not add the player prefix or suffix
     * Used for console name
     */
    public static String parse(String str, String player, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replace("{bounty}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        return parse(str, player, amount, receiver);
    }

    public static String parse(String str, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replace("{bounty}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty) + NumberFormatting.currencySuffix));
        return parse(str, amount, receiver);
    }

    public static String parse(String str, double amount, double bounty, long time, LocalTime.TimeFormat format, OfflinePlayer receiver) {
        if (str.contains("{time}")) {
            String timeString = formatTime(time, format, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        return parse(str, amount, bounty, receiver);
    }

    public static String parse(String str, OfflinePlayer player, double amount, OfflinePlayer receiver) {
        if (player != null) {
            String replacement;
            if (player.getName() != null) {
                replacement = player.getName();
            } else {
                replacement = NotBounties.getPlayerName(player.getUniqueId());
            }
            replacement = playerPrefix + replacement + playerSuffix;
            if (papiEnabled)
                replacement = new PlaceholderAPIClass().parse(player, replacement);
            str = str.replace("{player}", replacement);
        }
        return parse(str, amount, receiver);
    }

    public static String parse(String str, OfflinePlayer player, OfflinePlayer receiver) {
        if (player != null) {
            String replacement;
            if (player.getName() != null) {
                replacement = player.getName();
            } else {
                replacement = NotBounties.getPlayerName(player.getUniqueId());
            }
            replacement = playerPrefix + replacement + playerSuffix;
            if (papiEnabled)
                replacement = new PlaceholderAPIClass().parse(player, replacement);
            str = str.replace("{player}", replacement);
        }
        return parse(str, receiver);
    }

    public static String parse(String str, OfflinePlayer player, double amount, double totalBounty, OfflinePlayer receiver) {
        str = str.replace("{bounty}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.currencySuffix));
        return parse(str, player, amount, receiver);
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

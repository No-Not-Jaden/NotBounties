package me.jadenp.notbounties.ui.gui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class GUIClicks {
    enum ClickAction {
        POSTER, TRACKER, VIEW, REMOVE, EDIT, NONE
    }
    private static ClickAction playerRight;
    private static ClickAction playerLeft;
    private static ClickAction playerMiddle;
    private static ClickAction playerBedrock;
    private static ClickAction playerDrop;
    private static ClickAction adminRight;
    private static ClickAction adminLeft;
    private static ClickAction adminMiddle;
    private static ClickAction adminBedrock;
    private static ClickAction adminDrop;
    public static void loadConfiguration(ConfigurationSection configuration) {
        playerRight = getClickActionOrNone(configuration.getString("right"));
        playerLeft = getClickActionOrNone(configuration.getString("left"));
        playerMiddle = getClickActionOrNone(configuration.getString("middle"));
        playerBedrock = getClickActionOrNone(configuration.getString("bedrock"));
        playerDrop = getClickActionOrNone(configuration.getString("drop"));
        adminRight = getClickActionOrNone(configuration.getString("admin.right"));
        adminLeft = getClickActionOrNone(configuration.getString("admin.left"));
        adminMiddle = getClickActionOrNone(configuration.getString("admin.middle"));
        adminBedrock = getClickActionOrNone(configuration.getString("admin.bedrock"));
        adminDrop = getClickActionOrNone(configuration.getString("admin.drop"));
    }

    private static ClickAction getClickActionOrNone(@Nullable String action) {
        if (action == null)
            return ClickAction.NONE;
        try {
            return ClickAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ClickAction.NONE;
        }
    }

    public static List<String> getClickLore(Player player, boolean buyBack, double buyBackPrice) {
        List<String> lore = new ArrayList<>();
        boolean admin = player.hasPermission(NotBounties.getAdminPermission());
        if (admin && usingActions(ClickAction.REMOVE, false, true))
            getListMessage("remove-bounty-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if (admin && usingActions(ClickAction.EDIT, false, true))
            getListMessage("edit-bounty-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if (buyBack && !admin && usingActions(ClickAction.REMOVE, false, false)) // only add buy back if there isn't an admin remove action
            getListMessage("buy-back-lore").stream().map(bbLore -> parse(bbLore, buyBackPrice, player)).forEach(lore::add);
        if (ConfigOptions.giveOwnMap && usingActions(ClickAction.POSTER, buyBack, admin))
            getListMessage("give-poster-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if ((BountyTracker.isEnabled() && (BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers() || admin) && player.hasPermission("notbounties.tracker")) && usingActions(ClickAction.TRACKER, buyBack, admin))
            getListMessage("give-tracker-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if (usingActions(ClickAction.VIEW, buyBack, admin))
            getListMessage("view-bounty-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        return lore;

    }

    private static boolean usingActions(ClickAction clickAction, boolean buyBack, boolean admin) {
        if (admin)
            return adminRight == clickAction || adminLeft == clickAction || adminMiddle == clickAction || adminDrop == clickAction;
        return playerRight == clickAction || (playerLeft == clickAction && !buyBack) || playerMiddle == clickAction || playerDrop == clickAction;
    }

    /**
     * Run click actions for a player clicking on a bounty in the bounty-gui.
     * ClickType.UNKNOWN will run a bedrock click
     * @param player Player to execute the click actions
     * @param bounty Bounty that the player clicked,
     * @param clickType Type of click
     */
    public static void runClickActions(Player player, Bounty bounty, ClickType clickType) {
        NotBounties.debugMessage("Running " + clickType.name() + " click action for " + player.getName(), false);
        switch (clickType) {
            case RIGHT -> {
                if (player.hasPermission(NotBounties.getAdminPermission())) {
                    runAction(player, bounty, adminRight);
                } else {
                    runAction(player, bounty, playerRight);
                }
            }
            case LEFT -> {
                if (player.hasPermission(NotBounties.getAdminPermission())) {
                    runAction(player, bounty, adminLeft);
                } else {
                    if (player.getUniqueId().equals(bounty.getUUID())) {
                        runAction(player, bounty, ClickAction.REMOVE); // buy back
                    } else {
                        runAction(player, bounty, playerLeft);
                    }
                }
            }
            case MIDDLE -> {
                if (player.hasPermission(NotBounties.getAdminPermission())) {
                    runAction(player, bounty, adminMiddle);
                } else {
                    runAction(player, bounty, playerMiddle);
                }
            }
            case UNKNOWN -> {
                // bedrock
                if (player.hasPermission(NotBounties.getAdminPermission())) {
                    runAction(player, bounty, adminBedrock);
                } else {
                    runAction(player, bounty, playerBedrock);
                }
            }
            case DROP -> {
                if (player.hasPermission(NotBounties.getAdminPermission())) {
                    runAction(player, bounty, adminDrop);
                } else {
                    runAction(player, bounty, playerDrop);
                }
            }
            default -> {
                // click type not registered - ignore
            }
        }
    }

    private static void runAction(Player player, Bounty bounty, ClickAction clickAction) {
        switch (clickAction) {
            case EDIT -> {
                if (player.hasPermission(NotBounties.getAdminPermission())) {
                    player.getOpenInventory().close();
                    TextComponent first = new TextComponent(ChatColor.GOLD + "To edit " + bounty.getName() + "'s bounty");
                    TextComponent click = new TextComponent(ChatColor.YELLOW + "" + ChatColor.BOLD + " Click Here ");
                    TextComponent last = new TextComponent(ChatColor.GOLD + "and enter the amount to change " + ChatColor.GRAY + "(e.g. 10, -500, 10k)" + ChatColor.GOLD + ".");
                    click.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + pluginBountyCommands.get(0) + " edit " + bounty.getName() + " "));
                    first.addExtra(click);
                    first.addExtra(last);
                    player.spigot().sendMessage(first);
                }
            }
            case REMOVE -> {
                if (player.hasPermission(NotBounties.getAdminPermission())) {
                    GUI.openGUI(player, "confirm", 1, bounty.getUUID(), 0);
                } else if (player.getUniqueId().equals(bounty.getUUID()) && buyBack) {
                    // buy back
                    double balance = NumberFormatting.getBalance(player);
                    if (balance >= (int) (bounty.getTotalDisplayBounty() * buyBackInterest)) {
                        GUI.openGUI(player, "confirm", 1, bounty.getUUID(), (buyBackInterest * bounty.getTotalDisplayBounty()));
                    } else {
                        player.sendMessage(parse(getPrefix() + getMessage("broke"), (bounty.getTotalDisplayBounty() * buyBackInterest), player));
                    }
                }
            }
            case POSTER -> {
                if (giveOwnMap) {
                    player.getOpenInventory().close();
                    Bukkit.getServer().dispatchCommand(player, pluginBountyCommands.get(0) + " poster " + NotBounties.getPlayerName(bounty.getUUID()));
                }
            }
            case TRACKER -> {
                if (BountyTracker.isEnabled() && (BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers() || player.hasPermission(NotBounties.getAdminPermission())) && player.hasPermission("notbounties.tracker")) {
                    player.getOpenInventory().close();
                    Bukkit.getServer().dispatchCommand(player, pluginBountyCommands.get(0) + " tracker " + NotBounties.getPlayerName(bounty.getUUID()));
                }
            }
            case VIEW -> GUI.openGUI(player, "view-bounty", 1, bounty.getUUID());
            case NONE -> {
                // no action
            }
        }
    }
}

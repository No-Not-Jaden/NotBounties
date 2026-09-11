package me.jadenp.notbounties.ui.commands;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.jadenp.notbounties.features.LanguageOptions.*;

class TrackerCommandHandler {
    private final Commands.UnknownPlayerHandler unknownPlayerHandler;

    TrackerCommandHandler(Commands.UnknownPlayerHandler unknownPlayerHandler) {
        this.unknownPlayerHandler = unknownPlayerHandler;
    }

    void tabComplete(CommandSender sender, String[] args, List<String> tab, boolean adminPermission) {
        if (!BountyTracker.isEnabled())
            return;

        if (args.length == 1 && hasCommandPermission(sender, false, adminPermission))
            tab.add("tracker");

        if (!args[0].equalsIgnoreCase("tracker") || !BountyTracker.isEnabled()) {
            return;
        }


        if (args.length == 2) {
            if (!hasCommandPermission(sender, false, adminPermission)) {
                return;
            }

            Set<String> bountyList = LoggedPlayers.getActiveBountyNames();
            if (bountyList.size() <= ConfigOptions.getMaxTabCompletePlayers()) {
                tab.addAll(bountyList);
            }
            if (adminPermission)
                tab.add("empty");
            if (BountyTracker.isTrackingExemptEnabled() && sender.hasPermission("notbounties.tracker.exempt"))
                tab.add("exempt");
            return;
        }

        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("exempt") && BountyTracker.isTrackingExemptEnabled() && (sender.hasPermission("notbounties.tracker.exempt") || adminPermission)) {
                tab.add("enable");
                tab.add("disable");
            } else {
                addNetworkPlayerSuggestions(tab);
            }
        }
    }

    CompletableFuture<Boolean> handle(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
        if (!BountyTracker.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> trackingExemptResult = handleTrackingExempt(sender, args, forcePermission, silent, parser);
        if (trackingExemptResult != null) {
            return trackingExemptResult;
        }

        if (!hasCommandPermission(sender, forcePermission, adminPermission)) {
            if (!silent)
                Messages.send(sender, getMessage("no-permission"), MessageContext.builder().receiver(parser).build());
            return CompletableFuture.completedFuture(false);
        }

        if (!Commands.applyGiveOwnCooldown(sender, forcePermission, adminPermission, silent)) {
            return CompletableFuture.completedFuture(false);
        }

        if (args.length <= 1) {
            if (!silent) {
                Messages.send(sender, getMessage("unknown-command"), MessageContext.builder().receiver(parser).build());
                sendHelpMessage(sender, getListMessage("help.tracker-own"));
                if (adminPermission)
                    sendHelpMessage(sender, getListMessage("help.tracker-other"));
            }
            CompletableFuture.completedFuture(false);
        }

        return handleTrackerTarget(sender, args, forcePermission, adminPermission, silent, parser);
    }

    private @Nullable CompletableFuture<Boolean> handleTrackingExempt(CommandSender sender, String[] args, boolean forcePermission, boolean silent, Player parser) {
        if (!(BountyTracker.isTrackingExemptEnabled()
                && sender instanceof Player player
                && args.length > 1
                && args[1].equalsIgnoreCase("exempt")
                && (forcePermission || sender.hasPermission("notbounties.tracker.exempt")))) {
            return null;
        }

        return DataManager.getPlayerDataAsync(player.getUniqueId()).thenApply(playerData -> {
            long sinceLastSet = System.currentTimeMillis() - playerData.getBountyCooldown();
            boolean enable = shouldEnableTrackingExempt(args, playerData);
            if (sinceLastSet < BountyTracker.getTrackingExemptDelayAfterSet() * 1000 && enable) {
                if (!silent)
                    Messages.send(sender, getMessage("tracker-exempt-cooldown"), MessageContext.builder().time(BountyTracker.getTrackingExemptDelayAfterSet() * 1000L - sinceLastSet, LocalTime.TimeFormat.RELATIVE).receiver(parser).build());
                return false;
            }

            playerData.setTrackingExempt(enable);
            DataManager.updatePlayerData(playerData);
            if (!silent) {
                if (enable) {
                    Messages.send(sender, getMessage("tracker-exempt-enable"), MessageContext.builder().receiver(parser).build());
                } else {
                    Messages.send(sender, getMessage("tracker-exempt-disable"), MessageContext.builder().receiver(parser).build());
                }
            }
            return true;
        });

    }

    private boolean shouldEnableTrackingExempt(String[] args, PlayerData playerData) {
        if (args.length > 2) {
            return args[2].equalsIgnoreCase("true")
                    || args[2].equalsIgnoreCase("on")
                    || args[2].equalsIgnoreCase("1")
                    || args[2].equalsIgnoreCase("enable");
        }
        return !playerData.isTrackingExempt();
    }

    private boolean hasCommandPermission(CommandSender sender, boolean forcePermission, boolean adminPermission) {
        return adminPermission || forcePermission || sender.hasPermission("notbounties.tracker");
    }

    private void addNetworkPlayerSuggestions(List<String> tab) {
        for (Map.Entry<UUID, String> entry : NotBounties.getNetworkPlayers().entrySet()) {
            if (entry.getValue().length() < 25)
                tab.add(entry.getValue());
        }
    }

    private CompletableFuture<Boolean> handleTrackerTarget(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {

        if (args[1].equalsIgnoreCase("empty")) {
            if (args.length > 2)
                return CompletableFuture.completedFuture(giveTrackerToOther(sender, args, null, adminPermission, silent));
            else
                return CompletableFuture.completedFuture(giveTrackerToSelf(sender, null, forcePermission, adminPermission, silent, parser));
        }

        UUID playerUUID = LoggedPlayers.getPlayer(args[1]);
        if (playerUUID == null) {
            unknownPlayerHandler.failUnknownPlayer(sender, args[1], silent);
            return CompletableFuture.completedFuture(false);
        }

        return DataManager.getBountyAsync(playerUUID).thenApply(bounty -> {
            if (bounty == null) {
                if (!silent)
                    Messages.send(sender, getMessage("no-bounty"), MessageContext.builder().player(playerUUID).receiver(parser).build());
                return false;
            }

            if (bounty.getTotalDisplayBounty() < BountyTracker.getMinBounty()) {
                if (!silent)
                    Messages.send(sender, getMessage("min-bounty"), MessageContext.builder().amount(BountyTracker.getMinBounty()).receiver(parser).build());
                return false;
            }

            if (args.length > 2) {
                return giveTrackerToOther(sender, args, playerUUID, adminPermission, silent);
            }
            return giveTrackerToSelf(sender, playerUUID, forcePermission, adminPermission, silent, parser);
        });

    }

    private boolean giveTrackerToOther(CommandSender sender, String[] args, @Nullable UUID playerUUID, boolean adminPermission, boolean silent) {
        // null playerUUID will give an empty tracker
        if (!adminPermission) {
            if (!silent)
                Messages.send(sender, getMessage("no-permission"), MessageContext.builder().receiver(Commands.getParser(sender)).build());
            return false;
        }

        Player receiver = Bukkit.getPlayer(args[2]);
        if (receiver == null) {
            unknownPlayerHandler.failUnknownPlayer(sender, args[2], silent);
            return false;
        }

        ItemStack tracker = playerUUID == null ? BountyTracker.getEmptyTracker() : BountyTracker.getTracker(playerUUID);
        NumberFormatting.givePlayer(receiver, tracker, 1);
        if (!silent) {
            if (playerUUID == null) {
                Messages.send(sender, getMessage("tracker-give").replace("{player}", ChatColor.RED + "X"), MessageContext.builder().receiver(receiver).build());
                Messages.send(receiver, getMessage("tracker-receive-empty"), MessageContext.builder().receiver(receiver).build());
            } else {
                Messages.send(sender, getMessage("tracker-give"), MessageContext.builder().player(playerUUID).receiver(receiver).build());
                Messages.send(receiver, getMessage("tracker-receive"), MessageContext.builder().player(Bukkit.getOfflinePlayer(playerUUID)).receiver(receiver).build());
            }
        }
        return true;
    }

    private boolean giveTrackerToSelf(CommandSender sender, @Nullable UUID playerUUID, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
        // null playerUUID will give an empty tracker
        if (!(sender instanceof Player)) {
            if (!silent)
                sender.sendMessage("You are not a player!");
            return false;
        }

        if (!(forcePermission || sender.hasPermission("notbounties.tracker") || adminPermission)) {
            if (!silent)
                Messages.send(sender, getMessage("no-permission"), MessageContext.builder().receiver(parser).build());
            return false;
        }

        ItemStack tracker = playerUUID == null ? BountyTracker.getEmptyTracker() : BountyTracker.getTracker(playerUUID);
        final UUID trackedPlayers = BountyTracker.getTrackedPlayer(parser.getInventory().getItemInMainHand());
        if ((BountyTracker.isWriteEmptyTrackers() && sender.hasPermission("notbounties.tracker.writeempty"))
                && DataManager.GLOBAL_SERVER_ID.equals(trackedPlayers)) {
            BountyTracker.removeEmptyTracker(parser, true);
            NumberFormatting.givePlayer(parser, tracker, 1);
        } else if (BountyTracker.isGiveOwnTracker() || sender.hasPermission("notbounties.spawntracker") || adminPermission || forcePermission) {
            NumberFormatting.givePlayer(parser, tracker, 1);
        } else {
            if (BountyTracker.isWriteEmptyTrackers() && sender.hasPermission("notbounties.tracker.writeempty")) {
                if (!silent)
                    Messages.send(sender, getMessage("no-empty-tracker"), MessageContext.builder().receiver(parser).build());
            } else {
                if (!silent)
                    Messages.send(sender, getMessage("no-permission"), MessageContext.builder().receiver(parser).build());
            }
            return false;
        }

        if (playerUUID == null) {
            if (!silent)
                Messages.send(sender, getMessage("tracker-receive-empty"), MessageContext.builder().receiver(parser).build());
        } else {
            if (!silent)
                Messages.send(sender, getMessage("tracker-receive"), MessageContext.builder().player(playerUUID).receiver(parser).build());
        }
        return true;
    }
}
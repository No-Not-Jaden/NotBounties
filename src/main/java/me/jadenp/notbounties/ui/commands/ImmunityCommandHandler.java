package me.jadenp.notbounties.ui.commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.massivecraft.factions.Conf;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.challenges.ChallengeType;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.money.NotEnoughCurrencyException;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static me.jadenp.notbounties.features.LanguageOptions.*;
import static me.jadenp.notbounties.features.settings.money.NumberFormatting.checkBalance;
import static me.jadenp.notbounties.features.settings.money.NumberFormatting.tryParse;
import static me.jadenp.notbounties.ui.gui.GUI.openGUI;

class ImmunityCommandHandler {
    private final Commands.UnknownPlayerHandler unknownPlayerHandler;
    private final CommandInvoker commandInvoker;
    private final Cache<UUID, Double> repeatBuyImmunityCommand = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
    ImmunityCommandHandler(Commands.UnknownPlayerHandler unknownPlayerHandler, CommandInvoker commandInvoker) {
        this.unknownPlayerHandler = unknownPlayerHandler;
        this.commandInvoker = commandInvoker;
    }

    void tabComplete(CommandSender sender, String[] args, List<String> tab, boolean adminPermission) {
        if (args.length == 1) {
            if ((ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.DISABLE || adminPermission) && sender.hasPermission("notbounties.buyimmunity")) {
                tab.add("immunity");
            }
            return;
        }

        if (!args[0].equalsIgnoreCase("immunity")) {
            return;
        }

        if (args.length == 2) {
            if (adminPermission || sender.hasPermission("notbounties.removeimmunity")) {
                tab.add("remove");
            }
            if ((adminPermission || sender.hasPermission("notbounties.buyimmunity")) && ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.PERMANENT) {
                tab.add("--confirm");
            }
            if (adminPermission) {
                tab.add("give");
            }
        } else if (args.length == 3) {
            if ((args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("give"))) {
                if (adminPermission) {
                    for (Map.Entry<UUID, String> entry : NotBounties.getNetworkPlayers().entrySet()) {
                        if (entry.getValue().length() < ConfigOptions.getMaxTabCompletePlayers())
                            tab.add(entry.getValue());
                    }
                }
            } else if ((adminPermission || sender.hasPermission("notbounties.buyimmunity"))
                    && (ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.SCALING
                    || ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.TIME)) {
                tab.add("--confirm");
            }

        }
    }

    void addEmptyTabFallback(String[] args, List<String> tab, boolean adminPermission) {
        if (args.length == 3 && args[0].equalsIgnoreCase("immunity") && args[1].equalsIgnoreCase("remove") && adminPermission && LoggedPlayers.getLoggedPlayers().size() < ConfigOptions.getMaxTabCompletePlayers()) {
            tab.addAll(LoggedPlayers.getLoggedPlayers().values());
        }
    }

    CompletableFuture<Boolean> handle(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {

        sendPermissionImmunityMessages(sender, parser);

        if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
            return handleRemoveCommand(sender, args, forcePermission, adminPermission, silent, parser);
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("give") && adminPermission) {
            return handleGiveCommand(sender, args, forcePermission, true, silent, parser);
        }

        return handleBuyImmunity(sender, args, forcePermission, silent, parser);
    }

    private void sendPermissionImmunityMessages(CommandSender sender, Player parser) {
        if (!ImmunityManager.isPermissionImmunity()) {
            return;
        }

        if (sender.hasPermission("notbounties.immune")) {
            Messages.send(sender, getMessage("permission-immunity-bounty"), MessageContext.builder().receiver(parser).build());
            return;
        }

        sendPermissionMessage(sender, parser, "notbounties.immunity.murder", "permission-immunity-murder");

        sendPermissionMessage(sender, parser, "notbounties.immunity.timed", "permission-immunity-timed");

        sendPermissionMessage(sender, parser, "notbounties.immunity.random", "permission-immunity-random");
    }

    private void sendPermissionMessage(CommandSender sender, Player parser, String permission, String messageKey) {
        if (sender.hasPermission(permission)) {
            Messages.send(sender, getMessage(messageKey), MessageContext.builder().receiver(parser).build());
        }
    }

    private CompletableFuture<Boolean> handleGiveCommand(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
        // /bounty immunity give (player) (amount)
        if (args.length != 4) {
            if (!silent) {
                Messages.send(sender, getMessage("unknown-command"), MessageContext.builder().receiver(parser).build());
                if (adminPermission) {
                    sendHelpMessage(sender, getListMessage("help.admin"));
                }
            }
            return CompletableFuture.completedFuture(false);
        }

        // run /bounty stat immunity (player) edit (amount)
        return commandInvoker.execute(parser, new String[]{"stat", "immunity", args[2], "edit", args[3]}, forcePermission, adminPermission);

    }

    private CompletableFuture<Boolean> handleRemoveCommand(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
        // /bounty immunity remove ...
        if (args.length == 2 ||
                (args.length == 3 && args[2].equalsIgnoreCase("--confirm"))) {
            // /bounty immunity remove <--confirm>
            return handleSelfRemove(sender, args, forcePermission, adminPermission, silent, parser);
        }

        if (args.length == 3) {
            // /bounty immunity remove (player)
            return handleAdminRemove(sender, args, forcePermission, adminPermission, silent, parser);
        }

        sendRemoveUsage(sender, adminPermission, parser);
        return CompletableFuture.completedFuture(false);
    }

    private CompletableFuture<Boolean> handleSelfRemove(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
        // /bounty immunity remove <--confirm>

        if (!(forcePermission || sender.hasPermission("notbounties.removeimmunity") || adminPermission)) {

            return CompletableFuture.completedFuture(false);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("You don't have immunity!");
            return CompletableFuture.completedFuture(false);
        }

        boolean confirmed = args.length == 3 && args[2].equalsIgnoreCase("--confirm");

        if (!confirmed) {
            openGUI(player, "confirm-remove-immunity", 1);
            return CompletableFuture.completedFuture(true);
        }

        return removeOwnImmunity(sender, player, adminPermission, silent, parser);
    }

    private CompletableFuture<Boolean> removeOwnImmunity(CommandSender sender, Player player, boolean adminPermission, boolean silent, Player parser) {
        CompletableFuture<Boolean> future = ImmunityManager.removeImmunity(player.getUniqueId());
        future.thenAccept(immunity -> {
            if (Boolean.TRUE.equals(immunity)) {
                if (!silent) {
                    Messages.send(sender, getMessage("removed-immunity"), MessageContext.builder().receiver(parser).build());
                }
                return;
            }
            // player doesn't have immunity

            if (silent) {
                return;
            }
            // print out any permission immunities that they may have

            DataManager.getPlayerDataAsync(player.getUniqueId()).thenAccept(playerData -> {
                String permissions = buildImmunityPermissionList(playerData);

                if (permissions.isEmpty()) {
                    Messages.send(sender, getMessage("no-immunity"), MessageContext.builder().receiver(parser).build());
                } else {
                    Messages.send(sender, getMessage("permission-immunity").replace("{permission}", permissions), MessageContext.builder().receiver(parser).build());

                    if (adminPermission) {
                        Messages.send(sender,
                                ChatColor.RED + "If you think this is is a mistake, see this FAQ entry: " +
                                        ChatColor.GRAY + "https://github.com/No-Not-Jaden/NotBounties/wiki/FAQ#how-do-i-make-operatorsadmins-not-immune-to-bounties",
                                MessageContext.builder().receiver(parser).build()); // should be clickable in chat just by sending a link
                    }
                }
            });

        });


        return future;
    }

    private String buildImmunityPermissionList(PlayerData playerData) {
        if (!ImmunityManager.isPermissionImmunity()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        if (playerData.hasGeneralImmunity()) {
            builder.append("notbounties.immune ");
        }

        if (playerData.hasMurderImmunity()) {
            builder.append("notbounties.immunity.murder ");
        }

        if (playerData.hasTimedImmunity()) {
            builder.append("notbounties.immunity.timed ");
        }

        if (playerData.hasRandomImmunity()) {
            builder.append("notbounties.immunity.random ");
        }

        return builder.toString().trim();
    }

    private CompletableFuture<Boolean> handleAdminRemove(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {

        if (!(forcePermission || adminPermission)) {
            sendRemoveUsage(sender, false, parser);
            return CompletableFuture.completedFuture(false);
        }

        UUID targetUUID = LoggedPlayers.getPlayer(args[2]);

        if (targetUUID == null) {
            unknownPlayerHandler.failUnknownPlayer(sender, args[2], silent);
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> removed = ImmunityManager.removeImmunity(targetUUID);

        if (!silent) {
            removed.thenAccept(immunity -> Messages.send(sender, getMessage(Boolean.TRUE.equals(immunity) ? "removed-other-immunity" : "no-immunity-other"), MessageContext.builder().player(targetUUID).receiver(parser).build()));

        }

        return removed;
    }

    private void sendRemoveUsage(CommandSender sender, boolean adminPermission, Player parser) {
        Messages.send(sender, getMessage("unknown-command"), MessageContext.builder().receiver(parser).build());
        if (adminPermission) {
            sendHelpMessage(sender, getListMessage("help.admin"));
        }
        if (sender.hasPermission("notbounties.removeimmunity")) {
            sendHelpMessage(sender, getListMessage("help.remove-immunity"));
        }
    }

    private CompletableFuture<Boolean> handleBuyImmunity(CommandSender sender, String[] args, boolean forcePermission, boolean silent, Player parser) {
        // /bounty immunity <price>
        if (!(sender instanceof Player player)) {
            if (!silent) {
                sender.sendMessage("Only players can buy immunity!");
            }
            return CompletableFuture.completedFuture(false);
        }

        if (ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.DISABLE) {
            if (!silent) {
                Messages.send(sender, getMessage("unknown-command"), MessageContext.builder().receiver(parser).build());
            }
            return CompletableFuture.completedFuture(false);
        }

        if (!(forcePermission || sender.hasPermission("notbounties.buyimmunity"))) {
            if (!silent) {
                Messages.send(sender, getMessage("no-permission"), MessageContext.builder().receiver(parser).build());
            }
            return CompletableFuture.completedFuture(false);
        }

        if (ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.PERMANENT) {
            return handlePermanentImmunityPurchase(player, args, silent, parser);
        }

        return handleScalingOrTimedImmunityPurchase(player, args, silent, parser);
    }

    private CompletableFuture<Boolean> handlePermanentImmunityPurchase(Player player, String[] args, boolean silent, Player parser) {
        // /bounty immunity
        return ImmunityManager.getImmunity(Objects.requireNonNull(parser).getUniqueId()).thenApply(immunitySpent -> {
            boolean hasPermissionImmunity = player.hasPermission("notbounties.immune") && ImmunityManager.isPermissionImmunity();

            if (immunitySpent >= ImmunityManager.getPermanentCost() || hasPermissionImmunity) {

                if (!silent) {
                    Messages.send(player, getMessage("already-bought-perm"), MessageContext.builder().receiver(parser).build());
                }

                return false;
            }

            boolean confirmed =
                    Objects.equals(ImmunityManager.getPermanentCost(), repeatBuyImmunityCommand.getIfPresent(player.getUniqueId()))
                            || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"));

            if (!confirmed) {
                repeatBuyImmunityCommand.put(player.getUniqueId(), ImmunityManager.getPermanentCost());

                if (!silent) {
                    Messages.send(player, getMessage("repeat-command-immunity"), MessageContext.builder().amount(ImmunityManager.getPermanentCost()).receiver(parser).build());
                }

                return true;
            }

            repeatBuyImmunityCommand.invalidate(player.getUniqueId());

            return purchasePermanentImmunity(player, ImmunityManager.getPermanentCost(), silent, parser);
        });
    }

    private boolean purchasePermanentImmunity(Player player, double cost, boolean silent, Player parser) {

        if (!checkBalance(parser, cost)) {
            return sendBrokeMessage(player, cost, silent, parser);
        }

        try {
            NumberFormatting.doRemoveCommands(parser, cost, new ArrayList<>());

            ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.PURCHASE_IMMUNITY, cost);

            DataManager.changeStat(parser.getUniqueId(), Leaderboard.IMMUNITY, cost);

            if (!silent) {
                Messages.send(player, getMessage("buy-permanent-immunity"), MessageContext.builder().amount(Leaderboard.IMMUNITY.getStat(parser.getUniqueId()).join()).receiver(parser).build());
            }

            return true;

        } catch (NotEnoughCurrencyException e) {
            return sendBrokeMessage(player, cost, silent, parser);
        }
    }

    private CompletableFuture<Boolean> handleScalingOrTimedImmunityPurchase(Player player, String[] args, boolean silent, Player parser) {
        // /bounty immunity (price)
        if (args.length <= 1) {
            // /bounty immunity
            // send them their current immunity
            return commandInvoker.execute(player, new String[]{"stat", "immunity"}, false, false);
        }

        double amount;

        try {
            amount = tryParse(args[1]);
        } catch (NumberFormatException ignored) {
            if (!silent) {
                Messages.send(player, getMessage("unknown-number"), MessageContext.builder().receiver(parser).build());
            }

            return CompletableFuture.completedFuture(false);
        }

        boolean confirmed = Objects.equals(repeatBuyImmunityCommand.getIfPresent(player.getUniqueId()), amount) || (args.length > 2 && args[2].equalsIgnoreCase("--confirm"));

        if (!confirmed) {
            repeatBuyImmunityCommand.put(player.getUniqueId(), amount);

            if (!silent) {
                Messages.send(player, getMessage("repeat-command-immunity"), MessageContext.builder().amount(amount).receiver(parser).build());
            }

            return CompletableFuture.completedFuture(true);
        }

        repeatBuyImmunityCommand.invalidate(player.getUniqueId());

        return CompletableFuture.completedFuture(purchaseScalingOrTimedImmunity(player, amount, silent, parser));
    }

    private boolean purchaseScalingOrTimedImmunity(Player player, double amount, boolean silent, Player parser) {

        if (!checkBalance(parser, amount)) {
            return sendBrokeMessage(player, amount, silent, parser);
        }

        try {
            NumberFormatting.doRemoveCommands(parser, amount, new ArrayList<>());

            ImmunityManager.addImmunity(parser.getUniqueId(), amount);

            ChallengeManager.updateChallengeProgress(parser.getUniqueId(), ChallengeType.PURCHASE_IMMUNITY, amount);

            sendImmunityPurchaseMessage(player, silent, parser);

            return true;

        } catch (NotEnoughCurrencyException e) {
            return sendBrokeMessage(player, amount, silent, parser);
        }
    }

    private void sendImmunityPurchaseMessage(Player player, boolean silent, Player parser) {

        if (silent) {
            return;
        }

        ImmunityManager.getImmunity(parser.getUniqueId()).thenAccept(immunity -> {
            if (ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.SCALING) {

                Messages.send(player, getMessage("buy-scaling-immunity"), MessageContext.builder().amount(immunity * ImmunityManager.getScalingRatio()).receiver(parser).build());

                return;
            }

            Messages.send(player, getMessage("buy-time-immunity"),
                    MessageContext.builder()
                            .time(ImmunityManager.getTimeImmunity(parser.getUniqueId()), LocalTime.TimeFormat.RELATIVE)
                            .amount(immunity * ImmunityManager.getTime())
                            .receiver(parser)
                            .build()
            );
        });

    }

    private boolean sendBrokeMessage(Player player, double amount, boolean silent, Player parser) {

        if (!silent) {
            Messages.send(player, getMessage("broke"), MessageContext.builder().amount(amount).receiver(parser).build());
        }

        return false;
    }

    @FunctionalInterface
    interface CommandInvoker {
        CompletableFuture<Boolean> execute(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission);
    }

}

package me.jadenp.notbounties.ui.commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.massivecraft.factions.Conf;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ConfigOptions;
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

import java.util.*;
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

    boolean handle(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {

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
            sender.sendMessage(parse(getPrefix() + getMessage("permission-immunity-bounty"), parser));
            return;
        }

        sendPermissionMessage(sender, parser, "notbounties.immunity.murder", "permission-immunity-murder");

        sendPermissionMessage(sender, parser, "notbounties.immunity.timed", "permission-immunity-timed");

        sendPermissionMessage(sender, parser, "notbounties.immunity.random", "permission-immunity-random");
    }

    private void sendPermissionMessage(CommandSender sender, Player parser, String permission, String messageKey) {
        if (sender.hasPermission(permission)) {
            sender.sendMessage(parse(getPrefix() + getMessage(messageKey), parser));
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
        // /bounty immunity give (player) (amount)
        if (args.length != 4) {
            if (!silent) {
                sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                if (adminPermission) {
                    sendHelpMessage(sender, getListMessage("help.admin"));
                }
            }
            return false;
        }

        // run /bounty stat immunity (player) edit (amount)
        return commandInvoker.execute(parser, new String[]{"stat", "immunity", args[2], "edit", args[3]}, forcePermission, adminPermission);

    }

    private boolean handleRemoveCommand(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
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
        return false;
    }

    private boolean handleSelfRemove(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {
        // /bounty immunity remove <--confirm>

        if (!(forcePermission || sender.hasPermission("notbounties.removeimmunity") || adminPermission)) {

            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("You don't have immunity!");
            return false;
        }

        boolean confirmed = args.length == 3 && args[2].equalsIgnoreCase("--confirm");

        if (!confirmed) {
            openGUI(player, "confirm-remove-immunity", 1);
            return true;
        }

        return removeOwnImmunity(sender, player, adminPermission, silent, parser);
    }

    private boolean removeOwnImmunity(CommandSender sender, Player player, boolean adminPermission, boolean silent, Player parser) {

        if (ImmunityManager.removeImmunity(player.getUniqueId())) {
            if (!silent) {
                sender.sendMessage(parse(getPrefix() + getMessage("removed-immunity"), parser));
            }
            return true;
        }
        // player doesn't have immunity

        if (silent) {
            return false;
        }
        // print out any permission immunities that they may have

        PlayerData playerData = DataManager.getPlayerData(player.getUniqueId());

        String permissions = buildImmunityPermissionList(playerData);

        if (permissions.isEmpty()) {
            sender.sendMessage(parse(getPrefix() + getMessage("no-immunity"), parser));
        } else {
            sender.sendMessage(parse(getPrefix() + getMessage("permission-immunity").replace("{permission}", permissions), parser));

            if (adminPermission) {
                sender.sendMessage(parse(getPrefix() +
                                ChatColor.RED + "If you think this is is a mistake, see this FAQ entry: " +
                                ChatColor.GRAY + "https://github.com/No-Not-Jaden/NotBounties/wiki/FAQ#how-do-i-make-operatorsadmins-not-immune-to-bounties",
                        parser)); // should be clickable in chat just by sending a link
            }
        }

        return false;
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

    private boolean handleAdminRemove(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent, Player parser) {

        if (!(forcePermission || adminPermission)) {
            sendRemoveUsage(sender, false, parser);
            return false;
        }

        UUID targetUUID = LoggedPlayers.getPlayer(args[2]);

        if (targetUUID == null) {
            unknownPlayerHandler.failUnknownPlayer(sender, args[2], silent);
            return false;
        }

        boolean removed = ImmunityManager.removeImmunity(targetUUID);

        if (!silent) {
            sender.sendMessage(parse(getPrefix() + getMessage(removed ? "removed-other-immunity" : "no-immunity-other"), targetUUID, parser));
        }

        return removed;
    }

    private void sendRemoveUsage(CommandSender sender, boolean adminPermission, Player parser) {
        sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
        if (adminPermission) {
            sendHelpMessage(sender, getListMessage("help.admin"));
        }
        if (sender.hasPermission("notbounties.removeimmunity")) {
            sendHelpMessage(sender, getListMessage("help.remove-immunity"));
        }
    }

    private boolean handleBuyImmunity(CommandSender sender, String[] args, boolean forcePermission, boolean silent, Player parser) {
        // /bounty immunity <price>
        if (!(sender instanceof Player player)) {
            if (!silent) {
                sender.sendMessage("Only players can buy immunity!");
            }
            return false;
        }

        if (ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.DISABLE) {
            if (!silent) {
                sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
            }
            return false;
        }

        if (!(forcePermission || sender.hasPermission("notbounties.buyimmunity"))) {
            if (!silent) {
                sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
            }
            return false;
        }

        if (ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.PERMANENT) {
            return handlePermanentImmunityPurchase(player, args, silent, parser);
        }

        return handleScalingOrTimedImmunityPurchase(player, args, silent, parser);
    }

    private boolean handlePermanentImmunityPurchase(Player player, String[] args, boolean silent, Player parser) {
        // /bounty immunity
        double immunitySpent = ImmunityManager.getImmunity(Objects.requireNonNull(parser).getUniqueId());

        boolean hasPermissionImmunity = player.hasPermission("notbounties.immune") && ImmunityManager.isPermissionImmunity();

        if (immunitySpent >= ImmunityManager.getPermanentCost() || hasPermissionImmunity) {

            if (!silent) {
                player.sendMessage(parse(getPrefix() + getMessage("already-bought-perm"), parser));
            }

            return false;
        }

        boolean confirmed =
                Objects.equals(ImmunityManager.getPermanentCost(), repeatBuyImmunityCommand.getIfPresent(player.getUniqueId()))
                        || (args.length > 1 && args[1].equalsIgnoreCase("--confirm"));

        if (!confirmed) {
            repeatBuyImmunityCommand.put(player.getUniqueId(), ImmunityManager.getPermanentCost());

            if (!silent) {
                player.sendMessage(parse(getPrefix() + getMessage("repeat-command-immunity"), ImmunityManager.getPermanentCost(), parser));
            }

            return true;
        }

        repeatBuyImmunityCommand.invalidate(player.getUniqueId());

        return purchasePermanentImmunity(player, ImmunityManager.getPermanentCost(), silent, parser);
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
                player.sendMessage(parse(getPrefix() + getMessage("buy-permanent-immunity"), Leaderboard.IMMUNITY.getStat(parser.getUniqueId()), parser));
            }

            return true;

        } catch (NotEnoughCurrencyException e) {
            return sendBrokeMessage(player, cost, silent, parser);
        }
    }

    private boolean handleScalingOrTimedImmunityPurchase(Player player, String[] args, boolean silent, Player parser) {
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
                player.sendMessage(parse(getPrefix() + getMessage("unknown-number"), parser));
            }

            return false;
        }

        boolean confirmed = Objects.equals(repeatBuyImmunityCommand.getIfPresent(player.getUniqueId()), amount) || (args.length > 2 && args[2].equalsIgnoreCase("--confirm"));

        if (!confirmed) {
            repeatBuyImmunityCommand.put(player.getUniqueId(), amount);

            if (!silent) {
                player.sendMessage(parse(getPrefix() + getMessage("repeat-command-immunity"), amount, parser));
            }

            return true;
        }

        repeatBuyImmunityCommand.invalidate(player.getUniqueId());

        return purchaseScalingOrTimedImmunity(player, amount, silent, parser);
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

        if (ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.SCALING) {

            player.sendMessage(parse(getPrefix() + getMessage("buy-scaling-immunity"), ImmunityManager.getImmunity(parser.getUniqueId()) * ImmunityManager.getScalingRatio(), parser));

            return;
        }

        player.sendMessage(parse(getPrefix() + getMessage("buy-time-immunity")
                                .replace("{time}", LocalTime.formatTime(ImmunityManager.getTimeImmunity(parser.getUniqueId()), LocalTime.TimeFormat.RELATIVE)
                                ), ImmunityManager.getImmunity(parser.getUniqueId()) * ImmunityManager.getTime(), parser
        ));
    }

    private boolean sendBrokeMessage(Player player, double amount, boolean silent, Player parser) {

        if (!silent) {
            player.sendMessage(parse(getPrefix() + getMessage("broke"), amount, parser));
        }

        return false;
    }

    @FunctionalInterface
    interface CommandInvoker {
        boolean execute(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission);
    }

}

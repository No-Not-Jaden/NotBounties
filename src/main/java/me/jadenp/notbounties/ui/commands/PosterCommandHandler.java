package me.jadenp.notbounties.ui.commands;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.display.map.BountyMap;
import me.jadenp.notbounties.features.settings.display.map.HologramRenderer;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

import static me.jadenp.notbounties.features.LanguageOptions.*;
import static me.jadenp.notbounties.utils.BountyManager.getAllBounties;
import static me.jadenp.notbounties.utils.BountyManager.getBounty;
import static me.jadenp.notbounties.utils.BountyManager.getPublicBounties;

class PosterCommandHandler {
    private final Commands.UnknownPlayerHandler unknownPlayerHandler;

    PosterCommandHandler(Commands.UnknownPlayerHandler unknownPlayerHandler) {
        this.unknownPlayerHandler = unknownPlayerHandler;
    }

    void tabComplete(CommandSender sender, String[] args, List<String> tab, boolean adminPermission) {
        if (!BountyMap.isEnabled() || !hasCommandPermission(sender, false, adminPermission))
            return;

        if (args.length == 1)
            tab.add("poster");
        if (args.length < 1 || !args[0].equalsIgnoreCase("poster")) {
            return;
        }

        if (args.length == 2 || (args.length == 3 && (adminPermission || BountyMap.isGiveOwn() || sender.hasPermission("notbounties.spawnposter")))) {
            // tab complete players for args 2 and 3 (admins only)
            List<Bounty> bountyList = adminPermission ? getAllBounties(-1) : getPublicBounties(-1);
            if (bountyList.size() <= ConfigOptions.getMaxTabCompletePlayers()) {
                for (Bounty bounty : bountyList) {
                    tab.add(bounty.getName());
                }
            }
        }
    }

    boolean handle(CommandSender sender, String[] args, boolean forcePermission, boolean adminPermission, boolean silent , Player parser) {
        if (!hasCommandPermission(sender, forcePermission, adminPermission)) {
            if (!silent)
                sender.sendMessage(parse(getPrefix() + getMessage("no-permission"), parser));
            return false;
        }

        if (!Commands.applyGiveOwnCooldown(sender, forcePermission, adminPermission, silent)) {
            return false;
        }

        if (args.length == 1) {
            if (!silent) {
                sender.sendMessage(parse(getPrefix() + getMessage("unknown-command"), parser));
                sendHelpMessage(sender, getListMessage("help.poster-own"));
                if (adminPermission)
                    sendHelpMessage(sender, getListMessage("help.poster-other"));
            }
            return false;
        }

        UUID playerUUID = LoggedPlayers.getPlayer(args[1]);
        if (playerUUID == null) {
            unknownPlayerHandler.failUnknownPlayer(sender, args[1], silent);
            return false;
        }

        Bounty bounty = getBounty(playerUUID);
        Player receiver;
        if (args.length > 2 && adminPermission) {
            // /bounty poster (player) (player/hologram)
            if (args[2].equalsIgnoreCase("hologram") && parser != null) {
                BountyMap.registerHologram(new HologramRenderer(playerUUID, NotBounties.getInstance(), parser.getLocation(), parser.getEyeLocation().getDirection()));
                sender.sendMessage(parse(getPrefix() + ChatColor.YELLOW + "EXPERIMENTAL: Spawned a bounty hologram", parser));
                return true;
            }
            receiver = Bukkit.getPlayer(args[2]);
            if (receiver == null) {
                unknownPlayerHandler.failUnknownPlayer(sender, args[2], silent);
                return false;
            }
            if (!silent)
                sender.sendMessage(parse(getPrefix() + getMessage("map-give"), Bukkit.getOfflinePlayer(playerUUID), receiver));
        } else if (sender instanceof Player player) {
            // /bounty poster (player)
            receiver = player;
            if (player.getInventory().getItemInMainHand().getType() == Material.MAP
                    && (BountyMap.isWritePoster() || forcePermission || sender.hasPermission("notbounties.poster.write"))) {
                if (player.getInventory().getItemInMainHand().getAmount() > 1) {
                    player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            } else if (!(BountyMap.isGiveOwn() || adminPermission || forcePermission || sender.hasPermission("notbounties.spawnposter"))) {
                // not writing map in hand and doesn't have permission to spawn a poster
                if (!silent)
                    sender.sendMessage(parse(getPrefix() + getMessage("require-empty-map"), parser));
                return false;
            }
        } else {
            if (!silent)
                sender.sendMessage("You cant give yourself this item!");
            return false;
        }

        ItemStack mapItem = bounty != null ? BountyMap.getMap(bounty) : BountyMap.getMap(playerUUID, 0, System.currentTimeMillis());
        NumberFormatting.givePlayer(receiver, mapItem, 1);
        receiver.sendMessage(parse(getPrefix() + getMessage("map-receive"), Bukkit.getOfflinePlayer(playerUUID)));
        return true;
    }

    private boolean hasCommandPermission(CommandSender sender, boolean forcePermission, boolean adminPermission) {
        return adminPermission || forcePermission || sender.hasPermission("notbounties.poster");
    }

}

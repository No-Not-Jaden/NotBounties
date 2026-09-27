package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.RemovePersistentEntitiesEvent;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.features.settings.auto_bounties.BigBounty;
import me.jadenp.notbounties.features.settings.display.BountyHunt;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.features.settings.money.Vouchers;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.settings.auto_bounties.TrickleBounties;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.*;
import me.jadenp.notbounties.features.settings.auto_bounties.TimedBounties;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.integrations.external_api.MMOLibClass;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.BountyManager.*;

import static me.jadenp.notbounties.features.LanguageOptions.*;

public class Events implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (NotBounties.isPaused())
            return;

        BigBounty.removeParticle(event.getPlayer().getUniqueId());
        WantedTags.removeWantedTag(event.getPlayer().getUniqueId());

        if (ConfigOptions.getIntegrations().isMmoLibEnabled())
            MMOLibClass.removeStats(event.getPlayer());

        BountyHunt.logout(event.getPlayer());
        TimedBounties.logout(event.getPlayer());
        ImmunityManager.logout(event.getPlayer());
        BountyExpire.logout(event.getPlayer());
        DataManager.logout(event.getPlayer());


        DataManager.getBountyAsync(event.getPlayer().getUniqueId()).thenAccept(bounty -> {
            if (bounty != null) {
                ActionCommands.executeBountyQuit(event.getPlayer(), bounty);
            }
        });
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (NotBounties.isPaused()) {
            NotBounties.debugMessage("Plugin is paused. Ignoring death.", false);
            return;
        }
        if (event.getEntity() instanceof Player player) {
            if (event.getEntity().getKiller() == null) {
                // natural death
                NotBounties.debugMessage("Natural death for " + player.getName(), false);
                DataManager.getBountyAsync(player.getUniqueId()).thenAccept(currentBounty -> {
                    if (currentBounty != null) {
                        Bounty lostBounty = TrickleBounties.getLostBounty(currentBounty);
                        List<Setter> removedSetters = new LinkedList<>(lostBounty.getSetters());
                        if (!removedSetters.isEmpty()) {
                            Messages.send(player, LanguageOptions.getMessage("natural-death"), MessageContext.builder().amount(lostBounty.getTotalBounty()).receiver(player).build());
                            DataManager.removeSetters(currentBounty, removedSetters);
                        }
                    }
                });
            } else {
                if (ConfigOptions.getClaimOrder() == ConfigOptions.ClaimOrder.REGULAR) {
                    Player killer = event.getEntity().getKiller();
                    claimBounty(player, killer, event.getDrops(), false, ConfigOptions.getMoney().getDeathTax());
                }
            }
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (NotBounties.isPaused())
            return;
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR && NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.AUTOMATIC && event.getItem() != null) {
            // redeem reward later vouchers
            ItemStack item = player.getInventory().getItemInMainHand();
            // Vouchers.redeemHeldVoucher handles a successful voucher redeem
            if (
                    (Vouchers.isVoucher(item) && !Vouchers.redeemHeldVoucher(player))
                    || (Vouchers.isLegacyVoucher(item) && !Vouchers.redeemHeldLegacyVoucher(item, player))
            ) {
                // voucher cannot be redeemed
                Messages.send(player, LanguageOptions.getMessage("voucher-redeem-fail"), MessageContext.builder().receiver(player).build());
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && BountyBoard.getBoardSetup().containsKey(event.getPlayer().getUniqueId())) {
            // bounty board setup
            event.setCancelled(true);
            if (BountyBoard.getBoardSetup().get(event.getPlayer().getUniqueId()) == -1) {
                BountyBoard.getBoardSetup().remove(event.getPlayer().getUniqueId());
                Messages.send(player, ChatColor.RED + "Canceled board removal.", MessageContext.builder().receiver(player).build());
                return;
            }
            Location location = Objects.requireNonNull(event.getClickedBlock()).getRelative(event.getBlockFace()).getLocation();
            BountyBoard.addBountyBoard(new BountyBoard(location, event.getBlockFace(), BountyBoard.getBoardSetup().get(event.getPlayer().getUniqueId())));
            Messages.send(player, ChatColor.GREEN + "Registered bounty board at " + location.getX() + " " + location.getY() + " " + location.getZ() + ".", MessageContext.builder().receiver(player).build());
            BountyBoard.getBoardSetup().remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (NotBounties.isPaused())
            return;
        if (BountyBoard.getBoardSetup().containsKey(event.getPlayer().getUniqueId()) && BountyBoard.getBoardSetup().get(event.getPlayer().getUniqueId()) == -1 && (event.getRightClicked().getType() == EntityType.ITEM_FRAME || (NotBounties.getServerVersion() >= 17 && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME))) {
            event.setCancelled(true);
            int removes = BountyBoard.removeSpecificBountyBoard((ItemFrame) event.getRightClicked());
            BountyBoard.getBoardSetup().remove(event.getPlayer().getUniqueId());
            Messages.send(event.getPlayer(), ChatColor.GREEN + "Removed " + removes + " bounty board(s).", MessageContext.builder().receiver(event.getPlayer()).build());
        }
    }

    public static void login(Player player) {
        LoggedPlayers.login(player);
        DataManager.login(player);
        BountyHunt.login(player);
        TimedBounties.login(player);
        ImmunityManager.login(player);
        BountyExpire.login(player);
        ChallengeManager.login(player);

        NotBounties.getServerImplementation().entity(player).runDelayed(() -> {
            // make sure they are online still
            if (!player.isOnline())
                return;

            // log timezone
            LocalTime.formatTime(0, LocalTime.TimeFormat.PLAYER, player);

            // get skin info
            SkinManager.isSkinLoaded(player.getUniqueId());

            DataManager.handleRefund(player);

            DataManager.getPlayerDataAsync(player.getUniqueId()).thenAccept(playerData -> {
                if (Whitelist.isEnabled() && !Whitelist.isAllowTogglingWhitelist()) {
                    playerData.getWhitelist().setBlacklist(!Whitelist.isDefaultWhitelist());
                    DataManager.updatePlayerData(playerData);
                }
            });

        }, 40);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        NotBounties.debugMessage("Player logged in: " +  event.getPlayer().getName(), false);
        if (NotBounties.isPaused()) {
            NotBounties.debugMessage("Plugin is paused. Ignoring login.", false);
            return;
        }
        login(event.getPlayer());

        DataManager.getBountyAsync(event.getPlayer().getUniqueId()).thenAccept(bounty -> {
            if (bounty != null) {
                DataManager.notifyBounty(event.getPlayer(), bounty);
                // check if the player should be given a wanted tag
                if (WantedTags.isEnabled() && bounty.getTotalDisplayBounty() >= WantedTags.getMinWanted()) {
                    WantedTags.addWantedTag(event.getPlayer());
                }

                if (ConfigOptions.getIntegrations().isMmoLibEnabled())
                    MMOLibClass.addStats(event.getPlayer(), bounty.getTotalDisplayBounty());

                ActionCommands.executeBountyJoin(event.getPlayer(), bounty);
            }
        });


        // check for updates
        if (NotBounties.isUpdateAvailable() && !ConfigOptions.getUpdateNotification().equals("false")
                && NotBounties.getLatestVersion() != null
                && !ConfigOptions.getUpdateNotification().equalsIgnoreCase(getLatestVersion())
                && event.getPlayer().hasPermission(NotBounties.getAdminPermission())) {
            Messages.send(event.getPlayer(), getMessage("update-notification").replace("{current}", NotBounties.getInstance().getDescription().getVersion()).replace("{latest}", NotBounties.getLatestVersion()), MessageContext.builder().receiver(event.getPlayer()).build());

            CompletableFuture<TextComponent> prefixMsg =  Messages.getTextComponent(getPrefix(), MessageContext.builder().receiver(event.getPlayer()).withPrefix(false).build());
            CompletableFuture<TextComponent> disableUpdate =  Messages.getTextComponent(getMessage("disable-update-notification"), MessageContext.builder().receiver(event.getPlayer()).withPrefix(false).build());
            CompletableFuture<TextComponent> skipUpdate =  Messages.getTextComponent(getMessage("skip-update"), MessageContext.builder().receiver(event.getPlayer()).withPrefix(false).build());
            CompletableFuture.allOf(prefixMsg, disableUpdate, skipUpdate).thenAccept(v -> {
                TextComponent prefix = prefixMsg.join();
                TextComponent disable = disableUpdate.join();
                TextComponent skip = skipUpdate.join();
                disable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_PURPLE + "update-notification: false")));
                disable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  "/" + ConfigOptions.getPluginBountyCommands().getFirst() + " update-notification false"));
                skip.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_PURPLE + "update-notification: " + getLatestVersion())));
                skip.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  "/" + ConfigOptions.getPluginBountyCommands().getFirst() + " update-notification " + getLatestVersion()));
                NotBounties.getServerImplementation().entity(event.getPlayer()).run(() -> {
                    BaseComponent[] baseComponents = new BaseComponent[]{prefix, skip};
                    event.getPlayer().spigot().sendMessage(baseComponents);
                    baseComponents = new BaseComponent[]{prefix, disable};
                    event.getPlayer().spigot().sendMessage(baseComponents);
                });
            });


        }

        // remove persistent bounty entities in chunk
        if (WantedTags.isEnabled() || !BountyBoard.getBountyBoards().isEmpty())
            RemovePersistentEntitiesEvent.cleanChunk(event.getPlayer().getLocation());

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (ConfigOptions.getClaimOrder() != ConfigOptions.ClaimOrder.BEFORE || !(event.getEntity() instanceof Player player) || !(event.getDamager() instanceof Player) || NotBounties.isPaused())
            return;
        if (event.getDamage() >= player.getHealth() && player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && player.getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
            claimBounty(player, (Player) event.getDamager(), Arrays.asList(player.getInventory().getContents()), true, ConfigOptions.getMoney().getDeathTax());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (ConfigOptions.getClaimOrder() != ConfigOptions.ClaimOrder.AFTER || NotBounties.isPaused())
            return;
        Player player = event.getPlayer();
        Player killer = player.getKiller();
        if (killer != null)
            claimBounty(player, killer, Arrays.asList(player.getInventory().getContents()), true, ConfigOptions.getMoney().getDeathTax());

    }

    @EventHandler
    public void onCommandSend(PlayerCommandPreprocessEvent event) {
        if (NotBounties.isPaused() || !LoggedPlayers.hasActiveBounty(event.getPlayer().getUniqueId()))
            return;
        String message = event.getMessage().toLowerCase();
        // remove starting /
        if (message.startsWith("/"))
            message = message.substring(1);
        // remove trailing space
        if (!message.isEmpty() && message.charAt(message.length() - 1) == ' ') {
            message = message.substring(0, message.length() - 1);
        }
        // check if message has anything left
        if (message.isEmpty())
            return;
        // remove plugin specific command identifier.
        if (message.contains(" ") && message.substring(0, message.indexOf(" ")).contains(":")) {
            message = message.substring(message.indexOf(":") + 1);
        }

        for (String command : ConfigOptions.getAutoBounties().getBlockedBountyCommands()) {
            if (message.startsWith(command)) {
                event.setCancelled(true);
                Messages.send(event.getPlayer(), LanguageOptions.getMessage("blocked-bounty-command"), MessageContext.builder().receiver(event.getPlayer()).build());
                return;
            }
        }
    }
}

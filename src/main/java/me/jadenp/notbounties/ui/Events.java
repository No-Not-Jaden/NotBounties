package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.RemovePersistentEntitiesEvent;
import me.jadenp.notbounties.data.PlayerData;
import me.jadenp.notbounties.data.RewardHead;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.features.settings.auto_bounties.BigBounty;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.BountyManager;
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

        TimedBounties.logout(event.getPlayer());
        ImmunityManager.logout(event.getPlayer());
        BountyExpire.logout(event.getPlayer());
        DataManager.logout(event.getPlayer());

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (NotBounties.isPaused())
            return;
        if (event.getEntity() instanceof Player player) {
            if (event.getEntity().getKiller() == null) {
                // natural death
                Bounty currentBounty = BountyManager.getBounty(player.getUniqueId());
                if (currentBounty != null) {
                    Bounty lostBounty = TrickleBounties.getLostBounty(currentBounty);
                    List<Setter> removedSetters = new LinkedList<>(lostBounty.getSetters());
                    if (!removedSetters.isEmpty()) {
                        player.sendMessage(parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("natural-death"), lostBounty.getTotalBounty(), player));
                        DataManager.removeSetters(currentBounty, removedSetters);
                    }
                }
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
        // redeem reward later
        if (event.getAction() == Action.RIGHT_CLICK_AIR && NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.AUTOMATIC && event.getItem() != null) {
            ItemStack item = event.getItem();
            Player player = event.getPlayer();
            if (item.getType() == Material.PAPER && item.getItemMeta() != null && item.getItemMeta().getLore() != null) {
                if (!item.getItemMeta().getLore().isEmpty()) {
                    String lastLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
                    if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                        String reward = ChatColor.stripColor(lastLine).substring(1);
                        double amount;
                        try {
                            amount = NumberFormatting.tryParse(reward);
                        } catch (NumberFormatException ignored) {
                            player.sendMessage(ChatColor.RED + "Error redeeming reward");
                            return;
                        }
                        NumberFormatting.doAddCommands(player, amount * item.getAmount());
                        player.getInventory().remove(item);
                        player.sendMessage(parse(getPrefix() + getMessage("redeem-voucher"), amount * item.getAmount(), player));
                    }
                }
            }

        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && BountyBoard.getBoardSetup().containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            if (BountyBoard.getBoardSetup().get(event.getPlayer().getUniqueId()) == -1) {
                BountyBoard.getBoardSetup().remove(event.getPlayer().getUniqueId());
                event.getPlayer().sendMessage(parse(getPrefix() + ChatColor.RED + "Canceled board removal.", event.getPlayer()));
                return;
            }
            Location location = Objects.requireNonNull(event.getClickedBlock()).getRelative(event.getBlockFace()).getLocation();
            BountyBoard.addBountyBoard(new BountyBoard(location, event.getBlockFace(), BountyBoard.getBoardSetup().get(event.getPlayer().getUniqueId())));
            event.getPlayer().sendMessage(parse(getPrefix() + ChatColor.GREEN + "Registered bounty board at " + location.getX() + " " + location.getY() + " " + location.getZ() + ".", event.getPlayer()));
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
            event.getPlayer().sendMessage(parse(getPrefix() + ChatColor.GREEN + "Removed " + removes + " bounty boards.", event.getPlayer()));
        }
    }

    public static void login(Player player) {
        TimedBounties.login(player);
        ImmunityManager.login(player);
        BountyExpire.login(player);
        ChallengeManager.login(player);
        DataManager.login(player);
        LoggedPlayers.login(player);

        NotBounties.getServerImplementation().entity(player).runDelayed(() -> {
            // make sure they are online still
            if (!player.isOnline())
                return;

            // log timezone
            LocalTime.formatTime(0, LocalTime.TimeFormat.PLAYER, player);

            // get skin info
            SkinManager.isSkinLoaded(player.getUniqueId());
        }, 40);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (NotBounties.isPaused())
            return;

        login(event.getPlayer());

        Bounty bounty = getBounty(event.getPlayer().getUniqueId());
        if (bounty != null) {
            bounty.setDisplayName(event.getPlayer().getName());
            DataManager.notifyBounty(event.getPlayer());
            // check if player should be given a wanted tag
            if (WantedTags.isEnabled() && bounty.getTotalDisplayBounty() >= WantedTags.getMinWanted()) {
                WantedTags.addWantedTag(event.getPlayer());
            }

            if (ConfigOptions.getIntegrations().isMmoLibEnabled())
                MMOLibClass.addStats(event.getPlayer(), bounty.getTotalDisplayBounty());
        }

        PlayerData playerData = DataManager.getPlayerData(event.getPlayer().getUniqueId());

        for (RewardHead rewardHead : playerData.getRewardHeads()) {
            NumberFormatting.givePlayer(event.getPlayer(), rewardHead.getItem(), 1);
        }
        playerData.getRewardHeads().clear();

        // check for updates
        if (NotBounties.isUpdateAvailable() && !ConfigOptions.getUpdateNotification().equals("false")
                && NotBounties.getLatestVersion() != null
                && !ConfigOptions.getUpdateNotification().equalsIgnoreCase(getLatestVersion())
                && event.getPlayer().hasPermission(NotBounties.getAdminPermission())) {
            event.getPlayer().sendMessage(parse(getPrefix() + getMessage("update-notification").replace("{current}", NotBounties.getInstance().getDescription().getVersion()).replace("{latest}", NotBounties.getLatestVersion()), event.getPlayer()));
            TextComponent prefixMsg = (TextComponent) TextComponent.fromLegacy(parse(getPrefix(), event.getPlayer()));
            TextComponent disableUpdate = (TextComponent) TextComponent.fromLegacy(parse(getMessage("disable-update-notification"), event.getPlayer()));
            disableUpdate.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_PURPLE + "update-notification: false")));
            disableUpdate.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  "/" + ConfigOptions.getPluginBountyCommands().get(0) + " update-notification false"));
            TextComponent skipUpdate = (TextComponent) TextComponent.fromLegacy(parse(getMessage("skip-update"), event.getPlayer()));
            skipUpdate.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_PURPLE + "update-notification: " + getLatestVersion())));
            skipUpdate.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  "/" + ConfigOptions.getPluginBountyCommands().get(0) + " update-notification " + getLatestVersion()));
            BaseComponent[] baseComponents = new BaseComponent[]{prefixMsg, skipUpdate};
            event.getPlayer().spigot().sendMessage(baseComponents);
            baseComponents = new BaseComponent[]{prefixMsg, disableUpdate};
            event.getPlayer().spigot().sendMessage(baseComponents);
        }


        // check if they had a bounty refunded
        if (playerData.getRefundAmount() > 0 && NumberFormatting.getManualEconomy() != NumberFormatting.ManualEconomy.PARTIAL) {
            NumberFormatting.doAddCommands(event.getPlayer(), playerData.getRefundAmount());
        }
        if (!playerData.getRefundItems().isEmpty() && NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.AUTOMATIC) {
            NumberFormatting.givePlayer(event.getPlayer(), playerData.getRefundItems(), false);
        }
        playerData.clearRefund();

        // remove persistent bounty entities in chunk
        if (WantedTags.isEnabled() || !BountyBoard.getBountyBoards().isEmpty())
            RemovePersistentEntitiesEvent.cleanChunk(event.getPlayer().getLocation());

        playerData.setLastSeen(System.currentTimeMillis());
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
        if (DataManager.getLocalData().getOnlineBounty(event.getPlayer().getUniqueId()) == null)
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
                event.getPlayer().sendMessage(parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("blocked-bounty-command"), event.getPlayer()));
                return;
            }
        }

    }

}

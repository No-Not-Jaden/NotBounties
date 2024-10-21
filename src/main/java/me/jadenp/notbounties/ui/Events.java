package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.RemovePersistentEntitiesEvent;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.UpdateChecker;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class Events implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (NotBounties.isPaused())
            return;
        if (immunePerms.contains(event.getPlayer().getUniqueId().toString())) {
            if (!event.getPlayer().hasPermission("notbounties.immune")) {
                immunePerms.remove(event.getPlayer().getUniqueId().toString());
            }
        } else {
            if (event.getPlayer().hasPermission("notbounties.immune")) {
                immunePerms.add(event.getPlayer().getUniqueId().toString());
            }
        }
        if (autoImmuneMurderPerms.contains(event.getPlayer().getUniqueId().toString())) {
            if (!event.getPlayer().hasPermission("notbounties.immunity.murder")) {
                autoImmuneMurderPerms.remove(event.getPlayer().getUniqueId().toString());
            }
        } else {
            if (event.getPlayer().hasPermission("notbounties.immunity.murder")) {
                autoImmuneMurderPerms.add(event.getPlayer().getUniqueId().toString());
            }
        }
        if (autoImmuneRandomPerms.contains(event.getPlayer().getUniqueId().toString())) {
            if (!event.getPlayer().hasPermission("notbounties.immunity.random")) {
                autoImmuneRandomPerms.remove(event.getPlayer().getUniqueId().toString());
            }
        } else {
            if (event.getPlayer().hasPermission("notbounties.immunity.random")) {
                autoImmuneRandomPerms.add(event.getPlayer().getUniqueId().toString());
            }
        }
        if (autoImmuneTimedPerms.contains(event.getPlayer().getUniqueId().toString())) {
            if (!event.getPlayer().hasPermission("notbounties.immunity.timed")) {
                autoImmuneTimedPerms.remove(event.getPlayer().getUniqueId().toString());
            }
        } else {
            if (event.getPlayer().hasPermission("notbounties.immunity.timed")) {
                autoImmuneTimedPerms.add(event.getPlayer().getUniqueId().toString());
            }
        }
        displayParticle.remove(event.getPlayer().getUniqueId());
        NotBounties.removeWantedTag(event.getPlayer().getUniqueId());

        TimedBounties.logout(event.getPlayer());
        Immunity.logout(event.getPlayer());
        BountyExpire.logout(event.getPlayer());
        DataManager.logout(event.getPlayer());

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (claimOrder != ClaimOrder.REGULAR ||
                !(event.getEntity() instanceof Player player) ||
                event.getEntity().getKiller() == null ||
                NotBounties.isPaused())
            return;
        Player killer = event.getEntity().getKiller();
        claimBounty(player, killer, event.getDrops(), false);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (NotBounties.isPaused())
            return;
        // redeem reward later
        if (event.getAction() == Action.RIGHT_CLICK_AIR && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.AUTOMATIC && event.getItem() != null) {
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

        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && NotBounties.boardSetup.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            if (NotBounties.boardSetup.get(event.getPlayer().getUniqueId()) == -1) {
                NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
                event.getPlayer().sendMessage(parse(getPrefix() + ChatColor.RED + "Canceled board removal.", event.getPlayer()));
                return;
            }
            Location location = Objects.requireNonNull(event.getClickedBlock()).getRelative(event.getBlockFace()).getLocation();
            addBountyBoard(new BountyBoard(location, event.getBlockFace(), NotBounties.boardSetup.get(event.getPlayer().getUniqueId())));
            event.getPlayer().sendMessage(parse(getPrefix() + ChatColor.GREEN + "Registered bounty board at " + location.getX() + " " + location.getY() + " " + location.getZ() + ".", event.getPlayer()));
            NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (NotBounties.isPaused())
            return;
        if (NotBounties.boardSetup.containsKey(event.getPlayer().getUniqueId()) && NotBounties.boardSetup.get(event.getPlayer().getUniqueId()) == -1 && (event.getRightClicked().getType() == EntityType.ITEM_FRAME || (serverVersion >= 17 && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME))) {
            event.setCancelled(true);
            int removes = removeSpecificBountyBoard((ItemFrame) event.getRightClicked());
            NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage(parse(getPrefix() + ChatColor.GREEN + "Removed " + removes + " bounty boards.", event.getPlayer()));
        }
    }

    public static void logPlayer(Player player) {
        // check if they are logged yet
        if (!loggedPlayers.containsValue(player.getUniqueId())) {
            // if not, add them
            loggedPlayers.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
        } else {
            // if they are, check if their username has changed, and update it
            if (!loggedPlayers.containsKey(player.getName().toLowerCase(Locale.ROOT))) {
                String nameToRemove = "";
                for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                    if (entry.getValue().equals(player.getUniqueId())) {
                        nameToRemove = entry.getKey();
                    }
                }
                if (!Objects.equals(nameToRemove, "")) {
                    loggedPlayers.remove(nameToRemove);
                }
                loggedPlayers.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
            }
        }
    }

    public static void login(Player player) {
        TimedBounties.login(player);
        Immunity.login(player);
        BountyExpire.login(player);
        ChallengeManager.login(player);
        DataManager.login(player);

        logPlayer(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                // make sure they are online still
                if (!player.isOnline())
                    return;

                // log timezone
                LocalTime.formatTime(0, LocalTime.TimeFormat.PLAYER, player);

                // get skin info
                SkinManager.isSkinLoaded(player.getUniqueId());
            }
        }.runTaskLater(NotBounties.getInstance(), 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (NotBounties.isPaused())
            return;

        login(event.getPlayer());

        if (hasBounty(event.getPlayer().getUniqueId())) {
            Bounty bounty = getBounty(event.getPlayer().getUniqueId());
            assert bounty != null;
            bounty.setDisplayName(event.getPlayer().getName());
            DataManager.notifyBounty(event.getPlayer());
            // check if player should be given a wanted tag
            if (WantedTags.isEnabled() && bounty.getTotalDisplayBounty() >= WantedTags.getMinWanted() && !NotBounties.wantedText.containsKey(event.getPlayer().getUniqueId())) {
                NotBounties.wantedText.put(event.getPlayer().getUniqueId(), new WantedTags(event.getPlayer()));
            }
        }

        if (headRewards.containsKey(event.getPlayer().getUniqueId())) {
            for (RewardHead rewardHead : headRewards.get(event.getPlayer().getUniqueId())) {
                NumberFormatting.givePlayer(event.getPlayer(), rewardHead.getItem(), 1);
            }
            headRewards.remove(event.getPlayer().getUniqueId());
        }
        if (BountyExpire.removeExpiredBounties()) {
            GUI.reopenBountiesGUI();
        }

        // check for updates
        if (updateNotification && !NotBounties.latestVersion && event.getPlayer().hasPermission(NotBounties.getAdminPermission())) {
                new UpdateChecker(NotBounties.getInstance(), 104484).getVersion(version -> {
                    if (NotBounties.getInstance().getDescription().getVersion().contains("dev"))
                        return;
                    if (NotBounties.getInstance().getDescription().getVersion().equals(version))
                        return;
                    event.getPlayer().sendMessage(parse(getPrefix() + getMessage("update-notification").replace("{current}", NotBounties.getInstance().getDescription().getVersion()).replace("{latest}", version), event.getPlayer()));
                    TextComponent prefixMsg = new TextComponent(parse(getPrefix(), event.getPlayer()));
                    TextComponent msg = new TextComponent(parse(getMessage("disable-update-notification"), event.getPlayer()));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_PURPLE + "update-notification: false")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  "/" + pluginBountyCommands.get(0) + " update-notification false"));
                    BaseComponent[] baseComponents = new BaseComponent[]{prefixMsg, msg};
                    event.getPlayer().spigot().sendMessage(baseComponents);
                });
            }


        // check if they had a bounty refunded
        if (refundedBounties.containsKey(event.getPlayer().getUniqueId())) {
            if (NumberFormatting.manualEconomy != NumberFormatting.ManualEconomy.PARTIAL)
                NumberFormatting.doAddCommands(event.getPlayer(), refundedBounties.get(event.getPlayer().getUniqueId()));
            refundedBounties.remove(event.getPlayer().getUniqueId());
        }
        if (refundedItems.containsKey(event.getPlayer().getUniqueId())) {
            if (NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.AUTOMATIC)
                NumberFormatting.givePlayer(event.getPlayer(), refundedItems.get(event.getPlayer().getUniqueId()), false);
            refundedItems.remove(event.getPlayer().getUniqueId());
        }

        // remove persistent bounty entities in chunk
        if (WantedTags.isEnabled() || !bountyBoards.isEmpty())
            RemovePersistentEntitiesEvent.cleanChunk(event.getPlayer().getLocation());


    }


    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (NotBounties.isPaused())
            return;
        // remove persistent entities (wanted tags & bounty boards)
        if (serverVersion <= 16)
            if (WantedTags.isEnabled() || !bountyBoards.isEmpty())
                for (Entity entity : event.getChunk().getEntities()) {
                    if (entity == null)
                        return;
                    if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.ITEM_FRAME)
                        continue;
                    PersistentDataContainer container = entity.getPersistentDataContainer();
                    if (container.has(namespacedKey, PersistentDataType.STRING)) {
                        String value = container.get(namespacedKey, PersistentDataType.STRING);
                        if (value == null)
                            continue;
                        if (!value.equals(sessionKey)) {
                            entity.remove();
                        }
                    }
                }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (claimOrder != ClaimOrder.BEFORE || !(event.getEntity() instanceof Player player) || !(event.getDamager() instanceof Player) || NotBounties.isPaused())
            return;
        if (event.getDamage() >= player.getHealth() && player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && player.getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
            claimBounty(player, (Player) event.getDamager(), Arrays.asList(player.getInventory().getContents()), true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (claimOrder != ClaimOrder.AFTER || NotBounties.isPaused())
            return;
        Player player = event.getPlayer();
        Player killer = player.getKiller();
        if (killer != null)
            claimBounty(player, killer, Arrays.asList(player.getInventory().getContents()), true);

    }

}

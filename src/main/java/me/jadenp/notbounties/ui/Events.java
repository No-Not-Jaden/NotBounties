package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.bountyEvents.BountyClaimEvent;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.utils.BountyClaimRequirements;
import me.jadenp.notbounties.utils.UpdateChecker;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.configuration.autoBounties.MurderBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class Events implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (immunePerms.contains(event.getPlayer().getUniqueId().toString())) {
            if (!event.getPlayer().hasPermission("notbounties.immune")) {
                immunePerms.remove(event.getPlayer().getUniqueId().toString());
            }
        } else {
            if (event.getPlayer().hasPermission("notbounties.immune")) {
                immunePerms.add(event.getPlayer().getUniqueId().toString());
            }
        }
        displayParticle.remove(event.getPlayer());
        if (NotBounties.wantedText.containsKey(event.getPlayer().getUniqueId())) {
            NotBounties.wantedText.get(event.getPlayer().getUniqueId()).removeStand();
            NotBounties.wantedText.remove(event.getPlayer().getUniqueId());
        }

        TimedBounties.logout(event.getPlayer());
        Immunity.logout(event.getPlayer());
        BountyExpire.logout(event.getPlayer());

        if (SQL.isConnected())
            data.logout(event.getPlayer());

    }

    public void claimBounty(@NotNull Player player, @NotNull Player killer, List<ItemStack> drops, boolean forceEditDrops) {
        TimedBounties.onDeath(player);
        // check if a bounty can be claimed
        if (!BountyClaimRequirements.canClaim(player, killer))
            return;

        MurderBounties.killPlayer(player, killer);

        if (!hasBounty(player) || player == killer)
            return;

        // check if it is a npc
        if (!npcClaim && killer.hasMetadata("NPC"))
            return;
        Bounty bounty = getBounty(player);
        assert bounty != null;
        // check if killer can claim it
        if (bounty.getTotalBounty(killer) < 0.01)
            return;
        BountyClaimEvent event1 = new BountyClaimEvent(killer, new Bounty(bounty));
        Bukkit.getPluginManager().callEvent(event1);
        if (event1.isCancelled())
            return;
        double totalBounty = bounty.getTotalBounty(killer);

        List<Setter> claimedBounties = new ArrayList<>(bounty.getSetters());
        claimedBounties.removeIf(setter -> !setter.canClaim(killer));

        displayParticle.remove(player);

        // broadcast message
        String message = parse(prefix + claimBountyBroadcast, player.getName(), killer.getName(), bounty.getTotalBounty(killer), player);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ((!NotBounties.disableBroadcast.contains(p.getUniqueId()) && bounty.getTotalBounty(killer) >= minBroadcast) || p.getUniqueId().equals(player.getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(killer).getUniqueId())) {
                p.sendMessage(message);
            }
        }

        // reward head
        RewardHead rewardHead = new RewardHead(player.getName(), player.getUniqueId(), bounty.getTotalBounty(killer));

        if (rewardHeadSetter) {
            for (Setter setter : claimedBounties) {
                if (!setter.getUuid().equals(new UUID(0, 0))) {
                    Player p = Bukkit.getPlayer(setter.getUuid());
                    if (p != null) {
                        if (!rewardHeadClaimed || !Objects.requireNonNull(killer).getUniqueId().equals(setter.getUuid())) {
                            NumberFormatting.givePlayer(p, rewardHead.getItem(), 1);
                        }
                    } else {
                        if (headRewards.containsKey(setter.getUuid())) {
                            // I think this could be replaced with headRewards.get(setter.getUuid()).add(rewardHead)
                            List<RewardHead> heads = new ArrayList<>(headRewards.get(setter.getUuid()));
                            heads.add(rewardHead);
                            headRewards.replace(setter.getUuid(), heads);
                        } else {
                            headRewards.put(setter.getUuid(), Collections.singletonList(rewardHead));
                        }
                    }
                }
            }
        }
        if (rewardHeadClaimed) {
            NumberFormatting.givePlayer(killer, rewardHead.getItem(), 1);
        }
        // do commands
        if (ConfigOptions.deathTax > 0 && NumberFormatting.manualEconomy != NumberFormatting.ManualEconomy.PARTIAL) {
            Map<Material, Long> removedItems = NumberFormatting.doRemoveCommands(player, bounty.getTotalBounty(killer) * ConfigOptions.deathTax, drops);
            if (!removedItems.isEmpty()) {
                // send message
                long totalLoss = 0;
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<Material, Long> entry : removedItems.entrySet()) {
                    builder.append(entry.getValue()).append("x").append(entry.getKey().toString()).append(", ");
                    totalLoss += entry.getValue();
                }
                builder.replace(builder.length() - 2, builder.length(), "");
                if (totalLoss > 0) {
                    player.sendMessage(parse(prefix + LanguageOptions.deathTax.replaceAll("\\{items}", Matcher.quoteReplacement(builder.toString())), player));
                    // modify drops
                    if (forceEditDrops)
                        for (Map.Entry<Material, Long> entry : removedItems.entrySet())
                            NumberFormatting.removeItem(player, entry.getKey(), entry.getValue(), -1);
                    ListIterator<ItemStack> dropsIterator = drops.listIterator();
                    while (dropsIterator.hasNext()) {
                        ItemStack drop = dropsIterator.next();
                        if (removedItems.containsKey(drop.getType())) {
                            if (removedItems.get(drop.getType()) > drop.getAmount()) {
                                removedItems.replace(drop.getType(), removedItems.get(drop.getType()) - drop.getAmount());
                                dropsIterator.remove();
                            } else if (removedItems.get(drop.getType()) == drop.getAmount()) {
                                removedItems.remove(drop.getType());
                                dropsIterator.remove();
                            } else {
                                drop.setAmount((int) (drop.getAmount() - removedItems.get(drop.getType())));
                                dropsIterator.set(drop);
                                removedItems.remove(drop.getType());
                            }
                        }
                    }
                }
            }
        }
        if (!redeemRewardLater) {
            if (NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL) {
                NumberFormatting.doAddCommands(killer, bounty.getBounty(new UUID(0,0)).getTotalBounty(killer));
            } else {
                NumberFormatting.doAddCommands(killer, bounty.getTotalBounty(killer));
            }
        } else {
            // give voucher
            if (NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL) {
                NumberFormatting.doAddCommands(killer, bounty.getBounty(new UUID(0,0)).getTotalBounty(killer));
            }
            if (RRLVoucherPerSetter) {
                // multiple vouchers
                for (Setter setter : bounty.getSetters()) {
                    if (!setter.canClaim(killer))
                        continue;
                    if (setter.getUuid().equals(new UUID(0,0)) && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL)
                        continue;
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    assert meta != null;
                    ArrayList<String> lore = new ArrayList<>();
                    for (String str : voucherLore) {
                        lore.add(parse(str.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalBounty(killer)) + NumberFormatting.currencySuffix)), player.getName(), Objects.requireNonNull(player).getName(),setter.getAmount(), player));
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.setDisplayName(parse(bountyVoucherName.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalBounty(killer)) + NumberFormatting.currencySuffix)), player.getName(), Objects.requireNonNull(killer).getName(), setter.getAmount(), player));
                    ArrayList<String> setterLore = new ArrayList<>(lore);
                    if (!RRLSetterLoreAddition.isEmpty()) {
                        setterLore.add(parse(RRLSetterLoreAddition, setter.getName(), setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                    setterLore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + setter.getAmount());
                    meta.setLore(setterLore);
                    item.setItemMeta(meta);
                    item.addUnsafeEnchantment(Enchantment.DURABILITY, 0);
                    NumberFormatting.givePlayer(killer, item, 1);
                }
            } else {
                // one voucher
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                assert meta != null;
                ArrayList<String> lore = new ArrayList<>();
                for (String str : voucherLore) {
                    lore.add(parse(str, player.getName(), Objects.requireNonNull(player.getKiller()).getName(), bounty.getTotalBounty(killer), player));
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.setDisplayName(parse(bountyVoucherName, player.getName(), Objects.requireNonNull(killer).getName(), bounty.getTotalBounty(killer), player));
                if (!RRLSetterLoreAddition.isEmpty()) {
                    for (Setter setter : bounty.getSetters()) {
                        if (!setter.canClaim(killer))
                            continue;
                        if (setter.getUuid().equals(new UUID(0,0)) && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL)
                            continue;
                        lore.add(parse(RRLSetterLoreAddition, setter.getName(), setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                }
                lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + bounty.getTotalBounty(killer));
                meta.setLore(lore);
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(Enchantment.DURABILITY, 0);
                NumberFormatting.givePlayer(killer, item, 1);
            }
        }
        if (SQL.isConnected()) {
            data.addData(player.getUniqueId().toString(), 0, 0, 1, bounty.getTotalBounty(killer), 0, 0);
            data.addData(killer.getUniqueId().toString(), 1, 0, 0, 0, 0, bounty.getTotalBounty(killer));
        } else {
            allTimeBounties.put(player.getUniqueId(), Leaderboard.ALL.getStat(player.getUniqueId()) + bounty.getTotalBounty(killer));
            killBounties.put(killer.getUniqueId(), Leaderboard.KILLS.getStat(killer.getUniqueId()) + 1);
            deathBounties.put(player.getUniqueId(), Leaderboard.DEATHS.getStat(player.getUniqueId()) + 1);
            allClaimedBounties.put(killer.getUniqueId(), Leaderboard.CLAIMED.getStat(killer.getUniqueId()) + bounty.getTotalBounty(killer));
        }
        bounty.claimBounty(killer);
        updateBounty(bounty);

        if (bounty.getTotalBounty() < minWanted) {
            // remove bounty tag
            NotBounties.removeWantedTag(bounty.getUUID());
        }

        for (Setter setter : claimedBounties) {
            if (!setter.getUuid().equals(new UUID(0, 0))) {
                if (SQL.isConnected()) {
                    data.addData(setter.getUuid().toString(), 0, 1, 0, 0, 0, 0);
                } else {
                    setBounties.put(setter.getUuid(), Leaderboard.SET.getStat(setter.getUuid()) + 1);
                }
                Player p = Bukkit.getPlayer(setter.getUuid());
                if (p != null) {
                    p.playSound(p.getEyeLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1, 1);
                }
            }
        }
       Immunity.startGracePeriod(player);

        if (trackerRemove > 0)
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack[] contents = p.getInventory().getContents();
                boolean change = false;
                for (int x = 0; x < contents.length; x++) {
                    if (contents[x] != null)
                        if (contents[x].getItemMeta() != null) {
                            if (Objects.requireNonNull(contents[x].getItemMeta()).getLore() != null) {
                                if (!Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).isEmpty()) {
                                    String lastLine = Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).get(Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).size() - 1);
                                    if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                        int number;
                                        try {
                                            number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                        } catch (NumberFormatException ignored) {
                                            continue;
                                        }
                                        if (trackedBounties.containsKey(number)) {
                                            if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                contents[x] = null;
                                                change = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
                if (change) {
                    p.getInventory().setContents(contents);
                }
            }
        Commands.reopenBountiesGUI();
        ActionCommands.executeBountyClaim(player, killer, totalBounty);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (claimOrder != ClaimOrder.REGULAR)
            return;
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (event.getEntity().getKiller() == null)
            return;
        Player killer = event.getEntity().getKiller();
        claimBounty(player, killer, event.getDrops(), false);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (tracker)
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getItem() != null) {
                    ItemStack item = event.getItem();
                    Player player = event.getPlayer();
                    if (item.getType() == Material.COMPASS) {
                        if (item.getItemMeta() != null) {
                            if (item.getItemMeta().getLore() != null) {
                                if (!item.getItemMeta().getLore().isEmpty()) {
                                    String lastLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
                                    if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                        if (event.getPlayer().hasPermission("notbounties.tracker")) {
                                            int number;
                                            try {
                                                number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                            } catch (NumberFormatException ignored) {
                                                return;
                                            }
                                            if (trackedBounties.containsKey(number)) {
                                                if (hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();

                                                    String actionBar;
                                                    Bounty bounty = getBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))));
                                                    assert bounty != null;
                                                    Player p = Bukkit.getPlayer(bounty.getUUID());
                                                    if (p != null) {
                                                        compassMeta.setLodestone(p.getLocation());
                                                        compassMeta.setLodestoneTracked(false);
                                                        if (trackerGlow > 0) {
                                                            if (p.getWorld().equals(player.getWorld())) {
                                                                if (player.getLocation().distance(p.getLocation()) < trackerGlow) {
                                                                    p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15, 0));
                                                                }
                                                            }
                                                        }
                                                        actionBar = ChatColor.DARK_GRAY + "|";
                                                        if (TABPlayerName) {
                                                            actionBar += " " + ChatColor.YELLOW + p.getName() + ChatColor.DARK_GRAY + " |";
                                                        }
                                                        if (TABDistance) {
                                                            if (p.getWorld().equals(player.getWorld())) {
                                                                actionBar += " " + ChatColor.GOLD + ((int) player.getLocation().distance(p.getLocation())) + "m" + ChatColor.DARK_GRAY + " |";
                                                            } else {
                                                                actionBar += " ?m |";
                                                            }
                                                        }
                                                        if (TABPosition) {
                                                            actionBar += " " + ChatColor.RED + p.getLocation().getBlockX() + "x " + p.getLocation().getBlockY() + "y " + p.getLocation().getBlockZ() + "z" + ChatColor.DARK_GRAY + " |";
                                                        }
                                                        if (TABWorld) {
                                                            actionBar += " " + ChatColor.LIGHT_PURPLE + p.getWorld().getName() + ChatColor.DARK_GRAY + " |";
                                                        }
                                                    } else {
                                                        actionBar = ChatColor.GRAY + "*offline*";
                                                        if (compassMeta.getLodestone() != null)
                                                            if (!Objects.equals(compassMeta.getLodestone().getWorld(), player.getWorld()))
                                                                if (Bukkit.getWorlds().size() > 1) {
                                                                    for (World world : Bukkit.getWorlds()) {
                                                                        if (!world.equals(player.getWorld())) {
                                                                            compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), world.getSpawnLocation().getY(), world.getSpawnLocation().getZ()));
                                                                        }
                                                                    }
                                                                } else {
                                                                    //compassMeta.setLodestone(null);
                                                                    compassMeta.setLodestoneTracked(true);
                                                                }
                                                    }
                                                    item.setItemMeta(compassMeta);
                                                    // display action bar
                                                    if (trackerActionBar) {
                                                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
                                                    }
                                                }
                                            }
                                        } else {
                                            if (trackerActionBar) {
                                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No Permission."));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // redeem reward later
        if (event.getAction() == Action.RIGHT_CLICK_AIR && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.AUTOMATIC) {
            if (event.getItem() != null) {
                ItemStack item = event.getItem();
                Player player = event.getPlayer();
                if (item.getType() == Material.PAPER) {
                    if (item.getItemMeta() != null) {
                        if (item.getItemMeta().getLore() != null) {
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
                                    player.sendMessage(parse(prefix + redeemVoucher, amount * item.getAmount(), player));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (NotBounties.boardSetup.containsKey(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                if (NotBounties.boardSetup.get(event.getPlayer().getUniqueId()) == -1) {
                    NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
                    event.getPlayer().sendMessage(parse(prefix + ChatColor.RED + "Canceled board removal.", event.getPlayer()));
                    return;
                }
                Location location = Objects.requireNonNull(event.getClickedBlock()).getRelative(event.getBlockFace()).getLocation();
                addBountyBoard(new BountyBoard(location, event.getBlockFace(), NotBounties.boardSetup.get(event.getPlayer().getUniqueId())));
                event.getPlayer().sendMessage(parse(prefix + ChatColor.GREEN + "Registered bounty board at " + location.getX() + " " + location.getY() + " " + location.getZ() + ".", event.getPlayer()));
                NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (NotBounties.boardSetup.containsKey(event.getPlayer().getUniqueId()) && NotBounties.boardSetup.get(event.getPlayer().getUniqueId()) == -1 && (event.getRightClicked().getType() == EntityType.ITEM_FRAME || (serverVersion >= 17 && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME))) {
            event.setCancelled(true);
            int removes = removeSpecificBountyBoard((ItemFrame) event.getRightClicked());
            NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage(parse(prefix + ChatColor.GREEN + "Removed " + removes + " bounty boards.", event.getPlayer()));
        }
    }

    @EventHandler
    public void onHold(PlayerItemHeldEvent event) {
        if (trackerRemove > 0) {
            ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
            if (item != null) {
                if (item.getType() == Material.COMPASS) {
                    if (item.getItemMeta() != null) {
                        if (item.getItemMeta().getLore() != null) {
                            if (!item.getItemMeta().getLore().isEmpty()) {
                                String lastLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
                                if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                    int number;
                                    try {
                                        number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                    } catch (NumberFormatException ignored) {
                                        return;
                                    }
                                    if (trackedBounties.containsKey(number)) {
                                        if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                            event.getPlayer().getInventory().setItem(event.getNewSlot(), null);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onOpenInv(InventoryOpenEvent event) {
        if (tracker)
            if (trackerRemove == 3)
                if (event.getInventory().getType() != InventoryType.CRAFTING) {
                    ItemStack[] contents = event.getInventory().getContents();
                    boolean change = false;
                    for (int x = 0; x < contents.length; x++) {
                        if (contents[x] != null) {
                            if (contents[x].getType() == Material.COMPASS) {
                                if (contents[x].getItemMeta() != null) {
                                    if (Objects.requireNonNull(contents[x].getItemMeta()).getLore() != null) {
                                        if (!Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).isEmpty()) {
                                            String lastLine = Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).get(Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).size() - 1);
                                            if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                                int number;
                                                try {
                                                    number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                                } catch (NumberFormatException ignored) {
                                                    return;
                                                }
                                                if (trackedBounties.containsKey(number)) {
                                                    if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                        contents[x] = null;
                                                        change = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (change) {
                        event.getInventory().setContents(contents);
                    }
                    contents = event.getView().getPlayer().getInventory().getContents();
                    change = false;
                    for (int x = 0; x < contents.length; x++) {
                        if (contents[x] != null) {
                            if (contents[x].getType() == Material.COMPASS) {
                                if (contents[x].getItemMeta() != null) {
                                    if (Objects.requireNonNull(contents[x].getItemMeta()).getLore() != null) {
                                        if (!Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).isEmpty()) {
                                            String lastLine = Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).get(Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).size() - 1);
                                            if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                                int number;
                                                try {
                                                    number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                                } catch (NumberFormatException ignored) {
                                                    return;
                                                }
                                                if (trackedBounties.containsKey(number)) {
                                                    if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                        contents[x] = null;
                                                        change = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (change) {
                        event.getPlayer().getInventory().setContents(contents);
                    }
                }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // check if they are logged yet
        if (!loggedPlayers.containsValue(event.getPlayer().getUniqueId())) {
            // if not, add them
            loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId());
        } else {
            // if they are, check if their username has changed, and update it
            if (!loggedPlayers.containsKey(event.getPlayer().getName().toLowerCase(Locale.ROOT))) {
                String nameToRemove = "";
                for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                    if (entry.getValue().equals(event.getPlayer().getUniqueId())) {
                        nameToRemove = entry.getKey();
                    }
                }
                if (!Objects.equals(nameToRemove, "")) {
                    loggedPlayers.remove(nameToRemove);
                }
                loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId());
            }
        }

        if (hasBounty(event.getPlayer())) {
            Bounty bounty = getBounty(event.getPlayer());
            assert bounty != null;
            bounty.setDisplayName(event.getPlayer().getName());
            double addedAmount = 0;
            for (Setter setter : bounty.getSetters()) {
                if (!setter.isNotified()) {
                    event.getPlayer().sendMessage(parse(prefix + offlineBounty, setter.getName(), setter.getAmount(), event.getPlayer()));
                    setter.setNotified(true);
                    addedAmount += setter.getAmount();
                }
            }
            bounty.combineSetters();
            updateBounty(bounty);
            BigBounty.setBounty(event.getPlayer(), bounty, addedAmount);
            if (bounty.getTotalBounty() > BigBounty.getThreshold()) {
                displayParticle.add(event.getPlayer());
            }
            if (wanted && bounty.getTotalBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(event.getPlayer().getUniqueId())) {
                    NotBounties.wantedText.put(event.getPlayer().getUniqueId(), new AboveNameText(event.getPlayer()));
                }
            }
        }

        if (headRewards.containsKey(event.getPlayer().getUniqueId())) {
            for (RewardHead rewardHead : headRewards.get(event.getPlayer().getUniqueId())) {
                NumberFormatting.givePlayer(event.getPlayer(), rewardHead.getItem(), 1);
            }
            headRewards.remove(event.getPlayer().getUniqueId());
        }
        if (BountyExpire.removeExpiredBounties()) {
            Commands.reopenBountiesGUI();
        }

        // check for updates
        if (updateNotification && !NotBounties.latestVersion) {
            if (event.getPlayer().hasPermission("notbounties.admin")) {
                new UpdateChecker(NotBounties.getInstance(), 104484).getVersion(version -> {
                    if (NotBounties.getInstance().getDescription().getVersion().contains("dev"))
                        return;
                    if (NotBounties.getInstance().getDescription().getVersion().equals(version))
                        return;
                    event.getPlayer().sendMessage(parse(prefix, event.getPlayer()) + ChatColor.YELLOW + "A new update is available. Current version: " + ChatColor.GOLD + NotBounties.getInstance().getDescription().getVersion() + ChatColor.YELLOW + " Latest version: " + ChatColor.GREEN + version);
                    event.getPlayer().sendMessage(ChatColor.YELLOW + "Download a new version here:" + ChatColor.GRAY + " https://www.spigotmc.org/resources/104484/");
                    TextComponent prefixMsg = new TextComponent(parse(prefix, event.getPlayer()));
                    TextComponent click = new TextComponent(ChatColor.GOLD + "" + ChatColor.BOLD + "Click here ");
                    TextComponent msg = new TextComponent(ChatColor.YELLOW + "to disable this message in the future.");
                    click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_PURPLE + "Set update-notification to false")));
                    click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "bounty update-notification false"));
                    BaseComponent[] baseComponents = new BaseComponent[]{prefixMsg,click,msg};
                    event.getPlayer().spigot().sendMessage(baseComponents);
                });
            }
        }

        // check if they had a bounty refunded
        if (refundedBounties.containsKey(event.getPlayer().getUniqueId())) {
            if (NumberFormatting.manualEconomy != NumberFormatting.ManualEconomy.PARTIAL)
                NumberFormatting.doAddCommands(event.getPlayer(), refundedBounties.get(event.getPlayer().getUniqueId()));
            refundedBounties.remove(event.getPlayer().getUniqueId());
        }

        TimedBounties.login(event.getPlayer());
        Immunity.login(event.getPlayer());
        BountyExpire.login(event.getPlayer());
        if (SQL.isConnected())
            data.login(event.getPlayer());

        // remove persistent bounty entities in chunk
        if (wanted || !bountyBoards.isEmpty())
            RemovePersistentEntitiesEvent.cleanChunk(event.getPlayer().getLocation());
    }



    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // remove persistent entities (wanted tags & bounty boards)
        if (serverVersion <= 16)
            if (wanted || !bountyBoards.isEmpty())
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
        if (claimOrder != ClaimOrder.BEFORE || !(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (event.getDamage() >= player.getHealth() && player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && player.getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING){
            claimBounty(player, (Player) event.getDamager(), Arrays.asList(player.getInventory().getContents()), true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (claimOrder != ClaimOrder.AFTER)
            return;
        Player player = event.getPlayer();
        Player killer = player.getKiller();
        if (killer != null)
            claimBounty(player, killer, Arrays.asList(player.getInventory().getContents()), true);

        /*if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            Bukkit.getLogger().info("1");
            EntityDamageByEntityEvent event1 = (EntityDamageByEntityEvent) player.getLastDamageCause();
            if (!(event1.getDamager() instanceof Player))
                return;
            Bukkit.getLogger().info("2");
            Player killer1 = (Player) event1.getDamager();
            //claimBounty(player, killer1, Arrays.asList(player.getInventory().getContents()), true);

        }*/
    }

}

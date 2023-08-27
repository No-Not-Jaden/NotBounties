package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.NumberFormatting;
import me.jadenp.notbounties.utils.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.ConfigOptions.*;

public class Events implements Listener {
    private final NotBounties nb;

    private static Map<UUID, Map<UUID, Long>> playerKills = new HashMap<>();

    public Events(NotBounties nb) {
        this.nb = nb;
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (nb.immunePerms.contains(event.getPlayer().getUniqueId().toString())) {
            if (!event.getPlayer().hasPermission("notbounties.immune")) {
                nb.immunePerms.remove(event.getPlayer().getUniqueId().toString());
            }
        } else {
            if (event.getPlayer().hasPermission("notbounties.immune")) {
                nb.immunePerms.add(event.getPlayer().getUniqueId().toString());
            }
        }
        nb.displayParticle.remove(event.getPlayer());

    }

    public static void cleanPlayerKills(){
        Map<UUID, Map<UUID, Long>> updatedMap = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, Long>> entry : playerKills.entrySet()) {
            Map<UUID, Long> deaths = entry.getValue();
            deaths.entrySet().removeIf(entry1 -> entry1.getValue() < System.currentTimeMillis() - murderCooldown * 1000L);
            updatedMap.put(entry.getKey(), deaths);
        }
        updatedMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        playerKills = updatedMap;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        // check world filter
        if (worldFilter && !worldFilterNames.contains(event.getEntity().getWorld().getName()))
            return;
        if (!worldFilter && worldFilterNames.contains(event.getEntity().getWorld().getName()))
            return;
        Player player = (Player) event.getEntity();
        // check if we should increase the killer's bounty
        if (murderBountyIncrease > 0 && event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            if (!killer.hasMetadata("NPC")) { // don't raise bounty on a npc
                if (!playerKills.containsKey(killer.getUniqueId()) ||
                        !playerKills.get(killer.getUniqueId()).containsKey(player.getUniqueId()) ||
                        playerKills.get(killer.getUniqueId()).get(player.getUniqueId()) < System.currentTimeMillis() - murderCooldown * 1000L) {
                    if (!murderExcludeClaiming || !nb.hasBounty(player) || Objects.requireNonNull(nb.getBounty(player)).getTotalBounty(killer) < 0.01) {
                        // increase
                        NotBounties.getInstance().addBounty(killer, murderBountyIncrease, new ArrayList<>());
                        killer.sendMessage(parse(speakings.get(0) + speakings.get(59), player.getName(), Objects.requireNonNull(nb.getBounty(killer)).getTotalBounty(), killer));
                        Map<UUID, Long> kills = playerKills.containsKey(killer.getUniqueId()) ? playerKills.get(killer.getUniqueId()) : new HashMap<>();
                        kills.put(player.getUniqueId(), System.currentTimeMillis());
                        playerKills.put(killer.getUniqueId(), kills);
                    }
                }
            }
        }
        if (!nb.hasBounty(player) || event.getEntity().getKiller() == null || player == event.getEntity().getKiller())
            return;

        Player killer = event.getEntity().getKiller();

        // check if it is a npc
        if (!npcClaim && killer.hasMetadata("NPC"))
            return;
        Bounty bounty = nb.getBounty(player);
        assert bounty != null;
        // check if killer can claim it
        if (bounty.getTotalBounty(killer) < 0.01)
            return;
        List<Setter> claimedBounties = new ArrayList<>(bounty.getSetters());
        claimedBounties.removeIf(setter -> !setter.canClaim(killer));

        nb.displayParticle.remove(player);

        // broadcast message
        String message = parse(speakings.get(0) + speakings.get(7), player.getName(), killer.getName(), bounty.getTotalBounty(killer), player);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ((!nb.disableBroadcast.contains(p.getUniqueId().toString()) && bounty.getTotalBounty(killer) >= minBroadcast) || p.getUniqueId().equals(event.getEntity().getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId())) {
                p.sendMessage(message);
            }
        }

        // reward head
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setOwningPlayer(player);
        skull.setItemMeta(skullMeta);

        if (rewardHeadSetter) {
            for (Setter setter : claimedBounties) {
                if (!setter.getUuid().equals(new UUID(0, 0))) {
                    Player p = Bukkit.getPlayer(setter.getUuid());
                    if (p != null) {
                        if (!rewardHeadClaimed || !Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId().equals(setter.getUuid())) {
                            NumberFormatting.givePlayer(p, skull, 1);
                        }
                    } else {
                        if (nb.headRewards.containsKey(setter.getUuid().toString())) {
                            List<String> heads = new ArrayList<>(nb.headRewards.get(setter.getUuid().toString()));
                            heads.add(player.getUniqueId().toString());
                            nb.headRewards.replace(setter.getUuid().toString(), heads);
                        } else {
                            nb.headRewards.put(setter.getUuid().toString(), Collections.singletonList(player.getUniqueId().toString()));
                        }
                    }
                }
            }
        }
        if (rewardHeadClaimed) {
            NumberFormatting.givePlayer(killer, skull, 1);
        }
        // do commands
        if (deathTax > 0) {
            Map<Material, Long> removedItems = NumberFormatting.doRemoveCommands(player, bounty.getTotalBounty(killer) * deathTax, event.getDrops());
            if (removedItems.size() > 0) {
                // send message
                long totalLoss = 0;
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<Material, Long> entry : removedItems.entrySet()) {
                    builder.append(entry.getValue()).append("x").append(entry.getKey().toString()).append(", ");
                    totalLoss += entry.getValue();
                }
                builder.replace(builder.length() - 2, builder.length(), "");
                if (totalLoss > 0) {
                    player.sendMessage(parse(speakings.get(0) + speakings.get(64).replaceAll("\\{items}", Matcher.quoteReplacement(builder.toString())), player));
                    // modify drops
                    ListIterator<ItemStack> dropsIterator = event.getDrops().listIterator();
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
            NumberFormatting.doAddCommands(event.getEntity().getKiller(), bounty.getTotalBounty(killer));
        } else {
            // give voucher
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(parse(speakings.get(40), event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(killer), (Player) event.getEntity()));
            ArrayList<String> lore = new ArrayList<>();
            for (String str : voucherLore) {
                lore.add(parse(str, event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(killer), (Player) event.getEntity()));
            }
            lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + "" + ChatColor.UNDERLINE + "" + ChatColor.ITALIC + "@" + bounty.getTotalBounty(killer));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 0);
            NumberFormatting.givePlayer(event.getEntity().getKiller(), item, 1);
        }
        bounty.claimBounty(killer);
        nb.updateBounty(bounty);
        if (nb.SQL.isConnected()) {
            nb.data.addData(player.getUniqueId().toString(), 0, 0, 1, bounty.getTotalBounty(killer), 0, 0);
            nb.data.addData(killer.getUniqueId().toString(), 1, 0, 0, 0, 0, bounty.getTotalBounty(killer));
        } else {
            nb.allTimeBounties.put(player.getUniqueId().toString(), Leaderboard.ALL.getStat(player.getUniqueId()) + bounty.getTotalBounty(killer));
            nb.killBounties.put(killer.getUniqueId().toString(), Leaderboard.KILLS.getStat(killer.getUniqueId()) + 1);
            nb.deathBounties.put(player.getUniqueId().toString(), Leaderboard.DEATHS.getStat(player.getUniqueId()) + 1);
            nb.allClaimedBounties.put(player.getUniqueId().toString(), Leaderboard.CLAIMED.getStat(killer.getUniqueId()) + bounty.getTotalBounty(killer));
        }

        for (Setter setter : claimedBounties) {
            if (!setter.getUuid().equals(new UUID(0, 0))) {
                if (nb.SQL.isConnected()) {
                    nb.data.addData(setter.getUuid().toString(), 0, 1, 0, 0, 0, 0);
                } else {
                    nb.setBounties.put(setter.getUuid().toString(), Leaderboard.SET.getStat(setter.getUuid()) + 1);
                }
                Player p = Bukkit.getPlayer(setter.getUuid());
                if (p != null) {
                    p.playSound(p.getEyeLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1, 1);
                }
            }
        }
        nb.gracePeriod.put(event.getEntity().getUniqueId().toString(), System.currentTimeMillis());

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
                                            return;
                                        }
                                        if (nb.trackedBounties.containsKey(number)) {
                                            if (!nb.hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(nb.trackedBounties.get(number))))) {
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
                                            if (nb.trackedBounties.containsKey(number)) {
                                                if (nb.hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(nb.trackedBounties.get(number))))) {
                                                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();

                                                    String actionBar;
                                                    Bounty bounty = nb.getBounty(Bukkit.getOfflinePlayer(UUID.fromString(nb.trackedBounties.get(number))));
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

        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
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
                                        amount = Double.parseDouble(reward);
                                    } catch (NumberFormatException ignored) {
                                        player.sendMessage(ChatColor.RED + "Error redeeming reward");
                                        return;
                                    }
                                    NumberFormatting.doAddCommands(player, amount);
                                    player.getInventory().remove(item);
                                    player.sendMessage(parse(speakings.get(0) + speakings.get(41), amount, player));
                                }
                            }
                        }
                    }
                }
            }
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
                                    if (nb.trackedBounties.containsKey(number)) {
                                        if (!nb.hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(nb.trackedBounties.get(number))))) {
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
                                                if (nb.trackedBounties.containsKey(number)) {
                                                    if (!nb.hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(nb.trackedBounties.get(number))))) {
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
                                                if (nb.trackedBounties.containsKey(number)) {
                                                    if (!nb.hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(nb.trackedBounties.get(number))))) {
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
        if (!nb.loggedPlayers.containsValue(event.getPlayer().getUniqueId().toString())) {
            // if not, add them
            nb.loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId().toString());
        } else {
            // if they are, check if their username has changed, and update it
            if (!nb.loggedPlayers.containsKey(event.getPlayer().getName().toLowerCase(Locale.ROOT))) {
                String nameToRemove = "";
                for (Map.Entry<String, String> entry : nb.loggedPlayers.entrySet()) {
                    if (entry.getValue().equals(event.getPlayer().getUniqueId().toString())) {
                        nameToRemove = entry.getKey();
                    }
                }
                if (!Objects.equals(nameToRemove, "")) {
                    nb.loggedPlayers.remove(nameToRemove);
                }
                nb.loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId().toString());
            }
        }

        if (nb.hasBounty(event.getPlayer())) {
            Bounty bounty = nb.getBounty(event.getPlayer());
            assert bounty != null;
            bounty.setDisplayName(event.getPlayer().getName());
            double addedAmount = 0;
            for (Setter setter : bounty.getSetters()) {
                if (!setter.isNotified()) {
                    event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(12), setter.getName(), setter.getAmount(), event.getPlayer()));
                    setter.setNotified(true);
                    addedAmount += setter.getAmount();
                }
            }
            bounty.combineSetters();
            nb.updateBounty(bounty);
            if (bounty.getTotalBounty() - addedAmount < bBountyThreshold && bounty.getTotalBounty() > bBountyThreshold) {
                event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(43), event.getPlayer()));
                if (bBountyCommands != null && !bBountyCommands.isEmpty()) {
                    for (String command : bBountyCommands) {
                        while (command.contains("{player}")) {
                            command = command.replace("{player}", event.getPlayer().getName());
                        }
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }
            }
            if (bounty.getTotalBounty() > bBountyThreshold) {
                nb.displayParticle.add(event.getPlayer());
            }
        }

        if (nb.headRewards.containsKey(event.getPlayer().getUniqueId().toString())) {
            for (String uuid : nb.headRewards.get(event.getPlayer().getUniqueId().toString())) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                assert meta != null;
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
                skull.setItemMeta(meta);
                NumberFormatting.givePlayer(event.getPlayer(), skull, 1);
            }
            nb.headRewards.remove(event.getPlayer().getUniqueId().toString());
        }
        // if they have expire-time enabled
        if (bountyExpire > 0) {
            boolean change = false;
            // go through all the bounties and remove setters if it has been more than expire time
            ListIterator<Bounty> bountyIterator = nb.expiredBounties.listIterator();
            while (bountyIterator.hasNext()) {
                Bounty bounty = bountyIterator.next();
                ListIterator<Setter> setterIterator = bounty.getSetters().listIterator();
                while (setterIterator.hasNext()) {
                    Setter setter = setterIterator.next();
                    // check if past expire time
                    if (System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire) {
                        // refund money
                        NumberFormatting.doAddCommands(event.getPlayer(), setter.getAmount());
                        event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(31), bounty.getName(), setter.getAmount(), event.getPlayer()));
                        setterIterator.remove();
                    }
                }
                //bounty.getSetters().removeIf(setter -> System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire);
                // remove bounty if all the setters have been removed
                if (bounty.getSetters().size() == 0) {
                    bountyIterator.remove();
                    change = true;
                }
            }
            if (change) {
                Commands.reopenBountiesGUI();
            }
        }

        // check for updates
        if (updateNotification) {
            if (event.getPlayer().hasPermission("notbounties.admin")) {
                new UpdateChecker(nb, 104484).getVersion(version -> {

                    if (!nb.getDescription().getVersion().equals(version) && !nb.getDescription().getVersion().contains("dev")) {
                        event.getPlayer().sendMessage(parse(speakings.get(0), event.getPlayer()) + ChatColor.YELLOW + "A new update is available. Current version: " + ChatColor.GOLD + nb.getDescription().getVersion() + ChatColor.YELLOW + " Latest version: " + ChatColor.GREEN + version);
                        event.getPlayer().sendMessage(ChatColor.YELLOW + "Download a new version here:" + ChatColor.GRAY + " https://www.spigotmc.org/resources/104484/");

                    }
                });
            }
        }

    }

}

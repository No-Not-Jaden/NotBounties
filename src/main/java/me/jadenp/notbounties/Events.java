package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Events implements Listener {
    private final NotBounties nb;

    public Events(NotBounties nb) {
        this.nb = nb;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains(nb.speakings.get(35))) {
            event.setCancelled(true);

            int page = Integer.parseInt(event.getView().getTitle().substring(event.getView().getTitle().lastIndexOf(" ") + 1));
            page -= 1;
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null)
                return;
            if (clickedItem.getType().isAir())
                return;
            if (clickedItem.isSimilar(nb.item.get("exit"))) {
                event.getView().close();
            } else if (clickedItem.isSimilar(nb.item.get("back"))) {
                nb.openGUI((Player) event.getWhoClicked(), page - 1);
            } else if (clickedItem.isSimilar(nb.item.get("next"))) {
                nb.openGUI((Player) event.getWhoClicked(), page + 1);
            } else if (clickedItem.getType() == Material.PLAYER_HEAD) {

                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                assert meta != null;
                assert meta.getOwningPlayer() != null;
                Bounty bounty = nb.getBounty(meta.getOwningPlayer());
                if (bounty != null) {
                    if (event.isRightClick()) {
                        if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                            // edit
                            event.getView().close();
                            TextComponent first = new TextComponent(ChatColor.GOLD + "To edit " + bounty.getName() + "'s bounty");
                            TextComponent click = new TextComponent(ChatColor.YELLOW + "" + ChatColor.BOLD + " Click Here ");
                            TextComponent last = new TextComponent(ChatColor.GOLD + "and enter the new amount.");
                            click.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bounty edit " + bounty.getName() + " "));
                            first.addExtra(click);
                            first.addExtra(last);
                            event.getWhoClicked().spigot().sendMessage(first);
                        }
                    } else if (event.isLeftClick()) {
                        if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                            // remove
                            Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                            ItemStack[] contents = confirmation.getContents();
                            Arrays.fill(contents, nb.item.get("fill"));
                            contents[29] = nb.item.get("yes");
                            contents[33] = nb.item.get("no");
                            contents[40] = nb.item.get("exit");
                            contents[13] = clickedItem;
                            confirmation.setContents(contents);
                            event.getWhoClicked().openInventory(confirmation);

                        } else {
                            if (bounty.getUUID().equals(event.getWhoClicked().getUniqueId().toString())) {
                                if (nb.buyBack) {
                                    if (nb.usingPapi) {
                                        // check if papi is enabled - parse to check
                                        if (nb.papiEnabled) {
                                            double balance;
                                            try {
                                                balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) event.getWhoClicked(), nb.currency));
                                            } catch (NumberFormatException ignored) {
                                                Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                return;
                                            }
                                            if (balance >= (int) (bounty.getTotalBounty() * nb.buyBackInterest)) {
                                                Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                                                ItemStack[] contents = confirmation.getContents();
                                                Arrays.fill(contents, nb.item.get("fill"));
                                                contents[29] = nb.item.get("yes");
                                                contents[33] = nb.item.get("no");
                                                contents[40] = nb.item.get("exit");
                                                contents[13] = clickedItem;
                                                confirmation.setContents(contents);
                                                event.getWhoClicked().openInventory(confirmation);
                                            } else {
                                                event.getWhoClicked().sendMessage(nb.parse(nb.speakings.get(0) + nb.speakings.get(6), (int) (bounty.getTotalBounty() * nb.buyBackInterest), (Player) event.getWhoClicked()));
                                            }
                                        } else {
                                            Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                        }
                                    } else {
                                        if (nb.checkAmount((Player) event.getWhoClicked(), Material.valueOf(nb.currency)) >= (int) (bounty.getTotalBounty() * nb.buyBackInterest)) {
                                            Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                                            ItemStack[] contents = confirmation.getContents();
                                            Arrays.fill(contents, nb.item.get("fill"));
                                            contents[29] = nb.item.get("yes");
                                            contents[33] = nb.item.get("no");
                                            contents[40] = nb.item.get("exit");
                                            contents[13] = clickedItem;
                                            confirmation.setContents(contents);
                                            event.getWhoClicked().openInventory(confirmation);
                                        } else {
                                            event.getWhoClicked().sendMessage(nb.parse(nb.speakings.get(0) + nb.speakings.get(6), (int) (bounty.getTotalBounty() * nb.buyBackInterest), (Player) event.getWhoClicked()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                        nb.openGUI((Player) event.getWhoClicked(), page);
                        event.getWhoClicked().sendMessage(PlaceholderAPI.setPlaceholders(meta.getOwningPlayer(), nb.parse(nb.speakings.get(0) + nb.speakings.get(8), meta.getOwningPlayer().getName(), null)));
                    }
                }
            } else {
                for (String[] itemInfo : nb.layout) {
                    int commandIndex;
                    try {
                        commandIndex = Integer.parseInt(itemInfo[0]);
                    } catch (NumberFormatException ignored) {
                        //Bukkit.getLogger().info(itemInfo[0]);
                        continue;
                    }
                    if (itemInfo[1] != null) {
                        if (itemInfo[1].contains("-")) {
                            int num1;
                            try {
                                num1 = Integer.parseInt(itemInfo[1].substring(0, itemInfo[1].indexOf("-")));
                            } catch (NumberFormatException ignored) {
                                continue;
                            }
                            int num2;
                            try {
                                num2 = Integer.parseInt(itemInfo[1].substring(itemInfo[1].indexOf("-") + 1));
                            } catch (NumberFormatException ignored) {
                                continue;
                            }
                            for (int i = Math.min(num1, num2); i < Math.max(num1, num2) + 1; i++) {
                                if (event.getRawSlot() == i) {
                                    for (String cmd : nb.itemCommands.get(commandIndex)) {
                                        while (cmd.contains("{player}")) {
                                            cmd = cmd.replace("{player}", event.getWhoClicked().getName());
                                        }
                                        if (cmd.equalsIgnoreCase("[close]")) {
                                            event.getView().close();
                                        } else {
                                            if (cmd.contains("[p]")) {
                                                if (cmd.indexOf("[p]") == 0) {
                                                    cmd = cmd.substring(4);
                                                    Bukkit.dispatchCommand(event.getWhoClicked(), cmd);
                                                    continue;
                                                }
                                            }
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                                        }

                                    }
                                    return;
                                }
                            }
                        } else {
                            try {
                                //Bukkit.getLogger().info(event.getRawSlot() + ">" + itemInfo[1]);
                                if (event.getRawSlot() == Integer.parseInt(itemInfo[1])) {
                                    //Bukkit.getLogger().info("match");
                                    for (String cmd : nb.itemCommands.get(commandIndex)) {
                                        //Bukkit.getLogger().info(cmd);
                                        while (cmd.contains("{player}")) {
                                            cmd = cmd.replace("{player}", event.getWhoClicked().getName());
                                        }
                                        if (cmd.equalsIgnoreCase("[close]")) {
                                            event.getView().close();
                                        } else {
                                            if (cmd.contains("[p]")) {
                                                if (cmd.indexOf("[p]") == 0) {
                                                    cmd = cmd.substring(4);
                                                    Bukkit.dispatchCommand(event.getWhoClicked(), cmd);
                                                    continue;
                                                }
                                            }
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                                        }

                                    }
                                    return;
                                }
                            } catch (NumberFormatException ignored) {

                            }
                        }
                    }
                }
            }
        } else if (event.getView().getTitle().equals(ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?")) {
            ItemStack skull = event.getInventory().getContents()[13];
            event.setCancelled(true);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            assert meta != null;
            assert meta.getOwningPlayer() != null;
            Bounty bounty = nb.getBounty(meta.getOwningPlayer());
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null)
                return;
            if (clickedItem.getType().isAir())
                return;
            if (bounty != null) {
                if (clickedItem.isSimilar(nb.item.get("exit"))) {
                    event.getView().close();
                } else if (clickedItem.isSimilar(nb.item.get("no"))) {
                    nb.openGUI((Player) event.getWhoClicked(), 0);
                } else if (clickedItem.isSimilar(nb.item.get("yes"))) {
                    if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                        nb.bountyList.remove(bounty);
                        nb.openGUI((Player) event.getWhoClicked(), 0);
                        event.getWhoClicked().sendMessage(PlaceholderAPI.setPlaceholders(meta.getOwningPlayer(), nb.parse(nb.speakings.get(0) + nb.speakings.get(14), meta.getOwningPlayer().getName(), null)));
                    } else {
                        if (!nb.usingPapi) {
                            nb.removeItem((Player) event.getWhoClicked(), Material.valueOf(nb.currency), (int) (bounty.getTotalBounty() * nb.buyBackInterest));
                        }
                        nb.doRemoveCommands((Player) event.getWhoClicked(), (int) (bounty.getTotalBounty() * nb.buyBackInterest));
                        nb.bountyList.remove(bounty);
                        event.getWhoClicked().sendMessage(nb.parse(nb.speakings.get(0) + nb.speakings.get(14), event.getWhoClicked().getName(), (Player) event.getWhoClicked()));
                        nb.openGUI((Player) event.getWhoClicked(), 0);
                    }
                }
            } else {
                nb.openGUI((Player) event.getWhoClicked(), 0);
            }
        }
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

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            if (nb.hasBounty((Player) event.getEntity())) {
                if (event.getEntity().getKiller() != null) {
                    if (event.getEntity() != event.getEntity().getKiller()) {
                        Player killer = event.getEntity().getKiller();
                        Player player = (Player) event.getEntity();
                        Bounty bounty = nb.getBounty(player);
                        assert bounty != null;
                        String message = nb.parse(nb.speakings.get(0) + nb.speakings.get(7), killer.getName(), player.getName(), bounty.getTotalBounty(), player);
                        Bukkit.getConsoleSender().sendMessage(message);
                        for (Player p : Bukkit.getOnlinePlayers()){
                            if (!nb.disableBroadcast.contains(p.getUniqueId().toString()) || p.getUniqueId().equals(event.getEntity().getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId())){
                                p.sendMessage(message);
                            }
                        }
                        if (!nb.usingPapi) {
                            if (!nb.redeemRewardLater) {
                                nb.addItem(killer, Material.valueOf(nb.currency), bounty.getTotalBounty());
                            }
                        }
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        assert skullMeta != null;
                        skullMeta.setOwningPlayer(player);
                        skull.setItemMeta(skullMeta);

                        if (nb.rewardHeadSetter) {
                            for (Setter setter : bounty.getSetters()) {
                                if (!setter.getUuid().equalsIgnoreCase("CONSOLE")) {
                                    Player p = Bukkit.getPlayer(UUID.fromString(setter.getUuid()));
                                    if (p != null) {
                                        if (!nb.rewardHeadClaimed || !Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId().equals(UUID.fromString(setter.getUuid()))) {
                                            nb.addItem(p, skull);
                                        }
                                    } else {
                                        if (nb.headRewards.containsKey(setter.getUuid())) {
                                            List<String> heads = nb.headRewards.get(setter.getUuid());
                                            heads.add(player.getUniqueId().toString());
                                            nb.headRewards.replace(setter.getUuid(), heads);
                                        } else {
                                            nb.headRewards.put(setter.getUuid(), Collections.singletonList(player.getUniqueId().toString()));
                                        }
                                    }
                                }
                            }
                        }
                        if (nb.rewardHeadClaimed){
                                nb.addItem(Objects.requireNonNull(event.getEntity().getKiller()), skull);
                        }
                        // do commands
                        if (!nb.redeemRewardLater) {
                            nb.doAddCommands(event.getEntity().getKiller(), bounty.getTotalBounty());
                        } else {
                            // give voucher
                            ItemStack item = new ItemStack(Material.PAPER);
                            ItemMeta meta = item.getItemMeta();
                            assert meta != null;
                            meta.setDisplayName(nb.parse(nb.speakings.get(40), event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(), (Player) event.getEntity()));
                            ArrayList<String> lore = new ArrayList<>();
                            for (String str : nb.voucherLore){
                                lore.add(nb.parse(str,  event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(), (Player) event.getEntity()));
                            }
                            lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + "" + ChatColor.UNDERLINE + "" + ChatColor.ITALIC + "@" + bounty.getTotalBounty());
                            meta.setLore(lore);
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            item.setItemMeta(meta);
                            item.addUnsafeEnchantment(Enchantment.DURABILITY, 0);
                            nb.addItem(event.getEntity().getKiller(), item);
                        }
                        nb.bountyList.remove(bounty);
                        nb.allTimeBounty.put(player.getUniqueId().toString(), nb.allTimeBounty.get(player.getUniqueId().toString()) + bounty.getTotalBounty());
                        nb.bountiesClaimed.replace(killer.getUniqueId().toString(), nb.bountiesClaimed.get(killer.getUniqueId().toString()) + 1);
                        nb.bountiesReceived.replace(player.getUniqueId().toString(), nb.bountiesReceived.get(player.getUniqueId().toString()) + 1);
                        for (Setter setter : bounty.getSetters()) {
                            if (!setter.getUuid().equalsIgnoreCase("CONSOLE")) {
                            nb.bountiesSet.replace(setter.getUuid(), nb.bountiesSet.get(setter.getUuid()) + 1);
                                Player p = Bukkit.getPlayer(UUID.fromString(setter.getUuid()));
                                if (p != null) {
                                    p.playSound(p.getEyeLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1, 1);
                                }
                            }
                        }
                        if (nb.gracePeriod.containsKey(event.getEntity().getUniqueId().toString())) {
                            nb.gracePeriod.replace(event.getEntity().getUniqueId().toString(), System.currentTimeMillis());
                        } else {
                            nb.gracePeriod.put(event.getEntity().getUniqueId().toString(), System.currentTimeMillis());
                        }
                        if (nb.trackerRemove > 0)
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
                                if (p.getOpenInventory().getType() != InventoryType.CRAFTING) {
                                    if (p.getOpenInventory().getTitle().contains(nb.speakings.get(35))) {
                                        nb.openGUI(p, Integer.parseInt(p.getOpenInventory().getTitle().substring(p.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (nb.tracker)
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
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
                                                    Player p = Bukkit.getPlayer(UUID.fromString(bounty.getUUID()));
                                                    if (p != null) {
                                                        compassMeta.setLodestone(p.getLocation());
                                                        compassMeta.setLodestoneTracked(false);
                                                        if (nb.trackerGlow > 0) {
                                                            if (p.getWorld().equals(player.getWorld())) {
                                                                if (player.getLocation().distance(p.getLocation()) < nb.trackerGlow) {
                                                                    p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15, 0));
                                                                }
                                                            }
                                                        }
                                                        actionBar = ChatColor.DARK_GRAY + "|";
                                                        if (nb.TABPlayerName) {
                                                            actionBar += " " + ChatColor.YELLOW + p.getName() + ChatColor.DARK_GRAY + " |";
                                                        }
                                                        if (nb.TABDistance) {
                                                            if (p.getWorld().equals(player.getWorld())) {
                                                                actionBar += " " + ChatColor.GOLD + ((int) player.getLocation().distance(p.getLocation())) + "m" + ChatColor.DARK_GRAY + " |";
                                                            } else {
                                                                actionBar += " ?m |";
                                                            }
                                                        }
                                                        if (nb.TABPosition) {
                                                            actionBar += " " + ChatColor.RED + p.getLocation().getBlockX() + "x " + p.getLocation().getBlockY() + "y " + p.getLocation().getBlockZ() + "z" + ChatColor.DARK_GRAY + " |";
                                                        }
                                                        if (nb.TABWorld) {
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
                                                    if (nb.trackerActionBar) {
                                                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
                                                    }
                                                }
                                            }
                                        } else {
                                            if (nb.trackerActionBar) {
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
                                    int amount;
                                    try {
                                        amount = Integer.parseInt(reward);
                                    } catch (NumberFormatException ignored){
                                        player.sendMessage(ChatColor.RED + "Error redeeming reward");
                                        return;
                                    }
                                    if (!nb.usingPapi){
                                        nb.addItem(player, Material.valueOf(nb.currency), amount);
                                    }
                                    nb.doAddCommands(player, amount);
                                    player.getInventory().remove(item);
                                    player.updateInventory();
                                    player.sendMessage(nb.parse(nb.speakings.get(0) + nb.speakings.get(41), amount, player));
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
        if (nb.trackerRemove > 0) {
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
        if (nb.tracker)
            if (nb.trackerRemove == 3)
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
            int addedAmount = 0;
            for (Setter setter : bounty.getSetters()){
                if (!setter.isNotified()){
                    event.getPlayer().sendMessage(nb.parse(nb.speakings.get(0) + nb.speakings.get(12), setter.getName(), setter.getAmount(), event.getPlayer()));
                    setter.setNotified(true);
                    addedAmount+= setter.getAmount();
                }
            }
            bounty.combineSetters();
            if (bounty.getTotalBounty() - addedAmount < nb.bBountyThreshold && bounty.getTotalBounty() > nb.bBountyThreshold){
                event.getPlayer().sendMessage(nb.parse(nb.speakings.get(0) + nb.speakings.get(43), event.getPlayer()));
                if (nb.bBountyCommands != null && !nb.bBountyCommands.isEmpty()){
                    for (String command : nb.bBountyCommands){
                        while (command.contains("{player}")){
                            command = command.replace("{player}", event.getPlayer().getName());
                        }
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }
            }
        }

        if (!nb.bountiesClaimed.containsKey(event.getPlayer().getUniqueId().toString())) {
            nb.bountiesClaimed.put(event.getPlayer().getUniqueId().toString(), 0);
            nb.bountiesSet.put(event.getPlayer().getUniqueId().toString(), 0);
            nb.bountiesReceived.put(event.getPlayer().getUniqueId().toString(), 0);
            nb.allTimeBounty.put(event.getPlayer().getUniqueId().toString(), 0);
            nb.immunitySpent.put(event.getPlayer().getUniqueId().toString(), 0);
        }

        if (nb.headRewards.containsKey(event.getPlayer().getUniqueId().toString())) {
            for (String uuid : nb.headRewards.get(event.getPlayer().getUniqueId().toString())) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                assert meta != null;
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
                skull.setItemMeta(meta);
                nb.addItem(event.getPlayer(), skull);
            }
            nb.headRewards.remove(event.getPlayer().getUniqueId().toString());
        }
        // if they have expire-time enabled
        if (nb.bountyExpire > 0) {
            boolean change = false;
            // go through all the bounties and remove setters if it has been more than expire time
            ListIterator<Bounty> bountyIterator = nb.expiredBounties.listIterator();
            while (bountyIterator.hasNext()) {
                Bounty bounty = bountyIterator.next();
                ListIterator<Setter> setterIterator = bounty.getSetters().listIterator();
                while (setterIterator.hasNext()) {
                    Setter setter = setterIterator.next();
                    // check if past expire time
                    if (System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * nb.bountyExpire) {
                        // refund money
                        if (!nb.usingPapi) {
                            nb.addItem(event.getPlayer(), Material.valueOf(nb.currency), setter.getAmount());
                        }
                        nb.doAddCommands(event.getPlayer(), setter.getAmount());
                        event.getPlayer().sendMessage(nb.parse(nb.speakings.get(0) + nb.speakings.get(31), bounty.getName(), setter.getAmount(), event.getPlayer()));
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
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                        if (player.getOpenInventory().getTitle().contains(nb.speakings.get(35))) {
                            nb.openGUI(player, Integer.parseInt(player.getOpenInventory().getTitle().substring(player.getOpenInventory().getTitle().lastIndexOf(" ") + 1)));
                        }
                    }
                }
            }
        }

        // check for updates
        if (event.getPlayer().hasPermission("notbounties.admin")) {
            new UpdateChecker(nb, 104484).getVersion(version -> {

                if (!nb.getDescription().getVersion().equals(version) && !nb.getDescription().getVersion().contains("dev_")) {
                    event.getPlayer().sendMessage(nb.parse(nb.speakings.get(0), event.getPlayer()) + ChatColor.YELLOW + "A new update is available. Current version: " + ChatColor.GOLD + nb.getDescription().getVersion() + ChatColor.YELLOW + " Latest version: " + ChatColor.GREEN + version);
                    event.getPlayer().sendMessage(ChatColor.YELLOW + "Download a new version here:" + ChatColor.GRAY + " https://www.spigotmc.org/resources/104484/");

                }
            });
        }

    }
}

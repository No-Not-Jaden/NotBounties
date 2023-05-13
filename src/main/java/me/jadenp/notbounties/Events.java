package me.jadenp.notbounties;

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

import static me.jadenp.notbounties.ConfigOptions.*;

public class Events implements Listener {
    private final NotBounties nb;

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

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            if (nb.hasBounty((Player) event.getEntity())) {
                if (event.getEntity().getKiller() != null) {
                    if (event.getEntity() != event.getEntity().getKiller()) {
                        Player killer = event.getEntity().getKiller();
                        Player player = (Player) event.getEntity();
                        Bounty bounty = nb.getBounty(player);
                        nb.displayParticle.remove(player);
                        assert bounty != null;
                        String message = parse(speakings.get(0) + speakings.get(7), killer.getName(), player.getName(), bounty.getTotalBounty(), player);
                        Bukkit.getConsoleSender().sendMessage(message);
                        for (Player p : Bukkit.getOnlinePlayers()){
                            if (!nb.disableBroadcast.contains(p.getUniqueId().toString()) || p.getUniqueId().equals(event.getEntity().getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId())){
                                p.sendMessage(message);
                            }
                        }
                        if (!usingPapi) {
                            if (!redeemRewardLater) {
                                nb.addItem(killer, Material.valueOf(currency), bounty.getTotalBounty());
                            }
                        }
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        assert skullMeta != null;
                        skullMeta.setOwningPlayer(player);
                        skull.setItemMeta(skullMeta);

                        if (rewardHeadSetter) {
                            for (Setter setter : bounty.getSetters()) {
                                if (!setter.getUuid().equalsIgnoreCase("CONSOLE")) {
                                    Player p = Bukkit.getPlayer(UUID.fromString(setter.getUuid()));
                                    if (p != null) {
                                        if (!rewardHeadClaimed || !Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId().equals(UUID.fromString(setter.getUuid()))) {
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
                        if (rewardHeadClaimed){
                                nb.addItem(Objects.requireNonNull(event.getEntity().getKiller()), skull);
                        }
                        // do commands
                        if (!redeemRewardLater) {
                            nb.doAddCommands(event.getEntity().getKiller(), bounty.getTotalBounty());
                        } else {
                            // give voucher
                            ItemStack item = new ItemStack(Material.PAPER);
                            ItemMeta meta = item.getItemMeta();
                            assert meta != null;
                            meta.setDisplayName(parse(speakings.get(40), event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(), (Player) event.getEntity()));
                            ArrayList<String> lore = new ArrayList<>();
                            for (String str : voucherLore){
                                lore.add(parse(str,  event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(), (Player) event.getEntity()));
                            }
                            lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + "" + ChatColor.UNDERLINE + "" + ChatColor.ITALIC + "@" + bounty.getTotalBounty());
                            meta.setLore(lore);
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            item.setItemMeta(meta);
                            item.addUnsafeEnchantment(Enchantment.DURABILITY, 0);
                            nb.addItem(event.getEntity().getKiller(), item);
                        }
                        if (nb.SQL.isConnected()){
                            nb.data.addData(player.getUniqueId().toString(), 0, 0, 1, bounty.getTotalBounty(), 0, 0);
                            nb.data.addData(killer.getUniqueId().toString(), 1,0,0,0,0, bounty.getTotalBounty());
                            nb.data.removeBounty(bounty.getUUID());
                        } else {
                            nb.bountyList.remove(bounty);
                            nb.allTimeBounty.put(player.getUniqueId().toString(), nb.allTimeBounty.get(player.getUniqueId().toString()) + bounty.getTotalBounty());
                            nb.bountiesClaimed.replace(killer.getUniqueId().toString(), nb.bountiesClaimed.get(killer.getUniqueId().toString()) + 1);
                            nb.bountiesReceived.replace(player.getUniqueId().toString(), nb.bountiesReceived.get(player.getUniqueId().toString()) + 1);
                            nb.allClaimed.replace(player.getUniqueId().toString(), nb.allClaimed.get(player.getUniqueId().toString()) + bounty.getTotalBounty());
                        }

                        for (Setter setter : bounty.getSetters()) {
                            if (!setter.getUuid().equalsIgnoreCase("CONSOLE")) {
                                if (nb.SQL.isConnected()) {
                                    nb.data.addData(setter.getUuid(), 0, 1, 0, 0, 0, 0);
                                } else {
                                    nb.bountiesSet.replace(setter.getUuid(), nb.bountiesSet.get(setter.getUuid()) + 1);
                                }
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
                }
            }
        }
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
                                                    Player p = Bukkit.getPlayer(UUID.fromString(bounty.getUUID()));
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
                                    int amount;
                                    try {
                                        amount = Integer.parseInt(reward);
                                    } catch (NumberFormatException ignored){
                                        player.sendMessage(ChatColor.RED + "Error redeeming reward");
                                        return;
                                    }
                                    if (!usingPapi){
                                        nb.addItem(player, Material.valueOf(currency), amount);
                                    }
                                    nb.doAddCommands(player, amount);
                                    player.getInventory().remove(item);
                                    player.updateInventory();
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
            int addedAmount = 0;
            for (Setter setter : bounty.getSetters()){
                if (!setter.isNotified()){
                    event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(12), setter.getName(), setter.getAmount(), event.getPlayer()));
                    setter.setNotified(true);
                    addedAmount+= setter.getAmount();
                }
            }
            bounty.combineSetters();
            if (bounty.getTotalBounty() - addedAmount < bBountyThreshold && bounty.getTotalBounty() > bBountyThreshold){
                event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(43), event.getPlayer()));
                if (bBountyCommands != null && !bBountyCommands.isEmpty()){
                    for (String command : bBountyCommands){
                        while (command.contains("{player}")){
                            command = command.replace("{player}", event.getPlayer().getName());
                        }
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }
            }
            if (bounty.getTotalBounty() > bBountyThreshold){
                nb.displayParticle.add(event.getPlayer());
            }
        }

        if (!nb.bountiesClaimed.containsKey(event.getPlayer().getUniqueId().toString())) {
            nb.bountiesClaimed.put(event.getPlayer().getUniqueId().toString(), 0);
            nb.allClaimed.put(event.getPlayer().getUniqueId().toString(), 0);
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
                        if (!usingPapi) {
                            nb.addItem(event.getPlayer(), Material.valueOf(currency), setter.getAmount());
                        }
                        nb.doAddCommands(event.getPlayer(), setter.getAmount());
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

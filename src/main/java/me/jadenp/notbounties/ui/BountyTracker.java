package me.jadenp.notbounties.ui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.map.BountyMap;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.BountyManager.hasBounty;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class BountyTracker implements Listener {
    private static boolean tracker;
    private static boolean giveOwnTracker;
    private static int trackerRemove;
    private static int trackerGlow;
    private static boolean trackerActionBar;
    private static boolean TABShowAlways;
    private static boolean TABPlayerName;
    private static boolean TABDistance;
    private static boolean TABPosition;
    private static boolean TABWorld;
    private static boolean writeEmptyTrackers;
    private static boolean washTrackers;
    private static boolean posterTracking;
    private static boolean craftTracker;
    private static boolean resetRemovedTrackers;
    private static double minBounty;

    private static long lastInventorySearch = 0;
    private static BiMap<Integer, UUID> trackedBounties = HashBiMap.create();
    private static NamespacedKey bountyTrackerRecipe;

    public static void loadConfiguration(ConfigurationSection configuration) {
        // general tracker settings
        tracker = configuration.getBoolean("enabled");
        giveOwnTracker = configuration.getBoolean("give-own");
        trackerRemove = configuration.getInt("remove");
        trackerGlow = configuration.getInt("glow");
        trackerActionBar = configuration.getBoolean("action-bar.enabled");
        writeEmptyTrackers = configuration.getBoolean("write-empty-trackers");
        washTrackers = configuration.getBoolean("wash-trackers");
        posterTracking = configuration.getBoolean("poster-tracking");
        resetRemovedTrackers = configuration.getBoolean("reset-removed-trackers");
        craftTracker = configuration.getBoolean("craft-tracker");
        minBounty = configuration.getDouble("minimum-bounty");

        // tracker action bar settings
        TABShowAlways = configuration.getBoolean("action-bar.show-always");
        TABPlayerName = configuration.getBoolean("action-bar.player-name");
        TABDistance = configuration.getBoolean("action-bar.distance");
        TABPosition = configuration.getBoolean("action-bar.position");
        TABWorld = configuration.getBoolean("action-bar.world");

        registerRecipies();
    }

    public static boolean isEnabled() {
        return tracker;
    }

    public static NamespacedKey getBountyTrackerRecipe() {
        return bountyTrackerRecipe;
    }

    private static void registerRecipies(){
        bountyTrackerRecipe = new NamespacedKey(NotBounties.getInstance(),"bounty_tracker");
        Bukkit.removeRecipe(bountyTrackerRecipe);
        ShapedRecipe bountyTrackerCraftingPattern = new ShapedRecipe(
                bountyTrackerRecipe,
                getEmptyTracker()
        );
        bountyTrackerCraftingPattern.shape(" AS", "ACA", "AA ");
        if (NotBounties.serverVersion >= 18)
            bountyTrackerCraftingPattern.setIngredient('S', Material.SPYGLASS);
        else
            bountyTrackerCraftingPattern.setIngredient('S', Material.TRIPWIRE_HOOK);
        if (NotBounties.serverVersion >= 17)
            bountyTrackerCraftingPattern.setIngredient('A', Material.AMETHYST_SHARD);
        else
            bountyTrackerCraftingPattern.setIngredient('A', Material.PAPER);
        bountyTrackerCraftingPattern.setIngredient('C', Material.COMPASS);
        Bukkit.addRecipe(bountyTrackerCraftingPattern);
    }

    public static ItemStack getEmptyTracker() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(LanguageOptions.parse(LanguageOptions.emptyTrackerName, null));
        List<String> lore = new ArrayList<>();
        LanguageOptions.emptyTrackerLore.forEach(str -> lore.add(LanguageOptions.parse(str, null)));
        lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@-1");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getTracker(UUID uuid) {
        if (!BountyManager.hasBounty(uuid))
            return getEmptyTracker();
        String playerName = NotBounties.getPlayerName(uuid);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = compass.getItemMeta();
        assert meta != null;
        meta.setDisplayName(parse(bountyTrackerName, playerName, player));

        ArrayList<String> lore = trackerLore.stream().map(str -> parse(str, playerName, player)).collect(Collectors.toCollection(ArrayList::new));

        int id = registerTrackedUUID(uuid);

        lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + id);
        meta.setLore(lore);
        if (NotBounties.isAboveVersion(20, 4)) {
            if (!meta.hasEnchantmentGlintOverride())
                meta.setEnchantmentGlintOverride(true);
        } else {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            compass.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
        }
        compass.setItemMeta(meta);
        return compass;
    }

    /**
     * Register a UUID with trackedBounties.
     * If the UUID already exists in the map, the ID will be returned. Otherwise, a new ID is created and returned;
     * @param uuid UUID of the tracked player
     * @return The associated ID of the tracked bounty.
     */
    private static int registerTrackedUUID(UUID uuid) {
        // check if the uuid already exists in the map
        if (trackedBounties.containsValue(uuid))
            return trackedBounties.inverse().get(uuid);
        // get the next usable ID
        int id = 0;
        while (trackedBounties.containsKey(id))
            id++;

        trackedBounties.put(id, uuid); // register to map
        return id; // return found id
    }

    /**
     * Get the tracker ID from a compass ItemStack
     * @param itemStack ItemStack to parse
     * @return The tracker id located at the end of the item's lore. An ID of -404 will be returned if the ItemStack isn't a tracker.
     */
    public static int getTrackerID(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.COMPASS)
            return -404;
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty() || !(meta instanceof CompassMeta))
            return -404;

        String lastLine = lore.get(lore.size()-1);
        if (!lastLine.startsWith(ChatColor.BLACK + "" + ChatColor.ITALIC + ChatColor.UNDERLINE + ChatColor.STRIKETHROUGH + "@"))
            return -404;
        lastLine = lastLine.substring(lastLine.indexOf("@") + 1);
        try {
            return Integer.parseInt(lastLine);
        } catch (NumberFormatException e) {
            return -404;
        }
    }

    public static boolean isGiveOwnTracker() {
        return giveOwnTracker;
    }

    public static boolean isWriteEmptyTrackers() {
        return writeEmptyTrackers;
    }

    public static double getMinBounty() {
        return minBounty;
    }

    public static void stopTracking(UUID uuid) {
        trackedBounties.inverse().remove(uuid);
    }

    /**
     * Uses the config options to remove trackers from the player
     * @param player Player to remove the tracker from
     */
    public static void removeTracker(Player player) {
        removeTracker(player.getInventory());
    }

    /**
     * Uses the config options to remove trackers from an inventory
     * @param inventory Inventory to check for trackers
     */
    public static void removeTracker(Inventory inventory) {
        if (trackerRemove <= 0)
            return;
        boolean update = false;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                int id = getTrackerID(contents[i]);
                if (id != -404 && id != -1 && !trackedBounties.containsKey(id)) {
                    if (resetRemovedTrackers) {
                        ItemStack emptyTracker = getEmptyTracker().clone();
                        emptyTracker.setAmount(contents[i].getAmount());
                        contents[i] = emptyTracker;
                    } else {
                        contents[i] = null;
                    }
                    update = true;
                }
            }
        }
        if (update) { // only update if there was a change to the inventory
            inventory.setContents(contents);
        }
    }

    /**
     * Removes empty trackers from a player's inventory
     * @param player Player to search for trackers
     * @param limitOne Whether only 1 empty tracker should be removed
     */
    public static void removeEmptyTracker(Player player, boolean limitOne) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                int id = getTrackerID(contents[i]);
                if (id == -1) {
                    if (contents[i].getAmount() > 1 && limitOne) {
                        contents[i] = new ItemStack(contents[i].getType(), contents[i].getAmount() - 1);
                        break;
                    } else {
                        contents[i] = null;
                        if (limitOne)
                            break;
                    }
                }
            }
        }
        player.getInventory().setContents(contents);
    }

    public static void update() {
        // tracker inventory search
        if (System.currentTimeMillis() - lastInventorySearch > 5 * 60 * 1000) { // 5 min
            lastInventorySearch = System.currentTimeMillis();
            if (trackerRemove > 1) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    removeTracker(player);
                }
            }
        }

        if (!tracker)
            return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            int id = getTrackerID(item);
            if (id == -404)
                continue;
            updateHeldTracker(player, item, id, false);
        }
    }

    private static void updateHeldTracker(Player player, ItemStack item, int id, boolean force) {
        if (item.getType() != Material.COMPASS)
            return;
        if (!trackedBounties.containsKey(id) && trackerRemove > 0 && id != -1) {
            // invalid tracker
            removeTracker(player);
            return;
        }
        CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
        assert compassMeta != null;
        Location previousLocation = compassMeta.hasLodestone() ? compassMeta.getLodestone() : null;
        if (!player.hasPermission("notbounties.tracker") || !trackedBounties.containsKey(id)) {
            // no permission or empty tracker - track other world for funky compass movements
            if (Bukkit.getWorlds().size() > 1) {
                for (World world : Bukkit.getWorlds()) {
                    if (!world.equals(player.getWorld())) {
                        if (compassMeta.isLodestoneTracked())
                            compassMeta.setLodestoneTracked(false);
                        compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), 0, world.getSpawnLocation().getZ()));
                        break;
                    }
                }
            } else {
                // only one world - set to tracking a lodestone at spawn (will only point to lodestone if there is one present)
                compassMeta.setLodestoneTracked(true);
                World world = player.getWorld();
                compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), 0, world.getSpawnLocation().getZ()));
            }
            if (id != -1) // not an empty tracker
                if (trackerActionBar && (TABShowAlways || force)) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(LanguageOptions.parse(trackerNoPermission, player)));
                }

            if (previousLocation == null || !Objects.equals(previousLocation.getWorld(), Objects.requireNonNull(compassMeta.getLodestone()).getWorld())) {
                // only update if location has changed worlds
                compassMeta.setLodestoneTracked(false);
                item.setItemMeta(compassMeta);
            }
            return;
        }

        UUID trackedUUID = trackedBounties.get(id);
        if (!hasBounty(trackedUUID))
            // no bounty
            return;
        OfflinePlayer trackedPlayer = Bukkit.getOfflinePlayer(trackedUUID);
        if (trackedPlayer.isOnline() && (NotBounties.serverVersion >= 17 && player.canSee(Objects.requireNonNull(trackedPlayer.getPlayer()))) && !isVanished(trackedPlayer.getPlayer())) {
            // can track player
            Player p = trackedPlayer.getPlayer();
            if (!compassMeta.hasLodestone() || compassMeta.getLodestone() == null) {
                compassMeta.setLodestone(p.getLocation().getBlock().getLocation());
            } else if (Objects.equals(compassMeta.getLodestone().getWorld(), p.getWorld())) {
                if (compassMeta.getLodestone().distance(p.getLocation()) > 2) {
                    compassMeta.setLodestone(p.getLocation().getBlock().getLocation());
                }
            } else {
                compassMeta.setLodestone(p.getLocation().getBlock().getLocation());
            }


            if (trackerGlow > 0) {
                if (p.getWorld().equals(player.getWorld())) {
                    if (player.getLocation().distance(p.getLocation()) < trackerGlow) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 45, 0));
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(parse(trackedNotify, p)));
                    }
                }
            }

            // build actionbar
            if (trackerActionBar && (TABShowAlways || force)) {
                StringBuilder actionBar = new StringBuilder(ChatColor.DARK_GRAY + "|");
                if (TABPlayerName)
                    actionBar.append(" " + ChatColor.YELLOW).append(p.getName()).append(ChatColor.DARK_GRAY).append(" |");
                if (TABDistance)
                    if (p.getWorld().equals(player.getWorld())) {
                        actionBar.append(" " + ChatColor.GOLD).append((int) player.getLocation().distance(p.getLocation())).append("m").append(ChatColor.DARK_GRAY).append(" |");
                    } else {
                        actionBar.append(" ?m |");
                    }
                if (TABPosition)
                    actionBar.append(" " + ChatColor.RED).append(p.getLocation().getBlockX()).append("x ").append(p.getLocation().getBlockY()).append("y ").append(p.getLocation().getBlockZ()).append("z").append(ChatColor.DARK_GRAY).append(" |");
                if (TABWorld)
                    actionBar.append(" " + ChatColor.LIGHT_PURPLE).append(p.getWorld().getName()).append(ChatColor.DARK_GRAY).append(" |");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar.toString()));
            }
            if (previousLocation == null || !Objects.equals(previousLocation.getWorld(), Objects.requireNonNull(compassMeta.getLodestone()).getWorld()) || previousLocation.distance(compassMeta.getLodestone()) > 2) {
                // only update if location is greater than 2 blocks away
                compassMeta.setLodestoneTracked(false);
                item.setItemMeta(compassMeta);
            }
        } else {
            // player offline -
            if (trackerActionBar && (TABShowAlways || force)) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(LanguageOptions.parse(trackerOffline, player)));
            }
            if (Bukkit.getWorlds().size() > 1) {
                for (World world : Bukkit.getWorlds()) {
                    if (!world.equals(player.getWorld())) {
                        compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), 0, world.getSpawnLocation().getZ()));
                        compassMeta.setLodestoneTracked(false);
                        break;
                    }
                }
            } else {
                // only one world - set to tracking a lodestone at spawn (will only point to lodestone if there is one present)
                World world = player.getWorld();
                compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), 0, world.getSpawnLocation().getZ()));
                compassMeta.setLodestoneTracked(true);
            }
            if (previousLocation == null || !Objects.equals(previousLocation.getWorld(), Objects.requireNonNull(compassMeta.getLodestone()).getWorld()))
                // only update if location has changed worlds
                item.setItemMeta(compassMeta);
        }
    }

    public static BiMap<Integer, UUID> getTrackedBounties() {
        return trackedBounties;
    }

    public static void setTrackedBounties(BiMap<Integer, UUID> trackedBounties) {
        BountyTracker.trackedBounties = trackedBounties;
    }

    // update tracking manually
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!tracker)
            return;
        if (event.getItem() == null || !(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
            return;
        Player player = event.getPlayer();
        int id = getTrackerID(player.getInventory().getItemInMainHand());
        if (id == -404 || id == -1)
            // not a tracker or an empty tracker
            return;
        updateHeldTracker(player, player.getInventory().getItemInMainHand(), id, true);
    }

    // remove tracker if holding
    @EventHandler
    public void onHold(PlayerItemHeldEvent event) {
        if (trackerRemove <= 0)
            return;
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item == null)
            return;

        int id = getTrackerID(item);
        if (id == -404 || id == -1 || trackedBounties.containsKey(id))
            // not a tracker, empty tracker, or valid tracker
            return;
        removeTracker(event.getPlayer());
    }

    // remove trackers in container
    @EventHandler
    public void onOpenInv(InventoryOpenEvent event) {
        if (trackerRemove != 3 || event.getInventory().getType() == InventoryType.CRAFTING)
            return;
        removeTracker(event.getInventory());
    }

    // poster tracking
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!posterTracking || !(event.getRightClicked().getType() == EntityType.ITEM_FRAME || (serverVersion >= 17 && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME)))
            return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        int id = getTrackerID(item);
        if (id != -1)
            // not an empty tracker
            return;
        ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
        if (itemFrame.getItem().getType() != Material.FILLED_MAP)
            // not a map
            return;
        ItemStack frameItem = itemFrame.getItem();
        MapMeta mapMeta = (MapMeta) frameItem.getItemMeta();
        assert mapMeta != null;
        MapView mapView = mapMeta.getMapView();
        if (mapView == null || !BountyMap.mapIDs.containsKey(mapView.getId()))
            return;
        event.setCancelled(true);
        UUID posterPlayerUUID = BountyMap.mapIDs.get(mapView.getId());
        if (!hasBounty(posterPlayerUUID))
            return;
        removeEmptyTracker(event.getPlayer(), true); // remove one empty tracker
        NumberFormatting.givePlayer(event.getPlayer(), getTracker(posterPlayerUUID), 1); // give tracker
        // you have been given
        event.getPlayer().sendMessage(parse(prefix + trackerReceive, event.getPlayer().getName(), event.getPlayer()));
    }

    // check for tracking crafting
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        // may have to cancel event or set result to null instead of returning or may have to listen to the craft event
        if (!tracker || !craftTracker)
            return;
        ItemStack[] matrix = event.getInventory().getMatrix();
        boolean hasEmptyTracker = false;
        ItemStack head = null;
        for (ItemStack itemStack : matrix) {
            if (itemStack == null)
                continue;
            if (getTrackerID(itemStack) == -1) {
                if (!hasEmptyTracker) {
                    hasEmptyTracker = true;
                } else {
                    // already an empty tracker in previous slot
                    return;
                }
            }
            if (itemStack.getType() == Material.PLAYER_HEAD) {
                if (head == null) {
                    head = itemStack;
                } else {
                    // already a head in previous slot
                    return;
                }
            }
        }
        if (head == null || !hasEmptyTracker)
            // not enough requirements
            return;
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        if (meta.getOwningPlayer() == null) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Player head in crafting matrix has a null owner!");
            return;
        }
        UUID trackedPlayer = meta.getOwningPlayer().getUniqueId();
        if (!hasBounty(trackedPlayer))
            return;
        ItemStack tracker = getTracker(trackedPlayer);
        event.getInventory().setResult(tracker);
    }
    // complete tracker crafting
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!tracker || !craftTracker || !(event.getInventory() instanceof CraftingInventory))
            return;
        CraftingInventory inventory = (CraftingInventory) event.getInventory();
        int id = getTrackerID(inventory.getResult());
        if (id < 0)
            return;
        if (event.getRawSlot() == 0) {
            event.setCancelled(true);
        } else {
            return;
        }
        // update result
        int amountCrafted = 1;
        switch (event.getAction()){
            case PICKUP_ONE:
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_SOME:
                new BukkitRunnable() {
                    final int previousAmount = event.getCursor() != null ? event.getCursor().getAmount() : 0;
                    final ItemStack result = inventory.getResult();
                    @Override
                    public void run() {
                        assert result != null;
                        result.setAmount(previousAmount + 1);
                        event.getWhoClicked().getOpenInventory().setCursor(result);
                    }
                }.runTaskLater(NotBounties.getInstance(), 1);
                break;
            case DROP_ALL_SLOT:
            case DROP_ALL_CURSOR:
            case DROP_ONE_CURSOR:
            case DROP_ONE_SLOT:
                event.getWhoClicked().getWorld().dropItem(event.getWhoClicked().getEyeLocation(), inventory.getResult());
                break;
            case MOVE_TO_OTHER_INVENTORY:
                int minAmount = 65;
                ItemStack[] matrix = inventory.getMatrix();
                for (ItemStack itemStack : matrix) {
                    if (itemStack != null) {
                        if (itemStack.getAmount() < minAmount) {
                            minAmount = itemStack.getAmount();
                        }
                    }
                }
                NumberFormatting.givePlayer((Player) event.getWhoClicked(), inventory.getResult(), minAmount);
                amountCrafted = minAmount;
                break;
            case HOTBAR_MOVE_AND_READD:
            case HOTBAR_SWAP:
                // hit a number button to move to a hotbar slot - slot must be empty
                if (event.getWhoClicked().getOpenInventory().getBottomInventory().getItem(event.getHotbarButton()) == null)
                    event.getWhoClicked().getOpenInventory().getBottomInventory().setItem(event.getHotbarButton(), inventory.getResult());
                break;
            default:
                return;
        }
        // update matrix
        boolean changed = false;
        ItemStack[] matrix = inventory.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null) {
                if (matrix[i].getAmount() > amountCrafted) {
                    matrix[i].setAmount(matrix[i].getAmount() - amountCrafted);
                } else {
                    matrix[i] = null;
                    changed = true;
                }
            }
        }
        inventory.setMatrix(matrix);
        if (changed)
            inventory.setResult(null);

    }


    // wash trackers
    @EventHandler
    public void onPlayerItemDrop(PlayerDropItemEvent event) {
        if (!tracker || !washTrackers)
            return;
        int id = getTrackerID(event.getItemDrop().getItemStack());
        if (id == -404 || id == -1)
            // not a tracker or empty tracker
            return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!event.getItemDrop().isValid())
                    return;
                if ((NotBounties.serverVersion < 17 && event.getItemDrop().getLocation().getBlock().getType() == Material.CAULDRON) || (NotBounties.serverVersion >= 17 && event.getItemDrop().getLocation().getBlock().getType() == Material.WATER_CAULDRON)) {
                    ItemStack emptyTracker = getEmptyTracker().clone();
                    emptyTracker.setAmount(event.getItemDrop().getItemStack().getAmount());
                    event.getItemDrop().getWorld().dropItem(event.getItemDrop().getLocation(), emptyTracker);
                    event.getItemDrop().remove();
                }
            }
        }.runTaskLater(NotBounties.getInstance(), 40);
    }

}
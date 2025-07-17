package me.jadenp.notbounties.features.settings.display;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.BountyManager.hasBounty;
import static me.jadenp.notbounties.features.LanguageOptions.*;

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
    private static int alert;

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
        alert = configuration.getInt("alert");

        // tracker action bar settings
        TABShowAlways = configuration.getBoolean("action-bar.show-always");
        TABPlayerName = configuration.getBoolean("action-bar.player-name");
        TABDistance = configuration.getBoolean("action-bar.distance");
        TABPosition = configuration.getBoolean("action-bar.position");
        TABWorld = configuration.getBoolean("action-bar.world");

        try {
            registerRecipes();
        } catch (UnsupportedOperationException e) {
            Bukkit.getLogger().warning("[NotBounties] Bounty tracker recipe cannot be registered. This is probably due to an unsupported server type.");
            NotBounties.debugMessage(e.toString(), true);
        }
    }

    public static boolean isEnabled() {
        return tracker;
    }

    public static NamespacedKey getBountyTrackerRecipe() {
        return bountyTrackerRecipe;
    }

    private static void registerRecipes() throws UnsupportedOperationException{
        bountyTrackerRecipe = new NamespacedKey(NotBounties.getInstance(),"bounty_tracker");
        if (Bukkit.getRecipe(bountyTrackerRecipe) == null) {
            ShapedRecipe bountyTrackerCraftingPattern = new ShapedRecipe(
                    bountyTrackerRecipe,
                    getEmptyTracker()
            );
            bountyTrackerCraftingPattern.shape(" AS", "ACA", "AA ");
            if (NotBounties.getServerVersion() >= 18)
                bountyTrackerCraftingPattern.setIngredient('S', Material.SPYGLASS);
            else
                bountyTrackerCraftingPattern.setIngredient('S', Material.TRIPWIRE_HOOK);
            if (NotBounties.getServerVersion() >= 17)
                bountyTrackerCraftingPattern.setIngredient('A', Material.AMETHYST_SHARD);
            else
                bountyTrackerCraftingPattern.setIngredient('A', Material.PAPER);
            bountyTrackerCraftingPattern.setIngredient('C', Material.COMPASS);
            Bukkit.addRecipe(bountyTrackerCraftingPattern);
        }
    }

    public static ItemStack getEmptyTracker() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(LanguageOptions.parse(LanguageOptions.getMessage("empty-tracker-name"), null));
        List<String> lore = new ArrayList<>();
        LanguageOptions.getListMessage("empty-tracker-lore").forEach(str -> lore.add(LanguageOptions.parse(str, null)));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(getNamespacedKey(), PersistentDataType.STRING, DataManager.GLOBAL_SERVER_ID.toString());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getTracker(UUID uuid) {
        if (!BountyManager.hasBounty(uuid))
            return getEmptyTracker();
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = compass.getItemMeta();
        assert meta != null;
        meta.setDisplayName(parse(getMessage("bounty-tracker-name"), player));
        ArrayList<String> lore = getListMessage("bounty-tracker-lore").stream().map(str -> parse(str, player)).collect(Collectors.toCollection(ArrayList::new));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(getNamespacedKey(), PersistentDataType.STRING, uuid.toString());
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
     * Get the tracker ID from a compass ItemStack
     * @param itemStack ItemStack to parse
     * @return The tracker id located at the end of the item's lore. An ID of -404 will be returned if the ItemStack isn't a tracker.
     * @Depricated TrackerID is no longer given to items. Instead, use {@link BountyTracker#getTrackedPlayer(ItemStack)}.
     */
    private static int getTrackerID(ItemStack itemStack) {
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

    private static boolean isHuntTracker(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.COMPASS)
            return false;
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        return meta.getPersistentDataContainer().has(BountyHunt.getHuntKey(), PersistentDataType.STRING);
    }

    /**
     * Get the player that a bounty compass is tracking.
     * @param itemStack Compass that is tracking a bounty.
     * @return The UUID of the player that the compass is tracking.
     * If the item is an empty tracker, then the console id will be returned.
     * If the item isn't a tracker, then null will be returned.
     */
    public static @Nullable UUID getTrackedPlayer(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.COMPASS)
            return null;
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        if (meta.getPersistentDataContainer().has(getNamespacedKey(), PersistentDataType.STRING)) {
            return UUID.fromString(Objects.requireNonNull(meta.getPersistentDataContainer().get(getNamespacedKey(), PersistentDataType.STRING)));
        }
        // old ids
        int oldId = getTrackerID(itemStack);
        if (oldId == -1)
            return DataManager.GLOBAL_SERVER_ID;
        if (oldId != -404 && trackedBounties.containsKey(oldId)) {
            // old tracked bounty
            UUID uuid = trackedBounties.get(oldId);
            meta.getPersistentDataContainer().set(getNamespacedKey(), PersistentDataType.STRING, uuid.toString());
            itemStack.setItemMeta(meta);
            return uuid;
        }
        return null;
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
    public static boolean removeTracker(Player player) {
        return removeTracker(player.getInventory(), player);
    }

    /**
     * Uses the config options to remove trackers from an inventory
     * @param inventory Inventory to check for trackers
     * @return True if a tracker was removed or changed to be empty.
     */
    public static boolean removeTracker(Inventory inventory, Player owner) {
        if (trackerRemove <= 0 && !BountyHunt.isRemoveOldTrackers())
            return false;
        boolean update = false;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                UUID trackedUUID = getTrackedPlayer(contents[i]);
                if (trackedUUID != null && !trackedUUID.equals(DataManager.GLOBAL_SERVER_ID)) {
                    if (BountyHunt.isRemoveOldTrackers() && isHuntTracker(contents[i])) {
                        // this will also remove the hunt tracker if there is no bounty
                        BountyHunt hunt = BountyHunt.getHunt(trackedUUID);
                        if (hunt == null || !hunt.isParticipating(owner.getUniqueId())) {
                            contents[i] = null;
                            update = true;
                        }
                    } else if (!hasBounty(trackedUUID)) {
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
        }
        if (update) { // only update if there was a change to the inventory
            inventory.setContents(contents);
        }
        return update;
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
                UUID trackedUUID = getTrackedPlayer(contents[i]);
                if (DataManager.GLOBAL_SERVER_ID.equals(trackedUUID)) {
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
            UUID trackedUUID = getTrackedPlayer(item);
            if (trackedUUID == null)
                continue;
            updateHeldTracker(player, item, trackedUUID, false);
        }
    }

    private static void updateHeldTracker(Player player, ItemStack item, UUID uuid, boolean force) {
        if (item.getType() != Material.COMPASS)
            return;
        boolean canTrack = hasBounty(uuid);
        if (!DataManager.GLOBAL_SERVER_ID.equals(uuid) && !canTrack && trackerRemove > 0) {
            // invalid tracker
            removeTracker(player);
            return;
        }
        CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
        assert compassMeta != null;
        Location previousLocation = compassMeta.hasLodestone() ? compassMeta.getLodestone() : null;
        if (!player.hasPermission("notbounties.tracker") || !canTrack) {
            // no permission or empty tracker - track another world for funky compass movements
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
            if (!DataManager.GLOBAL_SERVER_ID.equals(uuid)) // not an empty tracker
                if (trackerActionBar && (TABShowAlways || force)) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(LanguageOptions.parse(getMessage("tracker-no-permission"), player)));
                }

            if (previousLocation == null || !Objects.equals(previousLocation.getWorld(), Objects.requireNonNull(compassMeta.getLodestone()).getWorld())) {
                // only update if location has changed worlds
                compassMeta.setLodestoneTracked(false);
                item.setItemMeta(compassMeta);
            }
            return;
        }
        if (!hasBounty(uuid))
            // no bounty
            return;
        Player trackedPlayer = Bukkit.getPlayer(uuid);
        if (trackedPlayer != null && (NotBounties.getServerVersion() >= 17 && player.canSee(Objects.requireNonNull(trackedPlayer))) && !isVanished(trackedPlayer)) {
            // can track player
            if (!compassMeta.hasLodestone() || compassMeta.getLodestone() == null) {
                compassMeta.setLodestone(trackedPlayer.getLocation().getBlock().getLocation());
            } else if (Objects.equals(compassMeta.getLodestone().getWorld(), trackedPlayer.getWorld())) {
                if (compassMeta.getLodestone().distance(trackedPlayer.getLocation()) > 2) {
                    compassMeta.setLodestone(trackedPlayer.getLocation().getBlock().getLocation());
                }
            } else {
                compassMeta.setLodestone(trackedPlayer.getLocation().getBlock().getLocation());
            }


            // give tracked player glow if close enough
            if ((trackerGlow > 0 && trackedPlayer.getWorld().equals(player.getWorld()) && player.getLocation().distance(trackedPlayer.getLocation()) < trackerGlow) || trackerGlow == -1) {
                trackedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 45, 0));
            }
            // give tracked player alert if close enough
            if ((alert > 0 && trackedPlayer.getWorld().equals(player.getWorld()) && player.getLocation().distance(trackedPlayer.getLocation()) < alert) || alert == -1) {
                trackedPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(parse(getMessage("tracked-notify"), trackedPlayer)));
            }

            // build actionbar
            if (trackerActionBar && (TABShowAlways || force)) {
                StringBuilder actionBar = new StringBuilder(ChatColor.DARK_GRAY + "|");
                if (TABPlayerName)
                    actionBar.append(" ").append(ChatColor.YELLOW).append(trackedPlayer.getName()).append(ChatColor.DARK_GRAY).append(" |");
                if (TABDistance) {
                    if (trackedPlayer.getWorld().equals(player.getWorld())) {
                        actionBar.append(" ").append(ChatColor.GOLD).append((int) player.getLocation().distance(trackedPlayer.getLocation())).append("m").append(ChatColor.DARK_GRAY).append(" |");
                    } else {
                        actionBar.append(" ?m |");
                    }
                }
                if (TABPosition)
                    actionBar.append(" ").append(ChatColor.RED).append(trackedPlayer.getLocation().getBlockX()).append("x ").append(trackedPlayer.getLocation().getBlockY()).append("y ").append(trackedPlayer.getLocation().getBlockZ()).append("z").append(ChatColor.DARK_GRAY).append(" |");
                if (TABWorld)
                    actionBar.append(" ").append(ChatColor.LIGHT_PURPLE).append(trackedPlayer.getWorld().getName()).append(ChatColor.DARK_GRAY).append(" |");
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
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(LanguageOptions.parse(getMessage("tracker-offline"), player)));
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
        if (!tracker || NotBounties.isPaused())
            return;
        if (event.getItem() == null || !(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
            return;
        Player player = event.getPlayer();
        UUID trackedPlayer = getTrackedPlayer(player.getInventory().getItemInMainHand());
        if (trackedPlayer == null || trackedPlayer.equals(DataManager.GLOBAL_SERVER_ID))
            // not a tracker or an empty tracker
            return;
        updateHeldTracker(player, player.getInventory().getItemInMainHand(), trackedPlayer, true);
    }

    // remove tracker if holding
    @EventHandler
    public void onHold(PlayerItemHeldEvent event) {
        if (trackerRemove <= 0 || NotBounties.isPaused())
            return;
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item == null)
            return;

        // check if trackers are invalid
        UUID trackedPlayer = getTrackedPlayer(item);
        if (trackedPlayer == null || DataManager.GLOBAL_SERVER_ID.equals(trackedPlayer) || hasBounty(trackedPlayer))
            // not a tracker, empty tracker, or valid tracker
            return;
        removeTracker(event.getPlayer());
    }

    // remove trackers in container
    @EventHandler
    public void onOpenInv(InventoryOpenEvent event) {
        if (trackerRemove != 3 || event.getInventory().getType() == InventoryType.CRAFTING || NotBounties.isPaused())
            return;
        removeTracker(event.getInventory(), (Player) event.getPlayer());
    }

    // poster tracking
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!posterTracking || !(event.getRightClicked().getType() == EntityType.ITEM_FRAME || (NotBounties.getServerVersion() >= 17 && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME)) || NotBounties.isPaused())
            return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        UUID trackedPlayer = getTrackedPlayer(item);
        if (!DataManager.GLOBAL_SERVER_ID.equals(trackedPlayer))
            // not an empty tracker
            return;
        ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
        if (itemFrame.getItem().getType() != Material.FILLED_MAP)
            // not a map
            return;
        ItemStack frameItem = itemFrame.getItem();
        MapMeta mapMeta = (MapMeta) frameItem.getItemMeta();
        assert mapMeta != null;
        if (!mapMeta.getPersistentDataContainer().has(getNamespacedKey(), PersistentDataType.STRING))
            return;
        event.setCancelled(true);
        UUID posterPlayerUUID = UUID.fromString(Objects.requireNonNull(mapMeta.getPersistentDataContainer().get(getNamespacedKey(), PersistentDataType.STRING)));
        if (!hasBounty(posterPlayerUUID))
            return;
        removeEmptyTracker(event.getPlayer(), true); // remove one empty tracker
        NumberFormatting.givePlayer(event.getPlayer(), getTracker(posterPlayerUUID), 1); // give tracker
        // you have been given
        event.getPlayer().sendMessage(parse(getPrefix() + getMessage("tracker-receive"), event.getPlayer()));
    }

    // check for tracking crafting
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        // may have to cancel event or set result to null instead of returning or may have to listen to the craft event
        if (!tracker || !craftTracker || NotBounties.isPaused())
            return;
        ItemStack[] matrix = event.getInventory().getMatrix();
        boolean hasEmptyTracker = false;
        ItemStack head = null;
        for (ItemStack itemStack : matrix) {
            if (itemStack == null)
                continue;
            if (DataManager.GLOBAL_SERVER_ID.equals(getTrackedPlayer(itemStack))) {
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
            NotBounties.debugMessage("Player head in crafting matrix has a null owner!", false);
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
        if (!tracker || !craftTracker || !(event.getInventory() instanceof CraftingInventory inventory) || NotBounties.isPaused())
            return;
        UUID trackedPlayer = getTrackedPlayer(inventory.getResult());
        if (trackedPlayer == null || DataManager.GLOBAL_SERVER_ID.equals(trackedPlayer))
            return;
        if (event.getRawSlot() == 0) {
            event.setCancelled(true);
        } else {
            return;
        }
        // update result
        int amountCrafted = 1;
        switch (event.getAction()){
            case PICKUP_SOME, PICKUP_HALF, PICKUP_ALL, PICKUP_ONE:
                final int previousAmount = event.getCursor() != null ? event.getCursor().getAmount() : 0;
                final ItemStack result = inventory.getResult();
                NotBounties.getServerImplementation().entity(event.getWhoClicked()).runDelayed(() -> {
                    if (result != null)
                        result.setAmount(previousAmount + 1);
                    event.getWhoClicked().getOpenInventory().setCursor(result);
                }, 1);
                break;
            case DROP_ONE_SLOT, DROP_ONE_CURSOR, DROP_ALL_CURSOR, DROP_ALL_SLOT:
                if (inventory.getResult() != null)
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
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD:
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
        if (!tracker || !washTrackers || NotBounties.isPaused())
            return;
        UUID trackedPlayer = getTrackedPlayer(event.getItemDrop().getItemStack());
        if (trackedPlayer == null || DataManager.GLOBAL_SERVER_ID.equals(trackedPlayer) || isHuntTracker(event.getItemDrop().getItemStack()))
            // not a tracker or it's an empty tracker
            return;
        NotBounties.getServerImplementation().entity(event.getItemDrop()).runDelayed(() -> {
            if (!event.getItemDrop().isValid())
                return;
            if ((NotBounties.getServerVersion() < 17 && event.getItemDrop().getLocation().getBlock().getType() == Material.CAULDRON) || (NotBounties.getServerVersion() >= 17 && event.getItemDrop().getLocation().getBlock().getType() == Material.WATER_CAULDRON)) {
                ItemStack emptyTracker = getEmptyTracker().clone();
                emptyTracker.setAmount(event.getItemDrop().getItemStack().getAmount());
                event.getItemDrop().getWorld().dropItem(event.getItemDrop().getLocation(), emptyTracker);
                event.getItemDrop().remove();
            }
        }, 40);
    }

}
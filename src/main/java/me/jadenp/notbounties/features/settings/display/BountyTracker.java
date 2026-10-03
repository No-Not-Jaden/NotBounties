package me.jadenp.notbounties.features.settings.display;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.gui.CompatabilityUtils;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.LoggedPlayers;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.BountyManager.getBounty;
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
    private static boolean trackingExemptEnabled;
    private static boolean trackingExemptAllowBountySetting;
    private static long trackingExemptDelayAfterSet;

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
        trackingExemptEnabled = configuration.getBoolean("tracking-exempt.enabled");
        trackingExemptAllowBountySetting = configuration.getBoolean("tracking-exempt.allow-bounty-setting");
        trackingExemptDelayAfterSet = configuration.getLong("tracking-exempt.delay-after-set");

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
            getEmptyTracker().thenAccept(tracker -> NotBounties.getServerImplementation().global().run(() -> {
                ShapedRecipe bountyTrackerCraftingPattern = new ShapedRecipe(
                        bountyTrackerRecipe,
                        tracker
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
            }));

        }
    }

    public static CompletableFuture<ItemStack> getEmptyTracker() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.getPersistentDataContainer().set(getNamespacedKey(), PersistentDataType.STRING, DataManager.GLOBAL_SERVER_ID.toString());
        item.setItemMeta(meta);
        return Messages.setItemText(item, LanguageOptions.getMessage("empty-tracker-name"), LanguageOptions.getListMessage("empty-tracker-lore"), MessageContext.builder().build());
    }

    /**
     * Get a tracker for a player.
     * @apiNote This method is not thread safe.
     * @param uuid UUID of the player to get a tracker for.
     * @return The tracker for the player.
     */
    public static CompletableFuture<ItemStack> getTracker(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return DataManager.getBountyAsync(uuid).thenApply(bounty -> {
            if (bounty == null)
                return getEmptyTracker().join();

            ItemStack compass = new ItemStack(Material.COMPASS, 1);
            ItemMeta meta = compass.getItemMeta();
            assert meta != null;
            meta.getPersistentDataContainer().set(getNamespacedKey(), PersistentDataType.STRING, uuid.toString());
            if (NotBounties.isAboveVersion(20, 4)) {
                if (!meta.hasEnchantmentGlintOverride())
                    meta.setEnchantmentGlintOverride(true);
            } else {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                compass.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
            }
            compass.setItemMeta(meta);
            return Messages.setItemText(compass, LanguageOptions.getMessage("bounty-tracker-name"), LanguageOptions.getListMessage("bounty-tracker-lore"), MessageContext.builder().receiver(player).build()).join();
        });

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
    public static void removeTracker(Player player) {
        if (NotBounties.getServerImplementation().isOwnedByCurrentRegion(player)) {
            removeTracker(player.getInventory(), player);
        } else {
            NotBounties.getServerImplementation().region(player.getLocation()).run(() -> removeTracker(player));
        }

    }

    /**
     * Uses the config options to remove trackers from an inventory
     * @param inventory Inventory to check for trackers
     */
    public static void removeTracker(Inventory inventory, Player owner) {
        if (trackerRemove <= 0 && !BountyHunt.isRemoveOldTrackers())
            return;
        boolean update = false;
        Map<UUID, CompletableFuture<Boolean>> expiredBounties = new HashMap<>();
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
                    } else {
                        if (!expiredBounties.containsKey(trackedUUID))
                            expiredBounties.put(trackedUUID, DataManager.getBountyAsync(trackedUUID).thenApply(bounty -> bounty == null || bounty.getTotalDisplayBounty() < minBounty));
                    }
                }
            }
        }
        if (update) { // only update if there was a change to the inventory
            inventory.setContents(contents);
        }
        // check inventory against expired bounties on the correct thread
        CompletableFuture.allOf(expiredBounties.values().toArray(new CompletableFuture[0])).thenAccept(v -> {
            Set<UUID> expiredBountiesSet = expiredBounties.entrySet().stream().filter(entry -> entry.getValue().join()).map(Map.Entry::getKey).collect(Collectors.toSet());
            if (!expiredBountiesSet.isEmpty()) {
                Location location = inventory.getLocation();
                getEmptyTracker().thenAccept(emptyTracker -> {
                    if (location != null) {
                        NotBounties.getServerImplementation().region(location).run(() -> removeTrackedUUIDs(expiredBountiesSet, inventory, emptyTracker));
                    } else {
                        NotBounties.getServerImplementation().global().run(() -> removeTrackedUUIDs(expiredBountiesSet, inventory, emptyTracker));
                    }
                });
            }

        });
    }

    private static void removeTrackedUUIDs(Set<UUID> uuids, Inventory inventory, ItemStack emptyTracker) {
        boolean update = false;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                UUID trackedUUID = getTrackedPlayer(contents[i]);
                if (trackedUUID != null && uuids.contains(trackedUUID)) {
                    if (resetRemovedTrackers) {
                        ItemStack emptyTrackerCopy = emptyTracker.clone();
                        emptyTrackerCopy.setAmount(contents[i].getAmount());
                        contents[i] = emptyTrackerCopy;
                    } else {
                        contents[i] = null;
                    }
                    update = true;
                }
            }
        }
        if (update) {
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
                UUID trackedUUID = getTrackedPlayer(contents[i]);
                if (DataManager.GLOBAL_SERVER_ID.equals(trackedUUID)) {
                    if (contents[i].getAmount() > 1 && limitOne) {
                        contents[i] = new ItemStack(contents[i].getType(), contents[i].getAmount() - 1);
                    } else {
                        contents[i] = null;
                    }
                    if (limitOne)
                        break;
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

    /**
     * Sets a random tracking location for a compass.
     * @param compassMeta The compass meta to set the location for.
     * @param trackingPlayer The player that is tracking the compass.
     * @return True if the location has changed, and the compass should be updated.
     */
    private static boolean setRandomTrackingLocation(CompassMeta compassMeta, Player trackingPlayer) {
        Location previousLocation = compassMeta.hasLodestone() ? compassMeta.getLodestone() : null;
        // track another world for funky compass movements
        if (Bukkit.getWorlds().size() > 1) {
            for (World world : Bukkit.getWorlds()) {
                if (!world.equals(trackingPlayer.getWorld())) {
                    if (compassMeta.isLodestoneTracked())
                        compassMeta.setLodestoneTracked(false);
                    compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), 0, world.getSpawnLocation().getZ()));
                    break;
                }
            }
        } else {
            // only one world - set to tracking a lodestone at spawn (will only point to lodestone if there is one present)
            compassMeta.setLodestoneTracked(true);
            World world = trackingPlayer.getWorld();
            compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), 0, world.getSpawnLocation().getZ()));
        }

        if (previousLocation == null || !Objects.equals(previousLocation.getWorld(), Objects.requireNonNull(compassMeta.getLodestone()).getWorld())) {
            // only update if location has changed worlds
            compassMeta.setLodestoneTracked(false);
            return true;
        }
        return false;
    }

    private static void updateHeldTracker(Player player, ItemStack item, UUID uuid, boolean force) {
        if (item.getType() != Material.COMPASS)
            return;
        if (!DataManager.GLOBAL_SERVER_ID.equals(uuid) && trackerRemove > 0) {
            DataManager.getBountyAsync(uuid).thenAccept(bounty -> {
                if (bounty == null || bounty.getTotalDisplayBounty() < minBounty) {
                    // invalid tracker
                    removeTracker(player);
                }
            });
        }

        CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
        assert compassMeta != null;
        Location previousLocation = compassMeta.hasLodestone() ? compassMeta.getLodestone() : null;
        if (!player.hasPermission("notbounties.tracker")) {
            // no permission

            if (!DataManager.GLOBAL_SERVER_ID.equals(uuid) && trackerActionBar && (TABShowAlways || force)) {
                Messages.sendActionBar(player, getMessage("tracker-no-permission"), MessageContext.builder().receiver(player).build());
            }

            if (setRandomTrackingLocation(compassMeta, player)) {
                item.setItemMeta(compassMeta);
            }
            return;
        }

        if (DataManager.GLOBAL_SERVER_ID.equals(uuid)) {
            // empty tracker
            if (setRandomTrackingLocation(compassMeta, player)) {
                item.setItemMeta(compassMeta);
            }
            return;
        }

        Player trackedPlayer = Bukkit.getPlayer(uuid);

        DataManager.getPlayerDataAsync(uuid).thenAccept(playerData -> {
            boolean immuneToTracking = trackedPlayer != null && (trackedPlayer.hasPermission("notbounties.immunity.tracked")
                    || (trackingExemptEnabled && playerData.isTrackingExempt()));
            if (trackedPlayer != null
                    && (NotBounties.getServerVersion() >= 17 && player.canSee(Objects.requireNonNull(trackedPlayer)))
                    && !isVanished(trackedPlayer)
                    && !immuneToTracking) {
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
                    NotBounties.getServerImplementation().entity(trackedPlayer).run(() -> trackedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 45, 0)));
                }
                // give tracked player alert if close enough
                if ((alert > 0 && trackedPlayer.getWorld().equals(player.getWorld()) && player.getLocation().distance(trackedPlayer.getLocation()) < alert) || alert == -1) {
                    Messages.sendActionBar(trackedPlayer, getMessage("tracked-notify"), MessageContext.builder().receiver(trackedPlayer).build());
                }

                // build actionbar
                if (trackerActionBar && (TABShowAlways || force)) {
                    sendActionBar(player, trackedPlayer);
                }
                if (previousLocation == null || !Objects.equals(previousLocation.getWorld(), Objects.requireNonNull(compassMeta.getLodestone()).getWorld()) || previousLocation.distance(compassMeta.getLodestone()) > 2) {
                    // only update if location is greater than 2 blocks away
                    compassMeta.setLodestoneTracked(false);
                    item.setItemMeta(compassMeta);
                }
            } else {
                // player offline -
                if (trackerActionBar && (TABShowAlways || force)) {
                    if (immuneToTracking) {
                        Messages.sendActionBar(player, getMessage("tracker-immune"), MessageContext.builder().receiver(player).player(trackedPlayer).build());
                    } else {
                        Messages.sendActionBar(player, getMessage("tracker-offline"), MessageContext.builder().receiver(player).build());
                    }
                }
                if (setRandomTrackingLocation(compassMeta, player)) {
                    item.setItemMeta(compassMeta);
                }
            }
        });

    }

    private static void sendActionBar(Player player, Player trackedPlayer) {
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

        NotBounties.getServerImplementation().entity(player).run(() -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar.toString())));
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
        if (trackedPlayer == null || DataManager.GLOBAL_SERVER_ID.equals(trackedPlayer))
            // not a tracker, or is an empty tracker
            return;
        DataManager.getBountyAsync(trackedPlayer).thenAccept(bounty -> {
            if (bounty == null) {
                removeTracker(event.getPlayer());
            }
        });

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
        if ((!posterTracking && !event.getPlayer().hasPermission("notbounties.postertracking")) || !(event.getRightClicked().getType() == EntityType.ITEM_FRAME || (NotBounties.getServerVersion() >= 17 && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME)) || NotBounties.isPaused())
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
        Bounty bounty = getBounty(posterPlayerUUID);
        if (bounty == null)
            return;
        if (bounty.getTotalDisplayBounty() < minBounty) {
            event.getPlayer().sendMessage(parse(getPrefix() + getMessage("min-bounty"), minBounty, event.getPlayer()));
            return;
        }
        removeEmptyTracker(event.getPlayer(), true); // remove one empty tracker
        NumberFormatting.givePlayer(event.getPlayer(), getTracker(posterPlayerUUID), 1); // give tracker
        // you have been given
        event.getPlayer().sendMessage(parse(getPrefix() + getMessage("tracker-receive"), event.getPlayer()));
    }

    // check for tracking crafting
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        // may have to cancel event or set result to null instead of returning or may have to listen to the craft event
        if (!tracker || NotBounties.isPaused() || !craftTracker)
            return;
        boolean hasPerm = false;
        for (HumanEntity humanEntity : event.getViewers()) {
            if (humanEntity.hasPermission("notbounties.tracker.craft")){
                hasPerm = true;
                break;
            }
        }
        if (!hasPerm)
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
        UUID trackedPlayer = null;
        if (meta.getPersistentDataContainer().has(Head.UUID_KEY)) {
            try {
                trackedPlayer = UUID.fromString(Objects.requireNonNull(meta.getPersistentDataContainer().get(Head.UUID_KEY, PersistentDataType.STRING)));
            } catch (IllegalArgumentException ignored) {
                NotBounties.debugMessage("Player head in crafting matrix has invalid uuid data!", false);
            }
        }
        if (trackedPlayer == null && meta.getOwnerProfile() != null) {
            trackedPlayer = meta.getOwnerProfile().getUniqueId();
        }
        if (trackedPlayer == null) {
            NotBounties.debugMessage("Could not get player from head in crafting matrix.", false);
            return;
        }
        if (LoggedPlayers.isMissing(trackedPlayer)) {
            if (meta.getOwnerProfile() != null && meta.getOwnerProfile().getUniqueId() != null && !LoggedPlayers.isMissing(meta.getOwnerProfile().getUniqueId())) {
                trackedPlayer = meta.getOwnerProfile().getUniqueId();
            } else {
                return;
            }
        }
        Bounty bounty = getBounty(trackedPlayer);
        if (bounty == null|| bounty.getTotalDisplayBounty() < minBounty)
            return;
        ItemStack trackerItem = getTracker(trackedPlayer);
        event.getInventory().setResult(trackerItem);
    }
    // complete tracker crafting
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!tracker || !craftTracker || !event.getWhoClicked().hasPermission("notbounties.tracker.craft") || !(event.getInventory() instanceof CraftingInventory inventory) || NotBounties.isPaused())
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
                    CompatabilityUtils.setCursor((Player) event.getWhoClicked(), result);
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
                Inventory bottomInventory = CompatabilityUtils.getBottomInventory((Player) event.getWhoClicked());
                if (bottomInventory.getItem(event.getHotbarButton()) == null)
                    bottomInventory.setItem(event.getHotbarButton(), inventory.getResult());
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
        if (!tracker || !washTrackers || !event.getPlayer().hasPermission("notbounties.tracker.wash") || NotBounties.isPaused())
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

    public static boolean isTrackingExemptEnabled() {
        return trackingExemptEnabled;
    }

    public static boolean isTrackingExemptAllowBountySetting() {
        return trackingExemptAllowBountySetting;
    }

    public static long getTrackingExemptDelayAfterSet() {
        return trackingExemptDelayAfterSet;
    }
}
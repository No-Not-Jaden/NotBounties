package me.jadenp.notbounties.ui.map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.challenges.ChallengeType;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static me.jadenp.notbounties.NotBounties.debug;
import static me.jadenp.notbounties.utils.BountyManager.getBounty;
import static me.jadenp.notbounties.utils.BountyManager.hasBounty;

public class BountyMap implements Listener {

    public static BufferedImage bountyPoster;
    public static BufferedImage deadBounty;
    private static Font playerFont = null;
    public static File posterDirectory;
    public static final BiMap<Integer, UUID> mapIDs = HashBiMap.create();
    private static File mapData;

    public static void initialize(){
        posterDirectory = new File(NotBounties.getInstance().getDataFolder() + File.separator + "posters");
        //noinspection ResultOfMethodCallIgnored
        posterDirectory.mkdir();
        if (!new File(posterDirectory + File.separator + "bounty poster.png").exists())
            NotBounties.getInstance().saveResource("posters/bounty poster.png", false);
        if (!new File(posterDirectory + File.separator + "poster template.png").exists())
            NotBounties.getInstance().saveResource("posters/poster template.png", false);
        if (!new File(posterDirectory + File.separator + "dead bounty.png").exists())
            NotBounties.getInstance().saveResource("posters/dead bounty.png", false);
        if (!new File(posterDirectory + File.separator + "READ_ME.txt").exists())
            NotBounties.getInstance().saveResource("posters/READ_ME.txt", false);
        mapData = new File(posterDirectory + File.separator + "mapdata.yml");
        if (!mapData.exists())
            NotBounties.getInstance().saveResource("posters/mapdata.yml", false);

        try {
            bountyPoster = ImageIO.read(new File(posterDirectory + File.separator + "bounty poster.png"));
            deadBounty = ImageIO.read(new File(posterDirectory + File.separator + "dead bounty.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // load mapIDs
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(mapData);
        for (String key : configuration.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int id = configuration.getInt(key);
                mapIDs.put(id, uuid);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Found a key in mapdata.yml that isn't a UUID. \"" + key + "\". This will be overwritten in 5 minutes.");
            }
        }
    }

    private static final List<String> posterFiles = Arrays.asList("bounty poster.png", "dead bounty.png", "mapdata.yml", "poster template.png", "READ_ME.txt", "playerfont.ttf", "Name Line.json", "Reward Line.json");
    public static void cleanPosters() {
        int deletedFiles = 0;
        File[] files = posterDirectory.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            if (!posterFiles.contains(file.getName()))
                if (file.delete())
                    deletedFiles++;
        }
        if (deletedFiles > 0)
            Bukkit.getLogger().info("[NotBounties] Cleaned out " + deletedFiles + " poster templates.");
    }

    public static void save() throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<Integer, UUID> entry : mapIDs.entrySet()) {
            configuration.set(entry.getValue().toString(), entry.getKey());
        }
        configuration.save(mapData);
    }

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView mapView = event.getMap();
        if (mapIDs.containsKey(mapView.getId())) {
            if (!mapView.isVirtual()) {
                UUID uuid = mapIDs.get(mapView.getId());
                mapView.setLocked(ConfigOptions.lockMap);
                mapView.getRenderers().forEach(mapView::removeRenderer);
                mapView.addRenderer(new Renderer(uuid));
            }
        }
    }

    // method to initialize maps for 1.20.5 & update challenge for holding your own map
    @EventHandler
    public void onMapHold(PlayerItemHeldEvent event) {
        ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (newItem == null || !isPoster(newItem))
            return;
        MapMeta mapMeta = (MapMeta) newItem.getItemMeta();
        assert mapMeta != null;
        MapView mapView = mapMeta.getMapView();
        assert mapView != null;
        if (mapIDs.containsKey(mapView.getId())) {
            UUID uuid = mapIDs.get(mapView.getId());
            if (uuid.equals(event.getPlayer().getUniqueId()))
                ChallengeManager.updateChallengeProgress(uuid, ChallengeType.HOLD_POSTER, 1);
            if (!NotBounties.isAboveVersion(20,4) && mapView.isVirtual()) {
                mapView.setLocked(ConfigOptions.lockMap);
                mapView.getRenderers().forEach(mapView::removeRenderer);
                mapView.addRenderer(new Renderer(uuid));
            }
        }
    }
    // craft maps with a player skull
    // check for tracking crafting
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        // may have to cancel event or set result to null instead of returning or may have to listen to the craft event
        if (!ConfigOptions.craftPoster || NotBounties.isPaused())
            return;
        ItemStack[] matrix = event.getInventory().getMatrix();
        boolean hasMap = false;
        ItemStack head = null;
        for (ItemStack itemStack : matrix) {
            if (itemStack == null)
                continue;
            if (itemStack.getType() == Material.MAP) {
                if (!hasMap) {
                    hasMap = true;
                } else {
                    // already a map in previous slot
                    return;
                }
            } if (itemStack.getType() == Material.PLAYER_HEAD) {
                if (head == null) {
                    head = itemStack;
                } else {
                    // already a head in previous slot
                    return;
                }
            }
        }
        if (head == null || !hasMap)
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
        ItemStack map = hasBounty(trackedPlayer) ? getMap(Objects.requireNonNull(getBounty(trackedPlayer))) : getMap(trackedPlayer, 0, System.currentTimeMillis());
        event.getInventory().setResult(map);
    }
    // complete tracker crafting
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ConfigOptions.craftPoster || !(event.getInventory() instanceof CraftingInventory inventory) || NotBounties.isPaused())
            return;
        if (inventory.getResult() == null || inventory.getResult().getType() != Material.FILLED_MAP)
            return;
        if (event.getRawSlot() == 0) {
            event.setCancelled(true);
        } else {
            return;
        }
        // update result
        int amountCrafted = 1;
        switch (event.getAction()){
            case PICKUP_ONE,PICKUP_ALL,PICKUP_HALF,PICKUP_SOME:
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
            case DROP_ONE_CURSOR,DROP_ALL_CURSOR,DROP_ALL_SLOT,DROP_ONE_SLOT:
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
            case HOTBAR_MOVE_AND_READD,HOTBAR_SWAP:
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
        if (!ConfigOptions.washPoster || !isPoster(event.getItemDrop().getItemStack()) || NotBounties.isPaused())
            return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!event.getItemDrop().isValid())
                    return;
                ItemStack map = new ItemStack(Material.MAP, event.getItemDrop().getItemStack().getAmount());
                if ((NotBounties.serverVersion < 17 && event.getItemDrop().getLocation().getBlock().getType() == Material.CAULDRON) || (NotBounties.serverVersion >= 17 && event.getItemDrop().getLocation().getBlock().getType() == Material.WATER_CAULDRON)) {
                    event.getItemDrop().getWorld().dropItem(event.getItemDrop().getLocation(), map);
                    event.getItemDrop().remove();
                }
            }
        }.runTaskLater(NotBounties.getInstance(), 40);
    }

    public static boolean isPoster(ItemStack itemStack) {
        if (itemStack.getType() != Material.FILLED_MAP)
            return false;
        MapMeta meta = (MapMeta) itemStack.getItemMeta();
        if (meta == null || meta.getMapView() == null)
            return false;
        try {
            return mapIDs.containsKey(meta.getMapView().getId());
        } catch (NullPointerException e) {
            // error with the map id
            NotBounties.debugMessage("Error getting map view from poster!", true);
            NotBounties.debugMessage(e.toString(), true);
            return false;
        }

    }

    public static void loadFont() {
        if (new File(posterDirectory + File.separator + "playerfont.ttf").exists()) {
            try {
                playerFont = Font.createFont(Font.TRUETYPE_FONT, new File(BountyMap.posterDirectory + File.separator + "playerfont.ttf"));
            } catch (Throwable throwable) {
                Bukkit.getLogger().warning("[NotBounties] Detected a custom font, but an error occurred when reading it! This may be because of a corrupted font file or an invalid java font configuration file.");
                Bukkit.getLogger().warning("Error: " + throwable.getMessage());
                Bukkit.getLogger().warning("Reverting to default font.");
                playerFont = new Font("Serif", Font.PLAIN, 20);
            }
        } else {
            playerFont = new Font("Serif", Font.PLAIN, 20);
        }

    }

    public static Font getPlayerFont(float fontSize, boolean bold) {
        if (bold)
            return playerFont.deriveFont(fontSize).deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_ULTRABOLD));
        return playerFont.deriveFont(fontSize);
    }

    public static ItemStack getMap(Bounty bounty) {
        return getMap(bounty.getUUID(), bounty.getTotalDisplayBounty(), bounty.getLatestUpdate());
    }

    public static ItemStack getMap(UUID uuid, double displayBounty, long updateTime) {
        OfflinePlayer parser = Bukkit.getOfflinePlayer(uuid);
        MapView mapView = getMapView(uuid);

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        assert meta != null;
        try {
            meta.setMapView(mapView);
        } catch (NullPointerException e) {
            // error with the map id
            NotBounties.debugMessage("Error setting map view for poster!", true);
            NotBounties.debugMessage(e.toString(), true);
            return null;
        }
        meta.setDisplayName(LanguageOptions.parse(LanguageOptions.getMessage("map-name"), displayBounty, parser));
        ArrayList<String> lore = new ArrayList<>();
        for (String str : LanguageOptions.getListMessage("map-lore")) {
            lore.add(LanguageOptions.parse(str, displayBounty, updateTime, LocalTime.TimeFormat.SERVER, parser));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.values()[5]);
        mapItem.setItemMeta(meta);
        return mapItem;
    }

    @SuppressWarnings("deprecation")
    public static MapView getMapView(UUID uuid) {
        MapView mapView;
        if (mapIDs.containsValue(uuid)){
            int id = mapIDs.inverse().get(uuid);
            mapView = Bukkit.getMap(id);
            if (mapView == null) {
                mapIDs.remove(id);
                mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
            }
        } else {
            mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
            mapIDs.put(mapView.getId(), uuid);
        }
        mapView.setUnlimitedTracking(false);
        mapView.getRenderers().forEach(mapView::removeRenderer);
        mapView.setLocked(ConfigOptions.lockMap);
        mapView.setTrackingPosition(false);
        mapView.addRenderer(new Renderer(uuid));
        return mapView;
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPreMultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPreMultiplied, null);
    }


}

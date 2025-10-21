package me.jadenp.notbounties.features.settings.display.map;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.util.ServerVersions;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.LanguageOptions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.getPublicBounties;

public class BountyBoard {

    private static int type;
    private static int updateInterval;
    private static int staggeredUpdate;
    private static boolean glow;
    private static boolean invisible;
    private static String itemName;
    private static int updateName;

    private static final List<BountyBoard> bountyBoards = new ArrayList<>();
    private static long lastBountyBoardUpdate = System.currentTimeMillis();
    private static List<BountyBoard> queuedBoards = new ArrayList<>();
    private static final Map<UUID, Integer> boardSetup = new HashMap<>();

    public static void loadConfiguration(ConfigurationSection config) {
        type = config.getInt("type");
        updateInterval = config.getInt("update-interval");
        glow = config.getBoolean("glow");
        invisible = config.getBoolean("invisible");
        staggeredUpdate = config.getInt("staggered-update");
        itemName = config.getString("item-name");
        updateName = config.getInt("update-name");
    }

    public static List<BountyBoard> getBountyBoards() {
        return bountyBoards;
    }

    public static Set<UUID> getBoardUUIDs() {
        Set<UUID> uuids = new HashSet<>();
        for (BountyBoard board : bountyBoards) {
            if (board.getFrame() != null)
                uuids.add(board.getFrame().getUniqueId());
        }
        return uuids;
    }

    public static Map<UUID, Integer> getBoardSetup() {
        return boardSetup;
    }

    public static void addBountyBoards(List<BountyBoard> bountyBoards) {
        BountyBoard.bountyBoards.addAll(bountyBoards);
    }

    public static void addBountyBoard(BountyBoard bountyBoard) {
        bountyBoards.add(bountyBoard);
    }

    public static long getLastBountyBoardUpdate() {
        return lastBountyBoardUpdate;
    }

    public static void clearBoard() {
        for (BountyBoard board : bountyBoards) {
            board.remove();
        }
        bountyBoards.clear();
    }

    public static int removeSpecificBountyBoard(ItemFrame frame) {
        ListIterator<BountyBoard> bountyBoardListIterator = bountyBoards.listIterator();
        int removes = 0;
        while (bountyBoardListIterator.hasNext()) {
            BountyBoard board = bountyBoardListIterator.next();
            if (frame.equals(board.getFrame())) {
                board.remove();
                bountyBoardListIterator.remove();
                removes++;
            }
        }
        return removes;
    }

    /**
     * Updates the bounty boards, following the config options.
     */
    public static synchronized void update() {
        if (BountyBoard.getLastBountyBoardUpdate() + updateInterval * 1000L < System.currentTimeMillis() && !Bukkit.getOnlinePlayers().isEmpty()) {
            // update bounty board
            if (queuedBoards.isEmpty()) {
                queuedBoards = new LinkedList<>(BountyBoard.getBountyBoards());
            }
            int minUpdate = staggeredUpdate == 0 ? queuedBoards.size() : staggeredUpdate;
            List<Bounty> bountyCopy = getPublicBounties(type);
            int numBoards = Math.min(queuedBoards.size(), minUpdate);
            for (int i = 0; i < numBoards; i++) {
                BountyBoard board = queuedBoards.remove(0);
                if (bountyCopy.size() >= board.getRank()) {
                    board.update(bountyCopy.get(board.getRank() - 1));
                } else {
                    board.update(null);
                }
            }

            lastBountyBoardUpdate = System.currentTimeMillis();
        }
    }

    private final Location location;
    private final BlockFace direction;
    private final int rank;
    private UUID lastUUID = null;
    private ItemFrame frame = null;
    double lastBounty = 0;

    public BountyBoard(Location location, BlockFace direction, int rank) {

        this.location = location;
        this.direction = direction;
        this.rank = rank;

    }

    public void update(Bounty bounty) {
        if (location.getWorld() == null || !location.isWorldLoaded()
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
            return;
        if (bounty == null) {
            lastUUID = null;
            lastBounty = 0;
            remove();
            return;
        }
        if (updateName == 2 || !bounty.getUUID().equals(lastUUID) || (updateName == 1 && lastBounty != bounty.getTotalDisplayBounty())) {
            lastUUID = bounty.getUUID();
            lastBounty = bounty.getTotalDisplayBounty();
            remove();
        }
        if (frame == null) {
            EntityType frameType = glow && NotBounties.getServerVersion() >= 17 ? EntityType.GLOW_ITEM_FRAME : EntityType.ITEM_FRAME;
            try {
                ItemStack map = BountyMap.getMap(bounty);
                if (map == null)
                    return;
                NotBounties.getServerImplementation().region(location).run(() -> {
                    frame = (ItemFrame) Objects.requireNonNull(location.getWorld()).spawnEntity(location, frameType);
                    frame.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);
                    frame.setFacingDirection(direction, true);
                    ItemMeta mapMeta = map.getItemMeta();
                    assert mapMeta != null;
                    mapMeta.setDisplayName(LanguageOptions.parse(itemName, bounty.getTotalDisplayBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())));
                    map.setItemMeta(mapMeta);
                    frame.setItem(map);
                    frame.setInvulnerable(true);
                    frame.setVisible(!invisible);
                    frame.setFixed(true);
                });

            } catch (IllegalArgumentException ignored) {
                // this is thrown when there is no space to place the board
            }
        }


    }



    public ItemFrame getFrame() {
        return frame;
    }

    public BlockFace getDirection() {
        return direction;
    }

    public Location getLocation() {
        return location;
    }

    public void remove() {
        // remove any duplicate frames
        if (!NotBounties.getInstance().isEnabled()) {
            if (ServerVersions.isFolia())
                // cannot remove entities while the plugin is disabling
                // https://github.com/PaperMC/Folia/issues/353
                return;

            for (Entity entity : Objects.requireNonNull(location.getWorld()).getNearbyEntities(location, 0.5, 0.5, 0.5))
                if (entity.getType() == EntityType.ITEM_FRAME || (NotBounties.getServerVersion() >= 17 && entity.getType() == EntityType.GLOW_ITEM_FRAME) && entity.getLocation().distance(location) < 0.01)
                    entity.remove();

            if (frame != null) {
                frame.setItem(null);
                frame.remove();
                frame = null;
            }
        } else {
            NotBounties.getServerImplementation().region(location).run(() -> {
                for (Entity entity : Objects.requireNonNull(location.getWorld()).getNearbyEntities(location, 0.5, 0.5, 0.5)) {
                    if (entity.getType() == EntityType.ITEM_FRAME || (NotBounties.getServerVersion() >= 17 && entity.getType() == EntityType.GLOW_ITEM_FRAME) && entity.getLocation().distance(location) < 0.01) {
                        entity.remove();
                    }
                }
            });


            if (frame != null) {
                NotBounties.getServerImplementation().entity(frame).run(() -> {
                    frame.setItem(null);
                    frame.remove();
                    frame = null;
                });
            }
        }
    }

    public int getRank() {
        return rank;
    }
}

package me.jadenp.notbounties.features.settings.display.map;

import com.cjcrafter.foliascheduler.util.ServerVersions;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import me.jadenp.notbounties.features.settings.databases.BountySortType;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BountyBoard {

    private static BountySortType type;
    private static int updateInterval;
    private static int staggeredUpdate;
    private static boolean glow;
    private static boolean invisible;
    private static String itemName;
    private static int updateName;

    private static final List<BountyBoard> bountyBoards = Collections.synchronizedList(new ArrayList<>());
    private static long lastBountyBoardUpdate = System.currentTimeMillis();
    private static int nextBoardUpdateIndex = 0;
    private static final Map<UUID, Integer> boardSetup = Collections.synchronizedMap(new HashMap<>());

    public static void loadConfiguration(ConfigurationSection config) {
        String typeString = config.getString("type");
        try {
            type = BountySortType.valueOf(Objects.requireNonNull(typeString).toUpperCase());
        } catch (IllegalArgumentException e) {
            NotBounties.getInstance().getLogger().warning("Invalid bounty board sort type: " + typeString);
            type = BountySortType.HIGHEST;
        }
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
        bountyBoards.sort(Comparator.comparingInt(BountyBoard::getRank));
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
    public static void update() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(NotBounties.getInstance(), BountyBoard::update);
            return;
        }

        if (BountyBoard.getLastBountyBoardUpdate() + updateInterval * 1000L < System.currentTimeMillis() && !Bukkit.getOnlinePlayers().isEmpty() && NotBounties.getInstance().isEnabled()) {
            // update bounty board

            int minUpdate = staggeredUpdate <= 0 ? bountyBoards.size() : staggeredUpdate; // 0 means update all boards
            List<BountyBoard> boardsToUpdate = new ArrayList<>();
            for (int i = 0; i < minUpdate; i++) {
                if (minUpdate < bountyBoards.size())
                    boardsToUpdate.add(bountyBoards.get(nextBoardUpdateIndex + i));
            }
            nextBoardUpdateIndex = nextBoardUpdateIndex + minUpdate;
            if (nextBoardUpdateIndex >= bountyBoards.size())
                nextBoardUpdateIndex = 0;
            // get the min and max ranks of the boards to update - should be in sorted order already
            int minRank = boardsToUpdate.getFirst().getRank();
            assert minRank > 0;
            int maxRank = boardsToUpdate.getLast().getRank();
            assert maxRank > minRank;
            DataManager.getPublicBountiesAsync(type, minRank - 1, maxRank - minRank + 1)
                    .thenAccept(bounties -> NotBounties.getServerImplementation().global().run(() ->
                        // iterate through the boards and update them
                        applyBounties(bounties, boardsToUpdate, minRank)));

            lastBountyBoardUpdate = System.currentTimeMillis();
        }

    }

    private static void applyBounties(List<Bounty> bounties, List<BountyBoard> boardsToUpdate, int minRank) {
        for (BountyBoard board : boardsToUpdate) {
            // first index in bounties is the minRank
            int bountyIndex = board.getRank() - minRank;
            if (bountyIndex >= bounties.size()) {
                board.update(null);
            } else {
                Bounty bounty = bounties.get(bountyIndex);
                board.update(bounty);
            }
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
        if (rank <= 0 ) {
            rank = 1;
        }
        this.rank = rank;

    }

    public synchronized void update(Bounty bounty) {
        if (location.getWorld() == null || !location.isWorldLoaded()
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
            return;
        if (bounty == null) {
            if (frame != null) {
                lastUUID = null;
                lastBounty = 0;
                remove();
            }
            return;
        }
        if (updateName == 2 || !bounty.getUUID().equals(lastUUID) || (updateName == 1 && lastBounty != bounty.getTotalDisplayBounty())) {
            lastUUID = bounty.getUUID();
            lastBounty = bounty.getTotalDisplayBounty();
            remove();
        }
        if (frame != null && !frame.isValid()) {
            remove();
        }
        if (frame == null) {
            placeBoard(bounty);
        }


    }

    private void placeBoard(Bounty bounty) {
        EntityType frameType = glow && NotBounties.getServerVersion() >= 17 ? EntityType.GLOW_ITEM_FRAME : EntityType.ITEM_FRAME;
        try {
            ItemStack map = BountyMap.getMap(bounty);
            if (map == null)
                return;
            NotBounties.getServerImplementation().region(location).run(() -> {
                frame = (ItemFrame) Objects.requireNonNull(location.getWorld()).spawnEntity(location, frameType);
                frame.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);
                frame.setFacingDirection(direction, true);
                frame.setInvulnerable(true);
                frame.setVisible(!invisible);
                frame.setFixed(true);
                ItemMeta mapMeta = map.getItemMeta();
                assert mapMeta != null;
                OfflinePlayer bountyPlayer = Bukkit.getOfflinePlayer(bounty.getUUID());
                Messages.parse(itemName, MessageContext.builder().amount(bounty.getTotalDisplayBounty()).receiver(bountyPlayer).build())
                        .thenAccept(name -> NotBounties.getServerImplementation().region(location).run(() -> {
                    map.setItemMeta(mapMeta);
                    frame.setItem(map);
                }));
            });

        } catch (IllegalArgumentException e) {
            // this is thrown when there is no space to place the board
            NotBounties.debugMessage("Failed to place a bounty board: " + e, true);
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

            removeDuplicateFrames();

            if (frame != null) {
                frame.setItem(null);
                frame.remove();
                frame = null;
            }
        } else {
            NotBounties.getServerImplementation().region(location).run(this::removeDuplicateFrames);

            if (frame != null) {
                NotBounties.getServerImplementation().entity(frame).run(() -> {
                    frame.setItem(null);
                    frame.remove();
                    frame = null;
                });
            }
        }
    }

    private void removeDuplicateFrames() {
        for (Entity entity : Objects.requireNonNull(location.getWorld()).getNearbyEntities(location, 0.5, 0.5, 0.5)) {
            if (entity.getType() == EntityType.ITEM_FRAME || (NotBounties.getServerVersion() >= 17 && entity.getType() == EntityType.GLOW_ITEM_FRAME) && entity.getLocation().distance(location) < 0.01) {
                entity.remove();
            }
        }
    }

    public int getRank() {
        return rank;
    }
}

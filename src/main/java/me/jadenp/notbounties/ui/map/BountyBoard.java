package me.jadenp.notbounties.ui.map;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.getPublicBounties;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;

public class BountyBoard {
    private static final List<BountyBoard> bountyBoards = new ArrayList<>();
    private static long lastBountyBoardUpdate = System.currentTimeMillis();
    private static List<BountyBoard> queuedBoards = new ArrayList<>();

    public static List<BountyBoard> getBountyBoards() {
        return bountyBoards;
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
    public static void update() {
        if (BountyBoard.getLastBountyBoardUpdate() + boardUpdate * 1000 < System.currentTimeMillis() && !Bukkit.getOnlinePlayers().isEmpty()) {
            // update bounty board
            if (queuedBoards.isEmpty()) {
                queuedBoards = new ArrayList<>(BountyBoard.getBountyBoards());
            }
            int minUpdate = boardStaggeredUpdate == 0 ? queuedBoards.size() : boardStaggeredUpdate;
            List<Bounty> bountyCopy = getPublicBounties(boardType);
            for (int i = 0; i < Math.min(queuedBoards.size(), minUpdate); i++) {
                BountyBoard board = queuedBoards.get(i);
                if (bountyCopy.size() >= board.getRank()) {
                    board.update(bountyCopy.get(board.getRank() - 1));
                } else {
                    board.update(null);
                }
            }
            if (Math.min(queuedBoards.size(), minUpdate) > 0) {
                queuedBoards.subList(0, Math.min(queuedBoards.size(), minUpdate)).clear();
            }
            lastBountyBoardUpdate = System.currentTimeMillis();
        }
    }

    private final Location location;
    private final Chunk chunk;
    private final BlockFace direction;
    private final int rank;
    private UUID lastUUID = null;
    private ItemFrame frame = null;
    double lastBounty = 0;

    public BountyBoard(Location location, BlockFace direction, int rank) {

        this.location = location;
        chunk = location.getChunk();
        this.direction = direction;
        this.rank = rank;

    }

    public void update(Bounty bounty) {
        if (!chunk.isLoaded())
            return;
        if (bounty == null) {
            lastUUID = null;
            lastBounty = 0;
            remove();
            return;
        }
        if (ConfigOptions.updateName == 2 || !bounty.getUUID().equals(lastUUID) || (ConfigOptions.updateName == 1 && lastBounty != bounty.getTotalDisplayBounty())) {
            lastUUID = bounty.getUUID();
            lastBounty = bounty.getTotalDisplayBounty();
            remove();
        }
        if (frame == null) {
            EntityType type = ConfigOptions.boardGlow && NotBounties.serverVersion >= 17 ? EntityType.GLOW_ITEM_FRAME : EntityType.ITEM_FRAME;
            try {
                ItemStack map = BountyMap.getMap(bounty);
                if (map == null)
                    return;
                frame = (ItemFrame) Objects.requireNonNull(location.getWorld()).spawnEntity(location, type);
                frame.getPersistentDataContainer().set(NotBounties.namespacedKey, PersistentDataType.STRING, NotBounties.sessionKey);
                frame.setFacingDirection(direction, true);
                ItemMeta mapMeta = map.getItemMeta();
                assert mapMeta != null;
                mapMeta.setDisplayName(LanguageOptions.parse(ConfigOptions.boardName, bounty.getTotalDisplayBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())));
                map.setItemMeta(mapMeta);
                frame.setItem(map);
                frame.setInvulnerable(true);
                frame.setVisible(!ConfigOptions.boardInvisible);
                frame.setFixed(true);
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
        for (Entity entity : Objects.requireNonNull(location.getWorld()).getNearbyEntities(location, 0.5,0.5,0.5)) {
            if (entity.getType() == EntityType.ITEM_FRAME || (NotBounties.serverVersion >= 17 && entity.getType() == EntityType.GLOW_ITEM_FRAME) && entity.getLocation().distance(location) < 0.01) {
                entity.remove();
            }
        }
        if (frame != null) {
            frame.setItem(null);
            frame.remove();
            frame = null;
        }
    }

    public int getRank() {
        return rank;
    }
}

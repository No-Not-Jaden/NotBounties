package me.jadenp.notbounties;

import me.jadenp.notbounties.map.BountyMap;
import me.jadenp.notbounties.utils.ConfigOptions;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class BountyBoard {
    private final Location location;
    private final BlockFace direction;
    private final int rank;
    private UUID lastUUID = null;
    private ItemFrame frame = null;

    public BountyBoard(Location location, BlockFace direction, int rank) {

        this.location = location;
        this.direction = direction;
        this.rank = rank;
    }

    public void update(Bounty bounty) throws IOException {
        if (!location.getChunk().isLoaded())
            return;
        if (bounty == null) {
            lastUUID = null;
            remove();
            return;
        }
        if (frame == null) {
            EntityType type = ConfigOptions.boardGlow ? EntityType.GLOW_ITEM_FRAME : EntityType.ITEM_FRAME;
            frame = (ItemFrame) Objects.requireNonNull(location.getWorld()).spawnEntity(location, type);
        }
        if (bounty.getUUID() != lastUUID) {
            lastUUID = bounty.getUUID();
            frame.setFacingDirection(direction);
            //frame.setRotation(Rotation.NONE);
            frame.setItem(BountyMap.getMap(bounty));
            frame.setFixed(true);
            frame.setInvulnerable(true);
            frame.setVisible(!ConfigOptions.boardInvisible);
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
        if (frame != null) {
            frame.remove();
            frame = null;
        }
    }

    public int getRank() {
        return rank;
    }
}

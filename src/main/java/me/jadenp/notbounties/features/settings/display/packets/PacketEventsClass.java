package me.jadenp.notbounties.features.settings.display.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PacketEventsClass {

    private PacketEventsClass() {}

    public static void teleportArmorStand(Set<Player> viewers, int entityId, Location location, Vector velocity) {
        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(entityId, SpigotConversionUtil.fromBukkitLocation(location), false);
        WrapperPlayServerEntityVelocity velocityPacket = new WrapperPlayServerEntityVelocity(entityId, new Vector3d(velocity.getX(), velocity.getY(), velocity.getZ()));
        PlayerManager pm = PacketEvents.getAPI().getPlayerManager();
        for (Player viewer : viewers) {
            pm.sendPacket(viewer, teleportPacket);
            pm.sendPacket(viewer, velocityPacket);
        }
    }

    public static void removeArmorStand(Set<Player> viewers, int entityId) {
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityId);
        PlayerManager pm = PacketEvents.getAPI().getPlayerManager();
        for (Player viewer : viewers) {
            pm.sendPacket(viewer, destroyPacket);
        }
    }

    public static void spawnArmorStand(Set<Player> viewers, int entityId, UUID uuid, Location location, Vector velocity) {
        com.github.retrooper.packetevents.protocol.world.Location loc = SpigotConversionUtil.fromBukkitLocation(location);

        // Spawn Packet
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                uuid,
                EntityTypes.ARMOR_STAND,
                loc,
                loc.getYaw(),
                0,
                new Vector3d(velocity.getX(), velocity.getY(), velocity.getZ())
        );

        PlayerManager pm = PacketEvents.getAPI().getPlayerManager();
        for (Player viewer : viewers) {
            pm.sendPacket(viewer, spawn);
        }
    }

    public static void setArmorStandText(int entityId, String text, Set<Player> viewers) {
        WrapperPlayServerEntityMetadata metaPacket = getArmorStandMeta(text, entityId);
        PlayerManager pm = PacketEvents.getAPI().getPlayerManager();
        for (Player viewer : viewers) {
            pm.sendPacket(viewer, metaPacket);
        }
    }

    private static @NotNull WrapperPlayServerEntityMetadata getArmorStandMeta(String customName, int entityId) {
        EntityData<Byte> flags = new EntityData<>(
                0,
                EntityDataTypes.BYTE,
                (byte)(0x20 /* invisible */)
        );
        EntityData<Optional<Component>> name = new EntityData<>(
                2,
                EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                Optional.of(Component.text(customName))
        );
        EntityData<Boolean> nameVisible = new EntityData<>(
                3,
                EntityDataTypes.BOOLEAN,
                true
        );
        EntityData<Boolean> noGravity = new EntityData<>(
                5,
                EntityDataTypes.BOOLEAN,
                true
        );
        EntityData<Byte> marker = new EntityData<>(
                15,
                EntityDataTypes.BYTE,
                (byte) 0x10
        );

        return new WrapperPlayServerEntityMetadata(
                entityId,
                List.of(flags, name, nameVisible, noGravity, marker)
        );
    }



}

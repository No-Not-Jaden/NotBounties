package me.jadenp.notbounties.features.settings.display.packets;

import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.display.TagProvider;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FakeArmorStand extends TagProvider {
    private final int entityId;
    private final UUID uuid;
    private final Set<Player> activePlayers = new HashSet<>();

    public FakeArmorStand(Player trackedPlayer) {
        super(trackedPlayer);
        this.entityId = SpigotReflectionUtil.generateEntityId();
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void setText(String text) {
        this.text = text;
        PacketEventsClass.setArmorStandText(entityId, text, activePlayers);
    }

    @Override
    public String getText() {
        return text;
    }

    public void updateVisibility() {
        // check if player leaves or enters view distance
        activePlayers.removeIf(player -> !player.isOnline());
        int viewDist = ConfigOptions.getDefaultEntityTrackingRangePlayer(); // default entity-tracking-range in spigot.yml
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(trackedPlayer.getUniqueId()) )
                continue;
            if (!player.getWorld().equals(trackedPlayer.getWorld()) || player.getLocation().distance(trackedPlayer.getLocation()) > viewDist) {
                // left visibility
                if (activePlayers.contains(player)) {
                    remove(player);
                }
            } else {
                // in visibility
                if (!activePlayers.contains(player))
                    spawn(player);
            }
        }
    }

    public void spawn() {
        updateVisibility();
        if (WantedTags.isShowOwn())
            spawn(trackedPlayer);
    }

    @Override
    public Location getLocation() {
        return lastLocation;
    }

    @Override
    public boolean isValid() {
        return !activePlayers.isEmpty();
    }

    public void spawn(Player... players) {
        if (!trackedPlayer.isOnline())
            return;
        Location location = trackedPlayer.getEyeLocation().add(0, WantedTags.getWantedOffset(), 0);
        lastLocation = location;
        if (ConfigOptions.getIntegrations().isPacketEventsEnabled())
            PacketEventsClass.spawnArmorStand(Set.of(players), entityId, uuid, location, trackedPlayer.getVelocity());
        activePlayers.addAll(List.of(players));
    }



    public void teleport() {
        if (!trackedPlayer.isOnline())
            return;
        Location location = trackedPlayer.getEyeLocation().add(0, WantedTags.getWantedOffset(), 0);
        lastLocation = location;
        if (ConfigOptions.getIntegrations().isPacketEventsEnabled())
            PacketEventsClass.teleportArmorStand(activePlayers, entityId, location, trackedPlayer.getVelocity());
    }

    @Override
    public void remove() {
        if (isValid()) {
            if (ConfigOptions.getIntegrations().isPacketEventsEnabled())
                PacketEventsClass.removeArmorStand(activePlayers, entityId);
            activePlayers.clear();
        }
    }

    public void remove(Player... player) {
        if (isValid()) {
            if (ConfigOptions.getIntegrations().isPacketEventsEnabled())
                PacketEventsClass.removeArmorStand(Set.of(player), entityId);
            Set.of(player).forEach(activePlayers::remove);
        }

    }
}
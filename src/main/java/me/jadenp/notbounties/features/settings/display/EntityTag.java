package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class EntityTag extends TagProvider {

    private ArmorStand armorStand = null;

    public EntityTag(Player trackedPlayer) {
        super(trackedPlayer);
    }

    @Override
    public void setText(String text) {
        this.text = text;
        armorStand.setCustomName(text);
    }

    @Override
    public String getText() {
        return armorStand.getCustomName();
    }

    @Override
    public void updateVisibility() {
        if (NotBounties.getServerVersion() >= 17 && trackedPlayer.canSee(armorStand) && !WantedTags.isShowOwn())
            trackedPlayer.hideEntity(NotBounties.getInstance(), armorStand);
        // a fix for 1.16, the random is to help with performance
        if (NotBounties.getServerVersion() <= 16 && Math.random() <= 0.5) {
            armorStand.setVisible(true);
            armorStand.setVisible(false);
            armorStand.setMarker(false);
            armorStand.setMarker(true);
            armorStand.setCustomName("This is a bug!");
            armorStand.setCustomName(text);
            armorStand.setCustomNameVisible(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setCollidable(true);
            armorStand.setCollidable(false);
        }
    }

    @Override
    public void teleport() {
        armorStand.teleport(trackedPlayer.getEyeLocation().add(new Vector(0, WantedTags.getWantedOffset(), 0)));
        armorStand.setVelocity(trackedPlayer.getVelocity());
    }

    @Override
    public void remove() {
        if (armorStand == null)
            return;
        lastLocation = armorStand.getLocation();
        armorStand.setInvulnerable(false);
        armorStand.setCollidable(true);
        armorStand.setVisible(true);
        armorStand.remove();
        armorStand = null;
    }

    @Override
    public void spawn() {
        armorStand = (ArmorStand) trackedPlayer.getWorld().spawnEntity(trackedPlayer.getEyeLocation().add(0,WantedTags.getWantedOffset(),0), EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setMarker(true);
        armorStand.setCustomNameVisible(true);
        armorStand.setAI(false);
        armorStand.setCollidable(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setInvulnerable(true);
        armorStand.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);
        if (NotBounties.getServerVersion() >= 17 && !WantedTags.isShowOwn())
            trackedPlayer.hideEntity(NotBounties.getInstance(), armorStand);
        lastLocation = armorStand.getLocation();
    }

    @Override
    public boolean isValid() {
        return armorStand != null;
    }
}

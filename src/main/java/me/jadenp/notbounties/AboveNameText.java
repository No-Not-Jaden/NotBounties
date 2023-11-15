package me.jadenp.notbounties;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import static me.jadenp.notbounties.utils.ConfigOptions.*;

@SuppressWarnings("UnstableApiUsage")
public class AboveNameText {
    private final Player player;
    private ArmorStand armorStand = null;
    public AboveNameText(Player player) {
        this.player = player;
        if (!NotBounties.getInstance().hasBounty(player))
            return;
        spawnWantedTag();
    }

    public void updateArmorStand(){
        if (player != null && player.isOnline() && NotBounties.getInstance().hasBounty(player)) {
            if (hideWantedWhenSneaking && player.isSneaking()) {
                if (armorStand != null)
                    removeStand();
                return;
            }
            if (armorStand == null) {
                spawnWantedTag();
            }
            String text = getWantedDisplayText(player);
            if (!text.equalsIgnoreCase(armorStand.getCustomName())) {
                armorStand.setCustomName(text);
            }
            if (player.canSee(armorStand))
                player.hideEntity(NotBounties.getInstance(), armorStand);
            if (!armorStand.getLocation().getChunk().isLoaded()) {
                armorStand.getLocation().getChunk().load();
                teleport();
                armorStand.getLocation().getChunk().unload();
            } else {
                teleport();
            }
        } else {
         removeStand();
        }
    }

    private void teleport(){
        armorStand.teleport(player.getEyeLocation().add(new Vector(0, wantedOffset, 0)));
    }

    public void removeStand(){
        if (armorStand == null)
            return;
        armorStand.remove();
        armorStand = null;
    }
    private void spawnWantedTag(){
        armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getEyeLocation().add(0,wantedOffset,0), EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setMarker(true);
        armorStand.setCustomName(getWantedDisplayText(player));
        armorStand.setCustomNameVisible(true);
        //armorStand.setPersistent(true);
        armorStand.setAI(false);
        armorStand.setCollidable(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setInvulnerable(true);
        armorStand.getPersistentDataContainer().set(NotBounties.namespacedKey, PersistentDataType.STRING, NotBounties.sessionKey);
        player.hideEntity(NotBounties.getInstance(), armorStand);

    }



}

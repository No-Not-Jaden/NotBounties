package me.jadenp.notbounties;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import static me.jadenp.notbounties.utils.ConfigOptions.*;

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
            armorStand.teleport(player.getEyeLocation().add(new Vector(0, wantedOffset, 0)));
        } else {
         removeStand();
        }
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
        armorStand.setPersistent(true);
        armorStand.setAI(false);
        armorStand.setCollidable(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setInvulnerable(true);
        player.hideEntity(NotBounties.getInstance(), armorStand);

    }



}

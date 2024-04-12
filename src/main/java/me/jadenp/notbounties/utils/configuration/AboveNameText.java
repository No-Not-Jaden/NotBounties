package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.BountyManager;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;

public class AboveNameText {
    private final Player player;
    private ArmorStand armorStand = null;
    private long nextTextUpdateTime = 0;
    private long nextVisibilityUpdateTime = 0;
    public AboveNameText(Player player) {
        this.player = player;
        if (!BountyManager.hasBounty(player))
            return;
        spawnWantedTag();
    }

    public void updateArmorStand(){
        if (player != null && player.isOnline() && BountyManager.hasBounty(player)) {
            if (nextVisibilityUpdateTime < System.currentTimeMillis()) {
                // conditions if the tag should be removed/invisible
                if ((hideWantedWhenSneaking && player.isSneaking()) || player.getGameMode().equals(GameMode.SPECTATOR) || player.isInvisible() || isVanished()) {
                    if (armorStand != null)
                        removeStand();
                    return;
                }
                if (armorStand == null) {
                    spawnWantedTag();
                }
                nextVisibilityUpdateTime = System.currentTimeMillis() + wantedVisibilityUpdateInterval;
            }

            if (nextTextUpdateTime < System.currentTimeMillis()) {
                // check if the name should be changed
                String text = getWantedDisplayText(player);
                if (!text.equalsIgnoreCase(armorStand.getCustomName())) {
                    armorStand.setCustomName(text);
                }
                nextTextUpdateTime = System.currentTimeMillis() + wantedTextUpdateInterval;
            }
            if (NotBounties.serverVersion >= 17 && player.canSee(armorStand))
                player.hideEntity(NotBounties.getInstance(), armorStand);
            // a fix for 1.16, the random is to help with performance
            if (NotBounties.serverVersion <= 16 && Math.random() <= 0.5) {
                armorStand.setVisible(true);
                armorStand.setVisible(false);
                armorStand.setMarker(false);
                armorStand.setMarker(true);
                armorStand.setCustomName("This is a bug!");
                armorStand.setCustomName(getWantedDisplayText(player));
                armorStand.setCustomNameVisible(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setCollidable(true);
                armorStand.setCollidable(false);
            }
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
        armorStand.setVisible(false);
        armorStand.setMarker(true);
        armorStand.setCustomName(getWantedDisplayText(player));
        armorStand.setCustomNameVisible(true);
        //armorStand.setPersistent(true);
        armorStand.setAI(false);
        armorStand.setCollidable(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setInvulnerable(true);
        armorStand.getPersistentDataContainer().set(NotBounties.namespacedKey, PersistentDataType.STRING, NotBounties.sessionKey);
        if (NotBounties.serverVersion >= 17)
            player.hideEntity(NotBounties.getInstance(), armorStand);

    }

    private boolean isVanished() {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }



}

package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Map;

import static me.jadenp.notbounties.NotBounties.isVanished;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;

public class WantedTags {
    private static boolean wanted;
    private static double wantedOffset;
    private static String wantedText;
    private static long wantedTextUpdateInterval;
    private static long wantedVisibilityUpdateInterval;
    private static double minWanted;
    private static boolean hideWantedWhenSneaking;
    private static boolean hideWantedWhenMoving;

    public static void loadConfiguration(ConfigurationSection configuration) {
        wanted = configuration.getBoolean("enabled");
        wantedOffset = configuration.getDouble("offset");
        wantedText = configuration.getString("text");
        minWanted = configuration.getDouble("min-bounty");
        hideWantedWhenSneaking = configuration.getBoolean("hide-when-sneaking");
        wantedTextUpdateInterval = configuration.getLong("text-update-interval");
        wantedVisibilityUpdateInterval = configuration.getLong("visibility-update-interval");
        hideWantedWhenMoving = configuration.getBoolean("hide-when-moving");
    }

    public static String getWantedDisplayText(OfflinePlayer player) {
        Bounty bounty = BountyManager.getBounty(player.getUniqueId());
        if (bounty == null)
            return "";
        String levelReplace = "";
        for (Map.Entry<Integer, String> entry : wantedLevels.entrySet()) {
            if (entry.getKey() <= bounty.getTotalDisplayBounty()) {
                levelReplace = entry.getValue();
            } else {
                break;
            }
        }
        return LanguageOptions.parse(wantedText.replace("{level}", (levelReplace)), bounty.getTotalDisplayBounty(), player);
    }

    public static boolean isEnabled() {
        return wanted;
    }

    public static double getMinWanted() {
        return minWanted;
    }

    private final Player player;
    private ArmorStand armorStand = null;
    private long nextTextUpdateTime = 0;
    private long nextVisibilityUpdateTime = 0;
    private boolean enabled = true;
    private Location lastPlayerLocation;
    
    public WantedTags(Player player) {
        this.player = player;
        lastPlayerLocation = player.getLocation();
        if (!BountyManager.hasBounty(player.getUniqueId()))
            return;
        spawnWantedTag();
    }

    private boolean hasMoved() {
        boolean moved = player.getWorld().equals(lastPlayerLocation.getWorld()) && player.getLocation().distance(lastPlayerLocation) > 0.1;
        lastPlayerLocation = player.getLocation();
        return moved;
    }

    public void updateArmorStand(){
        if (enabled && player != null && player.isOnline()) {
            if (nextVisibilityUpdateTime < System.currentTimeMillis()) {
                // conditions if the tag should be removed/invisible
                if (!BountyManager.hasBounty(player.getUniqueId())
                        || (hideWantedWhenSneaking && player.isSneaking())
                        || (hideWantedWhenMoving && hasMoved())
                        || player.getGameMode().equals(GameMode.SPECTATOR)
                        || player.isInvisible()
                        || isVanished(player)) {
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

    private void teleport() {
        armorStand.teleport(player.getEyeLocation().add(new Vector(0, wantedOffset, 0)));
    }

    public void disable() {
        enabled = false;
        removeStand();
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





}

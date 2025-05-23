package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
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

import java.util.*;

import static me.jadenp.notbounties.NotBounties.isVanished;


public class WantedTags {

    /**
     * Whether the Wanted Tags feature is enabled.
     */
    private static boolean tagsEnabled = false;

    /**
     * The offset in blocks that the tag should be above the player name.
     */
    private static double wantedOffset;

    /**
     * The format of the wanted text.
     */
    private static String wantedText;

    /**
     * How often the text should be updated in milliseconds.
     */
    private static long wantedTextUpdateInterval;

    /**
     * How often the visibility of the wanted tag should be updated in milliseconds.
     */
    private static long wantedVisibilityUpdateInterval;

    /**
     * The minimum amount of currency spent to get a wanted tag.
     */
    private static double minWanted;

    /**
     * Whether the tag should be hidden when the player is sneaking.
     */
    private static boolean hideWantedWhenSneaking;

    /**
     * Whether the tag should be hidden when the player is moving.
     */
    private static boolean hideWantedWhenMoving;

    /**
     * The current players with wanted tags.
     */
    private static final Map<UUID, WantedTags> activeTags = new HashMap<>();

    /**
     * Level of the wanted value. The level depends on how expensive the bounty is. Configured levels have text
     * attached to them to replace the {level} placeholder in the wanted tag.
     */
    private static Map<Double, String> wantedLevels = new LinkedHashMap<>();

    /**
     * Load the Wanted Tags configuration.
     * @param configuration wanted-tags configuration section.
     */
    public static void loadConfiguration(ConfigurationSection configuration) {
        boolean newTagsEnabled = configuration.getBoolean("enabled");
        if (!tagsEnabled && newTagsEnabled) {
            // enabling tags
            enableWantedTags();
        } else if (tagsEnabled && !newTagsEnabled) {
            // disabling tags
            disableWantedTags();
        }
        tagsEnabled = newTagsEnabled;
        wantedOffset = configuration.getDouble("offset");
        wantedText = configuration.getString("text");
        minWanted = configuration.getDouble("min-bounty");
        hideWantedWhenSneaking = configuration.getBoolean("hide-when-sneaking");
        wantedTextUpdateInterval = configuration.getLong("text-update-interval");
        wantedVisibilityUpdateInterval = configuration.getLong("visibility-update-interval");
        hideWantedWhenMoving = configuration.getBoolean("hide-when-moving");
        wantedLevels.clear();
        for (String key : Objects.requireNonNull(configuration.getConfigurationSection("level")).getKeys(false)) {
            try {
                double amount = NumberFormatting.tryParse(key);
                String text = configuration.getString("wanted-tag.level." + key);
                wantedLevels.put(amount, text);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().info("[NotBounties] Wanted tag level: \"" + key + "\" is not a number!");
            }
        }
        wantedLevels = sortByValue(wantedLevels);
    }

    private static Map<Double, String> sortByValue(Map<Double, String> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<Double, String>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort(Map.Entry.comparingByKey());

        // put data from sorted list to hashmap
        LinkedHashMap<Double, String> temp = new LinkedHashMap<>();
        for (Map.Entry<Double, String> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static void enableWantedTags() {
        // add wanted tags for online players with the minimum bounty amount
        for (Player wantedPlayer : Bukkit.getOnlinePlayers()) {
            Bounty bounty = BountyManager.getBounty(wantedPlayer.getUniqueId());
            if (bounty != null && bounty.getTotalBounty() >= minWanted)
                addWantedTag(wantedPlayer);
        }
    }

    public static void disableWantedTags() {
        for (Map.Entry<UUID, WantedTags> entry : activeTags.entrySet()) {
            entry.getValue().disable();
        }
        activeTags.clear();
    }

    /**
     * Get the wanted display text for a player's bounty.
     * @param player The player to parse the text for.
     * @return A string representing a wanted tag, or an empty string if the player doesn't meet the requirements.
     */
    public static String getWantedDisplayText(OfflinePlayer player) {
        Bounty bounty = BountyManager.getBounty(player.getUniqueId());
        if (bounty == null || bounty.getTotalDisplayBounty() < minWanted)
            return "";
        String levelReplace = "";
        for (Map.Entry<Double, String> entry : wantedLevels.entrySet()) {
            if (entry.getKey() <= bounty.getTotalDisplayBounty()) {
                levelReplace = entry.getValue();
            } else {
                break;
            }
        }
        return LanguageOptions.parse(wantedText.replace("{level}", (levelReplace)), bounty.getTotalDisplayBounty(), player);
    }

    /**
     * Whether the Wanted Tags feature is enabled.
     * @return True if wanted tags are enabled.
     */
    public static boolean isEnabled() {
        return tagsEnabled;
    }

    /**
     * Get the minimum bounty that a player must have to get a wanted tag.
     * @return the minimum bounty that a player must have to get a wanted tag.
     */
    public static double getMinWanted() {
        return minWanted;
    }


    public static void removeWantedTag(UUID uuid) {
        if (!activeTags.containsKey(uuid))
            return;
        activeTags.get(uuid).disable();
        activeTags.remove(uuid);
    }

    public static void addWantedTag(Player player) {
        if (activeTags.containsKey(player.getUniqueId())) {
            activeTags.get(player.getUniqueId()).disable();
        }
        activeTags.put(player.getUniqueId(), new WantedTags(player));
    }

    public static List<Location> getLastLocations() {
        return activeTags.values().stream().map(WantedTags::getLastLocation).toList();
    }

    public static void update() {
        for (WantedTags wantedTags : activeTags.values()) {
            wantedTags.updateArmorStand();
        }
    }

    /**
     * The player that this wanted tag is tracking.
     */
    private final Player player;
    /**
     * The wanted tag entity.
     */
    private ArmorStand armorStand = null;
    /**
     * The last location of the wanted tag.
     * This is for saving the last location of the tag on shutdown.
     */
    private Location lastLocation = null;
    /**
     * The time in milliseconds when the text should update next.
     */
    private long nextTextUpdateTime = 0;
    /**
     * The time in milliseconds when the visibility should update next.
     */
    private long nextVisibilityUpdateTime = 0;
    /**
     * Whether this specific wanted tag is enabled or not.
     * A tag is disabled if the player is offline.
     */
    private boolean enabled = true;
    /**
     * The last location of the player.
     * This is used to check if the player is moving.
     */
    private Location lastPlayerLocation;
    private String displayText;
    
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

    private boolean shouldHide() {
        return (hideWantedWhenSneaking && player.isSneaking())
                || (hideWantedWhenMoving && hasMoved())
                || player.getGameMode().equals(GameMode.SPECTATOR)
                || player.isInvisible()
                || isVanished(player)
                || !BountyManager.hasBounty(player.getUniqueId());
    }

    private void updateText() {
        if (nextTextUpdateTime < System.currentTimeMillis()) {
            // check if the name should be changed
            displayText = getWantedDisplayText(player);
            if (!displayText.equalsIgnoreCase(armorStand.getCustomName())) {
                armorStand.setCustomName(displayText);
            }
            nextTextUpdateTime = System.currentTimeMillis() + wantedTextUpdateInterval;
        }
        if (NotBounties.getServerVersion() >= 17 && player.canSee(armorStand))
            player.hideEntity(NotBounties.getInstance(), armorStand);
        // a fix for 1.16, the random is to help with performance
        if (NotBounties.getServerVersion() <= 16 && Math.random() <= 0.5) {
            armorStand.setVisible(true);
            armorStand.setVisible(false);
            armorStand.setMarker(false);
            armorStand.setMarker(true);
            armorStand.setCustomName("This is a bug!");
            armorStand.setCustomName(displayText);
            armorStand.setCustomNameVisible(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setCollidable(true);
            armorStand.setCollidable(false);
        }
    }

    public void updateArmorStand(){
        if (!enabled || player == null || !player.isOnline()) {
            removeStand();
            return;
        }

        if (nextVisibilityUpdateTime < System.currentTimeMillis()) {
            nextVisibilityUpdateTime = System.currentTimeMillis() + wantedVisibilityUpdateInterval;
            // conditions if the tag should be removed/invisible
            if (shouldHide()) {
                if (armorStand != null)
                    removeStand();
                return;
            }
            if (armorStand == null) {
                spawnWantedTag();
            }
            updateText();
        }


        if (armorStand != null) {
            // I think this just loads the chunk anyway and always is true (which probably is a good thing)
            if (armorStand.getLocation().getChunk().isLoaded()) {
                teleport();
            }
        }
    }

    private void teleport() {
        armorStand.teleport(player.getEyeLocation().add(new Vector(0, wantedOffset, 0)));
    }

    public void disable() {
        enabled = false;
        removeStand();
    }

    private void removeStand(){
        if (armorStand == null)
            return;
        lastLocation = armorStand.getLocation();
        armorStand.setInvulnerable(false);
        armorStand.setCollidable(true);
        armorStand.setVisible(true);
        armorStand.remove();
        armorStand = null;
    }

    private void spawnWantedTag() {
        armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getEyeLocation().add(0,wantedOffset,0), EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setMarker(true);
        displayText = getWantedDisplayText(player);
        armorStand.setCustomName(displayText);
        armorStand.setCustomNameVisible(true);
        armorStand.setAI(false);
        armorStand.setCollidable(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setInvulnerable(true);
        armorStand.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);
        if (NotBounties.getServerVersion() >= 17)
            player.hideEntity(NotBounties.getInstance(), armorStand);
        if (lastLocation == null)
            lastLocation = armorStand.getLocation();
    }

    public Location getLastLocation() {
        if (armorStand != null)
            lastLocation = armorStand.getLocation();
        return lastLocation;
    }





}

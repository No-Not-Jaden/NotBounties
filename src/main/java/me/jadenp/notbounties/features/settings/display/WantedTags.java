package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.display.packets.FakeArmorStand;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

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
     * Whether you can see your own wanted tag above your head.
     */
    private static boolean showOwn;

    /**
     * The current players with wanted tags.
     */
    private static final Map<UUID, WantedTags> activeTags = new HashMap<>();

    /**
     * Level of the wanted value. The level depends on how expensive the bounty is. Configured levels have a String
     * to replace the {level} placeholder in the wanted tag.
     */
    private static Map<Double, String> wantedLevels = new LinkedHashMap<>();

    /**
     * Load the Wanted Tags configuration.
     * @param configuration wanted-tags configuration section.
     */
    public static void loadConfiguration(ConfigurationSection configuration, Plugin plugin) {
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
        showOwn = configuration.getBoolean("show-own");

        wantedLevels.clear();
        for (String key : Objects.requireNonNull(configuration.getConfigurationSection("level")).getKeys(false)) {
            try {
                double amount = NumberFormatting.tryParse(key);
                String text = configuration.getString("level." + key);
                if (text != null) {
                    wantedLevels.put(amount, text);
                } else {
                    plugin.getLogger().log(Level.INFO, "Wanted tag level: \"{0}\" does not have the proper format!", key);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().log(Level.INFO, "Wanted tag level: \"{0}\" is not a number!", key);
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

        // put data from a sorted list to hashmap
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
        return LanguageOptions.parse(wantedText.replace("{level}", levelReplace), bounty.getTotalDisplayBounty(), player);
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

    public static double getWantedOffset() {
        return wantedOffset;
    }

    public static boolean isShowOwn() {
        return showOwn;
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
            if (wantedTags.player != null && wantedTags.player.isOnline()) {
                NotBounties.getServerImplementation().entity(wantedTags.player).run(wantedTags::updateArmorStand);
            } else {
                wantedTags.updateArmorStand();
            }

        }
    }

    public static Set<UUID> getTagUUIDs() {
        HashSet<UUID> uuids = new HashSet<>();
        for (WantedTags wantedTags : activeTags.values()) {
            if (wantedTags.tag != null && wantedTags.tag.getTagUUID() != null) {
                uuids.add(wantedTags.tag.getTagUUID());
            }
        }
        return uuids;
    }

    /**
     * The player that this wanted tag is tracking.
     */
    private final Player player;
    /**
     * The wanted tag entity.
     */
    private final TagProvider tag;
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
    /**
     * The text being displayed in the wanted tag.
     */
    private String displayText;
    
    public WantedTags(Player player) {
        this.player = player;
        lastPlayerLocation = player.getLocation();
        if (NotBounties.isAboveVersion(19, 3)) {
            tag = new TextDisplayTag(player);
        } else if (ConfigOptions.getIntegrations().isPacketEventsEnabled()) {
            tag = new FakeArmorStand(player); // packed based tag
        } else {
            tag = new EntityTag(player); // entity-based tag
        }
        if (!BountyManager.hasBounty(player.getUniqueId()))
            return;
        spawnWantedTag();
    }

    private boolean hasMoved() {
        boolean moved = !player.getWorld().equals(lastPlayerLocation.getWorld()) || player.getEyeLocation().distance(lastPlayerLocation) > 0.025;
        if (moved)
            lastPlayerLocation = player.getEyeLocation();
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
            if (!displayText.equalsIgnoreCase(tag.getText())) {
                tag.setText(displayText);
            }
            nextTextUpdateTime = System.currentTimeMillis() + wantedTextUpdateInterval;
            tag.updateVisibility();
        }
    }

    public void updateArmorStand(){
        if (!enabled || player == null || !player.isOnline()) {
            tag.remove();
            return;
        }

        if (nextVisibilityUpdateTime < System.currentTimeMillis()) {
            nextVisibilityUpdateTime = System.currentTimeMillis() + wantedVisibilityUpdateInterval;
            // conditions if the tag should be removed/invisible
            if (shouldHide()) {
                if (tag.isValid())
                    tag.remove();
                return;
            }
            if (!tag.isValid()) {
                spawnWantedTag();
            }
            updateText();
        }


        if (tag.isValid()) {
            teleport();
        }
    }

    private void teleport() {
        // check if the player moved or has velocity before teleporting
        if (hasMoved() || player.getVelocity().length() > 0.1)
            tag.teleport();
    }

    public void disable() {
        enabled = false;
        try {
            tag.remove();
        } catch (NullPointerException ignored) {
            // world data isn't loaded on folia
        }
    }

    private void spawnWantedTag() {
        tag.spawn();
        displayText = getWantedDisplayText(player);
        tag.setText(displayText);
        nextTextUpdateTime = System.currentTimeMillis() + wantedTextUpdateInterval;
    }

    public Location getLastLocation() {
        return tag.getLocation();
    }
}

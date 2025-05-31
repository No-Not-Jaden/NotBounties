package me.jadenp.notbounties.features.settings.display;

import org.bukkit.Location;

public interface TagProvider {

    /**
     * Update the text on the tag.
     * @param text Text to be displayed.
     */
    void setText(String text);

    /**
     * Get the text on the tag.
     * @return Text on the tag.
     */
    String getText();

    /**
     * Update the visibility of the tag. This is run every update interval.
     */
    void updateVisibility();

    /**
     * Teleport the tag to the player.
     */
    void teleport();

    /**
     * Remove the tag.
     */
    void remove();

    /**
     * Spawn to tag in.
     */
    void spawn();

    /**
     * Get the location of the tag.
     * @return The location of the tag.
     */
    Location getLocation();

    /**
     * Check if the tag is being used.
     * @return True if the tag is valid.
     */
    boolean isValid();
}

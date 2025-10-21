package me.jadenp.notbounties.features.settings.display;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class TagProvider {

    protected String text;
    protected Location lastLocation;
    protected final Player trackedPlayer;

    protected TagProvider(Player trackedPlayer) {
        this.trackedPlayer = trackedPlayer;
    }

    /**
     * Update the text on the tag.
     * @param text Text to be displayed.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the text on the tag.
     * @return Text on the tag.
     */
    public String getText() {
        return text;
    }

    /**
     * Update the visibility of the tag. This is run every update interval.
     */
    public abstract void updateVisibility();

    /**
     * Teleport the tag to the player.
     */
    public abstract void teleport();

    /**
     * Remove the tag.
     */
    public abstract void remove();

    /**
     * Spawn to tag in.
     */
    public abstract void spawn();

    /**
     * Get the location of the tag.
     * @return The location of the tag.
     */
    public Location getLocation() {
        return lastLocation;
    }

    /**
     * Check if the tag is being used.
     * @return True if the tag is valid.
     */
    public abstract boolean isValid();

    public abstract @Nullable UUID getTagUUID();
}

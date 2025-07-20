package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class TextDisplayTag extends TagProvider {

    private TextDisplay textDisplay = null;

    protected TextDisplayTag(Player trackedPlayer) {
        super(trackedPlayer);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        textDisplay.setText(text);
    }

    @Override
    public String getText() {
        return textDisplay.getText();
    }

    @Override
    public void updateVisibility() {
        if (trackedPlayer.canSee(textDisplay) && !WantedTags.isShowOwn()) {
            trackedPlayer.hideEntity(NotBounties.getInstance(), textDisplay);
        }
    }

    @Override
    public void teleport() {
        Location spawnLocation = trackedPlayer.getEyeLocation().clone().add(0, WantedTags.getWantedOffset() + 0.3, 0);
        spawnLocation.setPitch(0);
        spawnLocation.setYaw(0);
        NotBounties.getServerImplementation().global().runDelayed(() -> {
            if (isValid()) {
                textDisplay.teleport(spawnLocation);
                lastLocation = spawnLocation;
            }
        }, 3);

    }

    @Override
    public void remove() {
        if (textDisplay != null) {
            textDisplay.remove();
            textDisplay = null;
        }

    }

    @Override
    public void spawn() {

        Location spawnLocation = trackedPlayer.getEyeLocation().add(0, WantedTags.getWantedOffset() + 0.3, 0);
        spawnLocation.setPitch(0);
        spawnLocation.setYaw(0);
        textDisplay = trackedPlayer.getWorld().spawn(spawnLocation, TextDisplay.class);
        //textDisplay.setTransformation(new Transformation(new Vector3f(0f, (float) WantedTags.getWantedOffset() + 0.15f, 0f), new AxisAngle4f(0f, 0f, 0f, 1f), new Vector3f(1f, 1f, 1f), new AxisAngle4f(0f, 0f, 0f, 1f)));
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(64, 0, 0, 0));
        textDisplay.setSeeThrough(false);
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setBillboard(Display.Billboard.VERTICAL);
        textDisplay.setInterpolationDuration(10);
        textDisplay.setInterpolationDelay(0);
        if (!WantedTags.isShowOwn()) {
            trackedPlayer.hideEntity(NotBounties.getInstance(), textDisplay);
        }
        lastLocation = textDisplay.getLocation();

        textDisplay.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);
    }

    @Override
    public boolean isValid() {
        return textDisplay != null;
    }
}

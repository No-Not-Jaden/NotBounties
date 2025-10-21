package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

public class TextDisplayTag extends TagProvider {

    private TextDisplay textDisplay = null;
    private boolean toBeRemoved = false;

    protected TextDisplayTag(Player trackedPlayer) {
        super(trackedPlayer);
    }

    @Override
    public void setText(String text) {
        if (!isValid())
            return;
        if (!NotBounties.getServerImplementation().isOwnedByCurrentRegion(textDisplay)) {
            NotBounties.getServerImplementation().entity(textDisplay).run(() -> textDisplay.setText(text));
        } else {
            textDisplay.setText(text);
        }
        super.setText(text);

    }

    @Override
    public void updateVisibility() {
        if (!isValid())
            return;
        if (!NotBounties.getServerImplementation().isOwnedByCurrentRegion(textDisplay)) {
            NotBounties.getServerImplementation().entity(textDisplay).run(() -> {
                if (trackedPlayer.canSee(textDisplay) && !WantedTags.isShowOwn()) {
                    trackedPlayer.hideEntity(NotBounties.getInstance(), textDisplay);
                }
            });
        } else {
            if (trackedPlayer.canSee(textDisplay) && !WantedTags.isShowOwn()) {
                trackedPlayer.hideEntity(NotBounties.getInstance(), textDisplay);
            }
        }

    }

    @Override
    public void teleport() {
        if (!isValid())
            return;
        Location spawnLocation = trackedPlayer.getEyeLocation().clone().add(0, WantedTags.getWantedOffset() + 0.3, 0);
        spawnLocation.setPitch(0);
        spawnLocation.setYaw(0);
        NotBounties.getServerImplementation().entity(textDisplay).run(() -> {
            if (isValid()) {
                NotBounties.getServerImplementation().teleportAsync(textDisplay, spawnLocation).thenRun(() -> lastLocation = spawnLocation);
            }
        });

    }

    @Override
    public void remove() {
        if (isValid()) {
            if (!NotBounties.getServerImplementation().isOwnedByCurrentRegion(textDisplay) && NotBounties.getInstance().isEnabled()) {
                NotBounties.getServerImplementation().entity(textDisplay).run(() -> {
                    textDisplay.remove();
                    toBeRemoved = true;
                });
            } else {
                textDisplay.remove();
                toBeRemoved = true;
            }

        }

    }

    @Override
    public void spawn() {
        NotBounties.getServerImplementation().entity(trackedPlayer).run(() -> {
            if (isValid()) {
                return;
            }
            toBeRemoved = false;
            Location spawnLocation = trackedPlayer.getEyeLocation().add(0, WantedTags.getWantedOffset() + 0.3, 0);
            spawnLocation.setPitch(0);
            spawnLocation.setYaw(0);
            textDisplay = Objects.requireNonNull(spawnLocation.getWorld()).spawn(spawnLocation, TextDisplay.class);
            //textDisplay.setTransformation(new Transformation(new Vector3f(0f, (float) WantedTags.getWantedOffset() + 0.15f, 0f), new AxisAngle4f(0f, 0f, 0f, 1f), new Vector3f(1f, 1f, 1f), new AxisAngle4f(0f, 0f, 0f, 1f)));
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(64, 0, 0, 0));
            textDisplay.setSeeThrough(false);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            try {
                textDisplay.setTeleportDuration(2);
                textDisplay.setInterpolationDelay(0);
                textDisplay.setInterpolationDuration(10);
            } catch (NoSuchMethodError ignored) {
                // using an older server version
            }
            if (text != null && !text.isEmpty()) {
                textDisplay.setText(text);
            }
            if (!WantedTags.isShowOwn()) {
                trackedPlayer.hideEntity(NotBounties.getInstance(), textDisplay);
            }
            lastLocation = textDisplay.getLocation();

            textDisplay.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);


        });

    }

    @Override
    public boolean isValid() {
        if (textDisplay == null)
            return false;
        if (toBeRemoved) {
            try {
                if (!textDisplay.isValid()) {
                    textDisplay = null;
                } else {
                    textDisplay.remove();
                }
            } catch (NullPointerException ignored) {
                // won't be able to check if the display is valid if on Folia and not in the same region
                // validity checks will be made later on the correct region
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public UUID getTagUUID() {
        if (textDisplay == null)
            return null;
        return textDisplay.getUniqueId();
    }
}

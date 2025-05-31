package me.jadenp.notbounties.features.settings.display.map;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class HologramRenderer extends BountyPosterProvider {

    private static final int WIDTH_PIXELS = 128;
    private static final int HEIGHT_PIXELS = 128;
    private static final double WIDTH_BLOCKS = 1.0;
    private static final double HEIGHT_BLOCKS = 1.0;
    private static final Vector UP = new Vector(0, 1, 0);

    private final Location location;
    private final Vector direction;
    private final Display[][] pixels = new Display[WIDTH_PIXELS][HEIGHT_PIXELS];

    public HologramRenderer(UUID uuid, Plugin plugin, Location location, Vector direction) {
        super(uuid, plugin);
        this.location = location.getBlock().getLocation().add(0.5, 0.5, 0.5); // get center block location
        this.direction = new Vector(direction.getX(), 0, direction.getZ()).normalize();
    }

    private boolean colorsEqual(Color awtColor, org.bukkit.Color bukkitColor) {
        if (awtColor == null && bukkitColor == null)
            return true;
        if (awtColor == null || bukkitColor == null)
            return false;
        return awtColor.getRed() == bukkitColor.getRed() && awtColor.getBlue() == bukkitColor.getBlue()
                && awtColor.getGreen() == bukkitColor.getGreen() && awtColor.getAlpha() == bukkitColor.getAlpha();
    }

    @Override
    protected void setPixel(int x, int y, Color color) {
        if (pixels[x][y] != null) {
            if (colorsEqual(color, pixels[x][y].getGlowColorOverride())) {
                return;
            } else {
                pixels[x][y].remove();
            }
        }

        double xOffset = (WIDTH_BLOCKS / WIDTH_PIXELS) * (x - WIDTH_PIXELS / 2.0);
        double yOffset = (HEIGHT_BLOCKS / HEIGHT_PIXELS) * (y - HEIGHT_PIXELS / 2.0);
        // x offset needs to be converted to x and z blocks perpendicular to direction vector
        Vector offsetVector = UP.clone().crossProduct(direction).normalize().multiply(xOffset).subtract(new Vector(0, yOffset, 0));
        // move location back to the edge of the block, and over to the correct pixel position
        Location pixelLocation = location.clone().add(direction.clone().multiply(0.5)).add(offsetVector);
        // set the correct yaw for the location, so it faces the right way
        pixelLocation.setYaw((float) Math.toDegrees((Math.atan2(-direction.getX(), direction.getZ()) + 2 * Math.PI) % (2 * Math.PI)));

        // could try item or block display, or make my own interface
        TextDisplay textDisplay;
        textDisplay = (TextDisplay) Objects.requireNonNull(pixelLocation.getWorld()).spawnEntity(pixelLocation, EntityType.TEXT_DISPLAY);
        org.bukkit.Color bukkitColor = org.bukkit.Color.fromARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
        textDisplay.setBackgroundColor(bukkitColor);
        textDisplay.setGlowColorOverride(bukkitColor);
        textDisplay.setBillboard(Display.Billboard.FIXED);
        // maybe?
        textDisplay.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f), new AxisAngle4f(0f, 0f, 0f, 1f), new Vector3f(0.05f, 0.025f, 0.1f), new AxisAngle4f(0f, 0f, 0f, 1f)));
        textDisplay.setRotation(pixelLocation.getYaw(), pixelLocation.getPitch());
        textDisplay.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);
        textDisplay.setSeeThrough(false);
        textDisplay.setText(" ");

        pixels[x][y] = textDisplay;
    }

    @Override
    protected void drawRectangle(int x, int y, int w, int h, Color color) {
        // assuming the anchor point is the top left of a display
        if (pixels[x][y] != null && colorsEqual(color, pixels[x][y].getGlowColorOverride())) {
            return;
        }
        // remove previous pixels in square
        for (int ix = x; ix < x + w; ix++) {
            for (int iy = y; iy < y + h; iy++) {
                if (pixels[x][y] != null) {
                    pixels[x][y].remove();
                    pixels[x][y] = null;
                }
            }
        }
        setPixel(x, y, color);
        pixels[x][y].setTransformation(new Transformation(new Vector3f(0f, 0f, 0f), new AxisAngle4f(0f, 0f, 0f, 1f), new Vector3f(0.05f * w, 0.025f * h, 0.1f), new AxisAngle4f(0f, 0f, 0f, 1f)));
    }

    @Override
    protected @Nullable Color getPixelColor(int x, int y) {
        if (pixels[x][y] == null)
            return null;
        org.bukkit.Color color = pixels[x][y].getGlowColorOverride();
        if (color == null)
            return null;
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()) ;
    }

    public void remove() {
        for (Display[] displays : pixels)
            for (Display display : displays)
                if (display != null)
                    display.remove();
    }
}

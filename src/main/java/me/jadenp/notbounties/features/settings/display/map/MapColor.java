package me.jadenp.notbounties.features.settings.display.map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.jadenp.notbounties.NotBounties;
import org.bukkit.Bukkit;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Draw colors to maps.
 * Note: This class uses depreciated methods of Bukkit's MapPalette to get the current color palette and to support
 *       older versions. There is no replacement methods for the used methods as of writing this.
 */
public class MapColor {

    /**
     * @param ratio 0 for pure bukkitIndexA, 1 for pure bukkitIndexB
     * @param color apparent color
     */
    public record ColorEntry(byte bukkitIndexA, byte bukkitIndexB, double ratio, Color color) { }

    private final Cache<Color, ColorEntry> colorCache;
    private List<ColorEntry> palette; // palette of blendable colors
    private boolean paletteGenerated = false;
    private static int blends; // number of blendable combinations of 2 colors (not including pure colors)
    private static int maxColorDistance;

    public MapColor() {
        colorCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
        // build palette
        List<Byte> indexes = new ArrayList<>(); // valid color indexes
        List<Color> colors = new ArrayList<>(); // valid Bukkit colors
        NotBounties.getServerImplementation().async().runNow(() -> {
            getAllValidBukkitColors(indexes, colors);
            // each combination of colors * # of blends per color + original color size
            palette = new ArrayList<>((indexes.size() * indexes.size() / 2) * blends + indexes.size());
            // add Bukkit colors to palette
            for (int i = 0; i < indexes.size(); i++) {
                palette.add(new ColorEntry(indexes.get(i), indexes.get(i), 0.0, colors.get(i)));
            }
            // mix colors
            for (int i = 0; i < indexes.size(); i++) {
                byte index1 = indexes.get(i);
                Color bukkitColor1 = colors.get(i);
                for (int j = i + 1; j < indexes.size(); j++) {
                    byte index2 = indexes.get(j);
                    Color bukkitColor2 = colors.get(j);
                    if (maxColorDistance == 0 || getDistance(bukkitColor1, bukkitColor2) < maxColorDistance) {
                        for (int step = 1; step < blends + 1; step++) {
                            double ratio = step / (double) (blends + 1);
                            Color mixedColor = mix(bukkitColor1, bukkitColor2, ratio);
                            palette.add(new ColorEntry(index1, index2, ratio, mixedColor));
                        }
                    }
                }
            }

            paletteGenerated = true;
        });
    }

    private static void getAllValidBukkitColors(List<Byte> indexes, List<Color> colors) {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            try {
                @SuppressWarnings("deprecation") Color bukkitColor = MapPalette.getColor(b);
                indexes.add(b);
                colors.add(bukkitColor);
            } catch (ArrayIndexOutOfBoundsException e) {
                // not a valid color
            }
        }
    }

    /**
     * Mixes 2 colors together.
     * The resulting color is the ratio of bukkitColor2 added to the inverse ratio of bukkitColor1.
     * The ratio is for the second color.
     * @param bukkitColor1 First color to mix.
     * @param bukkitColor2 Second color to mix.
     * @param ratio Ratio of second color to first color.
     * @return The new mixed color.
     */
    private static Color mix(Color bukkitColor1, Color bukkitColor2, double ratio) {
        int r = (int) Math.round(bukkitColor1.getRed()   * (1 - ratio) + bukkitColor2.getRed()   * ratio);
        int g = (int) Math.round(bukkitColor1.getGreen() * (1 - ratio) + bukkitColor2.getGreen() * ratio);
        int bl= (int) Math.round(bukkitColor1.getBlue()  * (1 - ratio) + bukkitColor2.getBlue()  * ratio);
        return new Color(r, g, bl);
    }

    /**
     * Get the perceived distance between colors.
     * This is copied directly from the Spigot/Bukkit source code for how they calculate color distances.
     * <a href="https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/map/MapPalette.java">Source</a>
     * @param c1 First color to compare distance.
     * @param c2 Second color to compare distance.
     * @return The squared distance between the colors adjusted for the sensitivity of the color.
     */
    private static double getDistance(@NotNull Color c1, @NotNull Color c2) {
        double rmean = (c1.getRed() + c2.getRed()) / 2.0;
        double r = c1.getRed() - c2.getRed();
        double g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        double weightR = 2 + rmean / 256.0;
        double weightG = 4.0;
        double weightB = 2 + (255 - rmean) / 256.0;
        return weightR * r * r + weightG * g * g + weightB * b * b;
    }

    /**
     * Get the closest mix of 2 bukkit colors to the target color.
     * @param color Target color to mix to.
     * @return The closest color mix, or null if the palette isn't built.
     */
    private @Nullable ColorEntry getClosestColorEntry(Color color) {
        if (!isPaletteGenerated())
            return null;
        ColorEntry colorEntry = colorCache.getIfPresent(color);
        if (colorEntry != null)
            return colorEntry;
        ColorEntry closestEntry = null;
        double closestDistance = Double.MAX_VALUE;

        for (ColorEntry entry : palette) {
            double distance = getDistance(entry.color, color);
            if (distance < closestDistance) {
                closestEntry = entry;
                closestDistance = distance;
            }
        }
        if (closestEntry != null)
            colorCache.put(color, closestEntry);
        return closestEntry;
    }

    public void drawColor(int x, int y, int w, int h, Color color, MapCanvas canvas) {
        if (color.getAlpha() < 10)
            return;
        // get closest colors
        if (blends == 0 || !isPaletteGenerated()) {
            // palette not generated - this will draw the closest color
            for (int ix = x; ix < w + x; ix++) {
                for (int iy = y; iy < h + y; iy++) {
                    setPixel(ix, iy, color, canvas);
                }
            }
            return;
        }
        ColorEntry colorCombination = getClosestColorEntry(color);
        if (colorCombination == null)
            return;
        // palette has been generated
        final double spacing = 1 / colorCombination.ratio;
        for (int ix = x; ix < w + x; ix++) {
            int startOffset = ix % (blends + 1);
            for (int iy = y; iy < h + y; iy++) {
                // length * ratio = num B pixels
                // length / num pixels = spacing = 1/ratio
                @SuppressWarnings("deprecation") Color colorToUse =
                        (startOffset + (iy - y)) % spacing < 1
                                ? MapPalette.getColor(colorCombination.bukkitIndexB)
                                : MapPalette.getColor(colorCombination.bukkitIndexA);
                setPixel(ix, iy, colorToUse, canvas);
            }
        }


    }

    /**
     * Draw a color directly on the canvas.
     * @param x X position on the canvas.
     * @param y Y Position on the canvas.
     * @param color Color to draw.
     * @param canvas Canvas to draw the color on.
     */
    @SuppressWarnings("deprecation")
    public void setPixel(int x, int y, Color color, MapCanvas canvas) {
        if (color.getAlpha() > 10) {
            if (NotBounties.getServerVersion() >= 19)
                canvas.setPixelColor(x, y, color);
            else
                canvas.setPixel(x, y, MapPalette.matchColor(color));
        }
    }

    public boolean isPaletteGenerated() {
        return paletteGenerated;
    }

    public static void setBlends(int blends) {
        MapColor.blends = blends;
    }

    public static void setMaxColorDistance(int maxColorDistance) {
        MapColor.maxColorDistance = maxColorDistance;
    }
}

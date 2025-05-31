package me.jadenp.notbounties.features.settings.display.map;

import org.bukkit.map.MapCanvas;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;

public class MapProvider extends BountyPosterProvider{

    private MapCanvas canvas;

    protected MapProvider(UUID uuid, Plugin plugin) {
        super(uuid, plugin);
    }

    public void setCanvas(MapCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    protected void setPixel(int x, int y, Color color) {
        if (canvas != null)
            BountyMap.getMapColor().setPixel(x, y, color, canvas);
    }

    @Override
    protected void drawRectangle(int x, int y, int w, int h, Color color) {
        if (canvas != null)
            BountyMap.getMapColor().drawColor(x, y, w, h, color, canvas);
    }

    @Override
    protected @Nullable Color getPixelColor(int x, int y) {
        if (canvas == null)
            return null;
        return canvas.getPixelColor(x, y);
    }

    @Override
    public boolean isMissingElements() {
        return super.isMissingElements() || !BountyMap.getMapColor().isPaletteGenerated();
    }
}

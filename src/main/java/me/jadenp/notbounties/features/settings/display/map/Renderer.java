package me.jadenp.notbounties.features.settings.display.map;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Renderer extends MapRenderer {

    private final MapProvider mapProvider;

    public Renderer(UUID uuid, Plugin plugin) {
        mapProvider = new MapProvider(uuid, plugin);

    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player renderer) {
        mapProvider.setCanvas(canvas);
        mapProvider.render();
    }
}

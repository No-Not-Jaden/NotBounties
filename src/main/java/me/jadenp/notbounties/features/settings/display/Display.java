package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class Display extends ResourceConfiguration {
    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        WantedTags.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("wanted-tag")));
        BountyMap.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-posters")));
        BountyBoard.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-board")));
        BountyTracker.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-tracker")));
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[]{"wanted-tag.level"};
    }

    @Override
    protected String getPath() {
        return "settings/display.yml";
    }
}

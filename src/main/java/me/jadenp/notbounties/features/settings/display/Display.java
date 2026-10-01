package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import me.jadenp.notbounties.features.settings.databases.BountySortType;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyMap;
import me.jadenp.notbounties.ui.SkinManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class Display extends ResourceConfiguration {

    @Override
    protected void prepareConfig(YamlConfiguration config) {
        if (config.isInt("bounty-board.type")) {
            int oldType = config.getInt("bounty-board.type");
            BountySortType newType;
            switch (oldType) {
                case 0 -> newType = BountySortType.OLDEST;
                case 1 -> newType = BountySortType.NEWEST;
                case 3 -> newType = BountySortType.LOWEST;
                default -> newType = BountySortType.HIGHEST;
            }
            config.set("bounty-board.type", newType.name());
        }
    }

    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        WantedTags.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("wanted-tag")), plugin);
        BountyMap.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-posters")));
        BountyBoard.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-board")));
        BountyTracker.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-tracker")));
        BountyHunt.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-hunt")));
        SkinManager.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("skins")));
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

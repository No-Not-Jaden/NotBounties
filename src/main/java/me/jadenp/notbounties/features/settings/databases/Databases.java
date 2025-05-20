package me.jadenp.notbounties.features.settings.databases;

import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class Databases extends ResourceConfiguration {
    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        DataManager.loadDatabaseConfig(Objects.requireNonNull(config));
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[]{""};
    }

    @Override
    protected String getPath() {
        return "settings/databases.yml";
    }
}

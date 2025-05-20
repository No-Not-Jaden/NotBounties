package me.jadenp.notbounties.features.settings.immunity;

import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class Immunity extends ResourceConfiguration {
    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        ImmunityManager.loadConfiguration(Objects.requireNonNull(config));
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[0];
    }

    @Override
    protected String getPath() {
        return "settings/immunity.yml";
    }
}

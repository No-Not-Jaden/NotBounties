package me.jadenp.notbounties.features.settings;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

public abstract class ResourceConfiguration {

    private File file;
    private Plugin plugin;

    /**
     * Load the configuration of the file.
     * Intended to be overwritten by a child to read
     */
    public void loadFile(Plugin plugin) {
        this.plugin = plugin;
        String path = getPath();
        this.file = new File(plugin.getDataFolder() + File.separator + path);

        if (!file.exists()) {
            plugin.getLogger().info("Generated a new " + path + " file.");
            plugin.saveResource(path, false);
        }

        YamlConfiguration config;
        boolean saveChanges = true;
        try {
            config = getConfig();
        } catch (IOException e) {
            // YAML formatting errors
            saveChanges = false;
            if (plugin.getResource(path) != null)
                config = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(plugin.getResource(path))));
            else
                config = new YamlConfiguration();
        }

        prepareConfig(config);

        // fill in any default options that aren't present
        if (plugin.getResource(path) != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(plugin.getResource(path)))));
            for (String key : Objects.requireNonNull(config.getDefaults()).getKeys(true)) {
                if (Arrays.stream(getModifiableSections()).anyMatch(key::startsWith))
                    continue;
                if (!config.isSet(key))
                    config.set(key, config.getDefaults().get(key));
            }
        }

        loadConfiguration(config);

        if (saveChanges)
            saveConfig(config);
    }

    /**
     * Get the configuration for this file.
     * @return The configuration for this file.
     * @throws IOException When there are too few values in the configuration,
     *                     usually meaning there are YAML formatting errors
     */
    public YamlConfiguration getConfig() throws IOException {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        if (configuration.getKeys(true).size() <= 2) {
            plugin.getLogger().severe("Loaded an empty configuration for the " + getPath()
                    + " file. Fix the YAML formatting errors, or these features may not work!" +
                    "\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            throw new IOException("YAML Error");
        }
        return configuration;
    }

    /**
     * Save a configuration to the file.
     * @param configuration Configuration to save.
     */
    public void saveConfig(YamlConfiguration configuration) {
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save " + getPath() + ".");
            plugin.getLogger().warning(e.toString());
        }
    }

    /**
     * Prepare the config to be loaded. This is where changes to the config structure should take place.
     * e.g. changing a value into a configuration section. After this is called, the default values will be loaded into
     * any missing areas.
     * @param config Configuration that is being prepared.
     */
    protected void prepareConfig(YamlConfiguration config) {}

    protected abstract void loadConfiguration(YamlConfiguration config);

    protected abstract String[] getModifiableSections();

    protected abstract String getPath();
}

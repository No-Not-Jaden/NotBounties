package me.jadenp.notbounties.ui.gui.bedrock;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class BedrockGUI {


    public static boolean enabled;
    private static final Map<String, BedrockGUIOptions> customGuis = new HashMap<>();

    public static void reloadOptions() throws IOException {
        if (!getFile().exists())
            NotBounties.getInstance().saveResource("bedrock-gui.yml", false);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(getFile());
        // fill in any default options that aren't present
        if (NotBounties.getInstance().getResource("bedrock-gui.yml") != null) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("bedrock-gui.yml")))));
            for (String key : Objects.requireNonNull(configuration.getDefaults()).getKeys(true)) {
                if (!configuration.isSet(key))
                    configuration.set(key, configuration.getDefaults().get(key));
            }
            configuration.save(getFile());
        }
        enabled = configuration.getBoolean("enabled");
        // register GUIs
        customGuis.clear();
        for (String key : configuration.getKeys(false)) {
            if (key.equals("custom-components") || key.equals("enabled"))
                continue;
            customGuis.put(key, new BedrockGUIOptions(Objects.requireNonNull(configuration.getConfigurationSection(key)), configuration.getConfigurationSection("custom-components")));
        }
    }

    public static void openGUI(Player player, String guiName, long page, LinkedHashMap<UUID, String> values, String[] replacements) {
        if (!customGuis.containsKey(guiName))
            return;
        BedrockGUIOptions gui = customGuis.get(guiName);
        gui.openInventory(player, page, values, replacements);
    }

    public static boolean isGUIEnabled(String guiName) {
        if (!customGuis.containsKey(guiName))
            return false;
        return customGuis.get(guiName).isEnabled();
    }

    private static File getFile() {
        return new File(NotBounties.getInstance().getDataFolder() + File.separator + "bedrock-gui.yml");
    }
}

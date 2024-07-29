package me.jadenp.notbounties.ui.gui.bedrock;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.ui.gui.displayItems.DisplayItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class BedrockGUI {


    private static boolean enabled;
    private static final Map<String, BedrockGUIOptions> customGuis = new HashMap<>();

    private BedrockGUI(){}

    public static void reloadOptions(){
        if (!getFile().exists())
            NotBounties.getInstance().saveResource("bedrock-gui.yml", false);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(getFile());
        if (configuration.getKeys(true).size() <= 2) {
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the bedrock-gui.yml file. Fix the YAML formatting errors, or the GUI may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            if (NotBounties.getInstance().getResource("bedrock-gui.yml") != null) {
                configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("bedrock-gui.yml"))));
                loadGUI(configuration);
            }
        } else {
            loadGUI(configuration);
        }

    }

    private static void loadGUI(YamlConfiguration configuration) {
        // add options from new updates
        boolean changes = false;
        if (!configuration.isConfigurationSection("challenges")) {
            configuration.set("challenges.enabled", true);
            configuration.set("challenges.gui-name", "&c&lChallenges");
            configuration.set("challenges.override-type", "SIMPLE");
            configuration.set("challenges.components.-1", "return-selection");
            changes = true;
        }
        if (!configuration.isConfigurationSection("view-bounty")) {
            configuration.set("view-bounty.enabled", true);
            configuration.set("view-bounty.gui-name", "&d&l{player}'s &9&lBounty: &2{amount}");
            configuration.set("view-bounty.player-text", "&e{player} &7> &a{amount} &7{items}");
            configuration.set("view-bounty.components.1", "view-bounties");
            changes = true;
        }

        if (changes) {
            try {
                configuration.save(getFile());
            } catch (IOException e) {
                Bukkit.getLogger().warning("[NotBounties] Error trying to save updates to bedrock-gui.yml.");
            }
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

    public static void openGUI(Player player, String guiName, long page, List<DisplayItem> displayItems, Object[] data) {
        if (!customGuis.containsKey(guiName))
            return;
        BedrockGUIOptions gui = customGuis.get(guiName);
        String title = GUI.createTitle(gui, player, page, displayItems, data);
        gui.openInventory(player, page, displayItems, title, data);
        GUI.playerInfo.put(player.getUniqueId(), new PlayerGUInfo(page, guiName, data, displayItems, title));
    }

    public static boolean isGUIEnabled(String guiName) {
        if (!customGuis.containsKey(guiName))
            return false;
        return customGuis.get(guiName).isEnabled();
    }

    private static File getFile() {
        return new File(NotBounties.getInstance().getDataFolder() + File.separator + "bedrock-gui.yml");
    }

    public static boolean isEnabled() {
        return enabled;
    }
}

package me.jadenp.notbounties.ui.gui.bedrock;

import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.cumulus.component.Component;
import org.geysermc.cumulus.component.impl.*;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.cumulus.util.impl.FormImageImpl;

import java.util.*;

public class GUIComponent {
    public enum ComponentType {
        BUTTON, DROPDOWN, INPUT, LABEL, SLIDER, STEP_SLIDER, TOGGLE
    }
    private final String name;
    private String text = "";
    private Component component = null;
    private ButtonComponent buttonComponent = null;
    private List<String> commands = new ArrayList<>();
    private List<String> listOptions = new ArrayList<>();
    private ComponentType type;
    private Map<String, String> options = new HashMap<>();

    public GUIComponent(ConfigurationSection configuration) {
        name = configuration.getName();
        if (configuration.isSet("text"))
            text = configuration.getString("text");
        // this shouldn't be needed
        if (text == null)
            text = "";
        if (configuration.isList("commands"))
            commands = configuration.getStringList("commands");
        if (configuration.isList("options"))
            listOptions = configuration.getStringList("options");
        type = ComponentType.LABEL;
        try {
            type = ComponentType.valueOf(Objects.requireNonNull(configuration.getString("type")).toUpperCase());
        } catch (NullPointerException | IllegalFormatException e) {
            Bukkit.getLogger().warning("[NotBounties] Invalid or no type for component \"" + name + "\" in bedrock-gui.yml");
        }

        for (String key : configuration.getKeys(false)) {
            if (key.equals("text") || key.equals("commands") || key.equals("type"))
                continue;
            if (configuration.isString("key")) {
                options.put(key, configuration.getString(key));
            }
        }
    }

    private GUIComponent(String name, String text, Component component, ButtonComponent buttonComponent, List<String> commands, List<String> listOptions, ComponentType type, Map<String, String> options) {
        this.name = name;
        this.text = text;
        this.component = component;
        this.buttonComponent = buttonComponent;
        this.commands = commands;
        this.listOptions = listOptions;
        this.type = type;
        this.options = options;
    }

    public List<String> getCommands() {
        return commands;
    }

    public ComponentType getType() {
        return type;
    }

    public GUIComponent buildComponent(Player player) {
        String text = LanguageOptions.parse(this.text, player);
        switch (type) {
            case LABEL:
                component = new LabelComponentImpl(text);
                break;
            case BUTTON:
                FormImageImpl formImage = null;
                if (options.containsKey("image") && !options.get("image").isEmpty())
                    formImage = new FormImageImpl(FormImage.Type.URL, LanguageOptions.parse(options.get("image"), player));
                buttonComponent = new ButtonComponentImpl(text, formImage);
                component = new LabelComponentImpl(text);
                break;
            case INPUT:
                String placeholder = "";
                if (options.containsKey("placeholder"))
                    placeholder = options.get("placeholder");
                assert placeholder != null;
                component = new InputComponentImpl(text, LanguageOptions.parse(placeholder, player),"");
                break;
            case SLIDER:
                float min = 0;
                float max = 100;
                float step = 1;
                float defaultValue;
                if (options.containsKey("min")) {
                    try {
                        min = (float) NumberFormatting.tryParse(LanguageOptions.parse(options.get("min"), player));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotBounties] Could not parse a min value for slider component \"" + name + "\" in bedrock-gui.yml");
                    }
                }
                if (options.containsKey("max")) {
                    try {
                        max = (float) NumberFormatting.tryParse(LanguageOptions.parse(options.get("max"), player));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotBounties] Could not parse a max value for slider component \"" + name + "\" in bedrock-gui.yml");
                    }
                }
                if (options.containsKey("step")) {
                    try {
                        step = (float) NumberFormatting.tryParse(LanguageOptions.parse(options.get("step"), player));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotBounties] Could not parse a step value for slider component \"" + name + "\" in bedrock-gui.yml");
                    }
                }
                if (options.containsKey("default")) {
                    try {
                        defaultValue = (float) NumberFormatting.tryParse(LanguageOptions.parse(options.get("default"), player));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotBounties] Could not parse a default value for slider component \"" + name + "\" in bedrock-gui.yml");
                        defaultValue = min;
                    }
                } else {
                    defaultValue = min;
                }
                component = new SliderComponentImpl(text, min, max, step, defaultValue);
                break;
            case STEP_SLIDER:
                List<String> parsedSteps = new ArrayList<>();
                for (String str : listOptions)
                    parsedSteps.add(LanguageOptions.parse(str, player));
                int defaultStep = 1;
                if (options.containsKey("default-step")) {
                    try {
                        defaultStep = Integer.parseInt(options.get("default-step"));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotBounties] Could not parse a default slider value for step slider component \"" + name + "\" in bedrock-gui.yml");
                    }
                }
                component = new StepSliderComponentImpl(text,parsedSteps,defaultStep);
                break;
            case TOGGLE:
                boolean defaultToggle = false;
                if (options.containsKey("default")) {
                    defaultToggle = options.get("default").equalsIgnoreCase("true") || options.get("default").equalsIgnoreCase("on");
                }
                component = new ToggleComponentImpl(text, defaultToggle);
                break;
            case DROPDOWN:
                List<String> parsedOptions = new ArrayList<>();
                for (String str : listOptions)
                    parsedOptions.add(LanguageOptions.parse(str, player));
                component = new DropdownComponentImpl(text, parsedOptions, 0);
                break;

        }
        return this;
    }

    public GUIComponent copy(){
        return new GUIComponent(name, text, component, buttonComponent, commands, listOptions, type, options);
    }

    public List<String> getListOptions() {
        return listOptions;
    }

    public ButtonComponent getButtonComponent() {
        return buttonComponent;
    }

    public Component getComponent() {
        return component;
    }
}

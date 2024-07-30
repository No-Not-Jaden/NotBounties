package me.jadenp.notbounties.ui.gui.bedrock;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIClicks;
import me.jadenp.notbounties.ui.gui.displayItems.AmountItem;
import me.jadenp.notbounties.ui.gui.displayItems.DisplayItem;
import me.jadenp.notbounties.ui.gui.displayItems.PlayerItem;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.ActionCommands;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.GeyserMCClass;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.component.impl.DropdownComponentImpl;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.ModalFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.cumulus.util.impl.FormImageImpl;

import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.ui.gui.GUIOptions.getPageType;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.*;

public class BedrockGUIOptions {
    private final String type;
    private final String name;
    private final boolean addPage;
    private final int sortType;
    private final boolean removePageItems;
    private final boolean enabled;
    private final int maxPlayers;
    private final String playerText;
    private final List<String> formCompletionCommands;
    private final List<String> playerButtonCommands;
    private LinkedHashMap<Integer, GUIComponent> components = new LinkedHashMap<>();

    private enum GUIType {
        CUSTOM, SIMPLE, MODAL
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isAddPage() {
        return addPage;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    private final GUIType guiType;

    public BedrockGUIOptions(ConfigurationSection settings, ConfigurationSection customItems) {
        type = settings.getName();
        name = settings.isSet("gui-name") ? color(settings.getString("gui-name")) : "Custom GUI";
        addPage = settings.isSet("add-page") && settings.getBoolean("add-page");
        sortType = settings.isSet("sort-type") ? settings.getInt("sort-type") : 1;
        removePageItems = settings.getBoolean("remove-page-items");
        enabled = !settings.isSet("enabled") || settings.getBoolean("enabled");
        maxPlayers = settings.isSet("max-players") ? settings.getInt("max-players") : 9999;
        playerText = settings.isSet("player-text") ? settings.getString("player-text") : null;
        formCompletionCommands = settings.isSet("completion-commands") ? settings.getStringList("completion-commands") : new ArrayList<>();
        playerButtonCommands = settings.isSet("player-button-commands") ? settings.getStringList("player-button-commands") : new ArrayList<>();
        GUIType overrideType = null;
        if (settings.isString("override-type"))
            try {
                overrideType = GUIType.valueOf(Objects.requireNonNull(settings.getString("override-type")).toUpperCase());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotBounties] Invalid GUI override type in bedrock-gui \"" + name + "\": " + settings.getString("override-type"));
            }

        int buttonCount = 0;
        if (settings.isConfigurationSection("components"))
            for (String key : Objects.requireNonNull(settings.getConfigurationSection("components")).getKeys(false)) {
                int order;
                try {
                    order = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    order = key.charAt(0);
                }

                String componentID = settings.getString("components." + key);
                if (componentID != null)
                    if (customItems.isConfigurationSection(componentID)) {
                        GUIComponent component = new GUIComponent(Objects.requireNonNull(customItems.getConfigurationSection(componentID)));
                        if (component.getType() == GUIComponent.ComponentType.BUTTON)
                            buttonCount++;
                        components.put(order, component);
                    } else {
                        Bukkit.getLogger().warning("[NotBounties] Could not find custom item \"" + componentID + "\" in bedrock-gui.yml");
                    }
            }

        if (overrideType != null) {
            guiType = overrideType;
        } else {
            if (buttonCount > 2) {
                // simple form
                guiType = GUIType.SIMPLE;
            } else if (buttonCount > 0) {
                // modal form
                guiType = GUIType.MODAL;
            } else {
                // custom form
                guiType = GUIType.CUSTOM;
            }
        }

        components = sortByKey(components);


    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSortType() {
        return sortType;
    }

    private List<GUIComponent> addPlayerComponents(SimpleForm.Builder builder, Player player, long page, List<DisplayItem> displayItems, Object[] data) {
        // keeping track of added components
        List<GUIComponent> playerComponents = new ArrayList<>();
        // iterate through players to add
        // the number of components added is limited to maxPlayers
        // the start position in the playerItems and amount arrays depend on the page
        for (int i = type.equals("select-price") || type.equals("confirm-bounty") ? 0 : (int) ((page - 1) * maxPlayers); i < Math.min(maxPlayers * page, displayItems.size()); i++) {
            // get display item
            DisplayItem displayItem = displayItems.get(i);
            // get the player text (will be parsed already)
            String parsedPlayerText = displayItem.parseText(playerText, player);
            // set default player uuid for GUIComponent
            UUID p = player.getUniqueId();
            // set default amount for GUIComponent
            double amount = 0;
            if (displayItem instanceof PlayerItem playerItem) {
                // default texture id (question mark head)
                String imageTextureID = "46ba63344f49dd1c4f5488e926bf3d9e2b29916a6c50d610bb40a5273dc8c82";

                if (SkinManager.isSkinLoaded(playerItem.getUuid()))
                    imageTextureID = SkinManager.getSkin(playerItem.getUuid()).getId();
                // perspective head url
                String imageURL = "https://mc-heads.net/head/" + imageTextureID + ".png";
                // add button to the component
                builder.button(parsedPlayerText, new FormImageImpl(FormImage.Type.URL, imageURL));
                p = playerItem.getUuid();
            } else {
                builder.button(parsedPlayerText);
            }
            // check if this item has an amount
            if (displayItem instanceof AmountItem amountItem)
                amount = amountItem.getAmount();
            // create a component to add to the playerComponents list
            playerComponents.add(new GUIComponent(parsedPlayerText, GUIComponent.ComponentType.BUTTON, p, amount, playerButtonCommands, type));
        }
        if (type.equalsIgnoreCase("challenges")) {
            GUIComponent[] items = ChallengeManager.getDisplayComponents(player);
            for (GUIComponent component : items) {
                builder.button(component.getButtonComponent());
                playerComponents.add(component);
            }
        }
        return playerComponents;
    }

    private List<String> getPlayerText(Player player, long page, List<DisplayItem> displayItems, Object[] data) {
        List<String> text = new ArrayList<>();
        if (playerText == null)
            return text;
        for (int i = type.equals("select-price") || type.equals("confirm-bounty") ? 0 : (int) ((page - 1) * maxPlayers); i < Math.min(maxPlayers * page, displayItems.size()); i++) {
            text.add(displayItems.get(i).parseText(playerText, player));
        }
        if (type.equalsIgnoreCase("challenges")) {
            GUIComponent[] items = ChallengeManager.getDisplayComponents(player);
            for (GUIComponent item : items) {
                text.add(item.getComponent().text());
            }
        }
        return text;
    }

    public void openInventory(Player player, long page, List<DisplayItem> displayItems, String title, Object[] data) {
        if (!LanguageOptions.bedrockOpenGUI.isEmpty() && !GUI.playerInfo.containsKey(player.getUniqueId())) {
            player.sendMessage(LanguageOptions.parse(prefix + bedrockOpenGUI.replace("{page}", page + ""), player));
        }
        player.getOpenInventory().close();
        new BukkitRunnable() {
            long finalPage = page;
            int maxRequests = 10;

            @Override
            public void run() {
                    // load skins
                if (guiType == GUIType.SIMPLE) {
                    boolean loaded = true; // whether all the skin
                    for (DisplayItem displayItem : displayItems) {
                        if (displayItem instanceof PlayerItem playerItem && !SkinManager.isSkinLoaded(playerItem.getUuid())) {
                            // check if max requests hit
                            if (maxRequests <= 0) {
                                if (NotBounties.debug) {
                                    Bukkit.getLogger().warning("[NotBountiesDebug] Timed out loading skin for " + NotBounties.getPlayerName(playerItem.getUuid()));
                                }
                            } else {
                                if (loaded) {
                                    maxRequests--;
                                    loaded = false;
                                }
                            }
                        }
                    }
                    if (!loaded) // not all skins are loaded
                        return;
                }
                this.cancel();

                if (finalPage < 1) {
                    finalPage = 1;
                }

                List<GUIComponent> usedGUIComponents = new ArrayList<>();

                switch (guiType) {
                    case SIMPLE:
                        SimpleForm.Builder simpleBuilder = SimpleForm.builder().title(title);
                        StringBuilder content = new StringBuilder();
                        // before player values
                        for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                            if (entry.getKey() > 0)
                                break;
                            GUIComponent component = entry.getValue().buildComponent(player);
                            if (skipItem(component.getCommands(), page, displayItems.size()))
                                continue;

                            if (entry.getValue().getType() == GUIComponent.ComponentType.BUTTON) {
                                simpleBuilder.button(component.getButtonComponent());
                                usedGUIComponents.add(component.copy());
                            } else {
                                content.append(component.getComponent().text()).append("\n");
                            }
                        }
                        // player values
                        List<GUIComponent> addedComponents = addPlayerComponents(simpleBuilder, player, page, displayItems, data);
                        // add all components because they will all be buttons from this method
                        usedGUIComponents.addAll(addedComponents);
                        // after player values
                        for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                            if (entry.getKey() < 1)
                                continue;
                            GUIComponent component = entry.getValue().buildComponent(player);
                            if (skipItem(component.getCommands(), page, displayItems.size()))
                                continue;
                            if (entry.getValue().getType() == GUIComponent.ComponentType.BUTTON) {
                                simpleBuilder.button(component.getButtonComponent());
                                usedGUIComponents.add(component.copy());
                            } else {
                                content.append(component.getComponent().text()).append("\n");
                            }
                        }
                        simpleBuilder.content(content.toString());
                        simpleBuilder.validResultHandler(simpleFormResponse -> doClickActions(player, simpleFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    if (floodgateEnabled) {
                                        new FloodGateClass().sendForm(player.getUniqueId(), simpleBuilder);
                                    } else if (geyserEnabled) {
                                        new GeyserMCClass().sendForm(player.getUniqueId(), simpleBuilder);
                                    }
                                }
                            }
                        }.runTask(NotBounties.getInstance());

                        break;
                    case MODAL:
                        ModalForm.Builder modalBuilder = ModalForm.builder().title(title);
                        StringBuilder modalContent = new StringBuilder();
                        int buttons = 0;
                        // before player values
                        for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                            GUIComponent component = entry.getValue().buildComponent(player);
                            if (skipItem(component.getCommands(), page, displayItems.size()))
                                continue;
                            if (entry.getValue().getType() == GUIComponent.ComponentType.BUTTON && buttons < 2) {
                                if (buttons == 0)
                                    modalBuilder.button1(component.getButtonComponent().text());
                                else if (buttons == 1)
                                    modalBuilder.button2(component.getButtonComponent().text());
                                buttons++;
                                usedGUIComponents.add(component.copy());
                            } else {
                                modalContent.append(component.getComponent().text()).append("\n");
                            }
                        }
                        // add player values to content
                        for (String text : getPlayerText(player, page, displayItems, data)) {
                            modalContent.append(text).append("\n");
                        }
                        modalBuilder.content(modalContent.toString());
                        modalBuilder.validResultHandler(modalFormResponse -> doClickActions(player, modalFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    if (floodgateEnabled) {
                                        new FloodGateClass().sendForm(player.getUniqueId(), modalBuilder);
                                    } else if (geyserEnabled) {
                                        new GeyserMCClass().sendForm(player.getUniqueId(), modalBuilder);
                                    }
                                }
                            }
                        }.runTask(NotBounties.getInstance());

                        break;
                    case CUSTOM:
                        CustomForm.Builder customBuilder = CustomForm.builder().title(title);
                        // before player items
                        for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                            if (entry.getKey() > 0)
                                break;
                            GUIComponent component = entry.getValue().buildComponent(player);
                            if (skipItem(component.getCommands(), page, displayItems.size()))
                                continue;
                            usedGUIComponents.add(component.copy());

                            customBuilder.component(component.getComponent());
                        }
                        // player items
                        for (String text : getPlayerText(player, page, displayItems, data)) {
                            customBuilder.label(text);
                        }
                        // after player items
                        for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                            if (entry.getKey() < 1)
                                continue;
                            GUIComponent component = entry.getValue().buildComponent(player);
                            if (skipItem(component.getCommands(), page, displayItems.size()))
                                continue;
                            usedGUIComponents.add(component.copy());

                            customBuilder.component(component.getComponent());
                        }
                        customBuilder.validResultHandler(customFormResponse -> doClickActions(player, customFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    if (floodgateEnabled) {
                                        new FloodGateClass().sendForm(player.getUniqueId(), customBuilder);
                                    } else if (geyserEnabled) {
                                        new GeyserMCClass().sendForm(player.getUniqueId(), customBuilder);
                                    }
                                }
                            }
                        }.runTask(NotBounties.getInstance());

                        break;
                }
            }
        }.runTaskTimerAsynchronously(NotBounties.getInstance(), 0, 4);



    }

    private boolean skipItem(List<String> commands, long page, long maxSize) {
        if (!type.equals("select-price") && removePageItems) {
            // page items
            return (getPageType(commands) == 1 && page * maxPlayers >= maxSize) || (getPageType(commands) == 2 && page == 1);
        }
        return false;
    }

    private List<String> parseCompletionCommands(String value, double quantity, List<String> inputs) {
        List<String> commands = formCompletionCommands.stream().map(command -> command.replace("{value}", (value)).replace("{quantity}", (getValue(quantity)))).collect(Collectors.toList());
        List<String> parsedCommands = new ArrayList<>();
        for (String command : commands) {
            while (command.contains("{value") && command.substring(command.indexOf("{value")).contains("}")) {
                String index = command.substring(command.indexOf("{value") + 6, command.indexOf("{value") + command.substring(command.indexOf("{value")).indexOf("}"));
                String fullValue = "{value" + index + "}";
                String replacement = "";
                try {
                    int i = Integer.parseInt(index);
                    if (inputs.size() <= i) {
                        replacement = inputs.get(i + 1);
                    }
                } catch (NumberFormatException ignored) {
                }
                command = command.replace(fullValue, replacement);
            }
            parsedCommands.add(command);
        }
        return parsedCommands;
    }

    // simple
    private void doClickActions(Player player, SimpleFormResponse simpleFormResponse, List<GUIComponent> usedGUIComponents) {
        if (simpleFormResponse.clickedButtonId() >= usedGUIComponents.size()) {
            Bukkit.getLogger().warning(() -> "[NotBounties] This is a bug! User clicked an unregistered button in " + type + " bedrock gui.");
            return;
        }
        GUIComponent component = usedGUIComponents.get(simpleFormResponse.clickedButtonId());
        List<String> actions = new ArrayList<>(component.getCommands());
        double quantity;
        try {
            quantity = NumberFormatting.tryParse(component.getButtonComponent().text());
        } catch (NumberFormatException ignored) {
            quantity = 0;
        }
        actions.addAll(parseCompletionCommands(component.getButtonComponent().text(), quantity, new ArrayList<>()));
        ActionCommands.executeCommands(player, actions);

        // bounty-gui click actions
        // playerGUIType will be null if the component isn't built for a specific player
        if (component.getPlayerGUIType() != null && component.getPlayerGUIType().equals("bounty-gui") && component.getBuiltPlayer() != null) {
            Bounty bounty = BountyManager.getBounty(component.getBuiltPlayer().getUniqueId());
            if (bounty != null) {
                GUIClicks.runClickActions(player, bounty, ClickType.UNKNOWN);
            }
        }
    }

    // modal
    private void doClickActions(Player player, ModalFormResponse modalFormResponse, List<GUIComponent> usedGUIComponents) {
        if (modalFormResponse.clickedButtonId() >= usedGUIComponents.size()) {
            //Bukkit.getLogger().warning("[NotBounties] This is a bug! User clicked an unregistered button in " + type + " bedrock gui.");
            // buttons will appear even if none are specified
            return;
        }
        GUIComponent component = usedGUIComponents.get(modalFormResponse.clickedButtonId());
        List<String> actions = new ArrayList<>(component.getCommands());
        double quantity = 0;
        try {
            quantity = NumberFormatting.tryParse(component.getButtonComponent().text());
        } catch (NumberFormatException ignored) {}
        actions.addAll(parseCompletionCommands(component.getButtonComponent().text(), quantity, new ArrayList<>()));
        ActionCommands.executeCommands(player, actions);
    }

    // custom
    private void doClickActions(Player player, CustomFormResponse customFormResponse, List<GUIComponent> usedGUIComponents) {
        Map<GUIComponent, Double> quantity = new HashMap<>();
        Map<GUIComponent, String> value = new HashMap<>();
        List<String> inputs = new ArrayList<>();
        for (GUIComponent component : usedGUIComponents) {
            if (!customFormResponse.hasNext()) {
                Bukkit.getLogger().warning("[NotBounties] This is a bug! Form content length does not match records for " + type + " bedrock gui.");
                break;
            }
            if (component.getType() == GUIComponent.ComponentType.LABEL || component.getType() == GUIComponent.ComponentType.BUTTON) {
                inputs.add(component.getComponent().text());
                continue;
            }

            try {
                switch (component.getType()) {
                    case DROPDOWN:
                        int selectedDropdown = customFormResponse.asDropdown();
                        String dropdownText = ((DropdownComponentImpl) component.getComponent()).getOptions().get(selectedDropdown);
                        inputs.add(dropdownText);
                        if (component.getCommands().size() == component.getListOptions().size() && selectedDropdown < component.getCommands().size()) {
                            // 1 command
                            ActionCommands.executeCommands(player, Collections.singletonList(component.getCommands().get(selectedDropdown).replace("{dropdown}", (dropdownText)).replace("[@ll]", "")));
                        } else {
                            // maybe all commands
                            List<String> commands = new ArrayList<>();
                            int fillerIndex = -1;
                            for (int i = 0; i < component.getCommands().size(); i++) {
                                String cmd = component.getCommands().get(i);
                                if (cmd.startsWith("[@ll] ")) {
                                    cmd = cmd.substring(6);
                                    fillerIndex = i;
                                }
                                cmd = cmd.replace("{dropdown}", (dropdownText));
                                commands.add(cmd);
                            }

                            if (fillerIndex != -1 && commands.size() < ((DropdownComponentImpl) component.getComponent()).getOptions().size()) {
                                // 1 command
                                String cmd = commands.get(fillerIndex);
                                for (int i = commands.size(); i < ((DropdownComponentImpl) component.getComponent()).getOptions().size(); i++) {
                                    commands.add(i, cmd);
                                }
                                ActionCommands.executeCommands(player, Collections.singletonList(commands.get(selectedDropdown)));
                            } else {
                                // all commands
                                ActionCommands.executeCommands(player, commands);
                            }


                        }

                        try {
                            quantity.put(component, NumberFormatting.tryParse(dropdownText));
                        } catch (NumberFormatException e) {
                            value.put(component, dropdownText);
                        }
                        break;
                    case TOGGLE:
                        boolean toggleState = customFormResponse.asToggle();
                        inputs.add(toggleState + "");
                        List<String> commands = new ArrayList<>();
                        for (String command : component.getCommands()) {
                            if (command.startsWith("[on] ")) {
                                if (toggleState) {
                                    command = command.substring(5);
                                    commands.add(command);
                                }
                            } else if (command.startsWith("[off] ")) {
                                if (!toggleState) {
                                    command = command.substring(6);
                                    commands.add(command);
                                }
                            } else if (command.startsWith("[true] ")) {
                                if (toggleState) {
                                    command = command.substring(7);
                                    commands.add(command);
                                }
                            } else if (command.startsWith("[false] ")) {
                                if (!toggleState) {
                                    command = command.substring(8);
                                    commands.add(command);
                                }
                            } else {
                                commands.add(command);
                            }
                        }
                        ActionCommands.executeCommands(player, commands);
                        break;
                    case SLIDER:
                        float sliderValue = customFormResponse.asSlider();
                        inputs.add(NumberFormatting.getValue(sliderValue));
                        quantity.put(component, (double) sliderValue); // possible to be used as quantity
                        ActionCommands.executeCommands(player, component.getCommands().stream().map(command -> command.replace("{value}", (NumberFormatting.getValue(sliderValue)))).collect(Collectors.toList()));
                        break;
                    case STEP_SLIDER:
                        int stepSliderIndex = customFormResponse.asStepSlider();
                        String stepValue = component.getListOptions().get(stepSliderIndex);
                        inputs.add(stepValue);
                        value.put(component, stepValue);
                        ActionCommands.executeCommands(player, component.getCommands().stream().map(command -> command.replace("{value}", (stepValue))).collect(Collectors.toList()));
                        break;
                    case INPUT:
                        String input = customFormResponse.asInput();
                        if (input == null || input.isEmpty()) {
                            inputs.add("");
                            break;
                        }
                        inputs.add(input);
                        try {
                            quantity.put(component, NumberFormatting.tryParse(input));
                        } catch (NumberFormatException e) {
                            value.put(component, input);
                        }
                        ActionCommands.executeCommands(player, component.getCommands().stream().map(command -> command.replace("{value}", (input))).collect(Collectors.toList()));
                        break;
                }
            } catch (IllegalStateException e) {
                Bukkit.getLogger().warning("[NotBounties] This is a bug! Custom form response component does not match up with the recorded component in " + type + " bedrock gui.");
                Bukkit.getLogger().warning(e.toString());
            }
        }
        // do completion commands
        double quantityValue = 0;
        String valueValue = "";
        // 6 = input no cmd
        // 5 = slider no cmd
        // 4 = dropdown no cmd
        // 3 = input
        // 2 = slider
        // 1 = dropdown
        int rank = 0;
        for (Map.Entry<GUIComponent, String> entry : value.entrySet()) {
            int componentRank = rankComponent(entry.getKey());
            if (componentRank > rank) {
                valueValue = entry.getValue();
                rank = componentRank;
            }
        }
        rank = 0;
        for (Map.Entry<GUIComponent, Double> entry : quantity.entrySet()) {
            int componentRank = rankComponent(entry.getKey());
            if (componentRank > rank) {
                quantityValue = entry.getValue();
                rank = componentRank;
            }
        }
        List<String> cmds = parseCompletionCommands(valueValue, quantityValue, inputs);
        ActionCommands.executeCommands(player, cmds);
    }

    /**
     * 6 = input no cmd
     * 5 = slider no cmd
     * 4 = dropdown no cmd
     * 3 = input
     * 2 = slider
     * 1 = dropdown
     */
    private int rankComponent(GUIComponent component) {
        int componentRank = 0;
        if (component.getCommands().isEmpty() || component.getCommands().get(0).isEmpty())
            componentRank+=3;
        switch (component.getType()) {
            case INPUT:
                componentRank+=3;
                break;
            case STEP_SLIDER:
                componentRank+=2;
                break;
            case DROPDOWN:
                componentRank+=1;
                break;
        }
        return componentRank;
    }

    private static LinkedHashMap<Integer, GUIComponent> sortByKey(Map<Integer, GUIComponent> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<Integer, GUIComponent>> list = new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort(Map.Entry.comparingByKey());
        //Collections.reverse(list);

        // put data from sorted list to hashmap
        LinkedHashMap<Integer, GUIComponent> temp = new LinkedHashMap<>();
        for (Map.Entry<Integer, GUIComponent> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
}

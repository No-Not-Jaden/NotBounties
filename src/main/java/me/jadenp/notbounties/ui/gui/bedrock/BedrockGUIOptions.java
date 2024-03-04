package me.jadenp.notbounties.ui.gui.bedrock;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.utils.configuration.ActionCommands;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.ModalFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.cumulus.util.impl.FormImageImpl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.ui.gui.GUIOptions.getPageType;
import static me.jadenp.notbounties.utils.BountyManager.getBounty;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.bountyTax;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.bountyWhitelistCost;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.color;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.parse;
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

    private final GUIType guiType;

    public BedrockGUIOptions(ConfigurationSection settings, ConfigurationSection customItems) {
        type = settings.getName();
        name = settings.isSet("gui-name") ? color(settings.getString("gui-name")) : "Custom GUI";
        addPage = settings.isSet("add-page") && settings.getBoolean("add-page");
        sortType = settings.isSet("sort-type") ? settings.getInt("sort-type") : 1;
        removePageItems = settings.getBoolean("remove-page-items");
        enabled = !settings.isSet("enabled") || settings.getBoolean("enabled");
        maxPlayers = settings.isSet("max-players") ? settings.getInt("max-players") : 9999;
        playerText = settings.isSet("player-text") ? settings.getString("player-text") : "{player}";
        formCompletionCommands = settings.isSet("completion-commands") ? settings.getStringList("completion-commands") : new ArrayList<>();
        playerButtonCommands = settings.isSet("player-button-commands") ? settings.getStringList("player-button-commands") : new ArrayList<>();

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
                assert componentID != null;
                if (customItems.isConfigurationSection(componentID)) {
                    GUIComponent component = new GUIComponent(Objects.requireNonNull(customItems.getConfigurationSection(componentID)));
                    if (component.getType() == GUIComponent.ComponentType.BUTTON)
                        buttonCount++;
                    components.put(order, component);
                } else {
                    Bukkit.getLogger().warning("[NotBounties] Could not find custom item \"" + componentID + "\" in bedrock-gui.yml");
                }
            }

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

        components = sortByKey(components);


    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSortType() {
        return sortType;
    }

    private String getPlayerText(int index, Player player, OfflinePlayer p, String amount, String[] replacements) {
        long time = System.currentTimeMillis();
        Bounty bounty = getBounty(p);
        if (type.equals("bounty-gui")) {
            amount = currencyPrefix + NumberFormatting.formatNumber(tryParse(amount)) + currencySuffix;
            if (bounty != null) {
                time = bounty.getLatestSetter();
            }
        } else {
            Leaderboard leaderboard = null;
            try {
                leaderboard = Leaderboard.valueOf(replacements[0]);
            } catch (IllegalArgumentException ignored) {
            }
            if (leaderboard == null)
                amount = formatNumber(amount);
        }

        final String finalAmount = amount;
        final long rank = index + 1;
        String playerName = NotBounties.getPlayerName(p.getUniqueId());
        double total = parseCurrency(finalAmount) * (bountyTax + 1) + NotBounties.getPlayerWhitelist(player.getUniqueId()).getList().size() * bountyWhitelistCost;
        String text;
        try {
            text = playerText.replaceAll("\\{amount}", Matcher.quoteReplacement(finalAmount)).replaceAll("\\{rank}", Matcher.quoteReplacement(rank + "")).replaceAll("\\{leaderboard}", Matcher.quoteReplacement(replacements[0])).replaceAll("\\{player}", Matcher.quoteReplacement(playerName)).replaceAll("\\{amount_tax}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(total) + NumberFormatting.currencySuffix));
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Error parsing name and lore for player item! This is usually caused by a typo in the config.");
            text = playerText;
        }
        if (type.equalsIgnoreCase("set-whitelist"))
            if (NotBounties.getPlayerWhitelist(player.getUniqueId()).getList().contains(p.getUniqueId())) {
                text = ChatColor.GREEN + text;
            } else {
                text = ChatColor.YELLOW + text;
            }
        return parse(text, time, p);
    }

    private List<GUIComponent> addPlayerComponents(SimpleForm.Builder builder, Player player, long page, OfflinePlayer[] playerItems, String[] amount, String[] replacements) {
        // keeping track of added components
        List<GUIComponent> playerComponents = new ArrayList<>();
        // iterate through players to add
        // the number of components added is limited to maxPlayers
        // the start position in the playerItems and amount arrays depend on the page
        for (int i = type.equals("select-price") || type.equals("confirm-bounty") ? 0 : (int) ((page - 1) * maxPlayers); i < Math.min(maxPlayers * page, playerItems.length); i++) {
            // get player and amount for this index
            OfflinePlayer p = playerItems[i];
            String finalAmount = amount[i];
            // default texture id (question mark head)
            String imageTextureID = "46ba63344f49dd1c4f5488e926bf3d9e2b29916a6c50d610bb40a5273dc8c82";
            String tempId = Head.getTextureID(p.getUniqueId());
            if (tempId != null)
                imageTextureID = tempId;
            // perspective head url
            String imageURL = "https://mc-heads.net/head/" + imageTextureID + ".png";
            // get the player text (will be parsed already)
            String playerText = getPlayerText(i, player, p, finalAmount, replacements);
            // add button to the component
            builder.button(playerText, new FormImageImpl(FormImage.Type.URL, imageURL));
            // create a component to add to the playerComponents list
            playerComponents.add(new GUIComponent(playerText, GUIComponent.ComponentType.BUTTON, p, finalAmount, playerButtonCommands));
        }
        return playerComponents;
    }

    private List<String> getPlayerText(Player player, long page, OfflinePlayer[] playerItems, String[] amount, String[] replacements) {
        List<String> text = new ArrayList<>();
        for (int i = type.equals("select-price") || type.equals("confirm-bounty") ? 0 : (int) ((page - 1) * maxPlayers); i < Math.min(maxPlayers * page, playerItems.length); i++) {
            OfflinePlayer p = playerItems[i];
            String finalAmount = NumberFormatting.formatNumber(amount[i]);
            text.add(getPlayerText(i, player, p, finalAmount, replacements));
        }
        return text;
    }

    public void openInventory(Player player, long page, LinkedHashMap<UUID, String> values, String... replacements) {
        OfflinePlayer[] playerItems = new OfflinePlayer[values.size()];
        String[] amount = new String[values.size()];
        int index = 0;
        for (Map.Entry<UUID, String> entry : values.entrySet()) {
            playerItems[index] = Bukkit.getOfflinePlayer(entry.getKey());
            amount[index] = entry.getValue();
            index++;
        }
        // this is for adding more replacements in the future
        int desiredLength = 1;
        if (replacements.length < desiredLength)
            replacements = new String[desiredLength];
        for (int i = 0; i < replacements.length; i++) {
            if (replacements[i] == null)
                replacements[i] = "";
        }

        if (page < 1) {
            page = 1;
        }
        String name = addPage ? this.name + " " + page : this.name;
        if (amount.length > 0) {
            double totalCost = parseCurrency(amount[0]) * (bountyTax + 1) + NotBounties.getPlayerWhitelist(player.getUniqueId()).getList().size() * bountyWhitelistCost;
            String playerName = NotBounties.getPlayerName(playerItems[0].getUniqueId());
            name = name.replaceAll("\\{amount}", Matcher.quoteReplacement(amount[0])).replaceAll("\\{amount_tax}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalCost) + NumberFormatting.currencySuffix)).replaceAll("\\{leaderboard}", Matcher.quoteReplacement(replacements[0])).replaceAll("\\{player}", playerName);
        }
        name = parse(name, player);
        List<GUIComponent> usedGUIComponents = new ArrayList<>();

        switch (guiType) {
            case SIMPLE:
                SimpleForm.Builder simpleBuilder = SimpleForm.builder().title(name);
                StringBuilder content = new StringBuilder();
                // before player values
                for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                    if (entry.getKey() > 0)
                        break;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (skipItem(component.getCommands(), page, playerItems.length))
                        continue;

                    if (entry.getValue().getType() == GUIComponent.ComponentType.BUTTON) {
                        simpleBuilder.button(component.getButtonComponent());
                        usedGUIComponents.add(component.copy());
                    } else {
                        content.append(component.getComponent().text()).append("\n");
                    }
                }
                // player values
                List<GUIComponent> addedComponents = addPlayerComponents(simpleBuilder, player, page, playerItems, amount, replacements);
                // add all components because they will all be buttons from this method
                usedGUIComponents.addAll(addedComponents);
                // after player values
                for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                    if (entry.getKey() < 1)
                        continue;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (skipItem(component.getCommands(), page, playerItems.length))
                        continue;
                    if (entry.getValue().getType() == GUIComponent.ComponentType.BUTTON) {
                        simpleBuilder.button(component.getButtonComponent());
                        usedGUIComponents.add(component.copy());
                    } else {
                        content.append(component.getComponent().text()).append("\n");
                    }
                }
                simpleBuilder.content(content.toString());
                simpleBuilder.validResultHandler((simpleFormResponse) -> doClickActions(player, simpleFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                new FloodGateClass().sendForm(player.getUniqueId(), simpleBuilder);
                break;
            case MODAL:
                ModalForm.Builder modalBuilder = ModalForm.builder().title(name);
                StringBuilder modalContent = new StringBuilder();
                int buttons = 0;
                // before player values
                for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (skipItem(component.getCommands(), page, playerItems.length))
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
                for (String text : getPlayerText(player, page, playerItems, amount, replacements)) {
                    modalContent.append(text).append("\n");
                }
                modalBuilder.content(modalContent.toString());
                modalBuilder.validResultHandler(modalFormResponse -> doClickActions(player, modalFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                new FloodGateClass().sendForm(player.getUniqueId(), modalBuilder);
                break;
            case CUSTOM:
                CustomForm.Builder customBuilder = CustomForm.builder().title(name);
                // before player items
                for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                    if (entry.getKey() > 0)
                        break;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (skipItem(component.getCommands(), page, playerItems.length))
                        continue;
                    usedGUIComponents.add(component.copy());

                    customBuilder.component(component.getComponent());
                }
                // player items
                for (String text : getPlayerText(player, page, playerItems, amount, replacements)) {
                    customBuilder.label(text);
                }
                // after player items
                for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
                    if (entry.getKey() < 1)
                        continue;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (skipItem(component.getCommands(), page, playerItems.length))
                        continue;
                    usedGUIComponents.add(component.copy());

                    customBuilder.component(component.getComponent());
                }
                customBuilder.validResultHandler(customFormResponse -> doClickActions(player, customFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                new FloodGateClass().sendForm(player.getUniqueId(), customBuilder);
                break;
        }


    }

    private boolean skipItem(List<String> commands, long page, long maxSize) {
        if (!type.equals("select-price") && removePageItems) {
            // page items
            return (getPageType(commands) == 1 && page * maxPlayers >= maxSize) || (getPageType(commands) == 2 && page == 1);
        }
        return false;
    }

    private List<String> parseCompletionCommands(String value, double quantity, List<String> inputs) {
        List<String> commands = formCompletionCommands.stream().map(command -> command.replaceAll("\\{value}", Matcher.quoteReplacement(value)).replaceAll("\\{quantity}", Matcher.quoteReplacement(getValue(quantity)))).collect(Collectors.toList());
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
            Bukkit.getLogger().warning("[NotBounties] This is a bug! User clicked an unregistered button in " + type + " bedrock gui.");
            return;
        }
        GUIComponent component = usedGUIComponents.get(simpleFormResponse.clickedButtonId());
        List<String> actions = new ArrayList<>(component.getCommands());
        double quantity = 0;
        try {
            quantity = NumberFormatting.tryParse(component.getButtonComponent().text());
        } catch (NumberFormatException ignored) {}
        actions.addAll(parseCompletionCommands(component.getButtonComponent().text(), quantity, new ArrayList<>()));
        ActionCommands.executeGUI(player, actions);
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
        ActionCommands.executeGUI(player, actions);
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
                        String dropdownText = component.getListOptions().get(selectedDropdown);
                        inputs.add(dropdownText);
                        if (component.getCommands().size() == component.getListOptions().size()) {
                            // 1 command
                            ActionCommands.executeGUI(player, Collections.singletonList(component.getCommands().get(selectedDropdown).replaceAll("\\{dropdown}", Matcher.quoteReplacement(dropdownText))));
                        } else {
                            // all commands
                            List<String> commands = component.getCommands().stream().map(command -> command.replaceAll("\\{dropdown}", Matcher.quoteReplacement(dropdownText))).collect(Collectors.toList());
                            ActionCommands.executeGUI(player, commands);
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
                        ActionCommands.executeGUI(player, commands);
                        break;
                    case SLIDER:
                        float sliderValue = customFormResponse.asSlider();
                        inputs.add(NumberFormatting.getValue(sliderValue));
                        quantity.put(component, (double) sliderValue); // possible to be used as quantity
                        ActionCommands.executeGUI(player, component.getCommands().stream().map(command -> command.replaceAll("\\{value}", Matcher.quoteReplacement(NumberFormatting.getValue(sliderValue)))).collect(Collectors.toList()));
                        break;
                    case STEP_SLIDER:
                        int stepSliderIndex = customFormResponse.asStepSlider();
                        String stepValue = component.getListOptions().get(stepSliderIndex);
                        inputs.add(stepValue);
                        value.put(component, stepValue);
                        ActionCommands.executeGUI(player, component.getCommands().stream().map(command -> command.replaceAll("\\{value}", Matcher.quoteReplacement(stepValue))).collect(Collectors.toList()));
                        break;
                    case INPUT:
                        String input = customFormResponse.asInput();
                        if (input == null) {
                            inputs.add("");
                            break;
                        }
                        inputs.add(input);
                        try {
                            quantity.put(component, NumberFormatting.tryParse(input));
                        } catch (NumberFormatException e) {
                            value.put(component, input);
                        }
                        ActionCommands.executeGUI(player, component.getCommands().stream().map(command -> command.replaceAll("\\{value}", Matcher.quoteReplacement(input))).collect(Collectors.toList()));
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
        ActionCommands.executeGUI(player, cmds);
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

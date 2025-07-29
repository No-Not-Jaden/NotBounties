package me.jadenp.notbounties.utils.tasks;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUIOptions;
import me.jadenp.notbounties.ui.gui.bedrock.GUIComponent;
import me.jadenp.notbounties.ui.gui.display_items.DisplayItem;
import me.jadenp.notbounties.ui.gui.display_items.PlayerItem;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenBedrockGUI extends CancelableTask{

    private final Player player;
    private long finalPage;
    private long maxPage;
    private int maxRequests = 10;
    private final List<DisplayItem> displayItems;
    private final String title;
    private final Object[] data;
    private final BedrockGUIOptions guiOptions;

    public OpenBedrockGUI(Player player, long page, long maxPage, BedrockGUIOptions guiOptions, List<DisplayItem> displayItems, String title, Object[] data) {
        super();
        this.player = player;
        finalPage = page;
        this.maxPage = maxPage;
        this.guiOptions = guiOptions;
        this.displayItems = displayItems;
        this.title = title;
        this.data = data;
    }

    @Override
    public void run() {
        // load skins
        if (guiOptions.getGuiType() == BedrockGUIOptions.GUIType.SIMPLE) {
            boolean loaded = true; // whether all the skin
            for (DisplayItem displayItem : displayItems) {
                if (displayItem instanceof PlayerItem playerItem && !SkinManager.isSkinLoaded(playerItem.getUuid())) {
                    // check if max requests hit
                    if (maxRequests <= 0) {
                        NotBounties.debugMessage("Timed out loading skin for " + LoggedPlayers.getPlayerName(playerItem.getUuid()), false);
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

        switch (guiOptions.getGuiType()) {
            case SIMPLE:
                SimpleForm.Builder simpleBuilder = SimpleForm.builder().title(title);
                StringBuilder content = new StringBuilder();
                // before player values
                for (Map.Entry<Integer, GUIComponent> entry : guiOptions.getComponents().entrySet()) {
                    if (entry.getKey() > 0)
                        break;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (guiOptions.skipItem(component.getCommands(), finalPage, displayItems.size()))
                        continue;

                    if (entry.getValue().getType() == GUIComponent.ComponentType.BUTTON) {
                        simpleBuilder.button(component.getButtonComponent());
                        usedGUIComponents.add(component.copy());
                    } else {
                        content.append(component.getComponent().text()).append("\n");
                    }
                }
                // player values
                List<GUIComponent> addedComponents = guiOptions.addPlayerComponents(simpleBuilder, player, finalPage, maxPage, displayItems, data);
                // add all components because they will all be buttons from this method
                usedGUIComponents.addAll(addedComponents);
                // after player values
                for (Map.Entry<Integer, GUIComponent> entry : guiOptions.getComponents().entrySet()) {
                    if (entry.getKey() < 1)
                        continue;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (guiOptions.skipItem(component.getCommands(), finalPage, displayItems.size()))
                        continue;
                    if (entry.getValue().getType() == GUIComponent.ComponentType.BUTTON) {
                        simpleBuilder.button(component.getButtonComponent());
                        usedGUIComponents.add(component.copy());
                    } else {
                        content.append(component.getComponent().text()).append("\n");
                    }
                }
                simpleBuilder.content(content.toString());
                simpleBuilder.validResultHandler(simpleFormResponse -> guiOptions.doClickActions(player, simpleFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                guiOptions.sendForm(player, simpleBuilder);

                break;
            case MODAL:
                ModalForm.Builder modalBuilder = ModalForm.builder().title(title);
                StringBuilder modalContent = new StringBuilder();
                int buttons = 0;
                // before player values
                for (Map.Entry<Integer, GUIComponent> entry : guiOptions.getComponents().entrySet()) {
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (guiOptions.skipItem(component.getCommands(), finalPage, displayItems.size()))
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
                for (String text : guiOptions.getPlayerText(player, finalPage, maxPage, displayItems, data)) {
                    modalContent.append(text).append("\n");
                }
                modalBuilder.content(modalContent.toString());
                modalBuilder.validResultHandler(modalFormResponse -> guiOptions.doClickActions(player, modalFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                guiOptions.sendForm(player, modalBuilder);

                break;
            case CUSTOM:
                CustomForm.Builder customBuilder = CustomForm.builder().title(title);
                // before player items
                for (Map.Entry<Integer, GUIComponent> entry : guiOptions.getComponents().entrySet()) {
                    if (entry.getKey() > 0)
                        break;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (guiOptions.skipItem(component.getCommands(), finalPage, displayItems.size()))
                        continue;
                    usedGUIComponents.add(component.copy());

                    customBuilder.component(component.getComponent());
                }
                // player items
                for (String text : guiOptions.getPlayerText(player, finalPage, maxPage, displayItems, data)) {
                    customBuilder.label(text);
                }
                // after player items
                for (Map.Entry<Integer, GUIComponent> entry : guiOptions.getComponents().entrySet()) {
                    if (entry.getKey() < 1)
                        continue;
                    GUIComponent component = entry.getValue().buildComponent(player);
                    if (guiOptions.skipItem(component.getCommands(), finalPage, displayItems.size()))
                        continue;
                    usedGUIComponents.add(component.copy());

                    customBuilder.component(component.getComponent());
                }
                customBuilder.validResultHandler(customFormResponse -> guiOptions.doClickActions(player, customFormResponse, usedGUIComponents)).closedOrInvalidResultHandler(() -> GUI.playerInfo.remove(player.getUniqueId()));
                guiOptions.sendForm(player, customBuilder);
                break;
        }
    }
}

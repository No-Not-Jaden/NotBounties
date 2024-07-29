package me.jadenp.notbounties.ui.gui.displayItems;

import me.jadenp.notbounties.ui.gui.GUIOptions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface DisplayItem {
    /**
     * Get a formatted item for a player
     * @param player Player to format the item for
     * @param headName Name of the item
     * @param lore lore of the item
     * @return
     */
    ItemStack getFormattedItem(Player player, String headName, List<String> lore);
    String parseText(String text, Player player);
}

package me.jadenp.notbounties.ui.gui.display_items;

import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.ExcludedItemException;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.gui.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Represents a regular item
 */
public class UnmodifiedItem implements DisplayItem, AmountItem{
    private final UUID owningPlayer;
    private final ItemStack itemStack;
    private final List<String> additionalLore;

    @Override
    public ItemStack getFormattedItem(Player player, String headName, List<String> lore, int customModelData, String itemModel) {
        ItemStack item = itemStack.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        List<String> previousLore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) {
            previousLore.addAll(meta.getLore());
        }
        previousLore.addAll(additionalLore);
        meta.setLore(previousLore.stream().map(string -> LanguageOptions.parse(string, Bukkit.getOfflinePlayer(owningPlayer))).toList());
        if (customModelData != -1)
            meta.setCustomModelData(customModelData);
        if (itemModel != null)
            meta.setItemModel(CustomItem.getItemModel(itemModel));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String parseText(String text, Player player) {
        text = text.replace("{amount}", NumberFormatting.formatNumber(getAmount()))
                .replace("{items}", NumberFormatting.listItems(Collections.singletonList(itemStack), 'x'))
                .replace("{viewer}", LanguageOptions.getMessage("player-prefix") + player.getName() + LanguageOptions.getMessage("player-suffix"));
        return LanguageOptions.parse(text, Bukkit.getOfflinePlayer(owningPlayer));
    }

    public UnmodifiedItem(UUID owningPlayer, ItemStack itemStack, List<String> additionalLore) {

        this.owningPlayer = owningPlayer;
        this.itemStack = itemStack;
        this.additionalLore = additionalLore;
    }

    public UUID getOwningPlayer() {
        return owningPlayer;
    }

    @Override
    public double getAmount() {
        try {
            return NumberFormatting.getItemValue(itemStack);
        } catch (ExcludedItemException e) {
            return 0;
        }
    }
}

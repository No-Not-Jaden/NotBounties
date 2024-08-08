package me.jadenp.notbounties.ui.gui.displayItems;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a regular item
 */
public class UnmodifiedItem implements DisplayItem, AmountItem{
    private final UUID owningPlayer;
    private final ItemStack itemStack;
    private final List<String> additionalLore;

    @Override
    public ItemStack getFormattedItem(Player player, String headName, List<String> lore) {
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
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String parseText(String text, Player player) {
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
        return NumberFormatting.getItemValue(itemStack);
    }
}

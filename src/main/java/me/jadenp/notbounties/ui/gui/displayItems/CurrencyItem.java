package me.jadenp.notbounties.ui.gui.displayItems;

import me.jadenp.notbounties.ui.gui.GUI;
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
 * Represents currency
 */
public class CurrencyItem implements DisplayItem, AmountItem{
    private final UUID owningPlayer;
    private final double amount;
    private final List<String> additionalLore;

    @Override
    public ItemStack getFormattedItem(Player player, String headName, List<String> lore) {
        ItemStack item = GUI.getGeneralCurrencyItem().getFormattedItem(player, new String[] {"", NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix, "", ""});
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        List<String> previousLore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) {
             previousLore.addAll(meta.getLore());
        }
        previousLore.addAll(additionalLore);
        meta.setLore(previousLore.stream().map(string -> LanguageOptions.parse(string, amount, player)).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String parseText(String text, Player player) {
        text = text.replace("{items}", "");
        return LanguageOptions.parse(text, amount, Bukkit.getOfflinePlayer(owningPlayer));
    }

    public CurrencyItem(UUID owningPlayer, double amount, List<String> additionalLore) {

        this.owningPlayer = owningPlayer;
        this.amount = amount;
        this.additionalLore = additionalLore;
    }

    public UUID getOwningPlayer() {
        return owningPlayer;
    }

    @Override
    public double getAmount() {
        return amount;
    }
}

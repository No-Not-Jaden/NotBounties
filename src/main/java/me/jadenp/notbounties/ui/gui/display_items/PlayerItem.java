package me.jadenp.notbounties.ui.gui.display_items;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.ui.HeadFetcher;
import me.jadenp.notbounties.ui.gui.CustomItem;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player
 */

public class PlayerItem implements DisplayItem, AmountItem{
    private final UUID uuid;
    private final double amount;
    private final Leaderboard displayType;
    private final int index;
    private final long time;
    private final List<String> additionalLore;

    @Override
    public ItemStack getFormattedItem(Player player, String headName, List<String> lore, int customModelData, String itemModel) {
        ItemStack item = HeadFetcher.getUnloadedHead(uuid);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(parseText(headName, player));
        lore = new ArrayList<>(lore); // create a copy so the original lore isn't edited
        lore.addAll(additionalLore);
        meta.setLore(lore.stream().map(string -> parseText(string, player)).toList());
        if (customModelData != -1)
            meta.setCustomModelData(customModelData);
        if (itemModel != null)
            meta.setItemModel(CustomItem.getItemModel(itemModel));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String parseText(String text, Player player) {
        if (text.contains("{tax}") || text.contains("{amount_tax}")) {
            double tax = amount * ConfigOptions.getMoney().getBountyTax() + DataManager.getPlayerData(player.getUniqueId()).getWhitelist().getList().size() * Whitelist.getCost();
            double total = amount + tax;
            text = text.replace("{tax}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(tax) + NumberFormatting.getCurrencySuffix())
                    .replace("{amount_tax}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(total) + NumberFormatting.getCurrencySuffix());
        }
        if (!displayType.isMoney()) {
            text = text.replace("{amount}", NumberFormatting.formatNumber(amount));
        }
        text = text.replace("{rank}", (index + 1) + "")
                .replace("{leaderboard}", displayType.toString())
                .replace("{leaderboard_name}", displayType.getDisplayName())
                .replace("{items}", "")
                .replace("{viewer}", LanguageOptions.getMessage("player-prefix") + player.getName() + LanguageOptions.getMessage("player-suffix"));
        return LanguageOptions.parse(text, amount, time, LocalTime.TimeFormat.PLAYER, Bukkit.getOfflinePlayer(uuid));
    }

    public PlayerItem(UUID uuid, double amount, Leaderboard displayType, int index, long time, List<String> additionalLore) {

        this.uuid = uuid;
        this.amount = amount;
        this.displayType = displayType;
        this.index = index;
        this.time = time;
        this.additionalLore = additionalLore;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public double getAmount() {
        return amount;
    }
}

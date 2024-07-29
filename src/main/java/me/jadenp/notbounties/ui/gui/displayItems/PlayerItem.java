package me.jadenp.notbounties.ui.gui.displayItems;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.HeadFetcher;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
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
    public ItemStack getFormattedItem(Player player, String headName, List<String> lore) {
        ItemStack item = HeadFetcher.getUnloadedHead(uuid);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(parseText(headName, player));
        lore = new ArrayList<>(lore); // create a copy so the original lore isn't edited
        lore.addAll(additionalLore);
        meta.setLore(lore.stream().map(string -> parseText(string, player)).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String parseText(String text, Player player) {
        if (text.contains("{tax}") || text.contains("{amount_tax}")) {
            double tax = amount * ConfigOptions.bountyTax + NotBounties.getPlayerWhitelist(player.getUniqueId()).getList().size() * ConfigOptions.bountyWhitelistCost;
            double total = amount + tax;
            text = text.replace("{tax}", NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(tax) + NumberFormatting.currencySuffix)
                    .replace("{amount_tax}", NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(total) + NumberFormatting.currencySuffix);
        }
        if (!displayType.isMoney()) {
            text = text.replace("{amount}", NumberFormatting.formatNumber(amount));
        }
        text = text.replace("{rank}", (index + 1) + "")
                .replace("{leaderboard}", displayType.toString());
        return LanguageOptions.parse(text, NotBounties.getPlayerName(uuid), amount, time, LocalTime.TimeFormat.PLAYER, player);
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

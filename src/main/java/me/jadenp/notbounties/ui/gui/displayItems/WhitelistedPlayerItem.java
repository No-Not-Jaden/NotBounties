package me.jadenp.notbounties.ui.gui.displayItems;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class WhitelistedPlayerItem extends PlayerItem{
    private final String playerPrefix;

    @Override
    public ItemStack getFormattedItem(Player player, String headName, List<String> lore) {
        ItemStack itemStack = super.getFormattedItem(player, headName, lore);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return itemStack;
        if (NotBounties.isAboveVersion(20, 4)) {
            if (!meta.hasEnchantmentGlintOverride())
                meta.setEnchantmentGlintOverride(true);
        } else {
            itemStack.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public String parseText(String text, Player player) {
        text = text.replace("{player}", playerPrefix + LoggedPlayers.getPlayerName(super.getUuid()));
        return super.parseText(text, player);
    }

    public WhitelistedPlayerItem(UUID uuid, double amount, Leaderboard displayType, int index, long time, List<String> additionalLore, String playerPrefix) {
        super(uuid, amount, displayType, index, time, additionalLore);
        this.playerPrefix = playerPrefix;
    }
}

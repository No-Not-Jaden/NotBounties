package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CustomItem {
    private final ItemStack itemStack;
    private final List<String> commands;

    public CustomItem(ItemStack itemStack, List<String> commands){

        this.itemStack = itemStack;
        this.commands = commands;
    }

    public ItemStack getFormattedItem(Player player){
        if (itemStack == null)
            return null;
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(PlaceholderAPI.setPlaceholders(player, meta.getDisplayName()));
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            assert lore != null;
            lore.replaceAll(text -> PlaceholderAPI.setPlaceholders(player, text));
            meta.setLore(lore);
        }
        ItemStack newItem = itemStack.clone();
        newItem.setItemMeta(meta);
        return newItem;
    }

    public List<String> getCommands() {
        return commands;
    }

}

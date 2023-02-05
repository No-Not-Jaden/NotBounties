package me.jadenp.notbounties;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Item {
    public ItemStack get(String item){
        if (item.equalsIgnoreCase("fill")) {
            ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fMeta = fill.getItemMeta();
            assert fMeta != null;
            fMeta.setDisplayName(ChatColor.BLACK + "");
            fill.setItemMeta(fMeta);
            return fill;
        } else if (item.equalsIgnoreCase("next")) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + "Next");
            next.setItemMeta(meta);
            return next;
        } else if (item.equalsIgnoreCase("back")) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta bmeta = back.getItemMeta();
            assert bmeta != null;
            bmeta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + "Back");
            back.setItemMeta(bmeta);
            return back;
        } else if (item.equalsIgnoreCase("exit")){
            ItemStack itemStack = new ItemStack(Material.BARRIER);
            ItemMeta meta = itemStack.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Exit");
            itemStack.setItemMeta(meta);
            return itemStack;
        } else if (item.equalsIgnoreCase("yes")){
            ItemStack itemStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta meta = itemStack.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Yes");
            itemStack.setItemMeta(meta);
            return itemStack;
        } else if (item.equalsIgnoreCase("no")){
            ItemStack itemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = itemStack.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "No");
            itemStack.setItemMeta(meta);
            return itemStack;
        }
        return null;
    }
}

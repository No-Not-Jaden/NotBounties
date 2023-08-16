package me.jadenp.notbounties.gui;

import me.jadenp.notbounties.utils.Head;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static me.jadenp.notbounties.utils.ConfigOptions.color;
import static me.jadenp.notbounties.utils.ConfigOptions.parse;

public class CustomItem {
    private final Material material;
    private final int amount;
    private final int customModelData;
    private final String name;
    private final List<String> lore;
    private final boolean enchanted;
    private final boolean hideNBT;
    private final List<String> commands;
    private String headID = null;

    public CustomItem(ConfigurationSection configurationSection){
        Material material = Material.STONE;
        String mat = configurationSection.getString("material");
        if (mat != null) {
            if (mat.contains(" ") && mat.substring(0, mat.indexOf(" ")).equalsIgnoreCase("PLAYER_HEAD")) {
                // has head data
                headID = mat.substring(mat.indexOf(" ") + 1);
                mat = mat.substring(0, mat.indexOf(" "));

            }
                try {
                    material = Material.valueOf(mat.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("Unknown material \"" + mat + "\" in " + configurationSection.getName());
                }
        }
        this.material = material;
        amount = configurationSection.isInt("amount") ? configurationSection.getInt("amount") : 1;

        name = configurationSection.getString("name");
        if (configurationSection.isInt("custom-model-data"))
            customModelData = configurationSection.getInt("custom-model-data");
        else customModelData = -1;
        lore = configurationSection.getStringList("lore");
        enchanted = configurationSection.getBoolean("enchanted");
        hideNBT = configurationSection.getBoolean("hide-nbt");

        this.commands = configurationSection.isList("commands") ? configurationSection.getStringList("commands") : new ArrayList<>();
    }



    public ItemStack getFormattedItem(Player player, String[] replacements){
        ItemStack itemStack = headID != null && material == Material.PLAYER_HEAD ? Head.createPlayerSkull(headID) : new ItemStack(material, amount);
        if (itemStack == null)
            return null;
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        if (name != null)
            meta.setDisplayName(parse(color(name.replaceAll("\\{leaderboard}", replacements[0])), player));
        if (!lore.isEmpty()) {
            List<String> lore = new ArrayList<>(this.lore);
            lore.replaceAll(s -> parse(color(s.replaceAll("\\{leaderboard}", replacements[0])), player));
            meta.setLore(lore);
        }
        if (customModelData != -1)
            meta.setCustomModelData(customModelData);
        if (enchanted) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (hideNBT) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String toString(){
        return getFormattedItem(null, new String[0]).toString();
    }

}

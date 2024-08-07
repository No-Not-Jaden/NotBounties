package me.jadenp.notbounties.ui.gui;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.Head;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static me.jadenp.notbounties.utils.configuration.LanguageOptions.color;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.parse;

public class CustomItem {
    private final Material material;
    private final int amount;
    private final int customModelData;
    private final String name;
    private final List<String> lore;
    private final boolean enchanted;
    private final boolean hideNBT;
    private final boolean hideTooltip;
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
        hideTooltip = configurationSection.getBoolean("hide-tooltip");

        this.commands = configurationSection.isList("commands") ? configurationSection.getStringList("commands") : new ArrayList<>();
    }

    public CustomItem() {
        material = Material.STONE;
        amount = 1;
        customModelData = -1;
        name = null;
        lore = new ArrayList<>();
        enchanted = false;
        hideNBT = false;
        hideTooltip = false;
        commands = new ArrayList<>();
    }

    public CustomItem(Material material, int amount, int customModelData, String name, List<String> lore, boolean enchanted, boolean hideNBT, boolean hideTooltip, List<String> commands) {

        this.material = material;
        this.amount = amount;
        this.customModelData = customModelData;
        this.name = name;
        this.lore = lore;
        this.enchanted = enchanted;
        this.hideNBT = hideNBT;
        this.hideTooltip = hideTooltip;
        this.commands = commands;
    }


    public ItemStack getFormattedItem(OfflinePlayer player, String[] replacements){
        if (replacements == null || replacements.length == 0)
            replacements = new String[]{""};
        ItemStack itemStack = headID != null && material == Material.PLAYER_HEAD ? Head.createPlayerSkull(headID) : new ItemStack(material, amount);
        if (itemStack == null)
            return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;
        if (name != null)
            meta.setDisplayName(parse(color(name.replace("{leaderboard}", replacements[0])), player));
        if (!lore.isEmpty()) {
            List<String> lore = new ArrayList<>(this.lore);
            String[] finalReplacements = replacements;
            lore.replaceAll(s -> parse(color(s.replace("{leaderboard}", finalReplacements[0])), player));
            meta.setLore(lore);
        }
        if (customModelData != -1)
            meta.setCustomModelData(customModelData);
        if (enchanted) {
            if (NotBounties.isAboveVersion(20, 4)) {
                if (!meta.hasEnchantmentGlintOverride())
                    meta.setEnchantmentGlintOverride(true);
            } else {
                if (!hideNBT) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                itemStack.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
            }
        }
        if (hideNBT) {
            meta.getItemFlags().clear();
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(ItemFlag.HIDE_DYE);
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.values()[5]);
            Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
            meta.setAttributeModifiers(attributes);
        }
        if (hideTooltip && NotBounties.isAboveVersion(20, 4)) {
            meta.setHideTooltip(true);
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

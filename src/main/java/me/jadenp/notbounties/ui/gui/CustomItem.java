package me.jadenp.notbounties.ui.gui;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.Head;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

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
    private final Color color;

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
        color = parseColor(configurationSection.getString("color"));
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
        color = null;
    }

    public CustomItem(Material material, int amount, int customModelData, String name, List<String> lore, boolean enchanted, boolean hideNBT, boolean hideTooltip, List<String> commands, Color color) {

        this.material = material;
        this.amount = amount;
        this.customModelData = customModelData;
        this.name = name;
        this.lore = lore;
        this.enchanted = enchanted;
        this.hideNBT = hideNBT;
        this.hideTooltip = hideTooltip;
        this.commands = commands;
        this.color = color;
    }

    private String parseReplacements(String str, OfflinePlayer player, String[] replacements) {
        return parse(str.replace("{leaderboard}", replacements[0])
                .replace("{leaderboard_name}", replacements[1])
                .replace("{amount}", replacements[1])
                .replace("{tax}", replacements[2])
                .replace("{amount_tax}", replacements[3]), player);
    }


    public ItemStack getFormattedItem(OfflinePlayer player, String[] replacements){
        if (replacements == null)
            replacements = new String[]{"","","",""};
        if (replacements.length < 4) {
            String[] newReplacements = new String[]{"","","",""};
            System.arraycopy(replacements, 0, newReplacements, 0, replacements.length);
            replacements = newReplacements;
        }
        ItemStack itemStack = headID != null && material == Material.PLAYER_HEAD ? Head.createPlayerSkull(headID) : new ItemStack(material, amount);
        if (itemStack == null)
            return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;
        if (name != null)
            meta.setDisplayName(parseReplacements(name, player, replacements));
        if (!lore.isEmpty()) {
            List<String> newLore = new ArrayList<>(this.lore);
            String[] finalReplacements = replacements;
            newLore.replaceAll(s -> parseReplacements(s, player, finalReplacements));
            meta.setLore(newLore);
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
        if (color != null) {
            if (meta instanceof PotionMeta potionMeta) potionMeta.setColor(color);
            if (meta instanceof LeatherArmorMeta leatherArmorMeta) leatherArmorMeta.setColor(color);
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

    /**
     * Parses a string into a Bukkit Color.
     *
     * @param input The string representing the color (e.g., "red", "#FF0000", "255,0,0", "255 0 0").
     * @return A Bukkit Color object, or null if the input is invalid.
     */
    private static Color parseColor(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        input = input.trim().toLowerCase();

        Color parsedColor = getColorFromName(input);


        // Check for hex codes (#RRGGBB or RRGGBB)
        if (input.startsWith("#")) {
            input = input.substring(1); // Remove the leading #
        }
        if (input.matches("[0-9a-fA-F]{6}")) {
            try {
                int r = Integer.parseInt(input.substring(0, 2), 16);
                int g = Integer.parseInt(input.substring(2, 4), 16);
                int b = Integer.parseInt(input.substring(4, 6), 16);
                return Color.fromRGB(r, g, b);
            } catch (NumberFormatException ignored) {
                // could not get number from string
            }
        }

        // parse for R G B or R,G,B
        if (parsedColor == null)
            parsedColor = getRGB(" ", input);
        if (parsedColor == null)
            parsedColor = getRGB(",", input);
        return parsedColor;
    }

    /**
     * Parses an RGB string into a Bukkit Color
     * @param spacer The spacer between the RGB numbers.
     * @param input The RGB input string.
     * @return The parsed Bukkit Color, or null if it didn't match the pattern.
     */
    private static Color getRGB(String spacer, String input) {
        // Check for RGB format with spacer
        String number = "\\d{1,3}";
        if (input.matches( number + spacer + number + spacer + number)) {
            String[] parts = input.split(spacer);
            try {
                int r = Integer.parseInt(parts[0]);
                int g = Integer.parseInt(parts[1]);
                int b = Integer.parseInt(parts[2]);

                // Ensure values are within range
                if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                    return Color.fromRGB(r, g, b);
                }
            } catch (NumberFormatException ignored) {
                // could not get number from string
            }
        }
        return null;
    }

    /**
     * Get a Bukkit color from the name
     * @param input String input
     * @return A bukkit color representing the input, or null if the input doesn't match a color.
     */
    private static Color getColorFromName(String input) {
        // Check for named colors
        return switch (input) {
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "blue" -> Color.BLUE;
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            case "yellow" -> Color.YELLOW;
            case "aqua" -> Color.AQUA;
            case "fuchsia" -> Color.FUCHSIA;
            case "gray" -> Color.GRAY;
            case "lime" -> Color.LIME;
            case "maroon" -> Color.MAROON;
            case "navy" -> Color.NAVY;
            case "olive" -> Color.OLIVE;
            case "orange" -> Color.ORANGE;
            case "purple" -> Color.PURPLE;
            case "silver" -> Color.SILVER;
            case "teal" -> Color.TEAL;
            default -> null;
        };
    }

}

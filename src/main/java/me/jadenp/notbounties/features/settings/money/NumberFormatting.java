package me.jadenp.notbounties.features.settings.money;

import com.google.common.primitives.Floats;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.ui.gui.CustomItem;
import me.jadenp.notbounties.utils.ItemValue;
import me.jadenp.notbounties.utils.tasks.MultipleItemGive;
import me.jadenp.notbounties.utils.tasks.SingleItemGive;
import me.jadenp.notbounties.features.settings.integrations.external_api.EssentialsXClass;
import me.jadenp.notbounties.features.settings.integrations.external_api.PlaceholderAPIClass;
import me.jadenp.notbounties.features.settings.integrations.external_api.VaultClass;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Item;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.IntStream;



public class NumberFormatting {
    private static List<String> currency;
    private static List<String> customModelDatas;
    private static LinkedHashMap<String, Float> currencyValues;
    private static List<Float> currencyWeights;
    private static List<String> removeCommands;
    private static List<String> addCommands;
    private static String currencyPrefix = "";
    private static String currencySuffix = "";
    private static boolean useDivisions;
    private static LinkedHashMap<Long, String> nfDivisions = new LinkedHashMap<>();
    private static DecimalFormat decimalFormat;
    private static Locale locale;
    public enum CurrencyAddType {
        DESCENDING, FIRST, RATIO, BIMODAL
    }
    private static CurrencyAddType addSingleCurrency = CurrencyAddType.DESCENDING;
    private static boolean usingPapi = false;
    private static VaultClass vaultClass = null;
    private static boolean vaultEnabled;
    private static boolean overrideVault;
    private static boolean bountyItemsOverrideImmunity = false;
    public enum ManualEconomy {
        AUTOMATIC, PARTIAL, MANUAL
    }
    private static ManualEconomy manualEconomy;
    public enum BountyItemMode {
        ALLOW, DENY, EXCLUSIVE
    }
    private static BountyItemMode bountyItemMode;
    private static final Map<Material, ItemValue> itemValues = new HashMap<>();
    private static final Map<Enchantment, Double> enchantmentValues = new HashMap<>();
    private static double defaultItemValue = 1;
    private static double itemValueMultiplier = 1;
    private static boolean bountyItemsBuyItem = true;
    private static boolean tabCompleteItems = false;
    private static boolean bountyItemsDefaultGUI = false;
    public enum ItemValueMode {
        AUTO, ESSENTIALS, FILE, DISABLE
    }
    private static ItemValueMode bountyItemsUseItemValues = ItemValueMode.AUTO;
    private static boolean essentialsEnabled;
    private static EssentialsXClass essentialsXClass;
    private static Plugin plugin;

    public static void loadConfiguration(ConfigurationSection currencyOptions, ConfigurationSection numberFormatting, Plugin plugin) {
        NumberFormatting.plugin = plugin;
        essentialsEnabled = Bukkit.getServer().getPluginManager().isPluginEnabled("Essentials");
        if (essentialsEnabled)
            essentialsXClass = new EssentialsXClass();
        vaultEnabled = Bukkit.getServer().getPluginManager().isPluginEnabled("Vault");
        overrideVault = currencyOptions.getBoolean("override-vault");
        try {
            manualEconomy = ManualEconomy.valueOf(Objects.requireNonNull(currencyOptions.getString("manual-economy")).toUpperCase());
        } catch (IllegalArgumentException e) {
            manualEconomy = ManualEconomy.AUTOMATIC;
            plugin.getLogger().warning("Invalid manual-economy type!");
        }

        if (vaultEnabled && !overrideVault) {
            vaultClass = new VaultClass();
            NotBounties.debugMessage("Using Vault as currency!", false);
        }
        currency = new ArrayList<>();
        currencyWeights = new ArrayList<>();
        currencyValues = new LinkedHashMap<>();
        customModelDatas = new ArrayList<>();
        if (currencyOptions.isString("object")){
            currency = new ArrayList<>(Collections.singletonList(currencyOptions.getString("object")));
        } else if (currencyOptions.isList("object")){
            currency = currencyOptions.getStringList("object");
        }

        // check if all items are valid materials
        ListIterator<String> currencyIterator = currency.listIterator();
        while (currencyIterator.hasNext()){
            String currencyName = currencyIterator.next();
            String weight = "";
            String value = "";
            String customModelData = "-1";
            if (currencyName.contains("<") && currencyName.substring(currencyName.indexOf("<")).contains(">")){
                // has custom model data
                customModelData = currencyName.substring(currencyName.indexOf("<") + 1, currencyName.indexOf(">"));
                currencyName = currencyName.substring(0, currencyName.indexOf("<")) + currencyName.substring(currencyName.indexOf(">") + 1);
                if (!currencyName.contains(" "))
                    currencyIterator.set(currencyName);
            }
            customModelDatas.add(customModelData);
            // separate weight and value attached if there is any
            if (currencyName.contains(" ")){
                value = currencyName.substring(currencyName.indexOf(" ") + 1);
                currencyName = currencyName.substring(0, currencyName.indexOf(" "));
                if (value.contains(" ")){
                    weight = value.substring(value.indexOf(" ") + 1);
                    value = value.substring(0, value.indexOf(" "));
                }
                currencyIterator.set(currencyName);
            }
            // placeholder or item
            if (currencyName.contains("%")){
                if (!ConfigOptions.getIntegrations().isPapiEnabled() && !(vaultEnabled && !overrideVault)) {
                    plugin.getLogger().warning("Detected a placeholder as currency, but PlaceholderAPI is not enabled!");
                    plugin.getLogger().warning("Ignoring placeholder from currency.");
                    currencyIterator.remove();
                    continue;
                }
                usingPapi = true;
            } else {
                try {
                    Material.valueOf(currencyName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Could not get an item from: " + currencyName + "!");
                    plugin.getLogger().warning("Ignoring item from currency.");
                    currencyIterator.remove();
                    continue;
                }
            }
            if (value.isEmpty()) {
                currencyValues.put(currencyName, 1f);
                currencyWeights.add(0f);
            } else {
                try {
                    // has to be a whole number if using item
                    if (!currencyName.contains("%"))
                        currencyValues.put(currencyName, (float) Math.round(Float.parseFloat(value)));
                    else
                        currencyValues.put(currencyName, Float.parseFloat(value));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Could not get a number from value " + value + " after currency " + currencyName + "!");
                    currencyValues.put(currencyName, 1f);
                }
                if (weight.isEmpty())
                    currencyWeights.add(0f);
                else
                    try {
                        currencyWeights.add(Float.parseFloat(weight));
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Could not get a number from weight " + weight + " after currency " + currencyName + "!");
                        currencyWeights.add(0f);
                    }
            }
        }
        // in case all the currency is invalid
        if (currency.isEmpty() && currencyOptions.isSet("object")){
            if (!vaultEnabled || overrideVault)
                plugin.getLogger().info("No currency to use. Defaulting to DIAMOND as currency");
            currency.add("DIAMOND");
            currencyValues.put("DIAMOND", 1f);
            currencyWeights.add(0f);
        }


        if (currencyOptions.isSet("prefix"))
            currencyPrefix = LanguageOptions.color(Objects.requireNonNull(currencyOptions.getString("prefix")));
        if (currencyOptions.isSet("suffix"))
            currencySuffix = LanguageOptions.color(Objects.requireNonNull(currencyOptions.getString("suffix")));
        if (currencyOptions.isList("add-commands"))
            addCommands = currencyOptions.getStringList("add-commands");
        else addCommands = Collections.singletonList(currencyOptions.getString("add-commands"));

        if (currencyOptions.isList("remove-commands"))
            removeCommands = currencyOptions.getStringList("remove-commands");
        else removeCommands = Collections.singletonList(currencyOptions.getString("remove-commands"));

        try {
            addSingleCurrency = CurrencyAddType.valueOf(Objects.requireNonNull(currencyOptions.getString("add-single-currency")).toUpperCase());
        } catch (IllegalArgumentException e) {
            addSingleCurrency = CurrencyAddType.DESCENDING;
            plugin.getLogger().warning("Invalid add-single-currency type!");
        }

        // warning for not enough remove/add commands
        if (!vaultEnabled || overrideVault) {
            if (addSingleCurrency == CurrencyAddType.BIMODAL) {
                if (currency.size() < 2) {
                    plugin.getLogger().warning("Using bimodal currency but there aren't 2 currencies to use!");
                } else {
                    if (currency.get(0).contains("%") && addCommands.isEmpty())
                        plugin.getLogger().warning("Detected a placeholder for the first currency, but there are no add commands!");
                    if (currency.get(1).contains("%") && removeCommands.isEmpty())
                        plugin.getLogger().warning("Detected a placeholder for the second currency, but there are no remove commands!");
                }

            } else {
                int placeholderCurrencies = (int) currency.stream().filter(currencyName -> currencyName.contains("%")).count();
                if (addCommands.size() < placeholderCurrencies)
                    plugin.getLogger().log(Level.WARNING, "Detected {0} placeholder(s) as currency, but there are only {1} add commands!", new Object[]{placeholderCurrencies, addCommands.size()});
                if (removeCommands.size() < placeholderCurrencies)
                    plugin.getLogger().log(Level.WARNING, "Detected {0} placeholder(s) as currency, but there are only {1} remove commands!", new Object[]{placeholderCurrencies, removeCommands.size()});
            }
        }


        useDivisions = numberFormatting.getBoolean("use-divisions");

        String localeString = numberFormatting.getString("format-locale");
        String pattern = numberFormatting.getString("pattern");
        if (localeString == null) {
            // this shouldn't ever get used because of the auto-generation of missing options
            plugin.getLogger().warning("No number formatting locale set! Defaulting to en-US.");
            localeString = "en-US";
        }
        if (pattern == null) {
            // this shouldn't get used either
            plugin.getLogger().warning("No number formatting pattern set! Defaulting to \"#,###.##\"");
            pattern = "#,###.##";
        }

        locale = Locale.forLanguageTag(localeString);

        decimalFormat = new DecimalFormat(pattern, new DecimalFormatSymbols(locale));

        NumberFormatting.nfDivisions.clear();
        Map<Long, String> preDivisions = new HashMap<>();
        for (String s : Objects.requireNonNull(numberFormatting.getConfigurationSection("divisions")).getKeys(false)) {
            if (s.equals("decimals"))
                continue;
            try {
                preDivisions.put(Long.parseLong(s), numberFormatting.getString("divisions." + s));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Division is not a number: " + s);
            }
        }
        NumberFormatting.nfDivisions = (LinkedHashMap<Long, String>) NumberFormatting.sortByValue(preDivisions);

        // bounty items
        try {
            bountyItemMode = BountyItemMode.valueOf(Objects.requireNonNull(currencyOptions.getString("bounty-items.mode")).toUpperCase());
        } catch (IllegalArgumentException e) {
            bountyItemMode = BountyItemMode.DENY;
            plugin.getLogger().log(Level.WARNING, "Invalid bounty items mode: {0}", currencyOptions.getString("bounty-items.mode"));
        }
        if (currencyOptions.isSet("bounty-items.override-immunity"))
            bountyItemsOverrideImmunity = currencyOptions.getBoolean("bounty-items.override-immunity");

        if (currencyOptions.isSet("bounty-items.buy-items"))
            bountyItemsBuyItem = currencyOptions.getBoolean("bounty-items.buy-items");

        if (currencyOptions.isSet("bounty-items.tab-complete"))
            tabCompleteItems = currencyOptions.getBoolean("bounty-items.tab-complete");

        if (currencyOptions.isSet("bounty-items.default-gui"))
            bountyItemsDefaultGUI = currencyOptions.getBoolean("bounty-items.default-gui");

        ItemValueMode selectedMode;
        try {
            selectedMode = ItemValueMode.valueOf(currencyOptions.getString("bounty-items.item-values"));
        } catch (IllegalArgumentException e) {
            selectedMode = ItemValueMode.AUTO;
            plugin.getLogger().log(Level.WARNING, "Invalid item values mode: {0}", currencyOptions.getString("bounty-items.item-values"));
        }
        if (selectedMode == ItemValueMode.AUTO) {
            if (essentialsEnabled)
                bountyItemsUseItemValues = ItemValueMode.ESSENTIALS;
            else
                bountyItemsUseItemValues = ItemValueMode.FILE;
        } else {
            bountyItemsUseItemValues = selectedMode;
        }

        File itemValuesFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "item-values.yml");
        if (!itemValuesFile.exists()) {
            NotBounties.getInstance().saveResource("item-values.yml", false);
            plugin.getLogger().info("Created new item-values.yml file.");
        }
        itemValues.clear();
        YamlConfiguration itemValuesConfig = YamlConfiguration.loadConfiguration(itemValuesFile);
        if (itemValuesConfig.isSet("default"))
            defaultItemValue = itemValuesConfig.getDouble("default");
        if (itemValuesConfig.isSet("multiplier"))
            itemValueMultiplier = itemValuesConfig.getDouble("multiplier");
        if (itemValuesConfig.isConfigurationSection("item-values"))
            for (String key : Objects.requireNonNull(itemValuesConfig.getConfigurationSection("item-values")).getKeys(false)) {
                String materialName = key;
                String customModelData = "-1";
                // check if materialName has two arrows <>
                if (materialName.contains("<") && materialName.substring(materialName.indexOf("<")).contains(">")) {
                    // has custom model data
                    customModelData = materialName.substring(materialName.indexOf("<") + 1, materialName.indexOf(">"));
                    materialName = materialName.substring(0, materialName.indexOf("<"));
                }
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in item-values.yml file: \"" + materialName + "\"");
                    continue;
                }
                double value = itemValuesConfig.getDouble("item-values." + key);
                if (itemValues.containsKey(material))
                    itemValues.get(material).addValue(customModelData, value);
                else
                    itemValues.put(material, new ItemValue().addValue(customModelData, value));
            }
        if (itemValuesConfig.isConfigurationSection("enchantment-values"))
            for (String key : Objects.requireNonNull(itemValuesConfig.getConfigurationSection("enchantment-values")).getKeys(false)) {
                try {
                    Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase()));
                    if (enchantment == null) {
                        plugin.getLogger().log(Level.WARNING, "Unknown enchantment for enchantment-values: {0}", key);
                        continue;
                    }
                    enchantmentValues.put(enchantment, itemValuesConfig.getDouble("enchantment-values." + key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown enchantment for enchantment-values: " + key);
                    NotBounties.debugMessage(e.toString(), true);
                }
            }
    }

    public static boolean isUsingDecimals() {
        return usingPapi || shouldUseDecimals();
    }

    public static double getItemValue(ItemStack item) throws ExcludedItemException {
        if (item == null)
            return 0;
        switch (bountyItemsUseItemValues) {
            case FILE -> {
                Material material = item.getType();
                String customModelData = "-1";
                if (item.getItemMeta() != null) {
                    if (item.getItemMeta().hasItemModel()) {
                        customModelData = CustomItem.getItemModel(item.getItemMeta().getItemModel());
                    } else if (item.getItemMeta().hasCustomModelData()) {
                        customModelData = item.getItemMeta().getCustomModelData() + "";
                    }
                }

                // check if material is a currency
                for (String s : currency) {
                    if (material.toString().equalsIgnoreCase(s))
                        return (currencyValues.get(s) + getEnchantmentValue(item)) * item.getAmount();
                }
                // check if registered as an item value
                if (itemValues.containsKey(material)) {
                    double value = itemValues.get(material).getValue(customModelData);
                    if (value == 0)
                        throw new ExcludedItemException(material.toString());
                    if (value != -1)
                        return (value * itemValueMultiplier + getEnchantmentValue(item)) * item.getAmount();
                }
                if (defaultItemValue == 0)
                    throw new ExcludedItemException(material.toString());
                return (defaultItemValue + getEnchantmentValue(item)) * item.getAmount();
            }
            case ESSENTIALS -> {
                BigDecimal price = essentialsXClass.getItemValue(item);
                if (price == null) {
                    if (defaultItemValue == 0)
                        throw new ExcludedItemException(item.getType().toString());
                    return defaultItemValue * item.getAmount();
                }
                return price.doubleValue() * item.getAmount();
            }
            default -> {
                return 0;
            }
        }

    }

    private static double getEnchantmentValue(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasEnchants())
            return 0;
        double value = 0;
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            if (!enchantmentValues.containsKey(entry.getKey()))
                continue;
            value += enchantmentValues.get(entry.getKey()) * entry.getValue();
        }
        return value * itemValueMultiplier;
    }


    public static double getTotalValue(List<ItemStack> items) throws ExcludedItemException {
        if (items.isEmpty())
            return 0;
        double sum = 0.0;
        for (ItemStack item : items) {
            if (item != null) {
                double itemValue = getItemValue(item);
                sum += itemValue;
            }
        }
        return sum;
    }

    public static String formatNumber(String number) {
        if (number == null || number.isEmpty())
            return "";
        if (number.startsWith(currencyPrefix) && !currencyPrefix.isEmpty())
            return currencyPrefix + formatNumber(number.substring(currencyPrefix.length()));
        if (number.endsWith(currencySuffix) && !currencySuffix.isEmpty())
            return formatNumber(number.substring(0, number.length() - currencySuffix.length())) + currencySuffix;
        if (isNumber(number))
            return formatNumber(tryParse(number));
        if (!isNumber(number.substring(0, 1))) {
            // first digit isn't a number
            return number.charAt(0) + formatNumber(number.substring(1));
        }
        return formatNumber(number.substring(0, number.length() - 1)) + number.charAt(number.length() - 1);

    }

    public static double findFirstNumber(String str) {
        if (str.isEmpty())
            return 0;
        if (isNumber(str))
            return Double.parseDouble(str);
        if (isNumber(str.substring(0, 1)))
            return findFirstNumber(str.substring(0, str.length() - 1));
        return findFirstNumber(str.substring(1));
    }


    public static boolean isNumber(String str) {
        try {
            tryParse(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Format a number with number formatting options in the config
     *
     * @param number Number to be formatted
     * @return formatted number
     */
    public static String formatNumber(double number) {
        if (useDivisions) {
            // set divisions
            return setDivision(number);
        }
        return ruz(decimalFormat.format(number));
    }

    /**
     * Get the full number value without any formatting
     *
     * @param number to get
     * @return String value of number
     */
    public static String getValue(double number) {
        return ruz(String.format("%f",number));
    }

    /**
     * Remove Unnecessary Zeros (RUZ)
     *
     * @param value value to check zeros for
     * @return a value with no unnecessary Zeros
     */
    private static String ruz(String value) {
        if (value.isEmpty())
            return "";
        while (value.contains(Character.toString(decimalFormat.getDecimalFormatSymbols().getDecimalSeparator())) && (value.charAt(value.length() - 1) == '0' || value.charAt(value.length() - 1) == decimalFormat.getDecimalFormatSymbols().getDecimalSeparator()))
            value = value.substring(0, value.length() - 1);
        return value;
    }

    public static String setDivision(Double number) {
        for (Map.Entry<Long, String> entry : nfDivisions.entrySet()) {
            if (number / entry.getKey() >= 1) {
                return ruz(decimalFormat.format(number / entry.getKey())) + entry.getValue();
            }
        }
        return ruz(decimalFormat.format(number));
    }


    public static Map<Material, Long> doRemoveCommands(Player p, double amount, List<ItemStack> additionalItems) throws NotEnoughCurrencyException {
        if (manualEconomy == ManualEconomy.MANUAL) {
            for (String removeCommand : removeCommands) {
                if (removeCommand.isEmpty())
                    continue;
                String command = removeCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
            return new EnumMap<>(Material.class);
        }

        if (vaultEnabled && !overrideVault) {
            if (vaultClass.withdraw(p, amount)) {
                return new EnumMap<>(Material.class);
            } else {
                plugin.getLogger().warning("Error withdrawing currency with vault! This could be from someone trying to dupe or lag on the server.");
                throw new NotEnoughCurrencyException("Vault could not withdraw " + amount +  " from " + p.getName());
            }
        }
        if (currency.isEmpty()){
            plugin.getLogger().warning("Currency is not set up! Run /currency in-game to fix.");
            return new EnumMap<>(Material.class);
        }
        Map<Material, Long> removedItems = new EnumMap<>(Material.class);
        if (currency.size() > 1) {
            if (addSingleCurrency == CurrencyAddType.BIMODAL) {
                // just do remove commands
                for (String removeCommand : removeCommands) {
                    if (removeCommand.isEmpty())
                        continue;
                    String command = removeCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount)));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
                }
                if (!currency.get(1).contains("%")) {
                    removeItem(p, Material.valueOf(currency.get(1)), (long) amount, customModelDatas.get(1));
                    removedItems.put(Material.valueOf(currency.get(1)), (long) amount);
                }
                return removedItems;
            }
            List<String> modifiedRemoveCommands = new ArrayList<>(removeCommands);
            // add empty spaces in list for item currencies
            for (int i = 0; i < currency.size(); i++) {
                if (!currency.get(i).contains("%"))
                    modifiedRemoveCommands.add(i, "");
            }
            float[] currencyWeightsCopy = Floats.toArray(currencyWeights);
            float[] currencyValuesCopy = Floats.toArray(currencyValues.values());

            double[] balancedRemove = balanceRemoveCurrency(amount, currencyWeightsCopy, getSortedBalance(p, additionalItems), currencyValuesCopy);
            if (modifiedRemoveCommands.size() < balancedRemove.length - 1) {
                plugin.getLogger().warning("There are not enough remove commands for your currency! Currency will not be removed properly!");
            }
            for (int i = 0; i < Math.min(balancedRemove.length-1, modifiedRemoveCommands.size()); i++) {
                if (currency.get(i).contains("%")) {
                    String command = modifiedRemoveCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(balancedRemove[i])));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
                } else {
                    removeItem(p, Material.valueOf(currency.get(i)), (long) (balancedRemove[i]), customModelDatas.get(i));
                    removedItems.put(Material.valueOf(currency.get(i)), (long) (balancedRemove[i]));
                }
            }
            // refund amount
            if (balancedRemove[balancedRemove.length-1] > 0) {
                doAddCommands(p, balancedRemove[balancedRemove.length-1]);
            }
            // do the rest of the remove commands
            for (int i = balancedRemove.length-1; i < modifiedRemoveCommands.size(); i++) {
                if (modifiedRemoveCommands.get(i).isEmpty())
                    continue;
                String command = modifiedRemoveCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
        } else {
            // just do remove commands
            for (String removeCommand : removeCommands) {
                if (removeCommand.isEmpty())
                    continue;
                String command = removeCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
            if (!currency.get(0).contains("%")) {
                removeItem(p, Material.valueOf(currency.get(0)), (long) amount, customModelDatas.get(0));
                removedItems.put(Material.valueOf(currency.get(0)), (long) amount);
            }
        }
        return removedItems;
    }

    /**
     * Calculate what combination of items should be removed from a player's inventory
     * @param amount Currency amount to be removed
     * @param currencyWeights Array of currency weights - The weight of each currency, higher weights are removed more
     * @param currentBalance Array of currency balance - How much of each currency the player has
     * @param currencyValues Array of currency values - How much each currency is worth compared to the amount value
     * @return An array of doubles sized (currency.size() + 1). Each double relates to the amount of currency that should be removed, the last double represents the amount of currency that should be refunded.
     */
    public static double @NotNull [] balanceRemoveCurrency(double amount, float[] currencyWeights, double[] currentBalance, float[] currencyValues){
        float totalWeight = addUp(currencyWeights);
        if (totalWeight == 0){
            Arrays.fill(currencyWeights, 1);
            totalWeight = currencyWeights.length;
        }

        double[] balancedRemove = new double[currency.size() + 1];
        double totalAmount = amount;
        boolean balancedAttempt = false;
        while (amount > 0) {
            for (int i = 0; i < currency.size(); i++) {
                if (currencyWeights[i] == 0)
                    continue;
                // amount to be taken out of balance with currency weight accounted for
                double shareAmount = totalAmount * (currencyWeights[i] / totalWeight);
                if (currentBalance[i] >= shareAmount) {
                    // player has enough currency
                    balancedRemove[i] += shareAmount;
                    currentBalance[i] -= shareAmount;
                    amount-= shareAmount;
                } else {
                    // player doesn't have enough currency
                    balancedRemove[i] += currentBalance[i];
                    amount -= currentBalance[i];
                    currentBalance[i] = 0;
                    currencyWeights[i] = 0;
                }
            }
            totalWeight = addUp(currencyWeights);
            totalAmount = amount;
            if (totalWeight == 0){
                Arrays.fill(currencyWeights, 1);
                totalWeight = currencyWeights.length;
                if (balancedAttempt) {
                    plugin.getLogger().log(Level.WARNING, "Trying to remove currency without checking balance! Amount exploited: {0}", amount);
                    break;
                }
                balancedAttempt =  true;
            }
        }


        // round currencies that are items and can't have decimals
        // set real values from currencyValues
        double excessRounding = 0;
        for (int i = 0; i < currency.size(); i++) {
            if (!currency.get(i).contains("%")){
                double currencyRemove = balancedRemove[i];
                balancedRemove[i] = (long) currencyRemove;
                currentBalance[i] += currencyRemove - balancedRemove[i];
                currentBalance[i] += balancedRemove[i] % currencyValues[i];
                excessRounding += currencyRemove - balancedRemove[i];
                excessRounding += balancedRemove[i] % currencyValues[i];
                balancedRemove[i] = (long) (balancedRemove[i] / currencyValues[i]);
            } else {
                balancedRemove[i] /= currencyValues[i];
            }
        }
        // adding the excess currency from rounding back onto anywhere it can
        for (int i = 0; i < currency.size(); i++) {
            if (currency.get(i).contains("%")) {
                if (currentBalance[i] == 0)
                    continue;
                if (currentBalance[i] >= excessRounding) {
                    balancedRemove[i] += excessRounding / currencyValues[i];
                    currentBalance[i] -= excessRounding;
                    break;
                }
                balancedRemove[i] += currentBalance[i] / currencyValues[i];
                excessRounding -= currentBalance[i];
                currentBalance[i] = 0;
            } else if (excessRounding >= currencyValues[i]){
                if (currentBalance[i] == 0)
                    continue;
                if (currentBalance[i] >= excessRounding) {
                    balancedRemove[i] += (long) (excessRounding / currencyValues[i]);
                    currentBalance[i] -= excessRounding;
                    currentBalance[i] += currencyValues[i] > 1 ? excessRounding % currencyValues[i] : excessRounding - (long) excessRounding;
                    double originalRounding = excessRounding;
                    excessRounding += currencyValues[i] > 1 ? excessRounding % currencyValues[i] : excessRounding - (long) excessRounding;
                    excessRounding -= originalRounding;

                    continue;
                }
                balancedRemove[i] += (long) (currentBalance[i] / currencyValues[i]);
                excessRounding -= (long) currentBalance[i];
                excessRounding += currentBalance[i] % currencyValues[i];
                currentBalance[i] = 0;
            }
        }
        // remove an extra if there is still excess
        if (excessRounding > 0.01) {
            for (int i = 0; i < currency.size(); i++) {
                if (currentBalance[i] == 0)
                    continue;
                balancedRemove[i]++;
                double overDraw = currencyValues[i] - excessRounding;
                excessRounding-= currencyValues[i];
                if (overDraw == 0)
                    break;
                if (overDraw > 0) {
                    balancedRemove[balancedRemove.length-1] += overDraw;
                    break;
                }
            }
        }

        return balancedRemove;
    }

    public static double parseCurrency(String amount){
        if (amount == null)
            return 0;
        amount = ChatColor.stripColor(amount);
        // remove currency prefix and suffix
        String blankPrefix = ChatColor.stripColor(NumberFormatting.currencyPrefix);
        String blankSuffix = ChatColor.stripColor(NumberFormatting.currencySuffix);
        if (!blankPrefix.isEmpty() && amount.startsWith(blankPrefix))
            amount = amount.substring(blankPrefix.length());
        if (!blankSuffix.isEmpty() && amount.endsWith(blankSuffix))
            amount = amount.substring(0, amount.length() - blankSuffix.length());
        // get division or remove any non-numbers from the end
        long multiplyValue = 1;
        StringBuilder divisionString = new StringBuilder();
        while (!amount.isEmpty() && !isNumber(amount.substring(amount.length()-1))) {
            divisionString.append(amount.substring(amount.length() - 1));
            amount = amount.substring(0, amount.length()-1);
        }
        if (amount.isEmpty())
            return 0;
        if (useDivisions && nfDivisions.containsValue(divisionString.toString())) {
            for (Map.Entry<Long, String> entry : nfDivisions.entrySet()) {
                if (entry.getValue().contentEquals(divisionString)) {
                    multiplyValue = entry.getKey();
                    break;
                }
            }
        }

        try {
            return tryParse(amount) * multiplyValue;
        } catch (NumberFormatException ignored){
            return NumberFormatting.findFirstNumber(amount) * multiplyValue;
        }
    }

    public static LinkedHashMap<Integer, Float> sortByFloatValue(Map<Integer, Float> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<Integer, Float>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        // put data from sorted list to hashmap
        LinkedHashMap<Integer, Float> temp = new LinkedHashMap<>();
        for (Map.Entry<Integer, Float> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static double[] descendingAddCurrency(double amount, float[] currencyWeights, float[] currencyValues) {
        LinkedHashMap<Integer, Float> weightIndexes = new LinkedHashMap<>();
        for (int i = 0; i < currencyWeights.length; i++) {
            weightIndexes.put(i, currencyWeights[i]);
        }
        weightIndexes = sortByFloatValue(weightIndexes);


        double[] amounts = new double[currencyWeights.length];
        for (Map.Entry<Integer, Float> entry : weightIndexes.entrySet()) {
            if (currency.get(entry.getKey()).contains("%")) {
                amounts[entry.getKey()] = amount / currencyValues[entry.getKey()];
                break;
            }
            long itemAmount = (long) (amount / currencyValues[entry.getKey()]);
            amount-= itemAmount * currencyValues[entry.getKey()];
            amounts[entry.getKey()] = itemAmount;
        }
        return amounts;
    }


    public static double[] balanceAddCurrency(double amount, float[] currencyWeights, float[] currencyValues){
        float totalWeight = addUp(currencyWeights);
        if (totalWeight == 0){
            Arrays.fill(currencyWeights, 1);
            totalWeight = currencyWeights.length;
        }
        double[] balancedAdd = new double[currency.size()];
        for (int i = 0; i < currency.size(); i++) {
            if (currencyWeights[i] == 0)
                continue;
            balancedAdd[i] = amount * (currencyWeights[i] / totalWeight);
        }
        double excessRounding = 0;
        for (int i = 0; i < currency.size(); i++) {
            if (!currency.get(i).contains("%")){
                double currencyRemove = balancedAdd[i];
                balancedAdd[i] = (long) currencyRemove;
                excessRounding += currencyRemove - balancedAdd[i];
                excessRounding += balancedAdd[i] % currencyValues[i];
                balancedAdd[i] = (long) balancedAdd[i] / currencyValues[i];
            } else {
                balancedAdd[i] /= currencyValues[i];
            }
        }
        // adding the excess currency from rounding back onto anywhere it can
        for (int i = 0; i < currency.size(); i++) {
            if (currency.get(i).contains("%")) {
                balancedAdd[i] += excessRounding / currencyValues[i];
                break;
            } else if (excessRounding >= currencyValues[i]){
                balancedAdd[i] += (long) excessRounding / currencyValues[i];
                double originalRounding = excessRounding;
                excessRounding += currencyValues[i] > 1 ? excessRounding % currencyValues[i] : excessRounding - (long) excessRounding;
                excessRounding -= originalRounding;
            }
        }

        return balancedAdd;

    }

    public static float addUp(float[] numbers){
        float total = 0;
        for (float f : numbers)
            total+= f;
        return total;
    }

    public static VaultClass getVaultClass() {
        return vaultClass;
    }

    public static void doAddCommands(Player p, double amount) {
            NotBounties.debugMessage("Doing add commands for " + p.getName() + ". Amount: " + amount, false);
        if (manualEconomy == ManualEconomy.MANUAL) {
            for (String addCommand : addCommands) {
                if (addCommand.isEmpty())
                    continue;
                String command = addCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount / Floats.toArray(currencyValues.values())[0])));
                NotBounties.debugMessage("Executing command: '" + command + "'", false);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));

            }
            NotBounties.debugMessage("Manual economy is enabled, no more actions will be performed.", false);
            return;
        }

        if (vaultEnabled && !overrideVault) {
            if (vaultClass.deposit(p, amount)) {
                NotBounties.debugMessage("Deposited money with vault!", false);
                return;
            } else {
                plugin.getLogger().warning("Error depositing currency with vault!");
            }
        }
        if (currency.isEmpty()){
            plugin.getLogger().warning("Currency is not set up! Run /currency in-game to fix.");
            return;
        }
        List<String> modifiedAddCommands = new ArrayList<>(addCommands);
        // add empty spaces in list for item currencies
        NotBounties.debugMessage("Currency: ", false);
        for (int i = 0; i < currency.size(); i++) {
            if (!currency.get(i).contains("%")) {
                modifiedAddCommands.add(i, "");
                NotBounties.debugMessage("(item) " + currency.get(i), false);
            } else {
                NotBounties.debugMessage(currency.get(i), false);
            }
        }
        if (addSingleCurrency == CurrencyAddType.RATIO && currency.size() > 1) {
            float[] currencyWeightsCopy = Floats.toArray(currencyWeights);
            float[] currencyValuesCopy = Floats.toArray(currencyValues.values());
            double[] balancedAdd = balanceAddCurrency(amount, currencyWeightsCopy, currencyValuesCopy);
            if (modifiedAddCommands.size() < balancedAdd.length) {
                plugin.getLogger().warning("There are not enough add commands for your currency! Currency will not be added properly!");
            }
            for (int i = 0; i < Math.min(balancedAdd.length, modifiedAddCommands.size()); i++) {
                if (currency.get(i).contains("%")) {
                    String command = modifiedAddCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(balancedAdd[i])));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
                } else {
                    ItemStack item = new ItemStack(Material.valueOf(currency.get(i)));
                    if (!customModelDatas.get(i).equals("-1")) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            if (customModelDatas.get(i).contains(":")) {
                                meta.setItemModel(CustomItem.getItemModel(customModelDatas.get(i)));
                            } else {
                                meta.setCustomModelData(Integer.parseInt(customModelDatas.get(i)));
                            }
                            item.setItemMeta(meta);
                        }
                    }
                    givePlayer(p, item, (long) balancedAdd[i]);
                }
            }
            // do the rest of the add commands
            for (int i = balancedAdd.length; i < modifiedAddCommands.size(); i++) {
                if (modifiedAddCommands.get(i).isEmpty())
                    continue;
                String command = modifiedAddCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
        } else if (addSingleCurrency == CurrencyAddType.FIRST) {
            // remove other currency commands
            IntStream.range(1, currency.size()).forEach(modifiedAddCommands::remove);
            // just do add commands
            for (String addCommand : modifiedAddCommands) {
                if (addCommand.isEmpty())
                    continue;
                String command = addCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount / Floats.toArray(currencyValues.values())[0])));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
            // give item if using
            if (!currency.get(0).contains("%")) {
                ItemStack item = new ItemStack(Material.valueOf(currency.get(0)));
                if (!customModelDatas.get(0).equals("-1")) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (customModelDatas.get(0).contains(":")) {
                            meta.setItemModel(CustomItem.getItemModel(customModelDatas.get(0)));
                        } else {
                            meta.setCustomModelData(Integer.parseInt(customModelDatas.get(0)));
                        }
                        item.setItemMeta(meta);
                    }
                }
                givePlayer(p, item, (long) (amount / Floats.toArray(currencyValues.values())[0]));
            }
        } else if (addSingleCurrency == CurrencyAddType.BIMODAL) {
            // do all the add commands with the first currency
            for (String addCommand : addCommands) {
                if (addCommand.isEmpty())
                    continue;
                String command = addCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount / Floats.toArray(currencyValues.values())[0])));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
            // give item if using
            if (!currency.get(0).contains("%")) {
                ItemStack item = new ItemStack(Material.valueOf(currency.get(0)));
                if (!customModelDatas.get(0).equals("-1")) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (customModelDatas.get(0).contains(":")) {
                            meta.setItemModel(CustomItem.getItemModel(customModelDatas.get(0)));
                        } else {
                            meta.setCustomModelData(Integer.parseInt(customModelDatas.get(0)));
                        }
                        item.setItemMeta(meta);
                    }
                }
                givePlayer(p, item, (long) (amount / Floats.toArray(currencyValues.values())[0]));
            }
        } else {
            // descending
            float[] currencyWeightsCopy = Floats.toArray(currencyWeights);
            float[] currencyValuesCopy = Floats.toArray(currencyValues.values());
            NotBounties.debugMessage("Input Calculations: " + amount + " | " + Arrays.toString(currencyWeightsCopy) + " | " + Arrays.toString(currencyValuesCopy), false);
            double[] descendingAdd = descendingAddCurrency(amount, currencyWeightsCopy, currencyValuesCopy);
            NotBounties.debugMessage("Output: " + Arrays.toString(descendingAdd), false);

            if (modifiedAddCommands.size() < descendingAdd.length) {
                plugin.getLogger().warning("There are not enough add commands for your currency! Currency will not be added properly!");
            }
            for (int i = 0; i < Math.min(descendingAdd.length, modifiedAddCommands.size()); i++) {
                if (currency.get(i).contains("%")) {
                    String command = modifiedAddCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(descendingAdd[i])));
                    NotBounties.debugMessage("Executing command for " + currency.get(i) + ": '" + command + "'", false);
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
                } else {
                    ItemStack item = new ItemStack(Material.valueOf(currency.get(i)));
                    if (!customModelDatas.get(i).equals("-1")) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            if (customModelDatas.get(i).contains(":")) {
                                meta.setItemModel(CustomItem.getItemModel(customModelDatas.get(i)));
                            } else {
                                meta.setCustomModelData(Integer.parseInt(customModelDatas.get(i)));
                            }
                            item.setItemMeta(meta);
                        }
                    }
                    NotBounties.debugMessage("Giving item: " + currency.get(i) + " to " + p.getName() + ". Amount: " + (long) descendingAdd[i], false);
                    givePlayer(p, item, (long) descendingAdd[i]);
                }
            }
            // do the rest of the add commands
            for (int i = descendingAdd.length; i < modifiedAddCommands.size(); i++) {
                if (modifiedAddCommands.get(i).isEmpty())
                    continue;
                String command = modifiedAddCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
        }
    }

    public static void removeItem(Player player, Material material, long amount, String customModelData) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                if (isCorrectMaterial(contents[i], material, customModelData)) {
                    if (contents[i].getAmount() > amount) {
                        contents[i].setAmount((int) (contents[i].getAmount() - amount));
                        break;
                    } else if (contents[i].getAmount() < amount) {
                        amount -= contents[i].getAmount();
                        contents[i] = null;
                    } else {
                        contents[i] = null;
                        break;
                    }
                }
            }
        }
        player.getInventory().setContents(contents);
    }

    /**
     * Try to remove items from a player's inventory. If the player doesn't have all the items, or update is false, they won't be removed
     * @param player Player to remove items from
     * @param items Items to be removed
     * @param update Whether the player's inventory should be updated
     * @return A string of missing items, or an empty string if the items were removed
     */
    public static String removeItems(Player player, List<ItemStack> items, boolean update) {
        ItemStack[] contents = player.getInventory().getContents().clone();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                ListIterator<ItemStack> iIterator = items.listIterator();
                while (iIterator.hasNext()) {
                    ItemStack item = iIterator.next();
                    if (contents[i].isSimilar(item)) {
                        if (contents[i].getAmount() > item.getAmount()) {
                            contents[i].setAmount(contents[i].getAmount() - item.getAmount());
                            iIterator.remove();
                        } else if (contents[i].getAmount() < item.getAmount()) {
                            item.setAmount(item.getAmount() - contents[i].getAmount());
                            contents[i] = null;
                            break;
                        } else {
                            iIterator.remove();
                            contents[i] = null;
                            break;
                        }
                    }
                }
            }
        }
        if (items.isEmpty() && update)
            player.getInventory().setContents(contents);
        else
            return listItems(items, 'x');
        return "";
    }

    /**
     * Lists items in a string in the format: (material)x(amount),(material)x(amount),...
     * @param items Items to be listed
     * @param amountDenote The character you want to use between the material and amount
     * @return A list of items
     */
    public static String listItems(List<ItemStack> items, char amountDenote) {
        // iterate through requested items to get missing items and their amounts and add to string builder
        StringBuilder itemString = new StringBuilder();
        for (ItemStack itemStack : items) {
            if (itemStack == null)
                continue;
            if (!itemString.isEmpty())
                itemString.append(",");
            itemString.append(itemStack.getType()).append(amountDenote).append(itemStack.getAmount());
        }
        return itemString.toString();
    }

    /**
     * Lists items in a string in the format: (material)x(amount),(material)x(amount),...
     * @param items Items to be listed
     * @param amountDenote The character you want to use between the material and amount
     * @return A base component array with hover-able text
     */
    public static BaseComponent[] listHoverItems(List<ItemStack> items, char amountDenote) {
        items.removeIf(Objects::isNull);
        BaseComponent[] components = new BaseComponent[items.size()];
        for (int i = 0; i < items.size(); i++) {
            ItemStack itemStack = items.get(i);
            String nameString = "";
            if (i > 0)
                nameString = nameString + ",";
            nameString = nameString + itemStack.getType().name() + amountDenote + itemStack.getAmount();
            TextComponent name = LanguageOptions.getTextComponent(nameString);
            name.setHoverEvent(getHoverEvent(itemStack));
            components[i] = name;
        }
        return components;
    }

    private static HoverEvent getHoverEvent(ItemStack itemStack) {
        String nbt = null;
        if (itemStack.getItemMeta() != null)
            nbt = NotBounties.getServerVersion() >= 18 ? itemStack.getItemMeta().getAsString() : itemStack.getItemMeta().toString();
        ItemTag itemTag = ItemTag.ofNbt(nbt);
        // item nbt doesn't work for 1.20.6 - could possibly be fixed manually by fixing the json
        return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new Item(itemStack.getType().getKey().toString(), itemStack.getAmount(), itemTag));
    }

    public static boolean checkBalance(Player player, double amount) {
        if (vaultEnabled && !overrideVault)
            return vaultClass.checkBalance(player, amount);
        double bal = getBalance(player);
        if (!usingPapi)
            bal = (long) bal;
        return bal >= amount;
    }

    public static boolean shouldUseDecimals(){
        if (vaultEnabled && !overrideVault)
            return true;
        for (String c : currency) {
            if (c.contains("%"))
                return true;
        }
        return false;
    }



    public static void givePlayer(Player p, ItemStack itemStack, long amount) {
        SingleItemGive delayedGive = new SingleItemGive(p, itemStack, amount);
        delayedGive.setTaskImplementation(NotBounties.getServerImplementation().entity(p).runAtFixedRate(delayedGive,1, 5));
    }

    public static void givePlayerInstantly(Player p, ItemStack itemStack, long amount) {
        while (amount > 0) {
            if (amount > itemStack.getMaxStackSize()) {
                itemStack.setAmount(itemStack.getMaxStackSize());
                amount -= itemStack.getMaxStackSize();
            } else {
                itemStack.setAmount((int) amount);
                amount = 0;
            }
            HashMap<Integer, ItemStack> leftOver = new HashMap<>((p.getInventory().addItem(itemStack)));
            if (!leftOver.isEmpty()) {
                Location loc = p.getLocation();
                p.getWorld().dropItem(loc, leftOver.get(0));
            }
        }
    }

    public static void givePlayer(Player p, List<ItemStack> itemStacks, boolean instant) {
        itemStacks = new ArrayList<>(itemStacks);
        itemStacks.removeIf(Objects::isNull);
        if (itemStacks.isEmpty())
            return;
        if (instant) {
            p.playSound(p.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            for (ItemStack itemStack : itemStacks) {
                if (itemStack != null)
                    givePlayerInstantly(p, itemStack, itemStack.getAmount());
            }
            return;
        }
        MultipleItemGive multipleItemGive = new MultipleItemGive(p, itemStacks);
        multipleItemGive.setTaskImplementation(NotBounties.getServerImplementation().entity(p).runAtFixedRate(multipleItemGive, 1, 5));
    }


    /**
     * Separate items based on how many slots they take up and how many slots per page.
     * Each new row is a new page
     * @param items Items to serialize
     * @param maxSlotsPerPage Maximum slots per object in the returned array
     * @return A 2D array of ItemStacks with maxSlotsPerPage column length
     */
    public static ItemStack[][] separateItems(List<ItemStack> items, int maxSlotsPerPage) {
        ItemStack[] contents = new ItemStack[maxSlotsPerPage];
        if (items.isEmpty())
            return new ItemStack[1][maxSlotsPerPage];
        List<ItemStack[]> pages = new ArrayList<>();
        int index = 0;
        while (!items.isEmpty()) {
            if (index == contents.length) {
                pages.add(contents);
                contents = new ItemStack[maxSlotsPerPage];
                index = 0;
            }
            ItemStack currentItem = items.get(0).clone();
            if (currentItem.getAmount() > currentItem.getMaxStackSize()) {
                currentItem.setAmount(currentItem.getMaxStackSize());
                items.get(0).setAmount(items.get(0).getAmount() - items.get(0).getMaxStackSize());
            } else {
                items.remove(0);
            }
            contents[index] = currentItem;
            index++;
        }
        pages.add(contents);
        ItemStack[][] inventoryPages = new ItemStack[pages.size()][maxSlotsPerPage];
        for (int i = 0; i < pages.size(); i++) {
            inventoryPages[i] = pages.get(i);
        }
        return inventoryPages;
    }

    /**
     * Get the balance of a player. Checks if the currency is a placeholder and parses it, otherwise, gets the amount of items matching the currency material
     *
     * @param player Player to get balance from
     * @return Balance of player
     */
    public static double getBalance(OfflinePlayer player) {
        if (vaultEnabled && !overrideVault)
            return vaultClass.getBalance(player);
        if (currency.isEmpty()){
            plugin.getLogger().warning("Cannot get balance of player because there is nothing setup for currency!");
        }
        return IntStream.range(0, currency.size()).mapToDouble(i -> getBalance(player, currency.get(i), customModelDatas.get(i))).sum();
    }

    public static double[] getSortedBalance(Player player, List<ItemStack> additionalItems) {
        if (currency.isEmpty()){
            plugin.getLogger().warning("Cannot get balance of player because there is nothing setup for currency!");
        }
        double[] sortedBalance = new double[currency.size()];
        for (int i = 0; i < currency.size(); i++){
            sortedBalance[i] = getBalance(player, currency.get(i), customModelDatas.get(i));
            if (!currency.get(i).contains("%")) {
                ListIterator<ItemStack> itemStackListIterator = additionalItems.listIterator();
                while (itemStackListIterator.hasNext()) {
                    ItemStack item = itemStackListIterator.next();
                    if (Material.valueOf(currency.get(i)).equals(item.getType())){
                        sortedBalance[i] += item.getAmount() * currencyValues.get(currency.get(i));
                        itemStackListIterator.remove();
                    }
                }
            }
        }
        return sortedBalance;
    }

    private static double getBalance(OfflinePlayer player, String currencyName, String customModelData){
        float currencyValue;
        if (currencyValues.containsKey(currencyName)) {
            currencyValue = currencyValues.get(currencyName);
        } else {
            plugin.getLogger().log(Level.WARNING, "Currency value not set for {0}! This is a bug.", currencyName);
            currencyValue = 1f;
        }
        if (currencyName.contains("%")) {
            if (ConfigOptions.getIntegrations().isPapiEnabled()) {
                // using placeholderAPI
                String placeholder = new PlaceholderAPIClass().parse(player, currencyName);
                try {
                    return tryParse(placeholder) * currencyValue;
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Error getting a number from the currency placeholder " + currencyName + "!");
                    return 0;
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Currency {0} for bounties is a placeholder but PlaceholderAPI is not enabled!", currencyName);
                return 0;
            }
        } else {
            // item
            if (player.isOnline())
                return checkAmount(Objects.requireNonNull(player.getPlayer()), Material.valueOf(currencyName), customModelData) * currencyValue;
            return 0;
        }
    }

    /**
     * Tries to parse a double value from a string.
     * The method looks for any suffix values like 'K', 'M', 'B', etc.
     * The parsing is then done with decimal format.
     * @param number Number string to be parsed
     * @return The parsed number
     * @throws NumberFormatException if the string is not a number
     */
    public static double tryParse(String number) throws NumberFormatException {
        // remove any currency prefixes or suffixes
        if (!currencyPrefix.isEmpty() && number.startsWith(currencyPrefix)) {
            number = number.substring(currencyPrefix.length());
        }
        if (!currencySuffix.isEmpty() && number.startsWith(currencySuffix)) {
            number = number.substring(0, number.length()-currencySuffix.length());
        }
        // remove any colors
        number = ChatColor.stripColor(number);
        // parse any divisions
        long modifier = 1;
        // case-sensitive
        for (Map.Entry<Long, String> division : nfDivisions.entrySet()) {
            if (number.endsWith(division.getValue())) {
                modifier = division.getKey();
                number = number.substring(0,number.length() - division.getValue().length());
                break;
            }
        }
        // not case-sensitive
        if (modifier == 1)
            for (Map.Entry<Long, String> division : nfDivisions.entrySet()) {
                if (number.toLowerCase().endsWith(division.getValue().toLowerCase())) {
                    number = number.substring(0,number.length() - division.getValue().length());
                    modifier = division.getKey();
                    break;
                }
            }
        // try parsing through decimal format and regular double parsing
        double amount;
        try {
            amount = decimalFormat.parse(number).doubleValue();
        } catch (ParseException e) {
            amount = Double.parseDouble(number);
        }
        return amount * modifier;
    }

    /**
     * Check the amount of items matching a material
     *
     * @param player   Player whose inventory will be searched
     * @param material Material to check for
     * @return amount of items in the players inventory that are a certain material
     */
    public static int checkAmount(Player player, Material material, String customModelData) {
        int amount = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack content : contents) {
            if (content != null) {
                if (isCorrectMaterial(content, material, customModelData)) {
                    amount += content.getAmount();
                }
            }
        }
        return amount;
    }

    public static boolean isCorrectMaterial(ItemStack item, Material material, String customModelData) {
        return item.getType().equals(material) &&
                (customModelData.equals("-1") || (item.hasItemMeta() && (Objects.requireNonNull(item.getItemMeta()).hasCustomModelData() && customModelData.equals(item.getItemMeta().getCustomModelData() + "")) || (item.getItemMeta().hasItemModel() && CustomItem.isItemModelEqual(item.getItemMeta().getItemModel(), customModelData))));
    }

    public static Map<Long, String> sortByValue(Map<Long, String> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<Long, String>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (o2.getKey()).compareTo(o1.getKey()));

        // put data from sorted list to hashmap
        LinkedHashMap<Long, String> temp = new LinkedHashMap<>();
        for (Map.Entry<Long, String> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static List<String> getCurrency() {
        return currency;
    }

    public static Map<String, Float> getCurrencyValues() {
        return currencyValues;
    }

    public static String getCurrencyPrefix() {
        return currencyPrefix;
    }

    public static String getCurrencySuffix() {
        return currencySuffix;
    }

    public static boolean isBountyItemsDefaultGUI() {
        return bountyItemsDefaultGUI;
    }

    public static Locale getLocale() {
        return locale;
    }

    public static boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public static boolean isOverrideVault() {
        return overrideVault;
    }

    public static ManualEconomy getManualEconomy() {
        return manualEconomy;
    }

    public static boolean isBountyItemsBuyItem() {
        return bountyItemsBuyItem;
    }

    public static BountyItemMode getBountyItemMode() {
        return bountyItemMode;
    }

    public static ItemValueMode getBountyItemsUseItemValues() {
        return bountyItemsUseItemValues;
    }

    public static boolean isBountyItemsOverrideImmunity() {
        return bountyItemsOverrideImmunity;
    }

    public static boolean isTabCompleteItems() {
        return tabCompleteItems;
    }
}

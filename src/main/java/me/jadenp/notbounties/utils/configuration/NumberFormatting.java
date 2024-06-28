package me.jadenp.notbounties.utils.configuration;

import com.google.common.primitives.Floats;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.ItemValue;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.utils.externalAPIs.PlaceholderAPIClass;
import me.jadenp.notbounties.utils.externalAPIs.VaultClass;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

import static me.jadenp.notbounties.NotBounties.debug;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;

public class NumberFormatting {
    public static List<String> currency;
    public static List<Integer> customModelDatas;
    public static LinkedHashMap<String, Float> currencyValues;
    public static List<Float> currencyWeights;
    public static List<String> removeCommands;
    public static List<String> addCommands;
    public static String currencyPrefix = "";
    public static String currencySuffix = "";
    public static boolean useDivisions;
    public static LinkedHashMap<Long, String> nfDivisions = new LinkedHashMap<>();
    public static DecimalFormat decimalFormat;
    public static Locale locale;
    public enum CurrencyAddType {
        DESCENDING, FIRST, RATIO, BIMODAL
    }
    public static CurrencyAddType addSingleCurrency = CurrencyAddType.DESCENDING;
    public static boolean usingPapi = false;
    private static VaultClass vaultClass = null;
    public static boolean vaultEnabled;
    public static boolean overrideVault;
    public static boolean bountyItemsOverrideImmunity = false;
    public enum ManualEconomy {
        AUTOMATIC, PARTIAL, MANUAL
    }
    public static ManualEconomy manualEconomy;
    public enum BountyItemMode {
        ALLOW, DENY, EXCLUSIVE
    }
    public static BountyItemMode bountyItemMode;
    private static Map<Material, ItemValue> itemValues = new HashMap<>();
    private static Map<Enchantment, Double> enchantmentValues = new HashMap<>();
    private static double defaultItemValue = 1;
    private static double itemValueMultiplier = 1;
    public static boolean bountyItemsBuyItem = true;
    public static boolean tabCompleteItems = false;
    public static boolean bountyItemsDefaultGUI = false;
    public static boolean bountyItemsUseItemValues = true;

    public static void loadConfiguration(ConfigurationSection currencyOptions, ConfigurationSection numberFormatting) {
        vaultEnabled = Bukkit.getServer().getPluginManager().isPluginEnabled("Vault");
        overrideVault = currencyOptions.getBoolean("override-vault");
        try {
            manualEconomy = ManualEconomy.valueOf(Objects.requireNonNull(currencyOptions.getString("manual-economy")).toUpperCase());
        } catch (IllegalArgumentException e) {
            manualEconomy = ManualEconomy.AUTOMATIC;
            Bukkit.getLogger().warning("[NotBounties] Invalid manual-economy type!");
        }

        if (vaultEnabled && !overrideVault) {
            vaultClass = new VaultClass();
            //Bukkit.getLogger().info("Using Vault as currency!");
        }
        currency = new ArrayList<>();
        currencyWeights = new ArrayList<>();
        currencyValues = new LinkedHashMap<>();
        customModelDatas = new ArrayList<>();
        if (currencyOptions.isString("object")){
            currency = Collections.singletonList(currencyOptions.getString("object"));
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
            try {
                customModelDatas.add(Integer.parseInt(customModelData));
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Could not get custom model data <" + customModelData + "> for currency: " + currencyName);
                customModelDatas.add(-1);
            }
            // seperate weight and value attached if there is any
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
                if (!papiEnabled && !(vaultEnabled && !overrideVault)) {
                    Bukkit.getLogger().warning("Detected a placeholder as currency, but PlaceholderAPI is not enabled!");
                    Bukkit.getLogger().warning("Ignoring placeholder from currency.");
                    currencyIterator.remove();
                    continue;
                }
            } else {
                try {
                    Material.valueOf(currencyName);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("Could not get an item from: " + currencyName + "!");
                    Bukkit.getLogger().warning("Ignoring item from currency.");
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
                    Bukkit.getLogger().warning("Could not get a number from value " + value + " after currency " + currencyName + "!");
                    currencyValues.put(currencyName, 1f);
                }
                if (weight.isEmpty())
                    currencyWeights.add(0f);
                else
                    try {
                        currencyWeights.add(Float.parseFloat(weight));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("Could not get a number from weight " + weight + " after currency " + currencyName + "!");
                        currencyWeights.add(0f);
                    }
            }
        }
        // in case all the currency is invalid
        if (currency.isEmpty() && currencyOptions.isSet("object")){
            Bukkit.getLogger().info("No currency to use. Defaulting to DIAMOND as currency");
            currency.add("DIAMOND");
        }

        for (String currencyName : currency)
            if (currencyName.contains("%")) {
                usingPapi = true;
                break;
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
            Bukkit.getLogger().warning("[NotBounties] Invalid add-single-currency type!");
        }

        // warning for not enough remove/add commands
        if (addSingleCurrency == CurrencyAddType.BIMODAL) {
            if (currency.size() < 2) {
                Bukkit.getLogger().warning("Using bimodal currency but there aren't 2 currencies to use!");
            } else {
                if (currency.get(0).contains("%") && addCommands.isEmpty())
                    Bukkit.getLogger().warning("Detected a placeholder for the first currency, but there are no add commands!");
                if (currency.get(1).contains("%") && removeCommands.isEmpty())
                    Bukkit.getLogger().warning("Detected a placeholder for the second currency, but there are no remove commands!");
            }

        } else {
            int placeholderCurrencies = (int) currency.stream().filter(currencyName -> currencyName.contains("%")).count();
            if (addCommands.size() < placeholderCurrencies)
                Bukkit.getLogger().warning("Detected " + placeholderCurrencies + " placeholder(s) as currency, but there are only " + addCommands.size() + " add commands!");
            if (removeCommands.size() < placeholderCurrencies)
                Bukkit.getLogger().warning("Detected " + placeholderCurrencies + " placeholder(s) as currency, but there are only " + removeCommands.size() + " remove commands!");
        }


        useDivisions = numberFormatting.getBoolean("use-divisions");

        String localeString = numberFormatting.getString("format-locale");
        String pattern = numberFormatting.getString("pattern");
        if (localeString == null) {
            // this shouldn't ever get used because of the auto-generation of missing options
            Bukkit.getLogger().warning("[NotBounties] No number formatting locale set! Defaulting to en-US.");
            localeString = "en-US";
        }
        if (pattern == null) {
            // this shouldn't get used either
            Bukkit.getLogger().warning("[NotBounties] No number formatting pattern set! Defaulting to \"#,###.##\"");
            pattern = "#,###.##";
        }

        String[] localeSplit = localeString.split("-");
        locale = new Locale(localeSplit[0], localeSplit[1]);

        decimalFormat = new DecimalFormat(pattern, new DecimalFormatSymbols(locale));

        NumberFormatting.nfDivisions.clear();
        Map<Long, String> preDivisions = new HashMap<>();
        for (String s : Objects.requireNonNull(numberFormatting.getConfigurationSection("divisions")).getKeys(false)) {
            if (s.equals("decimals"))
                continue;
            try {
                preDivisions.put(Long.parseLong(s), numberFormatting.getString("divisions." + s));
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Division is not a number: " + s);
            }
        }
        NumberFormatting.nfDivisions = NumberFormatting.sortByValue(preDivisions);

        // bounty items
        try {
            bountyItemMode = BountyItemMode.valueOf(Objects.requireNonNull(currencyOptions.getString("bounty-items.mode")).toUpperCase());
        } catch (IllegalArgumentException e) {
            bountyItemMode = BountyItemMode.DENY;
            Bukkit.getLogger().warning("[NotBounties] Invalid bounty items mode!");
        }
        if (currencyOptions.isSet("bounty-items.override-immunity"))
            bountyItemsOverrideImmunity = currencyOptions.getBoolean("bounty-items.override-immunity");

        if (currencyOptions.isSet("bounty-items.buy-items"))
            bountyItemsBuyItem = currencyOptions.getBoolean("bounty-items.buy-items");

        if (currencyOptions.isSet("bounty-items.tab-complete"))
            tabCompleteItems = currencyOptions.getBoolean("bounty-items.tab-complete");

        if (currencyOptions.isSet("bounty-items.default-gui"))
            bountyItemsDefaultGUI = currencyOptions.getBoolean("bounty-items.default-gui");

        if (currencyOptions.isSet("bounty-items.use-item-values"))
            bountyItemsUseItemValues = currencyOptions.getBoolean("bounty-items.use-item-values");

        File itemValuesFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "item-values.yml");
        if (!itemValuesFile.exists()) {
            NotBounties.getInstance().saveResource("item-values.yml", false);
            Bukkit.getLogger().info("[NotBounties] Created new item-values.yml file.");
        }
        itemValues.clear();
        YamlConfiguration itemValuesConfig = YamlConfiguration.loadConfiguration(itemValuesFile);
        if (itemValuesConfig.isSet("default"))
            defaultItemValue = itemValuesConfig.getDouble("default");
        if (itemValuesConfig.isSet("multiplier"))
            itemValueMultiplier = itemValuesConfig.getDouble("multiplier");
        if (itemValuesConfig.isConfigurationSection("item-values"))
            for (String key : itemValuesConfig.getConfigurationSection("item-values").getKeys(false)) {
                String materialName = key;
                int customModelData = -1;
                // check if materialName has two arrows <>
                if (materialName.contains("<") && materialName.substring(materialName.indexOf("<")).contains(">")) {
                    // has custom model data
                    try {
                        // parse custom model data for number between arrows
                        customModelData = Integer.parseInt(materialName.substring(materialName.indexOf("<") + 1, materialName.indexOf(">")));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotBounties] Couldn't get custom model data for item value: " + materialName);
                    }
                    materialName = materialName.substring(0, materialName.indexOf("<"));
                }
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[NotBounties] Invalid material in item-values.yml file: \"" + materialName + "\"");
                    continue;
                }
                double value = itemValuesConfig.getDouble("item-values." + key);
                if (itemValues.containsKey(material))
                    itemValues.get(material).addValue(customModelData, value);
                else
                    itemValues.put(material, new ItemValue().addValue(customModelData, value));
            }
        if (itemValuesConfig.isConfigurationSection("enchantment-values"))
            for (String key : itemValuesConfig.getConfigurationSection("enchantment-values").getKeys(false)) {
                try {
                    Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase()));
                    if (enchantment == null) {
                        Bukkit.getLogger().warning("[NotBounties] Unknown enchantment for enchantment-values: " + key);
                        continue;
                    }
                    enchantmentValues.put(enchantment, itemValuesConfig.getDouble("enchantment-values." + key));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[NotBounties] Unknown enchantment for enchantment-values: " + key);
                    if (debug)
                        Bukkit.getLogger().warning(e.toString());
                }
            }
    }

    public static double getItemValue(ItemStack item) {
        if (item == null)
            return 0;
        if (!bountyItemsUseItemValues)
            return defaultItemValue;
        Material material = item.getType();
        int customModelData = -1;
        if (item.getItemMeta() != null && item.getItemMeta().hasCustomModelData())
            customModelData = item.getItemMeta().getCustomModelData();
        // check if material is a currency
        for (String s : currency) {
            if (material.toString().equalsIgnoreCase(s))
                return currencyValues.get(s) + getEnchantmentValue(item);
        }
        // check if registered as an item value
        if (itemValues.containsKey(material)) {
            double value = itemValues.get(material).getValue(customModelData);
            if (value != -1)
                return value * itemValueMultiplier + getEnchantmentValue(item);
        }
        return defaultItemValue + getEnchantmentValue(item);
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


    public static double getTotalValue(List<ItemStack> items) {
        return items.stream().filter(Objects::nonNull).mapToDouble(item -> NumberFormatting.getItemValue(item) * item.getAmount()).sum();
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
        return RUZ(decimalFormat.format(number));
    }

    /**
     * Get the full number value without any formatting
     *
     * @param number to get
     * @return String value of number
     */
    public static String getValue(double number) {
        //return decimalFormat.format(number);
        return RUZ(String.format("%f",number));
    }

    /**
     * Remove Unnecessary Zeros (RUZ)
     *
     * @param value value to check zeros for
     * @return a value with no unnecessary Zeros
     */
    public static String RUZ(String value) {
        if (value.isEmpty())
            return "";
        while (value.contains(Character.toString(decimalFormat.getDecimalFormatSymbols().getDecimalSeparator())) && (value.charAt(value.length() - 1) == '0' || value.charAt(value.length() - 1) == decimalFormat.getDecimalFormatSymbols().getDecimalSeparator()))
            value = value.substring(0, value.length() - 1);
        return value;
    }

    public static String setDivision(Double number) {
        for (Map.Entry<Long, String> entry : nfDivisions.entrySet()) {
            if (number / entry.getKey() >= 1) {
                return RUZ(decimalFormat.format((double) number / entry.getKey())) + entry.getValue();
            }
        }
        return RUZ(decimalFormat.format(number));
    }


    public static Map<Material, Long> doRemoveCommands(Player p, double amount, List<ItemStack> additionalItems) {
        if (manualEconomy == ManualEconomy.MANUAL) {
            for (String removeCommand : removeCommands) {
                if (removeCommand.isEmpty())
                    continue;
                String command = removeCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
            }
            return new HashMap<>();
        }

        if (vaultEnabled && !overrideVault) {
            if (vaultClass.withdraw(p, amount)) {
                return new HashMap<>();
            } else {
                Bukkit.getLogger().warning("Error withdrawing currency with vault!");
            }
        }
        if (currency.isEmpty()){
            Bukkit.getLogger().warning("Currency is not set up! Run /currency in-game to fix.");
            return new HashMap<>();
        }
        Map<Material, Long> removedItems = new HashMap<>();
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
                Bukkit.getLogger().warning("[NotBounties] There are not enough remove commands for your currency! Currency will not be removed properly!");
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
                    Bukkit.getLogger().warning("[NotBounties] Trying to remove currency without checking balance! Amount exploited: " + amount);
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
        } catch (NumberFormatException ignored){}
        return NumberFormatting.findFirstNumber(amount) * multiplyValue;
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
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Doing add commands for " + p.getName() + ". Amount: " + amount);
        if (manualEconomy == ManualEconomy.MANUAL) {
            for (String addCommand : addCommands) {
                if (addCommand.isEmpty())
                    continue;
                String command = addCommand.replace("{player}", (p.getName())).replace("{amount}", (getValue(amount / Floats.toArray(currencyValues.values())[0])));
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Executing command: '" + command + "'");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));

            }
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Manual economy is enabled, no more actions will be performed.");
            return;
        }

        if (vaultEnabled && !overrideVault) {
            if (vaultClass.deposit(p, amount)) {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Deposited money with vault!");
                return;
            } else {
                Bukkit.getLogger().warning("Error depositing currency with vault!");
            }
        }
        if (currency.isEmpty()){
            Bukkit.getLogger().warning("Currency is not set up! Run /currency in-game to fix.");
            return;
        }
        List<String> modifiedAddCommands = new ArrayList<>(addCommands);
        // add empty spaces in list for item currencies
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Currency: ");
        for (int i = 0; i < currency.size(); i++) {
            if (!currency.get(i).contains("%")) {
                modifiedAddCommands.add(i, "");
                if (debug)
                    Bukkit.getLogger().info("(item) " + currency.get(i));
            } else {
                if (debug)
                    Bukkit.getLogger().info(currency.get(i));
            }
        }
        if (addSingleCurrency == CurrencyAddType.RATIO && currency.size() > 1) {
            float[] currencyWeightsCopy = Floats.toArray(currencyWeights);
            float[] currencyValuesCopy = Floats.toArray(currencyValues.values());
            double[] balancedAdd = balanceAddCurrency(amount, currencyWeightsCopy, currencyValuesCopy);
            if (modifiedAddCommands.size() < balancedAdd.length) {
                Bukkit.getLogger().warning("[NotBounties] There are not enough add commands for your currency! Currency will not be added properly!");
            }
            for (int i = 0; i < Math.min(balancedAdd.length, modifiedAddCommands.size()); i++) {
                if (currency.get(i).contains("%")) {
                    String command = modifiedAddCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(balancedAdd[i])));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
                } else {
                    ItemStack item = new ItemStack(Material.valueOf(currency.get(i)));
                    if (customModelDatas.get(i) != -1) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setCustomModelData(customModelDatas.get(i));
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
                if (customModelDatas.get(0) != -1) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(customModelDatas.get(0));
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
                if (customModelDatas.get(0) != -1) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(customModelDatas.get(0));
                        item.setItemMeta(meta);
                    }
                }
                givePlayer(p, item, (long) (amount / Floats.toArray(currencyValues.values())[0]));
            }
        } else {
            // descending
            float[] currencyWeightsCopy = Floats.toArray(currencyWeights);
            float[] currencyValuesCopy = Floats.toArray(currencyValues.values());
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Input Calculations: " + amount + " | " + Arrays.toString(currencyWeightsCopy) + " | " + Arrays.toString(currencyValuesCopy));
            double[] descendingAdd = descendingAddCurrency(amount, currencyWeightsCopy, currencyValuesCopy);
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Output: " + Arrays.toString(descendingAdd));

            if (modifiedAddCommands.size() < descendingAdd.length) {
                Bukkit.getLogger().warning("[NotBounties] There are not enough add commands for your currency! Currency will not be added properly!");
            }
            for (int i = 0; i < Math.min(descendingAdd.length, modifiedAddCommands.size()); i++) {
                if (currency.get(i).contains("%")) {
                    String command = modifiedAddCommands.get(i).replace("{player}", (p.getName())).replace("{amount}", (getValue(descendingAdd[i])));
                    if (debug)
                        Bukkit.getLogger().info("[NotBountiesDebug] Executing command for " + currency.get(i) + ": '" + command + "'");
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageOptions.parse(command, p));
                } else {
                    ItemStack item = new ItemStack(Material.valueOf(currency.get(i)));
                    if (customModelDatas.get(i) != -1) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setCustomModelData(customModelDatas.get(i));
                            item.setItemMeta(meta);
                        }
                    }
                    if (debug)
                        Bukkit.getLogger().info("[NotBountiesDebug] Giving item: " + currency.get(i) + " to " + p.getName() + ". Amount: " + (long) descendingAdd[i]);
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

    public static void removeItem(Player player, Material material, long amount, int customModelData) {
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
            if (itemString.length() != 0)
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
            TextComponent name = new TextComponent(nameString);
            name.setHoverEvent(getHoverEvent(itemStack));
            components[i] = name;
        }
        return components;
    }

    private static HoverEvent getHoverEvent(ItemStack itemStack) {
        String nbt = null;
        if (itemStack.getItemMeta() != null)
            nbt = NotBounties.serverVersion >= 18 ? itemStack.getItemMeta().getAsString() : itemStack.getItemMeta().toString();
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

    // use this instead?
    public static void givePlayer(Player p, ItemStack itemStack, long amount) {
        new BukkitRunnable() {
            long toGive = amount;
            @Override
            public synchronized void cancel() {
                super.cancel();
                if (toGive > 0) {
                    // refund the rest of the items
                    itemStack.setAmount((int) toGive);
                    BountyManager.refundPlayer(p.getUniqueId(), 0, new ArrayList<>(List.of(itemStack)));
                }
            }
            @Override
            public void run() {
                if (toGive <= 0) {
                    this.cancel();
                    return;
                }
                if (p.isOnline()) {
                    if (toGive > itemStack.getMaxStackSize()) {
                        itemStack.setAmount(itemStack.getMaxStackSize());
                        toGive -= itemStack.getMaxStackSize();
                    } else {
                        itemStack.setAmount((int) toGive);
                        toGive = 0;
                    }
                    p.playSound(p.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                    givePlayerInstantly(p, itemStack, itemStack.getAmount());
                } else {
                    this.cancel();
                }

            }
        }.runTaskTimer(NotBounties.getInstance(), 0, 5);
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
        List<ItemStack> finalItemStacks = itemStacks;
        new BukkitRunnable() {
            int index = 0;
            @Override
            public synchronized void cancel() {
                super.cancel();
                if (index < finalItemStacks.size()) {
                    // refund the rest of the items
                    BountyManager.refundPlayer(p.getUniqueId(), 0, finalItemStacks.subList(index, finalItemStacks.size()));
                }
            }
            @Override
            public void run() {
                if (p.isOnline()) {
                    givePlayer(p, finalItemStacks.get(index), finalItemStacks.get(index).getAmount());
                    index++;
                    if (index >= finalItemStacks.size())
                        this.cancel();
                } else {
                    // cancel -> will add to refund
                    this.cancel();
                }
            }
        }.runTaskTimer(NotBounties.getInstance(), 0, 5);
    }


    /**
     * Serialized items after separating them based on how many slots they take up
     * @param items Items to serialize
     * @param maxSlotsPerPage Maximum slots per object in the returned array
     * @return An array of strings with serialized items
     */
    public static String[] serializeItems(List<ItemStack> items, int maxSlotsPerPage) {
        if (items.isEmpty())
            return new String[0];
        List<String> serialized = new ArrayList<>();
        ItemStack[] contents = new ItemStack[maxSlotsPerPage];
        int index = 0;
        while (!items.isEmpty()) {
            if (index == contents.length) {
                serialized.add(SerializeInventory.itemStackArrayToBase64(contents));
                contents = new ItemStack[maxSlotsPerPage];
                index = 0;
            }
            contents[index] = items.remove(0);
            index++;
        }
        serialized.add(SerializeInventory.itemStackArrayToBase64(contents));
        return serialized.toArray(new String[0]);
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
            Bukkit.getLogger().warning("[NotBounties] Cannot get balance of player because there is nothing setup for currency!");
        }
        return IntStream.range(0, currency.size()).mapToDouble(i -> getBalance(player, currency.get(i), customModelDatas.get(i))).sum();
    }

    public static double[] getSortedBalance(Player player, List<ItemStack> additionalItems) {
        if (currency.isEmpty()){
            Bukkit.getLogger().warning("[NotBounties] Cannot get balance of player because there is nothing setup for currency!");
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

    private static double getBalance(OfflinePlayer player, String currencyName, int customModelData){
        if (currencyName.contains("%")) {
            if (papiEnabled) {
                // using placeholderAPI
                String placeholder = new PlaceholderAPIClass().parse(player, currencyName);
                try {
                    return tryParse(placeholder) * currencyValues.get(currencyName);
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Error getting a number from the currency placeholder " + currencyName + "!");
                    return 0;
                }
            } else {
                Bukkit.getLogger().warning("Currency " + currencyName + " for bounties is a placeholder but PlaceholderAPI is not enabled!");
                return 0;
            }
        } else {
            // item
            if (player.isOnline())
                return checkAmount(Objects.requireNonNull(player.getPlayer()), Material.valueOf(currencyName), customModelData) * currencyValues.get(currencyName);
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
            //balance = Double.parseDouble(PlaceholderAPI.setPlaceholders(player, NumberFormatting.currency));
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
    public static int checkAmount(Player player, Material material, int customModelData) {
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

    public static boolean isCorrectMaterial(ItemStack item, Material material, int customModelData) {
        return item.getType().equals(material) &&
                (customModelData == -1 || (item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).hasCustomModelData() && item.getItemMeta().getCustomModelData() == customModelData));
    }

    public static LinkedHashMap<Long, String> sortByValue(Map<Long, String> hm) {
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
}

package me.jadenp.notbounties.utils;

import com.google.common.primitives.Floats;
import me.jadenp.notbounties.NotBounties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.ConfigOptions.*;

public class NumberFormatting {
    public static List<String> currency;
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
    public static boolean addSingleCurrency = true;
    public static boolean usingPapi = false;
    private static VaultClass vaultClass = null;
    private static boolean vaultEnabled;
    public static boolean overrideVault;


    public static void setCurrencyOptions(ConfigurationSection currencyOptions, ConfigurationSection numberFormatting) {
        vaultEnabled = Bukkit.getServer().getPluginManager().isPluginEnabled("Vault");
        overrideVault = currencyOptions.getBoolean("override-vault");
        if (vaultEnabled && !overrideVault) {
            vaultClass = new VaultClass();
            Bukkit.getLogger().info("Using Vault as currency!");
        }
        currency = new ArrayList<>();
        currencyWeights = new ArrayList<>();
        currencyValues = new LinkedHashMap<>();
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
                if (!papiEnabled) {
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
            currencyPrefix = color(Objects.requireNonNull(currencyOptions.getString("prefix")));
        if (currencyOptions.isSet("suffix"))
            currencySuffix = color(Objects.requireNonNull(currencyOptions.getString("suffix")));
        if (currencyOptions.isList("add-commands"))
            addCommands = currencyOptions.getStringList("add-commands");
        else addCommands = Collections.singletonList(currencyOptions.getString("add-commands"));

        if (currencyOptions.isList("remove-commands"))
            removeCommands = currencyOptions.getStringList("remove-commands");
        else removeCommands = Collections.singletonList(currencyOptions.getString("remove-commands"));

        // warning for not enough remove/add commands
        int placeholderCurrencies = (int) currency.stream().filter(currencyName -> currencyName.contains("%")).count();
        if (addCommands.size() < placeholderCurrencies)
            Bukkit.getLogger().warning("Detected " + placeholderCurrencies + " placeholder(s) as currency, but there are only " + addCommands.size() + " add commands!");
        if (removeCommands.size() < placeholderCurrencies)
            Bukkit.getLogger().warning("Detected " + placeholderCurrencies + " placeholder(s) as currency, but there are only " + removeCommands.size() + " remove commands!");

        addSingleCurrency = currencyOptions.getBoolean("currency.add-single-currency");



        useDivisions = numberFormatting.getBoolean("use-divisions");

        String localeString = numberFormatting.getString("format-locale");
        String pattern = numberFormatting.getString("pattern");

        assert localeString != null;
        String[] localeSplit = localeString.split("-");
        locale = new Locale(localeSplit[0], localeSplit[1]);

        assert pattern != null;
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
    }

    public static String formatNumber(String number) {
        if (number.length() == 0)
            return "";
        if (number.startsWith(currencyPrefix) && currencyPrefix.length() > 0)
            return currencyPrefix + formatNumber(number.substring(currencyPrefix.length()));
        if (number.endsWith(currencySuffix) && currencySuffix.length() > 0)
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
        if (str.length() == 0)
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
    public static String formatNumber(Double number) {
        if (useDivisions) {
            // set divisions
            return setDivision(number);
        }
        return RUZ(decimalFormat.format(number));
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
            List<String> modifiedRemoveCommands = new ArrayList<>(removeCommands);
            // add empty spaces in list for item currencies
            for (int i = 0; i < currency.size(); i++) {
                if (!currency.get(i).contains("%"))
                    modifiedRemoveCommands.add(i, "");
            }
            float[] currencyWeightsCopy = Floats.toArray(currencyWeights);
            float[] currencyValuesCopy = Floats.toArray(currencyValues.values());

            double[] balancedRemove = balanceRemoveCurrency(amount, currencyWeightsCopy, getSortedBalance(p, additionalItems), currencyValuesCopy);
            if (modifiedRemoveCommands.size() < balancedRemove.length) {
                Bukkit.getLogger().warning("[NotBounties] There are not enough remove commands for your currency! Currency will not be removed properly!");
            }
            for (int i = 0; i < Math.min(balancedRemove.length, modifiedRemoveCommands.size()); i++) {
                if (currency.get(i).contains("%")) {
                    String command = modifiedRemoveCommands.get(i).replaceAll("\\{player}", Matcher.quoteReplacement(p.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(String.format("%.8f", balancedRemove[i])));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(command, p));
                } else {
                    removeItem(p, Material.valueOf(currency.get(i)), (long) (balancedRemove[i]));
                    removedItems.put(Material.valueOf(currency.get(i)), (long) (balancedRemove[i]));
                }
            }
            // do the rest of the remove commands
            for (int i = balancedRemove.length; i < modifiedRemoveCommands.size(); i++) {
                String command = modifiedRemoveCommands.get(i).replaceAll("\\{player}", Matcher.quoteReplacement(p.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(String.format("%.8f", amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(command, p));
            }
        } else {
            // just do remove commands
            for (String removeCommand : removeCommands) {
                String command = removeCommand.replaceAll("\\{player}", Matcher.quoteReplacement(p.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(String.format("%.8f", amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(command, p));
            }
            if (!currency.get(0).contains("%")) {
                removeItem(p, Material.valueOf(currency.get(0)), (long) amount);
                removedItems.put(Material.valueOf(currency.get(0)), (long) amount);
            }
        }
        return removedItems;
    }

    public static double @NotNull [] balanceRemoveCurrency(double amount, float[] currencyWeights, double[] currentBalance, float[] currencyValues){
        float totalWeight = addUp(currencyWeights);
        if (totalWeight == 0){
            Arrays.fill(currencyWeights, 1);
            totalWeight = currencyWeights.length;
        }

        double[] balancedRemove = new double[currency.size()];
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

        return balancedRemove;
    }

    public static double parseCurrency(String amount){
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
                if (entry.getValue().equals(divisionString.toString())) {
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


    public static void doAddCommands(Player p, double amount) {
        if (vaultEnabled && !overrideVault) {
            if (vaultClass.deposit(p, amount)) {
                return;
            } else {
                Bukkit.getLogger().warning("Error depositing currency with vault!");
            }
        }
        if (currency.isEmpty()){
            Bukkit.getLogger().warning("Currency is not set up! Run /currency in-game to fix.");
            return;
        }

        if (!addSingleCurrency && currency.size() > 1) {
            List<String> modifiedAddCommands = new ArrayList<>(addCommands);
            // add empty spaces in list for item currencies
            for (int i = 0; i < currency.size(); i++) {
                if (!currency.get(i).contains("%"))
                    modifiedAddCommands.add(i, "");
            }
            float[] currencyWeightsCopy = Floats.toArray(currencyWeights);
            float[] currencyValuesCopy = Floats.toArray(currencyValues.values());
            double[] balancedAdd = balanceAddCurrency(amount, currencyWeightsCopy, currencyValuesCopy);
            if (modifiedAddCommands.size() < balancedAdd.length) {
                Bukkit.getLogger().warning("[NotBounties] There are not enough add commands for your currency! Currency will not be added properly!");
            }
            for (int i = 0; i < Math.min(balancedAdd.length, modifiedAddCommands.size()); i++) {
                if (currency.get(i).contains("%")) {
                    String command = modifiedAddCommands.get(i).replaceAll("\\{player}", Matcher.quoteReplacement(p.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(String.format("%.8f", balancedAdd[i])));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(command, p));
                } else {
                    givePlayer(p, new ItemStack(Material.valueOf(currency.get(i))), (long) balancedAdd[i]);
                }
            }
            // do the rest of the add commands
            for (int i = balancedAdd.length; i < modifiedAddCommands.size(); i++) {
                String command = modifiedAddCommands.get(i).replaceAll("\\{player}", Matcher.quoteReplacement(p.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(String.format("%.8f", amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(command, p));
            }
        } else {
            // just do add commands
            for (String addCommand : addCommands) {
                String command = addCommand.replaceAll("\\{player}", Matcher.quoteReplacement(p.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(String.format("%.8f", amount)));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(command, p));
            }
            if (!currency.get(0).contains("%"))
                givePlayer(p, new ItemStack(Material.valueOf(currency.get(0))), (long) amount);
        }
    }

    public static void removeItem(Player player, Material material, long amount) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                if (contents[i].getType().equals(material)) {
                    if (contents[i].getAmount() > amount) {
                        contents[i] = new ItemStack(contents[i].getType(), (int) (contents[i].getAmount() - amount));
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

    public static boolean checkBalance(Player player, double amount) {
        if (vaultEnabled && !overrideVault)
            return vaultClass.checkBalance(player, amount);
        double bal = getBalance(player);
        if (!usingPapi)
            bal = (long) bal;
        return bal >= amount;
    }

    // use this instead?
    public static void givePlayer(Player p, ItemStack itemStack, long amount) {
        new BukkitRunnable() {
            long toGive = amount;
            @Override
            public void run() {
                if (toGive <= 0) {
                    this.cancel();
                    return;
                }
                if (toGive > itemStack.getMaxStackSize()) {
                    itemStack.setAmount(itemStack.getMaxStackSize());
                    toGive -= itemStack.getMaxStackSize();
                } else {
                    itemStack.setAmount((int) toGive);
                    toGive = 0;
                }
                HashMap<Integer, ItemStack> leftOver = new HashMap<>((p.getInventory().addItem(itemStack)));
                if (!leftOver.isEmpty()) {
                    Location loc = p.getLocation();
                    p.getWorld().dropItem(loc, leftOver.get(0));
                }

            }
        }.runTaskTimer(NotBounties.getInstance(), 0, 5);
    }

    /**
     * Get the balance of a player. Checks if the currency is a placeholder and parses it, otherwise, gets the amount of items matching the currency material
     *
     * @param player Player to get balance from
     * @return Balance of player
     */
    public static double getBalance(Player player) {
        if (currency.isEmpty()){
            Bukkit.getLogger().warning("[NotBounties] Cannot get balance of player because there is nothing setup for currency!");
        }
        double amount = 0;
        for (String currencyName : currency){
            amount+= getBalance(player, currencyName);
        }
        return amount;
    }

    public static double[] getSortedBalance(Player player, List<ItemStack> additionalItems) {
        if (currency.isEmpty()){
            Bukkit.getLogger().warning("[NotBounties] Cannot get balance of player because there is nothing setup for currency!");
        }
        double[] sortedBalance = new double[currency.size()];
        for (int i = 0; i < currency.size(); i++){
            sortedBalance[i] = getBalance(player, currency.get(i));
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

    public static double getBalance(Player player, String currencyName){
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
            return checkAmount(player, Material.valueOf(currencyName)) * currencyValues.get(currencyName);
        }
    }

    public static double tryParse(String number) throws NumberFormatException {
        double amount;
        try {
            amount = decimalFormat.parse(number).doubleValue();
            //balance = Double.parseDouble(PlaceholderAPI.setPlaceholders(player, NumberFormatting.currency));
        } catch (ParseException e) {
            amount = Double.parseDouble(number);
        }
        return amount;
    }

    /**
     * Check the amount of items matching a material
     *
     * @param player   Player whose inventory will be searched
     * @param material Material to check for
     * @return amount of items in the players inventory that are a certain material
     */
    public static int checkAmount(Player player, Material material) {
        int amount = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack content : contents) {
            if (content != null) {
                if (content.getType().equals(material)) {
                    amount += content.getAmount();
                }
            }
        }
        return amount;
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

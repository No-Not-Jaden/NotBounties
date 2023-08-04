package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;

import static me.jadenp.notbounties.ConfigOptions.*;

public class NumberFormatting {
    public static List<String> currency = new ArrayList<>();
    public static List<Float> currencyValues = new ArrayList<>();
    public static List<Float> currencyWeights = new ArrayList<>();
    public static List<String> removeCommands;
    public static List<String> addCommands;
    public static String currencyPrefix = "";
    public static String currencySuffix = "";
    public static boolean useDivisions;
    public static LinkedHashMap<Long, String> nfDivisions = new LinkedHashMap<>();
    public static DecimalFormat decimalFormat;


    public static void setCurrencyOptions(ConfigurationSection currencyOptions, ConfigurationSection numberFormatting) {
        currency.clear();
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
            // seperate weight attached if there is any
            if (currencyName.contains(" ")){
                weight = currencyName.substring(currencyName.indexOf(" ") + 1);
                currencyName = currencyName.substring(0, currencyName.indexOf(" "));
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
        // in case all the currency is invalid
        if (currency.isEmpty() && currencyOptions.isSet("object")){
            Bukkit.getLogger().info("No currency to use. Defaulting to DIAMOND as currency");
            currency.add("DIAMOND");
        }

        usingPapi = Objects.requireNonNull(currencyOptions.getString("object")).contains("%");

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


        useDivisions = numberFormatting.getBoolean("use-divisions");

        String localeString = numberFormatting.getString("format-locale");
        String pattern = numberFormatting.getString("pattern");

        assert localeString != null;
        String[] localeSplit = localeString.split("-");
        Locale locale = new Locale(localeSplit[0], localeSplit[1]);

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


    public static void doRemoveCommands(Player p, double amount) {
        if (usingPapi) {
            if (removeCommands == null || removeCommands.isEmpty()) {
                Bukkit.getLogger().warning("NotBounties detected a placeholder as currency, but there are no remove commands to take away money! (Is it formatted correctly?)");
            }
        } else {
            removeItem(p, Material.valueOf(currency), (long) amount);
        }
        for (String str : removeCommands) {
            while (str.contains("{player}")) {
                str = str.replace("{player}", p.getName());
            }
            while (str.contains("{amount}")) {
                str = str.replace("{amount}", amount + "");
            }
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(str, p));
        }

    }

    public static double[] removeCurrency(double amount, float[] currencyWeights, double[] currentBalance){
        float totalWeight = addUp(currencyWeights);
        if (totalWeight == 0){
            Arrays.fill(currencyWeights, 1);
            totalWeight = currencyWeights.length;
        }

        double[] balancedRemove = new double[currency.size()];
        double excessCurrency = 0;
        for (int i = 0; i < currency.size(); i++) {
            if (currencyWeights[i] == 0)
                continue;
            // amount to be taken out of balance with currency weight accounted for
            double shareAmount = amount * (currencyWeights[i] / totalWeight);
            if (currentBalance[i] >= shareAmount){
                // player has enough currency
                balancedRemove[i] = shareAmount;
                currentBalance[i]-= shareAmount;
            } else {
                // player doesn't have enough currency
                balancedRemove[i] = currentBalance[i];
                excessCurrency+= shareAmount - currentBalance[i];  // add to excess currency
                currentBalance[i] = 0;
                currencyWeights[i] = 0;
            }
        }

        if (excessCurrency > 0){
            double[] adjusted = removeCurrency(excessCurrency, currencyWeights, currentBalance);
            for (int i = 0; i < adjusted.length; i++) {
                balancedRemove[i]+= adjusted[i];
                currentBalance[i]-= adjusted[i];
            }
        }

        // round currencies that are items and can't have decimals
        double excessRounding = 0;
        for (int i = 0; i < currency.size(); i++) {
            if (!currency.get(i).contains("%")){
                double currencyRemove = balancedRemove[i];
                balancedRemove[i] = (int) currencyRemove;
                excessRounding += balancedRemove[i] - currencyRemove;
            }
        }
        // adding the excess currency from rounding back onto anywhere it can
        for (int i = 0; i < currency.size(); i++) {
            if (currency.get(i).contains("%")) {
                if (currentBalance[i] == 0)
                    continue;
                if (currentBalance[i] >= excessRounding) {
                    balancedRemove[i] += excessRounding;
                    currentBalance[i] -= excessRounding;
                    break;
                }
                balancedRemove[i] += currentBalance[i];
                excessRounding -= currentBalance[i];
                currentBalance[i] = 0;
            } else if (excessRounding >= 1){
                if (currentBalance[i] == 0)
                    continue;
                if (currentBalance[i] >= excessRounding) {
                    balancedRemove[i] += (int) excessRounding;
                    currentBalance[i] -= (int) excessRounding;
                    break;
                }
                balancedRemove[i] += (int) currentBalance[i];
                excessRounding -= (int) currentBalance[i];
                currentBalance[i] = 0;
            }
        }

        return balancedRemove;
    }

    public static float addUp(float[] numbers){
        float total = 0;
        for (float f : numbers)
            total+= f;
        return total;
    }


    public static void doAddCommands(Player p, double amount) {
        if (usingPapi)
            if (addCommands == null || addCommands.isEmpty())
                Bukkit.getLogger().warning("We detected a placeholder as currency, but there are no add commands to give players there reward! (Is it formatted correctly?)");

        for (String str : addCommands) {
            str = str.replaceAll("\\{player}", p.getName()).replaceAll("\\{amount}", amount + "");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parse(str, p));
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

    public static double getBalance(Player player, String currencyName){
        if (currencyName.contains("%")) {
            if (papiEnabled) {
                // using placeholderAPI
                String placeholder = new PlaceholderAPIClass().parse(player, currencyName);
                try {
                    return tryParse(placeholder);
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
            return checkAmount(player, Material.valueOf(currencyName));
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

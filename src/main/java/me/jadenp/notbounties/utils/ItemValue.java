package me.jadenp.notbounties.utils;

import java.util.HashMap;
import java.util.Map;

public class ItemValue {
    // custom model data, value
    private final Map<String, Double> customModelDataValues = new HashMap<>();

    /**
     * Add a new custom model data with a value. Use -1 for no custom model data
     * @param customModelData Custom model data of an item
     * @param value Worth of the item in terms of currency
     * @return The updated ItemValue object
     */
    public ItemValue addValue(String customModelData, double value) {
        customModelDataValues.put(customModelData, value);
        return this;
    }

    /**
     * Get the value for a specified custom model data.
     * @param customModelData Custom model data to get a value for.
     * @return The value for the custom model data or -1 if one doesn't exist
     */
    public double getValue(String customModelData) {
        if (customModelDataValues.containsKey(customModelData))
            return customModelDataValues.get(customModelData);
        if (customModelDataValues.containsKey("-1"))
            return customModelDataValues.get("-1");
        return -1;
    }
}

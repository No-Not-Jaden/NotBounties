package me.jadenp.notbounties.utils.challenges;

import me.jadenp.notbounties.ui.gui.CustomItem;
import me.jadenp.notbounties.utils.configuration.ActionCommands;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.externalAPIs.PlaceholderAPIClass;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

public class Challenge {
    private final ChallengeType challengeType;
    private final List<String> inProgress;
    private final List<String> completed;
    private final List<Double> variations;
    private final List<Double> rewards;
    private final List<String> commands;
    private final String requirement;
    private final CustomItem guiItem;

    public Challenge(ChallengeType challengeType, ConfigurationSection configuration) {
        this.challengeType = challengeType;
        inProgress = configuration.isSet("in-progress") ? configuration.getStringList("in-progress") : new ArrayList<>();
        completed = configuration.isSet("completed") ? configuration.getStringList("completed") : new ArrayList<>();
        variations = configuration.isList("variations") ? configuration.getDoubleList("variations") : configuration.isDouble("variations") ? new ArrayList<>(Collections.singletonList(configuration.getDouble("variations"))) : new ArrayList<>();
        List<Double> tempRewards = configuration.isList("rewards") ? configuration.getDoubleList("rewards") : configuration.isDouble("rewards") ? new ArrayList<>(Collections.singletonList(configuration.getDouble("rewards"))) : new ArrayList<>();
        commands = configuration.isList("commands") ? configuration.getStringList("commands") : configuration.isString("commands") ? new ArrayList<>(Collections.singletonList(configuration.getString("commands"))) : new ArrayList<>();
        requirement = configuration.isSet("requirement") ? configuration.getString("requirement") : "";

        if (configuration.isConfigurationSection("item"))
            guiItem = new CustomItem(Objects.requireNonNull(configuration.getConfigurationSection("item")));
        else
            guiItem = new CustomItem();

        // format rewards to match variations
        if (tempRewards.size() < variations.size()) {
            double rewardCopy = !tempRewards.isEmpty() ? tempRewards.get(0) : 1; // the reward that will be copied to match the length of variations
            // fill up the tempRewards array
            for (int i = tempRewards.size(); i < variations.size(); i++) {
                tempRewards.add(rewardCopy * variations.get(i));
            }
        }
        // copy tempRewards to rewards
        rewards = tempRewards;
    }

    /**
     * Execute the commands for this challenge. This function will be called when the player completes the challenge
     * @param player Player to call the commands for
     * @param variationIndex The index of the variation that the player completed
     */
    public void executeCommands(Player player, int variationIndex) {
        List<String> parsedCommands = new ArrayList<>(commands);
        // replace {rewards} and {x}
        parsedCommands.forEach(str -> str.replace("{reward}", (NumberFormatting.getValue(rewards.get(variationIndex)))).replace("{x}", (NumberFormatting.getValue(variations.get(variationIndex)))));
        ActionCommands.executeCommands(player, parsedCommands);
    }


    public ItemStack getItem(OfflinePlayer player, double progress, double total, boolean canClaim) {
        ItemStack itemStack = guiItem.getFormattedItem(player, null);
        if (!itemStack.hasItemMeta())
            return itemStack;
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        List<String> itemText = progress >= total ? parseList(completed, player, progress, total) : parseList(inProgress, player, progress, total);
        if (itemText.isEmpty())
            return itemStack;
        meta.setDisplayName(itemText.remove(0));
        if (!itemText.isEmpty())
            meta.setLore(itemText);
        if (canClaim) {
            // lore can come from the item text or the guiItem lore depending on where they set it
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            assert lore != null;
            lore.add("");
            lore.add(LanguageOptions.parse(ChallengeManager.getGuiClaim(), player));
            meta.setLore(lore);
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public String getChallengeTitle(OfflinePlayer player, double progress, double total) {
        String title;
        if (progress >= total && !completed.isEmpty())
            title = completed.get(0);
        else if (!inProgress.isEmpty())
            title = inProgress.get(0);
        else
            title = "";
        return LanguageOptions.parse(title.replace("{progress}", (ChallengeType.getProgressString(progress, total))), player);
    }

    public List<Double> getVariations() {
        return variations;
    }

    /**
     * Gets the default progress for this challenge.
     * This will return 0 unless it is a custom challenge,
     * then, the placeholder is parsed and returned
     * @param player Player to get the progress from
     * @return The default progress for this challenge
     */
    public double getDefaultProgress(OfflinePlayer player) {
        if (requirement.isEmpty())
            return 0;
        String req = requirement;
        if (ConfigOptions.papiEnabled)
            req = new PlaceholderAPIClass().parse(player, req);
        String value = req.substring(0, req.indexOf(" "));
        try {
            return NumberFormatting.tryParse(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<String> parseList(List<String> list, OfflinePlayer player, double progress, double total) {
        List<String> parsedList = new ArrayList<>();
        for (String str : list) {
            parsedList.add(LanguageOptions.parse(str.replace("{progress}", (ChallengeType.getProgressString(progress, total))), player));
        }
        return parsedList;
    }

    public ChallengeType getChallengeType() {
        return challengeType;
    }
}

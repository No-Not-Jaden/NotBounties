package me.jadenp.notbounties.features.challenges;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.ui.gui.CustomItem;
import me.jadenp.notbounties.ui.gui.bedrock.GUIComponent;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.ActionCommands;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.features.settings.integrations.external_api.PlaceholderAPIClass;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Challenge {
    private final ChallengeType challengeType;
    private final List<String> inProgress;
    private final List<String> completed;
    private final List<Double> variations;
    private final List<Double> rewards;
    private final List<String> commands;
    private final String requirement;
    private final CustomItem guiItem; // java GUI
    private final GUIComponent guiComponent; // bedrock GUI

    public Challenge(ChallengeType challengeType, ConfigurationSection configuration) {
        this.challengeType = challengeType;
        inProgress = configuration.isSet("in-progress") ? configuration.getStringList("in-progress") : new ArrayList<>();
        completed = configuration.isSet("completed") ? configuration.getStringList("completed") : new ArrayList<>();
        if (configuration.isList("variations")) variations = configuration.getDoubleList("variations");
        else {
            if (configuration.isDouble("variations"))
                variations = new ArrayList<>(Collections.singletonList(configuration.getDouble("variations")));
            else variations = new ArrayList<>();
        }
        List<Double> tempRewards;
        if (configuration.isList("rewards")) tempRewards = configuration.getDoubleList("rewards");
        else {
            if (configuration.isDouble("rewards"))
                tempRewards = new ArrayList<>(Collections.singletonList(configuration.getDouble("rewards")));
            else tempRewards = new ArrayList<>();
        }
        if (configuration.isList("commands")) commands = configuration.getStringList("commands");
        else {
            if (configuration.isString("commands"))
                commands = new ArrayList<>(Collections.singletonList(configuration.getString("commands")));
            else commands = new ArrayList<>();
        }
        requirement = configuration.isSet("requirement") ? configuration.getString("requirement") : "";

        if (configuration.isConfigurationSection("item"))
            guiItem = new CustomItem(Objects.requireNonNull(configuration.getConfigurationSection("item")));
        else
            guiItem = new CustomItem();

        if (ConfigOptions.getIntegrations().isFloodgateEnabled() || ConfigOptions.getIntegrations().isGeyserEnabled()) {
            if (configuration.isConfigurationSection("bedrock-item"))
                guiComponent = new GUIComponent(Objects.requireNonNull(configuration.getConfigurationSection("bedrock-item")));
            else
                guiComponent = new GUIComponent();
        } else {
            guiComponent = null;
        }

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
    public void executeCommands(Player player, double start, double progress, double total, int variationIndex) {
        List<String> parsedCommands = new ArrayList<>();
        commands.forEach(str -> parsedCommands.add(str.replace("{reward}", (NumberFormatting.getValue(rewards.get(variationIndex)))).replace("{x}", (NumberFormatting.getValue(variations.get(variationIndex)))).replace("{challenge}", getChallengeTitle(player, start, progress, total, variationIndex))));
        ActionCommands.executeCommands(player, parsedCommands);
    }

    /**
     * Get a formatted ItemStack representing this challenge and a player's progress.
     * @param player Player to display the progress of.
     * @param start The starting value that the player had when the challenge was started.
     * @param progress The current value that the player has.
     * @param total The value that corresponds to a completed challenge.
     * @param variationIndex The index of the variation of this challenge that is being used.
     * @param canClaim Whether the player can claim the reward or not.
     * @return An ItemStack with a formatted name and lore.
     */
    public ItemStack getItem(OfflinePlayer player, double start, double progress, double total, int variationIndex, boolean canClaim) {
        ItemStack itemStack = guiItem.getFormattedItem(player, null);
        if (itemStack.getItemMeta() == null)
            return itemStack;
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        List<String> itemText = getDisplayText(player, start, progress, total, variationIndex, canClaim);
        if (itemText.isEmpty())
            return itemStack;
        meta.setDisplayName(itemText.remove(0));
        if (!itemText.isEmpty())
            meta.setLore(itemText);
        if (canClaim) {
            // lore can come from the item text or the guiItem lore depending on where they set it
            List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
            assert lore != null;
            lore.add("");
            lore.add(LanguageOptions.parse(LanguageOptions.getMessage("challenge-gui-claim"), player));
            meta.setLore(lore);
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public List<String> getDisplayText(OfflinePlayer player, double start, double progress, double total, int variationIndex, boolean canClaim) {
        return progress >= total && !canClaim ? parseList(completed, player, start, progress, total, variationIndex) : parseList(inProgress, player, start, progress, total, variationIndex);
    }

    /**
     * Get a built component for the player that represents this challenge.
     * @param player Player to build the component for.
     * @param start The starting value that the player had when the challenge was started.
     * @param progress The current value that the player has.
     * @param total The value that corresponds to a completed challenge.
     * @param variationIndex The index of the variation of this challenge that is being used.
     * @param canClaim Whether the player can claim the reward or not.
     * @return A built GUIComponent object ready to be displayed in a form.
     */
    public GUIComponent getComponent(OfflinePlayer player, double start, double progress, double total, int variationIndex, boolean canClaim) {
        GUIComponent component = guiComponent.copy();
        String text = "";
        if (progress >= total && !canClaim) {
            // completed
            if (!completed.isEmpty())
                text = parseChallenge(completed.get(0), player, start, progress, total, variationIndex);
        } else {
            // in-progress
            if (!inProgress.isEmpty())
                text = parseChallenge(inProgress.get(0), player, start, progress, total, variationIndex);
        }
        if (!text.isEmpty())
            component.setText(text);
        return component.buildComponent(player);
    }

    /**
     * Get a parsed title of the challenge. This is the first line of the in-progress or completed list.
     * @param player The player to parse the challenge for.
     * @param start The starting value that the player had when the challenge was started.
     * @param progress The current value that the player has.
     * @param total The value that corresponds to a completed challenge.
     * @return A formatted string of the title of this challenge.
     */
    public String getChallengeTitle(OfflinePlayer player, double start, double progress, double total, int variationIndex) {
        String title;
        if (progress >= total && !completed.isEmpty())
            title = completed.get(0);
        else if (!inProgress.isEmpty())
            title = inProgress.get(0);
        else
            title = "";
        return parseChallenge(title, player, start, progress, total, variationIndex);
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
        if (requirement.isEmpty()) {
            // built-in challenge
            switch (challengeType) {
                case CLAIM -> {
                    return Leaderboard.KILLS.getStat(player.getUniqueId());
                }
                case AMOUNT_SET -> {
                    return Leaderboard.SET.getStat(player.getUniqueId());
                }
                case BOUNTY_MULTIPLE, BOUNTY_INCREASE -> {
                    return Leaderboard.CURRENT.getStat(player.getUniqueId());
                }
                case PURCHASE_IMMUNITY -> {
                    return Leaderboard.IMMUNITY.getStat(player.getUniqueId());
                }
                case RECEIVE_BOUNTY -> {
                    return Leaderboard.DEATHS.getStat(player.getUniqueId());
                }
                case AUTO_BOUNTY -> {
                    double consoleAmount = 0;
                    if (BountyManager.hasBounty(player.getUniqueId()))
                        for (Setter setter : Objects.requireNonNull(BountyManager.getBounty(player.getUniqueId())).getSetters())
                            if (setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID))
                                consoleAmount+= setter.getAmount();
                    return consoleAmount;
                }
                default -> {
                    return 0;
                }
            }
        }
        return getCustomProgress(player);
    }

    /**
     * Get the custom progress of a player from the requirement string
     * @param player Player to parse the value for
     * @return The current value for this custom challenge
     */
    public double getCustomProgress(OfflinePlayer player) {
        if (requirement.isEmpty())
            return 0;
        String req = requirement;
        if (ConfigOptions.getIntegrations().isPapiEnabled())
            req = new PlaceholderAPIClass().parse(player, req.substring(0, req.indexOf(" ")));
        try {
            return NumberFormatting.tryParse(req);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Check if the custom requirement is completed.
     * @param player Player to parse the requirement for.
     * @return Whether the requirement is completed or not. This will return true if the challenge is not a custom challenge or placeholderAPI is not enabled
     */
    public boolean isCustomRequirementCompleted(OfflinePlayer player, double goal) {
        if (requirement.isEmpty() || !ConfigOptions.getIntegrations().isPapiEnabled()) {
            return true;
        }
        double value = getCustomProgress(player);
        String operator = requirement.substring(requirement.indexOf(" ") + 1, requirement.lastIndexOf(" "));
        return ActionCommands.compareObjects(goal, value, operator);
    }



    private List<String> parseList(List<String> list, OfflinePlayer player, double start, double progress, double total, int variationIndex) {
        List<String> parsedList = new ArrayList<>();
        for (String str : list) {
            parsedList.add(parseChallenge(str, player, start, progress, total, variationIndex));
        }
        return parsedList;
    }

    /**
     * Parse a string with challenge information. This also parses the string with the LanguageOptions.parse().
     * <p>{progress} -> % completion ({progress_value} after an operation)</p>
     * <p>{progress_bar} -> % completion display in a bar</p>
     * <p>{progress_bar_[x]} -> progress bar with x characters</p>
     * <p>{progress_raw} -> (progress)/(total)</p>
     * <p>{progress_value} -> (progress-start)/(total-start)</p>
     * <p>{start} -> start</p>
     * <p>{current} -> progress</p>
     * <p>{total} -> total</p>
     * @param text Text to be parsed.
     * @param player The player to parse the text for.
     * @param start The starting value that the player had when the challenge was started.
     * @param progress The current value that the player has.
     * @param total The value that corresponds to a completed challenge.
     * @return A parsed string
     */
    private String parseChallenge(String text, OfflinePlayer player, double start, double progress, double total, int variationIndex) {
        if (progress > total)
            progress = total;
        double progressPercent = (progress-start)/(total-start);
        if (progressPercent > 1)
            progressPercent = 1;
        text = text.replace("{progress}", ((int) (progressPercent * 100)) + "")
                .replace("{progress_raw}", NumberFormatting.formatNumber(progress) + "/" + NumberFormatting.formatNumber(total))
                .replace("{progress_value}", NumberFormatting.formatNumber(progress - start) + "/" + NumberFormatting.formatNumber(total - start))
                .replace("{start}", NumberFormatting.formatNumber(start))
                .replace("{current}", NumberFormatting.formatNumber(progress))
                .replace("{total}", NumberFormatting.formatNumber(total))
                .replace("{reward}", (NumberFormatting.formatNumber(rewards.get(variationIndex))))
                .replace("{x}", (NumberFormatting.formatNumber(variations.get(variationIndex))));

        text = saveChatColors(text, "{progress_bar}", generateProgressBar(progressPercent, 20));

        final String VARIABLE_PROGRESS_BAR = "{progress_bar_";
        while (text.contains(VARIABLE_PROGRESS_BAR) && text.substring(text.indexOf(VARIABLE_PROGRESS_BAR)).contains("}")) {
            String lengthString = text.substring(text.indexOf(VARIABLE_PROGRESS_BAR) + 14);
            lengthString = lengthString.substring(0, text.indexOf("}"));
            try {
                int length = Integer.parseInt(lengthString);
                text = saveChatColors(text, VARIABLE_PROGRESS_BAR + lengthString + "}", generateProgressBar(progressPercent, length));
            } catch (NumberFormatException e) {
                NotBounties.debugMessage("Error getting progress bar length for challenge: " + challengeType.getConfigurationName(), true);
                NotBounties.debugMessage(e.toString(), true);
                break;
            }
        }

        return LanguageOptions.parse(text, player);
    }

    /**
     * Replace a target in text and keep the chat colors after the replacement.
     * String.replace() should be used unless text and replacement have chat colors in them.
     * @param text Text that contains the target.
     * @param target Target text to be replaced.
     * @param replacement Replacement text.
     * @return The resulting string.
     */
    private String saveChatColors(String text, String target, String replacement) {
        text = ChatColor.translateAlternateColorCodes('&', text);
        replacement = ChatColor.translateAlternateColorCodes('&', replacement);
        while (text.contains(target)) {
            String before = text.substring(0, text.indexOf(target));
            String after = text.substring(text.indexOf(target) + target.length());
            text = before + replacement + ChatColor.getLastColors(before) + after;
        }
        return text;
    }

    /**
     * Generate a progress bar of '|' characters.
     * @param percent Percentage represented on the progress bar. (0-1)
     * @param length Number of characters used in the progress bar.
     * @return A progress bar representing the percent.
     */
    private String generateProgressBar(double percent, int length) {
        StringBuilder progressBar = new StringBuilder();
        percent = Math.min(percent, 1);
        int greenBars = (int) (percent * length);
        int grayBars = length - greenBars;
        if (greenBars > 0) {
            progressBar.append(ChatColor.GREEN);
            progressBar.append("|".repeat(greenBars));
        }
        if (grayBars > 0) {
            progressBar.append(ChatColor.DARK_GRAY);
            progressBar.append("|".repeat(grayBars));
        }
        return progressBar.toString();
    }

    public ChallengeType getChallengeType() {
        return challengeType;
    }

    public String getRequirement() {
        return requirement;
    }
}

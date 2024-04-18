package me.jadenp.notbounties.utils.challenges;

import me.jadenp.notbounties.utils.configuration.NumberFormatting;

public enum ChallengeType {
    CUSTOM, // Custom Requirement
    CLAIM, // Claim x bounties
    AMOUNT_SET, // Set bounties for a certain amount
    SUCCESSFUL_BOUNTY, // Have x of your set bounties be claimed
    BOUNTY_MULTIPLE, // Multiple of your bounty - 3x bounty increase
    BOUNTY_INCREASE, // Flat bounty increase
    PURCHASE_IMMUNITY, // Buy immunity
    WITNESS_BOUNTY_CLAIM, // Be near a claimed bounty
    WHITELISTED_BOUNTY_SET,  // Set a whitelisted bounty
    HOLD_POSTER, // Hold your own bounty poster
    AUTO_BOUNTY, // Have an auto bounty set on you
    RECEIVE_BOUNTY, // Have x number of people set a bounty on you
    NAKED_BOUNTY_CLAIM, // Claim a bounty with no armor on
    CLOSE_BOUNTY, // Claim a bounty with 2 hearts or fewer left
    BUY_OWN; // Buy your own bounty

    public String getConfigurationName() {
        String name = this.name().toLowerCase();
        return name.replaceAll("_", "-");
    }

    /**
     * Convert the configuration name of this challenge to the enum
     * @param name Configuration name
     * @return The challenge type associated with the configuration name
     * @throws IllegalArgumentException If the configuration name is not valid for the challenge type
     */
    public static ChallengeType convertFromConfiguration(String name) throws IllegalArgumentException {
        name = name.toUpperCase().replaceAll("-", "_");
        return ChallengeType.valueOf(name);
    }

    public static String getProgressString(double progress, double total) {
        if (progress > total) {
            return NumberFormatting.formatNumber(total) + "/" + NumberFormatting.formatNumber(total);
        }
        return NumberFormatting.formatNumber(progress) + "/" + NumberFormatting.formatNumber(total);
    }
}

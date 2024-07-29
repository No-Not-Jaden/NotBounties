package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.utils.configuration.BountyExpire;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.ListIterator;

public class TrickleBounties {
    private static float givenReward;
    private static float bountyTransfer;
    private static boolean requireBounty;

    private TrickleBounties(){}

    public static void loadConfiguration(ConfigurationSection configuration) {
        givenReward = (float) configuration.getDouble("given-reward");
        bountyTransfer = (float) configuration.getDouble("bounty-transfer");
        requireBounty = configuration.getBoolean("require-bounty");
    }

    /**
     * Returns a bounty with the values changed to reflect the givenReward ratio (rounded up).
     * Does not modify items.
     * @param bounty Bounty to be claimed
     * @param claimer Player claiming the bounty.
     * @return A bounty that should be rewarded to the claimer
     */
    public static Bounty getRewardedBounty(Bounty bounty, Player claimer) {
        Bounty rewardedBounty = new Bounty(bounty); // create a copy so the original isn't modified
        ListIterator<Setter> setterListIterator = rewardedBounty.getSetters().listIterator();
        while (setterListIterator.hasNext()) {
            Setter setter = setterListIterator.next();
            if (setter.canClaim(claimer)) {
                double newAmount = NumberFormatting.isUsingDecimals() ? setter.getAmount() * givenReward : Math.ceil(setter.getAmount() * givenReward);
                Setter newSetter = new Setter(setter.getName(), setter.getUuid(), newAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + (newAmount - setter.getAmount()));
                setterListIterator.set(newSetter);
            }
        }
        return rewardedBounty;
    }

    /**
     * Transfers a percentage of a bounty to the claimer (rounded down).
     * Items are not transferred.
     * @param bounty Bounty that was claimed.
     * @param claimer Player who claimed the bounty and is receiving the transfer
     */
    public static void transferBounty(Bounty bounty, Player claimer) {
        // check if a bounty should be transferred
        if ((!requireBounty || BountyManager.hasBounty(claimer.getUniqueId())) && bountyTransfer > 0 && bounty.getTotalBounty(claimer) > 0) {
            Bounty transferedBounty = new Bounty(claimer.getUniqueId(), new ArrayList<>(bounty.getSetters()), claimer.getName()); // create a copy so the original isn't modified
            ListIterator<Setter> setterListIterator = transferedBounty.getSetters().listIterator();
            while (setterListIterator.hasNext()) {
                Setter setter = setterListIterator.next();
                if (setter.canClaim(claimer)) {
                    double newAmount = NumberFormatting.isUsingDecimals() ? setter.getAmount() * bountyTransfer : Math.floor(setter.getAmount() * bountyTransfer);
                    Setter newSetter = new Setter(setter.getName(), setter.getUuid(), newAmount, new ArrayList<>(), System.currentTimeMillis(), setter.isNotified(), setter.getWhitelist(), BountyExpire.getTimePlayed(claimer.getUniqueId()), setter.getDisplayAmount() + (newAmount - setter.getAmount()));
                    setterListIterator.set(newSetter);
                } else {
                    setterListIterator.remove();
                }
            }
            // send message to claimer
            claimer.sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.trickleBounty, bounty.getName(), transferedBounty.getTotalBounty(), claimer));
            DataManager.addBounty(transferedBounty);
        }
    }
}

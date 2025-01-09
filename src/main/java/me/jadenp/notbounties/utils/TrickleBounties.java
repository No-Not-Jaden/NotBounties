package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.utils.configuration.BountyExpire;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
                double newAmount;
                if (setter.getAmount() < 0.01) {
                    // amount too small, reward all of it
                    newAmount = setter.getAmount();
                } else {
                    newAmount = NumberFormatting.isUsingDecimals() ? setter.getAmount() * givenReward : Math.floor(setter.getAmount() * givenReward);
                }
                Setter newSetter = new Setter(setter.getName(), setter.getUuid(), newAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + (newAmount - setter.getAmount()));
                setterListIterator.set(newSetter);
            } else {
                setterListIterator.remove();
            }
        }
        return rewardedBounty;
    }

    /**
     * Transfers a percentage of a bounty to the claimer (rounded down).
     * Items are not transferred.
     * @param bounty Bounty that was claimed.
     * @param claimer Player who claimed the bounty and is receiving the transfer
     * @return Bounty that was transfered
     */
    public static Bounty transferBounty(Bounty bounty, Player claimer) {
        // check if a bounty should be transferred
        if ((!requireBounty || BountyManager.hasBounty(claimer.getUniqueId())) && bountyTransfer > 0 && bounty.getTotalBounty(claimer) > 0) {
            List<Setter> newSetters = new LinkedList<>();
            for (Setter setter : bounty.getSetters()) {
                if (setter.canClaim(claimer)) {
                    double newAmount;
                    if (setter.getAmount() < 0.01) {
                        newAmount = 0;
                    } else {
                        newAmount = NumberFormatting.isUsingDecimals() ? setter.getAmount() * bountyTransfer : Math.ceil(setter.getAmount() * bountyTransfer);
                    }
                    if (newAmount > 0) {
                        Setter newSetter = new Setter(setter.getName(), setter.getUuid(), newAmount, new ArrayList<>(), System.currentTimeMillis(), setter.isNotified(), setter.getWhitelist(), BountyExpire.getTimePlayed(claimer.getUniqueId()), setter.getDisplayAmount() + (newAmount - setter.getAmount()));
                        newSetters.add(newSetter);
                    }
                }
            }
            Bounty transferedBounty = new Bounty(claimer.getUniqueId(), newSetters, claimer.getName()); // create a new bounty
            // send message to claimer
            claimer.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("trickle-bounty"), transferedBounty.getTotalBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())));
            DataManager.addBounty(transferedBounty);
            return transferedBounty;
        }
        return new Bounty(claimer.getUniqueId(), new LinkedList<>(), claimer.getName());
    }
}

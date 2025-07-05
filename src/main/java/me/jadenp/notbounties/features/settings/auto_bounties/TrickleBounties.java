package me.jadenp.notbounties.features.settings.auto_bounties;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.BountyExpire;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class TrickleBounties {
    private static float givenReward;
    private static float bountyTransfer;
    private static float unbountiedGivenReward;
    private static float unbountiedBountyTransfer;
    private static double naturalDeathBountyLoss;

    private TrickleBounties(){}

    public static void loadConfiguration(ConfigurationSection configuration) {
        givenReward = (float) configuration.getDouble("given-reward");
        bountyTransfer = (float) configuration.getDouble("bounty-transfer");
        unbountiedGivenReward = (float) configuration.getDouble("unbountied-claim.given-reward");
        unbountiedBountyTransfer = (float) configuration.getDouble("unbountied-claim.bounty-transfer");
        naturalDeathBountyLoss = configuration.getDouble("natural-death-bounty-loss");
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
        float givenRatio = getGivenRewardRatio(claimer);
        ListIterator<Setter> setterListIterator = rewardedBounty.getSetters().listIterator();
        while (setterListIterator.hasNext()) {
            Setter setter = setterListIterator.next();
            if (setter.canClaim(claimer)) {
                Setter newSetter = getSetterAmount(setter, givenRatio);
                setterListIterator.set(newSetter);
            } else {
                setterListIterator.remove();
            }
        }
        return rewardedBounty;
    }

    /**
     * Get bounty lost from a natural death.
     * @param bounty Original Bounty.
     * @return The bounty that is lost.
     */
    public static Bounty getLostBounty(Bounty bounty) {
        Bounty lostBounty = new Bounty(bounty); // create a copy so the original isn't modified
        ListIterator<Setter> setterListIterator = lostBounty.getSetters().listIterator();
        while (setterListIterator.hasNext()) {
            Setter setter = setterListIterator.next();
            Setter newSetter = getSetterAmount(setter, naturalDeathBountyLoss);
            if (newSetter.getAmount() == 0) {
                setterListIterator.remove();
            } else {
                setterListIterator.set(newSetter);
            }
        }
        return lostBounty;
    }

    private static @NotNull Setter getSetterAmount(Setter setter, double multiplier) {
        double newAmount;
        if (setter.getAmount() < 0.01) {
            // amount too small, reward all of it
            newAmount = setter.getAmount();
        } else {
            newAmount = NumberFormatting.isUsingDecimals() ? setter.getAmount() * multiplier : Math.floor(setter.getAmount() * multiplier);
        }
        return new Setter(setter.getName(), setter.getUuid(), newAmount, setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + (newAmount - setter.getAmount()));
    }

    /**
     * Transfers a percentage of a bounty to the claimer (rounded down).
     * Items are not transferred.
     * @param bounty Bounty that was claimed.
     * @param claimer Player who claimed the bounty and is receiving the transfer
     * @return Bounty that was transferred
     */
    public static Bounty transferBounty(Bounty bounty, Player claimer) {
        float transfterRatio = getBountyTransferRatio(claimer);
        // check if a bounty should be transferred
        if (transfterRatio <= 0 || bounty.getTotalBounty(claimer) <= 0) {
            return new Bounty(claimer.getUniqueId(), new LinkedList<>(), claimer.getName());
        }
        List<Setter> newSetters = new LinkedList<>();
        for (Setter setter : bounty.getSetters()) {
            if (setter.canClaim(claimer)) {
                double newAmount;
                if (setter.getAmount() < 0.01) {
                    newAmount = 0;
                } else {
                    newAmount = NumberFormatting.isUsingDecimals() ? setter.getAmount() * transfterRatio : Math.ceil(setter.getAmount() * transfterRatio);
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

    /**
     * Get the ratio of the bounty that should be given to the claimer.
     * @param claimer Player who claimed the bounty.
     * @return The ratio of the bounty that should be given to the claimer.
     */
    private static float getGivenRewardRatio(Player claimer) {
        if (BountyManager.hasBounty(claimer.getUniqueId())) {
            return givenReward;
        }
        return unbountiedGivenReward;
    }

    public static float getBountyTransferRatio(Player claimer) {
        if (BountyManager.hasBounty(claimer.getUniqueId())) {
            return bountyTransfer;
        }
        return unbountiedBountyTransfer;
    }
}

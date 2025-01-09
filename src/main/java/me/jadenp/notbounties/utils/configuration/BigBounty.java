package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.data.Bounty;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.IllegalFormatException;
import java.util.Objects;

import static me.jadenp.notbounties.NotBounties.displayParticle;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class BigBounty {
    private static int threshold;
    private static boolean particle;
    private enum Trigger {
        ONCE, AMOUNT, SET
    }
    private static Trigger trigger;
    public static void loadConfiguration(ConfigurationSection configuration) {
        threshold = configuration.getInt("bounty-threshold");
        particle = configuration.getBoolean("particle");
        try {
            trigger = Trigger.valueOf(Objects.requireNonNull(configuration.getString("trigger")).toUpperCase());
        } catch (IllegalFormatException e) {
            Bukkit.getLogger().warning("[NotBounties] Invalid type for big bounty trigger. (ONCE, AMOUNT, SET)");
            trigger = Trigger.ONCE;
        }
    }

    public static void setBounty(Player receiver, Bounty newBounty, double amountAdded) {
        switch (trigger) {
            case SET:
                if (newBounty.getTotalDisplayBounty() >= threshold)
                    ActionCommands.executeBigBounty(receiver, newBounty);
                break;
            case ONCE:
                if (newBounty.getTotalDisplayBounty() >= threshold && newBounty.getTotalDisplayBounty() - amountAdded < threshold)
                    ActionCommands.executeBigBounty(receiver, newBounty);
                break;
            case AMOUNT:
                if (amountAdded > threshold)
                    ActionCommands.executeBigBounty(receiver, newBounty);
                break;
        }
        if (newBounty.getTotalDisplayBounty() >= threshold && newBounty.getTotalDisplayBounty() - amountAdded < threshold) {
            displayParticle.add(receiver.getUniqueId());
            receiver.sendMessage(parse(getPrefix() + getMessage("big-bounty"), receiver));
        }
    }

    public static int getThreshold() {
        return threshold;
    }

    public static boolean isParticle() {
        return particle;
    }
}

package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.Bounty;
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
                if (newBounty.getTotalBounty() > threshold)
                    ActionCommands.executeBigBounty(receiver, newBounty.getTotalBounty());
                break;
            case ONCE:
                if (newBounty.getTotalBounty() > threshold && newBounty.getTotalBounty() - amountAdded < threshold)
                    ActionCommands.executeBigBounty(receiver, newBounty.getTotalBounty());
                break;
            case AMOUNT:
                if (amountAdded > threshold)
                    ActionCommands.executeBigBounty(receiver, newBounty.getTotalBounty());
                break;
        }
        if (newBounty.getTotalBounty() > threshold && newBounty.getTotalBounty() - amountAdded < threshold) {
            displayParticle.add(receiver);
            receiver.sendMessage(parse(prefix + bigBounty, receiver));
        }
    }

    public static int getThreshold() {
        return threshold;
    }

    public static boolean isParticle() {
        return particle;
    }
}

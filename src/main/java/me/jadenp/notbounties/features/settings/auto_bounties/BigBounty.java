package me.jadenp.notbounties.features.settings.auto_bounties;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.features.ActionCommands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.getAllBounties;
import static me.jadenp.notbounties.features.LanguageOptions.*;

public class BigBounty {

    private static final List<UUID> particlePlayers = new ArrayList<>();

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
            Bukkit.getLogger().warning("[NotBounties] Invalid type for big bounty trigger \"" + configuration.getString("trigger") + "\". (ONCE, AMOUNT, SET)");
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
            particlePlayers.add(receiver.getUniqueId());
            receiver.sendMessage(parse(getPrefix() + getMessage("big-bounty"), receiver));
        }
    }

    public static int getThreshold() {
        return threshold;
    }

    public static boolean isParticle() {
        return particle;
    }

    public static void displayParticle(){
        if (BigBounty.getThreshold() == -1 || !BigBounty.isParticle())
            return;

        for (Iterator<UUID> iterator = particlePlayers.iterator(); iterator.hasNext(); ) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if ((NotBounties.getServerVersion() >= 17 && viewer.canSee(Objects.requireNonNull(player.getPlayer())))
                            && viewer.getWorld().equals(player.getPlayer().getWorld())
                            && viewer.getLocation().distance(player.getPlayer().getLocation()) < Bukkit.getViewDistance() * 16
                            && !NotBounties.isVanished(player.getPlayer()))
                        viewer.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getPlayer().getEyeLocation().add(0, 1, 0), 0, 0, 0, 0);
                }
            } else {
                iterator.remove();
            }
        }
    }

    public static void refreshParticlePlayers() {
        particlePlayers.clear();
        List<Bounty> topBounties = getAllBounties(2);

        for (Bounty bounty : topBounties) {
            if (bounty.getTotalDisplayBounty() >= BigBounty.getThreshold()) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(bounty.getUUID());
                if (player.isOnline()) {
                    particlePlayers.add(Objects.requireNonNull(player.getPlayer()).getUniqueId());
                }
            } else {
                break;
            }
        }
    }

    public static void removeParticle(UUID uuid) {
        particlePlayers.remove(uuid);
    }

    public static void addParticle(UUID uuid) {
        if (isParticle()) {
            particlePlayers.add(uuid);
        }
    }

    /**
     * Checks if the bounty is still a big bounty after the change.
     * If it is no longer a big bounty, the player will be removed from the displayParticle list.
     * @param bounty Bounty to be changed.
     * @param change Change in the bounty amount.
     */
    public static void bigBountyCheck(@NotNull Bounty bounty, double change) {
        if (particlePlayers.contains(bounty.getUUID()) && bounty.getTotalDisplayBounty() + change < BigBounty.getThreshold())
            particlePlayers.remove(bounty.getUUID());
    }
}

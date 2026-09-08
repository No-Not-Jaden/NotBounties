package me.jadenp.notbounties.features.settings.auto_bounties;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ActionCommands;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.features.LanguageOptions.*;

public class MurderBounties {

    private MurderBounties(){}

    /**
     * Whether the bounty-increase represents the percent of the current bounty added, or a flat number.
     * If the multiplicative is set to true, and the current bounty is 0, the bounty will be set to the min-bounty.
     */
    private static boolean multiplicative;
    /**
     * The minimum time between bounties being placed on a player for murder.
     */
    private static int murderCooldown;
    /**
     * The amount of currency that the bounty increases by.
     */
    private static double murderBountyIncrease;
    /**
     * Whether bounties claimed should not set a bounty on the killer for murder.
     */
    private static boolean murderExcludeClaiming;
    /**
     * Cancel a trickle bounty if the murder bounty is greater, or cancel a murder bounty if the trickle bounty is greater.
     */
    private static boolean exclusiveMurderOrTrickle;
    /**
     * Whether NPCs should be able to raise bounties.
     */
    private static boolean allowNPC;
    /**
     * A map of the killer and the player they have killed with the time they were killed.
     * (Killer, (Player, Time))
     */
    private static final Map<UUID, Cache<UUID, Long>> playerKills = new HashMap<>();

    private static List<String> commands;

    public static void loadConfiguration(ConfigurationSection murderBounties) {
        murderCooldown = murderBounties.getInt("player-cooldown");
        murderBountyIncrease = murderBounties.getDouble("bounty-increase");
        murderExcludeClaiming = murderBounties.getBoolean("exclude-claiming");
        multiplicative = murderBounties.getBoolean("multiplicative");
        exclusiveMurderOrTrickle = murderBounties.getBoolean("exclusive-murder-or-trickle");
        allowNPC = murderBounties.getBoolean("allow-npc");
        commands = murderBounties.getStringList("commands");
        playerKills.clear();
    }
    /**
     * Removes old player kills from playerKills HashMap
     */
    public static void cleanPlayerKills() {
        playerKills.entrySet().removeIf(entry -> entry.getValue().size() == 0);
    }

    private static boolean canTriggerMurderBounty(Player player, Player killer) {
        return isEnabled()
                && !BountyManager.isNPC(killer) // don't raise bounty on an NPC
                && !( // don't raise bounty from a duel if blocked in config
                ConfigOptions.getIntegrations().isDuelsEnabled()
                        && !ConfigOptions.getIntegrations().getDuels().isMurderBounty()
                        && ConfigOptions.getIntegrations().getDuels().isInDuel(killer)
                )
                && (allowNPC || !BountyManager.isNPC(player)); // don't raise a bounty if the killed player was an NPC
    }

    private static CompletableFuture<Boolean> hasMurderImmunity(Player player, @Nullable Bounty playerBounty, Player killer, double bountyIncrease, @Nullable Bounty killerBounty) {
        CompletableFuture<Boolean> hasPermissionImmunity = ImmunityManager.hasPermissionImmunity(killer, "notbounties.immunity.murder", PlayerData::hasMurderImmunity);
        CompletableFuture<ImmunityManager.ImmunityType> appliedImmunity = ImmunityManager.getAppliedImmunity(killer.getUniqueId(), bountyIncrease);
        return CompletableFuture.allOf(hasPermissionImmunity, appliedImmunity).thenApply(ignored -> {
            double bountyAmount = playerBounty != null ? playerBounty.getTotalBounty() : 0;
            return ((!ConfigOptions.getAutoBounties().isOverrideImmunity() // immunity is not overridden
                    &&  // check external immunity
                    appliedImmunity.join() != ImmunityManager.ImmunityType.DISABLE) // has regular immunity
                    || hasPermissionImmunity.join() // has permission immunity
                    || (exclusiveMurderOrTrickle && TrickleBounties.getBountyTransferRatio(killerBounty != null) * bountyAmount > bountyIncrease) // trickle bounty will be used instead
                    )
                    && !( // check internal immunity
                    (
                            !playerKills.containsKey(killer.getUniqueId()) ||
                            !playerKills.get(killer.getUniqueId()).containsKey(player.getUniqueId()) ||
                            playerKills.get(killer.getUniqueId()).get(player.getUniqueId()) < System.currentTimeMillis() - murderCooldown * 1000L
                    ) // check for cooldown
                    && (!murderExcludeClaiming || bountyAmount < 0.01) // check if claiming a bounty is not allowed
            );
        });
    }

    /**
     * Checks if a bounty should be placed on the killer for murder, and places one if necessary.
     * @apiNote Bounties are loaded in this method. Call Asynchronously.
     * @param player Player that was killed.
     * @param killer Player that killed.
     * @return True if the trickle bounty should be canceled.
     */
    public static boolean killPlayer(Player player, @Nullable Bounty playerBounty, Player killer, @Nullable Bounty killerBounty) {
        // check if we should increase the killer's bounty
        if (canTriggerMurderBounty(player, killer)) {
            // check immunity
            double bountyIncrease = getBountyIncrease(killerBounty);
            if (Boolean.TRUE.equals(hasMurderImmunity(player, playerBounty, killer, bountyIncrease, killerBounty).join())) {
                NotBounties.debugMessage("Killer is currently immune to this murder bounty.", false);
                return false;
            }
            // increase
            if (bountyIncrease > 0) {
                addBounty(killer, bountyIncrease, new ArrayList<>(), new Whitelist(new TreeSet<>(), false)).thenAccept(bounty -> {
                    if (bounty != null) {
                        killer.sendMessage(parse(getPrefix() + getMessage("murder"), bounty.getTotalDisplayBounty(), player));
                    } else {
                        NotBounties.debugMessage("Could not increase killer's bounty.", false);
                    }
                });
            }
            if (!commands.isEmpty())
                ActionCommands.executeCommands(player, killer, commands);
            Cache<UUID, Long> kills = playerKills.computeIfAbsent(killer.getUniqueId(), k -> CacheBuilder.newBuilder().expireAfterWrite(murderCooldown, TimeUnit.SECONDS).build());
            kills.put(player.getUniqueId(), System.currentTimeMillis());
            playerKills.put(killer.getUniqueId(), kills);
            return exclusiveMurderOrTrickle;
        }
        return false;
    }

    private static double getBountyIncrease(@Nullable Bounty killerBounty) {
        double bountyAmount = killerBounty != null ? killerBounty.getTotalDisplayBounty() : 0;
        double bountyIncrease;
        if (multiplicative) {
            if (bountyAmount > ConfigOptions.getMoney().getMinBounty()) {
                bountyIncrease = bountyAmount * murderBountyIncrease;
            } else {
                bountyIncrease = ConfigOptions.getMoney().getMinBounty();
            }
        } else {
            // flat
            bountyIncrease = murderBountyIncrease;
        }
        // bound the increase to the max bounty
        if (bountyIncrease + bountyAmount > ConfigOptions.getMoney().getMaxBounty() && ConfigOptions.getMoney().getMaxBounty() > -1) {
            bountyIncrease = Math.max(ConfigOptions.getMoney().getMaxBounty() - bountyAmount, 0);
        }
        return bountyIncrease;

    }

    public static boolean isEnabled() {
        return murderBountyIncrease > 0 || !commands.isEmpty();
    }
}

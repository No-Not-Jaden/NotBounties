package me.jadenp.notbounties.features.settings.auto_bounties;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.ActionCommands;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.BountyManager.getBounty;
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
    private static Map<UUID, Map<UUID, Long>> playerKills = new HashMap<>();

    private static List<String> commands;

    public static void loadConfiguration(ConfigurationSection murderBounties) {
        murderCooldown = murderBounties.getInt("player-cooldown");
        murderBountyIncrease = murderBounties.getDouble("bounty-increase");
        murderExcludeClaiming = murderBounties.getBoolean("exclude-claiming");
        multiplicative = murderBounties.getBoolean("multiplicative");
        exclusiveMurderOrTrickle = murderBounties.getBoolean("exclusive-murder-or-trickle");
        allowNPC = murderBounties.getBoolean("allow-npc");
        commands = murderBounties.getStringList("commands");
    }

    /**
     * Removes old player kills from playerKills HashMap
     */
    public static void cleanPlayerKills() {
        Map<UUID, Map<UUID, Long>> updatedMap = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, Long>> entry : playerKills.entrySet()) {
            Map<UUID, Long> deaths = entry.getValue();
            deaths.entrySet().removeIf(entry1 -> entry1.getValue() < System.currentTimeMillis() - murderCooldown * 1000L);
            updatedMap.put(entry.getKey(), deaths);
        }
        updatedMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        playerKills = updatedMap;
    }

    private static boolean canTriggerMurderBounty(Player player, Player killer) {
        return isEnabled()
                && !BountyManager.isNPC(killer) // don't raise bounty on a npc
                && !( // don't raise bounty from a duel if blocked in config
                ConfigOptions.getIntegrations().isDuelsEnabled()
                        && !ConfigOptions.getIntegrations().getDuels().isMurderBounty()
                        && ConfigOptions.getIntegrations().getDuels().isInDuel(killer)
                )
                && (allowNPC || !BountyManager.isNPC(player)); // don't raise a bounty if the killed player was an npc
    }

    private static boolean hasMurderImmunity(Player player, Player killer, double bountyIncrease) {
        Bounty bounty = getBounty(player.getUniqueId());
        double bountyAmount = bounty != null ? bounty.getTotalBounty() : 0;
        return !ConfigOptions.getAutoBounties().isOverrideImmunity() // immunity is not overridden
                && ( // check external immunity
                        ImmunityManager.getAppliedImmunity(killer.getUniqueId(), bountyIncrease) != ImmunityManager.ImmunityType.DISABLE // has regular immunity
                        || hasPermissionImmunity(killer) // has permission immunity
                        || (exclusiveMurderOrTrickle && TrickleBounties.getBountyTransferRatio(killer) * bountyAmount > bountyIncrease) // trickle bounty will be used instead
                    )
                && !( // check internal immunity
                        (!playerKills.containsKey(killer.getUniqueId()) ||
                        !playerKills.get(killer.getUniqueId()).containsKey(player.getUniqueId()) ||
                        playerKills.get(killer.getUniqueId()).get(player.getUniqueId()) < System.currentTimeMillis() - murderCooldown * 1000L) // check for cooldown
                        && (!murderExcludeClaiming || bounty == null || bounty.getTotalDisplayBounty(killer) < 0.01) // check if claiming a bounty is not allowed
                    );
    }

    /**
     * Checks if a bounty should be placed on the killer for murder, and places one if necessary.
     * @param player Player that was killed.
     * @param killer Player that killed.
     * @return True if the trickle bounty should be canceled.
     */
    public static boolean killPlayer(Player player, Player killer) {
        // check if we should increase the killer's bounty
        if (canTriggerMurderBounty(player, killer)) {
            // check immunity
            double bountyIncrease = getBountyIncrease(killer);
            if (hasMurderImmunity(player, killer, bountyIncrease)) {
                NotBounties.debugMessage("Killer is currently immune to this murder bounty.", false);
                return false;
            }
            // increase
            if (bountyIncrease > 0) {
                addBounty(killer, bountyIncrease, new ArrayList<>(), new Whitelist(new TreeSet<>(), false));
                killer.sendMessage(parse(getPrefix() + getMessage("murder"), Objects.requireNonNull(getBounty(killer.getUniqueId())).getTotalDisplayBounty(), player));
            }
            if (!commands.isEmpty())
                ActionCommands.executeCommands(player, killer, commands);
            Map<UUID, Long> kills = playerKills.containsKey(killer.getUniqueId()) ? playerKills.get(killer.getUniqueId()) : new HashMap<>();
            kills.put(player.getUniqueId(), System.currentTimeMillis());
            playerKills.put(killer.getUniqueId(), kills);
            return exclusiveMurderOrTrickle;
        }
        return false;
    }

    private static double getBountyIncrease(Player player) {
        Bounty bounty = getBounty(player.getUniqueId());
        double bountyAmount = bounty != null ? bounty.getTotalDisplayBounty() : 0;
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

    private static boolean hasPermissionImmunity(OfflinePlayer player) {
        if (!ImmunityManager.isPermissionImmunity())
            return false;
        if (player.isOnline())
            return Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immunity.murder");
        return DataManager.getPlayerData(player.getUniqueId()).hasMurderImmunity();
    }
}

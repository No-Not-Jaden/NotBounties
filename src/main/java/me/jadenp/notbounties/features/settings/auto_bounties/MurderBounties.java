package me.jadenp.notbounties.features.settings.auto_bounties;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.ActionCommands;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
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

    /**
     * Checks if a bounty should be placed on the killer for murder, and places one if necessary.
     * @param player Player that was killed.
     * @param killer Player that killed.
     * @returns True if the trickle bounty should be canceled.
     */
    public static boolean killPlayer(Player player, Player killer) {
        // check if we should increase the killer's bounty
        if (isEnabled() && !killer.hasMetadata("NPC") && !(ConfigOptions.getIntegrations().isDuelsEnabled() && !ConfigOptions.getIntegrations().getDuels().isMurderBounty() && ConfigOptions.getIntegrations().getDuels().isInDuel(killer))) { // don't raise bounty on a npc
            // check immunity
            double bountyIncrease = getBountyIncrease(killer);
            Bounty bounty = getBounty(player.getUniqueId());
            double bountyAmount = bounty != null ? bounty.getTotalBounty() : 0;
            if (
                    !ConfigOptions.getAutoBounties().isOverrideImmunity()
                    && ImmunityManager.getAppliedImmunity(killer.getUniqueId(), bountyIncrease) != ImmunityManager.ImmunityType.DISABLE
                    || hasImmunity(killer)
                    || (exclusiveMurderOrTrickle && TrickleBounties.getBountyTransferRatio(killer) * bountyAmount > bountyIncrease)
            )
                return false;
            if ((!playerKills.containsKey(killer.getUniqueId()) ||
                    !playerKills.get(killer.getUniqueId()).containsKey(player.getUniqueId()) ||
                    playerKills.get(killer.getUniqueId()).get(player.getUniqueId()) < System.currentTimeMillis() - murderCooldown * 1000L)
                    && (!murderExcludeClaiming || !hasBounty(player.getUniqueId()) || Objects.requireNonNull(getBounty(player.getUniqueId())).getTotalDisplayBounty(killer) < 0.01)) {
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

    private static boolean hasImmunity(OfflinePlayer player) {
        if (!ImmunityManager.isPermissionImmunity())
            return false;
        if (player.isOnline())
            return Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immunity.murder");
        return DataManager.getPlayerData(player.getUniqueId()).hasMurderImmunity();
    }
}

package me.jadenp.notbounties.utils.configuration.auto_bounties;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.Immunity;
import me.jadenp.notbounties.utils.external_api.DuelsClass;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.BountyManager.getBounty;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

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
     * A map of the killer and the player they have killed with the time they were killed.
     * (Killer, (Player, Time))
     */
    private static Map<UUID, Map<UUID, Long>> playerKills = new HashMap<>();

    public static void loadConfiguration(ConfigurationSection murderBounties) {
        murderCooldown = murderBounties.getInt("player-cooldown");
        murderBountyIncrease = murderBounties.getDouble("bounty-increase");
        murderExcludeClaiming = murderBounties.getBoolean("exclude-claiming");
        multiplicative = murderBounties.getBoolean("multiplicative");
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
     */
    public static void killPlayer(Player player, Player killer) {
        // check if we should increase the killer's bounty
        if (isEnabled() && !killer.hasMetadata("NPC") && !(ConfigOptions.isDuelsEnabled() && !DuelsClass.isMurderBounty() && DuelsClass.isInDuel(killer))) { // don't raise bounty on a npc
            // check immunity
            double bountyIncrease = getBountyIncrease(killer);
            if (!ConfigOptions.autoBountyOverrideImmunity && Immunity.getAppliedImmunity(killer.getUniqueId(), bountyIncrease) != Immunity.ImmunityType.DISABLE || hasImmunity(killer))
                return;
            if ((!playerKills.containsKey(killer.getUniqueId()) ||
                    !playerKills.get(killer.getUniqueId()).containsKey(player.getUniqueId()) ||
                    playerKills.get(killer.getUniqueId()).get(player.getUniqueId()) < System.currentTimeMillis() - murderCooldown * 1000L)
                    && (!murderExcludeClaiming || !hasBounty(player.getUniqueId()) || Objects.requireNonNull(getBounty(player.getUniqueId())).getTotalDisplayBounty(killer) < 0.01)) {
                // increase
                addBounty(killer, bountyIncrease, new ArrayList<>(), new Whitelist(new ArrayList<>(), false));
                killer.sendMessage(parse(getPrefix() + getMessage("murder"), Objects.requireNonNull(getBounty(killer.getUniqueId())).getTotalDisplayBounty(), player));
                Map<UUID, Long> kills = playerKills.containsKey(killer.getUniqueId()) ? playerKills.get(killer.getUniqueId()) : new HashMap<>();
                kills.put(player.getUniqueId(), System.currentTimeMillis());
                playerKills.put(killer.getUniqueId(), kills);
            }

        }

    }

    private static double getBountyIncrease(Player player) {
        if (!multiplicative)
            return murderBountyIncrease;
        Bounty bounty = getBounty(player.getUniqueId());
        if (bounty != null && bounty.getTotalDisplayBounty() > ConfigOptions.minBounty) {
            return bounty.getTotalBounty() * murderBountyIncrease;
        } else {
            return ConfigOptions.minBounty;
        }
    }

    public static boolean isEnabled() {
        return murderBountyIncrease > 0;
    }

    private static boolean hasImmunity(OfflinePlayer player) {
        if (!Immunity.isPermissionImmunity())
            return false;
        if (player.isOnline())
            return Objects.requireNonNull(player.getPlayer()).hasPermission("notbounties.immunity.murder");
        return DataManager.getPlayerData(player.getUniqueId()).hasMurderImmunity();
    }
}

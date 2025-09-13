package me.jadenp.notbounties.features;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.PVPHistory;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.integrations.external_api.worldguard.WorldGuardClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.claimBounty;

public class PVPRestrictions implements Listener {
    private static List<String> worlds = new ArrayList<>();
    private static int rule;
    private static int pvpTime;
    private static final Map<UUID, List<PVPHistory>> historyMap = Collections.synchronizedMap(new HashMap<>()); // uuid = persion who got hit, list of the people who damaged them and when
    private static int combatLoggingTime;
    private static boolean combatLoggingSendMessage;
    private static double deathTaxOverride;

    public static void loadConfiguration(ConfigurationSection configurationSection) {
        worlds = configurationSection.getStringList("worlds");
        rule = configurationSection.getInt("rule");
        pvpTime = configurationSection.getInt("pvp-time");
        combatLoggingTime = configurationSection.getInt("combat-logging.time");
        combatLoggingSendMessage = configurationSection.getBoolean("combat-logging.send-message");
        deathTaxOverride = configurationSection.getDouble("combat-logging.death-tax-override");
    }

    @EventHandler
    public void onPVP(EntityDamageByEntityEvent event) {
        if ((event.getEntity() instanceof Player entity && event.getDamager() instanceof Player damager) && !NotBounties.isPaused())
            controlPVP(entity, damager, event);

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (
                event.getHitEntity() != null
                && event.getEntity().getShooter() != null
                && !NotBounties.isPaused()
                && event.getHitEntity() instanceof Player hitEntity
                && event.getEntity().getShooter() instanceof Player shooter
        ) {
            controlPVP(hitEntity, shooter, event);
        }

    }

    /**
     * Check if the damager attacked the receiver less than pvpTime seconds ago.
     * @param damager Player that is attacking.
     * @param receiver Player that is receiving the attack.
     * @return True if the damager has attacked the receiver recently.
     */
    private boolean hasAttackedRecently(Player damager, Player receiver) {
        if (!historyMap.containsKey(receiver.getUniqueId())) {
            return false;
        }
        List<PVPHistory> pvpHistoryList = historyMap.get(receiver.getUniqueId());
        for (PVPHistory pvpHistory : pvpHistoryList) {
            if (pvpHistory.getAttacker().equals(damager.getUniqueId())) {
                return pvpHistory.getLastHit() > System.currentTimeMillis() - pvpTime * 1000L;
            }
        }
        return false;
    }

    private static boolean isCombatSafe(Player player) {
        if (!historyMap.containsKey(player.getUniqueId())) {
            return true;
        }
        return historyMap.get(player.getUniqueId()).stream().allMatch(PVPHistory::isCombatSafe);
    }

    private void recordAttack(Player damager, Player receiver) {
        final UUID receiverId = receiver.getUniqueId();
        final UUID attackerId = damager.getUniqueId();

        List<PVPHistory> histories = historyMap.computeIfAbsent(receiverId, id -> new ArrayList<>());

        PVPHistory existing = findHistoryForAttacker(histories, attackerId);
        if (existing != null) {
            existing.setLastHit(attackerId);
            return;
        }

        // No history from the damager yet
        histories.add(new PVPHistory(attackerId));
    }

    private PVPHistory findHistoryForAttacker(List<PVPHistory> histories, UUID attackerId) {
        for (PVPHistory entry : histories) {
            if (entry.getAttacker().equals(attackerId)) {
                return entry;
            }
        }
        return null;
    }


    private void controlPVP(Player player, Player damager, Cancellable event) {
        int localPVPRule = ConfigOptions.getIntegrations().isWorldGuardEnabled() ? WorldGuardClass.getPVPRuleOverride(damager, player.getLocation()) : -1;

        if (localPVPRule == -1 && (!worlds.contains(player.getWorld().getName()) && !worlds.isEmpty()))
            return;

        if (localPVPRule == -1)
            localPVPRule = rule;

        int localCombatTime = getCombatLoggingTime(player);

        switch (localPVPRule) {
            case 1:
                if (!BountyManager.hasBounty(player.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
                break;
            case 2:
                if (!BountyManager.hasBounty(player.getUniqueId()) && !hasAttackedRecently(player, damager)) {
                    event.setCancelled(true);
                    return;
                }

                break;
            case 3:
                if (!BountyManager.hasBounty(player.getUniqueId()) && !BountyManager.hasBounty(damager.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
                break;
            default:
                // out of bounds - same rule as 0
                break;
        }

        if ((localCombatTime > 0 || localPVPRule == 2) && BountyManager.hasBounty(player.getUniqueId())) {
            if (isCombatSafe(player) && localCombatTime > 0 && combatLoggingSendMessage) {
                player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("combat-tag").replace("{time}", (LocalTime.formatTime(localCombatTime * 1000L, LocalTime.TimeFormat.RELATIVE))), player));
            }
            recordAttack(damager, player);
        }
    }

    public static synchronized void checkCombatExpiry() {
        if (combatLoggingTime < 1 && !ConfigOptions.getIntegrations().isWorldGuardEnabled()) // optimization return
            return;
        Iterator<Map.Entry<UUID, List<PVPHistory>>> iterator = historyMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, List<PVPHistory>> entry = iterator.next();
            List<PVPHistory> pvpHistories = entry.getValue();
            boolean allCombatSafe = true;
            boolean sendMsg = false;
            // go through each last interaction for this player
            for (Iterator<PVPHistory> iter = pvpHistories.iterator(); iter.hasNext(); ) {
                PVPHistory pvpHistory = iter.next();
                long timeSinceLastHit = System.currentTimeMillis() - pvpHistory.getLastHit();
                int combatLoggingTime = getCombatLoggingTime(Bukkit.getOfflinePlayer(entry.getKey()));
                if (!pvpHistory.isCombatSafe() && timeSinceLastHit > combatLoggingTime * 1000L) {
                    // too old for combat logging
                    sendMsg = true;
                    pvpHistory.setCombatSafe(true);
                } else {
                    allCombatSafe = false;
                }
                if ((timeSinceLastHit > pvpTime * 1000L && pvpHistory.isCombatSafe())) {
                    // is combat safe (for rule 2) and is past the combat logging time
                    iter.remove();
                }
            }
            if (allCombatSafe && sendMsg && combatLoggingSendMessage) {
                // send the combat-safe message
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null)
                    player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("combat-safe"), player));
            }
            if (pvpHistories.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static int getCombatLoggingTime(OfflinePlayer player) {
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null)
            return 0; // offline player should never have a combat logging time because they already combat logged
        if (player.isOnline() && ConfigOptions.getIntegrations().isWorldGuardEnabled()) {
            int value = WorldGuardClass.getCombatLogOverride(player.getPlayer(), onlinePlayer.getLocation());
            if (value != -1)
                return value;
        }

        if (worlds.isEmpty())
            return combatLoggingTime;
        if (onlinePlayer.getLocation().getWorld() != null && !worlds.contains(onlinePlayer.getLocation().getWorld().getName()))
            return 0;
        return combatLoggingTime;
    }

    // priority is so that drops are modified before other plugins see them
    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent event) {
        if (NotBounties.isPaused())
            return;
        int localCombatLoggingTime = ConfigOptions.getIntegrations().isWorldGuardEnabled() ? WorldGuardClass.getCombatLogOverride(event.getPlayer(), event.getPlayer().getLocation()) : -1;
        if ((combatLoggingTime < 1 && localCombatLoggingTime == -1) || localCombatLoggingTime == 0)
            return;
        if (localCombatLoggingTime == -1 && (!worlds.contains(event.getPlayer().getWorld().getName()) && !worlds.isEmpty()))
            return;
        if (!BountyManager.hasBounty(event.getPlayer().getUniqueId()) || !historyMap.containsKey(event.getPlayer().getUniqueId()))
            return;
        PVPHistory pvpHistory = getLastHit(event.getPlayer());
        if (pvpHistory == null)
            return;
        long timeSinceLastHit = System.currentTimeMillis() - pvpHistory.getLastHit();
        if (localCombatLoggingTime == -1)
            localCombatLoggingTime = combatLoggingTime;
        if (!pvpHistory.isCombatSafe() && timeSinceLastHit < localCombatLoggingTime * 1000L) {
            // claim bounty - attacker must still be online
            double deathTax = deathTaxOverride == -1 ? ConfigOptions.getMoney().getDeathTax() : deathTaxOverride;
            claimBounty(event.getPlayer(), Bukkit.getPlayer(pvpHistory.getAttacker()), Arrays.asList(event.getPlayer().getInventory().getContents()), true, deathTax);
        }

    }

    private static PVPHistory getLastHit(Player player) {
        if (!historyMap.containsKey(player.getUniqueId()))
            return null;
        List<PVPHistory> pvpHistories = historyMap.get(player.getUniqueId());
        if (pvpHistories.isEmpty())
            return null;
        PVPHistory minLastHit = pvpHistories.get(0);
        for (PVPHistory pvpHistory : pvpHistories) {
            if (pvpHistory.getLastHit() < minLastHit.getLastHit())
                minLastHit = pvpHistory;
        }
        return minLastHit;
    }

    public static void onBountyClaim(Player receiver) {
        // send safe message if player has combat timer
        int localCombatLoggingTime = ConfigOptions.getIntegrations().isWorldGuardEnabled() ? WorldGuardClass.getCombatLogOverride(receiver, receiver.getLocation()) : -1;
        if ((combatLoggingTime < 1 && localCombatLoggingTime == -1) || localCombatLoggingTime == 0)
            return;
        if (localCombatLoggingTime == -1 && (!worlds.contains(receiver.getWorld().getName()) && !worlds.isEmpty()))
            return;
        if (!historyMap.containsKey(receiver.getUniqueId()))
            return;
        PVPHistory lastHit = getLastHit(receiver);
        if (lastHit == null)
            return;
        long timeSinceLastHit = System.currentTimeMillis() - lastHit.getLastHit();
        if (localCombatLoggingTime == -1)
            localCombatLoggingTime = combatLoggingTime;
        if (!isCombatSafe(receiver) && timeSinceLastHit < localCombatLoggingTime * 1000L) {
            receiver.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("combat-safe"), receiver));
            historyMap.remove(receiver.getUniqueId());
        }
    }

}

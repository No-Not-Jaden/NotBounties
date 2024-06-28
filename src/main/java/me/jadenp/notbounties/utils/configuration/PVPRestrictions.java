package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.PVPHistory;
import me.jadenp.notbounties.utils.BountyClaimRequirements;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import me.jadenp.notbounties.utils.externalAPIs.WorldGuardClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.BountyManager.claimBounty;

public class PVPRestrictions implements Listener {
    private static List<String> worlds = new ArrayList<>();
    private static int rule;
    private static int pvpTime;
    private static final Map<UUID, PVPHistory> historyMap = new HashMap<>();
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
        if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        int localPVPRule = BountyClaimRequirements.worldGuardEnabled ? new WorldGuardClass().getPVPRuleOverride(damager, player.getLocation()) : 0;

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
                if (!BountyManager.hasBounty(player.getUniqueId()) && (!historyMap.containsKey(damager.getUniqueId()) || historyMap.get(damager.getUniqueId()).getLastHit() < System.currentTimeMillis() - pvpTime * 1000L)) {
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
            if (historyMap.containsKey(player.getUniqueId())) {
                PVPHistory history = historyMap.get(player.getUniqueId());
                if (history.isCombatSafe() && localCombatTime > 0 && combatLoggingSendMessage) {
                    event.getEntity().sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.combatTag.replace("{time}", (LocalTime.formatTime(localCombatTime * 1000L, LocalTime.TimeFormat.RELATIVE))), player));
                }
                history.setLastHit(event.getDamager().getUniqueId());
            } else {
                historyMap.put(player.getUniqueId(), new PVPHistory(damager.getUniqueId()));
                if (localCombatTime > 0 && combatLoggingSendMessage) {
                    event.getEntity().sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.combatTag.replace("{time}", (LocalTime.formatTime(localCombatTime * 1000L, LocalTime.TimeFormat.RELATIVE))), player));
                }
            }
        }
    }

    public static void checkCombatExpiry() {
        if (combatLoggingTime < 1 && !BountyClaimRequirements.worldGuardEnabled) // optimization return
            return;
        Iterator<Map.Entry<UUID, PVPHistory>> iterator = historyMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PVPHistory> entry = iterator.next();
            PVPHistory pvpHistory = entry.getValue();
            long timeSinceLastHit = System.currentTimeMillis() - pvpHistory.getLastHit();
            int combatLoggingTime = getCombatLoggingTime(Bukkit.getOfflinePlayer(entry.getKey()));
            if (!pvpHistory.isCombatSafe() && timeSinceLastHit > combatLoggingTime * 1000L) {
                // too old for combat logging
                if (combatLoggingSendMessage) {
                    // send safe message
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null)
                        player.sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.combatSafe, player));
                }
                pvpHistory.setCombatSafe(true);
            }
            if (timeSinceLastHit > pvpTime * 1000L && pvpHistory.isCombatSafe()) {
                iterator.remove();
            }
        }

    }

    private static int getCombatLoggingTime(OfflinePlayer player) {
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null)
            return 0; // offline player should never have a combat logging time because they already combat logged
        if (player.isOnline() && BountyClaimRequirements.worldGuardEnabled) {
            int value = new WorldGuardClass().getCombatLogOverride(player.getPlayer(), onlinePlayer.getLocation());
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
        int localCombatLoggingTime = BountyClaimRequirements.worldGuardEnabled ? new WorldGuardClass().getCombatLogOverride(event.getPlayer(), event.getPlayer().getLocation()) : -1;
        if ((combatLoggingTime < 1 && localCombatLoggingTime == -1) || localCombatLoggingTime == 0)
            return;
        if (localCombatLoggingTime == -1 && (!worlds.contains(event.getPlayer().getWorld().getName()) && !worlds.isEmpty()))
            return;
        if (!BountyManager.hasBounty(event.getPlayer().getUniqueId()) || !historyMap.containsKey(event.getPlayer().getUniqueId()))
            return;
        PVPHistory pvpHistory = historyMap.get(event.getPlayer().getUniqueId());
        long timeSinceLastHit = System.currentTimeMillis() - pvpHistory.getLastHit();
        if (localCombatLoggingTime == -1)
            localCombatLoggingTime = combatLoggingTime;
        if (!pvpHistory.isCombatSafe() && timeSinceLastHit < localCombatLoggingTime * 1000L) {
            // claim bounty - attacker must still be online
            double deathTaxCopy = ConfigOptions.deathTax;
            if (deathTaxOverride != -1)
                ConfigOptions.deathTax = deathTaxOverride;
            claimBounty(event.getPlayer(), Bukkit.getPlayer(pvpHistory.getAttacker()), Arrays.asList(event.getPlayer().getInventory().getContents()), true);
            ConfigOptions.deathTax = deathTaxCopy;
        }

    }

    public static void onBountyClaim(Player receiver) {
        // send safe message if player has combat timer
        int localCombatLoggingTime = BountyClaimRequirements.worldGuardEnabled ? new WorldGuardClass().getCombatLogOverride(receiver, receiver.getLocation()) : -1;
        if ((combatLoggingTime < 1 && localCombatLoggingTime == -1) || localCombatLoggingTime == 0)
            return;
        if (localCombatLoggingTime == -1 && (!worlds.contains(receiver.getWorld().getName()) && !worlds.isEmpty()))
            return;
        if (!historyMap.containsKey(receiver.getUniqueId()))
            return;
        PVPHistory pvpHistory = historyMap.get(receiver.getUniqueId());
        long timeSinceLastHit = System.currentTimeMillis() - pvpHistory.getLastHit();
        if (localCombatLoggingTime == -1)
            localCombatLoggingTime = combatLoggingTime;
        if (!pvpHistory.isCombatSafe() && timeSinceLastHit < localCombatLoggingTime * 1000L) {
            receiver.sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.combatSafe, receiver));
            historyMap.remove(receiver.getUniqueId());
        }
    }

}

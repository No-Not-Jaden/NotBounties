package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.PVPHistory;
import me.jadenp.notbounties.utils.BountyManager;
import org.bukkit.Bukkit;
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

    public static void loadConfiguration(ConfigurationSection configurationSection) {
        worlds = configurationSection.getStringList("worlds");
        rule = configurationSection.getInt("rule");
        pvpTime = configurationSection.getInt("pvp-time");
        combatLoggingTime = configurationSection.getInt("combat-logging.time");
        combatLoggingSendMessage = configurationSection.getBoolean("combat-logging.send-message");
    }
    @EventHandler
    public void onPVP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        if (!worlds.contains(player.getWorld().getName()) && !worlds.isEmpty())
            return;

        switch (rule) {
            case 1:
                if (!BountyManager.hasBounty(player))
                    event.setCancelled(true);
                break;
            case 2:
                if (!BountyManager.hasBounty(player)) {
                    if (!historyMap.containsKey(damager.getUniqueId()) || historyMap.get(damager.getUniqueId()).getLastHit() < System.currentTimeMillis() - pvpTime * 1000L) {
                        event.setCancelled(true);
                    }
                }
                break;
            case 3:
                if (!BountyManager.hasBounty(player) && !BountyManager.hasBounty(damager))
                    event.setCancelled(true);
                break;
        }

        if ((combatLoggingTime > 0 || rule == 2) && BountyManager.hasBounty(player)) {
            if (historyMap.containsKey(damager.getUniqueId())) {
                historyMap.get(damager.getUniqueId()).setLastHit(event.getDamager().getUniqueId());
            } else {
                historyMap.put(player.getUniqueId(), new PVPHistory(damager.getUniqueId()));
                if (combatLoggingTime > 0 && combatLoggingSendMessage) {
                    event.getEntity().sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.combatTag.replaceAll("\\{time}", Matcher.quoteReplacement(LanguageOptions.formatTime(combatLoggingTime * 1000L))), (OfflinePlayer) event.getEntity()));
                }
            }
        }
    }

    public static void checkCombatExpiry() {
        if (combatLoggingTime < 1)
            return;
        Iterator<Map.Entry<UUID, PVPHistory>> iterator = historyMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PVPHistory> entry = iterator.next();
            PVPHistory pvpHistory = entry.getValue();
            long timeSinceLastHit = System.currentTimeMillis() - pvpHistory.getLastHit();
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

    // priority is so that drops are modified before other plugins see them
    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent event) {
        if (combatLoggingTime < 1)
            return;
        if (!worlds.contains(event.getPlayer().getWorld().getName()) && !worlds.isEmpty())
            return;
        if (!BountyManager.hasBounty(event.getPlayer()) || !historyMap.containsKey(event.getPlayer().getUniqueId()))
            return;
        PVPHistory pvpHistory = historyMap.get(event.getPlayer().getUniqueId());
        long timeSinceLastHit = System.currentTimeMillis() - pvpHistory.getLastHit();
        if (!pvpHistory.isCombatSafe() && timeSinceLastHit < combatLoggingTime * 1000L) {
            // claim bounty - attacker must still be online
            claimBounty(event.getPlayer(), Bukkit.getPlayer(pvpHistory.getAttacker()), Arrays.asList(event.getPlayer().getInventory().getContents()), true);
        }

    }

}

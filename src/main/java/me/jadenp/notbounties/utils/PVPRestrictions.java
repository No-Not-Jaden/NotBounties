package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.PVPHistory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class PVPRestrictions implements Listener {
    private static List<String> worlds = new ArrayList<>();
    private static int rule;
    private static int pvpTime;
    private static final Map<UUID, PVPHistory> historyMap = new HashMap<>();

    public static void loadConfiguration(ConfigurationSection configurationSection) {
        worlds = configurationSection.getStringList("worlds");
        rule = configurationSection.getInt("rule");
        pvpTime = configurationSection.getInt("pvp-time");
    }
    @EventHandler
    public void onPVP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        if (!worlds.contains(player.getWorld().getName()))
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
                } else {
                    if (historyMap.containsKey(damager.getUniqueId()))
                        historyMap.get(damager.getUniqueId()).setLastHit();
                    else
                        historyMap.put(player.getUniqueId(), new PVPHistory(damager.getUniqueId()));
                }
                break;
            case 3:
                if (!BountyManager.hasBounty(player) && !BountyManager.hasBounty(damager))
                    event.setCancelled(true);
                break;
        }

    }

}

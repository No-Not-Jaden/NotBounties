package me.jadenp.notbounties.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlaceholderAPIClass {
    public String parse(OfflinePlayer player, String text){
        try {
            if (player.isOnline()) {
                Player p = player.getPlayer();
                if (p != null)
                    return PlaceholderAPI.setPlaceholders(p, text);
            }
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (NullPointerException e) {
            return text;
        }
    }
}

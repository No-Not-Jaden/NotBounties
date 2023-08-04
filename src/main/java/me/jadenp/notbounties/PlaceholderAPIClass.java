package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPIClass {
    public String parse(OfflinePlayer player, String text){
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}

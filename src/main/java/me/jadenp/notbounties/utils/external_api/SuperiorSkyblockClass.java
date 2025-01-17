package me.jadenp.notbounties.utils.external_api;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import org.bukkit.entity.Player;

public class SuperiorSkyblockClass {
    private SuperiorSkyblockClass() {}
    public static boolean onSameIsland(Player player1, Player player2) {
        return SuperiorSkyblockAPI.getPlayer(player1).getIsland().isMember(SuperiorSkyblockAPI.getPlayer(player2));
    }
}

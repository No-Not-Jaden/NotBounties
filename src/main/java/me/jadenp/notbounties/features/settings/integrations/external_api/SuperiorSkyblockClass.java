package me.jadenp.notbounties.features.settings.integrations.external_api;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.entity.Player;

public class SuperiorSkyblockClass {
    private SuperiorSkyblockClass() {}
    public static boolean onSameIsland(Player player1, Player player2) {
        Island island = SuperiorSkyblockAPI.getPlayer(player1).getIsland();
        return island != null && island.isMember(SuperiorSkyblockAPI.getPlayer(player2));
    }
}

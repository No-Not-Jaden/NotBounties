package me.jadenp.notbounties.features.settings.integrations.external_api;

import fr.xyness.SCS.API.SimpleClaimSystemAPI;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;

public class SimpleClaimSystemClass {
    private SimpleClaimSystemClass(){}

    private static boolean isRegistered() {
        SimpleClaimSystem scs = (SimpleClaimSystem) Bukkit.getPluginManager().getPlugin("SimpleClaimSystem");
        if (scs == null) return false;
        SimpleClaimSystemAPI_Provider.initialize(scs);
        return true;
    }

    public static boolean isClaimShared(Player player, Player target) {
        if (!isRegistered()) return false;
        SimpleClaimSystemAPI api = SimpleClaimSystemAPI_Provider.getAPI();
        Set<Claim> claims = api.getPlayerClaims(target);
        for (Claim claim : claims) {
            Bukkit.getLogger().info(claim.getName());
            if (claim.isMember(player.getUniqueId()))
                return true;
        }
        claims = api.getPlayerClaims(player);
        for (Claim claim : claims) {
            Bukkit.getLogger().info(claim.getName());
            if (claim.isMember(target.getUniqueId()))
                return true;
        }
        return false;
    }
}

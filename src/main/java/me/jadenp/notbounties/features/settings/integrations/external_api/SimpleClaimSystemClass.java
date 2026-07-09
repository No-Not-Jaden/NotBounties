package me.jadenp.notbounties.features.settings.integrations.external_api;

import fr.xyness.SimpleClaimSystem.API.SCS_API;
import fr.xyness.SimpleClaimSystem.API.SCS_API_Provider;
import fr.xyness.SimpleClaimSystem.Types.Claim;
import org.bukkit.entity.Player;

import java.util.List;

public class SimpleClaimSystemClass {
    private SimpleClaimSystemClass(){}

    private static SCS_API api = null;

    private static boolean isRegistered() {
        if (api != null) return true;
        if (SCS_API_Provider.isRegistered()) {
            api = SCS_API_Provider.get();
            return true;
        }
        return false;
    }

    public static boolean isClaimShared(Player player, Player target) {
        if (!isRegistered()) return false;
        // could use the async api, but the players should be in memory since they are online
        List<Claim> claims = api.getClaimsByOwner(target.getUniqueId());
        for (Claim claim : claims) {
            if (claim.isMember(player.getUniqueId()))
                return true;
        }
        claims = api.getClaimsByOwner(player.getUniqueId());
        for (Claim claim : claims) {
            if (claim.isMember(target.getUniqueId()))
                return true;
        }
        return false;
    }
}

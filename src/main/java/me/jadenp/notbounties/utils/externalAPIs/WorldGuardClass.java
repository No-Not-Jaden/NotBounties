package me.jadenp.notbounties.utils.externalAPIs;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardClass {
    private static WorldGuard api = null;
    private static StateFlag claimBounties;
    private static boolean failedStartup = false;
    public WorldGuardClass() {
        if (api == null) {
            api = WorldGuard.getInstance();
        }
    }

    public void registerClaimFlag(){
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag with the name "track-kills", defaulting to true
            StateFlag flag = new StateFlag("claim-bounties", true);
            registry.register(flag);
            claimBounties = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("track-kills");
            if (existing instanceof StateFlag) {
                claimBounties = (StateFlag) existing;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag, but with a different data type!");
                failedStartup = true;
            }
        }
    }

    public boolean canClaim(Player player, Location claimLocation) {
        if (failedStartup)
            return true;
        // query claimBounties flag for the player at the claim location
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        com.sk89q.worldedit.util.Location location = BukkitAdapter.adapt(claimLocation);
        return query.testState(location, localPlayer, claimBounties);
    }
}

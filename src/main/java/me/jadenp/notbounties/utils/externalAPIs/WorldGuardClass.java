package me.jadenp.notbounties.utils.externalAPIs;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
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
    private static IntegerFlag bountyPVPRule;
    private static IntegerFlag bountyCombatLogTime;
    private static boolean failedStartup = false;
    public WorldGuardClass() {
        if (api == null) {
            api = WorldGuard.getInstance();
        }
    }

    public void registerFlags() {
        registerClaimFlag();
        registerCombatLogFlag();
        registerPVPRuleFlag();
    }

    private void registerClaimFlag(){
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag, defaulting to true
            StateFlag flag = new StateFlag("claim-bounties", true);
            registry.register(flag);
            claimBounties = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("claim-bounties");
            if (existing instanceof StateFlag) {
                claimBounties = (StateFlag) existing;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag \"claim-bounties\", but with a different data type!");
                failedStartup = true;
            }
        }
    }

    private void registerPVPRuleFlag(){
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag, defaulting to true
            IntegerFlag flag = new IntegerFlag("bounty-pvp-rule");
            flag.setSuggestedValues(new Number[] {-1, 0, 1, 2, 3});
            registry.register(flag);
            bountyPVPRule = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("bounty-pvp-rule");
            if (existing instanceof IntegerFlag) {
                bountyPVPRule = (IntegerFlag) existing;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag \"bounty-pvp-rule\", but with a different data type!");
                failedStartup = true;
            }
        }
    }

    private void registerCombatLogFlag(){
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag, defaulting to true
            IntegerFlag flag = new IntegerFlag("bounty-combat-log-time");
            registry.register(flag);
            bountyCombatLogTime = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("bounty-combat-log-time");
            if (existing instanceof IntegerFlag) {
                bountyCombatLogTime = (IntegerFlag) existing;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag \"bounty-combat-log-time\", but with a different data type!");
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

    /**
     * Get a combat log override time at a location for a player in a region
     * @param player Player to query the value for
     * @param logutLocation Location the player logged out
     * @return The overriden value or -1 if no value has been set
     */
    public int getCombatLogOverride(Player player, Location logutLocation) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        com.sk89q.worldedit.util.Location location = BukkitAdapter.adapt(logutLocation);
        Integer value = query.queryValue(location, localPlayer, bountyCombatLogTime);
        if (value == null)
            return -1;
        return value;
    }

    /**
     * Get a pvp rule override at a location for a player in a region
     * @param player Player to query the value for
     * @param pvpLocation Location of the pvp
     * @return The overriden value or 0 if no value has been set
     */
    public int getPVPRuleOverride(Player player, Location pvpLocation) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        com.sk89q.worldedit.util.Location location = BukkitAdapter.adapt(pvpLocation);
        Integer value = query.queryValue(location, localPlayer, bountyPVPRule);
        if (value == null)
            return -1;
        return value;
    }
}

package me.jadenp.notbounties.utils.external_api;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuardClass {
    private static WorldGuard api = null;
    private static StateFlag claimBounties;
    private static IntegerFlag bountyPVPRule;
    private static IntegerFlag bountyCombatLogTime;
    private static StateFlag bountyEntry;
    private static StateFlag bountyExit;
    private static StateFlag bountyTeleportEntry;
    private static StateFlag bountyTeleportExit;
    private static boolean failedStartup = false;
    private static boolean enabled;
    private static final Map<UUID, Long> lastMessages = new HashMap<>();
    private static final long MESSAGE_INTERVAL_MS = 1000;

    public static void setEnabled(boolean enabled) {
        WorldGuardClass.enabled = enabled;
        if (enabled && api == null) {
            api = WorldGuard.getInstance();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private WorldGuardClass() {}

    public static void registerFlags() {
        registerClaimFlag();
        registerCombatLogFlag();
        registerPVPRuleFlag();
        registerBountyEntryFlag();
        registerBountyExitFlag();
        registerBountyTeleportEntryFlag();
        registerBountyTeleportExitFlag();
    }

    private static void registerBountyEntryFlag() {
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag, defaulting to true
            StateFlag flag = new StateFlag("bounty-entry", true);
            registry.register(flag);
            bountyEntry = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("bounty-entry");
            if (existing instanceof StateFlag stateFlag) {
                bountyEntry = stateFlag;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag \"bounty-entry\", but with a different data type!");
                failedStartup = true;
            }
        }
    }

    private static void registerBountyExitFlag() {
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag, defaulting to true
            StateFlag flag = new StateFlag("bounty-exit", true);
            registry.register(flag);
            bountyExit = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("bounty-exit");
            if (existing instanceof StateFlag stateFlag) {
                bountyExit = stateFlag;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag \"bounty-exit\", but with a different data type!");
                failedStartup = true;
            }
        }
    }

    private static void registerBountyTeleportEntryFlag() {
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag, defaulting to true
            StateFlag flag = new StateFlag("bounty-teleport-entry", true);
            registry.register(flag);
            bountyTeleportEntry = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("bounty-teleport-entry");
            if (existing instanceof StateFlag stateFlag) {
                bountyTeleportEntry = stateFlag;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag \"bounty-teleport-entry\", but with a different data type!");
                failedStartup = true;
            }
        }
    }

    private static void registerBountyTeleportExitFlag() {
        FlagRegistry registry = api.getFlagRegistry();
        try {
            // create a flag, defaulting to true
            StateFlag flag = new StateFlag("bounty-teleport-exit", true);
            registry.register(flag);
            bountyTeleportExit = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("bounty-teleport-exit");
            if (existing instanceof StateFlag stateFlag) {
                bountyTeleportExit = stateFlag;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Bukkit.getLogger().warning("[NotBounties] Another plugin is using the same WorldGuard flag \"bounty-teleport-exit\", but with a different data type!");
                failedStartup = true;
            }
        }
    }

    private static void registerClaimFlag(){
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

    private static void registerPVPRuleFlag(){
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

    private static void registerCombatLogFlag(){
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

    public static boolean canMove(Player player, Location from, Location to) {
        // check if moved more than one block
        if (from.getWorld() != null && from.getWorld().equals(to.getWorld()) && !from.getBlock().equals(to.getBlock()) && DataManager.getLocalData().getOnlineBounty(player.getUniqueId()) != null) {
            // query flags for the locations
            return canTranspose(player, from, to, false);
        }
        return true;
    }

    public static boolean canTeleport(Player player, Location from, Location to) {
        // check if moved more than one block
        if (from.getWorld() != null && to.getWorld() != null && DataManager.getLocalData().getOnlineBounty(player.getUniqueId()) != null) {
            // query flags for the locations
            return canTranspose(player, from, to, true);
        }
        return true;
    }

    /**
     * Check if a bountied player can move between locations.
     * @param player Player that is moving.
     * @param from The location that the player is moving from.
     * @param to The location that the player is moving to.
     * @param teleport Whether the player is teleporting to that location.
     * @return True if the player can move.
     */
    private static boolean canTranspose(Player player, Location from, Location to, boolean teleport) {
        com.sk89q.worldedit.util.Location fromLocation = BukkitAdapter.adapt(from);
        com.sk89q.worldedit.util.Location toLocation = BukkitAdapter.adapt(to);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        StateFlag exitFlag = teleport ? bountyTeleportExit : bountyExit;
        StateFlag entryFlag = teleport ? bountyTeleportEntry : bountyEntry;
        boolean denyExit = !query.testState(fromLocation, localPlayer, exitFlag) && query.testState(toLocation, localPlayer, exitFlag);
        boolean denyEntry = !query.testState(toLocation, localPlayer, entryFlag) && query.testState(fromLocation, localPlayer, entryFlag);
        if (!lastMessages.containsKey(player.getUniqueId()) || System.currentTimeMillis() - lastMessages.get(player.getUniqueId()) > MESSAGE_INTERVAL_MS) {
            if (denyEntry) {
                player.sendMessage(LanguageOptions.parse(LanguageOptions.getMessage("deny-entry"), player));
                lastMessages.put(player.getUniqueId(), System.currentTimeMillis());
            } else if (denyExit) {
                player.sendMessage(LanguageOptions.parse(LanguageOptions.getMessage("deny-exit"), player));
                lastMessages.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
        return !denyExit && !denyEntry;
    }

    public static boolean canClaim(Player player, Location claimLocation) {
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
    public static int getCombatLogOverride(Player player, Location logutLocation) {
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
    public static int getPVPRuleOverride(Player player, Location pvpLocation) {
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

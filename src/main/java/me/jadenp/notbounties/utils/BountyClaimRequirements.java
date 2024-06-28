package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.utils.externalAPIs.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;

public class BountyClaimRequirements {
    /**
     * KingdomsX
     */
    public static boolean kingdomsXEnabled;
    private static boolean kingdomsXNation;
    private static boolean kingdomsXNationAllies;
    private static boolean kingdomsXKingdom;
    private static boolean kingdomsXKingdomAllies;
    /**
     * Towny Advanced
     */
    public static boolean townyAdvancedEnabled;
    private static boolean townyNation;
    private static boolean townyTown;
    private static boolean townyAllies;
    /**
     * BetterTeams
     */
    public static boolean betterTeamsEnabled;
    private static boolean btClaim;
    private static boolean btAllies;
    /**
     * PlaceholderAPI
     */
    private static String teamsPlaceholder;
    /**
     * Vanilla Minecraft
     */
    private static boolean scoreboardTeamClaim;
    /**
     * Lands
     */
    public static boolean landsEnabled;
    private static boolean landsNation;
    private static boolean landsNationAllies;
    private static boolean landsLand;
    private static boolean landsLandAllies;
    /**
     * WorldGuard
     */
    public static boolean worldGuardEnabled;
    public static void loadConfiguration(ConfigurationSection configuration) {
        kingdomsXEnabled = Bukkit.getPluginManager().isPluginEnabled("Kingdoms");
        betterTeamsEnabled = Bukkit.getPluginManager().isPluginEnabled("BetterTeams");
        townyAdvancedEnabled = Bukkit.getPluginManager().isPluginEnabled("Towny");
        landsEnabled = Bukkit.getPluginManager().isPluginEnabled("Lands");
        worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");

        kingdomsXNation = configuration.getBoolean("kingdoms-x.nation");
        kingdomsXNationAllies = configuration.getBoolean("kingdoms-x.nation-ally");
        kingdomsXKingdom = configuration.getBoolean("kingdoms-x.kingdom");
        kingdomsXKingdomAllies = configuration.getBoolean("kingdoms-x.kingdom-ally");
        townyNation = configuration.getBoolean("towny-advanced.nation");
        townyTown = configuration.getBoolean("towny-advanced.town");
        townyAllies = configuration.getBoolean("towny-advanced.ally");
        scoreboardTeamClaim = configuration.getBoolean("scoreboard-claim");
        btClaim = configuration.getBoolean("better-teams.team");
        btAllies = configuration.getBoolean("better-teams.ally");
        teamsPlaceholder = configuration.getString("placeholder");
        landsNation = configuration.getBoolean("lands.nation");
        landsNationAllies = configuration.getBoolean("lands.nation-ally");
        landsLand = configuration.getBoolean("lands.land");
        landsLandAllies = configuration.getBoolean("lands.land-ally");

    }
    
    public static boolean canClaim(Player player, Player killer) {
        // check world filter
        if ((worldFilter && !worldFilterNames.contains(player.getWorld().getName()) || (!worldFilter && worldFilterNames.contains(player.getWorld().getName()))))
            return false;
        // check for teams
        if (betterTeamsEnabled) {
            BetterTeamsClass betterTeamsClass = new BetterTeamsClass();
            if ((!btClaim && betterTeamsClass.onSameTeam(player, killer)) || (!btAllies && betterTeamsClass.areAllies(player, killer)))
                return false;
        }
        if (townyAdvancedEnabled) {
            TownyAdvancedClass townyAdvancedClass = new TownyAdvancedClass();
            if ((!townyNation && townyAdvancedClass.inSameNation(player, killer)) || (!townyTown && townyAdvancedClass.inSameTown(player, killer)) || (!townyAllies && townyAdvancedClass.areNationsAllied(player, killer)))
                return false;
        }
        if (kingdomsXEnabled) {
            KingdomsXClass kingdomsXClass = new KingdomsXClass();
            if ((!kingdomsXNation && kingdomsXClass.inSameNation(player, killer)) || (!kingdomsXKingdom && kingdomsXClass.inSameKingdom(player,killer)) || (!kingdomsXNationAllies && kingdomsXClass.getNationRelation(player, killer) == 2) || (!kingdomsXKingdomAllies && kingdomsXClass.getKingdomRelation(player, killer) == 2))
                return false;
        }
        if (landsEnabled) {
            LandsClass landsClass = new LandsClass();
            if ((!landsNation && landsClass.shareNation(player, killer)) || (!landsLand && landsClass.shareLand(player,killer)) || (!landsNationAllies && landsClass.areNationsAllied(player, killer)) || (!landsLandAllies && landsClass.areLandsAllied(player, killer)) || !landsClass.canClaim(killer, player.getLocation()))
                return false;
        }
        if (!scoreboardTeamClaim && Bukkit.getScoreboardManager() != null) {
                Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
                for (Team team : sb.getTeams()) {
                    if (team.hasEntry(player.getDisplayName()) && team.hasEntry(killer.getDisplayName()))
                        return false;
                }
            }

        if (papiEnabled && !teamsPlaceholder.isEmpty()) {
            PlaceholderAPIClass placeholderAPIClass = new PlaceholderAPIClass();
            if (placeholderAPIClass.parse(player, teamsPlaceholder).equals(placeholderAPIClass.parse(killer, teamsPlaceholder)))
                return false;
        }
        if (worldGuardEnabled) {
            WorldGuardClass worldGuardClass = new WorldGuardClass();
            if (!worldGuardClass.canClaim(killer, player.getLocation()))
                return false;
        }

        return true;
    }
}

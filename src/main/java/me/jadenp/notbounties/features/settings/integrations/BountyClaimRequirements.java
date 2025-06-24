package me.jadenp.notbounties.features.settings.integrations;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.integrations.external_api.*;
import me.jadenp.notbounties.features.settings.integrations.external_api.worldguard.WorldGuardClass;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.List;



public class BountyClaimRequirements {
    /**
     * KingdomsX
     */
    private static boolean kingdomsXEnabled;
    private static boolean kingdomsXNation;
    private static boolean kingdomsXNationAllies;
    private static boolean kingdomsXKingdom;
    private static boolean kingdomsXKingdomAllies;
    /**
     * Towny Advanced
     */
    private static boolean townyAdvancedEnabled;
    private static boolean townyNation;
    private static boolean townyTown;
    private static boolean townyAllies;
    /**
     * BetterTeams
     */
    private static boolean betterTeamsEnabled;
    private static boolean btClaim;
    private static boolean btAllies;
    /**
     * PlaceholderAPI
     */
    private static List<String> teamsPlaceholder;
    /**
     * Vanilla Minecraft
     */
    private static boolean scoreboardTeamClaim;
    /**
     * Lands
     */
    private static boolean landsEnabled;
    private static boolean landsNation;
    private static boolean landsNationAllies;
    private static boolean landsLand;
    private static boolean landsLandAllies;
    /**
     * Factions - SabreFactions or FactionsUUID
     */
    private static boolean factionsEnabled;
    private static boolean factionsFaction;
    private static boolean factionsAlly;
    /**
     * SuperiorSkyblock2
     */
    private static boolean superiorSkyblockEnabled;
    private static boolean superiorSkyblockIslandMember;

    /**
     * SimpleClans
     */
    private static boolean simpleClansEnabled;
    private static boolean simpleClansClan;
    private static boolean simpleClansAlly;

    /**
     * Minecraft
     */
    private static int minRespawnDistance;
    private static boolean sameIPClaim;

    public static void loadConfiguration(ConfigurationSection configuration) {
        kingdomsXEnabled = Bukkit.getPluginManager().isPluginEnabled("Kingdoms");
        betterTeamsEnabled = Bukkit.getPluginManager().isPluginEnabled("BetterTeams");
        townyAdvancedEnabled = Bukkit.getPluginManager().isPluginEnabled("Towny");
        landsEnabled = Bukkit.getPluginManager().isPluginEnabled("Lands");
        factionsEnabled = Bukkit.getPluginManager().isPluginEnabled("Factions");
        superiorSkyblockEnabled = Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2");
        simpleClansEnabled = Bukkit.getPluginManager().isPluginEnabled("SimpleClans");

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
        if (configuration.isList("placeholder"))
            teamsPlaceholder = configuration.getStringList("placeholder");
        else
            teamsPlaceholder = Collections.singletonList(configuration.getString("placeholder"));
        landsNation = configuration.getBoolean("lands.nation");
        landsNationAllies = configuration.getBoolean("lands.nation-ally");
        landsLand = configuration.getBoolean("lands.land");
        landsLandAllies = configuration.getBoolean("lands.land-ally");
        factionsFaction = configuration.getBoolean("saber-factions.faction");
        factionsAlly = configuration.getBoolean("saber-factions.ally");
        superiorSkyblockIslandMember = configuration.getBoolean("superior-skyblock-2.island-member");
        simpleClansClan = configuration.getBoolean("simple-clans.clan");
        simpleClansAlly = configuration.getBoolean("simple-clans.ally");
        minRespawnDistance = configuration.getInt("min-respawn-distance");
        sameIPClaim = configuration.getBoolean("same-ip-claim");
    }
    
    public static boolean canClaim(Player player, Player killer) {
        // check world filter
        if ((ConfigOptions.isWorldFilter() && !ConfigOptions.getWorldFilterNames().contains(player.getWorld().getName()) || (!ConfigOptions.isWorldFilter() && ConfigOptions.getWorldFilterNames().contains(player.getWorld().getName())))) {
            NotBounties.debugMessage("Bounty claim canceled due to a filtered world.", false);
            return false;
        }
        // check for teams
        if (betterTeamsEnabled) {
            BetterTeamsClass betterTeamsClass = new BetterTeamsClass();
            if ((!btClaim && betterTeamsClass.onSameTeam(player, killer)) || (!btAllies && betterTeamsClass.areAllies(player, killer))) {
                NotBounties.debugMessage("Bounty claim canceled due to a BetterTeams team.", false);
                return false;
            }
        }
        if (townyAdvancedEnabled) {
            TownyAdvancedClass townyAdvancedClass = new TownyAdvancedClass();
            if ((!townyNation && townyAdvancedClass.inSameNation(player, killer)) || (!townyTown && townyAdvancedClass.inSameTown(player, killer)) || (!townyAllies && townyAdvancedClass.areNationsAllied(player, killer))) {
                NotBounties.debugMessage("Bounty claim canceled due to a TownyAdvanced town.", false);
                return false;
            }
        }
        if (kingdomsXEnabled) {
            KingdomsXClass kingdomsXClass = new KingdomsXClass();
            if ((!kingdomsXNation && kingdomsXClass.inSameNation(player, killer)) || (!kingdomsXKingdom && kingdomsXClass.inSameKingdom(player,killer)) || (!kingdomsXNationAllies && kingdomsXClass.getNationRelation(player, killer) == 2) || (!kingdomsXKingdomAllies && kingdomsXClass.getKingdomRelation(player, killer) == 2)) {
                NotBounties.debugMessage("Bounty claim canceled due to a KingdomsX kingdom.", false);
                return false;
            }
        }
        if (landsEnabled) {
            LandsClass landsClass = new LandsClass();
            if ((!landsNation && landsClass.shareNation(player, killer)) || (!landsLand && landsClass.shareLand(player,killer)) || (!landsNationAllies && landsClass.areNationsAllied(player, killer)) || (!landsLandAllies && landsClass.areLandsAllied(player, killer)) || !landsClass.canClaim(killer, player.getLocation())) {
                NotBounties.debugMessage("Bounty claim canceled due to a Lands plugin land.", false);
                return false;
            }
        }
        if (!scoreboardTeamClaim && Bukkit.getScoreboardManager() != null) {
                Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
                for (Team team : sb.getTeams()) {
                    if (team.hasEntry(player.getDisplayName()) && team.hasEntry(killer.getDisplayName())) {
                        NotBounties.debugMessage("Bounty claim canceled due to a scoreboard team.", false);
                        return false;
                    }
                }
            }
        // check for any matching placeholders
        if (ConfigOptions.getIntegrations().isPapiEnabled() && !teamsPlaceholder.isEmpty()) {
            PlaceholderAPIClass placeholderAPIClass = new PlaceholderAPIClass();
            for (String placeholder : teamsPlaceholder) {
                if (!placeholder.isEmpty() && placeholderAPIClass.parse(player, placeholder).equals(placeholderAPIClass.parse(killer, placeholder))) {
                    NotBounties.debugMessage("Bounty claim canceled due to a PlaceholderAPI placeholder.", false);
                    return false;
                }
            }
        }
        if (ConfigOptions.getIntegrations().isWorldGuardEnabled()) {
            if (!WorldGuardClass.canClaim(killer, player.getLocation())) {
                NotBounties.debugMessage("Bounty claim canceled due to a WorldGuard flag.", false);
                return false;
            }
        }
        if (factionsEnabled) {
            try {
                SaberFactionsClass sabreFactionsClass = new SaberFactionsClass();
                if ((!factionsFaction && sabreFactionsClass.inSameFaction(player, killer)) || (!factionsAlly && sabreFactionsClass.areFactionsAllied(player, killer))) {
                    NotBounties.debugMessage("Bounty claim canceled due to a SaberFaction faction.", false);
                    return false;
                }
            } catch (NoSuchMethodError ignored) {
                // not using this specific factions plugin
            }
            try {
                FactionsUUIDClass factionsUUIDClass = new FactionsUUIDClass();
                if ((!factionsFaction && factionsUUIDClass.inSameFaction(player, killer)) || (!factionsAlly && factionsUUIDClass.areFactionsAllied(player, killer))) {
                    NotBounties.debugMessage("Bounty claim canceled due to a FactionsUUID faction.", false);
                    return false;
                }
            } catch (NoSuchMethodError ignored) {
                // not using this specific factions plugin
            }
        }
        if (superiorSkyblockEnabled && !superiorSkyblockIslandMember && SuperiorSkyblockClass.onSameIsland(player, killer)) {
            NotBounties.debugMessage("Bounty claim canceled due to a SuperiorSkyblock2 island.", false);
            return false;
        }
        // check if the player is in a duel
        if (ConfigOptions.getIntegrations().isDuelsEnabled()
                && ConfigOptions.getIntegrations().getDuels().isEnabled()
                && ConfigOptions.getIntegrations().getDuels().isClaimBounties()
                && ConfigOptions.getIntegrations().getDuels().isInDuel(killer)) {
            NotBounties.debugMessage("Bounty claim canceled due to a Duels plugin duel.", false);
            return false;
        }
        // check if players are in the same clan or are allied
        if (simpleClansEnabled
                && ((!simpleClansClan && SimpleClansClass.inSameClan(player, killer))
                    || (!simpleClansAlly && SimpleClansClass.inAlliedClan(player, killer)))) {
            NotBounties.debugMessage("Bounty claim canceled due to a SimpleClans clan.", false);
            return false;
        }
        // check IP
        if (!sameIPClaim && player.getAddress() != null && killer.getAddress() != null && player.getAddress().getAddress().getHostAddress().equals(killer.getAddress().getAddress().getHostAddress())) {
            NotBounties.debugMessage("Bounty claim canceled due to a matching IP address.", false);
            return false;
        }
        // check respawn distance
        if (minRespawnDistance >= 0
                && player.getRespawnLocation() != null
                && player.getRespawnLocation().getWorld() != null
                && killer.getRespawnLocation() != null
                && player.getRespawnLocation().getWorld().equals(killer.getRespawnLocation().getWorld())
                && player.getRespawnLocation().distance(killer.getRespawnLocation()) < minRespawnDistance) {
            NotBounties.debugMessage("Bounty claim canceled due to a close respawn distance.", false);
            return false;
        }

        return true;
    }

    public static boolean isBetterTeamsEnabled() {
        return betterTeamsEnabled;
    }

    public static boolean isFactionsEnabled() {
        return factionsEnabled;
    }

    public static boolean isKingdomsXEnabled() {
        return kingdomsXEnabled;
    }

    public static boolean isLandsEnabled() {
        return landsEnabled;
    }

    public static boolean isSuperiorSkyblockEnabled() {
        return superiorSkyblockEnabled;
    }

    public static boolean isTownyAdvancedEnabled() {
        return townyAdvancedEnabled;
    }

    public static boolean isSimpleClansEnabled() {
        return simpleClansEnabled;
    }
}

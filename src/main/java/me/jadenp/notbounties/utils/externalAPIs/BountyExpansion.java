package me.jadenp.notbounties.utils.externalAPIs;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;

public class BountyExpansion extends PlaceholderExpansion {


    public BountyExpansion(){

    }

    @Override
    public @NotNull String getAuthor() {
        return "Not_Jaden";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "notbounties";
    }

    @Override
    public @NotNull String getVersion() {
        return NotBounties.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    /**
     * Add "_formatted" to the end to add the currency prefix and suffix
     * Add "_full" to the end of leaderboard to add what the stat is about
     * Add "_value" to the e nd of leaderboard to get the raw value
     * <p>%notbounties_bounty%</p>
     * <p>%notbounties_(all/kills/claimed/deaths/set/immunity/current)%</p>
     * <p>%notbounties_top_[x]_(all/kills/claimed/deaths/set/immunity/current)%</p>
     * <p>%notbounties_wanted%</p>
     * <p>%notbounties_notification%</p>
     * <p>%notbounties_mode%</p>
     * <p>%notbounties_timed_bounty%</p>
     * @Depricated <p>%notbounties_bounties_claimed%</p>
     * <p>%notbounties_bounties_set%</p>
     * <p>%notbounties_bounties_received%</p>
     * <p>%notbounties_immunity_spent%</p>
     * <p>%notbounties_all_time_bounty%</p>
     * <p>%notbounties_currency_gained%</p>
     */

    @Override
    public String onRequest(OfflinePlayer player, String params){
        UUID uuid = player.getUniqueId();
        if (params.equalsIgnoreCase("timed_bounty")) {
            if (BountyManager.hasBounty(player) && TimedBounties.isMaxed(Objects.requireNonNull(BountyManager.getBounty(player)).getTotalBounty()))
                // maxed out, cant get any higher
                return "";
            long next = TimedBounties.getUntilNextBounty(player.getUniqueId());
            if (next == -1)
                return "";
            return LanguageOptions.formatTime(next);
        }
        if (params.equalsIgnoreCase("wanted")) {
            Bounty bounty = BountyManager.getBounty(player);
            if (bounty == null)
                return "";
            return getWantedDisplayText(player);
        }
        if (params.startsWith("bounty")){
            Bounty bounty = BountyManager.getBounty(player);
            if (bounty != null){
                if (params.endsWith("_formatted"))
                    return LanguageOptions.color(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalBounty()) + NumberFormatting.currencySuffix);
                return NumberFormatting.getValue(bounty.getTotalBounty());
            }
            return "0";
        }

        if (params.equalsIgnoreCase("bounties_claimed")){
            if (BountyManager.SQL.isConnected()){
                return String.valueOf(BountyManager.data.getClaimed(player.getUniqueId().toString()));
            }
            return String.valueOf(BountyManager.killBounties.get(uuid));
        }

        if (params.equalsIgnoreCase("bounties_set")){
            if (BountyManager.SQL.isConnected()){
                return String.valueOf(BountyManager.data.getSet(player.getUniqueId().toString()));
            }
            return String.valueOf(BountyManager.setBounties.get(uuid));
        }

        if (params.equalsIgnoreCase("bounties_received")){
            if (BountyManager.SQL.isConnected()){
                return String.valueOf(BountyManager.data.getReceived(player.getUniqueId().toString()));
            }
            return String.valueOf(BountyManager.deathBounties.get(uuid));
        }

        if (params.equalsIgnoreCase("immunity_spent")){
            if (BountyManager.SQL.isConnected()){
                return String.valueOf(BountyManager.data.getImmunity(player.getUniqueId().toString()));
            }
            return String.valueOf(BountyManager.immunitySpent.get(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("all_time_bounty")){
            if (BountyManager.SQL.isConnected()){
                return String.valueOf(BountyManager.data.getAllTime(player.getUniqueId().toString()));
            }
            return String.valueOf(BountyManager.allTimeBounties.get(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("currency_gained")){
            if (BountyManager.SQL.isConnected()){
                return String.valueOf(BountyManager.data.getTotalClaimed(player.getUniqueId().toString()));
            }
            return String.valueOf(BountyManager.allClaimedBounties.get(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("notification")) {
            if (NotBounties.disableBroadcast.contains(player.getUniqueId())) {
                return "false";
            }
            return "true";
        }

        if (params.equalsIgnoreCase("mode")) {
            if (!NotBounties.playerWhitelist.containsKey(player.getUniqueId()))
                return "Whitelist"; // Any
            Whitelist whitelist = NotBounties.playerWhitelist.get(player.getUniqueId());
            if (whitelist.isBlacklist())
                return "Blacklist";
            return "Whitelist";
        }

        int ending = 0;
        if (params.endsWith("_full")) {
            ending = 1;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.endsWith("_formatted")) {
            ending = 2;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.endsWith("_value")) {
            ending = 3;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.startsWith("top_")) {


            params = params.substring(4);
            int rank = 0;
            try {
                if (params.contains("_"))
                    rank = Integer.parseInt(params.substring(0,params.indexOf("_")));
                else
                    rank = Integer.parseInt(params);
            } catch (NumberFormatException ignored) {
            }
            if (rank < 1)
                rank = 1;
            Leaderboard leaderboard;
            if (!params.contains("_")) {
                leaderboard = Leaderboard.CURRENT;
            } else {
                params = params.substring(params.indexOf("_") + 1);
                try {
                    leaderboard = Leaderboard.valueOf(params.toUpperCase());
                } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                    return null;
                }
            }
            LinkedHashMap<UUID, Double> stat = leaderboard.getTop(rank - 1, 1);
            if (stat.isEmpty())
                return "0";
            boolean useCurrency = leaderboard == Leaderboard.IMMUNITY || leaderboard == Leaderboard.CLAIMED || leaderboard == Leaderboard.ALL || leaderboard == Leaderboard.CURRENT;
            Map.Entry<UUID, Double> entry = stat.entrySet().iterator().next();
            double amount = entry.getValue();
            UUID uuid1 = entry.getKey();
            String name = NotBounties.getPlayerName(uuid1);
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid1);
            if (ending == 1)
                return LanguageOptions.parse(leaderboard.getStatMsg(true).replaceAll("\\{amount}", Matcher.quoteReplacement(leaderboard.getFormattedStat(uuid1))), p);
            if (ending == 2)
                return LanguageOptions.parse(leaderboard.getFormattedStat(uuid1),p);
            if (ending == 3)
                return NumberFormatting.getValue(leaderboard.getStat(uuid1));
            return Leaderboard.parseBountyTopString(rank, name, amount, useCurrency, p);
        }

        String value = params.contains("_") ? params.substring(0, params.indexOf("_")) : params;

        try {
            Leaderboard leaderboard = Leaderboard.valueOf(value.toUpperCase());
            if (ending == 1)
                return LanguageOptions.parse(leaderboard.getStatMsg(true).replaceAll("\\{amount}", Matcher.quoteReplacement(leaderboard.getFormattedStat(player.getUniqueId()))), player);
            if (ending == 2)
                return LanguageOptions.parse(leaderboard.getFormattedStat(player.getUniqueId()),player);
            if (ending == 3)
                return NumberFormatting.getValue(leaderboard.getStat(player.getUniqueId()));
            return NumberFormatting.formatNumber(leaderboard.getStat(player.getUniqueId()));
        } catch (IllegalArgumentException ignored){}

        return null;
    }
}
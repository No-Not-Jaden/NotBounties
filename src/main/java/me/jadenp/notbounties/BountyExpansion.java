package me.jadenp.notbounties;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.jadenp.notbounties.utils.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static me.jadenp.notbounties.utils.ConfigOptions.*;

public class BountyExpansion extends PlaceholderExpansion {

    private final NotBounties notBounties;

    public BountyExpansion(NotBounties notBounties){
        this.notBounties = notBounties;
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
        return notBounties.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    /**
     * Add "_formatted" to the end to add the currency prefix and suffix
     * Add "_full" to the end of leaderboard to add what the stat is about
     * <p>%notbounties_bounty%</p>
     * <p>%notbounties_(all/kills/claimed/deaths/set/immunity)%</p>
     * <p>%notbounties_top_[x]_(all/kills/claimed/deaths/set/immunity)%</p>
     * @Depricated <p>%notbounties_bounties_claimed%</p>
     * <p>%notbounties_bounties_set%</p>
     * <p>%notbounties_bounties_received%</p>
     * <p>%notbounties_immunity_spent%</p>
     * <p>%notbounties_all_time_bounty%</p>
     * <p>%notbounties_currency_gained%</p>
     */

    @Override
    public String onRequest(OfflinePlayer player, String params){
        String uuid = player.getUniqueId().toString();
        if (params.startsWith("bounty")){
            Bounty bounty = notBounties.SQL.isConnected() ? notBounties.data.getBounty(player.getUniqueId()) : notBounties.getBounty(player);
            if (bounty != null){
                if (params.endsWith("_formatted"))
                    return color(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalBounty()) + NumberFormatting.currencySuffix);
                return String.valueOf(bounty.getTotalBounty());
            }
            return "0";
        }

        if (params.equalsIgnoreCase("bounties_claimed")){
            if (notBounties.SQL.isConnected()){
                return String.valueOf(notBounties.data.getClaimed(player.getUniqueId().toString()));
            }
            return String.valueOf(notBounties.killBounties.get(uuid));
        }

        if (params.equalsIgnoreCase("bounties_set")){
            if (notBounties.SQL.isConnected()){
                return String.valueOf(notBounties.data.getSet(player.getUniqueId().toString()));
            }
            return String.valueOf(notBounties.setBounties.get(uuid));
        }

        if (params.equalsIgnoreCase("bounties_received")){
            if (notBounties.SQL.isConnected()){
                return String.valueOf(notBounties.data.getReceived(player.getUniqueId().toString()));
            }
            return String.valueOf(notBounties.deathBounties.get(uuid));
        }

        if (params.equalsIgnoreCase("immunity_spent")){
            if (notBounties.SQL.isConnected()){
                return String.valueOf(notBounties.data.getImmunity(player.getUniqueId().toString()));
            }
            return String.valueOf(notBounties.immunitySpent.get(player.getUniqueId().toString()));
        }

        if (params.equalsIgnoreCase("all_time_bounty")){
            if (notBounties.SQL.isConnected()){
                return String.valueOf(notBounties.data.getAllTime(player.getUniqueId().toString()));
            }
            return String.valueOf(notBounties.allTimeBounties.get(player.getUniqueId().toString()));
        }

        if (params.equalsIgnoreCase("currency_gained")){
            if (notBounties.SQL.isConnected()){
                return String.valueOf(notBounties.data.getTotalClaimed(player.getUniqueId().toString()));
            }
            return String.valueOf(notBounties.allClaimedBounties.get(player.getUniqueId().toString()));
        }

        if (params.startsWith("top_")) {
            params = params.substring(4);
            int rank = 0;
            try {
                rank = Integer.parseInt(params.substring(params.indexOf("_") + 1));
            } catch (NumberFormatException ignored) {
            }
            if (rank < 1)
                rank = 1;
            Leaderboard leaderboard;
            try {
                leaderboard = Leaderboard.valueOf(params.substring(0, params.indexOf("_")).toUpperCase());
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                return null;
            }
            LinkedHashMap<String, Double> stat = leaderboard.getTop(rank - 1, 1);
            if (stat.size() == 0)
                return "";
            boolean useCurrency = leaderboard == Leaderboard.IMMUNITY || leaderboard == Leaderboard.CLAIMED || leaderboard == Leaderboard.ALL;
            Map.Entry<String, Double> entry = stat.entrySet().iterator().next();
            double amount = entry.getValue();
            UUID uuid1 = UUID.fromString(entry.getKey());
            String name = NotBounties.getInstance().getPlayerName(uuid1);
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid1);
            if (params.endsWith("_full"))
                return leaderboard.getStatMsg(true);
            if (params.endsWith("_formatted"))
                return color(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(leaderboard.getStat(player.getUniqueId())) + NumberFormatting.currencySuffix);
            return Leaderboard.parseBountyTopString(rank, name, amount, useCurrency, p);
        }

        String value = params.contains("_") ? params.substring(0, params.indexOf("_")) : params;

        try {
            Leaderboard leaderboard = Leaderboard.valueOf(value.toUpperCase());
            if (params.endsWith("_full"))
                return leaderboard.getStatMsg(true);
            if (params.endsWith("_formatted"))
                return color(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(leaderboard.getStat(player.getUniqueId())) + NumberFormatting.currencySuffix);
            return NumberFormatting.formatNumber(leaderboard.getStat(player.getUniqueId()));
        } catch (IllegalArgumentException ignored){}

        return null;
    }
}
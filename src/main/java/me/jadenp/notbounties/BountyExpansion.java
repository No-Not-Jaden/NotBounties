package me.jadenp.notbounties;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationOptions;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.jadenp.notbounties.Commands.sortByValue;
import static me.jadenp.notbounties.ConfigOptions.*;
import static me.jadenp.notbounties.ConfigOptions.currencySuffix;

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

    @Override
    public String onRequest(OfflinePlayer player, String params){
        String uuid = player.getUniqueId().toString();
        if (params.equalsIgnoreCase("bounty")){
            Bounty bounty = notBounties.SQL.isConnected() ? notBounties.data.getBounty(player.getUniqueId().toString()) : notBounties.getBounty(player);
            if (bounty != null){
                return bounty.getTotalBounty() + "";
            }
            return "0";
        }

        if (params.equalsIgnoreCase("bounties_claimed")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getClaimed(player.getUniqueId().toString()) + "";
            }
            return notBounties.bountiesClaimed.get(uuid) + "";
        }

        if (params.equalsIgnoreCase("bounties_set")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getSet(player.getUniqueId().toString()) + "";
            }
            return notBounties.bountiesSet.get(uuid) + "";
        }

        if (params.equalsIgnoreCase("bounties_received")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getReceived(player.getUniqueId().toString()) + "";
            }
            return notBounties.bountiesReceived.get(uuid) + "";
        }

        if (params.equalsIgnoreCase("immunity_spent")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getImmunity(player.getUniqueId().toString()) + "";
            }
            return notBounties.immunitySpent.get(player.getUniqueId().toString()) + "";
        }

        if (params.equalsIgnoreCase("all_time_bounty")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getAllTime(player.getUniqueId().toString()) + "";
            }
            return notBounties.allTimeBounty.get(player.getUniqueId().toString()) + "";
        }

        if (params.equalsIgnoreCase("currency_gained")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getTotalClaimed(player.getUniqueId().toString()) + "";
            }
            return notBounties.allClaimed.get(player.getUniqueId().toString()) + "";
        }
        // im just reusing code for this next part, but there is definitely a better way to do this
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
            LinkedHashMap<String, Integer> stat = leaderboard.getTop(rank - 1, 1);
            if (stat.size() == 0)
                return "";
            boolean useCurrency = leaderboard == Leaderboard.IMMUNITY || leaderboard == Leaderboard.CLAIMED || leaderboard == Leaderboard.ALL;
            Map.Entry<String, Integer> entry = stat.entrySet().iterator().next();
            int amount = entry.getValue();
            OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
            String name = p.getName();
            if (name == null && notBounties.loggedPlayers.containsValue(entry.getKey())) {
                for (Map.Entry<String, String> logged : notBounties.loggedPlayers.entrySet()) {
                    if (logged.getValue().equals(entry.getKey())) {
                        name = logged.getKey();
                        break;
                    }
                    name = "???";
                }
            } else {
                name = "???";
            }
            return Leaderboard.parseBountyTopString(rank, name, amount, useCurrency, p);
        }

        return null;
    }
}
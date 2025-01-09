package me.jadenp.notbounties.utils.external_api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.WantedTags;
import me.jadenp.notbounties.utils.configuration.auto_bounties.TimedBounties;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.data.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BountyExpansion extends PlaceholderExpansion {

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
     * Add "_value" to the end of leaderboard to get the raw value
     * Add "_name" to the end of top placeholder to get the name of the player in that position
     * <p>%notbounties_bounty%</p>
     * <p>%notbounties_total%</p>
     * <p>%notbounties_(all/kills/claimed/deaths/set/immunity/current)%</p>
     * <p>%notbounties_top_[x]_(all/kills/claimed/deaths/set/immunity/current)%</p>
     * <p>%notbounties_wanted%</p> Wanted tag
     * <p>%notbounties_notification%</p> Bounty broadcast -> True/False
     * <p>%notbounties_mode%</p> Whitelist/Blacklist
     * <p>%notbounties_timed_bounty%</p>
     * <p>%notbounties_challenge_[x/time]%</p>
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
            if (BountyManager.hasBounty(uuid) && TimedBounties.isMaxed(Objects.requireNonNull(BountyManager.getBounty(uuid)).getTotalDisplayBounty()))
                // maxed out, cant get any higher
                return "";
            long next = TimedBounties.getUntilNextBounty(player.getUniqueId());
            if (next == -1)
                return "";
            return LocalTime.formatTime(next, LocalTime.TimeFormat.RELATIVE);
        }
        if (params.equalsIgnoreCase("wanted")) {
            Bounty bounty = BountyManager.getBounty(uuid);
            if (bounty == null)
                return "";
            return WantedTags.getWantedDisplayText(player);
        }
        if (params.startsWith("bounty")){
            Bounty bounty = BountyManager.getBounty(uuid);
            if (bounty != null){
                if (params.endsWith("_formatted"))
                    return LanguageOptions.color(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalDisplayBounty()) + NumberFormatting.currencySuffix);
                return NumberFormatting.getValue(bounty.getTotalDisplayBounty());
            }
            return "0";
        }
        if (params.startsWith("challenge")) {
            if (params.length() < 11)
                return ChallengeManager.getTimeLeft();
            params = params.substring(10);
            if (params.equalsIgnoreCase("time"))
                return ChallengeManager.getTimeLeft();
            try {
                int index = (int) NumberFormatting.tryParse(params);
                return ChallengeManager.getChallengeTitle(player, index);
            } catch (NumberFormatException e) {
                return "Placeholder Error";
            }
        }
        if (params.startsWith("total")) {
            if (params.equalsIgnoreCase("total")) {
                int bounties = BountyManager.getAllBounties(-1).size();
                return NumberFormatting.formatNumber(bounties);
            } else if (params.equalsIgnoreCase("total_unique")) {
                List<Bounty> bounties = BountyManager.getAllBounties(-1);
                List<UUID> counted = new ArrayList<>();
                for (Bounty bounty : bounties) {
                    for (Setter setter : bounty.getSetters()) {
                        if (!counted.contains(setter.getUuid()))
                            counted.add(setter.getUuid());
                    }
                }
                return NumberFormatting.formatNumber(counted.size());
            }
        }

        if (params.equalsIgnoreCase("bounties_claimed")){
            return String.valueOf(Leaderboard.KILLS.getStat(uuid));
        }

        if (params.equalsIgnoreCase("bounties_set")){
            return String.valueOf(Leaderboard.SET.getStat(uuid));
        }

        if (params.equalsIgnoreCase("bounties_received")){
            return String.valueOf(Leaderboard.DEATHS.getStat(uuid));
        }

        if (params.equalsIgnoreCase("immunity_spent")){
            return String.valueOf(Leaderboard.IMMUNITY.getStat(uuid));
        }

        if (params.equalsIgnoreCase("all_time_bounty")){
            return String.valueOf(Leaderboard.ALL.getStat(uuid));
        }

        if (params.equalsIgnoreCase("currency_gained")){
            return String.valueOf(Leaderboard.CLAIMED.getStat(uuid));
        }

        if (params.equalsIgnoreCase("notification")) {
            return DataManager.getPlayerData(player.getUniqueId()).isDisableBroadcast() + "";
        }

        if (params.equalsIgnoreCase("mode")) {
            Whitelist whitelist = DataManager.getPlayerData(player.getUniqueId()).getWhitelist();
            return whitelist.isBlacklist() ? "Blacklist" : "Whitelist";
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
        if (params.endsWith("_name")) {
            ending = 4;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.endsWith("_rank")) {
            ending = 5;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.startsWith("top_")) {
            params = params.substring(4);
            int rank;
            try {
                if (params.contains("_"))
                    rank = Integer.parseInt(params.substring(0,params.indexOf("_")));
                else
                    rank = Integer.parseInt(params);
            } catch (NumberFormatException ignored) {
                rank = 0;
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
            Map<UUID, Double> stat = leaderboard.getTop(rank - 1, 1);
            if (stat.isEmpty())
                return "...";
            boolean useCurrency = leaderboard == Leaderboard.IMMUNITY || leaderboard == Leaderboard.CLAIMED || leaderboard == Leaderboard.ALL || leaderboard == Leaderboard.CURRENT;
            Map.Entry<UUID, Double> entry = stat.entrySet().iterator().next();
            double amount = entry.getValue();
            UUID uuid1 = entry.getKey();
            String name = LoggedPlayers.getPlayerName(uuid1);
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid1);
            if (ending == 1)
                return LanguageOptions.parse(leaderboard.getStatMsg(true).replace("{amount}", (leaderboard.getFormattedStat(uuid1))), p);
            if (ending == 2)
                return LanguageOptions.parse(leaderboard.getFormattedStat(uuid1),p);
            if (ending == 3)
                return NumberFormatting.getValue(leaderboard.getStat(uuid1));
            if (ending == 4) {
                return name;
            }
            return Leaderboard.parseBountyTopString(rank, name, amount, useCurrency, p);
        }

        String value = params.contains("_") ? params.substring(0, params.indexOf("_")) : params;

        try {
            Leaderboard leaderboard = Leaderboard.valueOf(value.toUpperCase());
            if (ending == 1)
                return LanguageOptions.parse(leaderboard.getStatMsg(true).replace("{amount}", (leaderboard.getFormattedStat(player.getUniqueId()))), player);
            if (ending == 2)
                return LanguageOptions.parse(leaderboard.getFormattedStat(player.getUniqueId()),player);
            if (ending == 3)
                return NumberFormatting.getValue(leaderboard.getStat(player.getUniqueId()));
            if (ending == 5) {
                return NumberFormatting.formatNumber(leaderboard.getRank(player.getUniqueId()));
            }
            return NumberFormatting.formatNumber(leaderboard.getStat(player.getUniqueId()));
        } catch (IllegalArgumentException ignored){
            // not a valid leaderboard
        }

        return null;
    }
}
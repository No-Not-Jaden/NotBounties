package me.jadenp.notbounties;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.databases.BountySortType;
import me.jadenp.notbounties.features.settings.databases.StatSortType;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.features.LanguageOptions.*;

public enum Leaderboard {
    //(all/kills/claimed/deaths/set/immunity)
    ALL(true, "b_all_time"),
    KILLS(false, "b_claimed"),
    CLAIMED(true, "b_claim_amt"),
    DEATHS(false, "b_received"),
    SET(false, "b_set"),
    IMMUNITY(true, "immunity"),
    CURRENT(true, "display"); // not an actual column name

    private final boolean money;
    private final String columnName;
    Leaderboard(boolean decimals, String columnName){
        this.money = decimals;
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isMoney() {
        return money;
    }

    /**
     * Gets the stat from either local storage or the database if connected
     * @param uuid UUID of the player
     * @return stat
     */
    public CompletableFuture<Double> getStat(UUID uuid){
        return DataManager.getStatAsync(uuid, this);
    }

    /**
     * Correctly displays the player's stat
     *
     * @param shorten if the message is in a shortened form
     * @param player Player to display to
     */
    public void displayStats(OfflinePlayer player, boolean shorten){
        String msg = parseStats(getPrefix() + getStatMsg(shorten), player);
        if (player.isOnline()) {
            Player p = player.getPlayer();
            assert p != null;
            p.sendMessage(msg);
        }

    }

    public void displayStats(OfflinePlayer statOwner, OfflinePlayer receiver, boolean shorten) {
        String msg = parseStats(getPrefix() + getStatMsg(shorten), statOwner);
        if (receiver.isOnline()) {
            Player p = receiver.getPlayer();
            assert p != null;
            p.sendMessage(msg);
        }
    }

    /**
     * Formats the stat for display. This will return the formatted numeric value with currency prefix/suffix if necessary.
     * @param uuid UUID of the player.
     * @return The player's formatted stat.
     */
    public CompletableFuture<String> getFormattedStat(UUID uuid){
        if (money) {
            return getStat(uuid).thenApply(stat -> {
                String amountString = NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(stat) + NumberFormatting.getCurrencySuffix();
                if (this == Leaderboard.IMMUNITY && ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.TIME) {
                    amountString = amountString + ChatColor.WHITE + " (" + LocalTime.formatTime(ImmunityManager.currencyToTime(stat), LocalTime.TimeFormat.RELATIVE) + ")";
                }
                return amountString;
            });

        }
        return getStat(uuid).thenApply(NumberFormatting::formatNumber);
    }

    public String getStatMsg(boolean shorten){
        return switch (this) {
            case ALL -> shorten ? getMessage("bounty-stat.all.short") : getMessage("bounty-stat.all.long");
            case KILLS -> shorten ? getMessage("bounty-stat.kills.short") : getMessage("bounty-stat.kills.long");
            case CLAIMED -> shorten ? getMessage("bounty-stat.claimed.short") : getMessage("bounty-stat.claimed.long");
            case DEATHS -> shorten ? getMessage("bounty-stat.deaths.short") : getMessage("bounty-stat.deaths.long");
            case SET -> shorten ? getMessage("bounty-stat.set.short") : getMessage("bounty-stat.set.long");
            case IMMUNITY -> shorten ? getMessage("bounty-stat.immunity.short") : getMessage("bounty-stat.immunity.long");
            case CURRENT -> shorten ? getMessage("list-total") : getMessage("check-bounty");
        };
    }

    /**
     * Gets the top stats of the leaderboard type in descending order
     * @param amount Amount of values you want returned
     * @return Map of UUID and stat value in descending order
     */
    public CompletableFuture<Map<UUID, Double>> getTop(int skip, int amount){

        if (this == Leaderboard.CURRENT) {
            return DataManager.getPublicBountiesAsync(BountySortType.HIGHEST, skip, amount)
                    .thenApply(bounties -> bounties.stream()
                            .filter(bounty -> bounty.getTotalDisplayBounty() > 0)
                            .collect(Collectors.toMap(Bounty::getUUID, Bounty::getTotalDisplayBounty, (a, b) -> b, LinkedHashMap::new)));
        } else {
            return DataManager.getPublicStatsAsync(this, StatSortType.HIGHEST, skip, amount).thenApply(map -> {
                LinkedHashMap<UUID, Double> top = new LinkedHashMap<>();
                for (Map.Entry<UUID, PlayerStat> entry : map.entrySet()){
                    top.put(entry.getKey(), entry.getValue().leaderboardType(this));
                }
                return top;
            });
        }
    }

    public CompletableFuture<Integer> getRank(UUID uuid){
        int rank = 1;
        if (this == Leaderboard.CURRENT) {
            for (Bounty bounty : BountyManager.getPublicBounties(2)) {
                if (bounty.getUUID().equals(uuid))
                    return rank;
                rank++;
            }
        } else {
            LinkedHashMap<UUID, Double> map = sortByValue(getStatMap());
            for (Map.Entry<UUID, Double> entry : map.entrySet()){
                String name = LoggedPlayers.getPlayerName(entry.getKey());
                if (ConfigOptions.getHiddenNames().contains(name))
                    continue;
                if (entry.getKey().equals(uuid))
                    return rank;
                rank++;
            }
        }
        return rank;
    }


    public void displayTopStat(CommandSender sender, int amount){
        if (sender instanceof Player player)
            sender.sendMessage(parse(getMessage("bounty-top-title"), player));
        else
            sender.sendMessage(parse(getMessage("bounty-top-title"), null));
        boolean useCurrency = this == Leaderboard.IMMUNITY || this == Leaderboard.CLAIMED || this == Leaderboard.ALL;
        getTop(0, amount).thenAccept(map -> {
            int i = 0;
            for (Map.Entry<UUID, Double> entry : map.entrySet()){
                OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
                String name = LoggedPlayers.getPlayerName(entry.getKey());
                sender.sendMessage(parseBountyTopString(i + 1, name, entry.getValue(), useCurrency, p));
                i++;
            }
            sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                   ");
        });

    }


    /**
     * Parse the text for the player, replacing {amount} for the formatted stat.
     * @apiNote This will load the stats of the player, so it should be used asynchronously.
     * @param text Text to format.
     * @param player Player to format the text for.
     * @return A formatted String.
     */
    private String parseStats(String text, OfflinePlayer player){
        text = text.replace("{amount}", (getFormattedStat(player.getUniqueId()).join()));

        return parse(text, player);
    }

    private static LinkedHashMap<UUID, Double> sortByValue(Map<UUID, Double> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<UUID, Double>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        // put data from sorted list to hashmap
        LinkedHashMap<UUID, Double> temp = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
    private static LinkedHashMap<UUID, Double> sortByName(Map<UUID, Double> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<UUID, Double>> list = new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (LoggedPlayers.getPlayerName(o2.getKey())).compareTo(LoggedPlayers.getPlayerName(o1.getKey())));

        // put data from sorted list to hashmap
        LinkedHashMap<UUID, Double> temp = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static String parseBountyTopString(int rank, @NotNull String playerName, double amount, boolean useCurrency, OfflinePlayer player){
        String text = getMessage("bounty-top");
        text = text.replace("{rank}", rank + "");
        text = text.replace("{player}", playerName).replace("{player_displayname}", LoggedPlayers.getDisplayName(player));
        if (useCurrency)
            text = text.replace("{amount}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(amount) + NumberFormatting.getCurrencySuffix()));
        else
            text = text.replace("{amount}", (NumberFormatting.formatNumber(amount)));

        return parse(text, player);
    }

    public CompletableFuture<Map<UUID, Double>> getSortedList(int skip, int amount, int sortType) {
        return getTop(skip, amount).thenApply(map -> {
            if (sortType == 2)
                return reverseMap(map);
            if (sortType == 3)
                return sortByName(map);
            if (sortType == 4)
                return reverseMap(sortByName(map));
            return map;
        });
    }

    public CompletableFuture<Map<UUID, String>> getFormattedList(int skip, int amount, int sortType) {
        return getSortedList(skip, amount, sortType).thenApply(map -> {
            LinkedHashMap<UUID, String> formattedList = new LinkedHashMap<>();
            for (Map.Entry<UUID, Double> entry : map.entrySet()){
                if (this.isMoney()) {
                    formattedList.put(entry.getKey(), NumberFormatting.getCurrencyPrefix() + NumberFormatting.getValue(entry.getValue()) + NumberFormatting.getCurrencySuffix());
                } else {
                    formattedList.put(entry.getKey(), NumberFormatting.getValue(entry.getValue()));
                }
            }
            return formattedList;
        });
    }

    private static LinkedHashMap<UUID, Double> reverseMap(Map<UUID, Double> map){
        LinkedHashMap<UUID, Double> newMap = new LinkedHashMap<>();
        for (UUID key : map.keySet().stream().toList().reversed()){
            newMap.put(key, map.get(key));
        }
        return newMap;
    }

    public String getDisplayName() {
        return LanguageOptions.getMessage("bounty-stat." + this.name().toLowerCase() + ".name");
    }

    @Override
    public String toString() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }
}

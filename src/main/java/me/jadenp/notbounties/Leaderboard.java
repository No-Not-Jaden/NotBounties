package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static me.jadenp.notbounties.api.BountyManager.*;

public enum Leaderboard {
    //(all/kills/claimed/deaths/set/immunity)
    ALL(true),
    KILLS(false),
    CLAIMED(true),
    DEATHS(false),
    SET(false),
    IMMUNITY(true),
    CURRENT(true);

    private final boolean money;
    Leaderboard(boolean decimals){
        this.money = decimals;
    }

    public boolean isMoney() {
        return money;
    }

    /**
     * Gets the stat from either local storage or the database if connected
     * @param uuid UUID of the player
     * @return stat
     */
    public double getStat(UUID uuid){
        switch (this) {
            case ALL:
                if (SQL.isConnected())
                    return data.getAllTime(uuid.toString());
                if (!allTimeBounties.containsKey(uuid.toString()))
                    return 0;
                return allTimeBounties.get(uuid.toString());
            case KILLS:
                if (SQL.isConnected())
                    return data.getClaimed(uuid.toString());
                if (!killBounties.containsKey(uuid.toString()))
                    return 0;
                return killBounties.get(uuid.toString());
            case CLAIMED:
                if (SQL.isConnected())
                    return data.getTotalClaimed(uuid.toString());
                if (!allClaimedBounties.containsKey(uuid.toString()))
                    return 0;
                return allClaimedBounties.get(uuid.toString());
            case DEATHS:
                if (SQL.isConnected())
                    return data.getReceived(uuid.toString());
                if (!deathBounties.containsKey(uuid.toString()))
                    return 0;
                return deathBounties.get(uuid.toString());
            case SET:
                if (SQL.isConnected())
                    return data.getSet(uuid.toString());
                if (!setBounties.containsKey(uuid.toString()))
                    return 0;
                return setBounties.get(uuid.toString());
            case IMMUNITY:
                if (SQL.isConnected())
                    return data.getImmunity(uuid.toString());
                if (!immunitySpent.containsKey(uuid.toString()))
                    return 0;
                return immunitySpent.get(uuid.toString());
            case CURRENT:
                if (SQL.isConnected())
                    return data.getBounty(uuid).getTotalBounty();
                Bounty bounty = getBounty(Bukkit.getOfflinePlayer(uuid));
                if (bounty == null)
                    return 0;
                return bounty.getTotalBounty();
            default:
                return 0;
        }
    }

    /**
     * Correctly displays the player's stat
     *
     * @param shorten if the message is in a shortened form
     * @param player Player to display to
     */
    public void displayStats(OfflinePlayer player, boolean shorten){
        String msg = parseStats(speakings.get(0) + getStatMsg(shorten), player);
        if (player.isOnline()) {
            Player p = player.getPlayer();
            assert p != null;
            p.sendMessage(msg);
        }

    }

    public String getFormattedStat(UUID uuid){
        if (money) {
            return NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(getStat(uuid)) + NumberFormatting.currencySuffix;
        }
        return NumberFormatting.formatNumber(getStat(uuid));
    }

    public String getStatMsg(boolean shorten){
        String msg = "";
        switch (this){
            case ALL:
                msg = shorten ? speakings.get(50) : speakings.get(44);
                break;
            case KILLS:
                msg = shorten ? speakings.get(51) : speakings.get(45);
                break;
            case CLAIMED:
                msg = shorten ? speakings.get(52) : speakings.get(46);
                break;
            case DEATHS:
                msg = shorten ? speakings.get(53) : speakings.get(47);
                break;
            case SET:
                msg = shorten ? speakings.get(54) : speakings.get(48);
                break;
            case IMMUNITY:
                msg = shorten ? speakings.get(55) : speakings.get(49);
                break;
            case CURRENT:
                msg = shorten ? speakings.get(11) : speakings.get(9);
                break;
        }
        //msg = msg.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + "{amount}" + NumberFormatting.currencySuffix));
        return msg;
    }

    /**
     * Gets the top stats of the leaderboard type in descending order
     * @param amount Amount of values you want returned
     * @return Map of UUID and stat value in descending order
     */
    public LinkedHashMap<String, Double> getTop(int skip, int amount){
        if (SQL.isConnected())
            return data.getTopStats(this, hiddenNames, skip, amount);
        LinkedHashMap<String, Double> map;
        switch (this){
            case ALL:
                map = sortByValue(allTimeBounties);
                break;
            case KILLS:
                map = sortByValue(killBounties);
                break;
            case CLAIMED:
                map = sortByValue(allClaimedBounties);
                break;
            case DEATHS:
                map = sortByValue(deathBounties);
                break;
            case SET:
                map = sortByValue(setBounties);
                break;
            case IMMUNITY:
                map = sortByValue(immunitySpent);
                break;
            case CURRENT:
                map = new LinkedHashMap<>();
                for (Bounty bounty : bountyList)
                    map.put(bounty.getUUID().toString(), bounty.getTotalBounty());
                map = sortByValue(map);
                break;
            default:
                map = new LinkedHashMap<>();
        }
        LinkedHashMap<String, Double> top = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : map.entrySet()){
            if (amount == 0)
                break;
            if (skip == 0) {
                amount--;
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                if (player.getName() != null && !hiddenNames.contains(player.getName()))
                    top.put(entry.getKey(), entry.getValue());
            } else {
                skip--;
            }
        }
        return top;
    }

    public void displayTopStat(CommandSender sender, int amount){
        if (sender instanceof Player)
            sender.sendMessage(parse(speakings.get(37), (Player) sender));
        else
            sender.sendMessage(parse(speakings.get(37), null));
        boolean useCurrency = this == Leaderboard.IMMUNITY || this == Leaderboard.CLAIMED || this == Leaderboard.ALL;
        LinkedHashMap<String, Double> map = getTop(0, amount);
        int i = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()){
            OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
            String name = p.getName();
            if (name == null){
                if (NotBounties.loggedPlayers.containsValue(entry.getKey())) {
                    for (Map.Entry<String, String> logged : NotBounties.loggedPlayers.entrySet()) {
                        if (logged.getValue().equals(entry.getKey())) {
                            name = logged.getKey();
                            break;
                        }
                        name = "???";
                    }
                } else {
                    name = "???";
                }
            }
            sender.sendMessage(parseBountyTopString(i + 1, name, entry.getValue(), useCurrency, p));
            i++;
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                   ");
    }


    private String parseStats(String text, OfflinePlayer player){
        text = text.replaceAll("\\{amount}", Matcher.quoteReplacement(getFormattedStat(player.getUniqueId())));

        return parse(text, player);
    }

    public static LinkedHashMap<String, Double> sortByValue(Map<String, Double> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        // put data from sorted list to hashmap
        LinkedHashMap<String, Double> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
    public static LinkedHashMap<String, Double> sortByName(Map<String, Double> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double>> list = new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (Objects.requireNonNull(Bukkit.getOfflinePlayer(UUID.fromString(o2.getKey())).getName())).compareTo(Objects.requireNonNull(Bukkit.getOfflinePlayer(UUID.fromString(o1.getKey())).getName())));

        // put data from sorted list to hashmap
        LinkedHashMap<String, Double> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static String parseBountyTopString(int rank, @NotNull String playerName, double amount, boolean useCurrency, OfflinePlayer player){
        String text = speakings.get(36);
        text = text.replaceAll("\\{rank}", rank + "");
        text = text.replaceAll("\\{player}", playerName);
        if (useCurrency)
            text = text.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        else
            text = text.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.formatNumber(amount)));

        return parse(text, player);
    }

    public LinkedHashMap<String, Double> getSortedList(int skip, int amount, int sortType) {
        LinkedHashMap<String, Double> top = getTop(skip, amount);
        if (sortType == 2)
            top = reverseMap(top);
        if (sortType == 3)
            top = sortByName(top);
        if (sortType == 4)
            top = reverseMap(sortByName(top));
        return top;
    }
    public LinkedHashMap<String, String> getFormattedList(int skip, int amount, int sortType){
        LinkedHashMap<String, Double> top = getSortedList(skip, amount, sortType);
        LinkedHashMap<String, String> formattedList = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : top.entrySet()){
            switch (this){
                case ALL:
                case CLAIMED:
                case IMMUNITY:
                case CURRENT:
                    formattedList.put(entry.getKey(), NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(entry.getValue()) + NumberFormatting.currencySuffix);
                    break;
                case KILLS:
                case DEATHS:
                case SET:
                    formattedList.put(entry.getKey(), NumberFormatting.formatNumber(entry.getValue()));
                    break;
            }
        }
        return formattedList;
    }

    public static LinkedHashMap<String, Double> reverseMap(LinkedHashMap<String, Double> map){
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.reverse(keys);
        LinkedHashMap<String, Double> newMap = new LinkedHashMap<>();
        for (String key : keys){
            newMap.put(key, map.get(key));
        }
        return newMap;
    }
}

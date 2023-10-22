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
        NotBounties nb = NotBounties.getInstance();
        switch (this) {
            case ALL:
                if (nb.SQL.isConnected())
                    return nb.data.getAllTime(uuid.toString());
                if (!nb.allTimeBounties.containsKey(uuid.toString()))
                    return 0;
                return nb.allTimeBounties.get(uuid.toString());
            case KILLS:
                if (nb.SQL.isConnected())
                    return nb.data.getClaimed(uuid.toString());
                if (!nb.killBounties.containsKey(uuid.toString()))
                    return 0;
                return nb.killBounties.get(uuid.toString());
            case CLAIMED:
                if (nb.SQL.isConnected())
                    return nb.data.getTotalClaimed(uuid.toString());
                if (!nb.allClaimedBounties.containsKey(uuid.toString()))
                    return 0;
                return nb.allClaimedBounties.get(uuid.toString());
            case DEATHS:
                if (nb.SQL.isConnected())
                    return nb.data.getReceived(uuid.toString());
                if (!nb.deathBounties.containsKey(uuid.toString()))
                    return 0;
                return nb.deathBounties.get(uuid.toString());
            case SET:
                if (nb.SQL.isConnected())
                    return nb.data.getSet(uuid.toString());
                if (!nb.setBounties.containsKey(uuid.toString()))
                    return 0;
                return nb.setBounties.get(uuid.toString());
            case IMMUNITY:
                if (nb.SQL.isConnected())
                    return nb.data.getImmunity(uuid.toString());
                if (!nb.immunitySpent.containsKey(uuid.toString()))
                    return 0;
                return nb.immunitySpent.get(uuid.toString());
            case CURRENT:
                if (nb.SQL.isConnected())
                    return nb.data.getBounty(uuid).getTotalBounty();
                Bounty bounty = nb.getBounty(Bukkit.getOfflinePlayer(uuid));
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
     * @param player Player to display to
     */
    public void displayStats(OfflinePlayer player, boolean shorten){
        String msg = "";
        switch (this){
            case ALL:
            case CLAIMED:
            case IMMUNITY:
            case CURRENT:
                msg = parseStats(speakings.get(0) + getStatMsg(shorten), getStat(player.getUniqueId()), true, player);

                break;
            case KILLS:
            case DEATHS:
            case SET:
                msg = parseStats(speakings.get(0) + getStatMsg(shorten), getStat(player.getUniqueId()), false, player);
                break;
        }
        if (player.isOnline()) {
            Player p = player.getPlayer();
            assert p != null;
            p.sendMessage(msg);
        }

    }

    public String getStatMsg(boolean shorten){
        switch (this){
            case ALL:
                return shorten ? speakings.get(50) : speakings.get(44);
            case KILLS:
                return shorten ? speakings.get(51) : speakings.get(45);
            case CLAIMED:
                return shorten ? speakings.get(52) : speakings.get(46);
            case DEATHS:
                return shorten ? speakings.get(53) : speakings.get(47);
            case SET:
                return shorten ? speakings.get(54) : speakings.get(48);
            case IMMUNITY:
                return shorten ? speakings.get(55) : speakings.get(49);
            case CURRENT:
                return shorten ? speakings.get(11) : speakings.get(9);
        }
        return "";
    }

    /**
     * Gets the top stats of the leaderboard type in descending order
     * @param amount Amount of values you want returned
     * @return Map of UUID and stat value in descending order
     */
    public LinkedHashMap<String, Double> getTop(int skip, int amount){
        NotBounties nb = NotBounties.getInstance();
        if (nb.SQL.isConnected())
            return nb.data.getTopStats(this, hiddenNames, skip, amount);
        LinkedHashMap<String, Double> map;
        switch (this){
            case ALL:
                map = sortByValue(nb.allTimeBounties);
                break;
            case KILLS:
                map = sortByValue(nb.killBounties);
                break;
            case CLAIMED:
                map = sortByValue(nb.allClaimedBounties);
                break;
            case DEATHS:
                map = sortByValue(nb.deathBounties);
                break;
            case SET:
                map = sortByValue(nb.setBounties);
                break;
            case IMMUNITY:
                map = sortByValue(nb.immunitySpent);
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
        NotBounties nb = NotBounties.getInstance();
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
                if (nb.loggedPlayers.containsValue(entry.getKey())) {
                    for (Map.Entry<String, String> logged : nb.loggedPlayers.entrySet()) {
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


    private static String parseStats(String text, double amount, boolean useCurrency, OfflinePlayer player){
        text = useCurrency ? text.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix)) : text.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.formatNumber(amount)));

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

    public LinkedHashMap<String, String> getFormattedList(int skip, int amount, int sortType){
        LinkedHashMap<String, Double> top = getTop(skip, amount);
        if (sortType == 2)
            top = reverseMap(top);
        if (sortType == 3)
            top = sortByName(top);
        if (sortType == 4)
            top = reverseMap(sortByName(top));
        LinkedHashMap<String, String> formattedList = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : top.entrySet()){
            switch (this){
                case ALL:
                case CLAIMED:
                case IMMUNITY:
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

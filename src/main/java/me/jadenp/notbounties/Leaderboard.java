package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.configuration.Immunity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

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
                if (!allTimeBounties.containsKey(uuid))
                    return 0;
                return allTimeBounties.get(uuid);
            case KILLS:
                if (SQL.isConnected())
                    return data.getClaimed(uuid.toString());
                if (!killBounties.containsKey(uuid))
                    return 0;
                return killBounties.get(uuid);
            case CLAIMED:
                if (SQL.isConnected())
                    return data.getTotalClaimed(uuid.toString());
                if (!allClaimedBounties.containsKey(uuid))
                    return 0;
                return allClaimedBounties.get(uuid);
            case DEATHS:
                if (SQL.isConnected())
                    return data.getReceived(uuid.toString());
                if (!deathBounties.containsKey(uuid))
                    return 0;
                return deathBounties.get(uuid);
            case SET:
                if (SQL.isConnected())
                    return data.getSet(uuid.toString());
                if (!setBounties.containsKey(uuid))
                    return 0;
                return setBounties.get(uuid);
            case IMMUNITY:
                if (SQL.isConnected())
                    return data.getImmunity(uuid.toString());
                if (!immunitySpent.containsKey(uuid))
                    return 0;
                return immunitySpent.get(uuid);
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
        String msg = parseStats(prefix + getStatMsg(shorten), player);
        if (player.isOnline()) {
            Player p = player.getPlayer();
            assert p != null;
            p.sendMessage(msg);
        }

    }

    public void displayStats(OfflinePlayer statOwner, OfflinePlayer receiver, boolean shorten) {
        String msg = parseStats(prefix + getStatMsg(shorten), statOwner);
        if (receiver.isOnline()) {
            Player p = receiver.getPlayer();
            assert p != null;
            p.sendMessage(msg);
        }
    }

    public void setStat(UUID uuid, double newAmount) {
        if (SQL.isConnected()) {
            double current = getStat(uuid);
            switch (this) {
                case ALL:
                    data.addData(uuid.toString(), 0,0,0,current-newAmount,0,0);
                    return;
                case KILLS:
                    data.addData(uuid.toString(), (long) (current-newAmount),0,0,0,0,0);
                    return;
                case CLAIMED:
                    data.addData(uuid.toString(), 0,0,0,0,0,current-newAmount);
                    return;
                case DEATHS:
                    data.addData(uuid.toString(), 0L, 0L, (long) (current-newAmount),0,0,0);
                    return;
                case SET:
                    data.addData(uuid.toString(), 0,(long) (current-newAmount),0,0,0,0);
                    return;
                case IMMUNITY:
                    Immunity.setImmunity(uuid, newAmount);
                    return;
            }
            return;
        }
        switch (this){
            case ALL:
                allTimeBounties.put(uuid, newAmount);
                return;
            case KILLS:
                killBounties.put(uuid, newAmount);
                return;
            case CLAIMED:
                allClaimedBounties.put(uuid, newAmount);
                return;
            case DEATHS:
                deathBounties.put(uuid, newAmount);
                return;
            case SET:
                setBounties.put(uuid, newAmount);
                return;
            case IMMUNITY:
                Immunity.setImmunity(uuid, newAmount);
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
                msg = shorten ? bountyStatAllShort : bountyStatAllLong;
                break;
            case KILLS:
                msg = shorten ? bountyStatKillsShort : bountyStatKillsLong;
                break;
            case CLAIMED:
                msg = shorten ? bountyStatClaimedShort : bountyStatClaimedLong;
                break;
            case DEATHS:
                msg = shorten ? bountyStatDeathsShort : bountyStatDeathsLong;
                break;
            case SET:
                msg = shorten ? bountyStatSetShort : bountyStatSetLong;
                break;
            case IMMUNITY:
                msg = shorten ? bountyStatImmunityShort : bountyStatImmunityLong;
                break;
            case CURRENT:
                msg = shorten ? listTotal : checkBounty;
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
    public LinkedHashMap<UUID, Double> getTop(int skip, int amount){
        if (SQL.isConnected())
            return data.getTopStats(this, hiddenNames, skip, amount);
        LinkedHashMap<UUID, Double> map;
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
                    map.put(bounty.getUUID(), bounty.getTotalBounty());
                map = sortByValue(map);
                break;
            default:
                map = new LinkedHashMap<>();
        }
        LinkedHashMap<UUID, Double> top = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> entry : map.entrySet()){
            if (amount == 0)
                break;
            if (skip == 0) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                if (player.getName() != null && !hiddenNames.contains(player.getName())) {
                    top.put(entry.getKey(), entry.getValue());
                    amount--;
                }
            } else {
                skip--;
            }
        }
        return top;
    }

    public void displayTopStat(CommandSender sender, int amount){
        if (sender instanceof Player)
            sender.sendMessage(parse(bountyTopTitle, (Player) sender));
        else
            sender.sendMessage(parse(bountyTopTitle, null));
        boolean useCurrency = this == Leaderboard.IMMUNITY || this == Leaderboard.CLAIMED || this == Leaderboard.ALL;
        LinkedHashMap<UUID, Double> map = getTop(0, amount);
        int i = 0;
        for (Map.Entry<UUID, Double> entry : map.entrySet()){
            OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
            String name = NotBounties.getPlayerName(entry.getKey());
            sender.sendMessage(parseBountyTopString(i + 1, name, entry.getValue(), useCurrency, p));
            i++;
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                   ");
    }


    private String parseStats(String text, OfflinePlayer player){
        text = text.replaceAll("\\{amount}", Matcher.quoteReplacement(getFormattedStat(player.getUniqueId())));

        return parse(text, player);
    }

    public static LinkedHashMap<UUID, Double> sortByValue(Map<UUID, Double> hm) {
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
    public static LinkedHashMap<UUID, Double> sortByName(Map<UUID, Double> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<UUID, Double>> list = new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (NotBounties.getPlayerName(o2.getKey())).compareTo(NotBounties.getPlayerName(o1.getKey())));

        // put data from sorted list to hashmap
        LinkedHashMap<UUID, Double> temp = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static String parseBountyTopString(int rank, @NotNull String playerName, double amount, boolean useCurrency, OfflinePlayer player){
        String text = bountyTop;
        text = text.replaceAll("\\{rank}", rank + "");
        text = text.replaceAll("\\{player}", playerName);
        if (useCurrency)
            text = text.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(amount) + NumberFormatting.currencySuffix));
        else
            text = text.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.formatNumber(amount)));

        return parse(text, player);
    }

    public LinkedHashMap<UUID, Double> getSortedList(int skip, int amount, int sortType) {
        LinkedHashMap<UUID, Double> top = getTop(skip, amount);
        if (sortType == 2)
            top = reverseMap(top);
        if (sortType == 3)
            top = sortByName(top);
        if (sortType == 4)
            top = reverseMap(sortByName(top));
        return top;
    }
    public LinkedHashMap<UUID, String> getFormattedList(int skip, int amount, int sortType){
        LinkedHashMap<UUID, Double> top = getSortedList(skip, amount, sortType);
        LinkedHashMap<UUID, String> formattedList = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> entry : top.entrySet()){
            switch (this){
                case ALL:
                case CLAIMED:
                case IMMUNITY:
                case CURRENT:
                    formattedList.put(entry.getKey(), NumberFormatting.currencyPrefix + NumberFormatting.getValue(entry.getValue()) + NumberFormatting.currencySuffix);
                    break;
                case KILLS:
                case DEATHS:
                case SET:
                    formattedList.put(entry.getKey(), NumberFormatting.getValue(entry.getValue()));
                    break;
            }
        }
        return formattedList;
    }

    public static LinkedHashMap<UUID, Double> reverseMap(LinkedHashMap<UUID, Double> map){
        List<UUID> keys = new ArrayList<>(map.keySet());
        Collections.reverse(keys);
        LinkedHashMap<UUID, Double> newMap = new LinkedHashMap<>();
        for (UUID key : keys){
            newMap.put(key, map.get(key));
        }
        return newMap;
    }
}

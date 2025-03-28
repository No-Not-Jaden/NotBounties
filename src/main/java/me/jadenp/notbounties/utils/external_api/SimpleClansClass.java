package me.jadenp.notbounties.utils.external_api;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SimpleClansClass {

    private static SimpleClans api = null;

    private static SimpleClans getAPI() {
        if (api == null) {
            api = (SimpleClans) Bukkit.getServer().getPluginManager().getPlugin("SimpleClans");
        }
        return api;
    }

    public static boolean inSameClan(Player p1, Player p2) {
        SimpleClans simpleClans = getAPI();
        if (simpleClans == null)
            return false;

        ClanPlayer player1 = simpleClans.getClanManager().getClanPlayer(p1);
        ClanPlayer player2 = simpleClans.getClanManager().getClanPlayer(p2);
        if (player1 == null || player2 == null)
            return false;

        Clan clan1 = player1.getClan();
        Clan clan2 = player2.getClan();

        return clan1 != null && clan2 != null && clan1.getName().equals(clan2.getName());
    }

    public static boolean inAlliedClan(Player p1, Player p2) {
        SimpleClans simpleClans = getAPI();
        if (simpleClans == null)
            return false;

        ClanPlayer player1 = simpleClans.getClanManager().getClanPlayer(p1);
        ClanPlayer player2 = simpleClans.getClanManager().getClanPlayer(p2);
        if (player1 == null || player2 == null)
            return false;

        Clan clan1 = player1.getClan();
        Clan clan2 = player2.getClan();

        return clan1 != null && clan2 != null && clan1.getAllies().contains(clan2.getName());
    }
}

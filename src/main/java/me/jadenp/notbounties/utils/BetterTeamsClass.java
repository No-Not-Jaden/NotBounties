package me.jadenp.notbounties.utils;

import com.booksaw.betterTeams.Team;
import org.bukkit.entity.Player;

public class BetterTeamsClass {
    public BetterTeamsClass(){}
    public boolean onSameTeam(Player player1, Player player2) {
        return Team.getTeam(player1).equals(Team.getTeam(player2));
    }

    public boolean areAllies(Player player1, Player player2) {
        return Team.getTeam(player1).isAlly(Team.getTeam(player2).getID());
    }
}

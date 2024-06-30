package me.jadenp.notbounties.utils.externalAPIs;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.entity.Player;

public class SabreFactionsClass {
    public boolean inSameFaction(Player p1, Player p2) {
        FPlayer fPlayer1 = FPlayers.getInstance().getByPlayer(p1);
        FPlayer fPlayer2 = FPlayers.getInstance().getByPlayer(p2);
        if (fPlayer2.getFaction().isWilderness() || fPlayer1.getFaction().isWilderness())
            return false;
        return fPlayer1.getFactionId().equals(fPlayer2.getFactionId());
    }

    public boolean areFactionsAllied(Player p1, Player p2) {
        FPlayer fPlayer1 = FPlayers.getInstance().getByPlayer(p1);
        FPlayer fPlayer2 = FPlayers.getInstance().getByPlayer(p2);
        if (fPlayer2.getFaction().isWilderness() || fPlayer1.getFaction().isWilderness())
            return false;
        return fPlayer1.getFaction().getRelationTo(fPlayer2.getFaction()) == Relation.ALLY;
    }
}

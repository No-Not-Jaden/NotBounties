package me.jadenp.notbounties.utils.external_api;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.entity.Player;

public class SaberFactionsClass {
    public boolean inSameFaction(Player p1, Player p2) throws NoSuchMethodError{
        FPlayer fPlayer1 = FPlayers.getInstance().getByPlayer(p1);
        FPlayer fPlayer2 = FPlayers.getInstance().getByPlayer(p2);
        if (fPlayer2.getFaction().isWilderness() || fPlayer1.getFaction().isWilderness())
            return false;
        return fPlayer1.getFactionId().equals(fPlayer2.getFactionId());
    }

    public boolean areFactionsAllied(Player p1, Player p2) throws NoSuchMethodError {
        FPlayer fPlayer1 = FPlayers.getInstance().getByPlayer(p1);
        FPlayer fPlayer2 = FPlayers.getInstance().getByPlayer(p2);
        if (fPlayer2.getFaction().isWilderness() || fPlayer1.getFaction().isWilderness())
            return false;
        return fPlayer1.getFaction().getRelationTo(fPlayer2.getFaction()) == Relation.ALLY;
    }
}

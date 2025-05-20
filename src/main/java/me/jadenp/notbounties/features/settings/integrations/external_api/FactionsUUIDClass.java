package me.jadenp.notbounties.features.settings.integrations.external_api;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.entity.Player;

public class FactionsUUIDClass {
    public boolean inSameFaction(Player p1, Player p2) throws NoSuchMethodError {
        FPlayer fplayer1 = FPlayers.getInstance().getByPlayer(p1);
        FPlayer fplayer2 = FPlayers.getInstance().getByPlayer(p2);
        return fplayer1.hasFaction() && fplayer1.getFactionId().equals(fplayer2.getFactionId());
    }

    public boolean areFactionsAllied(Player p1, Player p2) throws NoSuchMethodError {
        Faction f1 = FPlayers.getInstance().getByPlayer(p1).getFaction();
        Faction f2 = FPlayers.getInstance().getByPlayer(p2).getFaction();
        if (!f1.isSystemFaction() && f1.getId().equals(f2.getId()))
            return true;
        return f1.getRelationTo(f2) == Relation.ALLY;
    }
}

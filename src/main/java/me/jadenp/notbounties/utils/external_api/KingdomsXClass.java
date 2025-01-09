package me.jadenp.notbounties.utils.external_api;


import org.bukkit.entity.Player;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.Nation;
import org.kingdoms.constants.group.model.relationships.KingdomRelation;
import org.kingdoms.constants.player.KingdomPlayer;

public class KingdomsXClass {
    public boolean inSameNation(Player player1, Player player2) {
        KingdomPlayer kingdomPlayer1 = KingdomPlayer.getKingdomPlayer(player1);
        Nation nation1 = kingdomPlayer1.getKingdom() != null ? kingdomPlayer1.getKingdom().getNation() : null;
        KingdomPlayer kingdomPlayer2 = KingdomPlayer.getKingdomPlayer(player2);
        Nation nation2 = kingdomPlayer2.getKingdom() != null ? kingdomPlayer2.getKingdom().getNation() : null;
        return nation1 != null && nation1.equals(nation2);
    }

    /**
     * 0 - Not enemies or allied.
     * 1 - Enemies.
     * 2 - Allied.
     * @return Nation Relation
     */
    public int getNationRelation(Player player1, Player player2) {
        KingdomPlayer kingdomPlayer1 = KingdomPlayer.getKingdomPlayer(player1);
        Nation nation1 = kingdomPlayer1.getKingdom() != null ? kingdomPlayer1.getKingdom().getNation() : null;
        KingdomPlayer kingdomPlayer2 = KingdomPlayer.getKingdomPlayer(player2);
        Nation nation2 = kingdomPlayer2.getKingdom() != null ? kingdomPlayer2.getKingdom().getNation() : null;
        if (nation1 == null || nation2 == null)
            return 0;
        if (nation1.getRelationWith(nation2) == KingdomRelation.ENEMY)
            return 1;
        if (nation1.getRelationWith(nation2) == KingdomRelation.ALLY)
            return 2;
        return 0;
    }
    /**
     * 0 - Not enemies or allied.
     * 1 - Enemies.
     * 2 - Allied.
     * @return Nation Relation
     */
    public int getKingdomRelation(Player player1, Player player2) {
        KingdomPlayer kingdomPlayer1 = KingdomPlayer.getKingdomPlayer(player1);
        Kingdom kingdom1 = kingdomPlayer1.getKingdom();
        KingdomPlayer kingdomPlayer2 = KingdomPlayer.getKingdomPlayer(player2);
        Kingdom kingdom2 = kingdomPlayer2.getKingdom();
        if (kingdom1 == null || kingdom2 == null)
            return 0;
        if (kingdom1.getRelationWith(kingdom2) == KingdomRelation.ENEMY)
            return 1;
        if (kingdom1.getRelationWith(kingdom2) == KingdomRelation.ALLY)
            return 2;
        return 0;
    }


    public boolean inSameKingdom(Player player1, Player player2) {
        KingdomPlayer kingdomPlayer1 = KingdomPlayer.getKingdomPlayer(player1);
        Kingdom kingdom1 = kingdomPlayer1.getKingdom();
        KingdomPlayer kingdomPlayer2 = KingdomPlayer.getKingdomPlayer(player2);
        Kingdom kingdom2 = kingdomPlayer2.getKingdom();
        return kingdom1 != null && kingdom1.equals(kingdom2);
    }
}

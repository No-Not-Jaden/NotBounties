package me.jadenp.notbounties.features.settings.integrations.external_api;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;

public class TownyAdvancedClass {
    public TownyAdvancedClass() {
    }
    public boolean inSameNation(Player player1, Player player2) {
        TownyAPI townyAPI = TownyAPI.getInstance();
        Nation nation1 = townyAPI.getNation(player1);
        Nation nation2 = townyAPI.getNation(player2);
        return nation1 != null && nation1.equals(nation2);
    }

    public boolean areNationsAllied(Player player1, Player player2) {
        TownyAPI townyAPI = TownyAPI.getInstance();
        Nation nation1 = townyAPI.getNation(player1);
        Nation nation2 = townyAPI.getNation(player2);
        return nation1 != null && nation2 != null && nation1.isAlliedWith(nation2);
    }

    public boolean inSameTown(Player player1, Player player2) {
        TownyAPI townyAPI = TownyAPI.getInstance();
        Town town1 = townyAPI.getTown(player1);
        Town town2 = townyAPI.getTown(player2);
        return town1 != null && town1.equals(town2);
    }

    public int getResidents(String name) {
        TownyAPI townyAPI = TownyAPI.getInstance();
        Town town = townyAPI.getTown(name);
        if (town == null)
            return -1;
        return town.getResidents().size();
    }
}

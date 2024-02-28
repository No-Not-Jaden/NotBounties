package me.jadenp.notbounties.utils.externalAPIs;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.inventory.ItemStack;

public class HeadDataBaseClass {
    public HeadDataBaseClass(){}
    public ItemStack getHead(String data) {
        HeadDatabaseAPI hdb = new HeadDatabaseAPI();
        return hdb.getItemHead(data);
    }
}

package me.jadenp.notbounties.utils.externalAPIs;

import com.earth2me.essentials.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class EssentialsXClass {
    private final IEssentials essentials;
    public EssentialsXClass() {
        essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
    }

    public BigDecimal getItemValue(ItemStack itemStack) {
        return essentials.getWorth().getPrice(essentials, itemStack);
    }
}

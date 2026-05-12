package me.jadenp.notbounties.features.settings.integrations.external_api;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.UUID;

public class EssentialsXClass {
    private final IEssentials essentials;
    public EssentialsXClass() {
        essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
    }

    public BigDecimal getItemValue(ItemStack itemStack) {
        return essentials.getWorth().getPrice(essentials, itemStack);
    }

    public @Nullable String getNick(UUID uuid) {
        User user = essentials.getUser(uuid);
        if (user != null) {
            return user.getNick();
        }
        return null;
    }

    public @Nullable UUID getUUID(String playerName) {
        User user = essentials.getOfflineUser(playerName);
        if (user != null) {
            return user.getUUID();
        }
        return null;
    }
}

package me.jadenp.notbounties;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.jadenp.notbounties.utils.ConfigOptions.*;

public class RewardHead {
    private final String playerName;
    private final UUID uuid;
    private final double amount;

    public RewardHead(String playerName, UUID uuid, double amount) {

        this.playerName = playerName;
        this.uuid = uuid;
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public ItemStack getItem(){
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(parse(speakings.get(70), player.getName(), amount, player));
        List<String> lore = new ArrayList<>();
        rewardHeadLore.stream().map(str -> parse(str, player.getName(), amount, player)).forEach(lore::add);
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        return skull;
    }

}

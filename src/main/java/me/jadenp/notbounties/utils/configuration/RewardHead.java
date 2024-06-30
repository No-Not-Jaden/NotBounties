package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.HeadFetcher;
import me.jadenp.notbounties.ui.SkinManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.jadenp.notbounties.utils.configuration.LanguageOptions.rewardHeadLore;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.rewardHeadName;

public class RewardHead {
    private final String playerName;
    private final UUID uuid;
    private final double amount;

    public RewardHead(String playerName, UUID uuid, double amount) {
        SkinManager.isSkinLoaded(uuid);
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
        ItemStack skull = Head.createPlayerSkull(uuid, SkinManager.getSkin(uuid).getUrl());
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setDisplayName(LanguageOptions.parse(rewardHeadName, playerName, amount, player));
        List<String> lore = new ArrayList<>();
        rewardHeadLore.forEach(str -> lore.add(LanguageOptions.parse(str, playerName, amount, player)));
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        return skull;
    }

}

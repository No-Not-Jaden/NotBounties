package me.jadenp.notbounties.data;

import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public record RewardHead(UUID uuid, UUID killer, double amount) {
    public RewardHead {
        SkinManager.isSkinLoaded(uuid);
    }

    public ItemStack getItem() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String killerName = LoggedPlayers.getPlayerName(killer);
        ItemStack skull = Head.createPlayerSkull(uuid, SkinManager.getSkin(uuid).getUrl());
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setDisplayName(LanguageOptions.parse(LanguageOptions.getMessage("reward-head-name").replace("{killer}", killerName), amount, player));
        List<String> lore = new ArrayList<>();
        LanguageOptions.getListMessage("reward-head-lore").forEach(str -> lore.add(LanguageOptions.parse(str.replace("{killer}", killerName), amount, player)));
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        return skull;
    }

    public static RewardHead decodeRewardHead(String input) {
        // uuid,killerUuid,amount
        try {
            if (!input.contains(",")) {
                UUID uuid = UUID.fromString(input);
                return new RewardHead( uuid, DataManager.GLOBAL_SERVER_ID, 0);
            } else {
                UUID uuid = UUID.fromString(input.substring(0, input.indexOf(",")));
                input = input.substring(input.indexOf(',') + 1);
                UUID killerUuid;
                try {
                    killerUuid = UUID.fromString(input.substring(0, input.indexOf(",")));
                } catch (IllegalArgumentException e) {
                    // reward heads used to store player names at this location, so a uuid may not be present.
                    killerUuid = DataManager.GLOBAL_SERVER_ID;
                }
                input = input.substring(input.indexOf(",") + 1);
                double amount = NumberFormatting.tryParse(input);
                return new RewardHead(uuid, killerUuid, amount);
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning(e.toString());
        }
        return null;
    }

}

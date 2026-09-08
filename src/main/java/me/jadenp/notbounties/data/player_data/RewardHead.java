package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public final class RewardHead {

    private static boolean rewardSetters;
    private static boolean rewardKiller;
    private static boolean rewardAnyKill;
    private static double minimumBounty;

    private RewardHead() {}

    public static void loadConfiguration(ConfigurationSection config) {
        rewardSetters = config.getBoolean("setters");
        rewardKiller = config.getBoolean("claimed");
        rewardAnyKill = config.getBoolean("any-kill");
        minimumBounty = config.getDouble("minimum-bounty");
    }

    public static double getMinimumBounty() {
        return minimumBounty;
    }

    public static boolean isRewardAnyKill() {
        return rewardAnyKill;
    }

    public static boolean isRewardKiller() {
        return rewardKiller;
    }

    public static boolean isRewardSetters() {
        return rewardSetters;
    }

    public static ItemStack getItem(UUID uuid, UUID killer, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String killerName = LoggedPlayers.getPlayerName(killer);
        ItemStack skull = Head.createPlayerSkull(uuid, SkinManager.getSkin(uuid).url());
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setDisplayName(Messages.parse(LanguageOptions.getMessage("reward-head-name").replace("{killer}", killerName), MessageContext.builder().withPrefix(false).amount(amount).receiver(player).build()).join());
        List<String> lore = new ArrayList<>();
        LanguageOptions.getListMessage("reward-head-lore").forEach(str -> lore.add(Messages.parse(str.replace("{killer}", killerName), MessageContext.builder().withPrefix(false).amount(amount).receiver(player).build()).join()));
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        return skull;
    }

}

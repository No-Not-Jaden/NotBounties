package me.jadenp.notbounties.ui;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.utils.tasks.HeadLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class HeadFetcher {
    private static final Cache<UUID, ItemStack> savedHeads = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    public void loadHeads(Player player, PlayerGUInfo guInfo, List<QueuedHead> heads) {
        HeadLoader headLoader = new HeadLoader(player, guInfo, heads, savedHeads);
        headLoader.setTaskImplementation(NotBounties.getServerImplementation().async().runAtFixedRate(headLoader, 1, 4));
    }


    public static ItemStack getUnloadedHead(UUID uuid) {
        ItemStack head = savedHeads.getIfPresent(uuid);
        if (head != null)
            return head.clone();
        if (SkinManager.isUseUnloadedPlayerProfile()) {
            return Head.createPlayerSkull(uuid, DefaultSkinHelper.get(uuid).url());
        }

        return Head.createPlayerSkull(uuid, SkinManager.getSkin(uuid).url());
    }
}

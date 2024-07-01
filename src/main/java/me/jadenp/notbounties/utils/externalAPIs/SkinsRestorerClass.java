package me.jadenp.notbounties.utils.externalAPIs;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.ProxyMessaging;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

public class SkinsRestorerClass {
    private SkinsRestorer skinsRestorer;
    private long lastHookError = 0;
    private boolean firstConnect = true;

    public SkinsRestorerClass() {
        connect();
    }

    private boolean connect(){
        try {
            skinsRestorer = SkinsRestorerProvider.get();
            return true;
        } catch (IllegalStateException e) {
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                // set first connect to false in 5 seconds (give the proxy time to respond)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        firstConnect = false;
                    }
                }.runTaskLaterAsynchronously(NotBounties.getInstance(), 5 * 20L);
            }
            if (!firstConnect && lastHookError < System.currentTimeMillis()) {
                    Bukkit.getLogger().warning("[NotBounties] Failed at hooking into SkinsRestorer, will try again on next call.");
                    lastHookError = System.currentTimeMillis() + 60000 * 5;
                }
            return false;
        }
    }


    public void saveSkin(UUID uuid) {
        if (ProxyMessaging.hasConnectedBefore()) {
            ProxyMessaging.requestPlayerSkin(uuid);
            // timeout runnable
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!SkinManager.isSkinLoaded(uuid))
                        requestNamedSkin(uuid);
                }
            }.runTaskLaterAsynchronously(NotBounties.getInstance(), 5 * 20L); // 5 seconds
            return;
        }
        String name = NotBounties.getPlayerName(uuid);
        if (!connect()) {
            if (!firstConnect)
                requestNamedSkin(uuid);
            return;
        }
        PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
        try {
            Optional<SkinProperty> skinProperty = playerStorage.getSkinForPlayer(uuid, name);
            if (skinProperty.isEmpty()) {
                if (NotBounties.debug)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getLogger().warning("[NotBountiesDebug] Skin property not present from SkinsRestorer for " + name + ".");
                        }
                    }.runTask(NotBounties.getInstance());
                requestNamedSkin(uuid);
                return;
            }
            String skinUrl = PropertyUtils.getSkinTextureUrl(skinProperty.get());
            String identifier = PropertyUtils.getSkinProfileData(skinProperty.get()).getProfileId();
            SkinManager.saveSkin(uuid, new PlayerSkin(new URL(skinUrl), identifier));
        } catch (MalformedURLException | DataRequestException e) {
            if (NotBounties.debug)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getLogger().warning("[NotBountiesDebug] Error getting skin from SkinsRestorer.");
                        Bukkit.getLogger().warning(e.toString());
                    }
                }.runTask(NotBounties.getInstance());
            requestNamedSkin(uuid);
        }
    }

    private static void requestNamedSkin(UUID uuid) {
        try {
            SkinManager.requestSkin(uuid);
        } catch (Exception e2) {
            NotBounties.debugMessage("[NotBountiesDebug] Unable to obtain a skin for " + uuid + ".", true);
        }
    }





}

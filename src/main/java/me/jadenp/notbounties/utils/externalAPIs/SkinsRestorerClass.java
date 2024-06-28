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

import java.io.IOException;
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
        firstConnect = false;
    }

    private boolean connect(){
        try {
            skinsRestorer = SkinsRestorerProvider.get();
            return true;
        } catch (IllegalStateException e) {
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
            return;
        }
        String name = NotBounties.getPlayerName(uuid);
        if (!connect()) {
            requestNamedSkin(uuid, name);
            return;
        }
        PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
        try {
            Optional<SkinProperty> skinProperty = playerStorage.getSkinForPlayer(uuid, name);
            if (!skinProperty.isPresent()) {
                if (NotBounties.debug)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getLogger().warning("[NotBountiesDebug] Skin property not present from SkinsRestorer for " + name + ".");
                        }
                    }.runTask(NotBounties.getInstance());
                requestNamedSkin(uuid, name);
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
            requestNamedSkin(uuid, name);
        }
    }

    private static void requestNamedSkin(UUID uuid, String name) {
        try {
            SkinManager.saveNamedSkin(uuid, name);
        } catch (Exception e2) {
            if (NotBounties.debug)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getLogger().warning("[NotBountiesDebug] Unable to obtain a skin for " + name + ".");
                    }
                }.runTask(NotBounties.getInstance());
        }
    }





}

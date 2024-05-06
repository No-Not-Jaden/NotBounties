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
            if (!firstConnect) // it will be normal to get this error on the first connection if in proxy mode
                if (lastHookError < System.currentTimeMillis()) {
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
        if (!connect())
            return;
        PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
        String name = NotBounties.getPlayerName(uuid);
        try {
            Optional<SkinProperty> skinProperty = playerStorage.getSkinForPlayer(uuid, name);
            if (!skinProperty.isPresent()) {
                if (NotBounties.debug)
                    Bukkit.getLogger().warning("[NotBountiesDebug] Skin property not present from SkinsRestorer for " + name + ".");
                return;
            }
            String skinUrl = PropertyUtils.getSkinTextureUrl(skinProperty.get());
            String identifier = PropertyUtils.getSkinProfileData(skinProperty.get()).getProfileId();
            SkinManager.saveSkin(uuid, new PlayerSkin(new URL(skinUrl), identifier));
        } catch (MalformedURLException | DataRequestException e) {
            if (NotBounties.debug) {
                Bukkit.getLogger().warning("[NotBountiesDebug] Error getting skin from SkinsRestorer.");
                Bukkit.getLogger().warning(e.toString());
            }
        }
    }





}

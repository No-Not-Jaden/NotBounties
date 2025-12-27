package me.jadenp.notbounties.features.settings.integrations.external_api;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyDatabase;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsRestorerClass {
    private SkinsRestorer skinsRestorer;
    private long lastHookError = 0;
    private boolean firstConnect = true;
    private boolean connected;
    private final Map<UUID, Long> delayedChecks = new ConcurrentHashMap<>();
    private TaskImplementation<Void> delayedCheckTask = null;
    private static final long MIN_DELAY = 4000L;
    private static final long MAX_DELAY = 6000L;

    public SkinsRestorerClass() {
        connected = false;
        connect();
    }

    private boolean connect(){
        try {
            skinsRestorer = SkinsRestorerProvider.get();
            connected = true;
            return true;
        } catch (IllegalStateException e) {
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                // set first connect to false in 5 seconds (give the proxy time to respond)
                NotBounties.getServerImplementation().global().runDelayed(() -> firstConnect = false, 5 * 20L);
            }
            if (!firstConnect && lastHookError < System.currentTimeMillis() && !(ProxyMessaging.hasConnectedBefore() && ProxyDatabase.areSkinRequestsEnabled())) {
                    Bukkit.getLogger().warning("[NotBounties] Failed at hooking into SkinsRestorer, will try again on next call.");
                    lastHookError = System.currentTimeMillis() + 60000 * 5;
                }
            return false;
        }
    }


    private void addDelayedSkinCheck(UUID uuid) {
        NotBounties.getServerImplementation().global().run(() -> {
            delayedChecks.computeIfAbsent(uuid, key -> System.currentTimeMillis() + MAX_DELAY);
            if (delayedCheckTask == null) {
                scheduleNextDelayedCheck();
            }
        });
    }

    private void scheduleNextDelayedCheck() {
        long tickDelay = 1;
        for (Map.Entry<UUID, Long> entry : delayedChecks.entrySet()) {
            tickDelay = (long) ((entry.getValue() - System.currentTimeMillis()) * 0.02);
            if (tickDelay > 0)
                break;
        }
        if (tickDelay < 0)
            tickDelay = 1;
        delayedCheckTask = createDelayedCheckTask(tickDelay);
    }

    private TaskImplementation<Void> createDelayedCheckTask(long tickDelay) {
        return NotBounties.getServerImplementation().global().runDelayed(() -> {
            Iterator<Map.Entry<UUID, Long>> mapIterator = delayedChecks.entrySet().iterator();
            while (mapIterator.hasNext()) {
                Map.Entry<UUID, Long> entry = mapIterator.next();
                if (entry.getValue() - System.currentTimeMillis() < MAX_DELAY - MIN_DELAY) {
                    // do check
                    if (!SkinManager.isSkinLoaded(entry.getKey())) {
                        NotBounties.debugMessage("Proxy skin request timed out for player: " + entry.getKey().toString(), true);
                        requestSkinManually(entry.getKey());
                    } else {
                        connected = true;
                    }
                    mapIterator.remove();
                } else {
                    // map should be in ascending order, so all the following values will be too large
                    break;
                }
            }
            // schedule next runnable if more are in the map
            if (!delayedChecks.isEmpty()) {
                scheduleNextDelayedCheck();
            } else {
                delayedCheckTask = null;
            }
        }, tickDelay);
    }

    public void saveSkin(UUID uuid) {
        if (ProxyMessaging.hasConnectedBefore() && ProxyDatabase.areSkinRequestsEnabled()) {
            ProxyMessaging.requestPlayerSkin(uuid);
            // timeout
            addDelayedSkinCheck(uuid);
            return;
        }
        if (!connected)
            connect();
        String name = LoggedPlayers.getPlayerName(uuid);
        if (!connected) {
            if (!firstConnect)
                requestSkinManually(uuid);
            return;
        }
        PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
        try {
            Optional<SkinProperty> skinProperty = playerStorage.getSkinForPlayer(uuid, name);
            if (skinProperty.isEmpty()) {
                NotBounties.debugMessage("[NotBountiesDebug] Skin property not present from SkinsRestorer for " + name + ".", true);
                requestSkinManually(uuid);
                return;
            }
            String skinUrl = PropertyUtils.getSkinTextureUrl(skinProperty.get());
            String identifier = PropertyUtils.getSkinProfileData(skinProperty.get()).getProfileId();
            SkinManager.saveSkin(uuid, new PlayerSkin(new URI(skinUrl).toURL(), identifier, false));
        } catch (MalformedURLException | DataRequestException | URISyntaxException | NullPointerException e) {
            // these could come from SkinsRestorer being on proxy mode, SkinsRestorer not having the skins, SkinsRestorer not setup correctly
            // afaik, this is the correct way to retrieve skins from SkinsRestorer, and any error is a problem with the SkinsRestorer plugin
            NotBounties.debugMessage("[NotBountiesDebug] Error getting skin from SkinsRestorer.", true);
            NotBounties.debugMessage(e.toString(), true);
            requestSkinManually(uuid);
        }
    }

    private static void requestSkinManually(UUID uuid) {
        SkinManager.requestSkin(uuid, false);
    }





}

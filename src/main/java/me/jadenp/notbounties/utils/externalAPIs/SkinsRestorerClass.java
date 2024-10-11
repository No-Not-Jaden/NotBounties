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
import org.bukkit.scheduler.BukkitTask;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class SkinsRestorerClass {
    private SkinsRestorer skinsRestorer;
    private long lastHookError = 0;
    private boolean firstConnect = true;
    private final Map<UUID, Long> delayedChecks = new LinkedHashMap<>();
    private BukkitTask delayedCheckTask = null;
    private static final long MIN_DELAY = 4000L;
    private static final long MAX_DELAY = 6000L;

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
                }.runTaskLater(NotBounties.getInstance(), 5 * 20L);
            }
            if (!firstConnect && lastHookError < System.currentTimeMillis()) {
                    Bukkit.getLogger().warning("[NotBounties] Failed at hooking into SkinsRestorer, will try again on next call.");
                    lastHookError = System.currentTimeMillis() + 60000 * 5;
                }
            return false;
        }
    }


    private void addDelayedSkinCheck(UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                delayedChecks.computeIfAbsent(uuid, key -> System.currentTimeMillis() + MAX_DELAY);
                if (delayedCheckTask == null) {
                    scheduleNextDelayedCheck();
                }
            }
        }.runTask(NotBounties.getInstance());
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

    private BukkitTask createDelayedCheckTask(long tickDelay) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, Long>> mapIterator = delayedChecks.entrySet().iterator();
                while (mapIterator.hasNext()) {
                    Map.Entry<UUID, Long> entry = mapIterator.next();
                    if (entry.getValue() - System.currentTimeMillis() < MAX_DELAY - MIN_DELAY) {
                        // do check
                        if (!SkinManager.isSkinLoaded(entry.getKey())) {
                            NotBounties.debugMessage("Proxy skin request timed out for player: " + entry.getKey().toString(), true);
                            requestSkinManually(entry.getKey());
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
            }
        }.runTaskLater(NotBounties.getInstance(), tickDelay);
    }

    public void saveSkin(UUID uuid) {
        if (ProxyMessaging.hasConnectedBefore()) {
            ProxyMessaging.requestPlayerSkin(uuid);
            // timeout
            addDelayedSkinCheck(uuid);
            return;
        }
        String name = NotBounties.getPlayerName(uuid);
        if (!connect()) {
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
            SkinManager.saveSkin(uuid, new PlayerSkin(new URL(skinUrl), identifier));
        } catch (MalformedURLException | DataRequestException e) {
            NotBounties.debugMessage("[NotBountiesDebug] Error getting skin from SkinsRestorer.", true);
            NotBounties.debugMessage(e.toString(), true);
            requestSkinManually(uuid);
        }
    }

    private static void requestSkinManually(UUID uuid) {
        SkinManager.requestSkin(uuid, false);
    }





}

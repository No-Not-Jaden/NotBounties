package me.jadenp.notbounties.features.settings.databases.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import me.jadenp.notbounties.utils.DataManager;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class PlayerCache<T> implements Cache<UUID, T> {

    // Zero or a negative duration disables expiration.
    private volatile long expirationNanos;
    private final long maxCacheSize; // TODO: implement

    private final Map<UUID, CacheEntry<T>> entries =
            new ConcurrentHashMap<>();
    private final CacheMapView<T> mapView;

    public PlayerCache(Duration expiration, long maxCacheSize) {
        setExpiration(expiration);
        this.maxCacheSize = maxCacheSize;
        mapView = new CacheMapView<>(this);
    }

    public void setExpiration(Duration expiration) {
        if (expiration == null || expiration.isNegative()
                || expiration.isZero()) {
            expirationNanos = -1L;
        } else {
            expirationNanos = expiration.toNanos();
        }
    }

    @Override
    public @Nullable T getIfPresent(@NotNull Object key) {
        if (!(key instanceof UUID uuid)) return null;

        CacheEntry<T> entry = entries.get(uuid);

        if (entry == null) {
            return null;
        }

        synchronized (entry) {
            // Ensure this entry hasn't been replaced or removed.
            if (entries.get(uuid) != entry) {
                return getIfPresent(uuid);
            }

            if (isExpired(uuid, entry)) {
                entries.remove(uuid, entry);
                return null;
            }

            // Refresh the idle timeout.
            entry.lastAccessNanos = System.nanoTime();
            return entry.value;
        }
    }

    @Override
    public @NonNull T get(@NonNull UUID key, @NonNull Callable<? extends T> loader) throws ExecutionException {
        T value = getIfPresent(key);
        if (value != null) {
            return value;
        }

        try {
            return loader.call();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public @NonNull ImmutableMap<UUID, T> getAllPresent(@NonNull Iterable<?> keys) {
        cleanUp();
        // copy entries so we don't return CacheEntry values
        Map<UUID, T> newEntries = new HashMap<>();
        entries.forEach((uuid, entry) -> newEntries.put(uuid, entry.value));
        return ImmutableMap.copyOf(newEntries);
    }

    public void put(@NonNull UUID uuid, @NonNull T value) {
        entries.put(uuid, new CacheEntry<>(value));
    }

    @Override
    public void putAll(Map<? extends UUID, ? extends T> m) {
        m.forEach(this::put);
    }

    @Override
    public void invalidate(@NonNull Object key) {
        if (!(key instanceof UUID)) return;
        entries.remove(key);
    }

    @Override
    public void invalidateAll(Iterable<?> keys) {
        for (Object key : keys) {
            invalidate(key);
        }
    }

    @Override
    public void invalidateAll() {
        entries.clear();
    }

    public void remove(UUID uuid) {
        entries.remove(uuid);
    }

    public boolean contains(UUID uuid) {
        return getIfPresent(uuid) != null;
    }

    private boolean isExpired(UUID uuid, CacheEntry<T> entry) {
        long expiration = expirationNanos;

        if (expiration < 0 || DataManager.isOnline(uuid)) {
            return false;
        }

        return System.nanoTime() - entry.lastAccessNanos >= expiration;
    }

    public long size() {
        return entries.size();
    }

    @Override
    public @NonNull CacheStats stats() {
        return new CacheStats(0,0,0,0,0,0);
    }

    Map<UUID, CacheEntry<T>> entries() {
        return entries;
    }

    @Override
    public @NonNull ConcurrentMap<UUID, T> asMap() {
        return mapView;
    }

    @Override
    public void cleanUp() {
        for (Map.Entry<UUID, CacheEntry<T>> item : entries.entrySet()) {
            UUID uuid = item.getKey();
            CacheEntry<T> entry = item.getValue();

            synchronized (entry) {
                if (entries.get(uuid) == entry && isExpired(uuid, entry)) {
                    entries.remove(uuid, entry);
                }
            }
        }
    }
}
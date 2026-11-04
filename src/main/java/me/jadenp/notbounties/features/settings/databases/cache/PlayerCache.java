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
import java.util.concurrent.*;

public class PlayerCache<T> implements Cache<UUID, T> {

    // Zero or a negative duration disables expiration.
    private volatile long expirationNanos;
    // This is useful for when player data is modified by admins on another server. Expected to not be very common.
    private volatile long onlineExpirationNanos;
    // This is useful to save space, especially if there is no expiration.
    private volatile long maxCacheSize;
    private boolean removingOverflow = false;

    private final Map<UUID, CacheEntry<T>> entries =
            new ConcurrentHashMap<>();
    private final CacheMapView<T> mapView;

    public PlayerCache(Duration expiration, Duration onlineExpiration, long maxCacheSize) {
        setExpiration(expiration);
        setOnlineExpiration(onlineExpiration);
        setMaxCacheSize(maxCacheSize);
        mapView = new CacheMapView<>(this);
    }

    public PlayerCache() {
        // initialize with default values
        setExpiration(Duration.ZERO);
        setOnlineExpiration(Duration.ZERO);
        setMaxCacheSize(0);
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

    public void setOnlineExpiration(Duration onlineExpiration) {
        if (onlineExpiration == null || onlineExpiration.isNegative()
                || onlineExpiration.isZero()) {
            onlineExpirationNanos = -1L;
        } else {
            onlineExpirationNanos = onlineExpiration.toNanos();
        }
    }

    public void setMaxCacheSize(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
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
        // check for space
        if (maxCacheSize > 0 && entries.size() >= maxCacheSize) {
            // invalidate a key, one with the lowest lastAccess time
            CompletableFuture.runAsync(this::removeOverflow); // lazy removal
        }
        entries.put(uuid, new CacheEntry<>(value));

    }

    /**
     * Remove entries past the maxCacheSize with the lowest lastAccessNanos.
     */
    private void removeOverflow() {
        // skip if this function is already running
        if (removingOverflow) return;
        removingOverflow = true;
        if (maxCacheSize <= 0) return;
        long entriesToRemove = entries.size() - maxCacheSize;
        if (entriesToRemove <= 0) return;
        List<Map.Entry<UUID, CacheEntry<T>>> entriesToRemoveList = new ArrayList<>(entries.entrySet());
        entriesToRemoveList.sort(Comparator.comparingLong(entry -> entry.getValue().lastAccessNanos));
        for (int i = 0; i < entriesToRemove; i++) {
            entries.remove(entriesToRemoveList.get(i).getKey());
        }
        removingOverflow = false;
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
        long expiration = getExpirationTime(uuid);

        if (expiration < 0 ) {
            return false;
        }

        return System.nanoTime() - entry.lastAccessNanos >= expiration;
    }

    private long getExpirationTime(UUID uuid) {
        if (DataManager.isOnline(uuid)) {
            return onlineExpirationNanos;
        } else {
            return expirationNanos;
        }
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
        removeOverflow();
    }
}
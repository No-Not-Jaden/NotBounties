package me.jadenp.notbounties.features.settings.databases;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import me.jadenp.notbounties.utils.DataManager;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class PlayerCache<T> implements Cache<UUID, T> {

    // Zero or a negative duration disables expiration.
    private volatile long expirationNanos;

    private final Map<UUID, CacheEntry<T>> entries =
            new ConcurrentHashMap<>();

    public PlayerCache(Duration expiration) {
        setExpiration(expiration);
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
    public T get(@NonNull UUID key, @NonNull Callable<? extends T> loader) throws ExecutionException {
        T value = getIfPresent(key);
        if (value != null) {
            return value;
        }

        return ;
    }

    @Override
    public ImmutableMap<UUID, T> getAllPresent(Iterable<?> keys) {
        return null;
    }

    public void put(UUID uuid, T value) {
        entries.put(uuid, new CacheEntry<>(value));
    }

    @Override
    public void putAll(Map<? extends UUID, ? extends T> m) {

    }

    @Override
    public void invalidate(Object key) {

    }

    @Override
    public void invalidateAll(Iterable<?> keys) {

    }

    @Override
    public void invalidateAll() {

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
    public CacheStats stats() {
        return null;
    }

    @Override
    public ConcurrentMap<UUID, T> asMap() {
        return null;
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

    private static class CacheEntry<T> {
        private final T value;
        private volatile long lastAccessNanos;

        private CacheEntry(T value) {
            this.value = value;
            this.lastAccessNanos = System.nanoTime();
        }
    }
}
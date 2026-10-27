package me.jadenp.notbounties.features.settings.databases.cache;

import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CacheMapView<T> extends ConcurrentHashMap<UUID, T> {

    private final PlayerCache<T> cache;

    public CacheMapView(PlayerCache<T> cache) {
        this.cache = cache;
    }

    @Override
    public T get(Object key) {
        if (!(key instanceof UUID)) return null;
        return cache.getIfPresent((UUID) key);
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof UUID)) return false;
        return cache.contains((UUID) key);
    }

    @Override
    public T put(@NonNull UUID key, @NonNull T value) {
        T previous = get(key);
        cache.put(key, value);
        return previous;
    }

    @Override
    public T remove(@NonNull Object key) {
        if (!(key instanceof UUID)) return null;
        T previous = get(key);
        cache.remove((UUID) key);
        return previous;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public @NonNull Set<Entry<UUID, T>> entrySet() {
        return new AbstractSet<>() {

            @Override
            public int size() {
                return Math.toIntExact(cache.size());
            }

            @Override
            public void clear() {
                cache.invalidateAll();
            }

            @Override
            public @NonNull Iterator<Entry<UUID, T>> iterator() {
                Iterator<Map.Entry<UUID, CacheEntry<T>>> iterator =
                        cache.entries().entrySet().iterator();

                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Map.Entry<UUID, T> next() {
                        Map.Entry<UUID, CacheEntry<T>> entry = iterator.next();

                        return new Map.Entry<>() {
                            @Override
                            public UUID getKey() {
                                return entry.getKey();
                            }

                            @Override
                            public T getValue() {
                                CacheEntry<T> value = entry.getValue();
                                return value == null ? null : value.value;
                            }

                            @Override
                            public T setValue(T value) {
                                CacheEntry<T> previous =
                                        entry.setValue(new CacheEntry<>(value));

                                return previous == null ? null : previous.value;
                            }

                            @Override
                            public boolean equals(Object obj) {
                                if (!(obj instanceof Map.Entry<?, ?> other)) {
                                    return false;
                                }

                                return Objects.equals(getKey(), other.getKey())
                                        && Objects.equals(getValue(), other.getValue());
                            }

                            @Override
                            public int hashCode() {
                                return Objects.hashCode(getKey())
                                        ^ Objects.hashCode(getValue());
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }

    @Override
    public int size() {
        return Math.toIntExact(cache.size());
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}

package me.jadenp.notbounties.features.settings.databases.cache;

public class CacheEntry<T> {
        final T value;
        volatile long lastAccessNanos;

        public CacheEntry(T value) {
            this.value = value;
            this.lastAccessNanos = System.nanoTime();
        }

}

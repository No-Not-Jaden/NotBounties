package me.jadenp.notbounties.utils;

/*
 * This class was generated with ChatGPT
 */

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.jadenp.notbounties.NotBounties;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class HttpSyncPool implements Closeable {
    private final org.apache.hc.client5.http.impl.classic.CloseableHttpClient client;
    /**
     * Bounded pool so we don't overwhelm the server or our JVM with too many runnables.
     */
    private final ExecutorService exec = new ThreadPoolExecutor(
            4, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024),
            r -> {
                Thread t = new Thread(r, "http-sync-pool");
                t.setDaemon(true);
                return t;
            });

    private final SimpleRateLimiter limiter;

    /**
     * @param requestsPerSecond steady-state RPS
     * @param burstCapacity     max instant permits available (small burst cushion)
     */
    public HttpSyncPool(double requestsPerSecond, int burstCapacity) {
        var cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .build();

        client = HttpClients.custom()
                .setConnectionManager(cm)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        limiter = new SimpleRateLimiter(requestsPerSecond, burstCapacity);
    }

    public CompletableFuture<String> requestPlayerNameAsync(UUID uuid, ResponseHandler handler) {
        // 1) Wait for a permit without blocking threads
        return limiter.acquireAsync()
                // 2) Then do the HTTP call on our bounded pool
                .thenApplyAsync(ignored -> {
                    var request = new org.apache.hc.client5.http.classic.methods.HttpGet(
                            "https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid);
                    try {
                        return client.execute(request, handler);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, exec)
                // 3) Safety timeout for the whole operation
                .orTimeout(7, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        limiter.close();
        exec.shutdownNow();
        try {
            client.close();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------
    // Lightweight async token-bucket limiter (no blocking threads)
    // ------------------------------------------------------------
    static final class SimpleRateLimiter implements Closeable {
        private final int burst;
        private final long tickNanos;
        private final ScheduledExecutorService scheduler;
        private final AtomicInteger tokens = new AtomicInteger(0);
        private final ConcurrentLinkedQueue<CompletableFuture<Void>> waiters = new ConcurrentLinkedQueue<>();

        /**
         * @param rps   permits per second (e.g., 5.0)
         * @param burst max stored permits (e.g., 10)
         */
        SimpleRateLimiter(double rps, int burst) {
            if (rps <= 0) throw new IllegalArgumentException("rps must be > 0");
            if (burst <= 0) throw new IllegalArgumentException("burst must be > 0");
            this.burst = burst;
            this.tickNanos = (long) (1_000_000_000L / rps);

            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "rate-limiter");
                t.setDaemon(true);
                return t;
            });

            // Add one permit per tick, up to burst; also drain queued waiters.
            scheduler.scheduleAtFixedRate(this::refillAndDrain, tickNanos, tickNanos, TimeUnit.NANOSECONDS);
        }

        public CompletableFuture<Void> acquireAsync() {
            // Fast path: consume a token immediately if available
            while (true) {
                int available = tokens.get();
                if (available > 0) {
                    if (tokens.compareAndSet(available, available - 1)) {
                        return CompletableFuture.completedFuture(null);
                    }
                } else {
                    // No tokens; enqueue a waiter and return its future.
                    CompletableFuture<Void> fut = new CompletableFuture<>();
                    waiters.add(fut);
                    // A concurrent refill might have just happened; try to drain quickly.
                    refillAndDrain();
                    return fut;
                }
            }
        }

        private void refillAndDrain() {
            // Refill one token per tick up to burst
            int current;
            do {
                current = tokens.get();
                if (current >= burst) break;
            } while (!tokens.compareAndSet(current, current + 1));

            // Serve queued waiters as long as tokens remain
            while (true) {
                CompletableFuture<Void> w = waiters.peek();
                if (w == null) break;

                int avail = tokens.get();
                if (avail <= 0) break;

                if (tokens.compareAndSet(avail, avail - 1)) {
                    waiters.poll();
                    w.complete(null);
                }
            }
        }

        @Override
        public void close() {
            scheduler.shutdownNow();
            // Best-effort: fail any remaining waiters on shutdown
            CompletableFuture<Void> w;
            while ((w = waiters.poll()) != null) {
                w.completeExceptionally(new CancellationException("Limiter closed"));
            }
        }
    }

    static class ResponseHandler implements HttpClientResponseHandler<String> {
        @Override
        public String handleResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {
            HttpEntity entity = classicHttpResponse.getEntity();
            if (entity != null) { // return it as a String
                String result = EntityUtils.toString(entity);
                classicHttpResponse.close();
                NotBounties.debugMessage("Received api player name.", false);
                JsonObject input;
                try {
                    input = new JsonParser().parse(result).getAsJsonObject();
                } catch (
                        JsonSyntaxException e) {
                    NotBounties.debugMessage("Bad Syntax.", false);
                    throw new IOException("Bad Syntax.");
                }
                if (input.has("name")) {
                    return input.get("name").getAsString();
                } else {
                    NotBounties.debugMessage(input.toString(), false);
                    throw new IOException("Invalid Response");
                }
            } else {
                throw new IOException("Did not get a result from request.");
            }
        }
    }
}





package me.jadenp.notbounties.ui;

import com.cjcrafter.foliascheduler.TaskImplementation;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.iso_renderer.IsometricRenderer;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.ConfigOptions;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class SkinManager {

    public interface SkinUpdateListener {
        void onSkinUpdate(UUID uuid);
    }
    private static final Map<UUID, PlayerSkin> savedSkins = new ConcurrentHashMap<>();
    private static final Cache<UUID, BufferedImage> isometricHeadCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private static final long MOJANG_API_LIMIT_MS = 2 * 60 * 1000L;
    private static final long MOJANG_API_LIMIT_COUNT = 200;
    private static long lastLimitCheck = System.currentTimeMillis();
    private static long refreshInterval = 86400000; // 24 hours in ms
    private static boolean useUnloadedPlayerProfile = true;
    /**
     * Time when the skin was loaded. Used for refreshing skins after the refreshInterval
     */
    private static final Map<UUID, Long> skinLoadTime = new ConcurrentHashMap<>();
    private static final List<SkinUpdateListener> updateListeners = Collections.synchronizedList(new ArrayList<>());
    private static BufferedImage missingSkinFace;
    private static BufferedImage missingSkinIso;
    private static PlayerSkin missingSkin = new PlayerSkin(null, true);
    private static final IsometricRenderer isoRenderer = new IsometricRenderer();
    private static final Set<UUID> pendingRequests = ConcurrentHashMap.newKeySet();

    public static void loadConfiguration(ConfigurationSection config) {
        refreshInterval = config.getLong("refresh-interval", 86400L) * 1000L;
        useUnloadedPlayerProfile = config.getBoolean("use-unloaded-player-profile", true);
        missingSkin = new PlayerSkin(config.getString("missing-skin-id"), true);
        savedSkins.put(DataManager.GLOBAL_SERVER_ID, missingSkin);
        missingSkinFace = getPlayerFace(DataManager.GLOBAL_SERVER_ID);
        missingSkinIso = getIsometricFace(DataManager.GLOBAL_SERVER_ID);
    }

    private static final List<Long> rateLimit = Collections.synchronizedList(new LinkedList<>()); // 200 requests / min
    private static final List<UUID> queuedRequests = Collections.synchronizedList(new LinkedList<>());

    private SkinManager(){}

    public static PlayerSkin getMissingSkin() {
        return missingSkin;
    }

    public static boolean isUseUnloadedPlayerProfile() {
        return useUnloadedPlayerProfile;
    }

    public static void refreshSkinRequests() {
        pendingRequests.clear();
        try {
            savedSkins.entrySet().removeIf(pair -> isMissingSkin(pair.getValue()) && !pair.getKey().equals(DataManager.GLOBAL_SERVER_ID)); // remove any skins that are set to missing
        } catch (ConcurrentModificationException ignored) {
            // skins are still being processed
            Bukkit.getLogger().warning("[NotBounties] Failed to refresh skin cache.");
            Bukkit.getLogger().warning("[NotBounties] Skins are currently being processed.");
            Bukkit.getLogger().warning("[NotBounties] Please try again.");
        }
    }

    /**
     * Save a player skin to the cache.
     * @param uuid UUID of the player to save the skin for.
     * @param playerSkin The skin to save.
     */
    public static void saveSkin(UUID uuid, PlayerSkin playerSkin) {
        savedSkins.put(uuid, playerSkin);
        skinLoadTime.put(uuid, System.currentTimeMillis());
        pendingRequests.remove(uuid);
        NotBounties.debugMessage("Saved player skin -> " + uuid + " as " + playerSkin.url(), false);
        synchronized (updateListeners) {
            updateListeners.forEach(listener -> listener.onSkinUpdate(uuid));
        }
    }

    public static void addUpdateListener(SkinUpdateListener listener) {
        updateListeners.add(listener);
    }

    public static void removeUpdateListener(SkinUpdateListener listener) {
        updateListeners.remove(listener);
    }

    /**
     * Check if a skin has been loaded. If a skin hasn't been loaded, a request will be made to load it.
     * @param uuid UUID of the player
     * @return Whether the skin is loaded and can be obtained with getSkin(UUID uuid)
     */
    public static boolean isSkinLoaded(UUID uuid) {
        if (savedSkins.containsKey(uuid)) {
            // check if the skin is missing
            if (isMissingSkin(savedSkins.get(uuid)) && !uuid.equals(DataManager.GLOBAL_SERVER_ID) && (System.currentTimeMillis() - skinLoadTime.getOrDefault(uuid, 0L) > refreshInterval)) {
                // Skin is missing. Send a request to load the skin again if the uuid isn't on a cooldown
                savedSkins.remove(uuid);
                saveSkin(uuid);
                return false;
            }
            // check if skin has expired
            if (refreshInterval > 0 && !uuid.equals(DataManager.GLOBAL_SERVER_ID)) {
                long loadTime = skinLoadTime.getOrDefault(uuid, 0L);
                if (System.currentTimeMillis() - loadTime > refreshInterval
                        && isSafeToRequest()) {
                    saveSkin(uuid);
                }

            }
            return true;
        }
        saveSkin(uuid);
        return false;
    }

    /**
     * Get the skin from a player. {@link #isSkinLoaded(UUID)} should be called before using this.
     * @param uuid UUID of a player
     * @return The player's skin information, or the information of a question mark skin if the skin isn't loaded.
     */
    public static PlayerSkin getSkin(UUID uuid) {
        if (!savedSkins.containsKey(uuid))
            return missingSkin;
        return savedSkins.get(uuid);
    }

    /**
     * Request a player skin from an api to be saved.
     *
     * @param uuid UUID of the player
     */
    private static void saveSkin(UUID uuid) {

        skinLoadTime.put(uuid, System.currentTimeMillis());
        requestSkin(uuid, true);
    }

    public static void requestSkin(UUID uuid, boolean firstAttempt) {
        if (!pendingRequests.contains(uuid) || !firstAttempt) {
            if (firstAttempt) {
                NotBounties.debugMessage("Attempting to save skin for: " + uuid, false);
            }
            pendingRequests.add(uuid);
            if (isSafeToRequest()) {
                SkinResponseHandler.webRequestSkin(uuid, firstAttempt);
            } else {
                NotBounties.debugMessage("Too many requests to save skins! Delaying some... ", false);
                requestLater(uuid);
            }
        }
    }

    private static boolean isSafeToRequest(){
        return getRecentRequestCount() < MOJANG_API_LIMIT_COUNT;
    }

    protected static int getRecentRequestCount() {
        if (System.currentTimeMillis() - lastLimitCheck > 5000) {
            lastLimitCheck = System.currentTimeMillis();
            long minKeepTime = lastLimitCheck - MOJANG_API_LIMIT_MS;
            synchronized (rateLimit) {
                rateLimit.removeIf(l -> l < minKeepTime);
            }
        }
        return rateLimit.size();
    }

    /**
     * Call this function when a 429 response code is returned from a request.
     * This means the internal rate limiting has failed, which is likely due to another plugin requesting from the api,
     * or the rate limit has changed.
     * This function automatically fills the rate limit.
     */
    protected static void failRateLimit() {
        long fillSize = MOJANG_API_LIMIT_COUNT;
        final long entryTime = System.currentTimeMillis(); // expires in 60 seconds
        if (rateLimit.size() < fillSize) {
            synchronized (rateLimit) {
                for (int i = 0; i < fillSize - rateLimit.size(); i++) {
                    rateLimit.add(entryTime);
                }
            }
        }
    }

    public static void removeOldData() {
        long currentTime = System.currentTimeMillis();
        long minKeepTime = currentTime - MOJANG_API_LIMIT_MS;
        synchronized (rateLimit) {
            rateLimit.removeIf(l -> l < minKeepTime); // remove times more than 1 minute ago
        }
        SkinResponseHandler.checkSleep();
    }

    /**
     * Close the http client used for getting skin requests
     */
    public static void shutdown() {
        SkinResponseHandler.closeClient();
    }

    protected static void requestLater(UUID uuid) {
        if (queuedRequests.isEmpty()) {
            queuedRequests.add(uuid);
            long nextRequestTime;
            if (rateLimit.isEmpty()) {
                nextRequestTime = 1000L;
            } else {
                // 10 seconds after the first item in the rate limit expires. (expires after 60 seconds)
                // minimum of 1 second in the future
                nextRequestTime = Math.max((10000 + MOJANG_API_LIMIT_MS - (System.currentTimeMillis() - rateLimit.get(0))) / 50, 1000L);
            }
            if (NotBounties.getInstance().isEnabled()) {
                NotBounties.getServerImplementation().async().runDelayed(() -> {
                    List<UUID> requests;
                    synchronized (queuedRequests) {
                        requests = new ArrayList<>(queuedRequests);
                        queuedRequests.clear();
                    }
                    for (UUID currentUUID : requests) {
                        pendingRequests.remove(currentUUID);
                        isSkinLoaded(currentUUID);
                    }
                }, nextRequestTime);
            }
        } else {
            queuedRequests.add(uuid);
        }
    }

    public static void incrementMojangRate() {
        rateLimit.add(System.currentTimeMillis());
    }

    public static void failRequest(@NotNull UUID uuid) {
        skinLoadTime.put(uuid, System.currentTimeMillis());
        savedSkins.computeIfAbsent(uuid, k -> missingSkin);
        pendingRequests.remove(uuid);
    }

    public static boolean isMissingSkin(PlayerSkin playerSkin) {
        return (Objects.equals(playerSkin.id(), missingSkin.id()) && playerSkin.url().equals(missingSkin.url())) || playerSkin.missing();
    }

    /**
     * Get the saved player face for this player.
     * @param uuid UUID of the player to get the face for.
     * @return A 8x8 face for the player include their mask.
     */
    public static BufferedImage getPlayerFace(UUID uuid) {
        if (!isSkinLoaded(uuid))
            return null;
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID) && missingSkinFace != null)
            return missingSkinFace;
        try {
            URL textureUrl = new URI(getSkin(uuid).url()).toURL();

            BufferedImage skin = ImageIO.read(textureUrl);
            BufferedImage head = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);

            return copyHead(skin, head);
        } catch (IOException | URISyntaxException e) {
            NotBounties.debugMessage("Error reading texture url for rendering player face.\n" + e, true);

        }
        return null;
    }

    /**
     * Copies the head and mask from a skin to the head image.
     * @param skin Skin image to copy from.
     * @param head Image to paste the head and mask to.
     * @return An image of the skin's face.
     */
    private static BufferedImage copyHead(BufferedImage skin, BufferedImage head) {
        try {
            // base head
            copySquare(skin, head, 8, 8);
            // mask
            copySquare(skin, head, 40, 8);
        } catch (IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning(e.toString());
            return null;
        }
        return head;
    }


    public static BufferedImage getIsometricFace(UUID uuid) {
        if (!isSkinLoaded(uuid))
            return null;
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID) && missingSkinIso != null)
            return missingSkinIso;
        BufferedImage cachedHead = isometricHeadCache.getIfPresent(uuid);
        if (cachedHead != null)
            return cachedHead;
        try {
            URL textureUrl = new URI(getSkin(uuid).url()).toURL();

            BufferedImage skin = ImageIO.read(textureUrl);
            BufferedImage head = isoRenderer.render(skin, 128, true);
            isometricHeadCache.put(uuid, head);
            return head;

        } catch (IOException | URISyntaxException e) {
            NotBounties.debugMessage("Error reading texture url for rendering isometric head.\n" + e, true);
        }
        return null;
    }

    private static void copySquare(BufferedImage from, BufferedImage to, int x, int y) {
        for (int x1 = 0; x1 < 8; x1++) {
            for (int y1 = 0; y1 < 8; y1++) {
                int color = from.getRGB(x + x1, y + y1);
                int a = (color >> 24) & 0xFF;
                if (a == 0)
                    continue;
                to.setRGB(x1, y1, color);
            }
        }
    }

    public static String getTextureURL(String texture) {
        String urlJson = new String(Base64.getDecoder().decode(texture));
        JsonObject urlInput = new JsonParser().parse(urlJson).getAsJsonObject();
        JsonElement skinURL = urlInput.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url");
        return skinURL.getAsString();
    }

    public static String getTextureId(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}

enum SkinType {
    JAVA, BEDROCK, SKINSRESTORER, USERNAME
}

record SkinRequestType(UUID uuid, SkinType skinType) {}

class SkinResponseHandler {
    private SkinResponseHandler() {}
    private static TaskImplementation<Void> client = null;
    private static final Queue<SkinRequestType> requestQueue = new ConcurrentLinkedQueue<>();

    private static long lastRequest = System.currentTimeMillis();
    private static final long SLEEP_TIME = 10 * 60 * 1000L; // 10 minutes

    /**
     * Checks if there hasn't been a request in a while. If so, it will close the http client.
     */
    public static void checkSleep() {
        if (client != null && System.currentTimeMillis() - lastRequest > SLEEP_TIME) {
            closeClient();
        }
    }

    public static void closeClient(){
        if (client != null) {
            client.cancel();
            client = null;
            NotBounties.debugMessage("Http client has been closed.", false);
        }
    }

    private static void createClient() {
        if (client != null || !NotBounties.getInstance().isEnabled())
            return;
        NotBounties.debugMessage("Creating new skin request client.", false);
        client = NotBounties.getServerImplementation().async().runAtFixedRate(() -> {
            if (requestQueue.isEmpty())
                return;
            UUID uuid = null;
            try (final CloseableHttpClient httpclient = HttpClients.custom().disableAutomaticRetries().build()) {
                SkinRequestType skinRequestType = requestQueue.poll();
                while (skinRequestType != null) {
                    uuid = skinRequestType.uuid();
                    PlayerSkin playerSkin = requestPlayerSkin(skinRequestType, httpclient);
                    // a null player skin means it was requested using skinsrestorer
                    if (playerSkin != null) {
                        SkinManager.saveSkin(uuid, playerSkin);
                    }
                    skinRequestType = requestQueue.poll();
                }
            } catch(IOException | IllegalStateException | RateLimitException e){
                // skin request fail
                if (uuid != null)
                    SkinManager.failRequest(uuid);
                NotBounties.debugMessage("Failed to request skin: " + uuid, true);
                NotBounties.debugMessage(e.toString(), true);
            }
            setLastRequest();
        }, 1, 3);
    }

    private static void failRateLimit(UUID uuid) {
        SkinManager.failRateLimit();
        SkinManager.requestLater(uuid);
        while (!requestQueue.isEmpty()) {
            SkinRequestType skinRequestType = requestQueue.poll();
            if (skinRequestType != null) {
                SkinManager.requestLater(skinRequestType.uuid());
            }
        }
    }

    private static @Nullable PlayerSkin requestPlayerSkin(SkinRequestType skinRequestType, CloseableHttpClient httpClient) throws IOException, RateLimitException {
        UUID uuid = skinRequestType.uuid();
        switch (skinRequestType.skinType()) {
            case JAVA -> {
                return saveJavaSkin(httpClient, uuid, true);
            }
            case BEDROCK -> {
                return saveGeyserSkin(httpClient, NotBounties.getXuid(uuid));
            }
            case USERNAME -> {
                try {
                    return saveNamedSkin(httpClient, uuid);
                } catch (IOException e) {
                    if (SkinManager.isUseUnloadedPlayerProfile()) {
                        // return default skin
                        return DefaultSkinHelper.get(uuid);
                    } else {
                        throw e;
                    }
                }
            }
            case SKINSRESTORER -> ConfigOptions.getIntegrations().getSkinsRestorerClass().saveSkin(uuid);
        }
        return null;
    }

    private static void setLastRequest() {
        lastRequest = System.currentTimeMillis();
    }

    protected static void webRequestSkin(UUID uuid, boolean firstAttempt){
        if (client == null)
            createClient();
        if (ConfigOptions.getIntegrations().isSkinsRestorerEnabled() && firstAttempt) {
            requestQueue.add(new SkinRequestType(uuid, SkinType.SKINSRESTORER));
        } else if (NotBounties.isBedrockPlayer(uuid)) {
            requestQueue.add(new SkinRequestType(uuid, SkinType.BEDROCK));
        } else {
            if (uuid.version() == 4)
                requestQueue.add(new SkinRequestType(uuid, SkinType.JAVA));
            else
                requestQueue.add(new SkinRequestType(uuid, SkinType.USERNAME));
        }
    }

    private static PlayerSkin saveGeyserSkin(CloseableHttpClient httpClient, String xUID) throws IOException {
        NotBounties.debugMessage("Attempting to get bedrock skin from xuid: " + xUID, false);

        final HttpGet request = new HttpGet("https://api.geysermc.org/v2/skin/" + xUID);

        return httpClient.execute(request, response -> {
            StatusLine statusLine = new StatusLine(response);
            NotBounties.debugMessage(request + "->" + new StatusLine(response), false);

            if (statusLine.getStatusCode() == 200 && response.getEntity() != null) {
                String text = EntityUtils.toString(response.getEntity());
                JsonObject input = new JsonParser().parse(text).getAsJsonObject();
                if (!input.has("value") || !input.has("texture_id")) {
                    throw new IOException("Missing value or texture_id from bedrock skin response. (" + text + ")");
                }
                String value = input.get("value").getAsString();
                String textureID = SkinManager.getTextureId(SkinManager.getTextureURL(value));
                String id = input.get("texture_id").getAsString();

                return new PlayerSkin(textureID, false);
            } else {
                throw new IOException("Couldn't get a skin from this xuid.");
            }

        });
    }

    private static PlayerSkin saveJavaSkin(CloseableHttpClient httpClient, UUID requestUUID, boolean tryNamed) throws IOException, RateLimitException {
        SkinManager.incrementMojangRate();

        final HttpGet request = new HttpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + requestUUID);

        return httpClient.execute(request, response -> {
            StatusLine statusLine = new StatusLine(response);
            NotBounties.debugMessage(request + "->" + statusLine, false);
            if (statusLine.getStatusCode() == 200 && response.getEntity() != null) {
                String text = EntityUtils.toString(response.getEntity());
                JsonObject input = new JsonParser().parse(text).getAsJsonObject();
                JsonObject textureProperty = input.get("properties").getAsJsonArray().get(0).getAsJsonObject();
                String value = textureProperty.get("value").getAsString();
                String textureID = SkinManager.getTextureId(SkinManager.getTextureURL(value));
                String id = input.get("id").getAsString(); // this is the player's uuid

                return new PlayerSkin(textureID, false);
            } else if (statusLine.getStatusCode() == 429) {
                // hit rate limit
                failRateLimit(requestUUID);
                throw new RateLimitException("Exceeding Mojang rate limit with: " + requestUUID + "\n Recorded rate: " + SkinManager.getRecentRequestCount());
            } else {
                if (tryNamed) {
                    NotBounties.debugMessage("Saving named skin", false);
                    return saveNamedSkin(httpClient, requestUUID);
                }
                throw new IOException("Failed to save java skin for: " + requestUUID);
            }
        });
    }


    private static PlayerSkin saveNamedSkin(CloseableHttpClient httpClient, UUID uuid) throws IOException, RateLimitException {
        SkinManager.incrementMojangRate();
        String playerName = LoggedPlayers.getPlayerName(uuid);
        if (playerName.length() > 24) {
            // not a valid length
            throw new IOException("Recorded player name is not of a valid length. (May not be recorded): " + uuid);
        }

        final HttpGet request = new HttpGet("https://api.mojang.com/users/profiles/minecraft/" + playerName);

        return httpClient.execute(request, response -> {
            StatusLine statusLine = new StatusLine(response);
            NotBounties.debugMessage(request + "->" + statusLine, false);
            if (statusLine.getStatusCode() == 429) {
                // hit rate limit
                failRateLimit(uuid);
                throw new RateLimitException("Exceeding Mojang rate limit with: " + playerName + "\n Recorded rate: " + SkinManager.getRecentRequestCount());
            }
            if (statusLine.getStatusCode() != 200 || response.getEntity() == null) {
                throw new IOException("Failed to get skin for " + playerName + ": Null contents");
            }

            String text = EntityUtils.toString(response.getEntity());
            JsonObject input;
            try {
                input = new JsonParser().parse(text).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                throw new IOException("Failed to get skin for " + playerName + ": Syntax Error.", e);
            }
            if (input.has("errorMessage") && !input.get("errorMessage").isJsonNull()) {
                throw new IOException("Failed to get skin for " + playerName + ": " + input.get("errorMessage").getAsString());
            }

            if (input.has("id")) {
                // convert uuid without dashes to one with
                // https://stackoverflow.com/questions/18986712/creating-a-uuid-from-a-string-with-no-dashes
                UUID onlineUUID = java.util.UUID.fromString(
                        input.get("id").getAsString()
                                .replaceFirst(
                                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                                )
                );
                return saveJavaSkin(httpClient, onlineUUID, false);
            } else {
                throw new IOException("Failed to get skin for " + playerName + ": id not present.");
            }
        });
    }
}

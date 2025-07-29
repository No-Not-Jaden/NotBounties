package me.jadenp.notbounties.ui;

import com.cjcrafter.foliascheduler.TaskImplementation;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.ConfigOptions;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SkinManager {
    private static final Map<UUID, PlayerSkin> savedSkins = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> requestCooldown = new ConcurrentHashMap<>();
    private static final long REQUEST_FAIL_TIMEOUT = 60000 * 30L; // 30 min
    private static final long CONCURRENT_REQUEST_INTERVAL = 10000;
    private static final String MISSING_SKIN_TEXTURE = "https://textures.minecraft.net/texture/b6e0dfed46c33023110e295b177c623fd36b39e4137aeb7241777064af7a0b57";
    private static final String MISSING_SKIN_ID = "46ba63344f49dd1c4f5488e926bf3d9e2b29916a6c50d610bb40a5273dc8c82";
    private static final BufferedImage MISSING_SKIN_FACE;
    private static final PlayerSkin missingSkin;

    static {
        try {
            missingSkin = new PlayerSkin(new URI(MISSING_SKIN_TEXTURE).toURL(), MISSING_SKIN_ID);
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        savedSkins.put(DataManager.GLOBAL_SERVER_ID, missingSkin);
        MISSING_SKIN_FACE = getPlayerFace(DataManager.GLOBAL_SERVER_ID);
    }

    private static final List<Long> rateLimit = new ArrayList<>(); // 200 requests / min
    private static final List<UUID> queuedRequests = new ArrayList<>();

    private SkinManager(){}

    public static void refreshSkinRequests() {
        requestCooldown.clear(); // clear request times
        try {
            savedSkins.entrySet().removeIf(pair -> isMissingSkin(pair.getValue()) && !pair.getKey().equals(DataManager.GLOBAL_SERVER_ID)); // remove any skins that are set to missing
        } catch (ConcurrentModificationException ignored) {
            // skins are still being processed
            Bukkit.getLogger().warning("[NotBounties] Failed to refresh skin cache.");
            Bukkit.getLogger().warning("[NotBounties] Skins are currently being processed.");
            Bukkit.getLogger().warning("[NotBounties] Please try again.");
        }
    }

    public static void saveSkin(UUID uuid, PlayerSkin playerSkin) {
        savedSkins.put(uuid, playerSkin);
        NotBounties.debugMessage("Saved player skin -> " + uuid, false);
    }

    /**
     * Check if a skin has been loaded. If a skin hasn't been loaded, a request will be made to load it.
     * @param uuid UUID of the player
     * @return Whether the skin is loaded and can be obtained with getSkin(UUID uuid)
     */
    public static boolean isSkinLoaded(UUID uuid) {
        if (savedSkins.containsKey(uuid)) {
            // check if the skin is missing
            if (isMissingSkin(savedSkins.get(uuid)) && !uuid.equals(DataManager.GLOBAL_SERVER_ID) && (!requestCooldown.containsKey(uuid) || requestCooldown.get(uuid) < System.currentTimeMillis())) {
                // Skin is missing. Send a request to load the skin again if the uuid isn't on a cooldown
                savedSkins.remove(uuid);
                saveSkin(uuid);
                return false;
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
    public static void saveSkin(UUID uuid) {
        if (requestCooldown.containsKey(uuid) && System.currentTimeMillis() < requestCooldown.get(uuid)) {
            return;
        }

        requestCooldown.put(uuid, System.currentTimeMillis() + CONCURRENT_REQUEST_INTERVAL);
        NotBounties.debugMessage("Attempting to save skin for: " + uuid, false);
        if (savedSkins.containsKey(uuid))
            return;
        requestSkin(uuid, true);
    }

    public static void requestSkin(UUID uuid, boolean firstAttempt) {
        if (isSafeToRequest()) {
            SkinResponseHandler.webRequestSkin(uuid, firstAttempt);
        } else {
            NotBounties.debugMessage("Too many requests to save skins! Delaying some... ", false);
            requestLater(uuid);
        }
    }

    private static boolean isSafeToRequest(){
        List<Long> rateLimitCopy = new ArrayList<>(rateLimit);
        // remove on main thread
        // remove times more than 1 minute ago
        NotBounties.getServerImplementation().global().run(() -> rateLimit.removeIf(l -> l < System.currentTimeMillis() - 60000));
        rateLimitCopy.removeIf(l -> l < System.currentTimeMillis() - 60000);
        if (rateLimitCopy.size() < 66)
            return true;
        long threshold = System.currentTimeMillis() - 5000;
        int numRecent = (int) rateLimitCopy.stream().mapToLong(l -> l).filter(l -> l > threshold).count(); // get number of requests less than 5 seconds ago
        return rateLimitCopy.size() < 190 && rateLimitCopy.size() + numRecent < 200;
    }

    public static void removeOldData() {
        long currentTime = System.currentTimeMillis();
        rateLimit.removeIf(l -> l < currentTime - 60000); // remove times more than 1 minute ago
        requestCooldown.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        SkinResponseHandler.checkSleep();
    }

    /**
     * Close the http client used for getting skin requests
     */
    public static void shutdown() {
        SkinResponseHandler.closeClient();
    }

    private static void requestLater(UUID uuid) {
        if (queuedRequests.isEmpty() && !rateLimit.isEmpty()) {
            queuedRequests.add(uuid);
            long nextRequestTime = (70000 - (System.currentTimeMillis() - rateLimit.get(0))) / 50; // 10 seconds after the first item in the rate limit expires. (expires after 60 seconds)
            NotBounties.getServerImplementation().async().runDelayed(() -> {
                List<UUID> requests = new ArrayList<>(queuedRequests);
                queuedRequests.clear();
                for (UUID currentUUID : requests)
                    isSkinLoaded(currentUUID);
            }, nextRequestTime);
        } else {
            queuedRequests.add(uuid);
        }
    }

    public static void incrementMojangRate() {
        NotBounties.getServerImplementation().global().run(() -> rateLimit.add(System.currentTimeMillis()));
    }

    public static void failRequest(@NotNull UUID uuid) {
        requestCooldown.put(uuid, System.currentTimeMillis() + REQUEST_FAIL_TIMEOUT);
        savedSkins.computeIfAbsent(uuid, u -> missingSkin);
    }

    public static boolean isMissingSkin(PlayerSkin playerSkin) {
        return Objects.equals(playerSkin.id(), missingSkin.id()) && playerSkin.url() == missingSkin.url();
    }

    /**
     * Get the saved player face for this player.
     * @param uuid UUID of the player to get the face for.
     * @return A 8x8 face for the player include their mask.
     */
    public static BufferedImage getPlayerFace(UUID uuid) {
        if (!isSkinLoaded(uuid))
            return null;
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID) && MISSING_SKIN_FACE != null)
            return MISSING_SKIN_FACE;
        try {
            URL textureUrl = getSkin(uuid).url();

            BufferedImage skin = ImageIO.read(textureUrl);
            BufferedImage head = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
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

        } catch (IOException e) {
            NotBounties.debugMessage("Error reading texture url for bounty poster.\n" + e, true);

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

    public static URL getTextureURL(String texture) {
        try {
            String urlJson = new String(Base64.getDecoder().decode(texture));
            JsonObject urlInput = new JsonParser().parse(urlJson).getAsJsonObject();
            JsonElement skinURL = urlInput.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url");
            return new URI(skinURL.getAsString()).toURL();
        } catch (IOException | URISyntaxException e) {
            // too many requests
            NotBounties.debugMessage("Error getting texture url: " + texture, true);
            return null;
        }
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
        if (client != null)
            return;
        NotBounties.debugMessage("Creating new skin request client.", false);
        client = NotBounties.getServerImplementation().async().runAtFixedRate(() -> {
            if (requestQueue.isEmpty())
                return;
            UUID uuid = null;
            try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
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
            } catch(IOException | IllegalStateException e){
                // skin request fail
                if (uuid != null)
                    SkinManager.failRequest(uuid);
                NotBounties.debugMessage("Failed to request skin: " + uuid, true);
                NotBounties.debugMessage(e.toString(), true);
            }
            setLastRequest();
        }, 1, 3);
    }

    private static @Nullable PlayerSkin requestPlayerSkin(SkinRequestType skinRequestType, CloseableHttpClient httpClient) throws IOException {
        UUID uuid = skinRequestType.uuid();
        switch (skinRequestType.skinType()) {
            case JAVA -> {
                return saveJavaSkin(httpClient, uuid, true);
            }
            case BEDROCK -> {
                return saveGeyserSkin(httpClient, NotBounties.getXuid(uuid));
            }
            case USERNAME -> {
                return saveNamedSkin(httpClient, uuid);
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
                String id = input.get("texture_id").getAsString();

                return new PlayerSkin(SkinManager.getTextureURL(value), id);
            } else {
                throw new IOException("Couldn't get a skin from this xuid.");
            }

        });
    }

    private static PlayerSkin saveJavaSkin(CloseableHttpClient httpClient, UUID requestUUID, boolean tryNamed) throws IOException {
        SkinManager.incrementMojangRate();

        final HttpGet request = new HttpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + requestUUID);

        return httpClient.execute(request, response -> {
            StatusLine statusLine = new StatusLine(response);
            NotBounties.debugMessage(request + "->" + new StatusLine(response), false);
            if (statusLine.getStatusCode() == 200 && response.getEntity() != null) {
                String text = EntityUtils.toString(response.getEntity());
                JsonObject input = new JsonParser().parse(text).getAsJsonObject();
                JsonObject textureProperty = input.get("properties").getAsJsonArray().get(0).getAsJsonObject();
                String value = textureProperty.get("value").getAsString();
                String id = input.get("id").getAsString();

                return new PlayerSkin(SkinManager.getTextureURL(value), id);
            } else {
                if (tryNamed) {
                    NotBounties.debugMessage("Saving named skin", false);
                    return saveNamedSkin(httpClient, requestUUID);
                }
                throw new IOException("Failed to save java skin for: " + requestUUID);
            }
        });
    }


    private static PlayerSkin saveNamedSkin(CloseableHttpClient httpClient, UUID uuid) throws IOException {
        SkinManager.incrementMojangRate();
        String playerName = LoggedPlayers.getPlayerName(uuid);
        if (playerName.length() > 24) {
            // not a valid length
            throw new IOException("Recorded player name is not of a valid length. (May not be recorded): " + uuid);
        }

        final HttpGet request = new HttpGet("https://api.mojang.com/users/profiles/minecraft/" + playerName);

        return httpClient.execute(request, response -> {
            NotBounties.debugMessage(request + "->" + new StatusLine(response), false);
            if (response.getEntity() == null) {
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

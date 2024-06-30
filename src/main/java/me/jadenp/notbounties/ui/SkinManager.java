package me.jadenp.notbounties.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SkinManager {
    private static final Map<UUID, PlayerSkin> savedSkins = new HashMap<>();
    private static final Map<UUID, BufferedImage> savedFaces = new HashMap<>();
    private static final Map<UUID, Long> requestCooldown = new HashMap<>();
    private static final long REQUEST_FAIL_TIMEOUT = 60000 * 30L; // 30 min
    private static final long CONCURRENT_REQUEST_INTERVAL = 10000;
    private static final PlayerSkin missingSkin;
    private static final List<Long> rateLimit = new ArrayList<>(); // 200 requests / min
    private static final List<UUID> queuedRequests = new ArrayList<>();

    static {
        try {
            missingSkin = new PlayerSkin(new URL("http://textures.minecraft.net/texture/b6e0dfed46c33023110e295b177c623fd36b39e4137aeb7241777064af7a0b57"), "46ba63344f49dd1c4f5488e926bf3d9e2b29916a6c50d610bb40a5273dc8c82");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        savedSkins.put(new UUID(0,0), missingSkin); // console
    }

    private SkinManager(){}

    public static void refreshSkinRequests() {
        requestCooldown.clear(); // clear request times
        savedSkins.entrySet().removeIf(pair -> isMissingSkin(pair.getValue())); // remove any skins that are set to missing
    }

    public static void saveSkin(UUID uuid, PlayerSkin playerSkin) {
        savedSkins.put(uuid, playerSkin);
    }

    /**
     * Check if a skin has been loaded. If a skin hasn't been loaded, a request will be made to load it.
     * @param uuid UUID of the player
     * @return Whether the skin is loaded and can be obtained with getSkin(UUID uuid)
     */
    public static boolean isSkinLoaded(UUID uuid) {
        if (savedSkins.containsKey(uuid)) {
            // check if the skin is missing
            if (isMissingSkin(savedSkins.get(uuid)) && (!requestCooldown.containsKey(uuid) || requestCooldown.get(uuid) < System.currentTimeMillis())) {
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
     * Get the skin from a player. isSkinLoaded(UUID uuid) should be called before using this.
     * @param uuid UUID of a player
     * @return The player's skin information.
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
        new BukkitRunnable() {
            @Override
            public void run() {
                if (savedSkins.containsKey(uuid))
                    return;
                if (ConfigOptions.skinsRestorerEnabled) {
                    ConfigOptions.skinsRestorerClass.saveSkin(uuid);
                    return;
                }
                if (ConfigOptions.floodgateEnabled) {
                    FloodGateClass floodGateClass = new FloodGateClass();
                    if (floodGateClass.isBedrockPlayer(uuid)) {
                        floodGateClass.saveSkin(uuid);
                        return;
                    }
                }



                if (isSafeToRequest()) {
                    try {
                        new SkinResponseHandler(uuid).saveJavaSkin();
                    } catch (ExecutionException | InterruptedException e) {
                        NotBounties.debugMessage("[NotBountiesDebug] Could not save a java skin for: " + NotBounties.getPlayerName(uuid), true);
                        NotBounties.debugMessage(e.toString(), true);
                    }
                } else {
                    requestLater(uuid);
                }
            }
        }.runTaskAsynchronously(NotBounties.getInstance());
    }

    private static boolean isSafeToRequest(){
        rateLimit.removeIf(l -> l < System.currentTimeMillis() - 60000); // remove times more than 1 minute ago
        if (rateLimit.size() < 66)
            return true;
        long threshold = System.currentTimeMillis() - 5000;
        int numRecent = (int) rateLimit.stream().mapToLong(l -> l).filter(l -> l > threshold).count(); // get number of requests less than 5 seconds ago
        return rateLimit.size() < 190 && rateLimit.size() + numRecent < 200;
    }

    private static void requestLater(UUID uuid) {
        if (queuedRequests.isEmpty() && !rateLimit.isEmpty()) {
            queuedRequests.add(uuid);
            long nextRequestTime = 70000 - (System.currentTimeMillis() - rateLimit.get(0)); // 10 seconds after the first item in the rate limit expires.
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<UUID> requests = new ArrayList<>(queuedRequests);
                    queuedRequests.clear();
                    for (UUID currentUUID : requests)
                        isSkinLoaded(currentUUID);
                }
            }.runTaskLaterAsynchronously(NotBounties.getInstance(), nextRequestTime);
        } else {
            queuedRequests.add(uuid);
        }
    }

    public static void incrementMojangRate() {
        rateLimit.add(System.currentTimeMillis());
    }

    public static void failRequest(UUID uuid) {
        requestCooldown.put(uuid, System.currentTimeMillis() + REQUEST_FAIL_TIMEOUT);
        savedSkins.computeIfAbsent(uuid, u -> missingSkin);
    }

    public static boolean isMissingSkin(PlayerSkin playerSkin) {
        return Objects.equals(playerSkin.getId(), missingSkin.getId()) && playerSkin.getUrl() == missingSkin.getUrl();
    }



    public static BufferedImage getPlayerFace(UUID uuid) {
        if (savedFaces.containsKey(uuid))
            return savedFaces.get(uuid);
        if (!isSkinLoaded(uuid))
            return null;
        try {
            URL textureUrl = getSkin(uuid).getUrl();

            BufferedImage skin = ImageIO.read(textureUrl);
            BufferedImage head = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            try {
                // base head
                copySquare(skin, head, 8, 8);
                // mask
                copySquare(skin, head, 40, 8);
            } catch (IndexOutOfBoundsException e) {
                Bukkit.getLogger().warning(e.toString());
                return null;
            }
            savedFaces.put(uuid, head);
            return head;

        } catch (IOException e) {
            if (NotBounties.debug)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getLogger().warning("[NotBountiesDebug] Error reading texture url for bounty poster.");
                        Bukkit.getLogger().warning(e.toString());
                    }
                }.runTask(NotBounties.getInstance());

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
                for (int x2 = 0; x2 < 8; x2++) {
                    for (int y2 = 0; y2 < 8; y2++) {
                        to.setRGB(x1 * 8 + x2, y1 * 8 + y2, color);
                    }
                }

            }
        }
    }

    public static URL getTextureURL(String texture) {
        try {
            String urlJson = new String(Base64.getDecoder().decode(texture));
            JsonObject urlInput = NotBounties.serverVersion >= 18 ? JsonParser.parseString(urlJson).getAsJsonObject() : new JsonParser().parse(urlJson).getAsJsonObject();
            JsonElement skinURL = urlInput.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url");

            return new URI(skinURL.getAsString()).toURL();
        } catch (IOException | URISyntaxException e) {
            // too many requests
            return null;
        }
    }
}

class SkinResponseHandler {
    private final UUID uuid;
    public SkinResponseHandler(UUID uuid){
        this.uuid = uuid;
    }

    public void saveJavaSkin() throws ExecutionException, InterruptedException {
        if (uuid.version() == 4)
            saveSkin(uuid, true);
        else
            saveNamedSkin();
    }

    private void saveSkin(UUID requestUUID, boolean tryNamed) throws ExecutionException, InterruptedException{
        SkinManager.incrementMojangRate();
        final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build();

        final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .build();

        client.start();

        final SimpleHttpRequest request = SimpleRequestBuilder.get("https://sessionserver.mojang.com/session/minecraft/profile/" + requestUUID)
                .build();

        final Future<SimpleHttpResponse> future = client.execute(
                SimpleRequestProducer.create(request),
                SimpleResponseConsumer.create(),
                new FutureCallback<SimpleHttpResponse>() {

                    @Override
                    public void completed(final SimpleHttpResponse response) {
                        StatusLine statusLine = new StatusLine(response);
                        NotBounties.debugMessage(request + "->" + new StatusLine(response), false);
                        if (statusLine.getStatusCode() == 200) {
                            JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseString(response.getBodyText()).getAsJsonObject() : new JsonParser().parse(response.getBodyText()).getAsJsonObject();
                            JsonObject textureProperty = input.get("properties").getAsJsonArray().get(0).getAsJsonObject();
                            String value = textureProperty.get("value").getAsString();
                            String id = input.get("id").getAsString();
                            SkinManager.saveSkin(uuid, new PlayerSkin(SkinManager.getTextureURL(value), id));
                            if (!requestUUID.equals(uuid))
                                SkinManager.saveSkin(requestUUID,  new PlayerSkin(SkinManager.getTextureURL(value), id));
                        } else {
                            failed(new IllegalArgumentException("Couldn't get profile from this uuid."));
                        }
                    }

                    @Override
                    public void failed(final Exception ex) {
                        NotBounties.debugMessage(request + "->" + ex, true);
                        if (tryNamed) {
                            NotBounties.debugMessage("Saving named skin", false);
                            try {
                                saveNamedSkin();
                            } catch (ExecutionException | InterruptedException e) {
                                NotBounties.debugMessage("Failed to save named skin.", true);
                            }
                        }
                    }

                    @Override
                    public void cancelled() {
                        NotBounties.debugMessage(request + " cancelled", true);
                    }

                });
        future.get();
        client.close(CloseMode.GRACEFUL);
    }

    private void saveNamedSkin() throws ExecutionException, InterruptedException {
        SkinManager.incrementMojangRate();
        String playerName = NotBounties.getPlayerName(uuid);
        final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build();

        final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .build();

        client.start();

        final SimpleHttpRequest request = SimpleRequestBuilder.get("https://api.mojang.com/users/profiles/minecraft/" + playerName)
                .build();

        final Future<SimpleHttpResponse> future = client.execute(
                SimpleRequestProducer.create(request),
                SimpleResponseConsumer.create(),
                new FutureCallback<SimpleHttpResponse>() {

                    @Override
                    public void completed(final SimpleHttpResponse response) {
                        NotBounties.debugMessage(request + "->" + new StatusLine(response), false);
                        JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseString(response.getBodyText()).getAsJsonObject() : new JsonParser().parse(response.getBodyText()).getAsJsonObject();
                        if (input.has("errorMessage") && !input.get("errorMessage").isJsonNull()) {
                            failed(new InterruptedException("Failed to get skin for " + playerName + ": " + input.get("errorMessage").getAsString()));
                            return;
                        }

                        // convert uuid without dashes to one with
                        // https://stackoverflow.com/questions/18986712/creating-a-uuid-from-a-string-with-no-dashes
                        UUID onlineUUID =  java.util.UUID.fromString(
                                input.get("id").getAsString()
                                        .replaceFirst(
                                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                                        )
                        );
                        try {
                            saveSkin(onlineUUID, false);
                        } catch (ExecutionException | InterruptedException e) {
                            NotBounties.debugMessage("Failed to get skin for " + playerName, true);
                            failed(e);
                        }
                    }

                    @Override
                    public void failed(final Exception ex) {
                        NotBounties.debugMessage(request + "->" + ex, true);
                        SkinManager.failRequest(uuid);
                    }

                    @Override
                    public void cancelled() {
                        NotBounties.debugMessage(request + " cancelled", true);
                    }

                });
        future.get();
        client.close(CloseMode.GRACEFUL);
    }
}

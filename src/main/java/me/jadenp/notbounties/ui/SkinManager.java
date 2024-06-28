package me.jadenp.notbounties.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class SkinManager {
    private static final Map<UUID, PlayerSkin> savedSkins = new HashMap<>();
    private static final Map<UUID, BufferedImage> savedFaces = new HashMap<>();
    private static final Map<UUID, Long> requestTimes = new HashMap<>();
    private static final long requestInterval = 60000 * 30; // 30 min
    private static final long requestTimeout = 10000;
    private static final PlayerSkin missingSkin;

    static {
        try {
            missingSkin = new PlayerSkin(new URL("http://textures.minecraft.net/texture/b6e0dfed46c33023110e295b177c623fd36b39e4137aeb7241777064af7a0b57"), "46ba63344f49dd1c4f5488e926bf3d9e2b29916a6c50d610bb40a5273dc8c82");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        savedSkins.put(new UUID(0,0), missingSkin); // console
    }

    public static void refreshSkinRequests() {
        requestTimes.clear(); // clear request times
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
            if (requestTimes.containsKey(uuid) && System.currentTimeMillis() - requestTimes.get(uuid) > requestInterval && isMissingSkin(savedSkins.get(uuid))) {
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
        if (requestTimes.containsKey(uuid)) {
            if (System.currentTimeMillis() - requestTimes.get(uuid) < requestTimeout)
                return;
            if (!savedSkins.containsKey(uuid)) {
                savedSkins.put(uuid, missingSkin);
                if (NotBounties.debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Did not receive a skin for " + NotBounties.getPlayerName(uuid));
            }
            return;
        }
        requestTimes.put(uuid, System.currentTimeMillis());
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

                try {
                    saveJavaSkin(uuid);
                } catch (Exception e) {
                    if (NotBounties.debug) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getLogger().warning("[NotBountiesDebug] Could not save a java skin for: " + NotBounties.getPlayerName(uuid));
                                Bukkit.getLogger().warning(e.toString());
                                Bukkit.getLogger().warning("[NotBountiesDebug] Attempting to load a skin from username.");
                            }
                        }.runTask(NotBounties.getInstance());
                    }
                    try {
                        saveNamedSkin(uuid, NotBounties.getPlayerName(uuid));
                    } catch (Exception e2) {
                        if (NotBounties.debug) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Bukkit.getLogger().warning("[NotBountiesDebug] Could not save a named skin for: " + NotBounties.getPlayerName(uuid));
                                    Bukkit.getLogger().warning(e2.toString());
                                    Bukkit.getLogger().warning("[NotBountiesDebug] Replacing with a missing skin.");
                                    saveSkin(uuid, missingSkin);
                                }
                            }.runTask(NotBounties.getInstance());
                        }
                    }
                }
            }
        }.runTaskAsynchronously(NotBounties.getInstance());
    }

    public static boolean isMissingSkin(PlayerSkin playerSkin) {
        return Objects.equals(playerSkin.getId(), missingSkin.getId()) && playerSkin.getUrl() == missingSkin.getUrl();
    }

    private static void saveJavaSkin(UUID uuid) throws Exception {

            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonParser().parse(reader).getAsJsonObject();
            JsonObject textureProperty = input.get("properties").getAsJsonArray().get(0).getAsJsonObject();
            reader.close();
            String value = textureProperty.get("value").getAsString();
            String id = input.get("id").getAsString();

            SkinManager.saveSkin(uuid, new PlayerSkin(getTextureURL(value), id));
    }

    public static void saveNamedSkin(UUID originalUUID, String playerName) throws Exception {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
        InputStreamReader reader = new InputStreamReader(url.openStream());
        JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonParser().parse(reader).getAsJsonObject();
        reader.close();
        if (input.has("errorMessage") && !input.get("errorMessage").isJsonNull()) {
            throw new IOException(input.get("errorMessage").getAsString());
        }

        // convert uuid without dashes to one with
        // https://stackoverflow.com/questions/18986712/creating-a-uuid-from-a-string-with-no-dashes
        UUID uuid =  java.util.UUID.fromString(
                input.get("id").getAsString()
                        .replaceFirst(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                        )
        );


        URL url2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        InputStreamReader reader2 = new InputStreamReader(url2.openStream());
        JsonObject input2 = NotBounties.serverVersion >= 18 ? JsonParser.parseReader(reader2).getAsJsonObject() : new JsonParser().parse(reader2).getAsJsonObject();
        JsonObject textureProperty = input2.get("properties").getAsJsonArray().get(0).getAsJsonObject();
        reader2.close();
        String value = textureProperty.get("value").getAsString();
        String id = input2.get("id").getAsString();

        SkinManager.saveSkin(originalUUID, new PlayerSkin(getTextureURL(value), id));
    }

    public static BufferedImage getPlayerFace(UUID uuid) {
        if (savedFaces.containsKey(uuid))
            return savedFaces.get(uuid);
        if (!isSkinLoaded(uuid))
            return null;
        try {
            URL textureUrl = getSkin(uuid).getUrl();
            //Bukkit.getLogger().info("URL: " + textureUrl);

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
            return new URL(skinURL.getAsString());
        } catch (IOException e) {
            //Bukkit.getLogger().warning(e.toString());
            // too many requests
            return null;
        }
    }
}

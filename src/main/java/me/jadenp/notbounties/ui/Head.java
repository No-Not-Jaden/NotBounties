package me.jadenp.notbounties.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.externalAPIs.HeadDataBaseClass;
import me.jadenp.notbounties.utils.externalAPIs.SkinsRestorerClass;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;

public class Head {
    private static Map<UUID, String> savedTextureIDs = new HashMap<>();
    public static ItemStack createPlayerSkull(String data){

        ItemStack item = null;
        if (usingBase64(data)){
            item = new ItemStack(Material.PLAYER_HEAD);
            if (NotBounties.serverVersion >= 18) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                assert meta != null;
                meta.setOwnerProfile(createProfile(data));
                item.setItemMeta(meta);
            } else {
                return Bukkit.getUnsafe().modifyItemStack(item, "{display:{Name:\"{\\\"text\\\":\\\"Head\\\"}\"},SkullOwner:{Id:[" + "I;1201296705,1414024019,-1385893868,1321399054" + "],Properties:{textures:[{Value:\"" + data + "\"}]}}}");
            }
        } else if (HDBEnabled){
            return (new HeadDataBaseClass().getHead(data));
        } else {
            Bukkit.getLogger().warning("[NotBounties] Could not create custom head. Use Base64 or install HeadDataBase.");
        }
        return item;
    }

    public static ItemStack createPlayerSkull(UUID uuid) {
        if (skinsRestorerEnabled) {
            ItemStack head = new SkinsRestorerClass().getPlayerHead(uuid);
            if (head != null)
                return head;
        }
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        } catch (NullPointerException e) {
            if (NotBounties.serverVersion >= 18) {
                PlayerProfile profile = Bukkit.createPlayerProfile(uuid, NotBounties.getPlayerName(uuid));
                profile.setTextures(Bukkit.getOfflinePlayer(uuid).getPlayerProfile().getTextures());
                meta.setOwnerProfile(profile);
            } else {
               Bukkit.getLogger().warning("[NotBounties] No supported way to get player texture!");
            }
        }
        head.setItemMeta(meta);
        return head;
    }

    public static URL getTextureURL(UUID uuid) {
        try {
            String texture = getTextureValue(uuid).get();
            String urlJson = new String(Base64.getDecoder().decode(texture));
            JsonObject urlInput = NotBounties.serverVersion >= 18 ? JsonParser.parseString(urlJson).getAsJsonObject() : new JsonParser().parse(urlJson).getAsJsonObject();
            JsonElement skinURL = urlInput.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url");
            return new URL(skinURL.getAsString());
        } catch (IOException | ExecutionException | InterruptedException e) {
            Bukkit.getLogger().warning(e.toString());
            return null;
        }
    }

    public static CompletableFuture<String> getTextureValue(UUID uuid) {
        if (floodgateEnabled) {
            FloodGateClass floodGateClass = new FloodGateClass();
            if (floodGateClass.isBedrockPlayer(uuid))
                return floodGateClass.getTextureValue(uuid);
        }
            return Head.getJavaTextureValue(uuid);
    }

    /**
     * Get the skin texture ID from a UUID. The uuid will work for bedrock players too.
     * @param uuid UUID of the player
     * @return The texture ID or null if there was an error obtaining it.
     */
    public static @Nullable String getTextureID(UUID uuid) {
        if (savedTextureIDs.containsKey(uuid)) {
            return savedTextureIDs.get(uuid);
        }
        CompletableFuture<String> completedID = null;
        if (skinsRestorerEnabled) {
            String id = new SkinsRestorerClass().getSkinTextureID(uuid);
            if (id != null) {
                completedID = CompletableFuture.completedFuture(id);
            }
        }
        if (floodgateEnabled && completedID == null) {
            FloodGateClass floodGateClass = new FloodGateClass();
            if (floodGateClass.isBedrockPlayer(uuid))
                completedID = floodGateClass.getTextureID(uuid);
        }
        if (completedID == null)
            completedID = Head.getJavaTextureID(uuid);

        try {
            String id = completedID.get();
            savedTextureIDs.put(uuid, id);
            return id;
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    private static CompletableFuture<String> getJavaTextureValue(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonParser().parse(reader).getAsJsonObject();
                JsonObject textureProperty = input.get("properties").getAsJsonArray().get(0).getAsJsonObject();
                reader.close();
                return textureProperty.get("value").getAsString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private static CompletableFuture<String> getJavaTextureID(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonParser().parse(reader).getAsJsonObject();
                reader.close();
                return input.get("id").getAsString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }


    public static PlayerProfile createProfile(String base64){
        try {
            String urlString = new String(Base64.getDecoder().decode(base64));
            String before = "{\"textures\":{\"SKIN\":{\"url\":\"";
            String after = "\"}}}";

            urlString = urlString.substring(before.length(), urlString.length() - after.length());
            URL url = new URL(urlString);
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(url);
            profile.setTextures(textures);
            return profile;
        } catch (IllegalArgumentException | MalformedURLException e){
            return null;
        }
    }

    private static boolean usingBase64(String str){
        try {
            Integer.parseInt(str);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}

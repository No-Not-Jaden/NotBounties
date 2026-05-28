package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.integrations.external_api.HeadDataBaseClass;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class Head {

    private static long headID = 1;

    public static final NamespacedKey UUID_KEY = new org.bukkit.NamespacedKey(NotBounties.getInstance(), "uuid");

    private Head(){}

    public static ItemStack createPlayerSkull(String data){

        ItemStack item = null;
        if (LoggedPlayers.isLogged(data) && LoggedPlayers.getPlayer(data) != null) {
            UUID uuid = LoggedPlayers.getPlayer(data);
            // loads skin async if not loaded
            SkinManager.isSkinLoaded(uuid);
            // will return missing skin if not loaded yet
            return createPlayerSkull(uuid, SkinManager.getSkin(uuid).url());
        } else if (usingBase64(data)){
            item = new ItemStack(Material.PLAYER_HEAD);
            if (NotBounties.getServerVersion() >= 18) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                assert meta != null;
                meta.setOwnerProfile(createProfile(data));
                item.setItemMeta(meta);
            } else {
                return Bukkit.getUnsafe().modifyItemStack(item, "{display:{Name:\"{\\\"text\\\":\\\"Head\\\"}\"},SkullOwner:{Id:[" + "I;1201296705,1414024019,-1385893868,1321399054" + "],Properties:{textures:[{Value:\"" + data + "\"}]}}}");
            }
        } else if (ConfigOptions.getIntegrations().isHeadDataBaseEnabled()){
            return (new HeadDataBaseClass().getHead(data));
        } else {
            Bukkit.getLogger().warning("[NotBounties] Could not create custom head. Use Base64 or install HeadDataBase.");
        }
        return item;
    }

    private static UUID hashToV5(UUID input) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

            // Hash the raw UUID bytes
            ByteBuffer bb = ByteBuffer.allocate(16);
            bb.putLong(input.getMostSignificantBits());
            bb.putLong(input.getLeastSignificantBits());

            byte[] hash = sha1.digest(bb.array());

            // Take first 16 bytes
            hash[6] &= 0x0f;
            hash[6] |= 0x50; // version 5

            hash[8] &= 0x3f;
            hash[8] |= 0x80; // IETF variant

            ByteBuffer out = ByteBuffer.wrap(hash);

            long msb = out.getLong();
            long lsb = out.getLong();

            return new UUID(msb, lsb);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static @NotNull ItemStack createPlayerSkull(UUID uuid, @Nullable String textureURL) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        if (NotBounties.getServerVersion() >= 18) {
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid); // could use hashToV5(uuid)
            if (textureURL != null) {
                PlayerTextures textures = profile.getTextures();
                try {
                    textures.setSkin(new URI(textureURL).toURL());
                    profile.setTextures(textures);
                } catch (URISyntaxException | MalformedURLException e) {
                    NotBounties.debugMessage("Error setting textures for " + uuid + " url: " + textureURL, true);
                    Arrays.stream(e.getStackTrace()).forEach(m -> NotBounties.debugMessage(m.toString(), true));
                }
            }
            meta.setOwnerProfile(profile);
            meta.getPersistentDataContainer().set(UUID_KEY, org.bukkit.persistence.PersistentDataType.STRING, uuid.toString());
            head.setItemMeta(meta);
            return head;
        } else {
            if (textureURL == null) return head;
            byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", textureURL).getBytes());
            return Bukkit.getUnsafe().modifyItemStack(head, "{display:{Name:\"{\\\"text\\\":\\\"Head\\\"}\"},SkullOwner:{Id:[" + "I;1201296705,1414024019,-1385893868,1321399054" + "],Properties:{textures:[{Value:\"" + new String(encodedData) + "\"}]}}}");
        }
    }

    public static PlayerProfile createProfile(String base64){
        try {
            String urlString = new String(Base64.getDecoder().decode(base64));
            String before = "{\"textures\":{\"SKIN\":{\"url\":\"";
            String after = "\"}}}";

            urlString = urlString.substring(before.length(), urlString.length() - after.length());
            URL url = new URL(urlString);
            PlayerProfile profile = Bukkit.createPlayerProfile(new UUID(13, headID++));
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
            Base64.getDecoder().decode(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.integrations.external_api.HeadDataBaseClass;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class Head {

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

    public static @NotNull ItemStack createPlayerSkull(UUID uuid, URL textureURL) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        if (NotBounties.getServerVersion() >= 18) {
            //PlayerProfile profile = Bukkit.createPlayerProfile(uuid);
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(textureURL);
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
            return head;
        } else {
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
            Base64.getDecoder().decode(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

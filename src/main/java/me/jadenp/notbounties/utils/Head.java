package me.jadenp.notbounties.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

import static me.jadenp.notbounties.utils.ConfigOptions.HDBEnabled;

public class Head {
    public static ItemStack createPlayerSkull(String data){

        ItemStack item = null;
        if (usingBase64(data)){
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            assert meta != null;
            meta.setOwnerProfile(createProfile(data));
            item.setItemMeta(meta);
        } else if (HDBEnabled){
            return (new HeadDataBaseClass().getHead(data));
        } else {
            Bukkit.getLogger().warning("[NotBounties] Could not create custom head. Use Base64 or install HeadDataBase.");
        }
        return item;
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

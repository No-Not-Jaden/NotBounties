package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
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
import static me.jadenp.notbounties.utils.ConfigOptions.skinsRestorerEnabled;

public class Head {
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
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid, NotBounties.getPlayerName(uuid));
            profile.setTextures(Bukkit.getOfflinePlayer(uuid).getPlayerProfile().getTextures());
            meta.setOwnerProfile(profile);
        }
        head.setItemMeta(meta);
        return head;
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

package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class SkinsRestorerClass {
    private SkinsRestorer skinsRestorer;

    public SkinsRestorerClass() {
        connect();
    }

    private boolean connect(){
        try {
            skinsRestorer = SkinsRestorerProvider.get();
            return true;
        } catch (IllegalStateException e) {
            Bukkit.getLogger().warning("[NotBounties] Failed at hooking into SkinsRestorer, will try again on next call.");
            return false;
        }
    }

    public ItemStack getPlayerHead(UUID uuid, String name){
        String textureURL = getSkinTextureURL(uuid, name);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (textureURL == null)
            return null;
        if (NotBounties.serverVersion >= 18) {
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid, name);
            PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(textureURL));
            } catch (MalformedURLException e) {
                return null;
            }
            profile.setTextures(textures);
            assert meta != null;
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
            return head;
        } else {
            try {
                byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", new URL(textureURL)).getBytes());
                return Bukkit.getUnsafe().modifyItemStack(head, "{display:{Name:\"{\\\"text\\\":\\\"Head\\\"}\"},SkullOwner:{Id:[" + "I;1201296705,1414024019,-1385893868,1321399054" + "],Properties:{textures:[{Value:\"" + new String(encodedData) + "\"}]}}}");
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    private String getSkinTextureURL(UUID uuid, String name) {
        if (!connect())
            return null;
        PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
        try {
            Optional<SkinProperty> property = playerStorage.getSkinForPlayer(uuid, name);
            if (property.isPresent()) {
                return PropertyUtils.getSkinTextureUrl(property.get());
            }
        } catch (DataRequestException ignored) {
        }
        return null;
    }

    public BufferedImage getPlayerFace(UUID uuid, String name) {
        if (!connect())
            return null;

        try {
                String textureUrl = getSkinTextureURL(uuid, name);
                if (textureUrl == null)
                    return null;
                BufferedImage skin = ImageIO.read(new URL(textureUrl));
                BufferedImage head = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                try {
                    // base head
                    copySquare(skin, head, 8,8);
                    // mask
                    copySquare(skin, head, 40, 8);
                } catch (IndexOutOfBoundsException e) {
                    Bukkit.getLogger().warning(e.toString());
                    return null;
                }
                return head;

        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
        }
        return null;
    }

    private void copySquare(BufferedImage from, BufferedImage to, int x, int y) {
        for (int x1 = 0; x1 < 8; x1++) {
            for (int y1 = 0; y1 < 8; y1++) {
                int color = from.getRGB(x + x1, y + y1);
                int a = (color>>24)&0xFF;
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

}

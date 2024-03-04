package me.jadenp.notbounties.utils.externalAPIs;

import me.jadenp.notbounties.NotBounties;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

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

    public ItemStack getPlayerHead(UUID uuid){
        String name = NotBounties.getPlayerName(uuid);
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

    public String getSkinTextureURL(UUID uuid, String name) {
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

    public String getSkinTextureID(UUID uuid) {
        if (!connect())
            return null;
        PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
        Optional<SkinIdentifier> property = playerStorage.getSkinIdOfPlayer(uuid);
        return property.map(SkinIdentifier::getIdentifier).orElse(null);
    }





}

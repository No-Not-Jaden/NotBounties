package me.jadenp.notbounties.features.settings.integrations.external_api;

import com.Zrips.CMI.Containers.CMIUser;

import javax.annotation.Nullable;
import java.util.UUID;

public class CMIClass {

    public static @Nullable String getNick(UUID uuid) {
        CMIUser player = CMIUser.getUser(uuid);
        if (player != null) {
            return player.getNickName();
        }
        return null;
    }

    public static @Nullable UUID getUUID(String playerName) {
        CMIUser player = CMIUser.getUser(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        return null;
    }
}

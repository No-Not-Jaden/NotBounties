package me.jadenp.notbounties.utils.external_api;

import litebans.api.Database;

import java.util.UUID;

public class LiteBansClass {
    public LiteBansClass(){}
    public boolean isPlayerNotBanned(UUID uuid) {
        try {
            return !Database.get().isPlayerBanned(uuid, null);
        } catch (IllegalStateException e) {
            return true;
        }
    }
}

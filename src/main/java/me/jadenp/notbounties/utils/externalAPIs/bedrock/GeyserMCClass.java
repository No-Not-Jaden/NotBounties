package me.jadenp.notbounties.utils.externalAPIs.bedrock;

import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

public class GeyserMCClass {
    private final GeyserApi api;
    public GeyserMCClass(){
        api = GeyserApi.api();
    }
    public boolean isBedrockPlayer(UUID uuid) {
        return api.isBedrockPlayer(uuid);
    }


}

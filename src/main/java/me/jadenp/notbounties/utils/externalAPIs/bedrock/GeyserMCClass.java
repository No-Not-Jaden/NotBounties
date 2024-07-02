package me.jadenp.notbounties.utils.externalAPIs.bedrock;

import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.geyser.api.GeyserApi;

import java.util.Objects;
import java.util.UUID;

public class GeyserMCClass {
    private final GeyserApi api;
    public GeyserMCClass(){
        api = GeyserApi.api();
    }
    public boolean isBedrockPlayer(UUID uuid) {
        return api.isBedrockPlayer(uuid);
    }

    public String getXuid(UUID uuid) {
        if (!api.isBedrockPlayer(uuid))
            throw new IllegalArgumentException("Invalid UUID.");
        return Objects.requireNonNull(api.connectionByUuid(uuid)).xuid();
    }

    public void sendForm(UUID uuid, FormBuilder<?, ?, ?> formBuilder) {
        api.sendForm(uuid, formBuilder);
    }


}

package me.jadenp.notbounties.features.settings.integrations.external_api.bedrock;

import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FloodGateClass {
    private final FloodgateApi api;
    public FloodGateClass(){
        api = FloodgateApi.getInstance();
    }

    public boolean isBedrockPlayer(UUID uuid) {
        return api.isFloodgatePlayer(uuid);
    }

    public void sendForm(UUID uuid, FormBuilder<?, ?, ?> formBuilder) {
        api.sendForm(uuid, formBuilder);
    }

    public String getXuid(UUID uuid) {
        if (!api.isFloodgatePlayer(uuid))
            throw new IllegalArgumentException("Invalid UUID.");
        return api.getPlayer(uuid).getXuid();
    }

}

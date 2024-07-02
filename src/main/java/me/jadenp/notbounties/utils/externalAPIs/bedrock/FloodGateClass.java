package me.jadenp.notbounties.utils.externalAPIs.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

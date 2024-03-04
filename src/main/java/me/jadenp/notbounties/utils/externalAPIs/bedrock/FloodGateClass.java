package me.jadenp.notbounties.utils.externalAPIs.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<String> getTextureValue(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!api.isFloodgatePlayer(uuid))
                throw new RuntimeException("Invalid UUID.");
            String xuid = api.getPlayer(uuid).getXuid();
            try {
                URL url = new URL("https://api.geysermc.org/v2/skin/" + xuid);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int code = connection.getResponseCode();
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonParser().parse(reader).getAsJsonObject();
                reader.close();
                if (code == 200) {
                    return input.get("value").getAsString();
                } else if (code == 400) {
                    throw new RuntimeException(input.get("message").getAsString());
                }
                throw new RuntimeException("Unknown code returned: " + code);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> getTextureID(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!api.isFloodgatePlayer(uuid))
                throw new RuntimeException("Invalid UUID.");
            String xuid = api.getPlayer(uuid).getXuid();
            try {
                URL url = new URL("https://api.geysermc.org/v2/skin/" + xuid);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int code = connection.getResponseCode();
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseReader(reader).getAsJsonObject() : new JsonParser().parse(reader).getAsJsonObject();
                reader.close();
                if (code == 200) {
                    return input.get("texture_id").getAsString();
                } else if (code == 400) {
                    throw new RuntimeException(input.get("message").getAsString());
                }
                throw new RuntimeException("Unknown code returned: " + code);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

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

    public void saveSkin(UUID uuid) {
        if (!api.isFloodgatePlayer(uuid))
            throw new IllegalArgumentException("Invalid UUID.");
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
                String value = input.get("value").getAsString();
                String id = input.get("texture_id").getAsString();

                SkinManager.saveSkin(uuid, new PlayerSkin(SkinManager.getTextureURL(value), id));
            } else if (code == 400) {
                if (NotBounties.debug)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getLogger().warning("[NotBountiesDebug] Error getting skin from floodgate (400)");
                            Bukkit.getLogger().warning(input.get("message").getAsString());
                        }
                    }.runTask(NotBounties.getInstance());
            } else {
                if (NotBounties.debug)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getLogger().warning("[NotBountiesDebug] Error getting skin from floodgate (" + code + ")");
                            Bukkit.getLogger().warning(input.get("message").getAsString());
                        }
                    }.runTask(NotBounties.getInstance());
            }
        } catch (IOException e) {
            if (NotBounties.debug)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getLogger().warning("[NotBountiesDebug] Error getting skin from floodgate");
                        Bukkit.getLogger().warning(e.toString());
                    }
                }.runTask(NotBounties.getInstance());
        }
    }

}

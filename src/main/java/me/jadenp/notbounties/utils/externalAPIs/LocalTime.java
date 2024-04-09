package me.jadenp.notbounties.utils.externalAPIs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LocalTime {

    // these codes will only be saved in RAM and will be deleted after the plugin is disabled
    private static final Map<UUID, TimeZone> savedTimeZones = new HashMap<>();
    private static int account;
    private static String license = null;

    private static void readAuthentication() throws IOException {
        // checks if secret file is present
        if (NotBounties.getInstance().getResource("secret.yml") == null)
            throw new IOException("No Secret File Found!");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("secret.yml"))));
        // load account id and license key from secret file
        account = configuration.getInt("geoip2.account");
        license = configuration.getString("geoip2.license");
    }

    public static String formatTime(long time, Player player) {
        if (!ConfigOptions.autoTimezone)
            return formatTime(time);
        if (license == null) {
            try {
                readAuthentication();
            } catch (IOException e) {
                return formatTime(time);
            }
        }
        if (savedTimeZones.containsKey(player.getUniqueId()))
            return formatTime(time, savedTimeZones.get(player.getUniqueId()));
        InetSocketAddress address = player.getAddress();
        if (address == null)
            return formatTime(time);


        CompletableFuture<TimeZone> timezone = getTimezone(address);
        try {
            savedTimeZones.put(player.getUniqueId(), timezone.get());
            return formatTime(time, timezone.get());
        } catch (ExecutionException | InterruptedException e) {
            return formatTime(time);
        }
    }

    private static CompletableFuture<TimeZone> getTimezone(InetSocketAddress address) {
        if (license == null)
            return CompletableFuture.failedFuture(new RuntimeException("No Authentication Credentials!"));
        return CompletableFuture.supplyAsync(() -> {

            HttpGet request = new HttpGet("https://geolite.info/geoip/v2.1/city/" + address.getAddress() + "?pretty");

            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials( account + "", license)
            );

            try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();
                 CloseableHttpResponse response = httpClient.execute(request)) {

                // 401 if wrong user/password
                //Bukkit.getLogger().info(response.getStatusLine().getStatusCode() + "");

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // return it as a String
                    String result = EntityUtils.toString(entity);
                    JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseString(result).getAsJsonObject() : new JsonParser().parse(result).getAsJsonObject();
                    JsonObject location = input.getAsJsonObject("location");

                    return TimeZone.getTimeZone(location.get("time_zone").getAsString());
                } else {
                    throw new RuntimeException("Did not get a result from request.");
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String formatTime(long time, TimeZone timeZone) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(time);
    }

    public static String formatTime(long time) {
        return ConfigOptions.dateFormat.format(time);
    }
}

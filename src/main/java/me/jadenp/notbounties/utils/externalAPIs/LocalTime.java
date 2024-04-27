package me.jadenp.notbounties.utils.externalAPIs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LocalTime {
    public enum TimeFormat {
        SERVER, PLAYER, RELATIVE
    }
    private static final Map<UUID, TimeZone> savedTimeZones = new HashMap<>();
    private static int account;
    private static String license = null;
    private static long lastException = 0;

    public static void addTimeZone(UUID uuid, String timeZone) {
        savedTimeZones.put(uuid, TimeZone.getTimeZone(timeZone));
    }

    private static void readAuthentication() throws IOException {
        // checks if secret file is present
        if (NotBounties.getInstance().getResource("secret.yml") == null)
            throw new IOException("No Secret File Found!");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("secret.yml"))));
        // load account id and license key from secret file
        account = configuration.getInt("geoip2.account");
        license = configuration.getString("geoip2.license");
    }

    private static String formatTime(long time, Player player) {
        if (!ConfigOptions.autoTimezone)
            return formatTime(time);
        if (lastException + 10 * 60 * 1000 > System.currentTimeMillis())
            return formatTime(time);
        if (license == null) {
            try {
                readAuthentication();
            } catch (IOException e) {
                return formatTime(time);
            }
        }
        if (savedTimeZones.containsKey(player.getUniqueId()))
            return formatTime(time, savedTimeZones.get(player.getUniqueId())) + " " + savedTimeZones.get(player.getUniqueId()).getDisplayName(false, TimeZone.SHORT);
        InetSocketAddress address = player.getAddress();
        if (address == null)
            return formatTime(time);


        new BukkitRunnable() {
            @Override
            public void run() {
                CompletableFuture<TimeZone> timezone = getTimezone(address);
                try {
                    savedTimeZones.put(player.getUniqueId(), timezone.get(2, TimeUnit.SECONDS));
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    lastException = System.currentTimeMillis();
                    if (NotBounties.debug) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getLogger().info("[NotBounties Debug] ->");
                                Bukkit.getLogger().info(e.toString());
                            }
                        }.runTask(NotBounties.getInstance());
                    }
                }
            }
        }.runTaskAsynchronously(NotBounties.getInstance());


        return formatTime(time);
    }

    private static CompletableFuture<TimeZone> getTimezone(InetSocketAddress address) {
        if (license == null)
            return CompletableFuture.failedFuture(new RuntimeException("No Authentication Credentials!"));
        return CompletableFuture.supplyAsync(() -> {

            HttpGet request = new HttpGet("https://geolite.info/geoip/v2.1/city/" + address.getAddress() + "?pretty");

            BasicCredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(new AuthScope("geolite.info", 443), new UsernamePasswordCredentials( account + "", license.toCharArray()));

            try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();
                 CloseableHttpResponse response = httpClient.execute(request)) {

                // 401 if wrong user/password
                //System.out.println(response.getCode());

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // return it as a String
                    String result = EntityUtils.toString(entity);
                    if (NotBounties.debug) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getLogger().info("[NotBounties Debug] ->");
                                Bukkit.getLogger().info(result);
                            }
                        }.runTask(NotBounties.getInstance());
                    }

                    JsonObject input = NotBounties.serverVersion >= 18 ? JsonParser.parseString(result).getAsJsonObject() : new JsonParser().parse(result).getAsJsonObject();
                    JsonObject location = input.getAsJsonObject("location");

                    return TimeZone.getTimeZone(location.get("time_zone").getAsString());
                } else {
                    throw new RuntimeException("Did not get a result from request.");
                }

            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String formatTime(long time, TimeZone timeZone) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(time);
    }

    private static String formatTime(long time) {
        return ConfigOptions.dateFormat.format(time) + " " + ConfigOptions.dateFormat.getTimeZone().getDisplayName(false, TimeZone.SHORT);
    }

    private static String formatRelativeTime(long ms) {
        long days = (long) (ms / (8.64 * Math.pow(10,7)));
        ms = (long) (ms % (8.64 * Math.pow(10,7)));
        long hours = ms / 3600000L;
        ms = ms % 3600000L;
        long minutes = ms / 60000L;
        ms = ms % 60000L;
        long seconds = ms / 1000L;
        String time = "";
        if (days > 0) time += days + "d ";
        if (hours > 0) time += hours + "h ";
        if (minutes > 0) time += minutes + "m ";
        if (seconds > 0) time += seconds + "s";
        if (time.isEmpty())
            return "0s";
        return time;
    }

    public static String formatTime(long time, TimeFormat format, Player... players) {
        switch (format) {
            case PLAYER:
                if (players.length > 0 && players[0] != null)
                    return formatTime(time, players[0]);
                return formatTime(time);
            case SERVER:
                return formatTime(time);
            case RELATIVE:
                return formatRelativeTime(time);
        }
        return formatTime(time);
    }

     public static Map<UUID, TimeZone> getSavedTimeZones() {
        return savedTimeZones;
    }
}

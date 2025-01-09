package me.jadenp.notbounties.utils.external_api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.databases.proxy.ProxyMessaging;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.util.*;

public class LocalTime {
    public enum TimeFormat {
        SERVER, PLAYER, RELATIVE
    }
    private static final Map<UUID, TimeZone> savedTimeZones = new HashMap<>();
    private static int account;
    private static String license = null;
    private static long lastException = 0;
    private static final String SECRET_FILE = "secret.yml";

    public static void addTimeZone(UUID uuid, String timeZone) {
        savedTimeZones.put(uuid, TimeZone.getTimeZone(timeZone));
    }

    private static void readAuthentication() throws IOException {
        // checks if secret file is present
        File secretFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + SECRET_FILE);
        YamlConfiguration configuration;
        if (secretFile.exists()) {
            configuration = YamlConfiguration.loadConfiguration(secretFile);
        } else if (NotBounties.getInstance().getResource(SECRET_FILE) != null) {
            configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource(SECRET_FILE))));
        } else {
            throw new IOException("No Secret File Found!");
        }

        // load account id and license key from secret file
        account = configuration.getInt("geoip2.account");
        license = decodeLicense(Objects.requireNonNull(configuration.getString("geoip2.license")));
    }

    private static String decodeLicense(String input) {
        char[] text = input.toCharArray();
        StringBuilder builder = new StringBuilder();
        for (char c : text) {
            builder.append(Character.valueOf((char) (c + 1)));
        }
        return builder.toString();
    }

    private static String formatTime(long time, Player player) {
        if (!ConfigOptions.autoTimezone)
            return formatTime(time, player.getLocale());
        if (lastException + 10 * 60 * 1000 > System.currentTimeMillis())
            return formatTime(time, player.getLocale());
        if (ProxyMessaging.hasConnectedBefore())
            return formatTime(time, player.getLocale());
        if (license == null) {
            try {
                readAuthentication();
            } catch (IOException e) {
                return formatTime(time, player.getLocale());
            }
        }
        if (savedTimeZones.containsKey(player.getUniqueId()))
            return formatTime(time, savedTimeZones.get(player.getUniqueId()));

        InetSocketAddress address = player.getAddress();
        if (address == null)
            return formatTime(time, player.getLocale());

        new BukkitRunnable() {
            @Override
            public void run() {
                registerTimeZone(player);
            }
        }.runTaskAsynchronously(NotBounties.getInstance());


        return formatTime(time, player.getLocale());
    }

    private static void registerTimeZone(Player player) {
        if (license == null)
            return;
        InetSocketAddress address = player.getAddress();
        if (address == null)
            return;

        HttpGet request = new HttpGet("https://geolite.info/geoip/v2.1/city/" + address.getAddress() + "?pretty");

        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(new AuthScope("geolite.info", 443), new UsernamePasswordCredentials( account + "", license.toCharArray()));

        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
        ) {

            // 401 if wrong user/password
            TimeZone timeZone = httpClient.execute(request, new ResponseHandler());
            savedTimeZones.put(player.getUniqueId(), timeZone);

        } catch (IOException ignored) {
            // cant get timezone
            lastException = System.currentTimeMillis();
        }
    }

    private static String formatTime(long time, TimeZone timeZone) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(time) + " " + timeZone.getDisplayName(false, TimeZone.SHORT);
    }

    private static String formatTime(long time, String localeString) {
        Locale locale = Locale.forLanguageTag(localeString);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale);
        return dateFormat.format(time) + " " + dateFormat.getTimeZone().getDisplayName(false, TimeZone.SHORT);
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
        return switch (format) {
            case PLAYER -> {
                if (players.length > 0 && players[0] != null)
                    yield formatTime(time, players[0]);
                yield formatTime(time);
            }
            case SERVER -> formatTime(time);
            case RELATIVE -> formatRelativeTime(time);
        };
    }

     public static Map<UUID, TimeZone> getSavedTimeZones() {
        return savedTimeZones;
    }
}

class ResponseHandler implements HttpClientResponseHandler<TimeZone> {

    @Override
    public TimeZone handleResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {
        HttpEntity entity = classicHttpResponse.getEntity();
        if (entity != null) {
            // return it as a String
            String result = EntityUtils.toString(entity);
            classicHttpResponse.close();

            JsonObject input = new JsonParser().parse(result).getAsJsonObject();
            NotBounties.debugMessage("Receieved Timezone Response!", false);
            if (input.has("location")) {
                NotBounties.debugMessage("Valid location!", false);
                JsonObject location = input.getAsJsonObject("location");
                return TimeZone.getTimeZone(location.get("time_zone").getAsString());
            } else {
                NotBounties.debugMessage(input.toString(), false);
                throw new IOException("Ran out of credits.");
            }
        } else {
            throw new IOException("Did not get a result from request.");
        }
    }
}

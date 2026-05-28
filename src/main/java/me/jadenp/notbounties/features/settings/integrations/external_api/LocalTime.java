package me.jadenp.notbounties.features.settings.integrations.external_api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalTime {
    public enum TimeFormat {
        SERVER, PLAYER, RELATIVE
    }
    private static final Map<UUID, TimeZone> savedTimeZones = new HashMap<>();
    private static int account;
    private static String license = null;
    private static long lastException = 0;
    private static final long ERROR_TIMEOUT_MS = 18 * 60 * 60 * 1000L;
    private static boolean autoTimezone = true;
    private static int defaultDateTimeStyle = DateFormat.DEFAULT;
    private static TimeZone defaultPlayerTimeZone = TimeZone.getDefault();
    private static String durationFormat = "[{d}d] [{h}h] [{m}m] [{s}s]";

    public static void loadConfiguration(ConfigurationSection config) {
        autoTimezone = config.getBoolean("auto-timezone");
        // Default player time formatting style
        String styleStr = config.getString("default-format-style", "DEFAULT").toUpperCase(Locale.ROOT);
        switch (styleStr) {
            case "FULL" -> defaultDateTimeStyle = DateFormat.FULL;
            case "LONG" -> defaultDateTimeStyle = DateFormat.LONG;
            case "MEDIUM" -> defaultDateTimeStyle = DateFormat.MEDIUM;
            case "SHORT" -> defaultDateTimeStyle = DateFormat.SHORT;
            default -> defaultDateTimeStyle = DateFormat.DEFAULT;
        }

        // Default player timezone when none recorded
        String tzStr = config.getString("time.default-timezone", "SERVER");
        if (tzStr.equalsIgnoreCase("SERVER") || tzStr.isEmpty()) {
            defaultPlayerTimeZone = TimeZone.getDefault();
        } else {
            defaultPlayerTimeZone = TimeZone.getTimeZone(tzStr);
        }
        durationFormat = config.getString("duration-format", durationFormat);

        account = config.getInt("geoip2.account");
        license = config.getString("geoip2.license");
    }

    public static void addTimeZone(UUID uuid, String timeZone) {
        savedTimeZones.put(uuid, TimeZone.getTimeZone(timeZone));
    }

    private static String formatTime(long time, Player player) {
        if (!autoTimezone)
            return formatTime(time);

        if (lastException + ERROR_TIMEOUT_MS > System.currentTimeMillis() || ProxyMessaging.hasConnectedBefore())
            return formatTime(time, player.getLocale());

        if (savedTimeZones.containsKey(player.getUniqueId()))
            return formatTime(time, savedTimeZones.get(player.getUniqueId()));

        if (license == null || account == 123456) {
            // the authentication is not loaded or is using the default
            return formatTime(time, player.getLocale());
        }

        InetSocketAddress address = player.getAddress();
        if (address == null)
            return formatTime(time, player.getLocale());

        NotBounties.getServerImplementation().async().runNow(() -> registerTimeZone(player));

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
                .build()
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
        DateFormat dateFormat = DateFormat.getDateTimeInstance(defaultDateTimeStyle, defaultDateTimeStyle, NumberFormatting.getLocale());
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(time) + " " + timeZone.getDisplayName(false, TimeZone.SHORT);
    }

    private static String formatTime(long time, String localeString) {
        Locale locale = Locale.forLanguageTag(localeString);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(defaultDateTimeStyle, defaultDateTimeStyle, locale);
        dateFormat.setTimeZone(defaultPlayerTimeZone);
        return dateFormat.format(time) + " " + dateFormat.getTimeZone().getDisplayName(false, TimeZone.SHORT);
    }

    private static String formatTime(long time) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(defaultDateTimeStyle, defaultDateTimeStyle, NumberFormatting.getLocale());
        dateFormat.setTimeZone(defaultPlayerTimeZone);
        return dateFormat.format(time) + " " + dateFormat.getTimeZone().getDisplayName(false, TimeZone.SHORT);
    }

    /**
     Tokens:
     {dd} {d}
     {hh} {h}
     {mm} {m}
     {ss} {s}

     Optional sections:
     [ ... ]
     A section is only included if at least one token inside is non-zero.
     */
    private static String formatDuration(long millis, String format) {
        long totalSeconds = millis / 1000;

        long days = totalSeconds / 86400;
        totalSeconds %= 86400;

        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        // Handle optional sections
        Pattern sectionPattern = Pattern.compile("\\[(.*?)]");
        Matcher sectionMatcher = sectionPattern.matcher(format);

        StringBuilder sectionBuffer = new StringBuilder();

        while (sectionMatcher.find()) {
            String section = sectionMatcher.group(1);

            boolean include =
                    (section.contains("{d") && days > 0) ||
                            (section.contains("{h") && hours > 0) ||
                            (section.contains("{m") && minutes > 0) ||
                            (section.contains("{s") && seconds > 0);

            sectionMatcher.appendReplacement(
                    sectionBuffer,
                    include ? Matcher.quoteReplacement(section) : ""
            );
        }

        sectionMatcher.appendTail(sectionBuffer);

        String result = sectionBuffer.toString();

        // Replace tokens
        result = result
                .replace("{dd}", String.format("%02d", days))
                .replace("{d}", String.valueOf(days))

                .replace("{hh}", String.format("%02d", hours))
                .replace("{h}", String.valueOf(hours))

                .replace("{mm}", String.format("%02d", minutes))
                .replace("{m}", String.valueOf(minutes))

                .replace("{ss}", String.format("%02d", seconds))
                .replace("{s}", String.valueOf(seconds));

        // Cleanup whitespace
        result = result.trim().replaceAll("\\s+", " ");

        return totalSeconds == 0 ? "0s" : result;
    }

    public static String formatTime(long time, TimeFormat format, Player... players) {
        return switch (format) {
            case PLAYER -> {
                if (players.length > 0 && players[0] != null)
                    yield formatTime(time, players[0]);
                yield formatTime(time);
            }
            case SERVER -> formatTime(time);
            case RELATIVE -> formatDuration(time, durationFormat);
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
            NotBounties.debugMessage("Receieved Timezone Response!", false);
            JsonObject input;
            try {
                input = new JsonParser().parse(result).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                NotBounties.debugMessage("Bad Syntax.", false);
                throw new IOException("Bad Syntax.");
            }
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

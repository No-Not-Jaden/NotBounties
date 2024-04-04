package me.jadenp.notbounties.utils.externalAPIs;

import com.maxmind.geoip2.WebServiceClient;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Objects;

public class LocalTime {

    private static WebServiceClient client = null;

    private static void registerClient() throws IOException {
        // checks if secret file is present
        if (NotBounties.getInstance().getResource("secret.yml") == null)
            throw new IOException("No Secret File Found!");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("secret.yml"))));
        // load account id and license key from secret file
        int account = configuration.getInt("geoip2.account");
        String license = configuration.getString("geoip2.license");
        client = new WebServiceClient.Builder(account, license).build();
    }

    public static String formatTime(long time, Player player) {
        if (!ConfigOptions.autoTimezone)
            return formatTime(time);
        if (client == null) {
            try {
                registerClient();
            } catch (IOException e) {
                return formatTime(time);
            }
        }
        InetSocketAddress address = player.getAddress();
        if (address == null)
            return formatTime(time);

        CityResponse cityResponse;
        try {
            cityResponse = client.city(address.getAddress());
        } catch (IOException | GeoIp2Exception e) {
            return formatTime(time);
        }

        String isoCode = cityResponse.getCountry().getIsoCode();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getISO3Country().equals(isoCode)) {
                return formatTime(time, locale);
            }
        }

        return formatTime(time);
    }

    private static String formatTime(long time, Locale locale) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale);
        return dateFormat.format(time);
    }

    public static String formatTime(long time) {
        return ConfigOptions.dateFormat.format(time);
    }
}

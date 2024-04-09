package me.jadenp.notbounties.utils.externalAPIs;

import com.maxmind.geoip2.WebServiceClient;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LocalTime {

    // these codes will only be saved in RAM and will be deleted after the plugin is disabled
    private static final Map<UUID, String> savedIsoCodes = new HashMap<>();

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
        if (savedIsoCodes.containsKey(player.getUniqueId()))
            return formatTime(time, savedIsoCodes.get(player.getUniqueId()));
        InetSocketAddress address = player.getAddress();
        if (address == null)
            return formatTime(time);


        CompletableFuture<String> isoCode = getIsoCode(address);
        Bukkit.getLogger().info("Requesting iso code: " + address);
        try {
            savedIsoCodes.put(player.getUniqueId(), isoCode.get());
            Bukkit.getLogger().info(isoCode.get());
            return formatTime(time, isoCode.get());
        } catch (ExecutionException | InterruptedException e) {
            Bukkit.getLogger().info(e.toString());
            return formatTime(time);
        }
    }

    private static CompletableFuture<String> getIsoCode(InetSocketAddress address) {
        return CompletableFuture.supplyAsync(() -> {
            Authenticator.setDefault (new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication ("username", "password".toCharArray());
                }
            });
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);

            return cityResponse.getCountry().getIsoCode();
        });
    }

    private static String formatTime(long time, String isoCode) {
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getISO3Country().equals(isoCode)) {
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale);
                dateFormat.setTimeZone(TimeZone.getTimeZone(isoCode));
                return dateFormat.format(time);
            }
        }

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, NumberFormatting.locale);
        dateFormat.setTimeZone(TimeZone.getTimeZone(isoCode));
        return dateFormat.format(time);
    }

    public static String formatTime(long time) {
        return ConfigOptions.dateFormat.format(time);
    }
}

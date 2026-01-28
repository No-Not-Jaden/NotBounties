package me.jadenp.notbounties.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class LoggedPlayers {

    private LoggedPlayers(){}

    /**
     * Name (lowercase), UUID
     */
    private static final Map<String, UUID> playerIDs = new HashMap<>();

    private static HttpSyncPool httpPool;

    @Deprecated(since = "1.22.0")
    public static void readOldConfiguration(ConfigurationSection configuration) {
        // add all previously logged on players to a map
        if (configuration.isSet("0")) {
            // old configuration - configuration section of numbers
            int i = 0;
            while (configuration.getString(i + ".name") != null) {
                String name = Objects.requireNonNull(configuration.getString( i + ".name"));
                UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString(i + ".uuid")));
                logPlayer(name, uuid);
                i++;
            }
        } else {
            // 2nd old configuration - key = uuid, value = name
            for (String key : configuration.getKeys(false)) {
                String name = Objects.requireNonNull(configuration.getString(key));
                try {
                    UUID uuid = UUID.fromString(key);
                    logPlayer(name, uuid);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("Key in logged-players is not a UUID: " + key);
                }
            }
        }
    }

    /**
     * Loads player data and saves their names to a map.
     */
    public static void loadPlayerData() {
        playerIDs.clear();
        List<UUID> duplicateUUIDs = new LinkedList<>();
        for (PlayerData entry : DataManager.getAllPlayerData()) {
            if (entry.getPlayerName() != null) {
                try {
                    UUID uuid = UUID.fromString(entry.getPlayerName());
                    duplicateUUIDs.add(uuid);
                } catch (IllegalArgumentException e) {
                    // name is not a uuid
                    if (playerIDs.containsKey(entry.getPlayerName().toLowerCase())) {
                        UUID duplicateUUID = playerIDs.get(entry.getPlayerName().toLowerCase());
                        Bukkit.getLogger().warning("Duplicate player name found \"" + entry.getPlayerName() + "\" for "  + duplicateUUID + " and " + entry.getUuid() + ".");
                        Bukkit.getLogger().warning("Replacing with UUID until the player login.");
                        duplicateUUIDs.add(duplicateUUID);
                        playerIDs.remove(entry.getPlayerName().toLowerCase());
                        playerIDs.put(duplicateUUID.toString(), duplicateUUID);
                        playerIDs.put(entry.getUuid().toString(), entry.getUuid());
                        entry.setPlayerName(entry.getUuid().toString());
                        DataManager.getPlayerData(duplicateUUID).setPlayerName(duplicateUUID.toString());
                    } else {
                        playerIDs.put(entry.getPlayerName().toLowerCase(), entry.getUuid());
                    }
                }

            }
        }
        if (!duplicateUUIDs.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[NotBounties] There are {0} logged players with no associated name.", duplicateUUIDs.size());
            NotBounties.debugMessage(duplicateUUIDs.toString(), false);
        }
        httpPool = new HttpSyncPool(1, 10);

    }

    public static Map<UUID, String> getLoggedPlayers() {
        Map<UUID, String> reversedPlayerIDs = new HashMap<>(playerIDs.size());
        for (Map.Entry<String, UUID> entry : playerIDs.entrySet()) {
            reversedPlayerIDs.put(entry.getValue(), entry.getKey());
        }
        return reversedPlayerIDs;
    }

    /**
     * Returns a UUID from their logged name.
     * @param name Name of the player
     * @return The UUID or null if one hasn't been logged yet.
     */
    public static @Nullable UUID getPlayer(@NotNull String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null)
            return player.getUniqueId();
        if (playerIDs.containsKey(name.toLowerCase(Locale.ROOT)))
            return playerIDs.get(name.toLowerCase(Locale.ROOT));
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            return getClosestPlayer(name);
        }
    }

    public static void logPlayer(@NotNull String name, @NotNull UUID uuid) {
        playerIDs.put(name.toLowerCase(), uuid);
    }

    public static void replacePlayerName(@NotNull String newName, @NotNull UUID uuid) {
        PlayerData playerData = DataManager.getPlayerData(uuid);
        String oldName =  playerData.getPlayerName();
        if (oldName != null)
            playerIDs.remove(oldName.toLowerCase());
        playerIDs.put(newName.toLowerCase(), uuid);
        playerData.setPlayerName(newName);
    }

    public static boolean isLogged(@NotNull String name) {
        return playerIDs.containsKey(name.toLowerCase());
    }

    public static boolean isMissing(@NotNull UUID uuid) {
        return DataManager.getPlayerData(uuid).getPlayerName() == null;
    }

    public static void login(Player player) {
        // check to see if anyone else had this player name
        if (playerIDs.containsKey(player.getName().toLowerCase(Locale.ROOT))) {
            UUID uuid = playerIDs.get(player.getName().toLowerCase(Locale.ROOT));
            if (!uuid.equals(player.getUniqueId())) {
                // another player has this name logged - remove their reference
                DataManager.getPlayerData(uuid).setPlayerName(uuid.toString());
                playerIDs.put(uuid.toString(), uuid);
                playerIDs.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
            }
        }

        // check if they are logged yet
        if (isMissing(player.getUniqueId())) {
            // if not, add them
            DataManager.getPlayerData(player.getUniqueId()).setPlayerName(player.getName());
            // send a proxy message to log
            ProxyMessaging.logNewPlayer(player.getName(), player.getUniqueId());
        } else {
            // if they are, check if their username has changed, and update it
            if (!getPlayerName(player.getUniqueId()).equals(player.getName())) {
                replacePlayerName(player.getName(), player.getUniqueId());
            }
        }
    }

    private static UUID getClosestPlayer(String playerName) {
        List<String> viableNames = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : playerIDs.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(playerName.toLowerCase()))
                viableNames.add(entry.getKey());
        }
        if (viableNames.isEmpty())
            return null;
        Collections.sort(viableNames);
        return playerIDs.get(viableNames.get(0));
    }

    public static @NotNull String getPlayerName(@NotNull UUID uuid) {
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
            return ConfigOptions.getAutoBounties().getConsoleBountyName();
        PlayerData playerData = DataManager.getPlayerData(uuid);
        if (playerData.getPlayerName() != null)
            return playerData.getPlayerName();
        if (uuid.version() == 4 /* check if online player */) {
            httpPool.requestPlayerNameAsync(uuid, new HttpSyncPool.ResponseHandler())
                    .thenAccept(playerData::setPlayerName)
                    .exceptionally(ex -> {
                        NotBounties.debugMessage("Failed to get player name for " + uuid + ".", true);
                        return null;
                    });
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null)
            return name;
        return uuid.toString();
    }

    public static void shutdown() {
        if (httpPool != null) {
            httpPool.close();
            httpPool = null;
        }
    }
}


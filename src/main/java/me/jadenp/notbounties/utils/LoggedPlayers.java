package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LoggedPlayers {

    private LoggedPlayers(){}

    /**
     * Name (lowercase), UUID
     */
    private static final Map<String, UUID> playerIDs = new HashMap<>();

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
        DataManager.getAllPlayerData().stream()
                .filter(entry -> entry.getPlayerName() != null)
                .forEach(entry -> playerIDs.put(entry.getPlayerName().toLowerCase(), entry.getUuid()));
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
        if (playerIDs.containsKey(name))
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
        // check if they are logged yet
        if (isMissing(player.getUniqueId())) {
            // if not, add them
            DataManager.getPlayerData(player.getUniqueId()).setPlayerName(player.getName());
            // send a proxy message to log
            ProxyMessaging.logNewPlayer(player.getName(), player.getUniqueId());
        } else
            // if they are, check if their username has changed, and update it
            if (!getPlayerName(player.getUniqueId()).equals(player.getName())) {
                replacePlayerName(player.getName(), player.getUniqueId());
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
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null)
            return name;
        return uuid.toString();
    }
}

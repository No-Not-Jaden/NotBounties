package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.databases.proxy.ProxyMessaging;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.consoleName;

public class LoggedPlayers {

    private LoggedPlayers(){}

    /**
     * Name (lowercase), UUID
     */
    private static final Map<String, UUID> playerIDs = new HashMap<>();
    /**
     * UUID, Name
     */
    private static final Map<UUID, String> playerNames = new HashMap<>();

    public static void readConfiguration(ConfigurationSection configuration) {
        // add all previously logged on players to a map
        if (configuration.isSet("0")) {
            // old configuration - configuration section of numbers
            int i = 0;
            while (configuration.getString(i + ".name") != null) {
                String name = Objects.requireNonNull(configuration.getString( i + ".name"));
                UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString(i + ".uuid")));
                playerIDs.put(name.toLowerCase(), uuid);
                playerNames.put(uuid, name);
                i++;
            }
        } else {
            // new version - key = uuid, value = name
            for (String key : configuration.getKeys(false)) {
                String name = Objects.requireNonNull(configuration.getString(key));
                try {
                    UUID uuid = UUID.fromString(key);
                    playerIDs.put(name.toLowerCase(), uuid);
                    playerNames.put(uuid, name);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("Key in logged-players is not a UUID: " + key);
                }
            }
        }
    }

    public static Map<UUID, String> getLoggedPlayers() {
        return playerNames;
    }

    /**
     * Returns an OfflinePlayer from their logged name
     * @param name Name of the player
     * @return The OfflinePlayer or null if one hasn't been logged yet.
     */
    public static UUID getPlayer(String name) {
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

    public static void logPlayer(String name, UUID uuid) {
        playerIDs.put(name.toLowerCase(), uuid);
        playerNames.put(uuid, name);
    }

    public static void replacePlayerName(String newName, UUID uuid) {
        String oldName = playerNames.get(uuid);
        if (oldName != null)
            playerIDs.remove(oldName.toLowerCase());
        playerIDs.put(newName.toLowerCase(), uuid);
        playerNames.put(uuid, newName);
    }

    public static boolean isLogged(String name) {
        return playerIDs.containsKey(name.toLowerCase());
    }

    public static boolean isLogged(UUID uuid) {
        return playerNames.containsKey(uuid);
    }

    public static void login(Player player) {
        // check if they are logged yet
        if (!isLogged(player.getUniqueId())) {
            // if not, add them
            logPlayer(player.getName(), player.getUniqueId());
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

    public static @NotNull String getPlayerName(UUID uuid) {
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
            return consoleName;
        if (playerNames.containsKey(uuid))
            return playerNames.get(uuid);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null)
            return name;
        return uuid.toString();
    }
}

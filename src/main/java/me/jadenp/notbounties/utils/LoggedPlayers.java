package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.ImpersistentPlayerData;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import me.jadenp.notbounties.features.settings.integrations.external_api.CMIClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LoggedPlayers {

    private LoggedPlayers(){}

    /**
     * Name (lowercase), UUID
     */
    private static final Map<String, UUID> playerIDs = new HashMap<>();
    private static final Set<UUID> requestingNames = new HashSet<>();

    private static HttpSyncPool httpPool;

    private static void loadName(PlayerData playerData) {
        if (playerData.getPlayerName() != null) {
            try {
                UUID.fromString(playerData.getPlayerName());
            } catch (IllegalArgumentException e) {
                // name is not a uuid
                playerIDs.put(playerData.getPlayerName().toLowerCase(), playerData.getUuid());
            }

        } else {
            webRequestPlayerName(playerData.getUuid());
        }
    }

    /**
     * Loads player data and saves their names to a map.
     */
    public static void loadPlayerData() {
        playerIDs.clear();
        DataManager.iterateAllPlayerData(LoggedPlayers::loadName);

    }

    private static void loadHttpPool() {
        if (httpPool == null) {
            httpPool = new HttpSyncPool(1, 100);
        }
    }

    public static Map<UUID, String> getLoggedPlayers() {
        Map<UUID, String> reversedPlayerIDs = new HashMap<>();
        ImpersistentPlayerData.getAll().forEach((uuid, impersistentPlayerData) -> {
            if (impersistentPlayerData.getPlayerName() != null) {
                reversedPlayerIDs.put(impersistentPlayerData.getUuid(), impersistentPlayerData.getPlayerName());
            }
        });
        return reversedPlayerIDs;
    }

    /**
     * Returns a UUID from their logged name.
     * @param name Name of the player
     * @return The UUID or null if one hasn't been logged yet.
     */
    public static @Nullable UUID getPlayer(@NotNull String name) {
        if (playerIDs.containsKey(name.toLowerCase(Locale.ROOT)))
            return playerIDs.get(name.toLowerCase(Locale.ROOT));
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            replacePlayerName(player.getName(), player.getUniqueId());
            return player.getUniqueId();
        }
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
        DataManager.getPlayerDataAsync(uuid).thenAcceptAsync(playerData -> {
            if (!newName.equals(playerData.getPlayerName())) {
                if (playerData.getPlayerName() != null)
                    playerIDs.remove(playerData.getPlayerName().toLowerCase());
                playerIDs.put(newName.toLowerCase(), uuid);
                playerData.setPlayerName(newName);
                DataManager.updatePlayerData(playerData);
            }
        });
    }

    public static boolean isLogged(@NotNull String name) {
        return playerIDs.containsKey(name.toLowerCase());
    }

    public static boolean isMissing(@NotNull UUID uuid) {
        return ImpersistentPlayerData.get(uuid).getPlayerName() == null;
    }

    public static void login(Player player) {
        // check to see if anyone else had this player name
        if (playerIDs.containsKey(player.getName().toLowerCase(Locale.ROOT))) {
            UUID uuid = playerIDs.get(player.getName().toLowerCase(Locale.ROOT));
            if (!uuid.equals(player.getUniqueId())) {
                NotBounties.debugMessage(player.getName() + " is logged with a different UUID! " + player.getUniqueId() + " != " + uuid, true);
                // another player has this name logged
                // replace the reference for now
                playerIDs.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
            }
        }
        // check if they are logged yet
        if (isMissing(player.getUniqueId())) {
            NotBounties.debugMessage("Logging player name: " + player.getName(), false);
            // if not, add them
            replacePlayerName(player.getName(), player.getUniqueId());
            // send a proxy message to log
            ProxyMessaging.logNewPlayer(player.getName(), player.getUniqueId());
        } else {
            // if they are, check if their username has changed, and update it
            String recordedName = getPlayerName(player.getUniqueId());
            if (!recordedName.equals(player.getName())) {
                NotBounties.debugMessage("Recorded name for " + player.getUniqueId() + " does not match. " + player.getName() + " != " + recordedName, true);
                try {
                    UUID.fromString(recordedName);
                    // log new player if their old name was a uuid
                    ProxyMessaging.logNewPlayer(player.getName(), player.getUniqueId());
                } catch (IllegalArgumentException ignored) {
                    // old name wasn't a uuid
                }
                replacePlayerName(player.getName(), player.getUniqueId());
            }
        }
    }

    private static UUID getClosestPlayer(String playerName) {
        if (ConfigOptions.getIntegrations().isEssentialsEnabled()) {
            UUID uuid = ConfigOptions.getIntegrations().getEssentialsXClass().getUUID(playerName);
            if (uuid != null)
                return uuid;
        }
        if (ConfigOptions.getIntegrations().isCMIEnabled()) {
            UUID uuid = CMIClass.getUUID(playerName);
            if (uuid != null)
                return uuid;
        }
        List<String> viableNames = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : playerIDs.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(playerName.toLowerCase()))
                viableNames.add(entry.getKey());
        }
        if (viableNames.isEmpty())
            return null;
        Collections.sort(viableNames);
        return playerIDs.get(viableNames.getFirst());
    }

    public static @NotNull String getPlayerName(@NotNull OfflinePlayer player) {
        if (player.isOnline() && player.getName() != null) {
            if (!isLogged(player.getName())) {
                logPlayer(player.getName(), player.getUniqueId());
                replacePlayerName(player.getName(), player.getUniqueId());
            }
            return player.getName();
        }
        return getPlayerName(player.getUniqueId());
    }

    public static @NotNull String getPlayerName(@NotNull UUID uuid) {
        // I don't think this function is an issue. Every uuid that is returned should not be tied to a player.
        // If that is the case, then the problem lies in functions calling this one using bad UUIDs.
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
            return ConfigOptions.getAutoBounties().getConsoleBountyName();
        ImpersistentPlayerData impersistentPlayerData = ImpersistentPlayerData.get(uuid);
        if (impersistentPlayerData.getPlayerName() != null)
            return impersistentPlayerData.getPlayerName();
        webRequestPlayerName(uuid);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null) {
            replacePlayerName(player.getName(), player.getUniqueId());
            logPlayer(name, uuid);
            return name;
        }
        return uuid.toString();
    }

    private static void webRequestPlayerName(@NotNull UUID uuid) {
        if (uuid.version() == 4 /* check if online player */ && !requestingNames.contains(uuid)) {
            requestingNames.add(uuid);
            loadHttpPool();
            httpPool.requestPlayerNameAsync(uuid, new HttpSyncPool.ResponseHandler())
                    .thenAccept(name -> {
                        DataManager.getPlayerDataAsync(uuid).thenAccept(playerData -> {
                            playerData.setPlayerName(name);
                            DataManager.updatePlayerData(playerData);
                        });
                        requestingNames.remove(uuid);
                        logPlayer(name, uuid);
                    })
                    .exceptionally(ex -> {
                        NotBounties.debugMessage("Failed to get player name for " + uuid + ".", true);
                        requestingNames.remove(uuid);
                        return null;
                    });
        }
    }

    public static @Nullable PlayerProfile getPlayerProfile(String key, UUID uuid, @Nullable String name) {
        loadHttpPool();
        return httpPool.getPlayerProfile(key, uuid, name);
    }

    public static void shutdown() {
        if (httpPool != null) {
            httpPool.close();
            httpPool = null;
        }
    }

    public static void loadAllDisplayNames() {
        for (UUID uuid : playerIDs.values()) {
            getDisplayName(uuid);
        }
    }

    public static String getDisplayName(OfflinePlayer p) {
        if (p == null)
            return "";
        if (p.isOnline()) {
            String name = Objects.requireNonNull(p.getPlayer()).getDisplayName();
            ImpersistentPlayerData.get(p.getUniqueId()).setDisplayName(name);
            return name;
        }
        return getDisplayName(p.getUniqueId());
    }

    public static String getDisplayName(UUID uuid) {
        if (uuid == null)
            return "";
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
            return ConfigOptions.getAutoBounties().getConsoleBountyName();
        return ImpersistentPlayerData.get(uuid).getDisplayName();
    }


}


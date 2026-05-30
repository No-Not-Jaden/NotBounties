package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import me.jadenp.notbounties.features.settings.integrations.external_api.CMIClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class LoggedPlayers {

    private LoggedPlayers(){}

    record CacheEntry(String value, long loadedAt) {}

    /**
     * Name (lowercase), UUID
     */
    private static final Map<String, UUID> playerIDs = new HashMap<>();
    private static final Set<UUID> requestingNames = new HashSet<>();
    private static final Map<UUID, CacheEntry> cachedNicknames = new ConcurrentHashMap<>();
    private static final long CACHE_REFRESH_TIME = TimeUnit.MINUTES.toMillis(10);

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
                DataManager.getPlayerData(uuid).setPlayerName(name);
                i++;
            }
        } else {
            // 2nd old configuration - key = uuid, value = name
            for (String key : configuration.getKeys(false)) {
                String name = Objects.requireNonNull(configuration.getString(key));
                try {
                    UUID uuid = UUID.fromString(key);
                    DataManager.getPlayerData(uuid).setPlayerName(name);
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
                        Bukkit.getLogger().warning("Replacing with UUID until the player logs in again.");
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

            } else {
                webRequestPlayerName(entry.getUuid(), entry);
            }
        }
        if (!duplicateUUIDs.isEmpty()) {
            Bukkit.getLogger().log(Level.WARNING, "[NotBounties] There are {0} logged players with no associated name.", duplicateUUIDs.size());
            NotBounties.debugMessage(duplicateUUIDs.toString(), false);
        }

    }

    private static void loadHttpPool() {
        if (httpPool == null) {
            httpPool = new HttpSyncPool(1, 100);
        }
    }

    public static Map<UUID, String> getLoggedPlayers() {

        Map<UUID, String> reversedPlayerIDs = new HashMap<>();
        DataManager.getAllPlayerData().stream().filter(playerData -> playerData.getPlayerName() != null).forEach(playerData -> reversedPlayerIDs.put(playerData.getUuid(), playerData.getPlayerName()));
        return reversedPlayerIDs;
    }

    /**
     * Returns a UUID from their logged name.
     * @param name Name of the player
     * @return The UUID or null if one hasn't been logged yet.
     */
    public static @Nullable UUID getPlayer(@NotNull String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            DataManager.getPlayerData(player.getUniqueId()).setPlayerName(player.getName());
            return player.getUniqueId();
        }
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
                NotBounties.debugMessage(player.getName() + " is logged with a different UUID! " + player.getUniqueId() + " != " + uuid, true);
                // another player has this name logged - remove their reference
                DataManager.getPlayerData(uuid).setPlayerName(uuid.toString());
                playerIDs.put(uuid.toString(), uuid);
                playerIDs.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
            }
        }
        // check if they are logged yet
        if (isMissing(player.getUniqueId())) {
            NotBounties.debugMessage("Logging player name: " + player.getName(), false);
            // if not, add them
            DataManager.getPlayerData(player.getUniqueId()).setPlayerName(player.getName());
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
        return playerIDs.get(viableNames.get(0));
    }

    public static @NotNull String getPlayerName(@NotNull OfflinePlayer player) {
        if (player.isOnline() && player.getName() != null) {
            if (!isLogged(player.getName())) {
                logPlayer(player.getName(), player.getUniqueId());
                DataManager.getPlayerData(player.getUniqueId()).setPlayerName(player.getName());
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
        PlayerData playerData = DataManager.getPlayerData(uuid);
        if (playerData.getPlayerName() != null)
            return playerData.getPlayerName();
        webRequestPlayerName(uuid, playerData);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null) {
            playerData.setPlayerName(name);
            logPlayer(name, uuid);
            return name;
        }
        return uuid.toString();
    }

    private static void webRequestPlayerName(@NotNull UUID uuid, PlayerData playerData) {
        if (uuid.version() == 4 /* check if online player */ && !requestingNames.contains(uuid)) {
            requestingNames.add(uuid);
            loadHttpPool();
            httpPool.requestPlayerNameAsync(uuid, new HttpSyncPool.ResponseHandler())
                    .thenAccept(name -> {
                        playerData.setPlayerName(name);
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
        for (PlayerData playerData : DataManager.getAllPlayerData()) {
            if (playerData.getPlayerName() != null) {
                getDisplayName(playerData.getUuid());
            }
        }
    }

    public static String getDisplayName(OfflinePlayer p) {
        if (p == null)
            return "";
        if (p.isOnline()) {
            String name = Objects.requireNonNull(p.getPlayer()).getDisplayName();
            cachedNicknames.put(p.getUniqueId(), new CacheEntry(name, System.currentTimeMillis()));
            return name;
        }
        return getDisplayName(p.getUniqueId());
    }

    public static String getDisplayName(UUID uuid) {
        if (uuid == null)
            return "";
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
            return ConfigOptions.getAutoBounties().getConsoleBountyName();
        if (cachedNicknames.containsKey(uuid)) {
            CacheEntry entry = cachedNicknames.get(uuid);
            if (System.currentTimeMillis() - entry.loadedAt() > CACHE_REFRESH_TIME) {
                // refresh name so it isn't loaded more than once
                cachedNicknames.put(uuid, new CacheEntry(entry.value(), System.currentTimeMillis()));
                loadAPIDisplayNameAsync(uuid);
            }
            return entry.value();
        }
        String name = getPlayerName(uuid);
        cachedNicknames.put(uuid, new CacheEntry(name, System.currentTimeMillis()));
        loadAPIDisplayNameAsync(uuid);
        return name;
    }

    private static @Nullable String getAPIDisplayName(@NotNull UUID uuid) {
        if (ConfigOptions.getIntegrations().isEssentialsEnabled()) {
            String name = ConfigOptions.getIntegrations().getEssentialsXClass().getNick(uuid);
            if (name != null)
                return name;
        }
        if (ConfigOptions.getIntegrations().isCMIEnabled()) {
            String name = CMIClass.getNick(uuid);
            if (name != null)
                return name;
        }
        return null;
    }

    private static void loadAPIDisplayNameAsync(UUID uuid) {
        NotBounties.getServerImplementation().async().runNow(() -> {
            String name2 = getAPIDisplayName(uuid);
            if (name2 != null) {
                cachedNicknames.put(uuid, new CacheEntry(name2, System.currentTimeMillis()));
            } else {
                if (ConfigOptions.getIntegrations().isEssentialsEnabled() || ConfigOptions.getIntegrations().isCMIEnabled()){
                    NotBounties.debugMessage("Failed to get API display name for " + uuid + ".", true);
                    cachedNicknames.remove(uuid);
                }
            }

        });
    }
}


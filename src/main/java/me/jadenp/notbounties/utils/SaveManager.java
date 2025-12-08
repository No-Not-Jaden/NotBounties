package me.jadenp.notbounties.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.RemovePersistentEntitiesEvent;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.data.player_data.PlayerDataAdapter;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.settings.auto_bounties.RandomBounties;
import me.jadenp.notbounties.features.settings.auto_bounties.TimedBounties;
import me.jadenp.notbounties.features.settings.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.features.settings.databases.proxy.PreparedUpdateMessage;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import me.jadenp.notbounties.features.settings.display.BountyHunt;
import me.jadenp.notbounties.features.settings.display.BountyHuntTypeAdapter;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyBoardTypeAdapter;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class SaveManager {

    private static Plugin plugin;

    private static Map<String, Long> databaseSyncTimes = new HashMap<>();

    public static void loadSyncTime(AsyncDatabaseWrapper database) {
        if (databaseSyncTimes.containsKey(database.getName())) {
            database.setLastSync(databaseSyncTimes.remove(database.getName()));
        }
    }

    private SaveManager(){}

    public static void save(Plugin plugin) throws IOException {
        NotBounties.debugMessage("Saving...", false);

        File dataDirectory = new File(plugin.getDataFolder() + File.separator + "data");
        if (dataDirectory.mkdir())
            NotBounties.debugMessage("Created new data directory", false);

        if (!NotBounties.getInstance().isEnabled()) {
            // sync databases if the plugin is disabling
            for (AsyncDatabaseWrapper database : DataManager.getDatabases()) {
                if (database.isConnected()) {
                    DataManager.getAndSyncDatabase(database.getDatabase());
                }
            }
            saveUnsentProxyMessages(dataDirectory);
        }

        // the three amigos
        saveBounties(dataDirectory);
        saveStats(dataDirectory);
        savePlayerData(dataDirectory);

        BackupManager.saveBackups(dataDirectory, new File(plugin.getDataFolder() + File.separator + "backups"));
    }

    /**
     * Saves the current bounties to a bounties.json file in the data directory.
     *
     * @param dataDirectory Directory to save the file in.
     * @throws IOException If an error occurs while writing to the file.
     */
    private static void saveBounties(File dataDirectory) throws IOException {
        // save bounties
        File bountiesFile = new File(dataDirectory + File.separator + "bounties.json");
        if (bountiesFile.createNewFile()) {
            NotBounties.debugMessage("Created a new bounties.json file.", false);
        }
        try (JsonWriter writer = new JsonWriter(new FileWriter(bountiesFile))) {
            writer.beginArray();
            BountyTypeAdapter adapter = new BountyTypeAdapter();
            Set<Bounty> bounties = DataManager.getLocalBounties();
            NotBounties.debugMessage("Saving " + bounties.size() + " bounties.", false);
            for (Bounty bounty : bounties) {
                adapter.write(writer, bounty);
            }
            writer.endArray();
        }
    }

    /**
     * Saves the current player stats to a player_stats.json file in the data directory.
     *
     * @param dataDirectory Directory to save the file in.
     * @throws IOException If an error occurs while writing to the file.
     */
    private static void saveStats(File dataDirectory) throws IOException {
        // save stats
        File statsFile = new File(dataDirectory + File.separator + "player_stats.json");
        if (statsFile.createNewFile()) {
            NotBounties.debugMessage("Created a new player_stats.json file.", false);
        }
        try (JsonWriter writer = new JsonWriter(new FileWriter(statsFile))) {
            writer.beginArray();
            PlayerStatAdapter adapter = new PlayerStatAdapter();
            Set<Map.Entry<UUID, PlayerStat>> playerStatMap = DataManager.getLocalStats();
            NotBounties.debugMessage("Saving " + playerStatMap.size() + " stats.", false);
            for (Map.Entry<UUID, PlayerStat> entry : playerStatMap) {
                writer.beginObject();
                writer.name("uuid").value(entry.getKey().toString());
                writer.name("stats");
                adapter.write(writer, entry.getValue());
                writer.endObject();
            }
            writer.endArray();
        }
    }

    /**
     * Saves the current player data to a player_data.json file in the data directory.
     *
     * @param dataDirectory Directory to save the file in.
     * @throws IOException If an error occurs while writing to the file.
     */
    private static void savePlayerData(File dataDirectory) throws IOException {
        // save player data
        File playerDataFile = new File(dataDirectory + File.separator + "player_data.json");
        if (playerDataFile.createNewFile()) {
            NotBounties.debugMessage("Created a new player_data.json file.", false);
        }

        try (JsonWriter writer = new JsonWriter(new FileWriter(playerDataFile))) {
            writer.beginObject();
            writer.name("players");
            writer.beginArray();
            PlayerDataAdapter adapter = new PlayerDataAdapter();
            Set<PlayerData> playerDataList = DataManager.getLocalPlayerData();
            NotBounties.debugMessage("Saving " + playerDataList.size() + " player data.", false);
            for (PlayerData playerData : playerDataList) {
                writer.beginObject();
                writer.name("data");
                adapter.write(writer, playerData);
                writer.endObject();
            }
            writer.endArray();

            writer.name("trackedBounties");
            writer.beginArray();
            for (Map.Entry<Integer, UUID> entry : BountyTracker.getTrackedBounties().entrySet()) {
                writer.beginObject();
                writer.name("id").value(entry.getKey());
                writer.name("uuid").value(entry.getValue().toString());
                writer.endObject();
            }
            writer.endArray();

            writer.name("databaseSyncTimes");
            writer.beginArray();
            for (AsyncDatabaseWrapper database : DataManager.getDatabases()) {
                if (!database.isPermDatabase() && System.currentTimeMillis() - database.getLastSync() < DataManager.CONNECTION_REMEMBRANCE_MS) {
                    writer.beginObject();
                    writer.name("name").value(database.getName());
                    writer.name("time").value(database.getLastSync());
                    writer.endObject();
                }
            }
            writer.endArray();

            if (RandomBounties.isEnabled()) {
                writer.name("nextRandomBounty").value(RandomBounties.getNextRandomBounty());
                writer.name("randomBountyTimeHash").value(RandomBounties.getTimeHash());
            }

            if (TimedBounties.isEnabled()) {
                writer.name("nextTimedBounties");
                writer.beginArray();
                for (Map.Entry<UUID, Long> entry : TimedBounties.getNextBounties().entrySet()) {
                    writer.beginObject();
                    writer.name("uuid").value(entry.getKey().toString());
                    writer.name("time").value(entry.getValue());
                    writer.endObject();
                }
                writer.endArray();
            }

            writer.name("bountyBoards");
            writer.beginArray();
            BountyBoardTypeAdapter bountyBoardTypeAdapter = new BountyBoardTypeAdapter();
            for (BountyBoard board : BountyBoard.getBountyBoards()) {
                bountyBoardTypeAdapter.write(writer, board);
            }
            writer.endArray();

            writer.name("nextChallengeChange").value(ChallengeManager.getNextChallengeChange());
            writer.name("serverID").value(DataManager.getDatabaseServerID(false).toString());
            writer.name("paused").value(NotBounties.isPaused());
            writer.name("newPlayerImmunity").value(ImmunityManager.getNewPlayerImmunity());

            List<String> wantedTagLocations = DataManager.locationListToStringList(WantedTags.getLastLocations());
            if (!wantedTagLocations.isEmpty()) {
                writer.name("wantedTagLocations");
                writer.beginArray();
                for (String loc : wantedTagLocations)
                    writer.value(loc);
                writer.endArray();
            }

            List<BountyHunt> hunts = BountyHunt.getHunts();
            if (!hunts.isEmpty()) {
                writer.name("bountyHunts");
                writer.beginArray();
                BountyHuntTypeAdapter bountyHuntTypeAdapter = new BountyHuntTypeAdapter();
                for (BountyHunt hunt : hunts) {
                    bountyHuntTypeAdapter.write(writer, hunt);
                }
                writer.endArray();
            }

            writer.endObject();
        }
    }

    public static void read(Plugin plugin) throws IOException {
        SaveManager.plugin = plugin;

        File dataDirectory = new File(plugin.getDataFolder() + File.separator + "data");
        readPlayerData(dataDirectory);
        readBounties(dataDirectory);
        readStats(dataDirectory);

        List<byte[]> messages = readUnsentProxyMessages(dataDirectory);
        if (!messages.isEmpty())
            ProxyMessaging.addPreparedUpdateMessage(new PreparedUpdateMessage(messages, -1));
    }

    private static void readStats(File dataDirectory) throws IOException {
        File statsFile = new File(dataDirectory + File.separator + "player_stats.json");
        if (!statsFile.exists())
            return;
        try (JsonReader reader = new JsonReader(new FileReader(statsFile))) {
            try {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return;
                }
            } catch (EOFException e) {
                // empty file, ignore
                return;
            }
            reader.beginArray();
            PlayerStatAdapter adapter = new PlayerStatAdapter();
            while (reader.hasNext()) {
                reader.beginObject();
                UUID uuid = null;
                PlayerStat playerStat = null;
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("uuid"))
                        uuid = UUID.fromString(reader.nextString());
                    else if (name.equals("stats"))
                        playerStat = adapter.read(reader);
                }
                reader.endObject();
                DataManager.getLocalData().addStats(uuid, playerStat);
            }
            reader.endArray();
        }
    }

    private static void readBounties(File dataDirectory) throws IOException {
        File bountiesFile = new File(dataDirectory + File.separator + "bounties.json");
        if (!bountiesFile.exists())
            return;
        try (JsonReader reader = new JsonReader(new FileReader(bountiesFile))) {
            try {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return;
                }
            } catch (EOFException e) {
                // empty file, ignore
                return;
            }
            reader.beginArray();
            BountyTypeAdapter adapter = new BountyTypeAdapter();
            while (reader.hasNext()) {
                DataManager.getLocalData().addBounty(adapter.read(reader));
            }
            reader.endArray();
        }
    }

    private static void readPlayerData(File dataDirectory) throws IOException {
        ChallengeManager.setNextChallengeChange(1); // prepare new challenges if the last challenge change wasn't read
        File playerDataFile = new File(dataDirectory + File.separator + "player_data.json");
        if (playerDataFile.exists()) {
            try (JsonReader reader = new JsonReader(new FileReader(playerDataFile))) {
                try {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        return;
                    }
                } catch (EOFException e) {
                    // empty file, ignore
                    return;
                }
                List<PlayerData> playerDataList = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "players" -> {
                            playerDataList = readPlayers(reader);
                            DataManager.getLocalData().addPlayerData(playerDataList);
                        }
                        case "trackedBounties" -> BountyTracker.setTrackedBounties(readTrackedBounties(reader));
                        case "databaseSyncTimes" -> databaseSyncTimes = readDatabaseSyncTimes(reader);
                        case "nextRandomBounty" -> readNextRandomBounty(reader);
                        case "randomBountyTimeHash" -> RandomBounties.setTimeHash(reader.nextInt());
                        case "nextTimedBounties" -> TimedBounties.setNextBounties(readTimedBounties(reader));
                        case "bountyBoards" -> BountyBoard.addBountyBoards(readBountyBoards(reader));
                        case "nextChallengeChange" -> ChallengeManager.setNextChallengeChange(reader.nextLong());
                        case "serverID" -> DataManager.setDatabaseServerID(UUID.fromString(reader.nextString()));
                        case "paused" -> NotBounties.setPaused(reader.nextBoolean());
                        case "wantedTagLocations" -> readWantedTagLocations(reader);
                        case "newPlayerImmunity" -> ImmunityManager.setNewPlayerImmunity(reader.nextLong());
                        case "bountyHunts" -> readBountyHunts(reader);
                        default -> // unexpected name
                                reader.skipValue();
                    }
                }
                reader.endObject();
                // set the server ID for these entries
                // serverID is a new value, and old data may not have one set
                if (playerDataList != null)
                    for (PlayerData playerData : playerDataList)
                        if (playerData.getServerID() == null || playerData.getServerID().equals(DataManager.GLOBAL_SERVER_ID))
                            playerData.setServerID(DataManager.getDatabaseServerID(true));
            }
        }

        // tell LoggedPlayers that it can read all the player names and store them in an easy-to-read hashmap
        LoggedPlayers.loadPlayerData();
    }

    private static void readNextRandomBounty(JsonReader reader) throws IOException {
        RandomBounties.setNextRandomBounty(reader.nextLong());
        if (!RandomBounties.isEnabled()) {
            RandomBounties.setNextRandomBounty(0);
        } else if (RandomBounties.getNextRandomBounty() == 0) {
            RandomBounties.setNextRandomBounty();
        }
    }

    private static void readBountyHunts(JsonReader reader) throws IOException {
        List<BountyHunt> hunts = new LinkedList<>();
        reader.beginArray();
        BountyHuntTypeAdapter bountyHuntTypeAdapter = new BountyHuntTypeAdapter();
        while (reader.hasNext()) {
            BountyHunt hunt = bountyHuntTypeAdapter.read(reader);
            if (hunt != null)
                hunts.add(hunt);
            else
                plugin.getLogger().info("Could not load a saved bounty hunt");
        }
        reader.endArray();
        BountyHunt.loadSavedHunts(hunts);
    }

    private static void readWantedTagLocations(JsonReader reader) throws IOException {
        List<Location> locations = getWantedTagLocations(reader);
        if (!locations.isEmpty()) {
            NotBounties.getServerImplementation().global().runDelayed(task -> {
                RemovePersistentEntitiesEvent.cleanChunks(locations);
            }, 500);
        }
    }

    private static List<Location> getWantedTagLocations(JsonReader reader) throws IOException {
        List<String> stringList = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            stringList.add(reader.nextString());
        }
        reader.endArray();

        return DataManager.stringListToLocationList(stringList);
    }

    private static List<BountyBoard> readBountyBoards(JsonReader reader) throws IOException {
        List<BountyBoard> bountyBoards = new LinkedList<>();
        reader.beginArray();
        BountyBoardTypeAdapter bountyBoardTypeAdapter = new BountyBoardTypeAdapter();
        while (reader.hasNext()) {
            BountyBoard bountyBoard = bountyBoardTypeAdapter.read(reader);
            if (bountyBoard != null)
                bountyBoards.add(bountyBoard);
            else
                plugin.getLogger().info("Could not load a saved bounty board. (Location does not exist)");
        }
        reader.endArray();

        return bountyBoards;
    }

    private static Map<String, Long> readDatabaseSyncTimes(JsonReader reader) throws IOException {
        Map<String, Long> syncMap = new HashMap<>();
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            String dbName = null;
            long time = 0;
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name" -> dbName = reader.nextString();
                    case "time" -> time = reader.nextLong();
                    default -> // unexpected name
                            reader.skipValue();
                }
            }
            reader.endObject();
            syncMap.put(dbName, time);
        }
        reader.endArray();

        return syncMap;
    }

    private static BiMap<Integer, UUID> readTrackedBounties(JsonReader reader) throws IOException {
        BiMap<Integer, UUID> map = HashBiMap.create();
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            int num = -404;
            UUID uuid = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("id"))
                    num = reader.nextInt();
                else if (name.equals("uuid"))
                    uuid = UUID.fromString(reader.nextString());
            }
            reader.endObject();
            map.put(num, uuid);
        }
        reader.endArray();

        return map;
    }

    private static Map<UUID, Long> readTimedBounties(JsonReader reader) throws IOException {
        Map<UUID, Long> timedBounties = new HashMap<>();
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            UUID uuid = null;
            long time = 0;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("time"))
                    time = reader.nextLong();
                else if (name.equals("uuid"))
                    uuid = UUID.fromString(reader.nextString());
            }
            reader.endObject();
            timedBounties.put(uuid, time);
        }
        reader.endArray();

        return timedBounties;
    }

    private static List<PlayerData> readPlayers(JsonReader reader) throws IOException {
        List<PlayerData> data = new ArrayList<>();
        reader.beginArray();
        PlayerDataAdapter adapter = new PlayerDataAdapter();
        while (reader.hasNext()) {
            reader.beginObject();
            UUID uuid = null;
            PlayerData playerData = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("uuid"))
                    uuid = UUID.fromString(reader.nextString());
                else if (name.equals("data"))
                    playerData = adapter.read(reader);
            }
            reader.endObject();
            if (playerData != null) {
                if (uuid != null)
                    playerData.setUuid(uuid);
                data.add(playerData);
            }
        }
        reader.endArray();

        return data;
    }

    public static void saveUnsentProxyMessages(File dataDirectory) throws IOException {
        List<byte[]> messages = ProxyMessaging.getUnsentMessages();
        File saveFile = new File(dataDirectory + File.separator + "proxy_message_cache.bin");
        if (!saveFile.exists()) {
            if (saveFile.createNewFile())
                NotBounties.getInstance().getLogger().info("Created a new proxy_message_cache.bin file.");
        }
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(saveFile)))) {
            // Write number of arrays
            dos.writeInt(messages.size());

            for (byte[] arr : messages) {
                // Write array length
                dos.writeInt(arr.length);
                // Write array contents
                dos.write(arr);
            }
        }
    }

    public static List<byte[]> readUnsentProxyMessages(File dataDirectory) throws IOException{
        List<byte[]> messages = new LinkedList<>();
        File saveFile = new File(dataDirectory + File.separator + "proxy_message_cache.bin");
        if (!saveFile.exists())
            return messages;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(saveFile)))) {
            int count = dis.readInt(); // number of arrays

            for (int i = 0; i < count; i++) {
                int length = dis.readInt();
                byte[] arr = new byte[length];
                dis.readFully(arr);
                messages.add(arr);
            }
        }

        if (Files.deleteIfExists(saveFile.toPath()))
            NotBounties.debugMessage("Deleted proxy_message_cache.bin.", false);

        return messages;
    }
}

package me.jadenp.notbounties.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.RemovePersistentEntitiesEvent;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.settings.auto_bounties.RandomBounties;
import me.jadenp.notbounties.features.settings.auto_bounties.TimedBounties;
import me.jadenp.notbounties.features.settings.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyBoardTypeAdapter;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SaveManager {

    private static Plugin plugin;

    private SaveManager(){}

    public static void save(Plugin plugin) throws IOException {
        NotBounties.debugMessage("Saving...", false);

        File dataDirectory = new File(plugin.getDataFolder() + File.separator + "data");
        if (dataDirectory.mkdir())
            NotBounties.debugMessage("Created new data directory", false);

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
            for (Bounty bounty : DataManager.getLocalBounties()) {
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
            for (Map.Entry<UUID, PlayerStat> entry : DataManager.getLocalStats()) {
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
            for (Map.Entry<UUID, PlayerData> entry : DataManager.getPlayerDataMap().entrySet()) {
                writer.beginObject();
                writer.name("uuid").value(entry.getKey().toString());
                writer.name("data");
                adapter.write(writer, entry.getValue());
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

            if (RandomBounties.isEnabled())
                writer.name("nextRandomBounty").value(RandomBounties.getNextRandomBounty());

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
            writer.endObject();
        }
    }

    public static void read(Plugin plugin) throws IOException {
        SaveManager.plugin = plugin;

        File dataDirectory = new File(plugin.getDataFolder() + File.separator + "data");
        readPlayerData(dataDirectory);
        readBounties(dataDirectory);
        readStats(dataDirectory);
    }

    private static void readStats(File dataDirectory) throws IOException {
        File statsFile = new File(dataDirectory + File.separator + "player_stats.json");
        if (!statsFile.exists())
            return;
        try (JsonReader reader = new JsonReader(new FileReader(statsFile))) {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
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
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
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
        if (!playerDataFile.exists())
            return;
        try (JsonReader reader = new JsonReader(new FileReader(playerDataFile))) {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return;
            }
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "players" -> DataManager.addPlayerData(readPlayers(reader));
                    case "trackedBounties" -> BountyTracker.setTrackedBounties(readTrackedBounties(reader));
                    case "databaseSyncTimes" -> {
                        Map<String, Long> syncTimes = readDatabaseSyncTimes(reader);
                        for (AsyncDatabaseWrapper databaseWrapper : DataManager.getDatabases()) {
                            if (syncTimes.containsKey(databaseWrapper.getName()))
                                databaseWrapper.setLastSync(syncTimes.get(databaseWrapper.getName()));
                        }
                    }
                    case "nextRandomBounty" -> {
                        RandomBounties.setNextRandomBounty(reader.nextLong());
                        if (!RandomBounties.isEnabled()) {
                            RandomBounties.setNextRandomBounty(0);
                        } else if (RandomBounties.getNextRandomBounty() == 0) {
                            RandomBounties.setNextRandomBounty();
                        }
                    }
                    case "nextTimedBounties" -> TimedBounties.setNextBounties(readTimedBounties(reader));
                    case "bountyBoards" -> BountyBoard.addBountyBoards(readBountyBoards(reader));
                    case "nextChallengeChange" -> ChallengeManager.setNextChallengeChange(reader.nextLong());
                    case "serverID" -> DataManager.setDatabaseServerID(UUID.fromString(reader.nextString()));
                    case "paused" -> NotBounties.setPaused(reader.nextBoolean());
                    case "wantedTagLocations" -> readWantedTagLocations(reader);
                    case "newPlayerImmunity" -> ImmunityManager.setNewPlayerImmunity(reader.nextLong());
                    default -> // unexpected name
                            reader.skipValue();
                }
            }
            reader.endObject();
        }

        // tell LoggedPlayers that it can read all the player names and store them in an easy to read hashmap
        LoggedPlayers.loadPlayerData();
    }

    private static void readWantedTagLocations(JsonReader reader) throws IOException {
        List<Location> locations = getWantedTagLocations(reader);
        if (!locations.isEmpty()) {
            NotBounties.getServerImplementation().global().runDelayed(task -> {
                RemovePersistentEntitiesEvent.cleanChunks(locations);
            }, 100);
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

    private static Map<UUID, PlayerData> readPlayers(JsonReader reader) throws IOException {
        Map<UUID, PlayerData> data = new HashMap<>();
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
            data.put(uuid, playerData);
        }
        reader.endArray();

        return data;
    }
}

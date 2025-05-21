package me.jadenp.notbounties.utils;

import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.settings.auto_bounties.RandomBounties;
import me.jadenp.notbounties.features.settings.auto_bounties.TimedBounties;
import me.jadenp.notbounties.features.settings.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyBoardTypeAdapter;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SaveManager {

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
}

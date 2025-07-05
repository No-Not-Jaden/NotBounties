package me.jadenp.notbounties.features.settings.display;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BountyHuntTypeAdapter extends TypeAdapter<BountyHunt> {
    @Override
    public void write(JsonWriter jsonWriter, BountyHunt bountyHunt) throws IOException {
        if (bountyHunt == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.beginObject();
        OfflinePlayer setter = bountyHunt.getSetter();
        if (setter != null)
            jsonWriter.name("setter").value(setter.getUniqueId().toString());
        jsonWriter.name("huntedPlayer").value(bountyHunt.getHuntedPlayer().getUniqueId().toString());
        jsonWriter.name("durationLeft").value(bountyHunt.getEndTime() - System.currentTimeMillis());
        jsonWriter.name("totalTime").value(bountyHunt.getEndTime() - bountyHunt.getStartTime());
        // trackers will be removed after restart
        /*jsonWriter.name("givenTrackers");
        jsonWriter.beginArray();
        for (UUID uuid : bountyHunt.getGivenTrackers()) {
            jsonWriter.value(uuid.toString());
        }
        jsonWriter.endArray();*/
        jsonWriter.endObject();
    }

    @Override
    public BountyHunt read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        jsonReader.beginObject();
        UUID setter = null;
        UUID huntedPlayer = null;
        long durationLeft = 0;
        long totalTime = 0;
        Set<UUID> givenTrackers = new HashSet<>();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "setter" -> setter = UUID.fromString(jsonReader.nextString());
                case "huntedPlayer" -> huntedPlayer = UUID.fromString(jsonReader.nextString());
                case "durationLeft" -> durationLeft = jsonReader.nextLong();
                case "totalTime" -> totalTime = jsonReader.nextLong();
                case "givenTrackers" -> {
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        givenTrackers.add(UUID.fromString(jsonReader.nextString()));
                    }
                    jsonReader.endArray();
                }
                default -> jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        if (huntedPlayer == null)
            return null;
        return new BountyHunt(setter, huntedPlayer, durationLeft, totalTime, givenTrackers);
    }
}

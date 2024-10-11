package me.jadenp.notbounties;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.utils.DataManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BountyTypeAdapter extends TypeAdapter<Bounty> {
    @Override
    public void write(JsonWriter writer, Bounty bounty) throws IOException {
        if (bounty == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("name").value(bounty.getName());
        writer.name("uuid").value(bounty.getUUID().toString());
        writer.name("server-id").value(bounty.getServerID().toString());
        writer.name("setters");
        writer.beginArray();
        TypeAdapter<Setter> setterTypeAdapter = new SetterTypeAdapter();
        for (Setter setter : bounty.getSetters()) {
            setterTypeAdapter.write(writer, setter);
        }
        writer.endArray();
        writer.endObject();
    }

    @Override
    public Bounty read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        reader.beginObject();
        String playerName = null;
        UUID uuid = null;
        UUID serverID = DataManager.GLOBAL_SERVER_ID;
        List<Setter> setters = null;
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "name" -> playerName = reader.nextString();
                case "uuid" -> uuid = UUID.fromString(reader.nextString());
                case "setters" -> setters = readSetters(reader);
                case "server-id" -> serverID = UUID.fromString(reader.nextString());
                default -> {
                    // unexpected data
                    // this shouldn't be reached
                }
            }
        }
        reader.endObject();
        return new Bounty(uuid, setters, playerName, serverID);
    }

    private List<Setter> readSetters(JsonReader reader) throws IOException {
        List<Setter> setters = new ArrayList<>();
        reader.beginArray();
        TypeAdapter<Setter> setterTypeAdapter = new SetterTypeAdapter();
        while (reader.hasNext()) {
            setters.add(setterTypeAdapter.read(reader));
        }
        reader.endArray();

        return setters;
    }


}

package me.jadenp.notbounties.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;

public class WhitelistTypeAdapter extends TypeAdapter<Whitelist> {
    @Override
    public void write(JsonWriter jsonWriter, Whitelist whitelist) throws IOException {
        if (whitelist == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.beginObject();
        jsonWriter.name("blacklist").value(whitelist.isBlacklist());
        jsonWriter.name("uuids");
        jsonWriter.beginArray();
        for (UUID uuid : whitelist.getList()) {
            jsonWriter.value(uuid.toString());
        }
        jsonWriter.endArray();
        jsonWriter.endObject();
    }

    @Override
    public Whitelist read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }

        boolean blacklist = false;
        SortedSet<UUID> uuids = new TreeSet<>();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if (name.equals("blacklist")) {
                blacklist = jsonReader.nextBoolean();
            } else if (name.equals("uuids")) {
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    uuids.add(UUID.fromString(jsonReader.nextString()));
                }
                jsonReader.endArray();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        return new Whitelist(uuids, blacklist);
    }
}

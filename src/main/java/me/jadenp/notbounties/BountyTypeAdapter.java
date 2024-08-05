package me.jadenp.notbounties;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.utils.Whitelist;
import org.bukkit.inventory.ItemStack;

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
        writer.name("setters");
        writer.beginArray();
        TypeAdapter<Setter> setterTypeAdapter = new Gson().getAdapter(Setter.class);
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
        String playerName = null;
        UUID uuid = null;
        List<Setter> setters = null;
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "name" -> playerName = reader.nextString();
                case "uuid" -> uuid = UUID.fromString(reader.nextString());
                case "setters" -> setters = readSetters(reader);
            }
        }
        reader.close();
        return new Bounty(uuid, setters, playerName);
    }

    private List<Setter> readSetters(JsonReader reader) throws IOException {
        List<Setter> setters = new ArrayList<>();
        reader.beginArray();
        TypeAdapter<Setter> setterTypeAdapter = new Gson().getAdapter(Setter.class);
        while (reader.hasNext()) {
            setters.add(setterTypeAdapter.read(reader));
        }
        reader.beginArray();

        return setters;
    }


}

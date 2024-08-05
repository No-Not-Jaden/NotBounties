package me.jadenp.notbounties;

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

public class SetterTypeAdapter extends TypeAdapter<Setter> {
    @Override
    public void write(JsonWriter writer, Setter setter) throws IOException {
        writer.beginObject();
        writer.name("name").value(setter.getName());
        writer.name("uuid").value(setter.getUuid().toString());
        writer.name("amount").value(setter.getAmount());
        writer.name("items");
        if (setter.getItems().isEmpty()) {
            writer.nullValue();
        } else {
            writer.value(SerializeInventory.itemStackArrayToBase64(setter.getItems().toArray(new ItemStack[0])));
        }

        writer.name("time").value(setter.getTimeCreated());
        writer.name("playtime").value(setter.getReceiverPlaytime());
        writer.name("notified").value(setter.isNotified());
        writer.name("display").value(setter.getDisplayAmount());
        writer.name("whitelist");
        writer.beginObject();
        Whitelist whitelist = setter.getWhitelist();
        writer.name("blacklist").value(whitelist.isBlacklist());
        writer.name("uuids");
        writer.beginArray();
        for (UUID uuid : whitelist.getList()) {
            writer.value(uuid.toString());
        }
        writer.endArray();
        writer.endObject();
        writer.endObject();
    }

    @Override
    public Setter read(JsonReader reader) throws IOException {
        return readSetter(reader);
    }

    private Setter readSetter(JsonReader reader) throws IOException {
        String playerName = null;
        UUID uuid = null;
        double amount = 0;
        List<ItemStack> itemStacks = new ArrayList<>();
        long time = 0;
        long playtime = 0;
        boolean notified = false;
        double display = 0;
        Whitelist whitelist = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("name")) {
                playerName = reader.nextString();
            } else if (name.equals("uuid")) {
                uuid = UUID.fromString(reader.nextString());
            } else if (name.equals("amount")) {
                amount = reader.nextDouble();
            } else if (name.equals("items") && reader.peek() != JsonToken.NULL) {
                itemStacks.addAll(List.of(SerializeInventory.itemStackArrayFromBase64(reader.nextString())));
            } else if (name.equals("time")) {
                time = reader.nextLong();
            } else if (name.equals("playtime")) {
                playtime = reader.nextLong();
            } else if (name.equals("notified")) {
                notified = reader.nextBoolean();
            } else if (name.equals("display")) {
                display = reader.nextDouble();
            } else if (name.equals("whitelist")) {
                whitelist = readWhitelist(reader);
            }
        }
        reader.endObject();

        return new Setter(playerName, uuid, amount, itemStacks, time, notified, whitelist, playtime, display);
    }

    private Whitelist readWhitelist(JsonReader reader) throws IOException {
        boolean blacklist = false;
        List<UUID> uuids = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("blacklist")) {
                blacklist = reader.nextBoolean();
            } else if (name.equals("uuids")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    uuids.add(UUID.fromString(reader.nextString()));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Whitelist(uuids, blacklist);
    }
}

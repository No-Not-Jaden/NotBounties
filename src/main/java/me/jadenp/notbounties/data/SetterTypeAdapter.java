package me.jadenp.notbounties.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.utils.SerializeInventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;

public class SetterTypeAdapter extends TypeAdapter<Setter> {
    @Override
    public void write(JsonWriter writer, Setter setter) throws IOException {
        if (setter == null) {
            writer.nullValue();
            return;
        }

        writer.beginObject();
        writer.name("name").value(setter.getName());
        writer.name("uuid").value(setter.getUuid().toString());
        writer.name("amount").value(setter.getAmount());
        writer.name("items");
        if (setter.hasItems() && setter.isItemsLoaded()) {
            writer.value(SerializeInventory.itemStackArrayToBase64(setter.getItems().join().toArray(new ItemStack[0])));
        } else {
            writer.nullValue();
        }
        writer.name("hasItems").value(setter.hasItems());

        writer.name("time").value(setter.getTimeCreated());
        writer.name("playtime").value(setter.getReceiverPlaytime());
        writer.name("notified").value(setter.isNotified());
        writer.name("display").value(setter.getDisplayAmount());
        writer.name("whitelist");
        new WhitelistTypeAdapter().write(writer, setter.getWhitelist());
        writer.name("tags");
        writer.beginArray();
        for (String tag : setter.getTags()) {
            writer.value(tag);
        }
        writer.endArray();
        writer.name("bountyId");
        Optional<Integer> id = setter.getBountyId();
        if (id.isPresent()) {
            writer.value(id.get());
        } else {
            writer.nullValue();
        }
        writer.endObject();
    }

    @Override
    public Setter read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }

        UUID uuid = null;
        double amount = 0;
        List<ItemStack> itemStacks = new ArrayList<>();
        boolean hasItems = false;
        long time = 0;
        long playtime = 0;
        boolean notified = false;
        double display = 0;
        Whitelist whitelist = new Whitelist(new TreeSet<>(), false);
        Set<String> tags = new HashSet<>();
        Integer bountyId = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "uuid" -> uuid = UUID.fromString(reader.nextString());
                case "amount" -> amount = reader.nextDouble();
                case "items" -> {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                    } else {
                        itemStacks.addAll(List.of(SerializeInventory.itemStackArrayFromBase64(reader.nextString())));
                    }
                }
                case "hasItems" -> hasItems = reader.nextBoolean();
                case "time" -> time = reader.nextLong();
                case "playtime" -> playtime = reader.nextLong();
                case "notified" -> notified = reader.nextBoolean();
                case "display" -> display = reader.nextDouble();
                case "whitelist" -> whitelist = new WhitelistTypeAdapter().read(reader);
                case "tags" ->  {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        tags.add(reader.nextString());
                    }
                    reader.endArray();
                }
                case "bountyId" -> {
                    if (reader.peek() != JsonToken.NULL)
                        bountyId = reader.nextInt();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (!itemStacks.isEmpty()) {
            return new Setter(bountyId, uuid, amount, time, hasItems, notified, whitelist, playtime, display, tags);
        }
        return new Setter(bountyId, uuid, amount, time, itemStacks, notified, whitelist, playtime, display, tags);
    }

}

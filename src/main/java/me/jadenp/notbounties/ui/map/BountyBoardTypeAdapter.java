package me.jadenp.notbounties.ui.map;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class BountyBoardTypeAdapter extends TypeAdapter<BountyBoard> {
    @Override
    public void write(JsonWriter jsonWriter, BountyBoard bountyBoard) throws IOException {
        if (bountyBoard == null) {
            jsonWriter.nullValue();
            return;
        }

        jsonWriter.beginObject();
        jsonWriter.name("rank").value(bountyBoard.getRank());
        jsonWriter.name("direction").value(bountyBoard.getDirection().toString());
        writeLocation(jsonWriter, bountyBoard.getLocation());
        jsonWriter.endObject();
    }

    private void writeLocation(JsonWriter jsonWriter, Location location) throws IOException {
        if (location.getWorld() == null)
            return;
        jsonWriter.name("location");
        jsonWriter.beginObject();
        jsonWriter.name("world").value(location.getWorld().getUID().toString());
        jsonWriter.name("x").value(location.getX());
        jsonWriter.name("y").value(location.getY());
        jsonWriter.name("z").value(location.getZ());
        jsonWriter.name("pitch").value(location.getPitch());
        jsonWriter.name("yaw").value(location.getYaw());
        jsonWriter.endObject();
    }

    private Location readLocation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        World world = null;
        double x = 0;
        double y = 0;
        double z = 0;
        float pitch = 0;
        float yaw = 0;
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "world" -> world = Bukkit.getWorld(UUID.fromString(jsonReader.nextString()));
                case "x" -> x = jsonReader.nextDouble();
                case "y" -> y = jsonReader.nextDouble();
                case "z" -> z = jsonReader.nextDouble();
                case "pitch" -> pitch = (float) jsonReader.nextDouble();
                case "yaw" -> yaw = (float) jsonReader.nextDouble();
                default -> {
                    // unexpected name
                }
            }
        }
        jsonReader.endObject();
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public @Nullable BountyBoard read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        jsonReader.beginObject();
        int rank = 1;
        Location location = null;
        BlockFace direction = null;
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "rank" -> rank = jsonReader.nextInt();
                case "direction" -> direction = BlockFace.valueOf(jsonReader.nextString());
                case "location" -> location = readLocation(jsonReader);
                default -> {
                    // unexpected name
                }
            }
        }
        jsonReader.endObject();
        if (location == null)
            return null;
        return new BountyBoard(location, direction, rank);
    }
}

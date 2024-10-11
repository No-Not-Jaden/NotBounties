package me.jadenp.notbounties.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

public class PlayerStatAdapter extends TypeAdapter<PlayerStat> {
    @Override
    public void write(JsonWriter writer, PlayerStat playerStat) throws IOException {
        if (playerStat == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("kills").value(playerStat.kills());
        writer.name("set").value(playerStat.set());
        writer.name("deaths").value(playerStat.deaths());
        writer.name("all").value(playerStat.all());
        writer.name("immunity").value(playerStat.immunity());
        writer.name("server-id").value(playerStat.serverID().toString());
        writer.endObject();
    }

    @Override
    public PlayerStat read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        reader.beginObject();
        long kills = 0;
        long set = 0;
        long deaths = 0;
        double all = 0;
        double immunity = 0;
        double claimed = 0;
        UUID serverID = DataManager.getDatabaseServerID(false);
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "kills" -> kills = reader.nextLong();
                case "set" -> set = reader.nextLong();
                case "deaths" -> deaths = reader.nextLong();
                case "all" -> all = reader.nextDouble();
                case "immunity" -> immunity = reader.nextDouble();
                case "claimed" -> claimed = reader.nextDouble();
                case "server-id" -> serverID = UUID.fromString(reader.nextString());
                default -> {
                    // unknown stat
                }
            }
        }
        reader.endObject();
        return new PlayerStat(kills, set, deaths, all, immunity, claimed, serverID);
    }
}

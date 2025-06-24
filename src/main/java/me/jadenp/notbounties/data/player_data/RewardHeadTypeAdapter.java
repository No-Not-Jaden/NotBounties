package me.jadenp.notbounties.data.player_data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

public class RewardHeadTypeAdapter extends TypeAdapter<RewardHead> {
    @Override
    public void write(JsonWriter jsonWriter, RewardHead rewardHead) throws IOException {
        if (rewardHead == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.beginObject();
        jsonWriter.name("uuid").value(rewardHead.getUuid().toString());
        jsonWriter.name("amount").value(rewardHead.getAmount());
        jsonWriter.name("killer").value(rewardHead.getKiller().toString());
        jsonWriter.name("time").value(rewardHead.getLatestUpdate());
        if (rewardHead.getReason() != null)
            jsonWriter.name("reason").value(rewardHead.getReason());
        jsonWriter.endObject();
    }

    @Override
    public RewardHead read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        jsonReader.beginObject();
        UUID uuid = null;
        UUID killer = null;
        double amount = 0;
        long time = System.currentTimeMillis();
        String reason = null;
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "uuid" -> uuid = UUID.fromString(jsonReader.nextString());
                case "killer" -> killer = UUID.fromString(jsonReader.nextString());
                case "amount" -> amount = jsonReader.nextDouble();
                case "time" -> time = jsonReader.nextLong();
                case "reason" -> reason = jsonReader.nextString();
                default -> jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        if (uuid != null && killer != null) {
            return new RewardHead(uuid, killer, amount, time, reason);
        }
        return null;
    }
}

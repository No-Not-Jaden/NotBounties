package me.jadenp.notbounties.data.player_data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ItemRefundTypeAdapter extends TypeAdapter<ItemRefund> {
    @Override
    public void write(JsonWriter jsonWriter, ItemRefund itemRefund) throws IOException {
        if (itemRefund == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.beginObject();
        jsonWriter.name("items").value(itemRefund.getEncodedRefund());
        jsonWriter.name("time").value(itemRefund.getLatestUpdate());
        if (itemRefund.getReason() != null)
            jsonWriter.name("reason").value(itemRefund.getReason());
        jsonWriter.endObject();
    }

    @Override
    public ItemRefund read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        jsonReader.beginObject();
        String encodedRefund = null;
        long time = 0;
        String reason = null;
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "items" -> encodedRefund = jsonReader.nextString();
                case "time" -> time = jsonReader.nextLong();
                case "reason" -> reason = jsonReader.nextString();
                default -> jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        return new ItemRefund(encodedRefund, time, reason);
    }
}

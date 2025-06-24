package me.jadenp.notbounties.data.player_data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class AmountRefundTypeAdapter extends TypeAdapter<AmountRefund> {
    @Override
    public void write(JsonWriter jsonWriter, AmountRefund amountRefund) throws IOException {
        if (amountRefund == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.beginObject();
        jsonWriter.name("amount").value(amountRefund.getRefund());
        jsonWriter.name("time").value(amountRefund.getLatestUpdate());
        if (amountRefund.getReason() != null)
            jsonWriter.name("reason").value(amountRefund.getReason());
        jsonWriter.endObject();
    }

    @Override
    public AmountRefund read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        jsonReader.beginObject();
        double refund = 0;
        long time = 0;
        String reason = null;
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "amount" -> refund = jsonReader.nextDouble();
                case "time" -> time = jsonReader.nextLong();
                case "reason" -> reason = jsonReader.nextString();
                default -> jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        return new AmountRefund(refund, time, reason);
    }
}

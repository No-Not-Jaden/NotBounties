package me.jadenp.notbounties.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.utils.SerializeInventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;

public class PlayerDataAdapter extends TypeAdapter<PlayerData> {

    @Override
    public void write(JsonWriter jsonWriter, PlayerData playerData) throws IOException {
        if (playerData == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.beginObject();
        jsonWriter.name("playerName").value(playerData.getPlayerName());
        writeImmunity(jsonWriter, playerData);
        TimeZone timeZone = playerData.getTimeZone();
        if (timeZone != null)
            jsonWriter.name("timeZone").value(timeZone.getID());
        writeRefund(jsonWriter, playerData.getRefundAmount(), playerData.getRefundItems());
        jsonWriter.name("disableBroadcast").value(playerData.getBroadcastSettings().toString());
        writeRewardHeads(jsonWriter, playerData.getRewardHeads());
        jsonWriter.name("bountyCooldown").value(playerData.getBountyCooldown());
        jsonWriter.name("whitelist");
        new WhitelistTypeAdapter().write(jsonWriter, playerData.getWhitelist());
        jsonWriter.name("newPlayer").value(playerData.isNewPlayer());
        jsonWriter.name("lastSeen").value(playerData.getLastSeen());
        jsonWriter.endObject();
    }

    private void writeRewardHeads(JsonWriter jsonWriter, List<RewardHead> rewardHeads) throws IOException {
        if (!rewardHeads.isEmpty()) {
            jsonWriter.name("rewardHeads");
            jsonWriter.beginArray();
            for (RewardHead rewardHead : rewardHeads) {
                jsonWriter.beginObject();
                jsonWriter.name("uuid").value(rewardHead.uuid().toString());
                jsonWriter.name("killer").value(rewardHead.killer().toString());
                jsonWriter.name("amount").value(rewardHead.amount());
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
        }
    }

    private void writeRefund(JsonWriter jsonWriter, double amount, List<ItemStack> items) throws IOException {
        if (amount > 0)
            jsonWriter.name("refundAmount").value(amount);
        if (!items.isEmpty())
            jsonWriter.name("refundItems").value(SerializeInventory.itemStackArrayToBase64(items.toArray(new ItemStack[0])));
    }

    private void writeImmunity(JsonWriter jsonWriter, PlayerData playerData) throws IOException {
        jsonWriter.name("immunity");
        jsonWriter.beginArray();
        if (playerData.hasGeneralImmunity())
            jsonWriter.value("general");
        if (playerData.hasMurderImmunity())
            jsonWriter.value("murder");
        if (playerData.hasRandomImmunity())
            jsonWriter.value("random");
        if (playerData.hasTimedImmunity())
            jsonWriter.value("time");
        jsonWriter.endArray();
    }

    @Override
    public PlayerData read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        jsonReader.beginObject();
        PlayerData playerData = new PlayerData();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "playerName" -> {
                    if (jsonReader.peek() == JsonToken.NULL) {
                        jsonReader.nextNull();
                    } else {
                        playerData.setPlayerName(jsonReader.nextString());
                    }
                }
                case "immunity" -> readImmunity(jsonReader, playerData);
                case "timeZone" -> playerData.setTimeZone(TimeZone.getTimeZone(jsonReader.nextString()));
                case "refundAmount" -> playerData.addRefund(jsonReader.nextDouble());
                case "refundItems" -> playerData.addRefund(Arrays.asList(SerializeInventory.itemStackArrayFromBase64(jsonReader.nextString())));
                case "disableBroadcast" -> readBroadcast(jsonReader, playerData);
                case "rewardHeads" -> readRewardHeads(jsonReader, playerData);
                case "bountyCooldown" -> playerData.setBountyCooldown(jsonReader.nextLong());
                case "whitelist" -> playerData.setWhitelist(new WhitelistTypeAdapter().read(jsonReader));
                case "newPlayer" -> playerData.setNewPlayer(jsonReader.nextBoolean());
                case "lastSeen" -> playerData.setLastSeen(jsonReader.nextLong());
                default -> // unexpected name
                        jsonReader.skipValue();
            }
        }

        jsonReader.endObject();
        return playerData;
    }

    private void readBroadcast(JsonReader jsonReader, PlayerData playerData) throws IOException {
        PlayerData.BroadcastSettings broadcastSettings;
        if (jsonReader.peek() == JsonToken.BOOLEAN) {
            // old style
            broadcastSettings = jsonReader.nextBoolean() ? PlayerData.BroadcastSettings.EXTENDED : PlayerData.BroadcastSettings.DISABLE;
        } else {
            try {
                broadcastSettings = PlayerData.BroadcastSettings.valueOf(jsonReader.nextString());
            } catch (IllegalArgumentException e) {
                // unknown value
                broadcastSettings = PlayerData.BroadcastSettings.EXTENDED;
            }
        }
        playerData.setBroadcastSettings(broadcastSettings);
    }

    private void readRewardHeads(JsonReader jsonReader, PlayerData playerData) throws IOException {
        List<RewardHead> rewardHeads = new LinkedList<>();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            UUID uuid = null;
            UUID killer = null;
            double amount = 0;
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                switch (name) {
                    case "uuid" -> uuid = UUID.fromString(jsonReader.nextString());
                    case "killer" -> killer = UUID.fromString(jsonReader.nextString());
                    case "amount" -> amount = jsonReader.nextDouble();
                    default -> {
                        // unexpected name
                    }
                }
            }
            rewardHeads.add(new RewardHead(uuid, killer, amount));
            jsonReader.endObject();
        }
        jsonReader.endArray();
        playerData.addRewardHeads(rewardHeads);
    }

    private void readImmunity(JsonReader jsonReader, PlayerData playerData) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            String type = jsonReader.nextString();
            switch (type) {
                case "general" -> playerData.setGeneralImmunity(true);
                case "murder" -> playerData.setMurderImmunity(true);
                case "random" -> playerData.setRandomImmunity(true);
                case "time" -> playerData.setTimedImmunity(true);
                default -> {
                    // unexpected value
                }
            }
        }
        jsonReader.endArray();
    }
}

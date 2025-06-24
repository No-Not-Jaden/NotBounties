package me.jadenp.notbounties.data.player_data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.data.WhitelistTypeAdapter;
import org.bukkit.Bukkit;

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
        jsonWriter.name("uuid").value(playerData.getUuid().toString());
        jsonWriter.name("playerName").value(playerData.getPlayerName());
        writeImmunity(jsonWriter, playerData);
        TimeZone timeZone = playerData.getTimeZone();
        if (timeZone != null)
            jsonWriter.name("timeZone").value(timeZone.getID());
        writeRefund(jsonWriter, playerData.getRefund());
        jsonWriter.name("disableBroadcast").value(playerData.getBroadcastSettings().toString());
        jsonWriter.name("bountyCooldown").value(playerData.getBountyCooldown());
        jsonWriter.name("whitelist");
        new WhitelistTypeAdapter().write(jsonWriter, playerData.getWhitelist());
        jsonWriter.name("newPlayer").value(playerData.isNewPlayer());
        jsonWriter.name("lastSeen").value(playerData.getLastSeen());
        jsonWriter.name("lastClaim").value(playerData.getLastClaim());
        jsonWriter.name("serverID").value(playerData.getServerID().toString());
        jsonWriter.endObject();
    }

    private void writeRefund(JsonWriter jsonWriter, List<OnlineRefund> refunds) throws IOException {
        jsonWriter.name("refunds");
        jsonWriter.beginArray();
        for (OnlineRefund refund : refunds) {
            jsonWriter.beginObject();
            jsonWriter.name(refund.getClass().getName());
            PlayerData.writeRefund(jsonWriter, refund);
            jsonWriter.endObject();
        }
        jsonWriter.endArray();
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
                case "uuid" -> playerData.setUuid(UUID.fromString(jsonReader.nextString()));
                case "immunity" -> readImmunity(jsonReader, playerData);
                case "timeZone" -> playerData.setTimeZone(TimeZone.getTimeZone(jsonReader.nextString()));
                case "refundAmount" -> playerData.addRefund(new AmountRefund(jsonReader.nextDouble(), null)); // old data since 1.22.0
                case "refundItems" -> playerData.addRefund(new ItemRefund(jsonReader.nextString(), null)); // old data since 1.22.0
                case "refunds" -> readRefund(jsonReader, playerData);
                case "disableBroadcast" -> readBroadcast(jsonReader, playerData);
                case "rewardHeads" -> readRewardHeads(jsonReader, playerData); // old data since 1.22.0
                case "bountyCooldown" -> playerData.setBountyCooldown(jsonReader.nextLong());
                case "whitelist" -> playerData.setWhitelist(new WhitelistTypeAdapter().read(jsonReader));
                case "newPlayer" -> playerData.setNewPlayer(jsonReader.nextBoolean());
                case "lastSeen" -> playerData.setLastSeen(jsonReader.nextLong());
                case "lastClaim" -> playerData.setLastClaim(jsonReader.nextLong());
                case "serverID" -> playerData.setServerID(UUID.fromString(jsonReader.nextString()));
                default -> // unexpected name
                        jsonReader.skipValue();
            }
        }

        jsonReader.endObject();
        return playerData;
    }

    private void readRefund(JsonReader jsonReader, PlayerData playerData) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            String type = jsonReader.nextName();
            try {
                Class<?> clazz = Class.forName(type);
                if (OnlineRefund.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends OnlineRefund> refundClass = (Class<? extends OnlineRefund>) clazz;
                    playerData.addRefund(PlayerData.readRefund(jsonReader, refundClass));

                } else {
                    Bukkit.getLogger().warning("Found an invalid refund class: " + type + " for player " + playerData.getPlayerName());
                    jsonReader.skipValue();
                }

            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().warning("Could not find refund class: " + type + " for player " + playerData.getPlayerName());
                jsonReader.skipValue();
            }
            jsonReader.endObject();
        }
        jsonReader.endArray();
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

    @Deprecated(since = "1.22.0")
    private void readRewardHeads(JsonReader jsonReader, PlayerData playerData) throws IOException {
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
            playerData.addRefund(new RewardHead(uuid, killer, amount, null));
            jsonReader.endObject();
        }
        jsonReader.endArray();
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

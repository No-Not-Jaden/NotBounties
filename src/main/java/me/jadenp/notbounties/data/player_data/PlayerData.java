package me.jadenp.notbounties.data.player_data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.Inconsistent;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class PlayerData extends Inconsistent implements Comparable<PlayerData> {

    private static final Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(PlayerData.class, new PlayerDataAdapter());
        builder.registerTypeAdapter(ItemRefund.class, new ItemRefundTypeAdapter());
        builder.registerTypeAdapter(AmountRefund.class, new AmountRefundTypeAdapter());
        builder.registerTypeAdapter(RewardHead.class, new RewardHeadTypeAdapter());
        gson = builder.create();
    }

    public static <T extends OnlineRefund> T readRefund(JsonReader reader, Class<T> clazz) throws IOException {
        if (reader == null || clazz == null) {
            throw new IllegalArgumentException("Reader and clazz must not be null");
        }

        TypeToken<T> typeToken = TypeToken.get(clazz);
        TypeAdapter<T> adapter = gson.getAdapter(typeToken);

        if (adapter == null) {
            throw new IllegalStateException("No TypeAdapter found for " + clazz);
        }

        return adapter.read(reader);
    }

    public static void writeRefund(JsonWriter writer, OnlineRefund onlineRefund) throws IOException {
        if (writer == null || onlineRefund == null) {
            throw new IllegalArgumentException("Writer and refund must not be null");
        }

        TypeToken<?> typeToken = TypeToken.get(onlineRefund.getClass());
        TypeAdapter<?> adapter = gson.getAdapter(typeToken);

        if (adapter == null) {
            throw new IllegalStateException("No TypeAdapter found for " + onlineRefund.getClass());
        }

        //noinspection unchecked
        ((TypeAdapter<OnlineRefund>) adapter).write(writer, onlineRefund);
    }

    public static PlayerData fromJson(String jsonString) {
        try {
            return fromJson(new JsonReader(new StringReader(jsonString)));
        } catch (IOException e) {
            return gson.fromJson(jsonString, PlayerData.class);
        }
    }

    public static PlayerData fromJson(JsonReader reader) throws IOException {
        return new PlayerDataAdapter().read(reader);
    }

    @Override
    public int compareTo(@NotNull PlayerData o) {
        return this.uuid.compareTo(o.uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PlayerData that = (PlayerData) o;
        return generalImmunity == that.generalImmunity && murderImmunity == that.murderImmunity && randomImmunity == that.randomImmunity && timedImmunity == that.timedImmunity && bountyCooldown == that.bountyCooldown && newPlayer == that.newPlayer && lastSeen == that.lastSeen && lastClaim == that.lastClaim && Objects.equals(uuid, that.uuid) && Objects.equals(playerName, that.playerName) && Objects.equals(timeZone, that.timeZone) && broadcastSettings == that.broadcastSettings && Objects.equals(refund, that.refund) && Objects.equals(whitelist, that.whitelist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, playerName, generalImmunity, murderImmunity, randomImmunity, timedImmunity, timeZone, broadcastSettings, refund, bountyCooldown, whitelist, newPlayer, lastSeen, lastClaim);
    }

    public enum BroadcastSettings {
        EXTENDED, SHORT, DISABLE
    }

    private UUID uuid;
    private String playerName = null;
    private boolean generalImmunity = false;
    private boolean murderImmunity = false;
    private boolean randomImmunity = false;
    private boolean timedImmunity = false;
    private TimeZone timeZone = null;
    private BroadcastSettings broadcastSettings;
    private final List<OnlineRefund> refund = new LinkedList<>();
    private long bountyCooldown = 0;
    private Whitelist whitelist;
    private boolean newPlayer = true;
    private long lastSeen = 0;
    private long lastClaim = 0;
    private UUID serverID = null;

    public PlayerData() {
        broadcastSettings = ConfigOptions.getMoney().getDefaultBroadcastSetting();
        whitelist = new Whitelist(new TreeSet<>(), Whitelist.isDefaultWhitelist());
    }

    public JsonObject toJson() {
        return (JsonObject) gson.toJsonTree(this);
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
        if (playerName != null && uuid != null) {
            LoggedPlayers.logPlayer(playerName, uuid);
        }
    }

    public void setServerID(UUID serverID) {
        this.serverID = serverID;
    }

    public UUID getServerID() {
        return serverID;
    }

    public void setLastClaim(long lastClaim) {
        this.lastClaim = lastClaim;
    }

    public long getLastClaim() {
        return lastClaim;
    }

    public void setNewPlayer(boolean newPlayer) {
        this.newPlayer = newPlayer;
    }

    public boolean isNewPlayer() {
        return newPlayer;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
        if (playerName != null && uuid != null && !LoggedPlayers.isLogged(playerName)) {
            LoggedPlayers.logPlayer(playerName, uuid);
        }
    }

    public @Nullable String getPlayerName() {
        return playerName;
    }

    public void setGeneralImmunity(boolean generalImmunity) {
        this.generalImmunity = generalImmunity;
    }

    public void setMurderImmunity(boolean murderImmunity) {
        this.murderImmunity = murderImmunity;
    }

    public void setRandomImmunity(boolean randomImmunity) {
        this.randomImmunity = randomImmunity;
    }

    public void setTimedImmunity(boolean timedImmunity) {
        this.timedImmunity = timedImmunity;
    }

    public boolean hasGeneralImmunity() {
        return generalImmunity;
    }

    public boolean hasMurderImmunity() {
        return murderImmunity;
    }

    public boolean hasRandomImmunity() {
        return randomImmunity;
    }

    public boolean hasTimedImmunity() {
        return timedImmunity;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public List<OnlineRefund> getRefund() {
        return refund;
    }

    public void addRefund(OnlineRefund onlineRefund) {
        refund.add(onlineRefund);
    }

    public void clearRefund() {
        refund.clear();
    }

    public void setRefund(List<OnlineRefund> onlineRefund) {
        refund.clear();
        refund.addAll(onlineRefund);
    }

    public boolean hasRefund() {
        return !refund.isEmpty();
    }

    public void setBroadcastSettings(@Nullable BroadcastSettings broadcastSettings) {
        if (broadcastSettings == null)
            this.broadcastSettings = ConfigOptions.getMoney().getDefaultBroadcastSetting();
        else
            this.broadcastSettings = broadcastSettings;
    }

    public @NotNull BroadcastSettings getBroadcastSettings() {
        if (broadcastSettings == null)
            broadcastSettings = ConfigOptions.getMoney().getDefaultBroadcastSetting();
        if (broadcastSettings == null)
            return BroadcastSettings.EXTENDED;
        return broadcastSettings;
    }

    public long getBountyCooldown() {
        return bountyCooldown;
    }

    public void setBountyCooldown(long bountyCooldown) {
        this.bountyCooldown = bountyCooldown;
    }

    public void setWhitelist(Whitelist whitelist) {
        this.whitelist = whitelist;
    }

    public Whitelist getWhitelist() {
        return whitelist;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getID() {
        return uuid.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Inconsistent> T copy() {
        PlayerData playerData = new PlayerData();
        playerData.setUuid(uuid);
        playerData.setPlayerName(playerName);
        playerData.setGeneralImmunity(generalImmunity);
        playerData.setTimedImmunity(timedImmunity);
        playerData.setRandomImmunity(randomImmunity);
        playerData.setMurderImmunity(murderImmunity);
        playerData.setRefund(playerData.getRefund());
        playerData.setNewPlayer(newPlayer);
        playerData.setBountyCooldown(bountyCooldown);
        playerData.setTimeZone(timeZone);
        playerData.setBroadcastSettings(broadcastSettings);
        playerData.setWhitelist(whitelist);
        playerData.setLastSeen(lastSeen);
        playerData.setLastClaim(lastClaim);
        playerData.setServerID(serverID);
        return (T) playerData;
    }

    @Override
    public long getLatestUpdate() {
        return getLastSeen();
    }

    @Override
    public List<Inconsistent> getSubElements() {
        return new ArrayList<>(refund);
    }

    @Override
    public void setSubElements(List<Inconsistent> subElements) {
        // convert to onlineRefunds
        refund.clear();
        refund.addAll(subElements.stream().filter(OnlineRefund.class::isInstance).map(OnlineRefund.class::cast).toList());
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", playerName='" + playerName + '\'' +
                ", generalImmunity=" + generalImmunity +
                ", murderImmunity=" + murderImmunity +
                ", randomImmunity=" + randomImmunity +
                ", timedImmunity=" + timedImmunity +
                ", timeZone=" + timeZone +
                ", broadcastSettings=" + broadcastSettings +
                ", refund=" + refund +
                ", bountyCooldown=" + bountyCooldown +
                ", whitelist=" + whitelist +
                ", newPlayer=" + newPlayer +
                ", lastSeen=" + lastSeen +
                ", lastClaim=" + lastClaim +
                ", serverID=" + serverID +
                '}';
    }
}

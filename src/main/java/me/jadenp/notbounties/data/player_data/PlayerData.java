package me.jadenp.notbounties.data.player_data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerData implements Comparable<PlayerData> {

    private static final Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(PlayerData.class, new PlayerDataAdapter());
        builder.registerTypeAdapter(ItemRefund.class, new ItemRefundTypeAdapter());
        builder.registerTypeAdapter(AmountRefund.class, new AmountRefundTypeAdapter());
        gson = builder.create();
    }

    public static <T extends OnlineRefund<?>> T readRefund(JsonReader reader, Class<T> clazz) throws IOException {
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

    public static void writeRefund(JsonWriter writer, OnlineRefund<?> onlineRefund) throws IOException {
        if (writer == null || onlineRefund == null) {
            throw new IllegalArgumentException("Writer and refund must not be null");
        }

        TypeToken<?> typeToken = TypeToken.get(onlineRefund.getClass());
        TypeAdapter<?> adapter = gson.getAdapter(typeToken);

        if (adapter == null) {
            throw new IllegalStateException("No TypeAdapter found for " + onlineRefund.getClass());
        }

        //noinspection unchecked
        ((TypeAdapter<OnlineRefund<?>>) adapter).write(writer, onlineRefund);
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
        return generalImmunity == that.generalImmunity && murderImmunity == that.murderImmunity && randomImmunity == that.randomImmunity && timedImmunity == that.timedImmunity && bountyCooldown == that.bountyCooldown && lastSeen == that.lastSeen && lastClaim == that.lastClaim && Objects.equals(uuid, that.uuid) && Objects.equals(playerName, that.playerName) && Objects.equals(timeZone, that.timeZone) && broadcastSettings == that.broadcastSettings && Objects.equals(whitelist, that.whitelist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, playerName, generalImmunity, murderImmunity, randomImmunity, timedImmunity, timeZone, broadcastSettings, bountyCooldown, whitelist, lastSeen, lastClaim);
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
    private long bountyCooldown = 0; // Time in ms when they set a bounty last
    private Whitelist whitelist;
    private long lastSeen = 0;
    private long lastClaim = 0;
    private UUID serverID = null; // ID used for which server the data is on
    private UUID onlineServerID = DataManager.GLOBAL_SERVER_ID; // ID used for which server the player is on
    private boolean trackingExempt = false;
    private long playTime = 0;
    private PlayerSkin skin = SkinManager.getMissingSkin();
    private ImpersistentPlayerData impersistentPlayerData;

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

    public @NotNull ImpersistentPlayerData getImpersistentPlayerData() throws IllegalStateException {
        if (impersistentPlayerData == null) {
            if (uuid == null)
                throw new IllegalStateException("UUID must be set before setting impersistent player data");
            impersistentPlayerData = ImpersistentPlayerData.get(uuid);
        }
        return impersistentPlayerData;
    }

    public long getPlayTime() {
        return playTime;
    }

    public void setPlayTime(long playTime) {
        this.playTime = playTime;
    }

    public PlayerSkin getSkin() {
        return skin;
    }

    public void setSkin(PlayerSkin skin) {
        this.skin = skin;
    }

    public void setTrackingExempt(boolean trackingExempt) {
        this.trackingExempt = trackingExempt;
    }

    public boolean isTrackingExempt() {
        return trackingExempt;
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

    public boolean isNewPlayer() {
        return ImmunityManager.getNewPlayerImmunity() * 1000L > playTime;
    }

    public void setPlayerName(@NotNull String playerName) {
        try {
            Objects.requireNonNull(playerName, "Player name cannot be null");
        } catch (NullPointerException e) {
            Logger logger = NotBounties.getInstance().getLogger();
            NotBounties.getInstance().getLogger().log(Level.WARNING, "Player name cannot be null", e);
            Arrays.asList(e.getStackTrace()).forEach(m -> logger.warning(m.toString()));
            return;
        }
        this.playerName = playerName;
        getImpersistentPlayerData().setPlayerName(playerName);
        if (uuid != null && !LoggedPlayers.isLogged(playerName)) {
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

    public UUID getOnlineServerID() {
        return onlineServerID;
    }

    public void setOnlineServerID(UUID onlineServerID) {
        this.onlineServerID = onlineServerID;
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
                ", bountyCooldown=" + bountyCooldown +
                ", whitelist=" + whitelist +
                ", lastSeen=" + lastSeen +
                ", lastClaim=" + lastClaim +
                ", serverID=" + serverID +
                '}';
    }
}

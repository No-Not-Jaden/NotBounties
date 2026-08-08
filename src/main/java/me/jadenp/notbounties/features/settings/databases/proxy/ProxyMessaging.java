package me.jadenp.notbounties.features.settings.databases.proxy;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.utils.tasks.SkinRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.util.concurrent.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.sql.SQLException;

public class ProxyMessaging implements PluginMessageListener {
    // DTO for grouped transaction operations
    public record DBOp(String sql, List<DBParam> params) {
        public DBOp(String sql, List<DBParam> params) {
            this.sql = sql;
            this.params = params == null ? Collections.emptyList() : params;
        }
    }

    // Database messaging support
    public record DBParam(int index, ProxyMessaging.DBParam.Kind kind, Object value) {
        public enum Kind {NULL, BYTES, INT, LONG, DOUBLE, STRING, BLOB}
    }
    public static class DBResponse {
        public List<Map<String, Object>> rows; // for queries
        public List<String> columns;
        public int updateCount = 0; // for updates
        public int[] batchCounts; // for batches
        public String error; // if non-null indicates failure
    }
    private static final Map<Long, CompletableFuture<DBResponse>> PENDING_DB = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong DB_ID = new java.util.concurrent.atomic.AtomicLong(1);

    // Wire type tags for cell encoding
    private static final byte TYPE_NULL = 0;
    private static final byte TYPE_BYTES = 1;
    private static final byte TYPE_INT = 2;
    private static final byte TYPE_LONG = 3;
    private static final byte TYPE_DOUBLE = 4;
    private static final byte TYPE_STRING = 5;
    private static final byte TYPE_BLOB = 6;
    private static boolean connectedBefore = false;
    protected static final String CHANNEL = "notbounties:main";
    private static final String DATABASE_SUBCHANNEL = "Database";

    private static final List<PreparedUpdateMessage> preparedUpdateMessage = Collections.synchronizedList(new LinkedList<>());
    private static long idCounter = 0;
    private static final int DUPLICATE_MESSAGE_THRESHOLD = 300;
    private static final Map<Integer, Long> hashedMessages = Collections.synchronizedMap(new HashMap<>());


    private static void setConnectedBefore() {
        ProxyMessaging.connectedBefore = true;
    }

    public static void cleanCache() {
        long threshold = System.currentTimeMillis() - DUPLICATE_MESSAGE_THRESHOLD;
        hashedMessages.entrySet().removeIf(entry -> entry.getValue() < threshold);
    }

    /**
     * Check if the proxy has been connected since server start
     * @return True if the proxy has connected to the plugin since the server has started
     */
    public static boolean hasConnectedBefore() {
        return connectedBefore;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] bytes) {
        if (!channel.equals(CHANNEL) || !ProxySettings.isEnabled())
            return;
        setConnectedBefore();
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        try {
            receiveMessage(in);
        } catch (IOException e) {
            player.kickPlayer(e.getMessage());
        }
    }

    /**
     * Controls the outcome of messages received from the proxy.
     * @param in The message bytes received.
     * @throws IOException If an error occurs while reading the message.
     */
    private void receiveMessage(ByteArrayDataInput in) throws IOException {
        String subChannel = in.readUTF();
        short len = in.readShort();
        byte[] msgBytes = new byte[len];
        in.readFully(msgBytes);
        int hash = Arrays.hashCode(msgBytes);
        if (System.currentTimeMillis() - hashedMessages.getOrDefault(hash, 0L) < DUPLICATE_MESSAGE_THRESHOLD) {
            // duplicate message
            NotBounties.debugMessage("Duplicate message received from proxy: " + subChannel + " hash: " + Arrays.hashCode(msgBytes), false);
            return;
        }
        hashedMessages.put(hash, System.currentTimeMillis());

        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgBytes));

        NotBounties.debugMessage("Received a message from proxy: " + subChannel + " hash: " + Arrays.hashCode(msgBytes), false);

        switch (subChannel) {
            case "ReceiveConnection" -> receiveConnection(msgIn);
            case "PlayerList" -> receivePlayerList(msgIn);
            case DATABASE_SUBCHANNEL -> receiveDatabase(msgIn);
            case "Forward" -> {
                String subSubChannel = msgIn.readUTF();
                NotBounties.debugMessage("Sub Channel: " + subSubChannel, false);
                if (subSubChannel.equals("LogPlayer")) {
                    short numPlayers = msgIn.readShort();
                    for (short i = 0; i < numPlayers; i++) {
                        Map.Entry<UUID, String> entry = parsePlayerString(msgIn.readUTF());
                        DataManager.getPlayerData(entry.getKey()).setPlayerName(entry.getValue());
                    }
                } else {
                    throw new IllegalStateException("Unknown message!");
                }
            }
            case "PlayerSkin" -> {
                try {
                    receivePlayerSkin(msgIn);
                } catch (URISyntaxException e) {
                    Bukkit.getLogger().warning("[NotBounties] Invalid skin URI");
                    Bukkit.getLogger().warning(e.toString());
                }
            }
            default -> throw new IllegalStateException("Unknown message!");
        }
        msgIn.close();
    }

    /**
     * Parses a player uuid and name from the text in the form UUID:name
     * @param text Text to parse.
     * @return A map entry with the UUID and player name
     * @throws IllegalArgumentException if the text isn't in the correct format.
     */
    private static Map.Entry<UUID, String> parsePlayerString(String text) throws IllegalArgumentException{
        String[] player = text.split(":");
        UUID uuid = UUID.fromString(player[0]);
        String playerName = player[1];
        return new Map.Entry<>() {
            @Override
            public UUID getKey() {
                return uuid;
            }

            @Override
            public String getValue() {
                return playerName;
            }

            @Override
            public String setValue(String value) {
                return "";
            }
        };
    }

    private synchronized void receiveConnection(DataInputStream msgIn) throws IOException {
        if (!ProxySettings.isDatabaseSynchronization()) {
            return;
        }
        msgIn.readAllBytes();
        if (!preparedUpdateMessage.isEmpty() && !preparedUpdateMessage.getFirst().isSending()) {
            // restart messages
            preparedUpdateMessage.getFirst().sendMessage(!Bukkit.isPrimaryThread());
        }

    }

    private void receivePlayerSkin(DataInputStream msgIn) throws IOException, URISyntaxException {
        short numSkins = msgIn.readShort();
        for (int i = 0; i < numSkins; i++) {
            UUID uuid = UUID.fromString(msgIn.readUTF());
            msgIn.readUTF(); // id
            String url = msgIn.readUTF();
            SkinManager.saveSkin(uuid, new PlayerSkin(SkinManager.getTextureId(url), false));
        }
    }

    /**
     * Received a player list from the proxy.
     * @param msgIn The received message.
     * @throws IOException If there was an error reading the message.
     */
    private void receivePlayerList(DataInputStream msgIn) throws IOException {
        Map<UUID, String> players = new HashMap<>();
        short numPlayers = msgIn.readShort();
        for (short i = 0; i < numPlayers; i++) {
            Map.Entry<UUID, String> player = parsePlayerString(msgIn.readUTF());
            players.put(player.getKey(), player.getValue());
        }
        ProxySettings.setDatabaseOnlinePlayers(players);
    }

    /**
     * Sends a message to the backend server
     * @param identifier message identifier
     * @param data data to be sent
     */
    public static void sendMessage(String identifier, byte[] data) {
        if (ProxySettings.isEnabled() && NotBounties.getInstance().isEnabled()) {
            if (Bukkit.isPrimaryThread()) {
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    sendMessage(identifier, data, Bukkit.getOnlinePlayers().iterator().next());
                }
            } else {
                NotBounties.getServerImplementation().global().run(() -> {
                    if (!Bukkit.getOnlinePlayers().isEmpty()) {
                        sendMessage(identifier, data, Bukkit.getOnlinePlayers().iterator().next());
                    }
                });
            }
        }
    }

    /**
     * Sends a message to the backend server
     *
     * @param identifier message identifier
     * @param data       data to be sent
     * @param player     player to send the message through
     */
    public static void sendMessage(String identifier, byte[] data, Player player) {
        player.sendPluginMessage(NotBounties.getInstance(), identifier, data);
    }

    /**
     * Wraps a message in the needed bytes to send the message globally
     * @param stream A ByteArrayOutputStream to be sent as the message
     * @return A byte[] ready to be sent as a message
     */
    protected static byte[] wrapGlobalMessage(byte[] stream) {
        if (stream.length == 0)
            return new byte[0];
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        //out.writeUTF("ALL"); // This is the target server. "ALL" will message all servers apart from the one sending the message
        //out.writeUTF(channel); // This is the channel.

        out.writeShort(stream.length); // This is the length.
        out.write(stream); // This is the message.

        return out.toByteArray();
    }


    /**
     * Request the player list from the proxy.
     */
    public static void requestPlayerList(){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");

        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgBytes);
        try {
            msgout.writeUTF("ALL");
        } catch (IOException e) {
            NotBounties.debugMessage("Error preparing a player list request", true);
            NotBounties.debugMessage(e.toString(), true);
        }
        out.writeShort(msgBytes.toByteArray().length); // This is the length.
        out.write(msgBytes.toByteArray()); // This is the message.
        sendMessage(CHANNEL, out.toByteArray());
        NotBounties.debugMessage("Sent player list request.", false);
    }

    /**
     * Sends a player to the network to be logged.
     *
     * @param playerName The name of the player.
     * @param uuid       The UUID of the player.
     */
    public static void logNewPlayer(String playerName, UUID uuid) {
        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgBytes);
        try {
            msgout.writeUTF("LogPlayer");
            msgout.writeShort(1);
            msgout.writeUTF(uuid.toString() + ":" + playerName);
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
            return;
        }
        sendMessage(CHANNEL, wrapGlobalMessage(msgBytes.toByteArray()));
    }

    private static final List<UUID> queuedSkinRequests = new ArrayList<>();
    /**
     * Requests skin information for a player.
     * @param uuid UUID of the player
     */
    public static void requestPlayerSkin(UUID uuid) {
        if (ProxySettings.areSkinRequestsEnabled()) {
            NotBounties.getServerImplementation().global().run(() -> {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    SkinManager.failRequest(uuid);
                    queuedSkinRequests.add(uuid);
                } else {
                    sendSkinRequest(uuid);
                    UUID[] skinsToSend = queuedSkinRequests.toArray(new UUID[0]);
                    queuedSkinRequests.clear();
                    SkinRequest skinRequest = new SkinRequest(skinsToSend);
                    skinRequest.setTaskImplementation(NotBounties.getServerImplementation().global().runAtFixedRate(skinRequest,5, 5));
                }
            });
        }
    }

    public static void sendSkinRequest(UUID uuid) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerSkin");

        String playerName = LoggedPlayers.getPlayerName(uuid);
        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgBytes);
        try {
            msgout.writeUTF(playerName);
            msgout.writeUTF(uuid.toString());
        } catch (IOException e) {
            NotBounties.debugMessage(e.toString(), true);
            return;
        }
        out.writeShort(msgBytes.toByteArray().length); // This is the length.
        out.write(msgBytes.toByteArray()); // This is the message.
        sendMessage(CHANNEL, out.toByteArray());
        NotBounties.debugMessage("Sent player skin request.", false);
    }

    public static void onCompleteProxyMessage(long id) {
        synchronized (preparedUpdateMessage) {
            for (int i = 0; i < preparedUpdateMessage.size(); i++) {
                if (preparedUpdateMessage.get(i).getId() == id) {
                    preparedUpdateMessage.remove(i);
                    if (!preparedUpdateMessage.isEmpty() && !preparedUpdateMessage.getFirst().isSending())
                        preparedUpdateMessage.getFirst().sendMessage(!Bukkit.isPrimaryThread());
                    return;
                }
            }
        }
        NotBounties.debugMessage("Complete proxy message not found. id: " + id, true);
    }

    public static void onFailProxyMessage(long id) {
        synchronized (preparedUpdateMessage) {
            for (int i = 0; i < preparedUpdateMessage.size(); i++) {
                if (preparedUpdateMessage.get(i).getId() == id) {
                    preparedUpdateMessage.set(i, preparedUpdateMessage.get(i).getFirstUnsent());
                    return;
                }
            }
        }
        NotBounties.debugMessage("Failed proxy message not found. id: " + id, true);
    }

    public static List<byte[]> getUnsentMessages() {
        List<byte[]> messages = new LinkedList<>();
        if (!preparedUpdateMessage.isEmpty()) {
            for (PreparedUpdateMessage updateMessage : preparedUpdateMessage) {
                updateMessage.setCanceled(true);
                messages.addAll(updateMessage.getUnsentMessages());
            }
        }
        return messages;
    }

    public static void addPreparedUpdateMessage(PreparedUpdateMessage updateMessage) {
        synchronized (preparedUpdateMessage) {
            if (preparedUpdateMessage.isEmpty() && isAnyPlayerOnline()) {
                updateMessage.sendMessage(!Bukkit.isPrimaryThread());
            }
            preparedUpdateMessage.add(updateMessage);
        }
    }

    private static boolean isAnyPlayerOnline() {
        return !DataManager.getLocalOnlinePlayers().isEmpty();
    }


    // ===== Database proxy driver messaging =====
    private static void receiveDatabase(DataInputStream in) throws IOException {
        String op = in.readUTF();
        long requestId = in.readLong();
        DBResponse resp = new DBResponse();
        switch (op) {
            case "Result" -> {
                int colCount = in.readUnsignedShort();
                List<String> cols = new ArrayList<>(colCount);
                for (int i = 0; i < colCount; i++) cols.add(in.readUTF());
                int rowCount = in.readInt();
                List<Map<String, Object>> rows = new ArrayList<>(Math.max(0, rowCount));
                for (int r = 0; r < rowCount; r++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int c = 0; c < colCount; c++) {
                        byte t = in.readByte();
                        Object v;
                        switch (t) {
                            case TYPE_NULL -> v = null;
                            case TYPE_BYTES, TYPE_BLOB -> {
                                int len = in.readInt();
                                byte[] b = new byte[len];
                                in.readFully(b);
                                v = b;
                            }
                            case TYPE_INT -> v = in.readInt();
                            case TYPE_LONG -> v = in.readLong();
                            case TYPE_DOUBLE -> v = in.readDouble();
                            case TYPE_STRING -> v = in.readUTF();
                            default -> throw new IOException("Unknown cell type: " + t);
                        }
                        row.put(cols.get(c), v);
                    }
                    rows.add(row);
                }
                resp.columns = cols;
                resp.rows = rows;
            }
            case "Update" -> resp.updateCount = in.readInt();
            case "Batch" -> {
                int n = in.readInt();
                int[] counts = new int[n];
                for (int i = 0; i < n; i++) counts[i] = in.readInt();
                resp.batchCounts = counts;
            }
            case "Error" -> resp.error = in.readUTF();
            default -> {}
        }
        CompletableFuture<DBResponse> fut = PENDING_DB.remove(requestId);
        if (fut != null) fut.complete(resp);
    }

    private static void writeParam(DataOutputStream out, DBParam p) throws IOException {
        out.writeShort(p.index);
        switch (p.kind) {
            case NULL -> { out.writeByte(TYPE_NULL); out.writeInt(p.value instanceof Integer i ? i : java.sql.Types.NULL); }
            case BYTES -> { out.writeByte(TYPE_BYTES); byte[] b = (byte[]) p.value; out.writeInt(b.length); out.write(b); }
            case INT -> { out.writeByte(TYPE_INT); out.writeInt((Integer) p.value); }
            case LONG -> { out.writeByte(TYPE_LONG); out.writeLong((Long) p.value); }
            case DOUBLE -> { out.writeByte(TYPE_DOUBLE); out.writeDouble((Double) p.value); }
            case STRING -> { out.writeByte(TYPE_STRING); out.writeUTF((String) p.value); }
            case BLOB -> { out.writeByte(TYPE_BLOB); byte[] b = (byte[]) p.value; out.writeInt(b.length); out.write(b); }
        }
    }

    public static DBResponse requestExecuteQuery(String sql, List<DBParam> params, int timeoutMs) throws SQLException {
        long id = DB_ID.getAndIncrement();
        CompletableFuture<DBResponse> fut = new CompletableFuture<>();
        PENDING_DB.put(id, fut);
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgBytes);
            msgout.writeUTF("ExecuteQuery");
            msgout.writeLong(id);
            msgout.writeUTF(sql);
            msgout.writeShort(params == null ? 0 : params.size());
            if (params != null) {
                for (DBParam p : params) writeParam(msgout, p);
            }
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(DATABASE_SUBCHANNEL);
            out.writeShort(msgBytes.size());
            out.write(msgBytes.toByteArray());
            sendMessage(CHANNEL, out.toByteArray());
            DBResponse resp = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (resp.error != null) throw new SQLException(resp.error);
            return resp;
        } catch (Exception e) {
            PENDING_DB.remove(id);
            throw new SQLException("Database request failed", e);
        }
    }

    public static DBResponse requestExecuteUpdate(String sql, List<DBParam> params, int timeoutMs) throws SQLException {
        long id = DB_ID.getAndIncrement();
        CompletableFuture<DBResponse> fut = new CompletableFuture<>();
        PENDING_DB.put(id, fut);
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgBytes);
            msgout.writeUTF("ExecuteUpdate");
            msgout.writeLong(id);
            msgout.writeUTF(sql);
            msgout.writeShort(params == null ? 0 : params.size());
            if (params != null) {
                for (DBParam p : params) writeParam(msgout, p);
            }
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(DATABASE_SUBCHANNEL);
            out.writeShort(msgBytes.size());
            out.write(msgBytes.toByteArray());
            sendMessage(CHANNEL, out.toByteArray());
            DBResponse resp = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (resp.error != null) throw new SQLException(resp.error);
            return resp;
        } catch (Exception e) {
            PENDING_DB.remove(id);
            throw new SQLException("Database request failed", e);
        }
    }

    public static DBResponse requestExecuteBatch(String sql, List<List<DBParam>> batches, int timeoutMs) throws SQLException {
        long id = DB_ID.getAndIncrement();
        CompletableFuture<DBResponse> fut = new CompletableFuture<>();
        PENDING_DB.put(id, fut);
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgBytes);
            msgout.writeUTF("ExecuteBatch");
            msgout.writeLong(id);
            msgout.writeUTF(sql);
            msgout.writeInt(batches == null ? 0 : batches.size());
            if (batches != null) {
                for (List<DBParam> ps : batches) {
                    msgout.writeShort(ps == null ? 0 : ps.size());
                    if (ps != null) for (DBParam p : ps) writeParam(msgout, p);
                }
            }
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(DATABASE_SUBCHANNEL);
            out.writeShort(msgBytes.size());
            out.write(msgBytes.toByteArray());
            sendMessage(CHANNEL, out.toByteArray());
            DBResponse resp = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (resp.error != null) throw new SQLException(resp.error);
            return resp;
        } catch (Exception e) {
            PENDING_DB.remove(id);
            throw new SQLException("Database request failed", e);
        }
    }

    public static DBResponse requestExecuteTransaction(List<DBOp> ops, int timeoutMs) throws SQLException {
        long id = DB_ID.getAndIncrement();
        CompletableFuture<DBResponse> fut = new CompletableFuture<>();
        PENDING_DB.put(id, fut);
        try {
            // Build the ExecuteTransaction payload (Database subchannel inner message)
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgBytes);
            msgout.writeUTF("ExecuteTransaction");
            msgout.writeLong(id);
            int count = ops == null ? 0 : ops.size();
            msgout.writeInt(count);
            if (ops != null) {
                for (DBOp op : ops) {
                    msgout.writeUTF(op.sql);
                    List<DBParam> ps = op.params;
                    msgout.writeShort(ps == null ? 0 : ps.size());
                    if (ps != null) for (DBParam p : ps) writeParam(msgout, p);
                }
            }
            msgout.flush();

            // Send via PreparedUpdateMessage with sequencing so the proxy can reassemble large transactions
            PreparedUpdateMessage prepared = PreparedUpdateMessage.createSequenced(DATABASE_SUBCHANNEL, msgBytes.toByteArray(), idCounter++);
            addPreparedUpdateMessage(prepared);

            DBResponse resp = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (resp.error != null) throw new SQLException(resp.error);
            return resp;
        } catch (Exception e) {
            PENDING_DB.remove(id);
            throw new SQLException("Database request failed", e);
        }
    }

}
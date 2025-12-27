package me.jadenp.notbounties.features.settings.databases.proxy;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.settings.databases.LocalData;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.BountyChange;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.utils.tasks.SkinRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ProxyMessaging implements PluginMessageListener, Listener {
    private static boolean connectedBefore = false;
    protected static final String CHANNEL = "notbounties:main";

    private static List<Bounty> bountyCache;
    private static Map<UUID, PlayerStat> statCache;
    private static List<PlayerData> playerDataCache;
    private static final List<PreparedUpdateMessage> preparedUpdateMessage = Collections.synchronizedList(new LinkedList<>());
    private static long idCounter = 0;

    /**
     * Whether this server has up-to-date data. This server will not have up-to-date data if no players are online
     * or a few seconds after the first player joins.
     */
    private static boolean dataSynced = false;

    public static void setDataSynced(boolean dataSynced) {
        ProxyMessaging.dataSynced = dataSynced;
    }

    public static boolean isDataSynced() {
        return dataSynced;
    }

    private static void setConnectedBefore() {
        ProxyMessaging.connectedBefore = true;
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
        if (!channel.equals(CHANNEL) || !ProxyDatabase.isEnabled())
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
        NotBounties.debugMessage("Received a message from proxy: " + subChannel, false);
        short len = in.readShort();
        byte[] msgBytes = new byte[len];
        in.readFully(msgBytes);
        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgBytes));

        switch (subChannel) {
            case "ReceiveConnection" -> receiveConnection(msgIn);
            case "PlayerList" -> receivePlayerList(msgIn);
            case "Forward" -> {
                String subSubChannel = msgIn.readUTF();
                switch (subSubChannel) {
                    case "BountyUpdate" -> receiveBountyUpdate(msgIn);
                    case "StatUpdate" -> receiveStatUpdate(msgIn);
                    case "PlayerDataUpdate" -> receivePlayerDataUpdate(msgIn);
                    case "LogPlayer" -> {
                        short numPlayers = msgIn.readShort();
                        for (short i = 0; i < numPlayers; i++) {
                            Map.Entry<UUID, String> entry = parsePlayerString(msgIn.readUTF());
                            DataManager.getPlayerData(entry.getKey()).setPlayerName(entry.getValue());
                        }
                    }
                    default -> throw new IllegalStateException("Unknown message!");
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
        if (!ProxyDatabase.isDatabaseSynchronization()) {
            msgIn.readFully(new byte[msgIn.available()]);
        }
        short savedBounties = msgIn.readShort();
        List<Bounty> bounties = new LinkedList<>();
        for (short i = 0; i < savedBounties; i++) {
            bounties.add(new Bounty(msgIn.readUTF()));
        }
        short savedStats = msgIn.readShort();
        Map<UUID, PlayerStat> playerStatMap = new HashMap<>();
        for (short i = 0; i < savedStats; i++) {
            UUID uuid = null;
            try {
                uuid = UUID.fromString(msgIn.readUTF());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotBounties] Invalid uuid in received proxy message.");
            }
            if (uuid != null)
                playerStatMap.put(uuid, new PlayerStat(msgIn.readUTF()));
        }
        List<PlayerData> playerData = new LinkedList<>();
        try {
            short savedPlayerData = msgIn.readShort();
            for (short i = 0; i < savedPlayerData; i++) {
                playerData.add(PlayerData.fromJson(msgIn.readUTF()));
            }
        } catch (EOFException e) {
            // proxy isn't updated
            Bukkit.getLogger().severe("[NotBounties] Player data has not been transmitted from the proxy. Is the proxy NotBounties updated?");
        }
        short currentMessage;
        short remainingMessages;
        try {
            currentMessage = msgIn.readShort();
            remainingMessages = msgIn.readShort();
        } catch (EOFException e) {
            // proxy isn't updated
            Bukkit.getLogger().severe("[NotBounties] Message count has not been transmitted from the proxy. Is the proxy NotBounties updated?");
            currentMessage = 1;
            remainingMessages = 0;
        }
        if (currentMessage == 1) {
            bountyCache = bounties;
            statCache = playerStatMap;
            playerDataCache = playerData;
        } else {
            bountyCache.addAll(bounties);
            statCache.putAll(playerStatMap);
            playerDataCache.addAll(playerData);
        }

        if (remainingMessages == 0) {
            DataManager.connectProxy(bountyCache, statCache, playerDataCache);
        }
        if (!preparedUpdateMessage.isEmpty() && !preparedUpdateMessage.get(0).isSending()) {
            // restart messages
            preparedUpdateMessage.get(0).sendMessage(!Bukkit.isPrimaryThread());
        }

    }

    private void receivePlayerSkin(DataInputStream msgIn) throws IOException, URISyntaxException {
        short numSkins = msgIn.readShort();
        for (int i = 0; i < numSkins; i++) {
            UUID uuid = UUID.fromString(msgIn.readUTF());
            String id = msgIn.readUTF();
            String url = msgIn.readUTF();
            SkinManager.saveSkin(uuid, new PlayerSkin(new URI(url).toURL(), id, false));
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
        ProxyDatabase.setDatabaseOnlinePlayers(players);
    }

    /**
     * Received an update from another server with bounties that should be updated.
     * @param msgIn The received message.
     * @throws IOException If there was an error reading the message.
     */
    private void receiveBountyUpdate(DataInputStream msgIn) throws IOException {
        // Forward BountyUpdate
        short numBounties = msgIn.readShort();
        LocalData localData = DataManager.getLocalData();
        for (short i = 0; i < numBounties; i++) {
            BountyChange.ChangeType changeType = BountyChange.ChangeType.valueOf(msgIn.readUTF());
            Bounty bounty = new Bounty(msgIn.readUTF());
            if (ProxyDatabase.isDatabaseSynchronization()) {
                switch (changeType) {
                    case ADD_BOUNTY -> localData.addBounty(bounty);
                    case DELETE_BOUNTY -> localData.removeBounty(bounty);
                    case NOTIFY -> localData.notifyBounty(bounty.getUUID());
                    case REPLACE_BOUNTY -> localData.replaceBounty(bounty.getUUID(), bounty);
                }
            }
        }
    }

    /**
     * Received an update from another server with stats to be changed.
     * @param msgIn The received message.
     * @throws IOException If there was an error reading the message.
     */
    private void receiveStatUpdate(DataInputStream msgIn) throws IOException {
        // Forward StatUpdate
        short numStats = msgIn.readShort();
        LocalData localData = DataManager.getLocalData();
        for (short i = 0; i < numStats; i++) {
            UUID uuid = UUID.fromString(msgIn.readUTF());
            PlayerStat playerStat = new PlayerStat(msgIn.readUTF());
            if (ProxyDatabase.isDatabaseSynchronization())
                localData.addStats(uuid, playerStat);
        }
    }

    /**
     * Received an update from another server with player data to be changed.
     * @param msgIn The received message.
     * @throws IOException If there was an error reading the message.
     */
    private void receivePlayerDataUpdate(DataInputStream msgIn) throws IOException {
        // Forward StatUpdate
        short numData = msgIn.readShort();
        LocalData localData = DataManager.getLocalData();
        for (short i = 0; i < numData; i++) {
            PlayerData playerData = PlayerData.fromJson(msgIn.readUTF());
            if (ProxyDatabase.isDatabaseSynchronization())
                localData.updatePlayerData(playerData);
        }
    }

    /**
     * Sends a message to the backend server
     * @param identifier message identifier
     * @param data data to be sent
     */
    public static void sendMessage(String identifier, byte[] data) {
        if (ProxyDatabase.isEnabled() && NotBounties.getInstance().isEnabled()) {
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
        // return is for future compatibility
    }

    /**
     * Send data changes to the proxy.
     *
     * @param bountyChanges Bounties to be changed.
     */
    public static void sendBountyUpdate(List<BountyChange> bountyChanges) {
        // send proxy message
        NotBounties.debugMessage("Sending Bounty Update of " + bountyChanges.size() + " changes.", false);
        List<String[]> encoded = new LinkedList<>();
        for (BountyChange bountyChange : bountyChanges)
            encoded.add(new String[]{bountyChange.changeType().toString(), bountyChange.change().toJson().toString()});
        addPreparedUpdateMessage(new PreparedUpdateMessage("BountyUpdate", encoded, idCounter++));
    }

    /**
     * Send data changes to the proxy.
     *
     * @param statChanges Stats to be changed.
     */
    public static void sendStatUpdate(Map<UUID, PlayerStat> statChanges) {
        // send proxy message
        NotBounties.debugMessage("Sending Stat Update of " + statChanges.size() + " changes.", false);
        List<String[]> encoded = new LinkedList<>();
        for (Map.Entry<UUID, PlayerStat> entry : statChanges.entrySet())
            encoded.add(new String[]{entry.getKey().toString(), entry.getValue().toJson().toString()});
        addPreparedUpdateMessage(new PreparedUpdateMessage("StatUpdate", encoded, idCounter++));
    }

    public static void sendPlayerDataUpdate(List<PlayerData> playerDataList) {
        NotBounties.debugMessage("Sending PlayerData Update of " + playerDataList.size() + " changes.", false);
        List<String[]> encoded = new LinkedList<>();
        for (PlayerData playerData : playerDataList)
            encoded.add(new String[]{playerData.toJson().toString()});
        addPreparedUpdateMessage(new PreparedUpdateMessage("PlayerDataUpdate", encoded, idCounter++));
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
        if (ProxyDatabase.areSkinRequestsEnabled()) {
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
                    if (!preparedUpdateMessage.isEmpty() && !preparedUpdateMessage.get(0).isSending())
                        preparedUpdateMessage.get(0).sendMessage(!Bukkit.isPrimaryThread());
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
            if (preparedUpdateMessage.isEmpty()) {
                updateMessage.sendMessage(!Bukkit.isPrimaryThread());
            }
            preparedUpdateMessage.add(updateMessage);
        }
    }

}

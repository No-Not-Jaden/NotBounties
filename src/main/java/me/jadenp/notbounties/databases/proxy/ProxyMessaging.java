package me.jadenp.notbounties.databases.proxy;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.databases.LocalData;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.BountyChange;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.utils.PlayerStat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ProxyMessaging implements PluginMessageListener, Listener {
    private static boolean connectedBefore = false;
    private static final String CHANNEL = "notbounties:main";


    /**
     * Check if the proxy has been connected since server start
     * @return True if the proxy has connected to the plugin since the server has started
     */
    public static boolean hasConnectedBefore() {
        return connectedBefore;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] bytes) {
        if (!channel.equals(CHANNEL))
            return;
        connectedBefore = true;
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        try {
            receiveMessage(in);
        } catch (IOException e) {
            Bukkit.getLogger().warning(() -> "[NotBounties] Error receiving message from proxy!");
            Bukkit.getLogger().warning(e.toString());
        }
    }

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
                String subSubChannel = in.readUTF();
                switch (subSubChannel) {
                    case "BountyUpdate" -> receiveBountyUpdate(msgIn);
                    case "StatUpdate" -> receiveStatUpdate(msgIn);
                    case "LogPlayer" -> {
                        short numPlayers = msgIn.readShort();
                        for (short i = 0; i < numPlayers; i++) {
                            Map.Entry<UUID, String> entry = parsePlayerString(msgIn.readUTF());
                            LoggedPlayers.logPlayer(entry.getValue(), entry.getKey());
                        }
                    }
                    default -> Bukkit.getLogger().warning(() -> "[NotBounties] Received unknown message from proxy: " + subSubChannel);
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
            default -> Bukkit.getLogger().warning(() -> "[NotBounties] Received unknown message from proxy: " + subChannel);
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

    private void receiveConnection(DataInputStream msgIn) throws IOException {
        short savedBounties = msgIn.readShort();
        List<Bounty> bounties = new LinkedList<>();
        for (short i = 0; i < savedBounties; i++) {
            bounties.add(new Bounty(msgIn.readUTF()));
        }
        short savedStats = msgIn.readShort();
        Map<UUID, PlayerStat> playerStatMap = new HashMap<>();
        for (short i = 1; i < savedStats; i++) {
            UUID uuid = null;
            try {
                uuid = UUID.fromString(msgIn.readUTF());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotBounties] Invalid uuid in received proxy message.");
            }
            if (uuid != null)
                playerStatMap.put(uuid, new PlayerStat(msgIn.readUTF()));
        }
        DataManager.connectProxy(bounties, playerStatMap);
    }

    private void receivePlayerSkin(DataInputStream msgIn) throws IOException, URISyntaxException {
        short numSkins = msgIn.readShort();
        for (int i = 0; i < numSkins; i++) {
            UUID uuid = UUID.fromString(msgIn.readUTF());
            String id = msgIn.readUTF();
            String url = msgIn.readUTF();
            SkinManager.saveSkin(uuid, new PlayerSkin(new URI(url).toURL(), id));
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
            switch (changeType) {
                case ADD_BOUNTY -> localData.addBounty(bounty);
                case DELETE_BOUNTY -> localData.removeBounty(bounty);
                case NOTIFY -> localData.notifyBounty(bounty.getUUID());
                case REPLACE_BOUNTY -> localData.replaceBounty(bounty.getUUID(), bounty);
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
            localData.addStats(uuid, playerStat);
        }
    }

    /**
     * Sends a message to the backend server
     * @param identifier message identifier
     * @param data data to be sent
     */
    public static void sendMessage(String identifier, byte[] data) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    sendMessage(identifier, data, Bukkit.getOnlinePlayers().iterator().next());
                }
            }
        }.runTask(NotBounties.getInstance());

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
        try {
            byte[] message = wrapGlobalMessage(encodeMessage(bountyChanges));
            sendMessage(CHANNEL, message);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Could not send a data update.");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    /**
     * Send data changes to the proxy.
     *
     * @param statChanges Stats to be changed.
     */
    public static void sendStatUpdate(Map<UUID, PlayerStat> statChanges) {
        // send proxy message
        try {
            byte[] message = wrapGlobalMessage(encodeMessage(statChanges));
            sendMessage(CHANNEL, message);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Could not send a data update.");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    /**
     * Wraps a message in the needed bytes to send the message globally
     * @param stream A ByteArrayOutputStream to be sent as the message
     * @return A byte[] ready to be sent as a message
     */
    private static byte[] wrapGlobalMessage(ByteArrayOutputStream stream) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        //out.writeUTF("ALL"); // This is the target server. "ALL" will message all servers apart from the one sending the message
        //out.writeUTF(channel); // This is the channel.

        out.writeShort(stream.toByteArray().length); // This is the length.
        out.write(stream.toByteArray()); // This is the message.

        return out.toByteArray();
    }

    private static List<String> encodeBounty(BountyChange bountyChange) {
        List<String> encoded = new LinkedList<>();
        encoded.add(bountyChange.changeType().toString());
        encoded.add(bountyChange.change().toJson().toString());
        return encoded;
    }

    /**
     * Encodes a message in a byte array to be sent to all the other servers on the network.
     * This function will encode the bounties into a string array.
     * @param bountyChanges Bounties to encode.
     * @return A ByteArrayOutputStream with the message ready to be wrapped
     * @throws IOException if an I/O error occurs
     */
    private static ByteArrayOutputStream encodeMessage(List<BountyChange> bountyChanges) throws IOException {
        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgBytes);

        msgout.writeUTF("BountyUpdate"); // write the channel
        msgout.writeShort(bountyChanges.size()); // write the number of bounties
        for (BountyChange bounty : bountyChanges)
            for (String str : encodeBounty(bounty))
                msgout.writeUTF(str);

        return msgBytes;
    }

    /**
     * Encodes a message in a byte array to be sent to all the other servers on the network.
     * This function will encode the bounties into a string array.
     * @param statChanges Stats to encode.
     * @return A ByteArrayOutputStream with the message ready to be wrapped
     * @throws IOException if an I/O error occurs
     */
    private static ByteArrayOutputStream encodeMessage(Map<UUID, PlayerStat> statChanges) throws IOException {
        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgBytes);

        msgout.writeUTF("StatUpdate"); // write the channel
        msgout.writeShort(statChanges.size()); // write the number of stats
        for (Map.Entry<UUID, PlayerStat> entry : statChanges.entrySet()) {
            msgout.writeUTF(entry.getKey().toString());
            msgout.writeUTF(entry.getValue().toJson().toString());
        }
        return msgBytes;
    }

    /**
     * Request the player list from the proxy.
     */
    public static void requestPlayerList(){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        out.writeUTF("ALL");
        sendMessage(CHANNEL, out.toByteArray());
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
        sendMessage(CHANNEL, wrapGlobalMessage(msgBytes));
    }

    private static final List<UUID> queuedSkinRequests = new ArrayList<>();
    /**
     * Requests skin information for a player.
     * @param uuid UUID of the player
     */
    public static void requestPlayerSkin(UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    SkinManager.failRequest(uuid);
                    queuedSkinRequests.add(uuid);
                } else {
                    sendSkinRequest(uuid);
                    UUID[] skinsToSend = queuedSkinRequests.toArray(new UUID[0]);
                    queuedSkinRequests.clear();
                    new BukkitRunnable() {
                        int index = 0;
                        @Override
                        public void run() {
                            if (index >= skinsToSend.length) {
                                this.cancel();
                                return;
                            }
                            sendSkinRequest(skinsToSend[index]);
                            index++;
                            if (index >= skinsToSend.length) {
                                this.cancel();
                            }

                        }
                    }.runTaskTimer(NotBounties.getInstance(), 5, 5);
                }
            }
        }.runTask(NotBounties.getInstance());

    }

    private static void sendSkinRequest(UUID uuid) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerSkin");

        String playerName = LoggedPlayers.getPlayerName(uuid);
        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgBytes);
        try {
            msgout.writeUTF(playerName);
            msgout.writeUTF(uuid.toString());
        } catch (IOException e) {
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
            return;
        }
        out.writeShort(msgBytes.toByteArray().length); // This is the length.
        out.write(msgBytes.toByteArray()); // This is the message.
        sendMessage(CHANNEL, out.toByteArray());
        if (NotBounties.debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Sent player skin request.");
    }

}

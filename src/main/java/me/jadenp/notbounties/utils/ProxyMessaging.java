package me.jadenp.notbounties.utils;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.SkinManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.util.*;

public class ProxyMessaging implements PluginMessageListener, Listener {
    private static boolean connectedBefore = false;

    /**
     * Check if the proxy has been connected since server start
     * @return True if the proxy has connected to the plugin since the server has started
     */
    public static boolean hasConnectedBefore() {
        return connectedBefore;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] bytes) {
        if (!channel.equals("notbounties:main"))
            return;
        connectedBefore = true;
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();
        if (NotBounties.debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Received a message from proxy: " + subChannel);
        switch (subChannel) {
            case "ReceiveConnection":
                short savedPlayers = in.readShort();
                Map<UUID, Double> networkTokens = new HashMap<>();
                for (short i = 0; i < savedPlayers; i++) {
                    try {
                        UUID uuid = UUID.fromString(in.readUTF());
                        double amount = in.readDouble();
                        networkTokens.put(uuid, amount);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("[NotTokensPremium] Error reading uuid from proxy message!");
                    }
                }
                //TokenManager.connectProxy(networkTokens);
                break;
            case "PlayerList":
                String playerList = in.readUTF(); // CSV (Comma-Separated Values)

                String[] splitList = playerList.split(",");
                //Bukkit.getLogger().info("Received PlayerList: " + Arrays.toString(splitList));
                // send them over to LoggedPlayers
                //LoggedPlayers.clearOnlinePlayers();
                //LoggedPlayers.receiveNetworkPlayers(List.of(splitList));
                break;
            case "Forward": {
                in.readUTF(); // ALL
                String subSubChannel = in.readUTF();
                switch (subSubChannel) {
                    case "ServerTokenUpdate": {
                        short len = in.readShort();
                        byte[] msgbytes = new byte[len];
                        in.readFully(msgbytes);

                        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgbytes));
                        int maxReceive = 2000;
                        while (maxReceive > 0) {
                            try {
                                String uuid = msgIn.readUTF();
                                double tokenChange = msgIn.readDouble();
                                try {
                                    //TokenManager.editTokensSilently(UUID.fromString(uuid), tokenChange);
                                } catch (IllegalArgumentException e) {
                                    Bukkit.getLogger().warning("[NotTokensPremium] Could not get a uuid from the text: " + uuid + " (" + tokenChange + " token change)");
                                }
                            } catch (EOFException e) {
                                //Bukkit.getLogger().info("Reached End");
                                break;
                            } catch (IOException e) {
                                Bukkit.getLogger().warning("[NotTokensPremium] Error receiving message from proxy!");
                                Bukkit.getLogger().warning(e.toString());
                            }
                            maxReceive--;
                        }
                        break;
                    }
                    case "PlayerTokenUpdate": {
                        short len = in.readShort();
                        byte[] msgbytes = new byte[len];
                        in.readFully(msgbytes);

                        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgbytes));
                        try {
                            String uuid = msgIn.readUTF();
                            double tokenChange = msgIn.readDouble();
                            try {
                                //TokenManager.editTokensSilently(UUID.fromString(uuid), tokenChange);
                            } catch (IllegalArgumentException e) {
                                Bukkit.getLogger().warning("[NotTokensPremium] Could not get a uuid from the text: " + uuid + " (" + tokenChange + " token change)");
                            }
                        } catch (IOException e) {
                            Bukkit.getLogger().warning("[NotTokensPremium] Error receiving message from proxy!");
                            Bukkit.getLogger().warning(e.toString());
                        }
                        break;
                    }
                    case "LogPlayer": {
                        short len = in.readShort();
                        byte[] msgbytes = new byte[len];
                        in.readFully(msgbytes);

                        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgbytes));
                        try {
                            String playerName = msgIn.readUTF();
                            String uuid = msgIn.readUTF();
                            String xuid = msgIn.readUTF(); // will be "Java" if not a bedrock player
                            try {
                                //LoggedPlayers.logPlayer(playerName, UUID.fromString(uuid));
                            } catch (IllegalArgumentException e) {
                                Bukkit.getLogger().warning("[NotTokensPremium] Could not get a uuid from the text: " + uuid + " (" + playerName + ")");
                            }
                        } catch (IOException e) {
                            Bukkit.getLogger().warning("[NotTokensPremium] Error receiving message from proxy!");
                            Bukkit.getLogger().warning(e.toString());
                        }
                        break;
                    }
                    case "LogAllPlayers": {
                        short len = in.readShort();
                        byte[] msgbytes = new byte[len];
                        in.readFully(msgbytes);

                        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgbytes));
                        try {
                            for (int i = 0; i < 2000; i++) {
                                String msg = msgIn.readUTF();
                                String[] split = msg.split(":");
                                String playerName = split[0];
                                String uuid = split[1];
                                String xuid = split[2]; // will be "Java" if not a bedrock player
                                try {
                                    //LoggedPlayers.logPlayer(playerName, UUID.fromString(uuid));
                                } catch (IllegalArgumentException e) {
                                    Bukkit.getLogger().warning("[NotTokensPremium] Could not get a uuid from the text: " + uuid + " (" + playerName + ")");
                                }
                            }

                        } catch (EOFException e) {
                            //Bukkit.getLogger().info("Reached End");
                            break;
                        } catch (IOException e) {
                            Bukkit.getLogger().warning("[NotTokensPremium] Error receiving message from proxy!");
                            Bukkit.getLogger().warning(e.toString());
                        }
                        break;
                    }
                }
                break;
            }
            case "PlayerSkin": {
                short len = in.readShort();
                byte[] msgbytes = new byte[len];
                in.readFully(msgbytes);

                DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgbytes));
                try {
                    for (int i = 0; i < 2000; i++) {
                        String uuidString = msgIn.readUTF();
                        String id = msgIn.readUTF();
                        String url = msgIn.readUTF();
                        try {
                            UUID uuid = UUID.fromString(uuidString);
                            SkinManager.saveSkin(uuid, new PlayerSkin(new URL(url), id));
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().warning("[NotBounties] Could not get a uuid from the text: " + uuidString);
                        }
                    }

                } catch (EOFException e) {
                    //Bukkit.getLogger().info("Reached End");
                    break;
                } catch (IOException e) {
                    if (NotBounties.debug) {
                        Bukkit.getLogger().warning("[NotBounties] Error receiving message from proxy!");
                        Bukkit.getLogger().warning(e.toString());
                    }
                }
                break;
            }
        }
    }

    public ProxyMessaging(){
    }


    /**
     * Sends a message to the backend server
     * @param identifier message identifier
     * @param data data to be sent
     * @return true if the message was sent successfully
     */
    public static boolean sendMessage(String identifier, byte[] data) {
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            sendMessage(identifier, data, Bukkit.getOnlinePlayers().iterator().next());
            return true;
        }
         return false;
    }

    /**
     * Sends a message to the backend server
     *
     * @param identifier message identifier
     * @param data       data to be sent
     * @param player     player to send the message through
     */
    public static void sendMessage(String identifier, byte[] data, Player player) {
        //Bukkit.getLogger().info("Sending: " + identifier + " with " + player.getName());
        player.sendPluginMessage(NotBounties.getInstance(), identifier, data);
        // return is for future compatibility
    }

    /**
     * Send a token update for a single player
     *
     * @param uuid        UUID of player to be updated
     * @param tokenChange Tokens to be changed from the player's balance
     */
    public static void sendPlayerTokenUpdate(UUID uuid, double tokenChange) {
        // send proxy message
        try {
            byte[] message = wrapGlobalMessage(encodeMessage(uuid.toString(), tokenChange), "PlayerTokenUpdate");
            sendMessage("notbounties:main", message);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Could not send a token update for " + NotBounties.getPlayerName(uuid));
            Bukkit.getLogger().warning(e.toString());
        }
    }

    /**
     * Send token updates for multiple players
     *
     * @param playerTokens Map of uuid of player and amount of tokens to be changed
     */
    public static void sendServerTokenUpdate(Map<UUID, Double> playerTokens) {
        // send proxy message
        try {
            byte[] message = wrapGlobalMessage(encodeMessage(playerTokens), "ServerTokenUpdate");
            sendMessage("notbounties:main", message);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Could not send a server token update.");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    /**
     * Wraps a message in the needed bytes to send the message globally
     * @param stream A ByteArrayOutputStream to be sent as the message
     * @return A byte[] ready to be sent as a message
     */
    private static byte[] wrapGlobalMessage(ByteArrayOutputStream stream, String channel) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL"); // This is the target server. "ALL" will message all servers apart from the one sending the message
        out.writeUTF(channel); // This is the channel.

        out.writeShort(stream.toByteArray().length); // This is the length.
        out.write(stream.toByteArray()); // This is the message.

        return out.toByteArray();
    }

    /**
     * Wraps a message in the needed bytes to send the message just to the proxy
     * @param stream A ByteArrayOutputStream to be sent as the message
     * @return A byte[] ready to be sent as a message
     */
    private static byte[] wrapRequestMessage(ByteArrayOutputStream stream, String channel) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(channel); // This is the channel.

        out.writeShort(stream.toByteArray().length); // This is the length.
        out.write(stream.toByteArray()); // This is the message.

        return out.toByteArray();
    }

    /**
     * Encodes a message in a byte array to be sent to all the other servers on the network
     * @param message Message to be sent
     * @param value Value to be encoded with the message
     * @return A ByteArrayOutputStream with the message ready to be wrapped
     * @throws IOException if an I/O error occurs
     */
    private static ByteArrayOutputStream encodeMessage(String message, double value) throws IOException {
        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        msgout.writeUTF(message);
        msgout.writeDouble(value);
        return msgbytes;
    }

    /**
     * Encodes a message in a byte array to be sent to all the other servers on the network.
     * This function will encode every value in the map to the message as a string and double
     * @param playerTokens Map to encode
     * @return A ByteArrayOutputStream with the message ready to be wrapped
     * @throws IOException if an I/O error occurs
     */
    private static ByteArrayOutputStream encodeMessage(Map<UUID, Double> playerTokens) throws IOException {
        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        for (Map.Entry<UUID, Double> entry : playerTokens.entrySet()) {
            msgout.writeUTF(entry.getKey().toString());
            msgout.writeDouble(entry.getValue());
        }
        return msgbytes;
    }

    /**
     * Request the player list from all servers
     * @return True if the request was successful
     */
    public static boolean requestPlayerList(){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        out.writeUTF("ALL");
        return sendMessage("notbounties:main", out.toByteArray());
    }

    /**
     * Sends a player to the network to be logged
     * @param player Player to be logged
     */
    public static void logNewPlayer(Player player) {
        logNewPlayer(player.getName(), player.getUniqueId());
    }

    /**
     * Sends a player to the network to be logged
     *
     * @param playerName The name of the player
     * @param uuid       The UUID of the player
     */
    public static void logNewPlayer(String playerName, UUID uuid) {
        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        try {
            msgout.writeUTF(playerName);
            msgout.writeUTF(uuid.toString());
            if (NotBounties.isBedrockPlayer(uuid)) {
                msgout.writeUTF(NotBounties.getXuid(uuid));
            } else {
                msgout.writeUTF("Java");
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
            return;
        }
        sendMessage("notbounties:main", wrapGlobalMessage(msgbytes, "LogPlayer"));
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

        String playerName = NotBounties.getPlayerName(uuid);
        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        try {
            msgout.writeUTF(playerName);
            msgout.writeUTF(uuid.toString());
        } catch (IOException e) {
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
            return;
        }
        out.writeShort(msgbytes.toByteArray().length); // This is the length.
        out.write(msgbytes.toByteArray()); // This is the message.
        sendMessage("notbounties:main", out.toByteArray());
        if (NotBounties.debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Sent player skin request.");
    }

}

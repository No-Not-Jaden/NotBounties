package me.jadenp.notbounties.features.settings.databases.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.jadenp.notbounties.NotBounties;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.Messenger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class PreparedUpdateMessage {

    private PreparedUpdateMessage futureMessage = null;
    private final byte[] message;
    private static final int MAX_MESSAGE_SIZE = Messenger.MAX_MESSAGE_SIZE - 1024;
    private boolean sent = false;
    private boolean sending = false;
    private boolean canceled = false;


    public PreparedUpdateMessage(String channel, List<String[]> messages) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(channel);
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgBytes);

            List<String[]> missedMessages = new LinkedList<>();
            ByteArrayOutputStream tmpBytes = new ByteArrayOutputStream();
            DataOutputStream tmpMsg = new DataOutputStream(tmpBytes);
            int includedCount = 0;

            for (String[] packedPayload : messages) {
                long payloadSize = Arrays.stream(packedPayload).mapToInt(m -> m.getBytes(StandardCharsets.UTF_8).length).sum();
                if (tmpMsg.size() + msgout.size() + payloadSize > MAX_MESSAGE_SIZE) {
                    missedMessages.add(packedPayload);
                } else {
                    for (String m : packedPayload)
                        tmpMsg.writeUTF(m);
                    includedCount++;
                }
            }

            msgout.writeShort(includedCount);
            msgout.write(tmpBytes.toByteArray());

            tmpMsg.close();

            if (includedCount > 0 && !missedMessages.isEmpty()) {
                futureMessage = new PreparedUpdateMessage(channel, missedMessages);
            }

            out.writeShort(msgBytes.toByteArray().length); // This is the length.
            out.write(msgBytes.toByteArray()); // This is the message.

            message = out.toByteArray();

            msgout.close();
        }  catch (IOException e) {
            NotBounties.getInstance().getLogger().log(Level.WARNING, "Failed to prepare proxy message {}", e);
            throw new IllegalStateException("Message preparation failed", e);
        }


    }

    public void sendMessage() {
        if (message.length == 0) {
            return;
        }
        if (!NotBounties.getInstance().isEnabled()) {
            // save messages to be sent next connection
            NotBounties.getInstance().getLogger().log(Level.WARNING, "Plugin shutting down in the middle of a proxy message.");
            // keep track outside this, and make a var if the msg was sent
            return;
        }
        sending = true;
        NotBounties.getServerImplementation().global().runDelayed(() -> {
            if (!Bukkit.getOnlinePlayers().isEmpty() && NotBounties.getInstance().isEnabled() && ProxyDatabase.isEnabled() && !canceled) {
                ProxyMessaging.sendMessage(ProxyMessaging.CHANNEL, ProxyMessaging.wrapGlobalMessage(message), Bukkit.getOnlinePlayers().iterator().next());
                sent = true;
                if (futureMessage != null) {
                    futureMessage.sendMessage();
                } else {
                    ProxyMessaging.onCompleteProxyMessage();
                }
            } else {
                NotBounties.debugMessage("Failed to send proxy update.", true);
                ProxyMessaging.onFailProxyMessage();
                sending = false;
            }
        }, 5);

    }

    public List<byte[]> getUnsentMessages(){
        List<byte[]> unSent = futureMessage != null ? futureMessage.getUnsentMessages() : new LinkedList<>();
        if (!sent) {
            unSent.add(0, message);
        }
        return unSent;
    }

    public @Nullable PreparedUpdateMessage getFirstUnsent() {
        if (!sent)
            return this;
        if (futureMessage != null)
            return futureMessage.getFirstUnsent();
        return null;
    }

    public boolean isSent() {
        return sent;
    }

    public boolean isSending() {
        return sending;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}

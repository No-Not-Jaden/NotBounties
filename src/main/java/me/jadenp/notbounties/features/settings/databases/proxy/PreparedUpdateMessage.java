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
    private static final int MAX_MESSAGE_SIZE = 32000;
    private boolean sent = false;
    private boolean sending = false;
    private boolean canceled = false;
    private final long id;

    public PreparedUpdateMessage(List<byte[]> messages, long id) {
        this.id = id;
        message = messages.remove(0);
        if (!messages.isEmpty()) {
            futureMessage = new PreparedUpdateMessage(messages, id);
        }
    }

    public PreparedUpdateMessage(String channel, List<String[]> messages, long id) {
        this.id = id;
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(channel); // subchannel
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
                futureMessage = new PreparedUpdateMessage(channel, missedMessages, id);
            }

            //out.writeShort(msgBytes.toByteArray().length); // This is the length.
            out.write(msgBytes.toByteArray()); // This is the message.

            message = ProxyMessaging.wrapGlobalMessage(out.toByteArray());

            msgout.close();
        }  catch (IOException e) {
            NotBounties.getInstance().getLogger().log(Level.WARNING, "Failed to prepare proxy message", e);
            throw new IllegalStateException("Message preparation failed", e);
        }


    }

    public void sendMessage(boolean delay) {
        if (message.length == 0) {
            ProxyMessaging.onCompleteProxyMessage(id);
            return;
        }
        if (!NotBounties.getInstance().isEnabled()) {
            // save messages to be sent next connection
            NotBounties.getInstance().getLogger().log(Level.WARNING, "Plugin shutting down in the middle of a proxy message.");
            ProxyMessaging.onFailProxyMessage(id);
            return;
        }
        sending = true;
        if (delay) {
            NotBounties.getServerImplementation().global().runDelayed(this::executeMessage, 5);
        } else {
            executeMessage();
        }


    }

    private void executeMessage() {
        if (!Bukkit.getOnlinePlayers().isEmpty() && NotBounties.getInstance().isEnabled() && ProxyDatabase.isEnabled() && !canceled) {
            ProxyMessaging.sendMessage(ProxyMessaging.CHANNEL, message, Bukkit.getOnlinePlayers().iterator().next());
            sent = true;
            if (futureMessage != null) {
                futureMessage.sendMessage(true);
            } else {
                ProxyMessaging.onCompleteProxyMessage(id);
            }
        } else {
            NotBounties.debugMessage("Failed to send proxy update.", true);
            ProxyMessaging.onFailProxyMessage(id);
            sending = false;
        }
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
        if (futureMessage != null) {
            futureMessage.setCanceled(canceled);
        }
    }

    public long getId() {
        return id;
    }
}

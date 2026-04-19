package me.jadenp.notbounties.utils.tasks;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static me.jadenp.notbounties.features.LanguageOptions.*;

public class BroadcastTask extends CancelableTask {

    private final OfflinePlayer receiver;
    private final Player setter;
    private final double displayAmount;
    private final double totalBounty;
    private final Whitelist whitelist;

    public BroadcastTask(@Nullable Player setter, @NotNull OfflinePlayer receiver, double displayAmount, double totalBounty, Whitelist whitelist) {
        super();
        this.receiver = receiver;
        this.setter = setter;
        this.displayAmount = displayAmount;
        this.totalBounty = totalBounty;
        this.whitelist = whitelist;
    }

    @Override
    public void run() {
        if (!SkinManager.isSkinLoaded(receiver.getUniqueId()))
            return;
        this.cancel();
        final String setterName = setter == null ? ConfigOptions.getAutoBounties().getConsoleBountyName() : setter.getName();
        String receiverMsg = getMessage("bounty-receiver");
        if (!receiverMsg.isBlank())
            receiverMsg = parse(getPrefix() + receiverMsg, setterName, displayAmount, totalBounty, receiver);
        String setterMsg = getMessage("bounty-success");
        if (!setterMsg.isBlank())
            setterMsg = parse(getPrefix() + setterMsg, displayAmount, totalBounty, receiver);
        String shortMessage = getMessage("bounty-broadcast");
        if (!shortMessage.isBlank())
            shortMessage = parse(getPrefix() + shortMessage, setterName, displayAmount, totalBounty, receiver);
        String[] extendedMessage = getExtendedBroadcast(setterName, receiver, displayAmount, totalBounty);

        sendFormattedMessages(receiverMsg, setterMsg, shortMessage, extendedMessage);

    }

    private void sendFormattedMessages(String receiverMsg, String setterMsg, String shortMessage, String[] extendedMessage) {
        NotBounties.getServerImplementation().global().run(() -> {
            sendReceiverMsg(receiverMsg);

            sendSetterMsg(setterMsg);

            if (!shortMessage.isBlank())
                Bukkit.getConsoleSender().sendMessage(shortMessage);
            if (whitelist.getList().isEmpty()) {
                if (displayAmount >= ConfigOptions.getMoney().getMinBroadcast())
                    sendNonWhitelistedBroadcast(shortMessage, extendedMessage);
            } else {
                sendWhitelistedBroadcast(shortMessage, extendedMessage);
            }
        });
    }

    private void sendSetterMsg(String setterMsg) {
        if (setter != null && setter.isOnline()) {
            if (!setterMsg.isBlank())
                setter.sendMessage(setterMsg);
            if (NotBounties.getServerVersion() <= 16) {
                setter.playSound(setter.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                setter.playSound(setter.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
            }
        }
    }

    private void sendReceiverMsg(String receiverMsg) {
        if (receiver.isOnline()) {
            Player onlineReceiver = Objects.requireNonNull(receiver.getPlayer());
            if (!receiverMsg.isBlank())
                onlineReceiver.sendMessage(receiverMsg);
            if (NotBounties.getServerVersion() <= 16) {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
            }
        }
    }

    private void sendWhitelistedBroadcast(String shortMessage, String[] extendedMessage) {
        if (whitelist.isBlacklist()) {
            if (displayAmount < ConfigOptions.getMoney().getMinBroadcast())
                return;
            sendBlacklistBroadcast(shortMessage, extendedMessage);
        } else {
            sendWhitelistBroadcast(shortMessage, extendedMessage);
        }
    }

    private void sendWhitelistBroadcast(String shortMessage, String[] extendedMessage) {
        for (UUID uuid : whitelist.getList()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || player.getUniqueId().equals(receiver.getUniqueId()))
                continue;
            sendBroadcast(shortMessage, extendedMessage, player, true);
            getListMessage("whitelist-notify").stream().filter(str -> !str.isEmpty()).forEach(str -> player.sendMessage(parse(getPrefix() + str, player)));
        }
    }

    private void sendBlacklistBroadcast(String shortMessage, String[] extendedMessage) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if ((player.getUniqueId().equals(receiver.getUniqueId()) && !ConfigOptions.getMoney().isBroadcastToReceiver())
                    || whitelist.getList().contains(player.getUniqueId()))
                continue;
            sendBroadcast(shortMessage, extendedMessage, player, false); // if blacklist causes duplicate messages, this is why
            getListMessage("whitelist-notify").stream().filter(str -> !str.isEmpty()).forEach(str -> player.sendMessage(parse(getPrefix() + str, player)));
        }
    }

    private void sendNonWhitelistedBroadcast(String shortMessage, String[] extendedMessage) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(receiver.getUniqueId()) && !ConfigOptions.getMoney().isBroadcastToReceiver())
                continue;
            sendBroadcast(shortMessage, extendedMessage, player, false);
        }
    }

    private static void sendBroadcast(String shortMessage, String[] extendedMessage, Player player, boolean overrideDisable) {
        PlayerData.BroadcastSettings broadcastSettings = DataManager.getPlayerData(player.getUniqueId()).getBroadcastSettings();
        if (broadcastSettings == PlayerData.BroadcastSettings.SHORT
                || (broadcastSettings == PlayerData.BroadcastSettings.DISABLE && overrideDisable)) {
            if (!shortMessage.isBlank())
                player.sendMessage(shortMessage);
        } else if (broadcastSettings == PlayerData.BroadcastSettings.EXTENDED) {
            for (String string : extendedMessage) {
                player.sendMessage(string);
            }
        }
    }

    /**
     * Get the extended message for a bounty broadcast
     * @param setterName Name of the setter.
     * @param receiver Player that got a bounty set on them.
     * @param displayAmount The amount of the recently added bounty
     * @param totalBounty The total bounty amount.
     * @return An array of messages for the extended broadcast, or null if no message should be sent.
     */
    private static @Nullable String[] getExtendedBroadcast(String setterName, @NotNull OfflinePlayer receiver, double displayAmount, double totalBounty) {
        if (!SkinManager.isSkinLoaded(receiver.getUniqueId())) {
            NotBounties.debugMessage("Tried to get extended broadcast for " + receiver.getName() + " but they do not have a skin.", true);
            return null;
        }
        BufferedImage face = SkinManager.getPlayerFace(receiver.getUniqueId());
        if (face == null) {
            NotBounties.debugMessage("Tried to get extended broadcast for " + receiver.getName() + " but they do not have a face.", true);
            return null;
        }
        //▮
        String[] message = new String[8];
        final List<String> extendedText = LanguageOptions.getListMessage("extended-bounty-broadcast");
        if (extendedText.isEmpty())
            return null;
        for (int y = 0; y < 8; y++) {
            StringBuilder builder = new StringBuilder();
            for (int x = 0; x < 8; x++) {
                Color color = getColor(face.getRGB(x,y));
                builder.append(net.md_5.bungee.api.ChatColor.of(color)).append('█');
            }
            if (extendedText.size() > y)
                builder.append(" ").append(parse(extendedText.get(y), setterName, displayAmount, totalBounty, receiver));
            message[y] = builder.toString();
        }

        return message;
    }

    private static Color getColor(int argb) {
        int b = (argb)&0xFF;
        int g = (argb>>8)&0xFF;
        int r = (argb>>16)&0xFF;
        int a = (argb>>24)&0xFF;
        return new Color(r,g,b,a);
    }

}

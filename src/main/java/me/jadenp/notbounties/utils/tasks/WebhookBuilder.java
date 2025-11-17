package me.jadenp.notbounties.utils.tasks;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.webhook.DiscordWebhook;
import me.jadenp.notbounties.features.webhook.Webhook;
import me.jadenp.notbounties.features.webhook.WebhookField;
import me.jadenp.notbounties.features.webhook.WebhookOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.util.UUID;

public class WebhookBuilder extends CancelableTask{
    private final Webhook webhook;
    private final WebhookOptions webhookOptions;
    private final UUID receiver;
    private final UUID player;
    private final double amount;
    private final double total;
    private int maxRequests = 50;

    public WebhookBuilder(Webhook webhook, WebhookOptions webhookOptions, UUID receiver, UUID player, double amount, double total) {
        super();
        this.webhook = webhook;
        this.webhookOptions = webhookOptions;
        this.receiver = receiver;
        this.player = player;
        this.amount = amount;
        this.total = total;
    }

    @Override
    public void run() {

        boolean sendEmbed = !webhook.getTitle().isEmpty() || !webhook.getDescription().isEmpty() || !webhook.getFooterText().isEmpty() || webhook.isSendImage();
        DiscordWebhook.EmbedObject embed = null;
        UUID avatarUUID = webhook.isSwitchImages() ? receiver : player;
        UUID imageUUID = webhook.isSwitchImages() ? player : receiver;

        boolean avatarLoaded = SkinManager.isSkinLoaded(avatarUUID);
        boolean imageLoaded = SkinManager.isSkinLoaded(imageUUID);
        if ((!avatarLoaded || !imageLoaded) && maxRequests > 0) {
            // check if max requests hit
            maxRequests--;
            if (maxRequests <= 0) {
                NotBounties.debugMessage("Timed out loading skin for " + LoggedPlayers.getPlayerName(avatarUUID) + " or " + LoggedPlayers.getPlayerName(imageUUID), true);
            }
            return;
        }
        this.cancel();
        String avatarURL = WebhookOptions.parseImageURL(true, avatarUUID, avatarLoaded);
        String imageURL = WebhookOptions.parseImageURL(false, imageUUID, imageLoaded);

        if (sendEmbed) {
            embed = new DiscordWebhook.EmbedObject()
                    .setTitle(ChatColor.stripColor(LanguageOptions.parse(webhook.getTitle(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))))
                    .setDescription(ChatColor.stripColor(LanguageOptions.parse(webhook.getDescription(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))))
                    .setColor(webhook.getColor())
                    .setFooter(ChatColor.stripColor(LanguageOptions.parse(webhook.getFooterText(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))), webhook.getFooterURL());
            for (WebhookField field : webhook.getContent())
                embed.addField(ChatColor.stripColor(LanguageOptions.parse(field.getName(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))), ChatColor.stripColor(LanguageOptions.parse(field.getValue(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))), field.isInline());
            if (webhook.isSendImage())
                embed.setImage(imageURL);
        }
        String username = ChatColor.stripColor(LanguageOptions.parse(webhook.getUsername(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver)));
        String content = ChatColor.stripColor(LanguageOptions.parse(webhook.getMessage(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver)));
        try {
            webhookOptions.sendEmbed(embed, username, avatarURL, content);
        } catch (IOException e) {
            NotBounties.debugMessage("Could not send a discord webhook!", true);

        }
    }
}

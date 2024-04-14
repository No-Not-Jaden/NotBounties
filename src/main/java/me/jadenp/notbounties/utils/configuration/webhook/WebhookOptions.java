package me.jadenp.notbounties.utils.configuration.webhook;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.bountyEvents.BountyClaimEvent;
import me.jadenp.notbounties.bountyEvents.BountyEditEvent;
import me.jadenp.notbounties.bountyEvents.BountyRemoveEvent;
import me.jadenp.notbounties.bountyEvents.BountySetEvent;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WebhookOptions implements Listener {
    private static String link;
    private static boolean tts;
    private static final Map<Class<? extends Event>, Webhook> webhooks = new HashMap<>();
    public static File getFile() {
        return new File(NotBounties.getInstance().getDataFolder() + File.separator + "webhook.yml");
    }

    public static void reloadOptions() {
        if (!getFile().exists())
            NotBounties.getInstance().saveResource("webhook.yml", false);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(getFile());

        link = configuration.getString("link");
        tts = configuration.getBoolean("text-to-speech");
        webhooks.clear();
        webhooks.put(BountySetEvent.class, new Webhook(Objects.requireNonNull(configuration.getConfigurationSection("bounty-set"))));
        webhooks.put(BountyClaimEvent.class, new Webhook(Objects.requireNonNull(configuration.getConfigurationSection("bounty-claim"))));
        webhooks.put(BountyRemoveEvent.class, new Webhook(Objects.requireNonNull(configuration.getConfigurationSection("bounty-remove"))));
        webhooks.put(BountyEditEvent.class, new Webhook(Objects.requireNonNull(configuration.getConfigurationSection("bounty-edit"))));
    }

    @EventHandler
    public void onBountySet(BountySetEvent event) {
        if (link.equals("https://discord.com/api/webhooks/...") || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(Bukkit.getOfflinePlayer(event.getBounty().getUUID()));
        double total = totalBounty != null ? totalBounty.getTotalBounty() : 0;
        total = total + event.getBounty().getTotalBounty();
        buildWebhook(webhook, event.getBounty().getName(), event.getBounty().getUUID(), event.getBounty().getLastSetter().getName(), event.getBounty().getLastSetter().getUuid(), event.getBounty().getTotalBounty(), total);
    }

    @EventHandler
    public void onBountyClaim(BountyClaimEvent event) {
        if (link.equals("https://discord.com/api/webhooks/...") || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(Bukkit.getOfflinePlayer(event.getBounty().getUUID()));
        double total = totalBounty != null ? totalBounty.getTotalBounty() : 0;
        total = total - event.getBounty().getTotalBounty();
        buildWebhook(webhook, event.getBounty().getName(), event.getBounty().getUUID(), event.getKiller().getName(), event.getKiller().getUniqueId(), event.getBounty().getTotalBounty(), total);
    }

    @EventHandler
    public void onBountyRemove(BountyRemoveEvent event) {
        if (link.equals("https://discord.com/api/webhooks/...") || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(Bukkit.getOfflinePlayer(event.getBounty().getUUID()));
        double total = totalBounty != null ? totalBounty.getTotalBounty() : 0;
        total = total - event.getBounty().getTotalBounty();
        UUID removerUUID = event.getRemover() instanceof Player ? ((Player) event.getRemover()).getUniqueId() : new UUID(0,0);
        buildWebhook(webhook, event.getBounty().getName(), event.getBounty().getUUID(), event.getRemover().getName(), removerUUID, event.getBounty().getTotalBounty(), total);
    }

    @EventHandler
    public void onBountyEdit(BountyEditEvent event) {
        if (link.equals("https://discord.com/api/webhooks/...") || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        UUID editorUUID = event.getEditor() instanceof Player ? ((Player) event.getEditor()).getUniqueId() : new UUID(0,0);
        buildWebhook(webhook, event.getBeforeEdit().getName(), event.getBeforeEdit().getUUID(), event.getEditor().getName(), editorUUID, event.getBeforeEdit().getTotalBounty(), event.getAfterEdit().getTotalBounty());
    }
    
    private void buildWebhook(Webhook webhook, String playerName, UUID playerUUID, String receiverName, UUID receiverUUID, double amount, double total){
        new BukkitRunnable() {
            @Override
            public void run() {
                if (webhook.isEnabled()) {
                    boolean sendEmbed = !webhook.getTitle().isEmpty() || !webhook.getDescription().isEmpty() || !webhook.getFooterText().isEmpty() || webhook.isSendImage();
                    DiscordWebhook.EmbedObject embed = null;
                    UUID avatarUUID = webhook.isSwitchImages() ? playerUUID : receiverUUID;
                    UUID imageUUID = webhook.isSwitchImages() ? receiverUUID : playerUUID;
                    String avatarTextureID = "46ba63344f49dd1c4f5488e926bf3d9e2b29916a6c50d610bb40a5273dc8c82";
                    String imageTextureID = "46ba63344f49dd1c4f5488e926bf3d9e2b29916a6c50d610bb40a5273dc8c82";
                    String tempId = Head.getTextureID(avatarUUID);
                    if (tempId != null)
                        avatarTextureID = tempId;
                    tempId = Head.getTextureID(imageUUID);
                    if (tempId != null)
                        imageTextureID = tempId;
                    String avatarURL = "https://mc-heads.net/head/" + avatarTextureID + ".png";
                    String imageURL = "https://mc-heads.net/avatar/" + imageTextureID + "/128.png";
                    if (sendEmbed) {
                        embed = new DiscordWebhook.EmbedObject()
                                .setTitle(ChatColor.stripColor(LanguageOptions.parse(webhook.getTitle(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(receiverUUID))))
                                .setDescription(ChatColor.stripColor(LanguageOptions.parse(webhook.getDescription(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(receiverUUID))))
                                .setColor(webhook.getColor())
                                .setFooter(ChatColor.stripColor(LanguageOptions.parse(webhook.getFooterText(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(receiverUUID))), webhook.getFooterURL());
                        for (WebhookField field : webhook.getContent())
                            embed.addField(ChatColor.stripColor(LanguageOptions.parse(field.getName(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(receiverUUID))), ChatColor.stripColor(LanguageOptions.parse(field.getValue(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID))), field.isInline());
                        embed.setImage(imageURL);
                    }
                    String username = ChatColor.stripColor(LanguageOptions.parse(webhook.getUsername(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(receiverUUID)));
                    String content = ChatColor.stripColor(LanguageOptions.parse(webhook.getMessage(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(receiverUUID)));
                    try {
                        sendEmbed(embed, username, avatarURL, content);
                    } catch (IOException e) {
                        Bukkit.getLogger().warning("[NotBounties] Could not send a discord webhook!");
                        Bukkit.getLogger().warning(e.toString());
                    }
                }
            }
        }.runTaskAsynchronously(NotBounties.getInstance());

    }

    private void sendEmbed(DiscordWebhook.EmbedObject embed, String username, String avatarURL, String content) throws IOException {
        if (link.equals("https://discord.com/api/webhooks/..."))
            return;
        DiscordWebhook webhook = new DiscordWebhook(link);
        webhook.setTts(tts);
        webhook.setContent(content);
        webhook.setAvatarUrl(avatarURL);
        webhook.setUsername(username);
        if (embed != null)
            webhook.addEmbed(embed);
        webhook.execute();
    }

}

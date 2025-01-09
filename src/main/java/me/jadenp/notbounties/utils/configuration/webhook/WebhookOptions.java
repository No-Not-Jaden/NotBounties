package me.jadenp.notbounties.utils.configuration.webhook;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.bounty_events.BountyClaimEvent;
import me.jadenp.notbounties.bounty_events.BountyEditEvent;
import me.jadenp.notbounties.bounty_events.BountyRemoveEvent;
import me.jadenp.notbounties.bounty_events.BountySetEvent;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
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
    private static final String UNSET_LINK = "https://discord.com/api/webhooks/...";

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
        if (link.equals(UNSET_LINK) || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(event.getBounty().getUUID());
        double total = totalBounty != null ? totalBounty.getTotalDisplayBounty() : 0;
        total = total + event.getBounty().getTotalDisplayBounty();
        buildWebhook(webhook, event.getBounty().getUUID(), event.getBounty().getLastSetter().getUuid(), event.getBounty().getTotalDisplayBounty(), total);
    }

    @EventHandler
    public void onBountyClaim(BountyClaimEvent event) {
        if (link.equals(UNSET_LINK) || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(event.getBounty().getUUID());
        double total = totalBounty != null ? totalBounty.getTotalDisplayBounty() : 0;
        total = total - event.getBounty().getTotalDisplayBounty();
        buildWebhook(webhook, event.getBounty().getUUID(), event.getKiller().getUniqueId(), event.getBounty().getTotalDisplayBounty(), total);
    }

    @EventHandler
    public void onBountyRemove(BountyRemoveEvent event) {
        if (link.equals(UNSET_LINK) || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(event.getBounty().getUUID());
        double total = totalBounty != null ? totalBounty.getTotalDisplayBounty() : 0;
        total = total - event.getBounty().getTotalDisplayBounty();
        UUID removerUUID = event.getRemover() instanceof Player player ? player.getUniqueId() : DataManager.GLOBAL_SERVER_ID;
        buildWebhook(webhook, event.getBounty().getUUID(), removerUUID, event.getBounty().getTotalDisplayBounty(), total);
    }

    @EventHandler
    public void onBountyEdit(BountyEditEvent event) {
        if (link.equals(UNSET_LINK) || event.isCancelled())
            return;
        Webhook webhook = webhooks.get(event.getClass());
        UUID editorUUID = event.getEditor() instanceof Player player ? player.getUniqueId() : DataManager.GLOBAL_SERVER_ID;
        buildWebhook(webhook, event.getBeforeEdit().getUUID(), editorUUID, event.getBeforeEdit().getTotalDisplayBounty(), event.getAfterEdit().getTotalBounty());
    }
    
    private void buildWebhook(Webhook webhook, UUID receiver, UUID player, double amount, double total){
        if (webhook.isEnabled()) {
            new BukkitRunnable() {
                int maxRequests = 50;

                @Override
                public void run() {

                    boolean sendEmbed = !webhook.getTitle().isEmpty() || !webhook.getDescription().isEmpty() || !webhook.getFooterText().isEmpty() || webhook.isSendImage();
                    DiscordWebhook.EmbedObject embed = null;
                    UUID avatarUUID = webhook.isSwitchImages() ? receiver : player;
                    UUID imageUUID = webhook.isSwitchImages() ? player : receiver;

                    if (!SkinManager.isSkinLoaded(avatarUUID) || !SkinManager.isSkinLoaded(imageUUID)) {
                        // check if max requests hit
                        if (maxRequests <= 0) {
                            this.cancel();
                            if (NotBounties.debug) {
                                Bukkit.getLogger().warning("[NotBountiesDebug] Timed out loading skin for " + LoggedPlayers.getPlayerName(avatarUUID) + " or " + LoggedPlayers.getPlayerName(imageUUID));
                            }
                        }
                        maxRequests--;
                        return;
                    }
                    this.cancel();
                    String avatarTextureID = SkinManager.getSkin(avatarUUID).getId();
                    String imageTextureID = SkinManager.getSkin(imageUUID).getId();
                    String avatarURL = "https://mc-heads.net/head/" + avatarTextureID + ".png";
                    String imageURL = "https://mc-heads.net/avatar/" + imageTextureID + "/128.png";
                    if (sendEmbed) {
                        embed = new DiscordWebhook.EmbedObject()
                                .setTitle(ChatColor.stripColor(LanguageOptions.parse(webhook.getTitle(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))))
                                .setDescription(ChatColor.stripColor(LanguageOptions.parse(webhook.getDescription(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))))
                                .setColor(webhook.getColor())
                                .setFooter(ChatColor.stripColor(LanguageOptions.parse(webhook.getFooterText(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))), webhook.getFooterURL());
                        for (WebhookField field : webhook.getContent())
                            embed.addField(ChatColor.stripColor(LanguageOptions.parse(field.getName(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))), ChatColor.stripColor(LanguageOptions.parse(field.getValue(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver))), field.isInline());
                        embed.setImage(imageURL);
                    }
                    String username = ChatColor.stripColor(LanguageOptions.parse(webhook.getUsername(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver)));
                    String content = ChatColor.stripColor(LanguageOptions.parse(webhook.getMessage(), Bukkit.getOfflinePlayer(player), amount, total, Bukkit.getOfflinePlayer(receiver)));
                    try {
                        sendEmbed(embed, username, avatarURL, content);
                    } catch (IOException e) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getLogger().warning("[NotBounties] Could not send a discord webhook!");
                                Bukkit.getLogger().warning(e.toString());
                            }
                        }.runTask(NotBounties.getInstance());

                    }
                }


            }.runTaskTimerAsynchronously(NotBounties.getInstance(), 0, 4);
        }

    }

    private void sendEmbed(DiscordWebhook.EmbedObject embed, String username, String avatarURL, String content) throws IOException {
        if (link.equals(UNSET_LINK))
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

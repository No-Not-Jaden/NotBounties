package me.jadenp.notbounties.utils.configuration.webhook;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.bountyEvents.BountyClaimEvent;
import me.jadenp.notbounties.bountyEvents.BountyEditEvent;
import me.jadenp.notbounties.bountyEvents.BountyRemoveEvent;
import me.jadenp.notbounties.bountyEvents.BountySetEvent;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public static void reloadOptions() throws IOException {
        if (!getFile().exists())
            NotBounties.getInstance().saveResource("webhook.yml", false);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(getFile());
        // fill in any default options that aren't present
        configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("webhook.yml")))));
        for (String key : Objects.requireNonNull(configuration.getDefaults()).getKeys(true)) {
            if (!configuration.isSet(key))
                configuration.set(key, configuration.getDefaults().get(key));
        }
        configuration.save(getFile());

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
        if (link.equals("https://discord.com/api/webhooks/..."))
            return;
        Webhook webhook = webhooks.get(event.getClass());
        buildWebhook(webhook, event.getBounty().getName(), event.getBounty().getUUID(), event.getBounty().getLastSetter().getName(), event.getBounty().getLastSetter().getUuid(), event.getBounty().getLastSetter().getAmount(), event.getBounty().getTotalBounty());
    }

    @EventHandler
    public void onBountyClaim(BountyClaimEvent event) {
        if (link.equals("https://discord.com/api/webhooks/..."))
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(Bukkit.getOfflinePlayer(event.getBounty().getUUID()));
        double total = totalBounty != null ? totalBounty.getTotalBounty() : 0;
        buildWebhook(webhook, event.getBounty().getName(), event.getBounty().getUUID(), event.getKiller().getName(), event.getKiller().getUniqueId(), event.getBounty().getTotalBounty(), total);
    }

    @EventHandler
    public void onBountyRemove(BountyRemoveEvent event) {
        if (link.equals("https://discord.com/api/webhooks/..."))
            return;
        Webhook webhook = webhooks.get(event.getClass());
        Bounty totalBounty = BountyManager.getBounty(Bukkit.getOfflinePlayer(event.getBounty().getUUID()));
        double total = totalBounty != null ? totalBounty.getTotalBounty() : 0;
        UUID removerUUID = event.getRemover() instanceof Player ? ((Player) event.getRemover()).getUniqueId() : new UUID(0,0);
        buildWebhook(webhook, event.getBounty().getName(), event.getBounty().getUUID(), event.getRemover().getName(), removerUUID, event.getBounty().getTotalBounty(), total);
    }

    @EventHandler
    public void onBountyEdit(BountyEditEvent event) {
        if (link.equals("https://discord.com/api/webhooks/..."))
            return;
        Webhook webhook = webhooks.get(event.getClass());
        UUID editorUUID = event.getEditor() instanceof Player ? ((Player) event.getEditor()).getUniqueId() : new UUID(0,0);
        buildWebhook(webhook, event.getBeforeEdit().getName(), event.getBeforeEdit().getUUID(), event.getEditor().getName(), editorUUID, event.getBeforeEdit().getTotalBounty(), event.getAfterEdit().getTotalBounty());
    }
    
    private void buildWebhook(Webhook webhook, String playerName, UUID playerUUID, String receiverName, UUID receiverUUID, double amount, double total){
        
        if (webhook.isEnabled()) {
            DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                    .setTitle(LanguageOptions.parse(webhook.getTitle(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID)))
                    .setDescription(LanguageOptions.parse(webhook.getDescription(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID)))
                    .setColor(webhook.getColor())
                    .setFooter(LanguageOptions.parse(webhook.getFooterText(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID)), webhook.getFooterURL());
            for (WebhookField field : webhook.getContent())
                embed.addField(LanguageOptions.parse(field.getName(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID)), LanguageOptions.parse(field.getValue(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID)), field.isInline());
            String avatarUUID = webhook.isSwitchImages() ? playerUUID.toString() : receiverUUID.toString();
            String imageUUID = webhook.isSwitchImages() ? receiverUUID.toString() : playerUUID.toString();
            String avatarURL = "http://cravatar.eu/helmhead/" + avatarUUID + ".png";
            String imageURL = "http://cravatar.eu/helmavatar/" + imageUUID + ".png";
            embed.setImage(imageURL);
            String username = LanguageOptions.parse(webhook.getUsername(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID));
            String content = LanguageOptions.parse(webhook.getMessage(), playerName, receiverName, amount, total, Bukkit.getOfflinePlayer(playerUUID));
            try {
                sendEmbed(embed, username, avatarURL, content);
            } catch (IOException e) {
                Bukkit.getLogger().warning("[NotBounties] Could not send a discord webhook!");
                Bukkit.getLogger().warning(e.toString());
            }
        }
    }

    private void sendEmbed(DiscordWebhook.EmbedObject embed, String username, String avatarURL, String content) throws IOException {
        if (link.equals("https://discord.com/api/webhooks/..."))
            return;
        DiscordWebhook webhook = new DiscordWebhook(link);
        webhook.setTts(tts);
        webhook.setContent(content);
        webhook.setAvatarUrl(avatarURL);
        webhook.setUsername(username);
        webhook.addEmbed(embed);
        webhook.execute();
    }
}

package me.jadenp.notbounties.features.webhook;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.bounty_events.BountyClaimEvent;
import me.jadenp.notbounties.bounty_events.BountyEditEvent;
import me.jadenp.notbounties.bounty_events.BountyRemoveEvent;
import me.jadenp.notbounties.bounty_events.BountySetEvent;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.tasks.WebhookBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
            WebhookBuilder webhookBuilder = new WebhookBuilder(webhook, this, receiver, player, amount, total);
            webhookBuilder.setTaskImplementation(NotBounties.getServerImplementation().async().runAtFixedRate(webhookBuilder, 1, 4));
        }

    }

    public void sendEmbed(DiscordWebhook.EmbedObject embed, String username, String avatarURL, String content) throws IOException {
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

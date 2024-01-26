package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.DiscordWebhook;
import me.jadenp.notbounties.utils.LanguageOptions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.regex.Matcher;

public class WebhookOptions {
    private String link;
    private boolean textToSpeech;
    private ConfigurationSection bountySetConfiguration;
    public void reloadOptions() throws IOException {
        File webhookFile = getFile();
        if (!webhookFile.exists())
            NotBounties.getInstance().saveResource("webhook.yml", false);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(webhookFile);
        // fill in any default options that aren't present
        configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("language.yml")))));
        for (String key : Objects.requireNonNull(configuration.getDefaults()).getKeys(true)) {
            if (!configuration.isSet(key))
                configuration.set(key, configuration.getDefaults().get(key));
        }
        configuration.save(webhookFile);

        link = configuration.getString("link");
        textToSpeech = configuration.getBoolean("text-to-speech");
        bountySetConfiguration = configuration.getConfigurationSection("bounty-set");
    }

    public void sendBountySet(Bounty bounty) {
        if (!isEnabled())
            return;
        boolean switchImages = bountySetConfiguration.getBoolean("switch-images");
        DiscordWebhook webhook = new DiscordWebhook(link);
        webhook.setTts(textToSpeech);
        webhook.setAvatarUrl("http://cravatar.eu/helmhead/" + (switchImages ? bounty.getUUID() : bounty.getLastSetter().getUuid()) + ".png");
        String username = bountySetConfiguration.getString("username");
        assert username != null;
        if (!username.isEmpty())
            webhook.setUsername(LanguageOptions.parse(username, bounty.getName(), bounty.getLastSetter().getName(), bounty.getTotalBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())));
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setColor(getColor(bountySetConfiguration.getString("color")))
                .setTitle(LanguageOptions.parse(bountySetConfiguration.getString("title"), bounty.getName(), bounty.getLastSetter().getName(), bounty.getTotalBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())))
                .setDescription(LanguageOptions.parse(bountySetConfiguration.getString("description"), bounty.getName(), bounty.getLastSetter().getName(), bounty.getTotalBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())))
                .setImage("http://cravatar.eu/helmavatar/" + (switchImages ? bounty.getLastSetter().getUuid() : bounty.getUUID()) + ".png");
        for (String key : Objects.requireNonNull(bountySetConfiguration.getConfigurationSection("content")).getKeys(false)) {
            String name = bountySetConfiguration.getString("content." + key + ".name");
            String value = bountySetConfiguration.getString("content." + key + ".value");
            boolean inline = bountySetConfiguration.getBoolean("content." + key + ".inline");
            embed.addField(name, value, inline);
        }
        String footer = bountySetConfiguration.getString("footer.text");
        assert footer != null;
        if (!footer.isEmpty())
            embed.setFooter(footer, bountySetConfiguration.getString("footer.image-url"));
        webhook.addEmbed(embed);
        try {
            webhook.execute();
        } catch (IOException e) {
            Bukkit.getLogger().warning("[NotBounties] Failed to send a webhook message.");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    public void sendBountyClaim(Bounty bounty, Player killer) {
        if (!isEnabled())
            return;
        boolean switchImages = bountySetConfiguration.getBoolean("switch-images");
        DiscordWebhook webhook = new DiscordWebhook(link);
        webhook.setTts(textToSpeech);
        webhook.setAvatarUrl("http://cravatar.eu/helmhead/" + (switchImages ? bounty.getUUID() : killer.getUniqueId()) + ".png");
        String username = bountySetConfiguration.getString("username");
        assert username != null;
        if (!username.isEmpty())
            webhook.setUsername(LanguageOptions.parse(username, bounty.getName(), bounty.getLastSetter().getName(), bounty.getTotalBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())));
        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setColor(getColor(bountySetConfiguration.getString("color")))
                .setTitle(LanguageOptions.parse(Objects.requireNonNull(bountySetConfiguration.getString("title")).replaceAll("\\{killer}", Matcher.quoteReplacement(killer.getName())), bounty.getName(), bounty.getLastSetter().getName(), bounty.getTotalBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())))
                .setDescription(LanguageOptions.parse(Objects.requireNonNull(bountySetConfiguration.getString("description")).replaceAll("\\{killer}", Matcher.quoteReplacement(killer.getName())), bounty.getName(), bounty.getLastSetter().getName(), bounty.getTotalBounty(), Bukkit.getOfflinePlayer(bounty.getUUID())))
                .setImage("http://cravatar.eu/helmavatar/" + (switchImages ? killer.getUniqueId() : bounty.getUUID()) + ".png");
        for (String key : Objects.requireNonNull(bountySetConfiguration.getConfigurationSection("content")).getKeys(false)) {
            String name = bountySetConfiguration.getString("content." + key + ".name");
            String value = bountySetConfiguration.getString("content." + key + ".value");
            boolean inline = bountySetConfiguration.getBoolean("content." + key + ".inline");
            embed.addField(name, value, inline);
        }
        String footer = bountySetConfiguration.getString("footer.text");
        assert footer != null;
        if (!footer.isEmpty())
            embed.setFooter(footer, bountySetConfiguration.getString("footer.image-url"));
        webhook.addEmbed(embed);
        try {
            webhook.execute();
        } catch (IOException e) {
            Bukkit.getLogger().warning("[NotBounties] Failed to send a webhook message.");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    private Color getColor(String string) {
            Color color = Color.getColor(string);
            if (color == null)
                return Color.WHITE;
            return color;
    }

    private File getFile() {
        return new File(NotBounties.getInstance().getDataFolder() + File.separator + "webhook.yml");
    }

    public boolean isEnabled(){
        return !link.equals("https://discord.com/api/webhooks/...");
    }
}

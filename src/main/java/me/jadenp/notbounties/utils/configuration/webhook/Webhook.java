package me.jadenp.notbounties.utils.configuration.webhook;


import me.jadenp.notbounties.bountyEvents.BountySetEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;


public class Webhook {
    private final boolean enabled;
    private final String username;
    private final boolean switchImages;
    private final String title;
    private final String description;
    private final Color color;
    private final String footerText;
    private final String footerURL;
    private final List<WebhookField> content = new ArrayList<>();
    private final String message;
    public Webhook(ConfigurationSection configuration) {
        enabled = configuration.getBoolean("enabled");
        username = configuration.getString("username");
        switchImages = configuration.getBoolean("switch-images");
        title = configuration.getString("title");
        description = configuration.getString("description");
        color = Color.getColor(configuration.getString("color"));
        footerText = configuration.getString("footer.text");
        footerURL = configuration.getString("footer.image-url");
        content.clear();
        for (String key : Objects.requireNonNull(configuration.getConfigurationSection("content")).getKeys(false)) {
            content.add(new WebhookField(Objects.requireNonNull(configuration.getConfigurationSection("content." + key))));
        }
        message = configuration.getString("message");
    }

    public String getMessage() {
        return message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSwitchImages() {
        return switchImages;
    }

    public String getTitle() {
        return title;
    }

    public Color getColor() {
        return color;
    }

    public List<WebhookField> getContent() {
        return content;
    }

    public String getDescription() {
        return description;
    }

    public String getFooterText() {
        return footerText;
    }

    public String getFooterURL() {
        return footerURL;
    }

    public String getUsername() {
        return username;
    }
}

package me.jadenp.notbounties.utils.configuration.webhook;

import org.bukkit.configuration.ConfigurationSection;

public class WebhookField {
    private String name;
    private String value;
    private boolean inline;
    public WebhookField(ConfigurationSection configuration) {
        name = configuration.getString("name");
        value = configuration.getString("value");
        inline = configuration.getBoolean("inline");
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isInline() {
        return inline;
    }
}

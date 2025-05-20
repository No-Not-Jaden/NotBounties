package me.jadenp.notbounties.features.webhook;


import org.bukkit.configuration.ConfigurationSection;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Webhook {
    private final boolean enabled;
    private final String username;
    private final boolean switchImages;
    private final String title;
    private final String description;
    private Color color;
    private final String footerText;
    private final String footerURL;
    private final List<WebhookField> content = new ArrayList<>();
    private final String message;
    private final boolean sendImage;
    public Webhook(ConfigurationSection configuration) {
        enabled = configuration.isSet("enabled") && configuration.getBoolean("enabled"); // false default
        username = configuration.isSet("username") ? configuration.getString("username") : "";
        switchImages = configuration.isSet("switch-images") && configuration.getBoolean("switch-images"); // false default
        title = configuration.isSet("title") ? configuration.getString("title") : "";
        description = configuration.isSet("description") ? configuration.getString("description") : "";
        footerText = configuration.isSet("footer.text") ? configuration.getString("footer.text") : "";
        footerURL = configuration.isSet("footer.image-url") ? configuration.getString("footer.image-url") : "";
        message = configuration.isSet("message") ? configuration.getString("message") : "";
        sendImage = !configuration.isSet("send-image") || configuration.getBoolean("send-image"); // true default
        content.clear();
        if (configuration.isConfigurationSection("content"))
            for (String key : Objects.requireNonNull(configuration.getConfigurationSection("content")).getKeys(false)) {
                if (configuration.isConfigurationSection("content." + key))
                    content.add(new WebhookField(Objects.requireNonNull(configuration.getConfigurationSection("content." + key))));
            }

        String colorString = configuration.getString("color");
        if (colorString == null)
            colorString = "BLACK";
        try {
            Field field = Class.forName("java.awt.Color").getField(colorString);
            color = (Color) field.get(null);
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
            color = null;
        }
        if (color == null) {
            try {
                color = Color.decode(colorString);
            } catch (NumberFormatException e) {
                // try to get rgb values
                colorString = colorString.replace(",", " "); // replace commas with spaces
                String[] split = colorString.split(" "); // split between all spaces
                List<String> rgbValues = new ArrayList<>(List.of(split)); // turn into array list for better manipulation
                rgbValues.removeIf(String::isEmpty); // remove empty strings
                if (rgbValues.size() < 3) {
                    // not enough values
                    color = Color.BLACK;
                } else {
                    try {
                        int r = Integer.parseInt(rgbValues.get(0));
                        int g = Integer.parseInt(rgbValues.get(1));
                        int b = Integer.parseInt(rgbValues.get(2));
                        color = new Color(r,g,b);
                    } catch (NumberFormatException e2) {
                        // not numbers
                        color = Color.BLACK;
                    }
                }
            }
        }
    }

    public String getMessage() {
        return message;
    }

    public boolean isSendImage() {
        return sendImage;
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

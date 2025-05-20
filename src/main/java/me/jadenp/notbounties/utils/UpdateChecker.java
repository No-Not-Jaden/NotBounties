package me.jadenp.notbounties.utils;
import me.jadenp.notbounties.NotBounties;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.function.Consumer;
 // https://api.spigotmc.org/legacy/update.php?resource=104484
// From: https://www.spigotmc.org/wiki/creating-an-update-checker-that-checks-for-updates
public class UpdateChecker {

    private final JavaPlugin plugin;
    private final int resourceId;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        NotBounties.getServerImplementation().async().runNow(() -> {
            try (InputStream inputStream = new URI("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).toURL().openStream(); Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (IOException | URISyntaxException exception) {
                plugin.getLogger().info("Unable to check for updates: " + exception);
            }
        });
    }
}
package me.jadenp.notbounties.features.settings.display.map;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class BackupFontManager {

    private BackupFontManager(){}

    private static BackupFont nameLine = null;
    private static BackupFont rewardLine = null;
    private static boolean usingBackupFont = false;

    public static void loadBackupFonts(){
        Plugin plugin = NotBounties.getInstance();
        plugin.getLogger().warning("Unable to access font configuration on this system. Reverting to backup font!");
        File backupDirectory = new File(plugin.getDataFolder() + File.separator + "posters" + File.separator + "backup fonts");
        File nameLineFile = new File(backupDirectory + File.separator + "Name Line.json");
        File rewardLineFile = new File(backupDirectory + File.separator + "Reward Line.json");


        // creating the files if they aren't there
        if (backupDirectory.mkdir())
            plugin.getLogger().info("Creating backup fonts.");
        if (!nameLineFile.exists())
            plugin.saveResource("posters/backup fonts/Name Line.json", false);
        if (!rewardLineFile.exists())
            plugin.saveResource("posters/backup fonts/Reward Line.json", false);

        try {
            nameLine = new BackupFont(nameLineFile);
            rewardLine = new BackupFont(rewardLineFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Error reading backup fonts.");
            plugin.getLogger().warning(e.toString());
            return;
        }

        usingBackupFont = true;
    }

    public static BackupFont getNameLine() {
        return nameLine;
    }

    public static BackupFont getRewardLine() {
        return rewardLine;
    }

    public static boolean isUsingTraditionalFont() {
        return !usingBackupFont;
    }
}

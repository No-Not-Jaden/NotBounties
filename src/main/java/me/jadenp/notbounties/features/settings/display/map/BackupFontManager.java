package me.jadenp.notbounties.features.settings.display.map;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;

public class BackupFontManager {

    private static BackupFont nameLine = null;
    private static BackupFont rewardLine = null;
    private static boolean usingBackupFont = false;
    public static void loadBackupFonts(){
        File nameLineFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "posters" + File.separator + "backup fonts" + File.separator + "Name Line.json");
        File rewardLineFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "posters" + File.separator + "backup fonts" + File.separator + "Reward Line.json");
        File backupDirectory = new File(NotBounties.getInstance().getDataFolder() + File.separator + "posters" + File.separator + "backup fonts");

        // creating the files if they aren't there
        if (backupDirectory.mkdir())
            Bukkit.getLogger().info("[NotBounties] Creating backup fonts.");
        if (!nameLineFile.exists())
            NotBounties.getInstance().saveResource("posters/backup fonts/Name Line.json", false);
        if (!rewardLineFile.exists())
            NotBounties.getInstance().saveResource("posters/backup fonts/Reward Line.json", false);

        try {
            nameLine = new BackupFont(nameLineFile);
            rewardLine = new BackupFont(rewardLineFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[NotBounties] Error reading backup fonts.");
            Bukkit.getLogger().warning(e.toString());
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

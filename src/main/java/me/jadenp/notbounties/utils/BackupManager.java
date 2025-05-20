package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.features.ConfigOptions;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BackupManager {

    public static void saveBackups(File sourceDirectory, File destinationDirectory) throws IOException {
        if (!ConfigOptions.isBountyBackups()) {
            return;
        }
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd");
        File todayZip = new File(destinationDirectory + File.separator + simpleDateFormat.format(date) + ".zip");

        if (destinationDirectory.mkdir()) {
            Bukkit.getLogger().info("[NotBounties] Created backup directory.");
        }

        if (!todayZip.exists()) {
            // delete old backups
            deleteOldBackups(destinationDirectory, simpleDateFormat);
            // create new backup
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            URI uri = URI.create("jar:" + todayZip.toURI());
            try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
                File[] files = sourceDirectory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory()) {
                            Path pathInZip = zipfs.getPath(file.getName());
                            java.nio.file.Files.copy(file.toPath(), pathInZip, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                }
            }

        }

    }



    private static void deleteOldBackups(File backupDirectory, SimpleDateFormat simpleDateFormat) {
        File[] files = backupDirectory.listFiles();
        if (files == null)
            return;

        Map<File, Long> fileDates = getFileDates(files, simpleDateFormat);
        // sort files by time created in ascending order (older files first)
        Map<File, Long> sortedMap =
                fileDates.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));
        // find out which files need to be deleted
        final long maxWeeklyBackup = 1000L * 60 * 60 * 24 * 7 * 3; // 3 weeks
        final long maxDailyBackup = 1000L * 60 * 60 * 24 * 7;
        int weeklyBackups = 0;
        long lastWeeklyBackup = 0;
        List<File> pendingDeletion = new ArrayList<>();
        for (Map.Entry<File, Long> entry : sortedMap.entrySet()) {
            long timeSinceCreation = System.currentTimeMillis() - entry.getValue();
            if (timeSinceCreation > maxWeeklyBackup) {
                // too long ago to be useful
                pendingDeletion.add(entry.getKey());
                continue;
            }
            if (timeSinceCreation > maxDailyBackup) {
                // been over 7 days since this backup was created
                if (weeklyBackups < 3 && Math.abs(timeSinceCreation - lastWeeklyBackup) >= maxDailyBackup - (1000L * 60 * 60 * 24)) { // 6 days
                    // only keep 3 weekly backups
                    weeklyBackups++;
                    lastWeeklyBackup = timeSinceCreation;
                } else {
                    // delete
                    pendingDeletion.add(entry.getKey());
                }
            }
        }
        deleteFiles(pendingDeletion);
    }

    private static void deleteFiles(List<File> files) {
        for (File file : files) {
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles();
                if (subFiles != null)
                    deleteFiles(Arrays.stream(subFiles).toList());
            }
            try {
                java.nio.file.Files.delete(file.toPath());
            } catch (IOException e) {
                Bukkit.getLogger().warning("[NotBounties] Could not delete old backup: " + file.getName());
                Bukkit.getLogger().warning(e.toString());
            }

        }
    }

    private static Map<File, Long> getFileDates(File[] files, SimpleDateFormat simpleDateFormat) {
        Map<File, Long> fileDates = new HashMap<>();
        for (File value : files) {
            try {
                fileDates.put(value, simpleDateFormat.parse(value.getName()).getTime());
            } catch (ParseException ignored) {
                // file not in correct format
            }
        }

        return fileDates;
    }

}

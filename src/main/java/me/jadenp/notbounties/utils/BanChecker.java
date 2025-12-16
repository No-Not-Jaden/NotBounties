package me.jadenp.notbounties.utils;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.integrations.external_api.LiteBansClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class BanChecker {
    private static TaskImplementation<Void> banCheckTask = null;
    private static long taskInterval = -1;
    private static double completionPercent = -1;
    private static int maxChecks = -1;
    private static boolean enabled = false;

    private BanChecker() {}

    private static void setBanCheckTask() {
        banCheckTask = NotBounties.getServerImplementation().global().runAtFixedRate(new Runnable() {
            List<Bounty> bountyListCopy = new ArrayList<>();
            int playersPerRun = 1;
            @Override
            public void run() {
                if (!enabled || NotBounties.isPaused())
                    return;
                if (bountyListCopy.isEmpty()) {
                    bountyListCopy = BountyManager.getAllBounties(-1);
                    playersPerRun = (int) (bountyListCopy.size() * completionPercent + 1);
                    if (maxChecks > 0 && playersPerRun > maxChecks)
                        playersPerRun = maxChecks;
                }
                NotBounties.debugMessage("Checking " + playersPerRun + " players for bans.", false);
                for (int i = 0; i < playersPerRun; i++) {
                    if (i >= bountyListCopy.size())
                        break;
                    Bounty bounty = bountyListCopy.get(i);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(bounty.getUUID()); // not thread safe
                    NotBounties.getServerImplementation().async().runNow(() -> {
                        if (isPlayerBanned(player)) {
                            NotBounties.getServerImplementation().global().run(() -> BountyManager.removeBounty(bounty.getUUID()));
                        }
                    });
                }
                if (playersPerRun > 0) {
                    if (bountyListCopy.size() > playersPerRun)
                        bountyListCopy.subList(0, playersPerRun).clear();
                    else
                        bountyListCopy.clear();
                }
            }
        }, 101, taskInterval);
    }

    public static void loadConfiguration(ConfigurationSection config) {
        boolean enabled = config.getBoolean("enabled", false);
        long taskInterval = config.getLong("task-interval", 100);
        int maxChecks = config.getInt("max-checks", -1);
        double completionPercent = config.getDouble("completion-percent", 0.005);
        long simpleCheckInterval = config.getLong("simple-check-interval", 0);
        BanChecker.enabled = enabled;

        if (simpleCheckInterval > 0) {
            // override advanced options
            maxChecks = 500;
            // complete 10% every interval
            completionPercent = 0.1;
            // convert seconds to ticks and divide by 10 to reach the simple interval
            taskInterval = simpleCheckInterval * 2;
        }

        BanChecker.maxChecks = maxChecks;
        if (completionPercent <= 0)
            completionPercent = 0.005;
        if (completionPercent > 1)
            completionPercent = 1;
        BanChecker.completionPercent = completionPercent;
        if (taskInterval != BanChecker.taskInterval) {
            // new value
            BanChecker.taskInterval = taskInterval;
            if (banCheckTask != null) {
                banCheckTask.cancel();
                banCheckTask = null;
            }
            if (taskInterval > 0)
                setBanCheckTask();
        }
    }

    public static boolean isPlayerBanned(OfflinePlayer player) {
        if (player.isBanned())
            return true;
        try {
            return ConfigOptions.getIntegrations().isLiteBansEnabled() && !new LiteBansClass().isPlayerNotBanned(player.getUniqueId());
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }
}

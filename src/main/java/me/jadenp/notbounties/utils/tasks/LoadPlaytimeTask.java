package me.jadenp.notbounties.utils.tasks;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.PlayerData;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * A task to check playtime for players. Intended to be run at a fixed rate every tick.
 */
public class LoadPlaytimeTask extends CancelableTask{

    private final List<UUID> allUsers;
    private int index;
    private long totalTimeElapsed = 0;
    private final boolean increase;
    private long lastRun;
    private long tickCount = 0;
    private long skippedTicks = 0;
    private long playersChecked = 0;
    private static final int MIN_BATCH_TIME_MS = 15; // time used in the tick to request a player
    private static final int TICK_GOAL_MS = 50; // MSPT maximum

    public LoadPlaytimeTask(boolean increase) {
        super();
        allUsers = new ArrayList<>(DataManager.getPlayerDataMap().keySet());
        index = 0;
        this.increase = increase;
        lastRun = System.currentTimeMillis();
    }

    @Override
    public void run() {
        tickCount++;
        if (System.currentTimeMillis() - lastRun > TICK_GOAL_MS) {
            // server is lagging, skip for now
            lastRun = System.currentTimeMillis();
            skippedTicks++;
            return;
        }
        lastRun = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        long timeElapsed = 0;
        while (timeElapsed < MIN_BATCH_TIME_MS && index < allUsers.size()) {
            // check playtime
            UUID uuid = allUsers.get(index++);
            PlayerData playerData = DataManager.getPlayerData(uuid);
            // if the time decreased, old players don't need to be checked again
            if (increase || playerData.isNewPlayer()) {
                playersChecked++;
                OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                boolean isNew = ((double) p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 1200) < ImmunityManager.getNewPlayerImmunity();
                playerData.setNewPlayer(isNew);
            }

            timeElapsed = System.currentTimeMillis() - startTime;
        }
        totalTimeElapsed += timeElapsed;

        if (index >= allUsers.size()) {
            cancel();
            NotBounties.getInstance().getLogger().log(Level.INFO, "Loaded playtime for {0} players. Took {1} ms of compute over {2} ticks ({3} skipped).", new Object[]{playersChecked, totalTimeElapsed, tickCount, skippedTicks});
        }
    }
}

package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.integrations.external_api.CMIClass;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUI;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUIOptions;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Player data that is not synced with the database and resets after a restart.
 */
public class ImpersistentPlayerData {
    private static final Map<UUID, ImpersistentPlayerData> data = new ConcurrentHashMap<>();
    private static final long CACHE_REFRESH_TIME = TimeUnit.MINUTES.toMillis(10);

    public static @NotNull ImpersistentPlayerData get(UUID uuid) {
        return data.computeIfAbsent(uuid, ImpersistentPlayerData::new);
    }

    public static Map<UUID, ImpersistentPlayerData> getAll() {
        return new HashMap<>(data);
    }

    private final Map<String, Integer> guiSortType = new HashMap<>();
    private final UUID uuid;
    private String playerName;
    private String displayName;
    private long displayNameLoadedAt = 0;

    private ImpersistentPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    private static @Nullable String getAPIDisplayName(@NotNull UUID uuid) {
        if (ConfigOptions.getIntegrations().isEssentialsEnabled()) {
            String name = ConfigOptions.getIntegrations().getEssentialsXClass().getNick(uuid);
            if (name != null)
                return name;
        }
        if (ConfigOptions.getIntegrations().isCMIEnabled()) {
            return CMIClass.getNick(uuid);
        }
        return null;
    }

    private void loadAPIDisplayNameAsync(UUID uuid) {
        NotBounties.getServerImplementation().async().runNow(() -> {
            String name = getAPIDisplayName(uuid);
            if (name != null) {
                setDisplayName(name);
            } else {
                if (ConfigOptions.getIntegrations().isEssentialsEnabled() || ConfigOptions.getIntegrations().isCMIEnabled()){
                    NotBounties.debugMessage("Failed to get API display name for " + uuid + ".", true);
                    displayName = null;
                }
            }

        });
    }

    public String getDisplayName() {
        if (displayName != null) {
            // check if it needs to be loaded again
            if (System.currentTimeMillis() - displayNameLoadedAt > CACHE_REFRESH_TIME) {
                // refresh name so it isn't loaded more than once
                displayNameLoadedAt = System.currentTimeMillis();
                loadAPIDisplayNameAsync(uuid);
            }
            return displayName;
        }
        setDisplayName(LoggedPlayers.getPlayerName(uuid));
        loadAPIDisplayNameAsync(uuid);
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        displayNameLoadedAt = System.currentTimeMillis();
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setGUISortType(String guiName, int sortType) {
        this.guiSortType.put(guiName, sortType);
    }

    /**
     * Get the sort type of GUI for this player.
     * @param guiName The type of the GUI from the config.
     * @return The sort type of the GUI. -1 if the GUI is not found.
     */
    public int getGUISortType(String guiName) {
        if (guiName == null || guiName.isEmpty()) return -1;
        if (guiSortType.containsKey(guiName)) {
            return guiSortType.get(guiName);
        }
        if (NotBounties.isBedrockPlayer(uuid)) {
            BedrockGUIOptions guiOptions = BedrockGUI.getGUI(guiName);
            if (guiOptions != null) {
                return guiOptions.getSortType();
            }
        }
        GUIOptions guiOptions = GUI.getGUI(guiName);
        if (guiOptions != null) {
            return guiOptions.getSortType();
        }
        return -1;
    }

    public UUID getUuid() {
        return uuid;
    }
}

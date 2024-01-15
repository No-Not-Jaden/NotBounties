package me.jadenp.notbounties.ui.gui;

import java.util.UUID;

public class PlayerGUInfo {
    private final long page;
    private final Object[] data;
    private final String guiType;
    private final UUID[] players;

    public PlayerGUInfo(long page, String guiType, Object[] data, UUID[] players){

        this.page = page;
        this.data = data;
        this.guiType = guiType;
        this.players = players;
    }

    public long getPage() {
        return page;
    }

    public Object[] getData() {
        return data;
    }

    public String getGuiType() {
        return guiType;
    }

    public UUID[] getPlayers() {
        return players;
    }
}

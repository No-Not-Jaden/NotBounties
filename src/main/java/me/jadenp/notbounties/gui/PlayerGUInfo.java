package me.jadenp.notbounties.gui;

public class PlayerGUInfo {
    private final long page;
    private final Object[] data;
    private final String guiType;

    public PlayerGUInfo(long page, String guiType, Object[] data){

        this.page = page;
        this.data = data;
        this.guiType = guiType;
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
}

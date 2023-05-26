package me.jadenp.notbounties.gui;

public class PlayerGUInfo {
    private final int page;
    private final Object[] data;

    public PlayerGUInfo(int page, Object[] data){

        this.page = page;
        this.data = data;
    }

    public int getPage() {
        return page;
    }

    public Object[] getData() {
        return data;
    }
}

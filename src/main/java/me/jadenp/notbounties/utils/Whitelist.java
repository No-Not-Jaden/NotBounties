package me.jadenp.notbounties.utils;

import java.util.List;
import java.util.UUID;

public class Whitelist {
    private List<UUID> list;
    private boolean blacklist;

    public Whitelist(List<UUID> list, boolean blacklist) {

        this.list = list;
        this.blacklist = blacklist;
    }

    public List<UUID> getList() {
        return list;
    }

    public boolean isBlacklist() {
        return blacklist;
    }

    public void setList(List<UUID> list) {
        this.list = list;
    }
    public boolean toggleBlacklist(){
        blacklist = !blacklist;
        return blacklist;
    }
}

package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;

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

    /**
     * Toggle the blacklist mode
     * @return the state of the new blacklist mode
     */
    public boolean toggleBlacklist(){
        blacklist = !blacklist;
        return blacklist;
    }

    /**
     * Set the blacklist mode
     * @param blacklist New blacklist mode
     * @return true if there was a change in mode
     */
    public boolean setBlacklist(boolean blacklist) {
        boolean change = this.blacklist != blacklist;
        this.blacklist = blacklist;
        return change;
    }

    @Override
    public String toString() {
        if (list.isEmpty()) {
            return "<X>";
        } else {
            StringBuilder builder = new StringBuilder();
            for (UUID uuid : list) {
                builder.append(NotBounties.getPlayerName(uuid)).append(" ");
            }
            builder.deleteCharAt(builder.length()-1);
            return builder.toString();
        }
    }
}

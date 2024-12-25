package me.jadenp.notbounties.utils;

import java.util.*;

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
                builder.append(LoggedPlayers.getPlayerName(uuid)).append(" ");
            }
            builder.deleteCharAt(builder.length()-1);
            return builder.toString();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Whitelist whitelist)
            return whitelist.blacklist == this.blacklist && new HashSet<>(this.getList()).containsAll(whitelist.getList()) && new HashSet<>(whitelist.getList()).containsAll(this.getList());
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(list, blacklist);
    }
}

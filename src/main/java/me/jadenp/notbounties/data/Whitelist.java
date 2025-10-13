package me.jadenp.notbounties.data;

import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class Whitelist {

    private static boolean enabled;
    private static double cost;
    private static boolean showWhitelistedBounties;
    private static boolean variableWhitelist;
    private static boolean defaultWhitelist;
    private static boolean allowTogglingWhitelist;

    public static void loadConfiguration(ConfigurationSection config) {
        enabled = config.getBoolean("enabled");
        cost = config.getDouble("cost");
        showWhitelistedBounties = config.getBoolean("show-all-bounty");
        variableWhitelist = config.getBoolean("variable-whitelist");
        allowTogglingWhitelist = config.getBoolean("allow-toggling-whitelist");
        defaultWhitelist = config.getBoolean("default-whitelist");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double getCost() {
        return cost;
    }

    public static boolean isAllowTogglingWhitelist() { // replace all is blacklist enabled stuff
        return allowTogglingWhitelist;
    }

    public static boolean isDefaultWhitelist() {
        return defaultWhitelist;
    }

    public static boolean isShowWhitelistedBounties() {
        return showWhitelistedBounties;
    }

    public static boolean isVariableWhitelist() {
        return variableWhitelist;
    }

    private SortedSet<UUID> list;
    private boolean blacklist;

    public Whitelist(SortedSet<UUID> list, boolean blacklist) {

        this.list = list;
        this.blacklist = blacklist;
    }

    public SortedSet<UUID> getList() {
        return list;
    }

    public boolean isBlacklist() {
        return blacklist;
    }

    public void setList(SortedSet<UUID> list) {
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

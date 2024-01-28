package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.ConfigOptions;
import me.jadenp.notbounties.utils.Whitelist;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class Setter implements Comparable<Setter>{
    private final String name;
    private final UUID uuid;
    private final double amount;
    private final long timeCreated;
    private boolean notified;
    private final Whitelist whitelist;

    public Setter(String name, UUID uuid, double amount, long timeCreated, @Nullable Boolean notified, Whitelist whitelist){

        this.name = name;
        this.uuid = uuid;
        this.amount = amount;
        this.timeCreated = timeCreated;
        if (notified == null){
            this.notified = true;
        } else {
            this.notified = notified;
        }
        this.whitelist = whitelist;
    }

    public boolean canClaim(Player player) {
        if (player == null)
            return true;
        return canClaim(player.getUniqueId());
    }

    public boolean canClaim(UUID claimerUuid) {
        if (whitelist.getList().isEmpty())
            return true;
        if (ConfigOptions.variableWhitelist)
            return (NotBounties.getPlayerWhitelist(uuid).getList().isEmpty() || NotBounties.getPlayerWhitelist(uuid).isBlacklist()) != NotBounties.getPlayerWhitelist(uuid).getList().contains(claimerUuid);
        return whitelist.isBlacklist() != whitelist.getList().contains(claimerUuid);
    }

    public boolean isNotified() {
        return notified;
    }

    public Whitelist getWhitelist() {
        return whitelist;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int compareTo(@NotNull Setter o) {
        if (this.getAmount() != o.getAmount())
            return (int) Math.signum(this.getAmount() - o.getAmount());
        return this.getUuid().compareTo(o.getUuid());
    }
}

package me.jadenp.notbounties;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class Setter implements Comparable<Setter>{
    private final String name;
    private final UUID uuid;
    private final double amount;
    private final long timeCreated;
    private boolean notified;
    private final List<UUID> whitelist;

    public Setter(String name, UUID uuid, double amount, long timeCreated, @Nullable Boolean notified, List<UUID> whitelist){

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
        return whitelist.isEmpty() || whitelist.contains(player.getUniqueId());
    }

    public boolean isNotified() {
        return notified;
    }

    public List<UUID> getWhitelist() {
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

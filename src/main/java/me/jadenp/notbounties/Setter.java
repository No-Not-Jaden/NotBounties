package me.jadenp.notbounties;

import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.util.UUID;

public class Setter {
    private final String name;
    private final String uuid;
    private final long amount;
    private final long timeCreated;
    private boolean notified;

    public Setter(String name, String uuid, long amount, long timeCreated, @Nullable Boolean notified){

        this.name = name;
        this.uuid = uuid;
        this.amount = amount;
        this.timeCreated = timeCreated;
        if (notified == null){
            this.notified = true;
        } else {
            this.notified = notified;
        }

    }

    public boolean isNotified() {
        return notified;
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

    public long getAmount() {
        return amount;
    }

    public String getUuid() {
        return uuid;
    }
}

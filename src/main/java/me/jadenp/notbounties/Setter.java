package me.jadenp.notbounties;

import javax.annotation.Nullable;

public class Setter {
    private final String name;
    private final String uuid;
    private final double amount;
    private final long timeCreated;
    private boolean notified;

    public Setter(String name, String uuid, double amount, long timeCreated, @Nullable Boolean notified){

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

    public double getAmount() {
        return amount;
    }

    public String getUuid() {
        return uuid;
    }
}

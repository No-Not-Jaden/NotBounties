package me.jadenp.notbounties;

public class Setter {
    private final String name;
    private final String uuid;
    private final int amount;
    private final long timeCreated;

    public Setter(String name, String uuid, int amount, long timeCreated){

        this.name = name;
        this.uuid = uuid;
        this.amount = amount;
        this.timeCreated = timeCreated;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }

    public String getUuid() {
        return uuid;
    }
}

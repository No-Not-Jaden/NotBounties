package me.jadenp.notbounties;

import java.util.UUID;

public class PVPHistory {
    private final UUID attacker;
    private long lastHit;
    public PVPHistory(UUID attacker) {
        this.attacker = attacker;
        setLastHit();
    }

    public void setLastHit(){
        lastHit = System.currentTimeMillis();
    }

    public long getLastHit() {
        return lastHit;
    }

    public UUID getAttacker() {
        return attacker;
    }
}

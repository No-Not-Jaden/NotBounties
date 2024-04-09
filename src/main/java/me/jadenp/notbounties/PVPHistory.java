package me.jadenp.notbounties;

import java.util.UUID;

public class PVPHistory {
    private UUID attacker;
    private long lastHit;
    private boolean combatSafe;
    public PVPHistory(UUID attacker) {
        combatSafe = false;
        setLastHit(attacker);
    }

    public void setLastHit(UUID attacker){
        this.attacker = attacker;
        lastHit = System.currentTimeMillis();
        combatSafe = false;
    }

    public void setCombatSafe(boolean combatSafe) {
        this.combatSafe = combatSafe;
    }

    public boolean isCombatSafe() {
        return combatSafe;
    }

    public long getLastHit() {
        return lastHit;
    }

    public UUID getAttacker() {
        return attacker;
    }
}

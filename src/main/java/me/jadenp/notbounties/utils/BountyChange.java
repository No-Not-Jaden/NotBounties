package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.Bounty;

/**
 * A change in a bounty that occurred while a database was not connected
 */
public record BountyChange(me.jadenp.notbounties.utils.BountyChange.ChangeType changeType, Bounty change) {
    public enum ChangeType {
        ADD_BOUNTY, // change is a bounty
        DELETE_BOUNTY, // change is a bounty
        REPLACE_BOUNTY, // change is the new bounty
        NOTIFY // change is the bounty that has been notified
    }

}



package me.jadenp.notbounties.utils;

/**
 * A change in a bounty that occurred while a database was not connected
 */
public record BountyChange(me.jadenp.notbounties.utils.BountyChange.ChangeType changeType, Object change) {
    public enum ChangeType {
        ADD_BOUNTY, // change is a bounty
        DELETE_BOUNTY, // change is a bounty
        EDIT_BOUNTY, // change is a bounty with the first setter being the setter to edit, and the second setter being the console with the change to the first setter
        NOTIFY // change is a uuid array with index 0= bounty uuid 1= setter uuid
    }

}



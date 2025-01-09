package me.jadenp.notbounties.bounty_events;


import me.jadenp.notbounties.data.Bounty;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BountySetEvent extends Event implements Cancellable {

    private final Bounty bounty;
    private boolean canceled = false;
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public BountySetEvent(Bounty bounty) {
        this.bounty = bounty;
    }

    public Bounty getBounty() {
        return bounty;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean b) {
        canceled = b;
    }

}

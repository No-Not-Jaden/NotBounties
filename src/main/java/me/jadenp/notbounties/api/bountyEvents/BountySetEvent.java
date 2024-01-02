package me.jadenp.notbounties.api.bountyEvents;


import me.jadenp.notbounties.Bounty;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BountySetEvent extends Event implements Cancellable {

    private final Bounty bounty;
    private boolean canceled = false;

    public BountySetEvent(Bounty bounty) {
        this.bounty = bounty;
    }

    public Bounty getBounty() {
        return bounty;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return new HandlerList();
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

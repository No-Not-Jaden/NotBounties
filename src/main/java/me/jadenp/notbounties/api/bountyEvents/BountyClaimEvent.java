package me.jadenp.notbounties.api.bountyEvents;


import me.jadenp.notbounties.Bounty;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BountyClaimEvent extends Event implements Cancellable {
    private final Player killer;
    private final Bounty bounty;
    private boolean canceled = false;

    public BountyClaimEvent(Player killer, Bounty bounty) {

        this.killer = killer;
        this.bounty = bounty;
    }

    public Bounty getBounty() {
        return bounty;
    }

    public Player getKiller() {
        return killer;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean b) {
        canceled = b;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return new HandlerList();
    }
}

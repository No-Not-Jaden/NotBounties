package me.jadenp.notbounties.bountyEvents;

import me.jadenp.notbounties.Bounty;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BountyRemoveEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final CommandSender remover;
    private final boolean bought;
    private final Bounty bounty;
    private boolean canceled = false;

    public BountyRemoveEvent(CommandSender remover, boolean bought, Bounty bounty) {

        this.remover = remover;
        this.bought = bought;
        this.bounty = bounty;
    }

    public Bounty getBounty() {
        return bounty;
    }

    public CommandSender getRemover() {
        return remover;
    }

    public boolean isBought() {
        return bought;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
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

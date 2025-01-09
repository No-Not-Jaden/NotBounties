package me.jadenp.notbounties.bounty_events;


import me.jadenp.notbounties.data.Bounty;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BountyEditEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final CommandSender editor;
    private final Bounty beforeEdit;
    private final Bounty afterEdit;
    private boolean canceled = false;

    public BountyEditEvent(CommandSender editor, Bounty beforeEdit, Bounty afterEdit){

        this.editor = editor;
        this.beforeEdit = beforeEdit;
        this.afterEdit = afterEdit;
    }

    public Bounty getAfterEdit() {
        return afterEdit;
    }

    public Bounty getBeforeEdit() {
        return beforeEdit;
    }

    public CommandSender getEditor() {
        return editor;
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

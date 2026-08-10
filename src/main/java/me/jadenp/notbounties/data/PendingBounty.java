package me.jadenp.notbounties.data;

import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PendingBounty {
    private final UUID setterUUID;
    private final String setName;
    private final UUID receiverUUID;
    private final String receiverName;
    private final double amount;
    private final List<ItemStack> items;
    private final Whitelist whitelist;
    private final long setTime;
    private final long delaySeconds;

    public PendingBounty(UUID setterUUID, String setName, UUID receiverUUID, String receiverName, double amount, List<ItemStack> items, Whitelist whitelist, long setTime, long delaySeconds) {
        this.setterUUID = setterUUID;
        this.setName = setName;
        this.receiverUUID = receiverUUID;
        this.receiverName = receiverName;
        this.amount = amount;
        this.items = items;
        this.whitelist = whitelist;
        this.setTime = setTime;
        this.delaySeconds = delaySeconds;
    }

    public boolean isReady() {
        return System.currentTimeMillis() - setTime >= delaySeconds * 1000;
    }

    public UUID getSetterUUID() { return setterUUID; }
    public String getSetterName() { return setName; }
    public UUID getReceiverUUID() { return receiverUUID; }
    public String getReceiverName() { return receiverName; }
    public double getAmount() { return amount; }
    public List<ItemStack> getItems() { return items; }
    public Whitelist getWhitelist() { return whitelist; }
    public long getSetTime() { return setTime; }
    public long getDelaySeconds() { return delaySeconds; }
    public long getRemainingMillis() { return (setTime + delaySeconds * 1000) - System.currentTimeMillis(); }
}

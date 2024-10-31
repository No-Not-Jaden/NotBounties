package me.jadenp.notbounties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.jadenp.notbounties.utils.Inconsistent;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.Whitelist;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Setter implements Comparable<Setter>, Inconsistent {
    private final String name;
    private final UUID uuid;
    private final double amount;
    private final double displayBounty;
    private final List<ItemStack> items;
    private final long timeCreated;
    private boolean notified;
    private final Whitelist whitelist;
    private final long receiverPlaytime;
    private static final Gson gson;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Bounty.class, new BountyTypeAdapter());
        builder.registerTypeAdapter(Setter.class, new SetterTypeAdapter());
        gson = builder.create();
    }

    public Setter(String name, UUID uuid, double amount, List<ItemStack> items, long timeCreated, @Nullable Boolean notified, Whitelist whitelist, long receiverPlaytime){

        this.name = name;
        this.uuid = uuid;
        this.amount = amount;
        this.items = items;
        this.timeCreated = timeCreated;
        this.notified = Objects.requireNonNullElse(notified, true);
        this.whitelist = whitelist;
        this.receiverPlaytime = receiverPlaytime;
        displayBounty = amount + NumberFormatting.getTotalValue(items);
    }

    public Setter(String name, UUID uuid, double amount, List<ItemStack> items, long timeCreated, @Nullable Boolean notified, Whitelist whitelist, long receiverPlaytime, double displayBounty){

        this.name = name;
        this.uuid = uuid;
        this.amount = amount;
        this.items = items;
        this.timeCreated = timeCreated;
        this.notified = Objects.requireNonNullElse(notified, true);
        this.whitelist = new Whitelist(whitelist.getList(), whitelist.isBlacklist());
        this.receiverPlaytime = receiverPlaytime;
        if (displayBounty == -1) {
            this.displayBounty = amount + NumberFormatting.getTotalValue(items);
        } else {
            this.displayBounty = displayBounty;
        }

    }

    public JsonObject toJson(){
        return (JsonObject) gson.toJsonTree(this);
    }

    public long getReceiverPlaytime(){
        return receiverPlaytime;
    }

    public boolean canClaim(Player player) {
        if (player == null)
            return true;
        return canClaim(player.getUniqueId());
    }

    public boolean canClaim(UUID claimerUuid) {
        if (whitelist.getList().isEmpty() || !ConfigOptions.bountyWhitelistEnabled)
            return true;
        if (ConfigOptions.variableWhitelist)
            return (NotBounties.getPlayerWhitelist(uuid).getList().isEmpty() || NotBounties.getPlayerWhitelist(uuid).isBlacklist()) != NotBounties.getPlayerWhitelist(uuid).getList().contains(claimerUuid);
        return whitelist.isBlacklist() != whitelist.getList().contains(claimerUuid);
    }

    public boolean isNotified() {
        return notified;
    }

    public Whitelist getWhitelist() {
        if (ConfigOptions.variableWhitelist) {
            return NotBounties.getPlayerWhitelist(uuid);
        }
        return whitelist;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }
    public double getDisplayAmount() {
        return displayBounty;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int compareTo(@NotNull Setter o) {
        if (this.getAmount() != o.getAmount())
            return (int) Math.signum(this.displayBounty - o.displayBounty);
        if (this.getTimeCreated() != o.getTimeCreated())
            return (int) (this.timeCreated - o.timeCreated);
        return this.getUuid().compareTo(o.getUuid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Setter setter = (Setter) o;
        return Double.compare(amount, setter.amount) == 0 && timeCreated == setter.timeCreated && notified == setter.notified && receiverPlaytime == setter.receiverPlaytime && Objects.equals(uuid, setter.uuid) && Objects.equals(items, setter.items) && Objects.equals(whitelist, setter.whitelist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, amount, items, timeCreated, notified, whitelist, receiverPlaytime);
    }

    @Override
    public String getID() {
        return uuid.toString() + ":" + timeCreated;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Inconsistent> T copy() {
        return (T) new Setter(name, uuid, amount, items, timeCreated, notified, whitelist, receiverPlaytime, displayBounty);
    }

    @Override
    public long getLatestUpdate() {
        return getTimeCreated();
    }

    @Override
    public List<Inconsistent> getSubElements() {
        return List.of();
    }

    @Override
    public void setSubElements(List<Inconsistent> subElements) {
        // do nothing because a setter doesn't contain any important inconsistent sub elements
    }
}

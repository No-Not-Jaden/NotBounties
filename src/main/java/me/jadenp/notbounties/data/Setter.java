package me.jadenp.notbounties.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.money.ExcludedItemException;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Setter implements Comparable<Setter> {
    private Integer id;
    private final UUID uuid;
    private final double amount;
    private final double displayBounty;
    private final long timeCreated;
    private boolean notified;
    private final Whitelist whitelist;
    private final long receiverPlaytime;
    private final Set<String> tags = new TreeSet<>();
    private List<ItemStack> items;
    private static final Gson gson;
    private final boolean hasItems;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Bounty.class, new BountyTypeAdapter());
        builder.registerTypeAdapter(Setter.class, new SetterTypeAdapter());
        gson = builder.create();
    }

    public Setter(Integer id, UUID uuid, double amount, long timeCreated, boolean hasItems, @Nullable Boolean notified, Whitelist whitelist, long receiverPlaytime, double displayBounty, Set<String> tags){

        this.id = id;
        this.uuid = uuid;
        this.amount = amount;
        this.timeCreated = timeCreated;
        this.notified = Objects.requireNonNullElse(notified, true);
        this.whitelist = new Whitelist(whitelist.getList(), whitelist.isBlacklist());
        this.receiverPlaytime = receiverPlaytime;
        this.tags.addAll(tags);
        this.hasItems = hasItems;
        if (displayBounty == -1) {
            throw new UnsupportedOperationException("Display bounty has to be set");
        } else {
            this.displayBounty = displayBounty;
        }

    }

    public Setter(Integer id, UUID uuid, double amount, long timeCreated, List<ItemStack> items, @Nullable Boolean notified, Whitelist whitelist, long receiverPlaytime, double displayBounty, Set<String> tags){

        this.id = id;
        this.uuid = uuid;
        this.amount = amount;
        this.timeCreated = timeCreated;
        this.notified = Objects.requireNonNullElse(notified, true);
        this.whitelist = new Whitelist(whitelist.getList(), whitelist.isBlacklist());
        this.receiverPlaytime = receiverPlaytime;
        this.tags.addAll(tags);
        this.hasItems = items != null && !items.isEmpty();
        this.items = items;
        if (displayBounty == -1) {
            this.displayBounty = amount + NumberFormatting.getTotalValue(items);
        } else {
            this.displayBounty = displayBounty;
        }

    }

    public Setter(Setter setter) {
        this.id = setter.id;
        this.uuid = setter.uuid;
        this.amount = setter.amount;
        this.timeCreated = setter.timeCreated;
        this.notified = setter.notified;
        this.whitelist = new Whitelist(setter.whitelist.getList(), setter.whitelist.isBlacklist());
        this.receiverPlaytime = setter.receiverPlaytime;
        this.tags.addAll(setter.tags);
        this.hasItems = setter.items != null && !setter.items.isEmpty();
        this.items = setter.items;
        this.displayBounty = setter.displayBounty;
    }

    public Setter(Setter setter, double change) {
        this.id = setter.id;
        this.uuid = setter.uuid;
        this.amount = setter.amount + change;
        this.timeCreated = setter.timeCreated;
        this.notified = setter.notified;
        this.whitelist = new Whitelist(setter.whitelist.getList(), setter.whitelist.isBlacklist());
        this.receiverPlaytime = setter.receiverPlaytime;
        this.tags.addAll(setter.tags);
        this.hasItems = setter.items != null && !setter.items.isEmpty();
        this.items = setter.items;
        this.displayBounty = setter.displayBounty + change;
    }

    public JsonObject toJson(){
        return (JsonObject) gson.toJsonTree(this);
    }

    public long getReceiverPlaytime(){
        return receiverPlaytime;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public void addTag(String tag) {
        if (tag.length() > 256)
            tag = tag.substring(0, 256); // database limit
        tags.add(tag);
    }

    public boolean canClaim(Player player) {
        if (player == null)
            return true;
        return canClaim(player.getUniqueId());
    }

    public boolean canClaim(@NotNull UUID claimerUuid) {
        // check if the claimer set this bounty
        if (!ConfigOptions.isSetterClaimOwn() && claimerUuid.equals(uuid))
            return false;
        // check the whitelist
        Whitelist applicableWhitelist = getWhitelist();
        if (applicableWhitelist.getList().isEmpty())
            return true;
        return applicableWhitelist.isBlacklist() != applicableWhitelist.getList().contains(claimerUuid);
    }

    public boolean isNotified() {
        return notified;
    }

    public Whitelist getWhitelist() {
        if (Whitelist.isVariableWhitelist()) {
            return DataManager.getPlayerData(uuid).getWhitelist();
        }
        return whitelist;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public double getAmount() {
        return amount;
    }

    public double getDisplayAmount() {
        return displayBounty;
    }

    public String getName() {
        return LoggedPlayers.getPlayerName(uuid);
    }

    /**
     * Gets the items for this setter.
     * Calling this may load them from the database.
     * @return A list of items on this bounty.
     */
    public CompletableFuture<List<ItemStack>> getItems() {
        if ((items == null || items.isEmpty()) && hasItems && id != null) {
            CompletableFuture<List<ItemStack>> loadingItems =
                    ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getBountyItemsAsync(id);
            loadingItems.thenAccept(bountyItems -> items = bountyItems);
            return loadingItems;
        }
        return CompletableFuture.completedFuture(items);
    }

    public boolean isItemsLoaded() {
        return items != null && !items.isEmpty();
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    /**
     * Check if the setter has items.
     * @return True if this setter owns items.
     */
    public boolean hasItems() {
        return items != null;
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
    public String toString() {
        return "Setter{" +
                ", uuid=" + uuid +
                ", amount=" + amount +
                ", displayBounty=" + displayBounty +
                ", items=" + items +
                ", timeCreated=" + timeCreated +
                ", notified=" + notified +
                ", whitelist=" + whitelist +
                ", receiverPlaytime=" + receiverPlaytime +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uuid, amount, items, timeCreated, notified, whitelist, receiverPlaytime);
    }

    public Optional<Integer> getBountyId() {
        return Optional.ofNullable(id);
    }

    public long getLatestUpdate() {
        return getTimeCreated();
    }

}

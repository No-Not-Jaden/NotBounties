package me.jadenp.notbounties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.utils.configuration.BountyExpire;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Bounty implements Comparable<Bounty>{
    private final UUID uuid;
    private String name;
    private List<Setter> setters = new ArrayList<>();
    private static final Gson gson;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Bounty.class, new BountyTypeAdapter());
        builder.registerTypeAdapter(Setter.class, new SetterTypeAdapter());
        gson = builder.create();
    }

    public Bounty(Player setter, OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist){
        // save player
        this.uuid = receiver.getUniqueId();
        name = NotBounties.getPlayerName(receiver.getUniqueId());
        // add to the total bounty
        setters.add(new Setter(setter.getName(), setter.getUniqueId(), amount, items, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
    }

    public Bounty(OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist){
        // save player
        this.uuid = receiver.getUniqueId();
        name = NotBounties.getPlayerName(receiver.getUniqueId());
        // add to the total bounty
        setters.add(new Setter(ConfigOptions.consoleName, new UUID(0,0), amount, items, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
    }

    public Bounty(Bounty bounty) {
        uuid = bounty.getUUID();
        name = bounty.getName();
        for (Setter setter : bounty.getSetters()) {
            setters.add(new Setter(setter.getName(), setter.getUuid(), setter.getAmount(), setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime()));
        }
    }

    public Bounty(Bounty bounty, UUID claimer) {
        uuid = bounty.getUUID();
        name = bounty.getName();
        for (Setter setter : bounty.getSetters()) {
            if (setter.canClaim(claimer))
                setters.add(new Setter(setter.getName(), setter.getUuid(), setter.getAmount(), setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime()));
        }
    }

    public Bounty(UUID uuid, List<Setter> setters, String name){
        this.uuid = uuid;
        this.setters = setters;
        this.name = name;
    }

    public Bounty(JsonObject jsonObject) {
        this(gson.fromJson(jsonObject, Bounty.class));
    }

    public JsonObject toJson(){
        return (JsonObject) gson.toJsonTree(this);
    }

    /**
     * Change the display name of the target
     * The name is reset every time the target logs in
     * @param name the new name of the target
     */
    public void setDisplayName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void addBounty(Setter setter){
        // add a new setter
        setters.add(new Setter(setter.getName(), setter.getUuid(), setter.getAmount(), setter.getItems(), System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, setter.getWhitelist(), BountyExpire.getTimePlayed(uuid), setter.getDisplayAmount()));
    }

    // console set bounty
    public void addBounty(double amount, List<ItemStack> items, Whitelist whitelist){
        // add a new setter
        setters.add(new Setter(ConfigOptions.consoleName, new UUID(0,0), amount, items, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist, BountyExpire.getTimePlayed(uuid)));
    }


    /**
     * Removes the setters that correspond to the uuid.
     * @param uuid UUID of the setters to remove
     */
    public void removeBounty(UUID uuid){
        Iterator<Setter> settersIterator = setters.listIterator();
        while (settersIterator.hasNext()){
            Setter setter = settersIterator.next();
            if (setter.getUuid().equals(uuid)){
                settersIterator.remove();
                return;
            }
        }
    }

    /**
     * Returns an edited version of the bounty where a specific setter amount is changed. Editing a bounty this way will not edit it in a database.
     * @param uuid UUID of the setter to edit
     * @param change change for the setter
     */
    public void editBounty(UUID uuid, double change){
        for (int i = 0; i < setters.size(); i++){
            if (setters.get(i).getUuid().equals(uuid)){
                // same person
                double adjustedAmount = setters.get(i).getAmount() + change; // adjust the amount to compensate for bounty items
                setters.set(i, new Setter(setters.get(i).getName(), setters.get(i).getUuid(), adjustedAmount, setters.get(i).getItems(), setters.get(i).getTimeCreated(), setters.get(i).isNotified(), setters.get(i).getWhitelist(), setters.get(i).getReceiverPlaytime()));
                return;
            }
        }
        setters.add(new Setter(NotBounties.getPlayerName(uuid), uuid, change, new ArrayList<>(), System.currentTimeMillis(), false, new Whitelist(new ArrayList<>(), false), BountyExpire.getTimePlayed(uuid)));
    }

    /**
     * Get the latest time that a bounty was set
     * @return The time in milliseconds
     */
    public long getLatestSetter() {
        return setters.stream().mapToLong(Setter::getTimeCreated).filter(setter -> setter >= 0).max().orElse(0);
    }

    public Setter getLastSetter() {
        long latest = getLatestSetter();
        for (Setter setter : setters) {
            if (setter.getTimeCreated() == latest)
                return setter;
        }
        return setters.get(setters.size()-1);
    }


    public double getTotalBounty(){
        return setters.stream().mapToDouble(Setter::getAmount).sum();
    }
    public List<ItemStack> getTotalItemBounty() {
        return setters.stream().flatMap(setter -> setter.getItems().stream()).toList();
    }

    public double getTotalDisplayBounty() {
        return setters.stream().mapToDouble(Setter::getDisplayAmount).sum();
    }

    /**
     * Get the total bounty that is accessible by the player
     * @param claimer Player to get the total claimable bounty value
     * @return The total claimable bounty value for the player
     */
    public double getTotalBounty(Player claimer) { return getTotalBounty(claimer.getUniqueId());}
    public List<ItemStack> getTotalItemBounty(Player claimer) { return getTotalItemBounty(claimer.getUniqueId());}
    public double getTotalDisplayBounty(Player claimer) { return getTotalDisplayBounty(claimer.getUniqueId());}

    public double getTotalBounty(UUID claimerUuid) {
        if (claimerUuid.equals(uuid))
            return getTotalBounty();
        return setters.stream().filter(setters1 -> setters1.canClaim(claimerUuid)).mapToDouble(Setter::getAmount).sum();
    }

    public List<ItemStack> getTotalItemBounty(UUID claimerUuid) {
        if (claimerUuid.equals(uuid))
            return getTotalItemBounty();
        return setters.stream().filter(setters1 -> setters1.canClaim(claimerUuid)).flatMap(setters1 -> setters1.getItems().stream()).toList();
    }

    public double getTotalDisplayBounty(UUID claimerUuid) {
        if (claimerUuid.equals(uuid))
            return getTotalDisplayBounty();
        return  getTotalBounty(claimerUuid) + NumberFormatting.getTotalValue(getTotalItemBounty(claimerUuid));
    }

    public String getFormattedTotalBounty() {
        return NumberFormatting.formatNumber(getTotalBounty());
    }

    /**
     * Removes all the setters that have the player whitelisted
     * @param claimer The player to claim the bounty
     */
    public void claimBounty(Player claimer){
        setters.removeIf(setter -> setter.canClaim(claimer));
    }

    public List<Setter> getSetters() {
        return setters;
    }

    public void combineSetters(){
        Map<UUID, Setter> setterMap = new HashMap<>();
        for (Setter setter : setters) {

            if (setterMap.containsKey(setter.getUuid()) && compareWhitelists(setterMap.get(setter.getUuid()).getWhitelist(), setter.getWhitelist())) {
                // setter already exists
                Setter firstSetter = setterMap.get(setter.getUuid());
                // combine the items
                List<ItemStack> items = setter.getItems();
                items.addAll(firstSetter.getItems());
                // replace setter in map with combined values
                setterMap.replace(setter.getUuid(), new Setter(setter.getName(), setter.getUuid(), setter.getAmount() + firstSetter.getAmount(), items, Math.max(setter.getTimeCreated(), firstSetter.getTimeCreated()), setter.isNotified() && firstSetter.isNotified(), setter.getWhitelist(), firstSetter.getReceiverPlaytime()));
            } else {
                setterMap.put(setter.getUuid(), setter);
            }
        }
        setters = new ArrayList<>(setterMap.values());
    }

    private boolean compareWhitelists(Whitelist w1, Whitelist w2) {
        if ((w1 == null || w1.getList().isEmpty()) && (w2 == null || w2.getList().isEmpty()))
            return true;
        if (w1 == null || w2 == null)
            return false;
        if (w1.getList().size() != w2.getList().size())
            return false;
        if (w1.isBlacklist() != w2.isBlacklist())
            return false;
        for (UUID uuid : w1.getList()) {
            if (!w2.getList().contains(uuid))
                return false;
        }
        return true;
    }

    public Map<UUID, List<UUID>> getWhitelists(){
        //combineSetters();
        return setters.stream().filter(setter -> !setter.getWhitelist().getList().isEmpty()).collect(Collectors.toMap(Setter::getUuid, setter -> setter.getWhitelist().getList(), (a, b) -> b));
    }

    public List<UUID> getAllWhitelists() {
        if (ConfigOptions.variableWhitelist) {
            return setters.stream().map(setter -> NotBounties.getPlayerWhitelist(setter.getUuid())).filter(whitelist -> !whitelist.isBlacklist()).flatMap(whitelist -> whitelist.getList().stream()).toList();
        }
        return setters.stream().map(Setter::getWhitelist).filter(whitelist -> !whitelist.isBlacklist()).flatMap(whitelist -> whitelist.getList().stream()).toList();
    }
    public List<UUID> getAllBlacklists() {
        if (ConfigOptions.variableWhitelist) {
            return setters.stream().map(setter -> NotBounties.getPlayerWhitelist(setter.getUuid())).filter(Whitelist::isBlacklist).flatMap(whitelist -> whitelist.getList().stream()).toList();
        }
        return setters.stream().map(Setter::getWhitelist).filter(Whitelist::isBlacklist).flatMap(whitelist -> whitelist.getList().stream()).toList();
    }

    /**
     * Get a bounty object with only the setters that match the setterUUID parameter
     * @param setterUUID UUID of the desired setter
     * @return A bounty with either no setters or setters that match the setterUUID
     */
    public Bounty getBounty(UUID setterUUID) {
        List<Setter> matchingSetters = new ArrayList<>();
        for (Setter setter : setters) {
            if (setter.getUuid().equals(setterUUID))
                matchingSetters.add(setter);
        }
        return new Bounty(uuid, matchingSetters, name);
    }


    @Override
    public String toString() {
        return "Bounty{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", setters=" + setters +
                '}';
    }

    @Override
    public int compareTo(@NotNull Bounty o) {
        if (this.getTotalDisplayBounty() != o.getTotalDisplayBounty())
            return Double.compare(this.getTotalDisplayBounty(), o.getTotalDisplayBounty());
        if (this.getUUID() != o.getUUID())
            return this.getUUID().compareTo(o.getUUID());
        if (this.getSetters().size() != o.getSetters().size())
            return Integer.compare(this.getSetters().size(), o.getSetters().size());
        for (int i = 0; i < this.getSetters().size(); i++) {
            Setter thisSetter = this.getSetters().get(i);
            Setter otherSetter = o.getSetters().get(i);
            int compared = thisSetter.compareTo(otherSetter);
            if (compared != 0)
                return compared;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bounty bounty) {
            if (this.getTotalDisplayBounty() != bounty.getTotalDisplayBounty() || this.getUUID() != bounty.getUUID() || this.getSetters().size() != bounty.getSetters().size())
                return false;
            for (int i = 0; i < this.getSetters().size(); i++) {
                Setter thisSetter = this.getSetters().get(i);
                Setter otherSetter = bounty.getSetters().get(i);
                if (!thisSetter.equals(otherSetter))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, setters);
    }
}

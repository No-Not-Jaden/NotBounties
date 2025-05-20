package me.jadenp.notbounties.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.features.BountyExpire;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class Bounty implements Comparable<Bounty>, Inconsistent{
    private final UUID uuid;
    private String name;
    private List<Setter> setters = Collections.synchronizedList(new LinkedList<>());
    private static final Gson gson;
    private UUID serverID; // id used to identify which server should save the value
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Bounty.class, new BountyTypeAdapter());
        builder.registerTypeAdapter(Setter.class, new SetterTypeAdapter());
        gson = builder.create();
    }

    public Bounty(Player setter, OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist, UUID serverID){
        // save player
        this.uuid = receiver.getUniqueId();
        name = LoggedPlayers.getPlayerName(receiver.getUniqueId());
        // add to the total bounty
        setters.add(new Setter(setter.getName(), setter.getUniqueId(), amount, items, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
        this.serverID = serverID;
    }

    public Bounty(Player setter, OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        this(setter, receiver, amount, items, whitelist, DataManager.getDatabaseServerID(true));
    }

    public Bounty(OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist, UUID serverID){
        // save player
        this.uuid = receiver.getUniqueId();
        name = LoggedPlayers.getPlayerName(receiver.getUniqueId());
        // add to the total bounty
        setters.add(new Setter(ConfigOptions.getAutoBounties().getConsoleBountyName(), DataManager.GLOBAL_SERVER_ID, amount, items, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
        this.serverID = serverID;
    }

    public Bounty(OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        this(receiver, amount, items, whitelist, DataManager.getDatabaseServerID(true));
    }

    public Bounty(Bounty bounty) {
        uuid = bounty.getUUID();
        name = bounty.getName();
        for (Setter setter : bounty.getSetters()) {
            setters.add(new Setter(setter.getName(), setter.getUuid(), setter.getAmount(), new ArrayList<>(setter.getItems()), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount()));
        }
        serverID = bounty.serverID;
    }

    public Bounty(Bounty bounty, UUID claimer) {
        uuid = bounty.getUUID();
        name = bounty.getName();
        for (Setter setter : bounty.getSetters()) {
            if (setter.canClaim(claimer))
                setters.add(new Setter(setter.getName(), setter.getUuid(), setter.getAmount(), setter.getItems(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime()));
        }
        serverID = bounty.serverID;
    }

    public Bounty(UUID uuid, List<Setter> setters, String name, UUID serverID){
        this.uuid = uuid;
        this.setters = Collections.synchronizedList(setters);
        this.name = name;
        this.serverID = serverID;
    }

    public Bounty(UUID uuid, List<Setter> setters, String name) {
        this(uuid, setters, name, DataManager.getDatabaseServerID(true));
    }

    public Bounty(String jsonString) {
        this(gson.fromJson(jsonString, Bounty.class));
    }

    public Bounty(JsonReader jsonReader) throws IOException {
        this(new BountyTypeAdapter().read(jsonReader));
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
        setters.add(new Setter(setter.getName(), setter.getUuid(), setter.getAmount(), setter.getItems(), setter.getTimeCreated(), setter.isNotified() || Bukkit.getPlayer(uuid) != null, setter.getWhitelist(), BountyExpire.getTimePlayed(uuid), setter.getDisplayAmount()));
    }

    public void notifyBounty() {
        for (Setter setter : setters) {
            setter.setNotified(true);
        }
    }

    // console set bounty
    public void addBounty(double amount, List<ItemStack> items, Whitelist whitelist){
        // add a new setter
        setters.add(new Setter(ConfigOptions.getAutoBounties().getConsoleBountyName(), DataManager.GLOBAL_SERVER_ID, amount, items, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist, BountyExpire.getTimePlayed(uuid)));
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
        setters.add(new Setter(LoggedPlayers.getPlayerName(uuid), uuid, change, new ArrayList<>(), System.currentTimeMillis(), false, new Whitelist(new ArrayList<>(), false), BountyExpire.getTimePlayed(uuid)));
    }

    /**
     * Get the latest time that a bounty was set
     * @return The time in milliseconds
     */
    public long getLatestUpdate() {
        return setters.stream().mapToLong(Setter::getTimeCreated).filter(setter -> setter >= 0).max().orElse(0);
    }

    @Override
    public List<Inconsistent> getSubElements() {
        return new ArrayList<>(setters);
    }

    @Override
    public void setSubElements(List<Inconsistent> subElements) {
        setters.clear();
        subElements.stream().filter(Setter.class::isInstance).map(Setter.class::cast).forEach(setter -> setters.add(setter));
    }

    public Setter getLastSetter() {
        if (setters.isEmpty()) {
            DataManager.getLocalData().removeBounty(uuid);
            return null;
        }
        long latest = setters.get(0).getTimeCreated();
        Setter latestSetter = setters.get(0);
        for (int i = 1; i < setters.size(); i++) {
            if (setters.get(i).getTimeCreated() > latest) {
                latest = setters.get(i).getTimeCreated();
                latestSetter = setters.get(i);
            }
        }
        return latestSetter;
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

    public List<Setter> getSetters() {
        return setters;
    }

    public List<UUID> getAllWhitelists() {
        if (Whitelist.isVariableWhitelist()) {
            return setters.stream().map(setter -> DataManager.getPlayerData(setter.getUuid()).getWhitelist()).filter(whitelist -> !whitelist.isBlacklist()).flatMap(whitelist -> whitelist.getList().stream()).toList();
        }
        return setters.stream().map(Setter::getWhitelist).filter(whitelist -> !whitelist.isBlacklist()).flatMap(whitelist -> whitelist.getList().stream()).toList();
    }
    public List<UUID> getAllBlacklists() {
        if (Whitelist.isVariableWhitelist()) {
            return setters.stream().map(setter -> DataManager.getPlayerData(setter.getUuid()).getWhitelist()).filter(Whitelist::isBlacklist).flatMap(whitelist -> whitelist.getList().stream()).toList();
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

    public UUID getServerID() {
        if (serverID == null) {
            NotBounties.debugMessage("No server ID set for bounty!", true);
            serverID = DataManager.getDatabaseServerID(true);
        }
        return serverID;
    }
    
    public void setServerID(UUID serverID) {
        this.serverID = serverID;
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

    @Override
    public String getID() {
        return uuid.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Inconsistent> T copy() {
        return (T) new Bounty(this);
    }
}

package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.utils.configuration.BountyExpire;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Bounty implements Comparable<Bounty>{
    private final UUID uuid;
    private String name;
    private List<Setter> setters = new ArrayList<>();


    public Bounty(Player setter, OfflinePlayer receiver, double amount, Whitelist whitelist){
        // save player
        this.uuid = receiver.getUniqueId();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter(setter.getName(), setter.getUniqueId(), amount, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
    }

    public Bounty(OfflinePlayer receiver, double amount, Whitelist whitelist){
        // save player
        this.uuid = receiver.getUniqueId();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter(ConfigOptions.consoleName, new UUID(0,0), amount, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
    }

    public Bounty(Bounty bounty) {
        uuid = bounty.getUUID();
        name = bounty.getName();
        for (Setter setter : bounty.getSetters()) {
            setters.add(new Setter(setter.getName(), setter.getUuid(), setter.getAmount(), setter.getTimeCreated(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime()));
        }
    }

    public Bounty(UUID uuid, List<Setter> setters, String name){
        this.uuid = uuid;
        this.setters = setters;
        this.name = name;
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

    public void addBounty(Player setter, double amount, Whitelist whitelist){
        // first check if player already set a bounty
        if (Bukkit.getPlayer(uuid) != null) {
            for (int i = 0; i < setters.size(); i++) {
                if (setters.get(i).getUuid().equals(setter.getUniqueId()) && compareWhitelists(whitelist, setters.get(i).getWhitelist())) {
                    // same person
                    setters.set(i, new Setter(setter.getName(), setter.getUniqueId(), setters.get(i).getAmount() + amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist, BountyExpire.getTimePlayed(uuid)));
                    return;
                }
            }
        }
        // otherwise, add a new setter
        setters.add(new Setter(setter.getName(), setter.getUniqueId(), amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist, BountyExpire.getTimePlayed(uuid)));
    }

    // console set bounty
    public void addBounty(double amount, Whitelist whitelist){
        // first check if player already set a bounty
        for (int i = 0; i < setters.size(); i++){
            if (setters.get(i).getUuid().equals(new UUID(0,0)) && compareWhitelists(whitelist, setters.get(i).getWhitelist())){
                // same person
                setters.set(i, new Setter(ConfigOptions.consoleName, new UUID(0,0), setters.get(i).getAmount() + amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist, BountyExpire.getTimePlayed(uuid)));
                return;
            }
        }
        // otherwise, add a new setter
        setters.add(new Setter(ConfigOptions.consoleName, new UUID(0,0), amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist, BountyExpire.getTimePlayed(uuid)));
    }

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

    public void editBounty(UUID uuid, double newAmount){
        for (int i = 0; i < setters.size(); i++){
            if (setters.get(i).getUuid().equals(uuid)){
                // same person
                setters.set(i, new Setter(setters.get(i).getName(), setters.get(i).getUuid(), newAmount, setters.get(i).getTimeCreated(), setters.get(i).isNotified(), setters.get(i).getWhitelist(), setters.get(i).getReceiverPlaytime()));
                return;
            }
        }
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
        double total = 0;
        for (Setter setters : setters){
            total += setters.getAmount();
        }
        return total;
    }

    /**
     * Get the total bounty that is accessible by the player
     * @param claimer Player to get the total claimable bounty value
     * @return The total claimable bounty value for the player
     */
    public double getTotalBounty(Player claimer){
        return getTotalBounty(claimer.getUniqueId());
    }

    public double getTotalBounty(UUID claimerUuid) {
        if (claimerUuid.equals(uuid))
            return getTotalBounty();
        double total = 0;
        for (Setter setters : setters){
            if (setters.canClaim(claimerUuid))
                total += setters.getAmount();
        }
        return total;
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
                // replace setter in map with combined values
                setterMap.replace(setter.getUuid(), new Setter(setter.getName(), setter.getUuid(), setter.getAmount() + firstSetter.getAmount(), Math.max(setter.getTimeCreated(), firstSetter.getTimeCreated()), setter.isNotified() && firstSetter.isNotified(), setter.getWhitelist(), firstSetter.getReceiverPlaytime()));
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
            return setters.stream().map(setter -> NotBounties.getPlayerWhitelist(setter.getUuid())).filter(whitelist -> !whitelist.isBlacklist()).flatMap(whitelist -> whitelist.getList().stream()).collect(Collectors.toList());
        }
        return setters.stream().map(Setter::getWhitelist).filter(whitelist -> !whitelist.isBlacklist()).flatMap(whitelist -> whitelist.getList().stream()).collect(Collectors.toList());
    }
    public List<UUID> getAllBlacklists() {
        if (ConfigOptions.variableWhitelist) {
            return setters.stream().map(setter -> NotBounties.getPlayerWhitelist(setter.getUuid())).filter(Whitelist::isBlacklist).flatMap(whitelist -> whitelist.getList().stream()).collect(Collectors.toList());
        }
        return setters.stream().map(Setter::getWhitelist).filter(Whitelist::isBlacklist).flatMap(whitelist -> whitelist.getList().stream()).collect(Collectors.toList());
    }

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
        return Double.compare(this.getTotalBounty(), o.getTotalBounty());
    }
}

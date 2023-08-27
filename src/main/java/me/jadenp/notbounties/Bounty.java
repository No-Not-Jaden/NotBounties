package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.ConfigOptions;
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


    public Bounty(Player setter, OfflinePlayer receiver, double amount, List<UUID> whitelist){
        // save player
        this.uuid = receiver.getUniqueId();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter(setter.getName(), setter.getUniqueId(), amount, System.currentTimeMillis(),false, whitelist));
    }

    public Bounty(OfflinePlayer receiver, double amount, List<UUID> whitelist){
        // save player
        this.uuid = receiver.getUniqueId();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter(ConfigOptions.consoleName, new UUID(0,0), amount, System.currentTimeMillis(), false, whitelist));
    }

    public Bounty(UUID uuid, List<Setter> setters, String name){
        this.uuid = uuid;
        this.setters = setters;
        this.name = name;
    }

    public void setDisplayName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void addBounty(Player setter, double amount, List<UUID> whitelist){
        // first check if player already set a bounty
        if (Bukkit.getPlayer(uuid) != null) {
            for (int i = 0; i < setters.size(); i++) {
                if (setters.get(i).getUuid().equals(setter.getUniqueId()) && compareWhitelists(whitelist, setters.get(i).getWhitelist())) {
                    // same person
                    setters.set(i, new Setter(setter.getName(), setter.getUniqueId(), setters.get(i).getAmount() + amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist));
                    return;
                }
            }
        }
        // otherwise, add a new setter
        setters.add(new Setter(setter.getName(), setter.getUniqueId(), amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist));
    }

    // console set bounty
    public void addBounty(double amount, List<UUID> whitelist){
        // first check if player already set a bounty
        for (int i = 0; i < setters.size(); i++){
            if (setters.get(i).getUuid().equals(new UUID(0,0)) && compareWhitelists(whitelist, setters.get(i).getWhitelist())){
                // same person
                setters.set(i, new Setter(ConfigOptions.consoleName, new UUID(0,0), setters.get(i).getAmount() + amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist));
                return;
            }
        }
        // otherwise, add a new setter
        setters.add(new Setter(ConfigOptions.consoleName, new UUID(0,0), amount, System.currentTimeMillis(), Bukkit.getPlayer(uuid) != null, whitelist));
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
                setters.set(i, new Setter(setters.get(i).getName(), setters.get(i).getUuid(), newAmount, setters.get(i).getTimeCreated(), setters.get(i).isNotified(), setters.get(i).getWhitelist()));
                return;
            }
        }
    }

    public long getLatestSetter() {
        return setters.stream().mapToLong(Setter::getTimeCreated).filter(setter -> setter >= 0).max().orElse(0);
    }

    public double getTotalBounty(){
        double total = 0;
        for (Setter setters : setters){
            total += setters.getAmount();
        }
        return total;
    }
    public double getTotalBounty(Player claimer){
        if (claimer.getUniqueId().equals(uuid))
            return getTotalBounty();
        double total = 0;
        for (Setter setters : setters){
            if (setters.canClaim(claimer))
                total += setters.getAmount();
        }
        return total;
    }

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
                setterMap.replace(setter.getUuid(), new Setter(setter.getName(), setter.getUuid(), setter.getAmount() + firstSetter.getAmount(), Math.max(setter.getTimeCreated(), firstSetter.getTimeCreated()), setter.isNotified() && firstSetter.isNotified(), setter.getWhitelist()));
            } else {
                setterMap.put(setter.getUuid(), setter);
            }
        }
        setters = new ArrayList<>(setterMap.values());
    }

    private boolean compareWhitelists(List<UUID> w1, List<UUID> w2) {
        if ((w1 == null || w1.isEmpty()) && (w2 == null || w2.isEmpty()))
            return true;
        if (w1 == null || w2 == null)
            return false;
        if (w1.size() != w2.size())
            return false;
        for (UUID uuid : w1) {
            if (!w2.contains(uuid))
                return false;
        }
        return true;
    }

    public Map<UUID, List<UUID>> getWhitelists(){
        //combineSetters();
        return setters.stream().filter(setter -> !setter.getWhitelist().isEmpty()).collect(Collectors.toMap(Setter::getUuid, Setter::getWhitelist, (a, b) -> b));
    }

    public List<UUID> getAllWhitelists() {
        if (ConfigOptions.variableWhitelist) {
            return setters.stream().flatMap(setter -> NotBounties.getInstance().getPlayerWhitelist(setter.getUuid()).stream()).collect(Collectors.toList());
        }
        return setters.stream().flatMap(setter -> setter.getWhitelist().stream()).collect(Collectors.toList());
    }

    @Override
    public int compareTo(@NotNull Bounty o) {
        return Double.compare(this.getTotalBounty(), o.getTotalBounty());
    }
}

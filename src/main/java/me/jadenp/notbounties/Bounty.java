package me.jadenp.notbounties;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Bounty implements Comparable<Bounty>{
    private final String uuid;
    private String name;
    private List<Setter> setters = new ArrayList<>();
    private boolean notified;

    public Bounty(Player setter, Player receiver, int amount){
        // save player
        this.uuid = receiver.getUniqueId().toString();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter(setter.getName(), setter.getUniqueId().toString(), amount, System.currentTimeMillis()));
        notified = true;
    }

    public Bounty(Player setter, OfflinePlayer receiver, int amount){
        // save player
        this.uuid = receiver.getUniqueId().toString();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter(setter.getName(), setter.getUniqueId().toString(), amount, System.currentTimeMillis()));
        notified = false;
    }

    public Bounty(Player receiver, int amount){
        // save player
        this.uuid = receiver.getUniqueId().toString();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter("CONSOLE", "CONSOLE", amount, System.currentTimeMillis()));
        notified = true;
    }

    public Bounty(OfflinePlayer receiver, int amount){
        // save player
        this.uuid = receiver.getUniqueId().toString();
        name = receiver.getName();
        // add to the total bounty
        setters.add(new Setter("CONSOLE", "CONSOLE", amount, System.currentTimeMillis()));
        notified = false;
    }

    public Bounty(String uuid, List<Setter> setters, String name, boolean notified){
        this.uuid = uuid;
        this.setters = setters;
        this.name = name;
        this.notified = notified;
    }

    public void setDisplayName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public String getUUID() {
        return uuid;
    }

    public void addBounty(Player setter, int amount){
        // first check if player already set a bounty
        for (int i = 0; i < setters.size(); i++){
            if (setters.get(i).getUuid().equalsIgnoreCase(setter.getUniqueId().toString())){
                // same person
                setters.set(i, new Setter(setter.getName(), setter.getUniqueId().toString(), setters.get(i).getAmount() + amount, System.currentTimeMillis()));
                return;
            }
        }
        // otherwise, add a new setter
        setters.add(new Setter(setter.getName(), setter.getUniqueId().toString(), amount, System.currentTimeMillis()));
    }

    // console set bounty
    public void addBounty(int amount){
        // first check if player already set a bounty
        for (int i = 0; i < setters.size(); i++){
            if (setters.get(i).getUuid().equalsIgnoreCase("CONSOLE")){
                // same person
                setters.set(i, new Setter("CONSOLE", "CONSOLE", setters.get(i).getAmount() + amount, System.currentTimeMillis()));
                return;
            }
        }
        // otherwise, add a new setter
        setters.add(new Setter("CONSOLE", "CONSOLE", amount, System.currentTimeMillis()));
    }

    public void removeBounty(UUID uuid){
        Iterator<Setter> settersIterator = setters.listIterator();
        while (settersIterator.hasNext()){
            Setter setter = settersIterator.next();
            if (setter.getUuid().equalsIgnoreCase(uuid.toString())){
                settersIterator.remove();
                return;
            }
        }
    }

    public void editBounty(UUID uuid, int newAmount){
        for (int i = 0; i < setters.size(); i++){
            if (setters.get(i).getUuid().equalsIgnoreCase(uuid.toString())){
                // same person
                setters.set(i, new Setter(setters.get(i).getName(), setters.get(i).getUuid(), newAmount, setters.get(i).getTimeCreated()));
                return;
            }
        }
    }


    public int getTotalBounty(){
        int total = 0;
        for (Setter setters : setters){
            total += setters.getAmount();
        }
        return total;
    }

    public List<Setter> getSetters() {
        return setters;
    }

    @Override
    public int compareTo(@NotNull Bounty o) {
        return this.getTotalBounty() - o.getTotalBounty();
    }
}

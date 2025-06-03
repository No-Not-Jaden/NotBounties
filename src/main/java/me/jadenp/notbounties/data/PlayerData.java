package me.jadenp.notbounties.data;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerData {

    public enum BroadcastSettings {
        EXTENDED, SHORT, DISABLE
    }

    private String playerName = null;
    private boolean generalImmunity = false;
    private boolean murderImmunity = false;
    private boolean randomImmunity = false;
    private boolean timedImmunity = false;
    private TimeZone timeZone = null;
    private double refundAmount = 0;
    private final List<ItemStack> refundItems = new LinkedList<>();
    private BroadcastSettings broadcastSettings = BroadcastSettings.EXTENDED;
    private final List<RewardHead> rewardHeads = new LinkedList<>();
    private long bountyCooldown = 0;
    private Whitelist whitelist = new Whitelist(new ArrayList<>(), false);
    private boolean newPlayer = true;

    public void setNewPlayer(boolean newPlayer) {
        this.newPlayer = newPlayer;
    }

    public boolean isNewPlayer() {
        return newPlayer;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setGeneralImmunity(boolean generalImmunity) {
        this.generalImmunity = generalImmunity;
    }

    public void setMurderImmunity(boolean murderImmunity) {
        this.murderImmunity = murderImmunity;
    }

    public void setRandomImmunity(boolean randomImmunity) {
        this.randomImmunity = randomImmunity;
    }

    public void setTimedImmunity(boolean timedImmunity) {
        this.timedImmunity = timedImmunity;
    }

    public boolean hasGeneralImmunity() {
        return generalImmunity;
    }

    public boolean hasMurderImmunity() {
        return murderImmunity;
    }

    public boolean hasRandomImmunity() {
        return randomImmunity;
    }

    public boolean hasTimedImmunity() {
        return timedImmunity;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void addRefund(double amount) {
        refundAmount += amount;
    }

    public void addRefund(List<ItemStack> items) {
        refundItems.addAll(items);
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public List<ItemStack> getRefundItems() {
        return refundItems;
    }

    public void clearRefund() {
        refundAmount = 0;
        refundItems.clear();
    }

    public void setBroadcastSettings(BroadcastSettings broadcastSettings) {
        this.broadcastSettings = broadcastSettings;
    }

    public BroadcastSettings getBroadcastSettings() {
        return broadcastSettings;
    }

    public void addRewardHeads(List<RewardHead> rewardHeads) {
        this.rewardHeads.addAll(rewardHeads);
    }

    public List<RewardHead> getRewardHeads() {
        return rewardHeads;
    }

    public long getBountyCooldown() {
        return bountyCooldown;
    }

    public void setBountyCooldown(long bountyCooldown) {
        this.bountyCooldown = bountyCooldown;
    }

    public void setWhitelist(Whitelist whitelist) {
        this.whitelist = whitelist;
    }

    public Whitelist getWhitelist() {
        return whitelist;
    }
}

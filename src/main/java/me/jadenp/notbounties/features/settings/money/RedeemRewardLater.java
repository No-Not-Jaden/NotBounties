package me.jadenp.notbounties.features.settings.money;

import org.bukkit.configuration.ConfigurationSection;

public class RedeemRewardLater {
    private int rewardDelay;
    private boolean vouchers;
    private boolean voucherPerSetter;
    private String setterLoreAddition;

    public void loadConfiguration(ConfigurationSection config) {
        rewardDelay = config.getInt("reward-delay");
        vouchers = config.getBoolean("vouchers");
        voucherPerSetter = config.getBoolean("voucher-per-setter");
        setterLoreAddition = config.getString("setter-lore-addition");
    }

    /**
     * Get the delay between claiming a bounty and receiving the reward in seconds.
     * @return The reward delay.
     */
    public int getRewardDelay() {
        return rewardDelay;
    }

    public String getSetterLoreAddition() {
        return setterLoreAddition;
    }

    public boolean isVoucherPerSetter() {
        return voucherPerSetter;
    }

    public boolean isVouchers() {
        return vouchers;
    }
}

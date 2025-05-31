package me.jadenp.notbounties.features.settings.money;

import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class Money extends ResourceConfiguration {
    private double minBounty;
    private double maxBounty;
    private double minBroadcast;
    private double bountyTax;
    private double deathTax;
    private final RedeemRewardLater redeemRewardLater = new RedeemRewardLater();
    private boolean buyOwn;
    private double buyOwnCostMultiply;
    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        NumberFormatting.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("currency")), config.getConfigurationSection("number-formatting"));
        redeemRewardLater.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("redeem-reward-later")));

        minBounty = config.getDouble("minimum-bounty");
        maxBounty = config.getDouble("maximum-bounty");
        minBroadcast = config.getDouble("minimum-broadcast");
        bountyTax = config.getDouble("bounty-tax");
        deathTax = config.getDouble("death-tax");
        buyOwn = config.getBoolean("buy-own-bounties.enabled");
        buyOwnCostMultiply = config.getDouble("buy-own-bounties.cost-multiply");
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[]{"number-formatting.divisions"};
    }

    @Override
    protected String getPath() {
        return "settings/money.yml";
    }

    public boolean isBuyOwn() {
        return buyOwn;
    }

    public double getBuyOwnCostMultiply() {
        return buyOwnCostMultiply;
    }

    public double getMinBounty() {
        return minBounty;
    }

    public double getBountyTax() {
        return bountyTax;
    }

    public double getDeathTax() {
        return deathTax;
    }

    public double getMaxBounty() {
        return maxBounty;
    }

    public double getMinBroadcast() {
        return minBroadcast;
    }

    public RedeemRewardLater getRedeemRewardLater() {
        return redeemRewardLater;
    }
}

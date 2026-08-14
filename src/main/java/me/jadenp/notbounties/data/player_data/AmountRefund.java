package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AmountRefund extends OnlineRefund<Double> {

    private final double amount;

    public AmountRefund(double amount, String reason) {
        super(reason);
        this.amount = amount;
    }

    public AmountRefund(int id, double amount, long timeCreated, String reason) {
        super(id, reason, timeCreated);
        this.amount = amount;
    }

    @Override
    public Optional<Double> getRefund() {
        return Optional.of(amount);
    }

    @Override
    public CompletableFuture<Double> getRefundAsync() {
        return CompletableFuture.completedFuture(amount);
    }

    @Override
    public String getRefundAmountString() {
        return NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(amount) + NumberFormatting.getCurrencySuffix();
    }

    @Override
    public void giveRefund(Player player) {
        super.giveRefund(player);
        NumberFormatting.doAddCommands(player, amount);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AmountRefund that = (AmountRefund) o;
        return Double.compare(amount, that.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amount);
    }

}

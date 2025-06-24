package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.Inconsistent;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class AmountRefund extends OnlineRefund {

    private final double amount;

    public AmountRefund(double amount, String reason) {
        super(reason);
        this.amount = amount;
    }

    public AmountRefund(double amount, long timeCreated, String reason) {
        super(reason, timeCreated);
        this.amount = amount;
    }

    @Override
    public Double getRefund() {
        return amount;
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

    @Override
    public String getID() {
        return amount + ":" + timeCreated;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Inconsistent> T copy() {
        return (T) new AmountRefund(amount, timeCreated, reason);
    }

    @Override
    public List<Inconsistent> getSubElements() {
        return List.of();
    }

    @Override
    public void setSubElements(List<Inconsistent> subElements) {
        // no sub elements - all local variables are final
    }
}

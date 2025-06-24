package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.utils.Inconsistent;
import org.bukkit.entity.Player;

import java.util.Objects;

public abstract class OnlineRefund extends Inconsistent {

    protected String reason;
    protected long timeCreated;

    protected OnlineRefund(String reason) {
        this.reason = reason;
        this.timeCreated = System.currentTimeMillis();
    }

    protected OnlineRefund(String reason, long timeCreated) {
        this.reason = reason;
        this.timeCreated = timeCreated;
    }

    public abstract Object getRefund();

    public abstract String getRefundAmountString();

    public void giveRefund(Player player) {
        if (reason != null && !reason.isBlank()) {
            String message = LanguageOptions.getMessage("refund").replace("{amount}", getRefundAmountString()).replace("{reason}", reason);
            player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + message, player));
        }
    }

    /**
     * Get the reason for the refund.
     * @return The reason for the refund.
     */
    public String getReason() {
        return reason;
    }

    @Override
    public long getLatestUpdate() {
        return timeCreated;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OnlineRefund that = (OnlineRefund) o;
        return timeCreated == that.timeCreated && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason, timeCreated);
    }

    @Override
    public String toString() {
        return "OnlineRefund{" +
                "reason='" + reason + '\'' +
                ", timeCreated=" + timeCreated +
                '}';
    }
}

package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class OnlineRefund<T> {

    protected String reason;
    protected final long timeCreated;
    protected Integer id;
    protected T refund;

    protected OnlineRefund(String reason) {
        this.reason = reason;
        this.timeCreated = System.currentTimeMillis();
    }

    protected OnlineRefund(String reason, long timeCreated) {
        this.reason = reason;
        this.timeCreated = timeCreated;
    }

    protected OnlineRefund(T refund, String reason, long timeCreated) {
        this(reason, timeCreated);
        this.refund = refund;
    }

    protected OnlineRefund(int id, String reason, long timeCreated) {
        this(reason, timeCreated);
        this.id = id;
    }

    public abstract Optional<T> getRefund();

    public abstract CompletableFuture<T> getRefundAsync();

    public boolean isRefundLoaded() {
        return refund != null;
    }

    public abstract String getRefundAmountString();

    public void giveRefund(Player player) {
        if (reason != null && !reason.isBlank()) {
            String message = LanguageOptions.getMessage("refund").replace("{amount}", getRefundAmountString()).replace("{reason}", reason);
            Messages.send(player, message, MessageContext.builder().receiver(player).build());
        }
    }

    /**
     * Get the reason for the refund.
     * @return The reason for the refund.
     */
    public String getReason() {
        return reason;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public Optional<Integer> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OnlineRefund<?> that = (OnlineRefund<?>) o;
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

package me.jadenp.notbounties.features;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MessageContext {
    private final OfflinePlayer receiver;
    private final OfflinePlayer player;
    private final UUID playerUUID;
    private final Double amount;
    private final Double bountyAmount;
    private final Long time;
    private final LocalTime.TimeFormat timeFormat;
    private final boolean playerPrefix;
    private final boolean playerSuffix;
    private final boolean addPrefix;
    private final Map<String, String> placeholders;

    private MessageContext(Builder builder) {
        this.receiver = builder.receiver;
        this.player = builder.player;
        this.playerUUID = builder.playerUUID;
        this.amount = builder.amount;
        this.bountyAmount = builder.bountyAmount;
        this.time = builder.time;
        this.timeFormat = builder.timeFormat;
        this.playerPrefix = builder.playerPrefix;
        this.playerSuffix = builder.playerSuffix;
        this.addPrefix = builder.addPrefix;
        this.placeholders = Collections.unmodifiableMap(new LinkedHashMap<>(builder.placeholders));
    }

    public static Builder builder() {
        return new Builder();
    }

    public OfflinePlayer getReceiver() {
        return receiver;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Double getAmount() {
        return amount;
    }

    public Double getBountyAmount() {
        return bountyAmount;
    }

    public Long getTime() {
        return time;
    }

    public LocalTime.TimeFormat getTimeFormat() {
        return timeFormat;
    }

    public boolean isPlayerPrefix() {
        return playerPrefix;
    }

    public boolean isPlayerSuffix() {
        return playerSuffix;
    }

    public boolean isAddPrefix() {
        return addPrefix;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public static class Builder {
        private OfflinePlayer receiver;
        private OfflinePlayer player;
        private UUID playerUUID;
        private Double amount;
        private Double bountyAmount;
        private Long time;
        private LocalTime.TimeFormat timeFormat;
        private boolean playerPrefix = true;
        private boolean playerSuffix = true;
        private boolean addPrefix = true;
        private final Map<String, String> placeholders = new LinkedHashMap<>();

        public Builder withPrefix(boolean prefix) {
            this.playerPrefix = prefix;
            return this;
        }

        public Builder withPlayerPrefix(boolean playerPrefix) {
            this.playerPrefix = playerPrefix;
            return this;
        }

        public Builder withPlayerSuffix(boolean playerSuffix) {
            this.playerSuffix = playerSuffix;
            return this;
        }

        public Builder receiver(OfflinePlayer receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder player(OfflinePlayer player) {
            this.player = player;
            return this;
        }

        public Builder player(UUID playerUUID) {
            this.playerUUID = playerUUID;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder bounty(double bountyAmount) {
            this.bountyAmount = bountyAmount;
            return this;
        }

        public Builder bounty(Bounty bounty) {
            if (bounty != null) {
                this.bountyAmount = bounty.getTotalDisplayBounty();
            }
            return this;
        }

        public Builder time(long time, LocalTime.TimeFormat timeFormat) {
            this.time = time;
            this.timeFormat = timeFormat;
            return this;
        }

        public Builder placeholder(String key, Object value) {
            if (key != null) {
                this.placeholders.put(key, value == null ? "" : String.valueOf(value));
            }
            return this;
        }

        public MessageContext build() {
            return new MessageContext(this);
        }
    }
}

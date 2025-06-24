package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.Inconsistent;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public final class RewardHead extends OnlineRefund {

    private static boolean rewardSetters;
    private static boolean rewardKiller;
    private static boolean rewardAnyKill;

    private final UUID uuid;
    private final UUID killer;
    private final double amount;

    public static void loadConfiguration(ConfigurationSection config) {
        rewardSetters = config.getBoolean("setters");
        rewardKiller = config.getBoolean("claimed");
        rewardAnyKill = config.getBoolean("any-kill");
    }

    public static boolean isRewardAnyKill() {
        return rewardAnyKill;
    }

    public static boolean isRewardKiller() {
        return rewardKiller;
    }

    public static boolean isRewardSetters() {
        return rewardSetters;
    }

    public RewardHead(UUID uuid, UUID killer, double amount, String reason) {
        super(reason);
        SkinManager.isSkinLoaded(uuid);
        this.uuid = uuid;
        this.killer = killer;
        this.amount = amount;
    }

    public RewardHead(UUID uuid, UUID killer, double amount, long timeCreated, String reason) {
        super(reason, timeCreated);
        SkinManager.isSkinLoaded(uuid);
        this.uuid = uuid;
        this.killer = killer;
        this.amount = amount;
    }

    private ItemStack getItem() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String killerName = LoggedPlayers.getPlayerName(killer);
        ItemStack skull = Head.createPlayerSkull(uuid, SkinManager.getSkin(uuid).url());
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setDisplayName(LanguageOptions.parse(LanguageOptions.getMessage("reward-head-name").replace("{killer}", killerName), amount, player));
        List<String> lore = new ArrayList<>();
        LanguageOptions.getListMessage("reward-head-lore").forEach(str -> lore.add(LanguageOptions.parse(str.replace("{killer}", killerName), amount, player)));
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        return skull;
    }

    @Deprecated(since = "1.22.0")
    public static RewardHead decodeRewardHead(String input) {
        // uuid,killerUuid,amount
        try {
            if (!input.contains(",")) {
                UUID uuid = UUID.fromString(input);
                return new RewardHead(uuid, DataManager.GLOBAL_SERVER_ID, 0, null);
            } else {
                UUID uuid = UUID.fromString(input.substring(0, input.indexOf(",")));
                input = input.substring(input.indexOf(',') + 1);
                UUID killerUuid;
                try {
                    killerUuid = UUID.fromString(input.substring(0, input.indexOf(",")));
                } catch (IllegalArgumentException e) {
                    // reward heads used to store player names at this location, so a uuid may not be present.
                    killerUuid = DataManager.GLOBAL_SERVER_ID;
                }
                input = input.substring(input.indexOf(",") + 1);
                double amount = NumberFormatting.tryParse(input);
                return new RewardHead(uuid, killerUuid, amount, null);
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning(e.toString());
        }
        return null;
    }

    @Override
    public String toString() {
        return "RewardHead{" +
                "uuid=" + uuid +
                ", killer=" + killer +
                ", amount=" + amount +
                ", timeCreated=" + timeCreated +
                ", reason='" + reason + '\'' +
                '}';
    }


    public UUID getUuid() {
        return uuid;
    }

    public UUID getKiller() {
        return killer;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RewardHead that = (RewardHead) o;
        return Double.compare(amount, that.amount) == 0 && Objects.equals(uuid, that.uuid) && Objects.equals(killer, that.killer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, killer, amount);
    }

    @Override
    public ItemStack getRefund() {
        return getItem();
    }

    @Override
    public String getRefundAmountString() {
        return "a reward head";
    }

    @Override
    public void giveRefund(Player player) {
        super.giveRefund(player);
        NumberFormatting.givePlayer(player, getItem(), 1);
    }

    @Override
    public String getID() {
        return uuid.toString() + "," + killer.toString() + "," + amount + ":" + timeCreated;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Inconsistent> T copy() {
        return (T) new RewardHead(uuid, killer, amount, timeCreated, reason);
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

package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ItemRefund extends OnlineRefund<List<ItemStack>> {

    public ItemRefund(List<ItemStack> refund, String reason) {
        super(reason);
        this.refund = refund;
    }

    public ItemRefund(List<ItemStack> refund, long timeCreated, String reason) {
        super(refund, reason, timeCreated);
    }

    public ItemRefund(int id, String reason, long timeCreated) {
        super(id, reason, timeCreated);
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ItemRefund that = (ItemRefund) o;
        return Objects.equals(refund, that.refund);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), refund);
    }

    @Override
    public Optional<List<ItemStack>> getRefund() {
        return Optional.ofNullable(refund);
    }

    @Override
    public CompletableFuture<List<ItemStack>> getRefundAsync() {
        if (refund == null) {
            if (id == null)
                return CompletableFuture.completedFuture(Collections.emptyList());
            CompletableFuture<List<ItemStack>> loadingItems = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getRefundItemsAsync(id);
            loadingItems.thenApply(itemStacks -> refund = itemStacks);
            return loadingItems;
        }
        return CompletableFuture.completedFuture(refund);
    }


    @Override
    public String getRefundAmountString() {
        if (refund != null) {
            return NumberFormatting.listItems(refund, 'x');
        } else {
            return "⛏";
        }
    }

    @Override
    public void giveRefund(Player player) {
        super.giveRefund(player);
        NumberFormatting.givePlayer(player, refund, false);
    }
}

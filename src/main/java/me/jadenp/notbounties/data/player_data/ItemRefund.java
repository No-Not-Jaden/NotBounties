package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.SerializeInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

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
    public List<ItemStack> getRefundAsync() {
        if (refund == null) {
            if (id == null)
                return Collections.emptyList();
            refund = DataManager.loadRefundItems(id);
        }
        return refund;
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

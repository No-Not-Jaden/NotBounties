package me.jadenp.notbounties.data.player_data;

import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.utils.Inconsistent;
import me.jadenp.notbounties.utils.SerializeInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ItemRefund extends OnlineRefund {

    private final List<ItemStack> refund;
    private final String encodedRefund;

    public ItemRefund(List<ItemStack> refund, String reason) {
        super(reason);
        this.refund = refund;
        this.encodedRefund = SerializeInventory.itemStackArrayToBase64(refund.toArray(new ItemStack[0]));
    }

    public ItemRefund(String encodedRefund, String reason) throws IOException {
        super(reason);
        this.encodedRefund = encodedRefund;
        this.refund = Arrays.stream(SerializeInventory.itemStackArrayFromBase64(encodedRefund)).toList();
    }

    public ItemRefund(String encodedRefund, long timeCreated, String reason) throws IOException {
        super(reason, timeCreated);
        this.encodedRefund = encodedRefund;
        this.refund = Arrays.stream(SerializeInventory.itemStackArrayFromBase64(encodedRefund)).toList();
    }

    public ItemRefund(List<ItemStack> refund, String encodedRefund, long timeCreated, String reason) {
        super(reason, timeCreated);
        this.refund = refund;
        this.encodedRefund = encodedRefund;
    }

    public String getEncodedRefund() {
        return encodedRefund;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ItemRefund that = (ItemRefund) o;
        return Objects.equals(refund, that.refund) && Objects.equals(encodedRefund, that.encodedRefund);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), refund, encodedRefund);
    }

    @Override
    public List<ItemStack> getRefund() {
        return refund;
    }

    @Override
    public String getRefundAmountString() {
        return NumberFormatting.listItems(refund, 'x');
    }

    @Override
    public void giveRefund(Player player) {
        super.giveRefund(player);
        NumberFormatting.givePlayer(player, refund, false);
    }

    @Override
    public String getID() {
        return encodedRefund + ":" + timeCreated;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Inconsistent> T copy() {
        return (T) new ItemRefund(refund, encodedRefund, timeCreated, reason);
    }

    @Override
    public List<Inconsistent> getSubElements() {
        return List.of();
    }

    @Override
    public void setSubElements(List<Inconsistent> subElements) {
        // no sub elements - although, I could package the itemStacks individually to hold the entire item refund
        // in this object, but I don't see any other upsides.
    }
}

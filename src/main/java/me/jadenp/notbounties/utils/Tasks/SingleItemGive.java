package me.jadenp.notbounties.utils.Tasks;

import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SingleItemGive extends CancelableTask{
    private final Player p;
    private final ItemStack itemStack;
    private long toGive;

    public SingleItemGive(Player p, ItemStack itemStack, long amount) {
        super();
        this.p = p;
        this.itemStack = itemStack;
        this.toGive = amount;
    }

    @Override
    public void cancel() {
        if (toGive > 0) {
            // refund the rest of the items
            itemStack.setAmount((int) toGive);
            BountyManager.refundPlayer(p.getUniqueId(), 0, new ArrayList<>(List.of(itemStack)));
        }
    }

    @Override
    public void run() {
        if (toGive <= 0) {
            cancel();
            return;
        }
        if (p.isOnline()) {
            if (toGive > itemStack.getMaxStackSize()) {
                itemStack.setAmount(itemStack.getMaxStackSize());
                toGive -= itemStack.getMaxStackSize();
            } else {
                itemStack.setAmount((int) toGive);
                toGive = 0;
            }
            p.playSound(p.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            NumberFormatting.givePlayerInstantly(p, itemStack, itemStack.getAmount());
        } else {
            cancel();
        }
    }
}

package me.jadenp.notbounties.utils.Tasks;

import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MultipleItemGive extends CancelableTask{
    private final Player p;
    private final List<ItemStack> itemStackList;
    private int index = 0;

    public MultipleItemGive(Player p, List<ItemStack> itemStackList) {
        super();
        this.p = p;
        this.itemStackList = itemStackList;
    }
    @Override
    public void cancel() {
        super.cancel();
        if (index < itemStackList.size()) {
            // refund the rest of the items
            BountyManager.refundPlayer(p.getUniqueId(), 0, itemStackList.subList(index, itemStackList.size()));
        }
    }

    @Override
    public void run() {
        if (p.isOnline()) {
            NumberFormatting.givePlayer(p, itemStackList.get(index), itemStackList.get(index).getAmount());
            index++;
            if (index >= itemStackList.size())
                this.cancel();
        } else {
            // cancel -> will add to refund
            this.cancel();
        }
    }
}

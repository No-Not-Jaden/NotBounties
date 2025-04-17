package me.jadenp.notbounties.utils.Tasks;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.utils.BountyManager;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DelayedReward extends CancelableTask{
    // copying bounty in case it gets modified before the reward is given out
    private final Bounty finalBounty;
    // save the uuid, so no access of the player object is needed in the future.
    private final UUID uuid;

    public DelayedReward(Bounty bounty, Player player) {
        super();
        finalBounty = new Bounty(bounty);
        uuid = player.getUniqueId();
    }

    @Override
    public void cancel() {
        super.cancel();
        BountyManager.rewardBounty(uuid, finalBounty);
    }

    @Override
    public void run() {
        cancel();
    }
}

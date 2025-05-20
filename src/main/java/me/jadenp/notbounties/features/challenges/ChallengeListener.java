package me.jadenp.notbounties.features.challenges;

import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.bounty_events.BountyClaimEvent;
import me.jadenp.notbounties.bounty_events.BountySetEvent;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ChallengeListener implements Listener {
    @EventHandler
    public void onBountyClaim(BountyClaimEvent event) {
        ChallengeManager.updateChallengeProgress(event.getKiller().getUniqueId(), ChallengeType.CLAIM, 1);
        for (Setter setter : event.getBounty().getSetters()) {
            ChallengeManager.updateChallengeProgress(setter.getUuid(), ChallengeType.SUCCESSFUL_BOUNTY, 1);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(event.getKiller().getWorld()) && player.getLocation().distance(event.getKiller().getLocation()) < 50) {
                ChallengeManager.updateChallengeProgress(player.getUniqueId(), ChallengeType.WITNESS_BOUNTY_CLAIM, 1);
            }
        }
        boolean isNaked = true;
        for (ItemStack itemStack : event.getKiller().getInventory().getArmorContents()) {
            if (itemStack != null) {
                isNaked = false;
                break;
            }
        }
        if (isNaked)
            ChallengeManager.updateChallengeProgress(event.getKiller().getUniqueId(), ChallengeType.NAKED_BOUNTY_CLAIM, 1);
        ChallengeManager.updateChallengeProgress(event.getKiller().getUniqueId(), ChallengeType.CLOSE_BOUNTY, event.getKiller().getHealth());

    }

    @EventHandler
    public void onBountySet(BountySetEvent event) {
        ChallengeManager.updateChallengeProgress(event.getBounty().getLastSetter().getUuid(), ChallengeType.AMOUNT_SET, event.getBounty().getTotalDisplayBounty());
        ChallengeManager.updateChallengeProgress(event.getBounty().getUUID(), ChallengeType.BOUNTY_MULTIPLE, event.getBounty().getTotalDisplayBounty());
        ChallengeManager.updateChallengeProgress(event.getBounty().getUUID(), ChallengeType.BOUNTY_INCREASE, event.getBounty().getTotalDisplayBounty());
        ChallengeManager.updateChallengeProgress(event.getBounty().getUUID(), ChallengeType.RECEIVE_BOUNTY, 1);
        if (!event.getBounty().getLastSetter().getWhitelist().getList().isEmpty())
            ChallengeManager.updateChallengeProgress(event.getBounty().getLastSetter().getUuid(), ChallengeType.WHITELISTED_BOUNTY_SET, event.getBounty().getLastSetter().getWhitelist().getList().size());
        if (event.getBounty().getLastSetter().getUuid().equals(DataManager.GLOBAL_SERVER_ID))
            ChallengeManager.updateChallengeProgress(event.getBounty().getUUID(), ChallengeType.AUTO_BOUNTY, event.getBounty().getTotalDisplayBounty());
    }

}

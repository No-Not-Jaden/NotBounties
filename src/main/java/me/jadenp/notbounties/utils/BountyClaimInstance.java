package me.jadenp.notbounties.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record BountyClaimInstance(@NotNull Player player, @Nullable Player killer, List<ItemStack> drops, double deathTax) {
    public void claimBounty() {
        BountyManager.claimBounty(player, killer, drops, true, deathTax);
    }
}

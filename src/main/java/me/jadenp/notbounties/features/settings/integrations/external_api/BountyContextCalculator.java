package me.jadenp.notbounties.features.settings.integrations.external_api;

import me.jadenp.notbounties.utils.BountyManager;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BountyContextCalculator implements ContextCalculator<Player> {

    private static final String KEY = "bounty";

    @Override
    public void calculate(@NonNull Player player, @NonNull ContextConsumer contextConsumer) {
        contextConsumer.accept(KEY, BountyManager.hasBounty(player.getUniqueId()) + "");
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        builder.add(KEY, "false");
        builder.add(KEY, "true");
        return builder.build();
    }
}

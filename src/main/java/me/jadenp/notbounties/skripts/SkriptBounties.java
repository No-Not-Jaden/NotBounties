package me.jadenp.notbounties.skripts;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.utils.BountyManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class SkriptBounties extends SimpleExpression<Double> {

    static {
        Skript.registerExpression(SkriptBounties.class, Double.class, ExpressionType.COMBINED, "[the] bounty of %-player/offlineplayer%");
    }
    private Expression<OfflinePlayer> player;
    @Override
    protected Double[] get(@NotNull Event event) {
        OfflinePlayer p = player.getSingle(event);
        if (p != null) {
            Bounty bounty = BountyManager.getBounty(p.getUniqueId());
            double bountyAmount = bounty != null ? bounty.getTotalDisplayBounty() : 0;
            return new Double[] {bountyAmount};
        }

        return null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public @NotNull Class<? extends Double> getReturnType() {
        return Double.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, SkriptParser.@NotNull ParseResult parser) {
        player = (Expression<OfflinePlayer>) exprs[0];
        return true;
    }

    @Override
    public @NotNull String toString(Event event, boolean b) {
        return "Expression with player: " + player.toString(event, b);
    }

    @Override
    public Class<?>[] acceptChange(final Changer.@NotNull ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.ADD) {
            return CollectionUtils.array(Double.class);
        }
        return null;
    }

    @Override
    public void change(@NotNull Event event, Object[] delta, Changer.@NotNull ChangeMode mode) {
        OfflinePlayer p = player.getSingle(event);
        if (p != null) {
            Bounty bounty = BountyManager.getBounty(p.getUniqueId());
            if (bounty != null) {
                if (mode == Changer.ChangeMode.ADD) {
                    BountyManager.editBounty(bounty, null, (double) delta[0]);
                } else if (mode == Changer.ChangeMode.REMOVE) {
                    BountyManager.editBounty(bounty, null, -1 * (double) delta[0]);
                } else if (mode == Changer.ChangeMode.SET) {
                    BountyManager.editBounty(bounty, null, (double) delta[0] - bounty.getTotalDisplayBounty());
                }
            } else {
                if (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.SET) {
                    BountyManager.addBounty(p, (double) delta[0], new ArrayList<>(), new Whitelist(Collections.emptySortedSet(), false));
                }
            }
        }
    }

}

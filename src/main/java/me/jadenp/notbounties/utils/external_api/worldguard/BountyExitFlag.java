package me.jadenp.notbounties.utils.external_api.worldguard;

// This code is a modified version of the WorldGuard ExitFlag Handler
// https://github.com/EngineHub/WorldGuard/blob/master/worldguard-core/src/main/java/com/sk89q/worldguard/session/handler/ExitFlag.java

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;

public class BountyExitFlag extends FlagValueChangeHandler<State> {

    public static final Factory FACTORY = new Factory();
    public static class Factory extends Handler.Factory<BountyExitFlag> {
        @Override
        public BountyExitFlag create(Session session) {
            return new BountyExitFlag(session);
        }
    }

    private static final long MESSAGE_THRESHOLD = 1000 * 2L;
    private String storedMessage;
    private boolean exitViaTeleport = false;
    private long lastMessage;

    public BountyExitFlag(Session session) {
        super(session, WorldGuardClass.getBountyExit());
    }

    private void update(LocalPlayer localPlayer, ApplicableRegionSet set, boolean allowed) {
        if (!allowed) {
            storedMessage = LanguageOptions.parse(LanguageOptions.getMessage("deny-exit"), null);
            exitViaTeleport = set.testState(localPlayer, WorldGuardClass.getBountyTeleportExit());
        }
    }

    private void sendMessage(LocalPlayer player) {
        long now = System.currentTimeMillis();

        if ((now - lastMessage) > MESSAGE_THRESHOLD && storedMessage != null && !storedMessage.isEmpty()) {
            player.printRaw(storedMessage);
            lastMessage = now;
        }
    }

    @Override
    protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, State value) {
        update(player, set, StateFlag.test(value));
    }

    @Override
    protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State currentValue, State lastValue, MoveType moveType) {
        if (!NotBounties.getInstance().isEnabled() || getSession().getManager().hasBypass(player, (World) from.getExtent())) {
            return true;
        }

        boolean lastAllowed = StateFlag.test(lastValue);
        boolean allowed = StateFlag.test(currentValue);

        if (allowed && !lastAllowed && !(moveType.isTeleport() && exitViaTeleport) && moveType.isCancellable()
            && DataManager.getLocalData().getOnlineBounty(player.getUniqueId()) != null) {
            Boolean override = toSet.queryValue(player, Flags.EXIT_OVERRIDE);
            if (override == null || !override) {
                sendMessage(player);
                return false;
            }
        }

        update(player, toSet, allowed);
        return true;
    }

    @Override
    protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State lastValue, MoveType moveType) {
        if (!NotBounties.getInstance().isEnabled() || getSession().getManager().hasBypass(player, (World) from.getExtent())) {
            return true;
        }

        boolean lastAllowed = StateFlag.test(lastValue);

        if (!lastAllowed && moveType.isCancellable() && DataManager.getLocalData().getOnlineBounty(player.getUniqueId()) != null) {
            Boolean override = toSet.queryValue(player, Flags.EXIT_OVERRIDE);
            if (override == null || !override) {
                sendMessage(player);
                return false;
            }
        }

        return true;
    }

}
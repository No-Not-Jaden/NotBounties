package me.jadenp.notbounties.features.settings.integrations.external_api.worldguard;

// This code is a modified version of the WorldGuard EntryFlag Handler
// https://github.com/EngineHub/WorldGuard/blob/master/worldguard-core/src/main/java/com/sk89q/worldguard/session/handler/EntryFlag.java

import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.features.LanguageOptions;

import java.util.Set;

public class BountyEntryFlag extends Handler {

    public static final Factory FACTORY = new Factory();
    public static class Factory extends Handler.Factory<BountyEntryFlag> {
        @Override
        public BountyEntryFlag create(Session session) {
            return new BountyEntryFlag(session);
        }
    }

    private static final long MESSAGE_THRESHOLD = 1000 * 2L;
    private long lastMessage;

    public BountyEntryFlag(Session session) {
        super(session);
    }


    @Override
    public boolean onCrossBoundary(LocalPlayer player, com.sk89q.worldedit.util.Location from, com.sk89q.worldedit.util.Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        boolean allowed = toSet.testState(player, WorldGuardClass.getBountyEntry());

        if (!getSession().getManager().hasBypass(player, (World) to.getExtent()) && !allowed && moveType.isCancellable()
                && DataManager.getLocalData().getOnlineBounty(player.getUniqueId()) != null) {
            String message = LanguageOptions.parse(LanguageOptions.getMessage("deny-entry"), null);
            long now = System.currentTimeMillis();

            if (now - lastMessage > MESSAGE_THRESHOLD && !message.isEmpty()) {
                player.printRaw(message);
                lastMessage = now;
            }

            return false;
        } else {
            return true;
        }
    }
}

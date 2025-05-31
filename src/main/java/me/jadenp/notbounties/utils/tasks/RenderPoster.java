package me.jadenp.notbounties.utils.tasks;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.settings.display.map.BountyPosterProvider;
import me.jadenp.notbounties.ui.SkinManager;
import org.bukkit.Bukkit;

public class RenderPoster extends CancelableTask{

    private final String name;
    private final BountyPosterProvider renderer;

    public RenderPoster(String name, BountyPosterProvider renderer) {
        super();
        this.name = name;
        this.renderer = renderer;
    }

    @Override
    public void run() {
        if (Bukkit.getOnlinePlayers().isEmpty())
            return;
        this.cancel();
        AsyncRender asyncRender = new AsyncRender();
        asyncRender.setTaskImplementation(NotBounties.getServerImplementation().async().runAtFixedRate(asyncRender, 10, 4));
    }

    private class AsyncRender extends CancelableTask {

        private int maxRequests = 50;
        public AsyncRender() {
            super();
        }

        @Override
        public void run() {
            // check if max requests hit
            if (maxRequests <= 0) {
                this.cancel();
                NotBounties.debugMessage("Timed out waiting to generate a bounty poster for \"" + name + "\". Provider: " + renderer.getClass(), true);
                renderer.generateBackground(name);
                return;
            }
            maxRequests--;
            if (renderer.isMissingElements())
                return;
            if (!renderer.isPlayerFacePresent())
                renderer.setPlayerFace(SkinManager.getPlayerFace(renderer.getPlayer().getUniqueId()), name);
            renderer.generateBackground(name);
            this.cancel();
        }
    }
}

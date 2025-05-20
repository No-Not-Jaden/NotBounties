package me.jadenp.notbounties.utils.tasks;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.features.settings.display.map.Renderer;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.Bukkit;

public class RenderPoster extends CancelableTask{

    private final String name;
    private final Renderer renderer;

    public RenderPoster(String name, Renderer renderer) {
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
                NotBounties.debugMessage("Timed out getting skin from \"" + name + "\" for a bounty poster. A question mark will be displayed instead.", true);
                renderer.renderPoster(SkinManager.getPlayerFace(DataManager.GLOBAL_SERVER_ID), name, false);
                return;
            }
            maxRequests--;
            if (!SkinManager.isSkinLoaded(renderer.getPlayer().getUniqueId()))
                return;
            renderer.renderPoster(SkinManager.getPlayerFace(renderer.getPlayer().getUniqueId()), name, true);
            this.cancel();
        }
    }
}

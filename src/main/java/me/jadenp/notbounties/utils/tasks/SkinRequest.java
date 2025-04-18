package me.jadenp.notbounties.utils.tasks;

import me.jadenp.notbounties.databases.proxy.ProxyMessaging;

import java.util.UUID;

public class SkinRequest extends CancelableTask {


    private int index = 0;
    private UUID[] skinsToSend;

    public SkinRequest(UUID[] skinsToSend) {
        super();
        this.skinsToSend = skinsToSend;
    }

    @Override
    public void run() {
        if (index >= skinsToSend.length) {
            this.cancel();
            return;
        }
        ProxyMessaging.sendSkinRequest(skinsToSend[index]);
        index++;
        if (index >= skinsToSend.length) {
            this.cancel();
        }

    }
}

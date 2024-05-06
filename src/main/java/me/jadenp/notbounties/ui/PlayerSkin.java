package me.jadenp.notbounties.ui;

import java.net.URL;

public class PlayerSkin {
    private final URL url;
    private final String id;

    public PlayerSkin(URL url, String id) {

        this.url = url;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public URL getUrl() {
        return url;
    }
}

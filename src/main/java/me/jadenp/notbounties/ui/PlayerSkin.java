package me.jadenp.notbounties.ui;

public record PlayerSkin(String id, boolean missing) {
    public String url() {
        return "https://textures.minecraft.net/texture/" + id;
    }
}

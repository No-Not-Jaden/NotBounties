package me.jadenp.notbounties.map;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.ConfigOptions;
import me.jadenp.notbounties.utils.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class Renderer extends MapRenderer {
    private final BufferedImage image;
    private final BufferedImage reward = new BufferedImage(64, 12, BufferedImage.TYPE_INT_ARGB);
    private static final float maxFont = 20f;
    private double currentCost = -1;
    private final OfflinePlayer player;
    private static final float rewardFont = 13f;
    private long lastRender = System.currentTimeMillis();

    public Renderer(UUID uuid) throws IOException {
        this.player = Bukkit.getOfflinePlayer(uuid);
        String name;
        if (NotBounties.getInstance().hasBounty(player)) {
            Bounty bounty = NotBounties.getInstance().getBounty(player);
            assert bounty != null;
            name = bounty.getName();
        } else {
            name = NotBounties.getInstance().getPlayerName(uuid);
        }

        File imageFile = new File(BountyMap.posterDirectory + File.separator + name.toLowerCase() + ".png");
        if (imageFile.exists()) {
            image = ImageIO.read(imageFile);
            return;
        }
        image = BountyMap.deepCopy(BountyMap.bountyPoster);
        Graphics2D graphics = image.createGraphics();
        try {
            BufferedImage head = ImageIO.read(new URL("https://cravatar.eu/helmavatar/" + uuid + "/64.png"));
            graphics.drawImage(head, 32,32, null);
        } catch (IOException ignored) {

        }

        // center of text is y112
        float fontSize = maxFont;
        graphics.setFont(BountyMap.getPlayerFont(fontSize, true));
        FontMetrics metrics = graphics.getFontMetrics();
        while (metrics.stringWidth(name) > 120 && fontSize > 1) {
            fontSize--;
            graphics.setFont(BountyMap.getPlayerFont(fontSize, true));
            metrics = graphics.getFontMetrics();
        }
        int x = 64 - metrics.stringWidth(name) / 2; // center - width/2
        int y = ConfigOptions.displayReward ? 112 : 112 + metrics.getHeight() / 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(name, x, y);

        if (ConfigOptions.displayReward) {
            graphics.setFont(BountyMap.getPlayerFont(rewardFont, false));
            metrics = graphics.getFontMetrics();
            x = 64 - metrics.stringWidth(ConfigOptions.rewardText);
            y = 126;
            graphics.setColor(Color.BLACK);
            graphics.drawString(ConfigOptions.rewardText, x, y);
        }

        ImageIO.write(image, "PNG", imageFile);
    }

    public double getBountyAmount() {
        if (NotBounties.getInstance().hasBounty(player)) {
            Bounty bounty = NotBounties.getInstance().getBounty(player);
            assert bounty != null;
            return bounty.getTotalBounty();
        }
        return 0;
    }

    private Color getColor(int argb) {
        int b = (argb)&0xFF;
        int g = (argb>>8)&0xFF;
        int r = (argb>>16)&0xFF;
        int a = (argb>>24)&0xFF;
        return new Color(r,g,b,a);
    }

    private void drawTransparentImage(int x, int y, BufferedImage image, MapCanvas canvas) {
        for (int ix = 0; ix < image.getWidth(); ix++) {
            for (int iy = 0; iy < image.getHeight(); iy++) {
                Color color = getColor(image.getRGB(ix, iy));
                if (color.getAlpha() > 10) {
                    canvas.setPixelColor(x + ix, y + iy, color);
                }
            }
        }
    }


    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player renderer) {
        if (!map.isLocked() || currentCost == -1) {
            if (System.currentTimeMillis() - lastRender < ConfigOptions.updateInterval)
                return;
            lastRender = System.currentTimeMillis();
            double bountyAmount = getBountyAmount();
            if (currentCost != bountyAmount) {
                // redraw canvas
                currentCost = bountyAmount;
                canvas.drawImage(0, 0, image);
                if (ConfigOptions.displayReward) {
                    Graphics2D graphics = reward.createGraphics();
                    graphics.setComposite(AlphaComposite.Clear);
                    graphics.fillRect(0, 0, reward.getWidth(), reward.getHeight());
                    graphics.setFont(BountyMap.getPlayerFont(rewardFont, false));
                    int x = 0;
                    int y = 12;
                    graphics.setComposite(AlphaComposite.Src);
                    graphics.setColor(Color.BLACK);
                    String rewardText = ConfigOptions.currencyWrap ? NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(currentCost) + NumberFormatting.currencySuffix : NumberFormatting.formatNumber(currentCost);
                    graphics.drawString(ChatColor.stripColor(rewardText), x, y);
                    drawTransparentImage(64, 114, reward, canvas);
                }
                if (currentCost == 0) {
                    drawTransparentImage(28,28, BountyMap.deadBounty, canvas);
                }
            }
        }



    }
}

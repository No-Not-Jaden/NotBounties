package me.jadenp.notbounties.features.settings.display.map;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.features.settings.integrations.external_api.PlaceholderAPIClass;
import me.jadenp.notbounties.utils.tasks.RenderPoster;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Renderer extends MapRenderer {

    protected static final Map<Character, Color> colorTranslations = new HashMap<>();

    static {
        colorTranslations.put('0', Color.BLACK);
        colorTranslations.put('1', new Color(0,0,170));
        colorTranslations.put('2', new Color(0,170,0));
        colorTranslations.put('3', new Color(0,170,170));
        colorTranslations.put('4', new Color(170,0,0));
        colorTranslations.put('5', new Color(170,0,170));
        colorTranslations.put('6', new Color(255,170,0));
        colorTranslations.put('7', new Color(170,170,170));
        colorTranslations.put('8', new Color(85,85,85));
        colorTranslations.put('9', new Color(85,85,255));
        colorTranslations.put('a', new Color(85,255,85));
        colorTranslations.put('b', new Color(85,255,255));
        colorTranslations.put('c', new Color(255,85,85));
        colorTranslations.put('d', new Color(255,85,255));
        colorTranslations.put('e', new Color(255,255,85));
        colorTranslations.put('f', Color.WHITE);
        colorTranslations.put('r', Color.BLACK);
    }

    private BufferedImage image = null;
    private final BufferedImage reward = new BufferedImage(128, 14, BufferedImage.TYPE_INT_ARGB);
    private static final float MAX_FONT_SIZE = 20f;
    private double currentCost = -1;
    private final OfflinePlayer player;
    private static final float REWARD_FONT_SIZE = 13f;
    private long lastRender = System.currentTimeMillis();

    public Renderer(UUID uuid) {
        this.player = Bukkit.getOfflinePlayer(uuid);
        String name;
        if (BountyManager.hasBounty(uuid)) {
            Bounty bounty = BountyManager.getBounty(uuid);
            assert bounty != null;
            name = bounty.getName();
        } else {
            name = LoggedPlayers.getPlayerName(uuid);
        }
        File imageFile = new File(BountyMap.getPosterDirectory() + File.separator + name.toLowerCase() + ".png");
        if (BountyMap.isSaveTemplates() && imageFile.exists()) {
            try {
                image = ImageIO.read(imageFile);
            } catch (IOException e) {
                Bukkit.getLogger().warning(e.toString());
            }
            return;
        }
        RenderPoster renderPoster = new RenderPoster(name, this);
        renderPoster.setTaskImplementation(NotBounties.getServerImplementation().global().runAtFixedRate(renderPoster, 1, 40));

    }

    public void renderPoster(BufferedImage head, String name, boolean save) {
        if (image == null)
            image = BountyMap.deepCopy(BountyMap.getBountyPoster());
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(head, 32, 32, null);

        // center of text is y112
        // head display ends at y95
        float fontSize = MAX_FONT_SIZE;
        graphics.setFont(BountyMap.getPlayerFont(fontSize, true));
        String displayName = BountyMap.getNameLine().replace("{name}", (name));

        if (ConfigOptions.getIntegrations().isPapiEnabled())
            displayName = new PlaceholderAPIClass().parse(player, displayName);
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        int y = BountyMap.isDisplayReward() ? 112 : 120;
        if (BackupFontManager.isUsingTraditionalFont()) {
            try {
                int x = 64 - setBiggestFontSize(graphics, displayName, true, fontSize) / 2;
                drawColors(displayName, graphics, x, y);
            } catch (Throwable throwable) {
                // this usually happens when the server doesn't have a font configuration, or doesn't have access to it.
                Bukkit.getLogger().warning("[NotBounties] Unable to access font configuration on this system. Reverting to backup font!");
                BackupFontManager.loadBackupFonts();
                int x = 64 - BackupFontManager.getNameLine().getWidth(displayName) / 2;
                BackupFontManager.getNameLine().drawText(image, x, y, displayName);
            }
        } else {
            int x = 64 - BackupFontManager.getNameLine().getWidth(displayName) / 2;
            BackupFontManager.getNameLine().drawText(image, x, y, displayName);
        }

        graphics.dispose();

        File imageFile = new File(BountyMap.getPosterDirectory() + File.separator + name.toLowerCase() + ".png");

        if (BountyMap.isSaveTemplates() && save) {
            try {
                ImageIO.write(image, "PNG", imageFile);
            } catch (IOException e) {
                Bukkit.getLogger().warning(e.toString());
            }
        }
    }

    public double getBountyAmount() {
        if (BountyManager.hasBounty(player.getUniqueId())) {
            Bounty bounty = BountyManager.getBounty(player.getUniqueId());
            assert bounty != null;
            return bounty.getTotalDisplayBounty();
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
        for (int ix = 0; ix < Math.min(image.getWidth(), 128 - x); ix++) {
            for (int iy = 0; iy < Math.min(image.getHeight(), 128 - y); iy++) {
                Color color = getColor(image.getRGB(ix, iy));
                if (color.getAlpha() > 10) {
                    if (NotBounties.getServerVersion() >= 19)
                        canvas.setPixelColor(x + ix, y + iy, color);
                    else
                        //noinspection deprecation
                        canvas.setPixel(x + ix, y + iy, MapPalette.matchColor(color));

                }
            }
        }
    }


    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player renderer) {
        if (image == null)
            return;
        if (!map.isLocked() || currentCost == -1) {
            if (System.currentTimeMillis() - lastRender < BountyMap.getUpdateInterval())
                return;
            lastRender = System.currentTimeMillis();
            double bountyAmount = getBountyAmount();
            if (currentCost != bountyAmount || BountyMap.isAlwaysUpdate()) {
                // redraw canvas
                currentCost = bountyAmount;
                drawTransparentImage(0, 0, image, canvas);
                if (BountyMap.isDisplayReward()) {
                    Graphics2D graphics = reward.createGraphics();
                    graphics.setComposite(AlphaComposite.Clear);
                    graphics.fillRect(0, 0, reward.getWidth(), reward.getHeight());
                    graphics.setComposite(AlphaComposite.Src);
                    String rewardText = BountyMap.getRewardText();
                    String bountyText = BountyMap.isCurrencyWrap() ? NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(currentCost) + NumberFormatting.getCurrencySuffix() : NumberFormatting.formatNumber(currentCost);
                    rewardText = rewardText.replace("{reward}", (bountyText));
                    rewardText = LanguageOptions.parse(rewardText, player);
                    rewardText = ChatColor.translateAlternateColorCodes('&', rewardText);


                    int y = 12;
                    if (BackupFontManager.isUsingTraditionalFont()) {
                        try {
                            int x = 64 - setBiggestFontSize(graphics, rewardText, false, REWARD_FONT_SIZE) / 2;
                            drawColors(rewardText, graphics, x, y);
                        } catch (Throwable throwable) {
                            Bukkit.getLogger().warning("[NotBounties] Unable to access font configuration on this system. Reverting to backup font!");
                            BackupFontManager.loadBackupFonts();
                            int x = 64 - BackupFontManager.getRewardLine().getWidth(rewardText) / 2;
                            BackupFontManager.getRewardLine().drawText(reward, x, y, rewardText);
                        }
                    } else {
                        int x = 64 - BackupFontManager.getRewardLine().getWidth(rewardText) / 2;
                        BackupFontManager.getRewardLine().drawText(reward, x, y, rewardText);
                    }
                    drawTransparentImage(0, 114, reward, canvas);
                    graphics.dispose();
                }
                if (currentCost == 0) {
                    drawTransparentImage(64 - BountyMap.getDeadBounty().getWidth() / 2,64 - BountyMap.getDeadBounty().getHeight() / 2, BountyMap.getDeadBounty(), canvas);
                }
            }
        }



    }

    /**
     * Sets the size of the font in the graphics to be biggest it can between 1 and rewardFont
     * @param graphics The graphics to display the font with
     * @param text Text to be displayed
     * @return The width of the text
     */
    private int setBiggestFontSize(Graphics2D graphics, String text, boolean bold, float fontSize) {
        FontMetrics metrics = graphics.getFontMetrics();
        while (metrics.stringWidth(ChatColor.stripColor(text)) > 120 && fontSize > 1) {
            fontSize--;
            Font font = BountyMap.getPlayerFont(fontSize, bold);
            graphics.setFont(font);
            metrics = graphics.getFontMetrics();
        }
        return metrics.stringWidth(ChatColor.stripColor(text));
    }

    public static void drawColors(String text, Graphics2D graphics, int x, int y) {
        Color currentColor = Color.BLACK;
        StringBuilder currentText = new StringBuilder();
        FontMetrics metrics = graphics.getFontMetrics();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ChatColor.COLOR_CHAR) {
                char code = text.charAt(i+1);
                if (!colorTranslations.containsKey(code)) {
                    i++;
                    continue;
                }
                if (!currentText.isEmpty()) {
                    graphics.setColor(currentColor);
                    graphics.drawString(currentText.toString(), x, y);
                    x+= metrics.stringWidth(currentText.toString());
                }
                currentColor = colorTranslations.get(code);
                i++;
                currentText.delete(0, currentText.length());
            } else {
                currentText.append(c);
            }
            i++;
        }
        graphics.setColor(currentColor);
        graphics.drawString(currentText.toString(), x, y);
    }

    public OfflinePlayer getPlayer() {
        return player;
    }
}

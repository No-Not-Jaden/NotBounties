package me.jadenp.notbounties.ui.map;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.HeadFetcher;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.ConfigOptions;
import me.jadenp.notbounties.utils.NumberFormatting;
import me.jadenp.notbounties.utils.PlaceholderAPIClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

public class Renderer extends MapRenderer {
    private BufferedImage image = null;
    private final BufferedImage reward = new BufferedImage(128, 14, BufferedImage.TYPE_INT_ARGB);
    private static final float maxFont = 20f;
    private double currentCost = -1;
    private final OfflinePlayer player;
    private static final float rewardFont = 13f;
    private long lastRender = System.currentTimeMillis();

    public Renderer(UUID uuid) {
        this.player = Bukkit.getOfflinePlayer(uuid);
        String name;
        if (BountyManager.hasBounty(player)) {
            Bounty bounty = BountyManager.getBounty(player);
            assert bounty != null;
            name = bounty.getName();
        } else {
            name = NotBounties.getPlayerName(uuid);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                File imageFile = new File(BountyMap.posterDirectory + File.separator + name.toLowerCase() + ".png");
                if (ConfigOptions.saveTemplates) {
                    if (imageFile.exists()) {
                        try {
                            image = ImageIO.read(imageFile);
                        } catch (IOException e) {
                            Bukkit.getLogger().warning(e.toString());
                        }
                        return;
                    }
                }
                if (image == null)
                    image = BountyMap.deepCopy(BountyMap.bountyPoster);
                Graphics2D graphics = image.createGraphics();
                BufferedImage head = new HeadFetcher().getPlayerFace(uuid);
                /*if (head == null) {
                    try {
                        head = ImageIO.read(new URL("https://cravatar.eu/helmavatar/" + uuid + "/64.png"));
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(e.toString());
                    }
                }*/
                graphics.drawImage(head, 32, 32, null);

                // center of text is y112
                // head display ends at y95
                float fontSize = maxFont;
                graphics.setFont(BountyMap.getPlayerFont(fontSize, true));
                String displayName = ConfigOptions.nameLine.replaceAll("\\{name}", Matcher.quoteReplacement(name));

                if (ConfigOptions.papiEnabled)
                    displayName = new PlaceholderAPIClass().parse(player, displayName);
                displayName = ChatColor.translateAlternateColorCodes('&', displayName);
                int y = ConfigOptions.displayReward ? 112 : 120;
                if (BackupFontManager.isUsingTraditionalFont()) {
                    try {
                        int x = 64 - setBiggestFontSize(graphics, displayName, true, fontSize) / 2;
                        drawColors(displayName, graphics, x, y);
                    } catch (Throwable throwable) {
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

                if (ConfigOptions.saveTemplates) {
                    try {
                        ImageIO.write(image, "PNG", imageFile);
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(e.toString());
                    }
                }
            }
        }.runTaskAsynchronously(NotBounties.getInstance());
    }

    public double getBountyAmount() {
        if (BountyManager.hasBounty(player)) {
            Bounty bounty = BountyManager.getBounty(player);
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
                    if (NotBounties.serverVersion >= 19)
                        canvas.setPixelColor(x + ix, y + iy, color);
                    else
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
            if (System.currentTimeMillis() - lastRender < ConfigOptions.updateInterval)
                return;
            lastRender = System.currentTimeMillis();
            double bountyAmount = getBountyAmount();
            if (currentCost != bountyAmount || ConfigOptions.alwaysUpdate) {
                // redraw canvas
                currentCost = bountyAmount;
                canvas.drawImage(0, 0, image);
                if (ConfigOptions.displayReward) {
                    Graphics2D graphics = reward.createGraphics();
                    graphics.setComposite(AlphaComposite.Clear);
                    graphics.fillRect(0, 0, reward.getWidth(), reward.getHeight());
                    graphics.setComposite(AlphaComposite.Src);
                    String rewardText = ConfigOptions.rewardText;
                    String bountyText = ConfigOptions.currencyWrap ? NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(currentCost) + NumberFormatting.currencySuffix : NumberFormatting.formatNumber(currentCost);
                    rewardText = rewardText.replaceAll("\\{reward}", Matcher.quoteReplacement(bountyText));
                    if (ConfigOptions.papiEnabled)
                        rewardText = new PlaceholderAPIClass().parse(player, rewardText);
                    rewardText = ChatColor.translateAlternateColorCodes('&', rewardText);


                    int y = 12;
                    if (BackupFontManager.isUsingTraditionalFont()) {
                        try {
                            int x = 64 - setBiggestFontSize(graphics, rewardText, false, rewardFont) / 2;
                            drawColors(rewardText, graphics, x, y);
                        } catch (Throwable throwable) {
                            Bukkit.getLogger().warning("[NotBounties] Unable to access font configuration on this system. Reverting to backup font!");
                            BackupFontManager.loadBackupFonts();
                        }
                    } else {
                        int x = 64 - BackupFontManager.getRewardLine().getWidth(rewardText) / 2;
                        BackupFontManager.getRewardLine().drawText(reward, x, y, rewardText);
                    }
                    drawTransparentImage(0, 114, reward, canvas);
                    graphics.dispose();
                }
                if (currentCost == 0) {
                    drawTransparentImage(28,28, BountyMap.deadBounty, canvas);
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
    public static final Map<Character, Color> colorTranslations = new HashMap<>();
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
    public static void drawColors(String text, Graphics2D graphics, int x, int y) {
        Color currentColor = Color.BLACK;
        String currentText = "";
        FontMetrics metrics = graphics.getFontMetrics();
        //Bukkit.getLogger().info("text: " + text);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            //Bukkit.getLogger().info("Char: " + c);
            if (c == ChatColor.COLOR_CHAR) {
                char code = text.charAt(i+1);
                if (!colorTranslations.containsKey(code))
                    continue;
                if (!currentText.isEmpty()) {
                    graphics.setColor(currentColor);
                    graphics.drawString(currentText, x, y);
                    x+= metrics.stringWidth(currentText);
                    //Bukkit.getLogger().info(currentText);
                }
                currentColor = colorTranslations.get(code);
                i++;
                currentText = "";
            } else {
                currentText = currentText + c;
            }
        }
        graphics.setColor(currentColor);
        graphics.drawString(currentText, x, y);
    }
}

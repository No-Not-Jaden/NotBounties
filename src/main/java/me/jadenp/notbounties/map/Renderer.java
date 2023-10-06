package me.jadenp.notbounties.map;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.ConfigOptions;
import me.jadenp.notbounties.utils.NumberFormatting;
import me.jadenp.notbounties.utils.PlaceholderAPIClass;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

public class Renderer extends MapRenderer {
    private final BufferedImage image;
    private final BufferedImage reward = new BufferedImage(128, 14, BufferedImage.TYPE_INT_ARGB);
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
        if (ConfigOptions.saveTemplates) {
            if (imageFile.exists()) {
                image = ImageIO.read(imageFile);
                return;
            }
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
        String displayName = ConfigOptions.nameLine.replaceAll("\\{name}", Matcher.quoteReplacement(name));
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        if (ConfigOptions.papiEnabled)
            displayName = new PlaceholderAPIClass().parse(player, displayName);
        while (metrics.stringWidth(ChatColor.stripColor(displayName)) > 120 && fontSize > 1) {
            fontSize--;
            graphics.setFont(BountyMap.getPlayerFont(fontSize, true));
            metrics = graphics.getFontMetrics();
        }
        int x = 64 - metrics.stringWidth(ChatColor.stripColor(displayName)) / 2; // center - width/2
        int y = ConfigOptions.displayReward ? 112 : 112 + metrics.getHeight() / 2;
        drawColors(displayName, graphics, x, y);

        if (ConfigOptions.saveTemplates)
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
                    rewardText = ChatColor.translateAlternateColorCodes('&', rewardText);
                    if (ConfigOptions.papiEnabled)
                        rewardText = new PlaceholderAPIClass().parse(player, rewardText);
                    FontMetrics metrics = graphics.getFontMetrics();
                    float fontSize = rewardFont;
                    while (metrics.stringWidth(ChatColor.stripColor(rewardText)) > 120 && fontSize > 1) {
                        fontSize--;
                        graphics.setFont(BountyMap.getPlayerFont(fontSize, false));
                        metrics = graphics.getFontMetrics();
                    }
                    int x = 64 - metrics.stringWidth(ChatColor.stripColor(rewardText)) / 2;
                    int y = 12;
                    drawColors(rewardText, graphics, x, y);
                    drawTransparentImage(0, 114, reward, canvas);

                }
                if (currentCost == 0) {
                    drawTransparentImage(28,28, BountyMap.deadBounty, canvas);
                }
            }
        }



    }
    private static final Map<Character, Color> colorTranslations = new HashMap<>();
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

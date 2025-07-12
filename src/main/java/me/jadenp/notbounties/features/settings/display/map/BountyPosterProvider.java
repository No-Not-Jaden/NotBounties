package me.jadenp.notbounties.features.settings.display.map;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.integrations.external_api.PlaceholderAPIClass;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.utils.tasks.RenderPoster;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BountyPosterProvider {
    /**
     * Translations for a chat color to an RGB color.
     */
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

    protected BufferedImage playerFace = null; // null face = no skin = draw global skin
    protected BufferedImage background = null;
    protected OfflinePlayer player;
    protected final BufferedImage reward = new BufferedImage(128, 14, BufferedImage.TYPE_INT_ARGB);
    protected static final float MAX_FONT_SIZE = 20f;
    protected double currentCost = -1;
    protected static final float REWARD_FONT_SIZE = 13f;
    protected long lastRender = System.currentTimeMillis();
    protected final Plugin plugin;
    protected boolean loaded = false;

    protected BountyPosterProvider(UUID uuid, Plugin plugin) {
        this.plugin = plugin;
        this.player = Bukkit.getOfflinePlayer(uuid);
        String name;
        Bounty bounty = BountyManager.getBounty(uuid);
        if (bounty != null) {
            name = bounty.getName();
        } else {
            name = LoggedPlayers.getPlayerName(uuid);
        }

        if (BountyMap.isSaveTemplates()) {
            File faceFile = new File(BountyMap.getPosterDirectory() + File.separator + name.toLowerCase() + " face.png");
            if (faceFile.exists()) {
                try {
                    playerFace = ImageIO.read(faceFile);
                } catch (IOException e) {
                    plugin.getLogger().warning(e.toString());
                }
            }
            File backgroundFile = new File(BountyMap.getPosterDirectory() + File.separator + name.toLowerCase() + " background.png");
            if (backgroundFile.exists()) {
                try {
                    background = ImageIO.read(backgroundFile);
                } catch (IOException e) {
                    plugin.getLogger().warning(e.toString());
                }
            }
        }

        // render image in async
        RenderPoster renderPoster = new RenderPoster(name, this);
        renderPoster.setTaskImplementation(NotBounties.getServerImplementation().global().runAtFixedRate(renderPoster, 1, 40));
    }

    /**
     * Check if the player's face has been loaded.
     * @return if the player's face has been loaded.
     */
    public boolean isPlayerFacePresent() {
        return playerFace != null;
    }

    /**
     * Set the player face for this map.
     * @param head 8x8 image for the player face.
     */
    public void setPlayerFace(BufferedImage head, String name) {
        this.playerFace = head;
        File imageFile = new File(BountyMap.getPosterDirectory() + File.separator + name.toLowerCase() + " face.png");

        if (BountyMap.isSaveTemplates()) {
            try {
                ImageIO.write(head, "PNG", imageFile);
            } catch (IOException e) {
                plugin.getLogger().warning(e.toString());
            }
        }
    }

    /**
     * Get the player face to draw. If no player face has been set, the missing skin face will be drawn.
     * @return The player face for this poster.
     */
    public BufferedImage getPlayerFace() {
        return playerFace != null ? playerFace : SkinManager.getPlayerFace(DataManager.GLOBAL_SERVER_ID);
    }

    /**
     * If the poster provider is missing elements. The most basic element is the player's face, but other poster
     * providers may override this method to add additional requirements before loading the poster.
     * @return True if one or more elements are missing.
     */
    public boolean isMissingElements() {
        return !isPlayerFacePresent() && !SkinManager.isSkinLoaded(player.getUniqueId());
    }

    /**
     * Render the background of the poster. The background is the bounty poster template with the player's name.
     * @param name Name of the player.
     */
    public void generateBackground(String name) {
        // copy background
        background = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = background.createGraphics();
        graphics.drawImage(BountyMap.getBountyPoster(), 0, 0, 128, 128, null);

        // center of text is y112
        // head display ends at y95
        float fontSize = MAX_FONT_SIZE;
        graphics.setFont(BountyMap.getPlayerFont(fontSize, true));
        String displayName = BountyMap.getNameLine().replace("{name}", name);

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
                BackupFontManager.loadBackupFonts();
                int x = 64 - BackupFontManager.getNameLine().getWidth(displayName) / 2;
                BackupFontManager.getNameLine().drawText(background, x, y, displayName);
            }
        } else {
            int x = 64 - BackupFontManager.getNameLine().getWidth(displayName) / 2;
            BackupFontManager.getNameLine().drawText(background, x, y, displayName);
        }

        graphics.dispose();

        File imageFile = new File(BountyMap.getPosterDirectory() + File.separator + name.toLowerCase() + " background.png");

        if (BountyMap.isSaveTemplates()) {
            try {
                ImageIO.write(background, "PNG", imageFile);
            } catch (IOException e) {
                plugin.getLogger().warning(e.toString());
            }
        }

        if (isPlayerFacePresent())
            loaded = true;
    }

    protected void render() {
        if ((BountyMap.isLockMaps() && currentCost != -1)
            || System.currentTimeMillis() - lastRender < BountyMap.getUpdateInterval()
            || playerFace == null || background == null)
            return;
        lastRender = System.currentTimeMillis();
        double bountyAmount = getBountyAmount();
        if (currentCost != bountyAmount || BountyMap.isAlwaysUpdate()) {
            // update poster
            if (currentCost == -1 || getPixelColor(72,72) == null) {
                // first render - draw background
                drawBackground();
                drawPlayerFace();
            }
            currentCost = bountyAmount;
            if (BountyMap.isDisplayReward()) {
                drawReward();
            }
            if (currentCost == 0) {
                drawTransparentImage(64 - BountyMap.getDeadBounty().getWidth() / 2,64 - BountyMap.getDeadBounty().getHeight() / 2, BountyMap.getDeadBounty());
            }
        }
    }

    protected Color getColor(int argb) {
        int b = (argb)&0xFF;
        int g = (argb>>8)&0xFF;
        int r = (argb>>16)&0xFF;
        int a = (argb>>24)&0xFF;
        return new Color(r,g,b,a);
    }

    /**
     * Draws the player's face in the center of the map.
     */
    protected void drawPlayerFace() {
        // fallback to missing skin if image not loaded
        BufferedImage face = getPlayerFace();
        if (face == null)
            return;
        for (int faceX = 0; faceX < 8; faceX++) {
            for (int faceY = 0; faceY < 8; faceY++) {
                drawRectangle(faceX * 8 + 32, faceY * 8 + 32, 8, 8, getColor(face.getRGB(faceX, faceY)));
            }
        }
    }

    /**
     * Draws the background onto the canvas excluding the area for the player face.
     */
    protected void drawBackground() {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                if (x < 32 || x >= 96 || y < 32 || y >= 96) {
                    Color color = getColor(background.getRGB(x, y));
                    setPixel(x, y, color);
                }
            }
        }
    }

    protected void drawReward() {
        Graphics2D graphics = reward.createGraphics();
        // clear reward space
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, reward.getWidth(), reward.getHeight());
        // parse reward text
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
                BackupFontManager.loadBackupFonts();
                int x = 64 - BackupFontManager.getRewardLine().getWidth(rewardText) / 2;
                BackupFontManager.getRewardLine().drawText(reward, x, y, rewardText);
            }
        } else {
            int x = 64 - BackupFontManager.getRewardLine().getWidth(rewardText) / 2;
            BackupFontManager.getRewardLine().drawText(reward, x, y, rewardText);
        }

        // draw background to cover the old reward
        for (int ix = 0; ix < 128; ix++) {
            for (int iy = 114; iy < 128; iy++) {
                Color color = getColor(background.getRGB(ix, iy));
                setPixel(ix, iy, color);
            }
        }

        drawTransparentImage(0, 114, reward);
        graphics.dispose();
    }

    protected void drawTransparentImage(int x, int y, BufferedImage image) {
        for (int ix = 0; ix < Math.min(image.getWidth(), 128 - x); ix++) {
            for (int iy = 0; iy < Math.min(image.getHeight(), 128 - y); iy++) {
                Color color = getColor(image.getRGB(ix, iy));
                setPixel(x + ix, y + iy, color);
            }
        }

    }

    protected abstract void setPixel(int x, int y, Color color);

    protected abstract void drawRectangle(int x, int y, int w, int h, Color color);

    protected abstract @Nullable Color getPixelColor(int x, int y);

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

    public double getBountyAmount() {
        if (BountyManager.hasBounty(player.getUniqueId())) {
            Bounty bounty = BountyManager.getBounty(player.getUniqueId());
            assert bounty != null;
            return bounty.getTotalDisplayBounty();
        }
        return 0;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }
}

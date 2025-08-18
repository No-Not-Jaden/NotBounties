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


    // ChatGPT modified the following function and its helpers
    public static void drawColors(String text, Graphics2D graphics, int x, int y) {
        Color currentColor = Color.BLACK; // start color
        StringBuilder currentText = new StringBuilder();
        FontMetrics metrics = graphics.getFontMetrics();

        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);

            // Consider both § and & as color introducers
            if ((ch == ChatColor.COLOR_CHAR) || (ch == '&')) {
                // --- 1) Bungee verbose hex: §x§R§R§G§G§B§B (or &x&R&R&G&G&B&B) ---
                if (i + 13 < text.length()) { // need 14 chars total
                    char next = text.charAt(i + 1);
                    if (next == 'x' || next == 'X') {
                        if (isVerboseHexAt(text, i, ch)) {
                            // flush current run
                            if (!currentText.isEmpty()) {
                                graphics.setColor(currentColor);
                                graphics.drawString(currentText.toString(), x, y);
                                x += metrics.stringWidth(currentText.toString());
                                currentText.setLength(0);
                            }
                            String hex = "" +
                                    text.charAt(i + 3) + text.charAt(i + 5) +
                                    text.charAt(i + 7) + text.charAt(i + 9) +
                                    text.charAt(i + 11) + text.charAt(i + 13);
                            currentColor = parseHexColor(hex);

                            // consume the whole verbose sequence (14 chars)
                            i += 13; // loop's i++ will land us after it
                            i++;
                            continue;
                        }
                    }
                }

                // --- 2) Legacy color/reset/formatting: §a, §f, §r etc. (also &a) ---
                if (i + 1 < text.length()) {
                    char code = Character.toLowerCase(text.charAt(i + 1));

                    // legacy color code
                    if (colorTranslations.containsKey(code)) {
                        if (!currentText.isEmpty()) {
                            graphics.setColor(currentColor);
                            graphics.drawString(currentText.toString(), x, y);
                            x += metrics.stringWidth(currentText.toString());
                            currentText.setLength(0);
                        }
                        currentColor = colorTranslations.get(code);
                        i += 2; // consume introducer + code
                        continue;
                    }

                    // reset (§r / &r) — if you keep 'r' in the map, the branch above already handled it.
                    if (code == 'r') {
                        if (!currentText.isEmpty()) {
                            graphics.setColor(currentColor);
                            graphics.drawString(currentText.toString(), x, y);
                            x += metrics.stringWidth(currentText.toString());
                            currentText.setLength(0);
                        }
                        currentColor = colorTranslations.getOrDefault('r', Color.BLACK);
                        i += 2;
                        continue;
                    }

                    // formatting codes (l, m, n, o, k) — ignore for color, just skip
                    if ("klmno".indexOf(code) >= 0) {
                        i += 2;
                        continue;
                    }

                    // unknown code -> drop introducer and move on (matches your current behavior)
                    i++; // skip introducer only
                    continue;
                }
            }

            // Normal character
            currentText.append(ch);
            i++;
        }

        // draw trailing text
        if (!currentText.isEmpty()) {
            graphics.setColor(currentColor);
            graphics.drawString(currentText.toString(), x, y);
        }
    }

    private static boolean isVerboseHexAt(String s, int i, char introducer) {
        // Expect: [i] introducer, [i+1] 'x', then 6 times: introducer + hexDigit
        if (i + 13 >= s.length()) return false;
        if (s.charAt(i) != introducer) return false;
        char x = s.charAt(i + 1);
        if (!(x == 'x' || x == 'X')) return false;

        // positions of the 6 introducers and 6 hex digits
        int[] introPos = {i + 2, i + 4, i + 6, i + 8, i + 10, i + 12};
        int[] digitPos = {i + 3, i + 5, i + 7, i + 9, i + 11, i + 13};

        for (int p : introPos) {
            if (p >= s.length() || s.charAt(p) != introducer) return false;
        }
        for (int p : digitPos) {
            if (p >= s.length() || !isHexDigit(s.charAt(p))) return false;
        }
        return true;
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f') ||
                (c >= 'A' && c <= 'F');
    }

    private static Color parseHexColor(String hex6) {
        int rgb = Integer.parseInt(hex6, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new Color(r, g, b);
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

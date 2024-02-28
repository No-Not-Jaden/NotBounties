package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class HeadFetcher {
    private static final Map<UUID, ItemStack> savedHeads = new HashMap<>();

    public HeadFetcher(){}
    public void loadHeads(Player player, PlayerGUInfo guInfo, LinkedHashMap<OfflinePlayer, ItemStack> heads) {
        ItemStack[] fetchedHeads = new ItemStack[heads.size()];
        new BukkitRunnable() {
            @Override
            public void run() {
                int i = 0;
                for (Map.Entry<OfflinePlayer, ItemStack> entry : heads.entrySet()) {
                    if (savedHeads.containsKey(entry.getKey().getUniqueId())) {
                        fetchedHeads[i] = savedHeads.get(entry.getKey().getUniqueId());
                    } else {
                        ItemStack head = copyItemText(entry.getValue(), Head.createPlayerSkull(entry.getKey().getUniqueId()));
                        fetchedHeads[i] = head;
                        savedHeads.put(entry.getKey().getUniqueId(), head);
                    }
                    i++;
                }

                if (player.isOnline() && player.getOpenInventory().getType() == InventoryType.CHEST && GUI.playerInfo.containsKey(player.getUniqueId())) {
                    PlayerGUInfo currentInfo = GUI.playerInfo.get(player.getUniqueId());
                    if (!currentInfo.getGuiType().equals(guInfo.getGuiType()) || currentInfo.getPage() != guInfo.getPage())
                        return;
                    GUIOptions guiOptions = GUI.getGUI(guInfo.getGuiType());
                    if (guiOptions == null)
                        return;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Inventory inventory = player.getOpenInventory().getTopInventory();
                            ItemStack[] contents = inventory.getContents();
                            for (int j = 0; j < Math.min(guiOptions.getPlayerSlots().size(), fetchedHeads.length); j++) {
                                int slot = guiOptions.getPlayerSlots().get(j);
                                contents[slot] = fetchedHeads[j];
                            }
                            inventory.setContents(contents);
                        }
                    }.runTask(NotBounties.getInstance());

                }

            }
        }.runTaskLaterAsynchronously(NotBounties.getInstance(), 1);
    }

    public BufferedImage getPlayerFace(UUID uuid) {

        String name = NotBounties.getPlayerName(uuid);
        try {
            URL textureUrl = null;
            if (ConfigOptions.skinsRestorerEnabled)
                textureUrl = new URL(ConfigOptions.skinsRestorerClass.getSkinTextureURL(uuid, name));
            if (textureUrl == null)
                textureUrl = Head.getTextureURL(uuid);
            //Bukkit.getLogger().info("URL: " + textureUrl);
            if (textureUrl == null)
                return null;
            BufferedImage skin = ImageIO.read(textureUrl);
            BufferedImage head = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            try {
                // base head
                copySquare(skin, head, 8,8);
                // mask
                copySquare(skin, head, 40, 8);
            } catch (IndexOutOfBoundsException e) {
                Bukkit.getLogger().warning(e.toString());
                return null;
            }
            return head;

        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
        }
        return null;
    }

    private void copySquare(BufferedImage from, BufferedImage to, int x, int y) {
        for (int x1 = 0; x1 < 8; x1++) {
            for (int y1 = 0; y1 < 8; y1++) {
                int color = from.getRGB(x + x1, y + y1);
                int a = (color>>24)&0xFF;
                if (a == 0)
                    continue;
                for (int x2 = 0; x2 < 8; x2++) {
                    for (int y2 = 0; y2 < 8; y2++) {
                        to.setRGB(x1 * 8 + x2, y1 * 8 + y2, color);
                    }
                }

            }
        }
    }

    private ItemStack copyItemText(ItemStack from, ItemStack to) {
        ItemMeta fromMeta = from.getItemMeta();
        ItemMeta toMeta = to.getItemMeta();
        assert fromMeta != null;
        assert toMeta != null;
        if (fromMeta.hasDisplayName())
            toMeta.setDisplayName(fromMeta.getDisplayName());
        if (fromMeta.hasLore())
            toMeta.setLore(fromMeta.getLore());
        to.setItemMeta(toMeta);
        return to;
    }

    public static ItemStack getUnloadedHead(UUID uuid) {
        if (savedHeads.containsKey(uuid))
            return savedHeads.get(uuid);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        } catch (NullPointerException e) {
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid, NotBounties.getPlayerName(uuid));
            meta.setOwnerProfile(profile);
        }
        head.setItemMeta(meta);
        return head;
    }
}

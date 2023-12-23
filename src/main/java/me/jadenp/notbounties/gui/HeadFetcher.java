package me.jadenp.notbounties.gui;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.Head;
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
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid, NotBounties.getInstance().getPlayerName(uuid));
            meta.setOwnerProfile(profile);
        }
        head.setItemMeta(meta);
        return head;
    }
}

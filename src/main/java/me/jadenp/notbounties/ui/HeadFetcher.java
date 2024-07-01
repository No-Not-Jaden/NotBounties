package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
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

import java.util.*;

public class HeadFetcher {
    private static final Map<UUID, ItemStack> savedHeads = new HashMap<>();


    public HeadFetcher() {
    }

    public void loadHeads(Player player, PlayerGUInfo guInfo, LinkedHashMap<OfflinePlayer, ItemStack> heads) {

        new BukkitRunnable() {
            int maxRequests = 100;
            final ItemStack[] fetchedHeads = new ItemStack[heads.size()];
            @Override
            public void run() {
                boolean[] headsToUpdate = new boolean[heads.size()];
                int i = 0;
                for (Map.Entry<OfflinePlayer, ItemStack> entry : heads.entrySet()) {
                    if (fetchedHeads[i] == null) {
                        if (savedHeads.containsKey(entry.getKey().getUniqueId())) {
                            fetchedHeads[i] = savedHeads.get(entry.getKey().getUniqueId());
                            headsToUpdate[i] = true;
                        } else {
                            if (SkinManager.isSkinLoaded(entry.getKey().getUniqueId())) {
                                PlayerSkin playerSkin = SkinManager.getSkin(entry.getKey().getUniqueId());
                                ItemStack head = copyItemText(entry.getValue(), Head.createPlayerSkull(entry.getKey().getUniqueId(), playerSkin.getUrl()));
                                fetchedHeads[i] = head;
                                if (!SkinManager.isMissingSkin(playerSkin)) {
                                    // do not update head if the skin is missing.
                                    headsToUpdate[i] = true;
                                    savedHeads.put(entry.getKey().getUniqueId(), head);
                                }
                            }
                        }
                    }
                    i++;
                }

                // update inventory
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline() && player.getOpenInventory().getType() == InventoryType.CHEST && GUI.playerInfo.containsKey(player.getUniqueId())) {
                            PlayerGUInfo currentInfo = GUI.playerInfo.get(player.getUniqueId());
                            if (!currentInfo.getGuiType().equals(guInfo.getGuiType()) || currentInfo.getPage() != guInfo.getPage() || !currentInfo.getTitle().equals(guInfo.getTitle())) {
                                // no longer in same GUI
                                maxRequests = 0;
                                return;
                            }
                            GUIOptions guiOptions = GUI.getGUI(guInfo.getGuiType());
                            if (guiOptions == null)
                                return;
                            Inventory inventory = player.getOpenInventory().getTopInventory();
                            ItemStack[] contents = inventory.getContents();
                            for (int j = 0; j < Math.min(guiOptions.getPlayerSlots().size(), fetchedHeads.length); j++) {
                                if (!headsToUpdate[j])
                                    continue;
                                int slot = guiOptions.getPlayerSlots().get(j);
                                contents[slot] = fetchedHeads[j];
                            }
                            inventory.setContents(contents);
                        } else {
                            maxRequests = 0;
                            if (NotBounties.debug)
                                Bukkit.getLogger().info("[NotBountiesDebug] Player exited GUI while loading player heads.");
                        }

                    }
                }.runTask(NotBounties.getInstance());
                // check if max requests hit
                if (maxRequests <= 0) {
                    this.cancel();
                    if (NotBounties.debug) {
                        i = 0;
                        for (Map.Entry<OfflinePlayer, ItemStack> entry : heads.entrySet()) {
                            if (fetchedHeads[i] == null) {
                                NotBounties.debugMessage("[NotBountiesDebug] Timed out loading skin for " + NotBounties.getPlayerName(entry.getKey().getUniqueId()), true);
                            }
                            i++;
                        }
                    }
                }
                maxRequests--;
                // check if all heads are loaded
                for (ItemStack itemStack : fetchedHeads)
                    if (itemStack == null)
                        return;
                this.cancel();
            }
        }.runTaskTimerAsynchronously(NotBounties.getInstance(), 1, 4);
    }



    private ItemStack copyItemText(ItemStack from, ItemStack to) {
        ItemMeta fromMeta = from.getItemMeta();
        ItemMeta toMeta = to.getItemMeta();
        if (fromMeta == null || toMeta == null)
            return to;
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
            if (NotBounties.serverVersion >= 18) {
                try {
                    PlayerProfile profile = Bukkit.createPlayerProfile(uuid, NotBounties.getPlayerName(uuid));
                    meta.setOwnerProfile(profile);
                } catch (IllegalArgumentException ignored) {
                    // The name of the profile is longer than 16 characters
                    if (NotBounties.debug)
                        Bukkit.getLogger().info("Could not get an unloaded head for: " + NotBounties.getPlayerName(uuid));
                }
            }
        }
        head.setItemMeta(meta);
        return head;
    }
}

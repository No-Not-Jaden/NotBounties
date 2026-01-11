package me.jadenp.notbounties.utils.tasks;

import com.google.common.cache.Cache;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.PlayerSkin;
import me.jadenp.notbounties.ui.QueuedHead;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.CompatabilityUtils;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class HeadLoader extends CancelableTask{
    private int maxRequests = 100;
    private final ItemStack[] fetchedHeads;
    private final List<QueuedHead> heads;
    private final PlayerGUInfo guInfo;
    private final Player player;
    private final Cache<UUID, ItemStack> savedHeads;

    public HeadLoader(Player player, PlayerGUInfo guInfo, List<QueuedHead> heads, Cache<UUID, ItemStack> savedHeads) {
        super();
        fetchedHeads = new ItemStack[heads.size()];
        this.player = player;
        this.guInfo = guInfo;
        this.heads = heads;
        this.savedHeads = savedHeads;
    }

    @Override
    public void run() {
        boolean[] headsToUpdate = new boolean[heads.size()];
        int i = 0;
        for (QueuedHead queuedHead : heads) {
            if (fetchedHeads[i] == null) {
                ItemStack cachedHead = savedHeads.getIfPresent(queuedHead.uuid());
                if (cachedHead != null) {
                    fetchedHeads[i] = cachedHead;
                    headsToUpdate[i] = true;
                } else {
                    if (SkinManager.isSkinLoaded(queuedHead.uuid())) {
                        PlayerSkin playerSkin = SkinManager.getSkin(queuedHead.uuid());
                        ItemStack head = copyItemText(queuedHead.itemStack(), Head.createPlayerSkull(queuedHead.uuid(), playerSkin.url()));
                        fetchedHeads[i] = head;
                        if (!SkinManager.isMissingSkin(playerSkin)) {
                            // do not update head if the skin is missing.
                            headsToUpdate[i] = true;
                            savedHeads.put(queuedHead.uuid(), head);
                        }
                    }
                }
            }
            i++;
        }

        // update inventory
        NotBounties.getServerImplementation().entity(player).run(() -> {
            if (player.isOnline() && GUI.playerInfo.containsKey(player.getUniqueId())) {
                PlayerGUInfo currentInfo = GUI.playerInfo.get(player.getUniqueId());
                if (!currentInfo.guiType().equals(guInfo.guiType()) || currentInfo.page() != guInfo.page() || !currentInfo.title().equals(guInfo.title())) {
                    // no longer in same GUI
                    maxRequests = 0;
                    return;
                }
                GUIOptions guiOptions = GUI.getGUI(guInfo.guiType());
                if (guiOptions == null || CompatabilityUtils.getType(player) != guiOptions.getInventoryType()) {
                    // invalid guiOptions
                    maxRequests = 0;
                    return;
                }
                Inventory inventory = CompatabilityUtils.getTopInventory(player);
                ItemStack[] contents = inventory.getContents();
                for (int j = 0; j < Math.min(guiOptions.getPlayerSlots().size(), fetchedHeads.length); j++) {
                    QueuedHead head = heads.get(j);
                    if (!headsToUpdate[j] || !guiOptions.getPlayerSlots().contains(head.slot()))
                        continue;
                    contents[head.slot()] = fetchedHeads[j];
                }
                inventory.setContents(contents);
            } else {
                maxRequests = 0;
                NotBounties.debugMessage("Player exited GUI while loading player heads.", false);
            }
        });
        // check if max requests hit
        if (maxRequests <= 0) {
            this.cancel();
            if (NotBounties.isDebug()) {
                i = 0;
                for (QueuedHead queuedHead : heads) {
                    if (fetchedHeads[i] == null) {
                        NotBounties.debugMessage("[NotBountiesDebug] Timed out loading skin for " + LoggedPlayers.getPlayerName(queuedHead.uuid()), true);
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

    private ItemStack copyItemText(ItemStack from, ItemStack to) {
        ItemMeta fromMeta = from.getItemMeta();
        ItemMeta toMeta = to.getItemMeta();
        if (fromMeta == null || toMeta == null)
            return to;

        if (NotBounties.isAboveVersion(21, 3)) {
            if (fromMeta.hasItemModel()) {
                toMeta.setItemModel(fromMeta.getItemModel());
            }
        } else if (fromMeta.hasCustomModelData()) {
            toMeta.setCustomModelData(fromMeta.getCustomModelData());
        }
        if (fromMeta.hasDisplayName())
            toMeta.setDisplayName(fromMeta.getDisplayName());
        if (fromMeta.hasLore())
            toMeta.setLore(fromMeta.getLore());
        to.setItemMeta(toMeta);
        return to;
    }
}

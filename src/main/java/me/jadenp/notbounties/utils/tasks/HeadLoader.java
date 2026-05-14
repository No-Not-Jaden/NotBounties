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

import java.util.*;

public class HeadLoader extends CancelableTask{
    private int maxRequests = 100;
    private final ItemStack[] fetchedHeads;
    private final List<QueuedHead> heads;
    private final PlayerGUInfo guInfo;
    private final Player player;
    private final Cache<UUID, ItemStack> savedHeads;
    private final HeadStatus[] headLoadingStatus;
    private boolean runningCheck = false;

    private enum HeadStatus {
        UNLOADED,
        SKIN_LOADED,
        HEAD_CACHED,
        READY_TO_APPLY,
        COMPLETED
    }

    public HeadLoader(Player player, PlayerGUInfo guInfo, List<QueuedHead> heads, Cache<UUID, ItemStack> savedHeads) {
        super();
        fetchedHeads = new ItemStack[heads.size()];
        this.player = player;
        this.guInfo = guInfo;
        this.heads = heads;
        this.savedHeads = savedHeads;
        HeadStatus[] tempHeadLoadingStatus = new HeadStatus[heads.size()];
        Arrays.fill(tempHeadLoadingStatus, HeadStatus.UNLOADED);
        headLoadingStatus = tempHeadLoadingStatus;
    }

    @Override
    public void run() {
        if (runningCheck)
            return;
        runningCheck = true;
        checkUnloadedHeads();

        NotBounties.getServerImplementation().entity(player).run(() -> {
            fetchReadyHeads();
            if (!updateInventory()) {
                cancelWithDebug();
                return;
            }

            if (allCompleted()) {
                cancel();
                return;
            }

            // check if max requests hit
            if (--maxRequests <= 0) {
                cancelWithDebug();
            }

            runningCheck = false;
        });
    }

    private boolean allCompleted() {
        for (HeadStatus loadingStatus : headLoadingStatus) {
            if (loadingStatus != HeadStatus.COMPLETED)
                return false;
        }
        return true;
    }

    private void cancelWithDebug() {
        this.cancel();
        if (NotBounties.isDebug()) {
            int i = 0;
            for (QueuedHead queuedHead : heads) {
                if (headLoadingStatus[i] != HeadStatus.COMPLETED) {
                    NotBounties.debugMessage("Timed out loading skin for " + LoggedPlayers.getPlayerName(queuedHead.uuid()) + " Status: " + headLoadingStatus[i], true);
                }
                i++;
            }
        }
    }

    private void checkUnloadedHeads() {
        for (int i = 0; i < heads.size(); i++) {
            if (headLoadingStatus[i] != HeadStatus.UNLOADED)
                continue;
            QueuedHead queuedHead = heads.get(i);
            ItemStack cachedHead = savedHeads.getIfPresent(queuedHead.uuid());
            if (cachedHead != null) {
                fetchedHeads[i] = cachedHead;
                headLoadingStatus[i] = HeadStatus.HEAD_CACHED;
            } else {
                if (SkinManager.isSkinLoaded(queuedHead.uuid())) {
                    PlayerSkin playerSkin = SkinManager.getSkin(queuedHead.uuid());
                    if (!SkinManager.isMissingSkin(playerSkin)) {
                        // do not update head if the skin is missing.
                        headLoadingStatus[i] = HeadStatus.SKIN_LOADED;
                    } else {
                        NotBounties.debugMessage("(Loading Head) Missing skin for " + LoggedPlayers.getPlayerName(queuedHead.uuid()), false);
                        fetchedHeads[i] = queuedHead.itemStack();
                        headLoadingStatus[i] = HeadStatus.COMPLETED;
                    }
                }
            }
        }
    }

    private void fetchReadyHeads() {
        for (int i = 0; i < fetchedHeads.length; i++) {
            HeadStatus status = headLoadingStatus[i];
            QueuedHead head = heads.get(i);
            if (status == HeadStatus.SKIN_LOADED) {
                PlayerSkin playerSkin = SkinManager.getSkin(head.uuid());
                ItemStack newHead = copyItemText(head.itemStack(), Head.createPlayerSkull(head.uuid(), playerSkin.url()));
                fetchedHeads[i] = newHead;
                savedHeads.put(head.uuid(), newHead.clone());
                headLoadingStatus[i] = HeadStatus.READY_TO_APPLY;
            } else if (status == HeadStatus.HEAD_CACHED) {
                fetchedHeads[i] = copyItemText(head.itemStack(), fetchedHeads[i].clone());
                headLoadingStatus[i] = HeadStatus.READY_TO_APPLY;
            }
        }
    }

    private ItemStack copyItemText(ItemStack from, ItemStack to) {
        ItemMeta fromMeta = from.getItemMeta();
        ItemMeta toMeta = to.getItemMeta();
        if (fromMeta == null || toMeta == null)
            return to;

        if (NotBounties.isAboveVersion(21, 3) && fromMeta.hasItemModel()) {
            toMeta.setItemModel(fromMeta.getItemModel());
        }
        if (NotBounties.isAboveVersion(21, 4)) {
            toMeta.setCustomModelDataComponent(fromMeta.getCustomModelDataComponent());
        } else {
            if (fromMeta.hasCustomModelData()) {
                toMeta.setCustomModelData(fromMeta.getCustomModelData());
            }
        }
        if (fromMeta.hasDisplayName())
            toMeta.setDisplayName(fromMeta.getDisplayName());
        if (fromMeta.hasLore())
            toMeta.setLore(fromMeta.getLore());
        to.setItemMeta(toMeta);
        return to;
    }

    /**
     * Updates the inventory of the player with the fetched heads.
     * @return True if the inventory was accessed.
     */
    private boolean updateInventory() {
        if (player.isOnline() && GUI.playerInfo.containsKey(player.getUniqueId())) {
            PlayerGUInfo currentInfo = GUI.playerInfo.get(player.getUniqueId());
            if (!currentInfo.guiType().equals(guInfo.guiType()) || currentInfo.page() != guInfo.page() || !currentInfo.title().equals(guInfo.title())) {
                // no longer in same GUI
                return false;
            }
            GUIOptions guiOptions = GUI.getGUI(guInfo.guiType());
            if (guiOptions == null || CompatabilityUtils.getType(player) != guiOptions.getInventoryType()) {
                // invalid guiOptions
                return false;
            }
            Inventory inventory = CompatabilityUtils.getTopInventory(player);
            ItemStack[] contents = inventory.getContents();
            for (int j = 0; j < fetchedHeads.length; j++) {
                QueuedHead head = heads.get(j);
                HeadStatus status = headLoadingStatus[j];
                int slot = head.slot();
                if (status != HeadStatus.READY_TO_APPLY
                        || !guiOptions.getPlayerSlots().contains(slot)
                        || slot < 0
                        || slot >= contents.length)
                    continue;
                contents[head.slot()] = fetchedHeads[j];
                headLoadingStatus[j] = HeadStatus.COMPLETED;
            }
            inventory.setContents(contents);
        } else {
            maxRequests = 0;
            NotBounties.debugMessage("Player exited GUI while loading player heads.", false);
        }
        return true;
    }
}

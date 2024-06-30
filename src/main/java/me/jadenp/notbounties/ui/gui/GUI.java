package me.jadenp.notbounties.ui.gui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUI;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.utils.configuration.ActionCommands;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.Immunity;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.getNetworkPlayers;
import static me.jadenp.notbounties.NotBounties.isVanished;
import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class GUI implements Listener {

    public static final Map<UUID, PlayerGUInfo> playerInfo = new HashMap<>();
    private static final Map<String, GUIOptions> customGuis = new HashMap<>();

    public static void addGUI(GUIOptions gui, String name) {
        customGuis.put(name, gui);
    }

    public static GUIOptions getGUI(String guiName) {
        if (customGuis.containsKey(guiName))
            return customGuis.get(guiName);
        return null;
    }

    public static Map<UUID, String> getGUIValues(Player player, String name, long page, Object[] data) {
        if (!customGuis.containsKey(name))
            return new LinkedHashMap<>();
        GUIOptions gui = customGuis.get(name);
        if (page < 1)
            page = 1;

        boolean online = (data.length == 0 || !(data[0] instanceof String) || !((String) data[0]).equalsIgnoreCase("offline"));
        List<UUID> onlinePlayers = getNetworkPlayers().stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toList());
        LinkedHashMap<UUID, String> values = new LinkedHashMap<>();
        switch (name) {
            case "bounty-gui":
                List<Bounty> sortedList = BountyManager.getAllBounties(gui.getSortType());
                for (Bounty bounty : sortedList) {
                    double bountyAmount = showWhitelistedBounties || player.hasPermission("notbounties.admin") ? bounty.getTotalDisplayBounty() : bounty.getTotalDisplayBounty(player);
                    if (bountyAmount > 0)
                        values.put(bounty.getUUID(), String.format("%f", bountyAmount));
                    if (reducePageCalculations && values.size() > gui.getPlayerSlots().size() * page)
                        break;
                }
                break;
            case "leaderboard":
                Leaderboard leaderboard = data.length > 0 && data[0] instanceof Leaderboard ? (Leaderboard) data[0] : Leaderboard.ALL;
                values = leaderboard.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                break;
            case "set-bounty":
                values = Leaderboard.IMMUNITY.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                for (Map.Entry<String, UUID> entry : NotBounties.loggedPlayers.entrySet()) {
                    if (!values.containsKey(entry.getValue()))
                        values.put(entry.getValue(), NumberFormatting.currencyPrefix + "0" + NumberFormatting.currencySuffix);
                }
                // iterate through to remove specific players
                Iterator<Map.Entry<UUID, String>> iterator = values.entrySet().iterator();
                int addedPlayers = 0;
                while (iterator.hasNext()) {
                    Map.Entry<UUID, String> entry = iterator.next();
                    if (reducePageCalculations) {
                        // remove added players we don't need
                        if (addedPlayers > gui.getPlayerSlots().size() * page) {
                            iterator.remove();
                            continue;
                        }
                    }
                    OfflinePlayer player1 = Bukkit.getOfflinePlayer(entry.getKey());
                    if (online) {
                        // remove if offline or vanished
                        if (!onlinePlayers.contains(entry.getKey()) || (player.isOnline() && (isVanished(Objects.requireNonNull(player1.getPlayer())) || (NotBounties.serverVersion >= 17 && seePlayerList && !player.canSee(player1.getPlayer()))))) {
                            iterator.remove();
                            continue;
                        }
                    }
                    // remove if they are immune
                    Immunity.ImmunityType immunityType = Immunity.getAppliedImmunity(player1, 69);
                    if (immunityType == Immunity.ImmunityType.PERMANENT || immunityType == Immunity.ImmunityType.GRACE_PERIOD || immunityType == Immunity.ImmunityType.TIME) {
                        iterator.remove();
                        continue;
                    }
                    addedPlayers++;
                }
                break;
            case "set-whitelist":
                List<UUID> whitelist = NotBounties.getPlayerWhitelist(player.getUniqueId()).getList();
                for (UUID uuid : whitelist)
                    values.put(uuid, NumberFormatting.currencyPrefix + Leaderboard.IMMUNITY.getStat(uuid) + NumberFormatting.currencySuffix);
                for (Map.Entry<UUID, String> entry : Leaderboard.IMMUNITY.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet()) {
                    if (!values.containsKey(entry.getKey()))
                        values.put(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, UUID> entry : NotBounties.loggedPlayers.entrySet()) {
                    if (!values.containsKey(entry.getValue()))
                        values.put(entry.getValue(), NumberFormatting.currencyPrefix + "0" + NumberFormatting.currencySuffix);
                    if (reducePageCalculations) {
                        if (values.size() > gui.getPlayerSlots().size() * page)
                            break;
                    }
                }
                // iterate through to remove specific players
                Iterator<Map.Entry<UUID, String>> iterator1 = values.entrySet().iterator();
                int addedPlayers1 = 0;
                while (iterator1.hasNext()) {
                    Map.Entry<UUID, String> entry = iterator1.next();
                    if (reducePageCalculations) {
                        // remove extra players we don't need
                        if (addedPlayers1 > gui.getPlayerSlots().size() * page) {
                            iterator1.remove();
                            continue;
                        }
                    }
                    OfflinePlayer player1 = Bukkit.getOfflinePlayer(entry.getKey());
                    if (online) {
                        // remove if offline or vanished
                        if (!onlinePlayers.contains(entry.getKey()) || (player.isOnline() && (isVanished(Objects.requireNonNull(player1.getPlayer())) || (NotBounties.serverVersion >= 17 && seePlayerList && !player.canSee(player1.getPlayer()))))) {
                            iterator1.remove();
                            continue;
                        }
                    }
                    addedPlayers1++;
                }
                break;
            case "select-price":
                String uuid = data.length > 0 && data[0] instanceof String ? (String) data[0] : player.getUniqueId().toString();
                values.put(UUID.fromString(uuid), page + "");
                break;
            case "bounty-item-select":
                UUID uuid3 = data.length > 0 && data[0] instanceof UUID ? (UUID) data[0] : player.getUniqueId();
                double total = 0;
                try {
                    for (int i = 1; i < data.length; i++) {
                        ItemStack[] contents = SerializeInventory.itemStackArrayFromBase64((String) data[i]);
                        total+= NumberFormatting.getTotalValue(Arrays.asList(contents));
                    }
                } catch (IOException | ClassCastException e) {
                    Bukkit.getLogger().warning("[NotBounties] Error trying to open the bounty-item-select GUI.");
                    Bukkit.getLogger().warning(e.toString());
                }
                values.put(uuid3, NumberFormatting.getValue(total));
                break;
            case "confirm":
                UUID uuid1 = data.length > 0 && data[0] instanceof UUID ? (UUID) data[0] : player.getUniqueId();
                double price = data.length > 1 && data[1] instanceof Double ? (double) data[1] : 0;
                values.put(uuid1, NumberFormatting.getValue(price));
                break;
            case "confirm-bounty":
                String uuid2 = data.length > 0 && data[0] instanceof UUID ? data[0].toString() : player.getUniqueId().toString();
                long price2 = data.length > 1 && data[1] instanceof Long ? (long) data[1] : 0;
                values.put(UUID.fromString(uuid2), price2 + "");
                break;
            default:
                break;
        }
        return values;

    }

    public static void openGUI(Player player, String name, long page, Object... data) {
        if (page < 1)
            page = 1;

        String[] replacements = new String[0];
        if ("leaderboard".equals(name)) {
            Leaderboard leaderboard = data.length > 0 && data[0] instanceof Leaderboard ? (Leaderboard) data[0] : Leaderboard.ALL;
            replacements = new String[]{leaderboard.toString()};
        }
        if (name.equals("bounty-item-select")) {
            if (data.length > 1 && data[1] instanceof String[]) {
                // spread out array of serialized arrays
                String[] items = (String[]) data[1];
                Object first = data[0]; // record first obj
                data = new Object[items.length + 1]; // refactor array for new length
                data[0] = first; // rewrite first obj
                System.arraycopy(items, 0, data, 1, items.length); // copy items over
            }
            if (data.length > page) {
                // set to current items
                replacements = new String[]{data[(int) page].toString()};
            }
        }
        LinkedHashMap<UUID, String> values = (LinkedHashMap<UUID, String>) getGUIValues(player, name, page, data);

        if (floodgateEnabled && BedrockGUI.enabled && new FloodGateClass().isBedrockPlayer(player.getUniqueId()) && BedrockGUI.isGUIEnabled(name)) {
            // open bedrock gui
            BedrockGUI.openGUI(player, name, page, values, replacements);
        } else {
            // open java gui
            if (!customGuis.containsKey(name))
                return;
            GUIOptions gui = customGuis.get(name);
            Inventory inventory = gui.createInventory(player, page, values, replacements);
            if (playerInfo.containsKey(player.getUniqueId()) && gui.getType().equals(playerInfo.get(player.getUniqueId()).getGuiType()) && player.getOpenInventory().getTitle().equals(playerInfo.get(player.getUniqueId()).getTitle())) {
                // already has the gui type open - update contents
                player.getOpenInventory().getTopInventory().setContents(inventory.getContents());
                if (NotBounties.serverVersion >= 19)
                    player.getOpenInventory().setTitle(gui.getTitle(player, page, values, replacements));
            } else {
                player.openInventory(inventory);
            }

        }
        playerInfo.put(player.getUniqueId(), new PlayerGUInfo(page, name, data, values.keySet().toArray(new UUID[0]), player.getOpenInventory().getTitle()));
    }


    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (playerInfo.containsKey(event.getPlayer().getUniqueId()) && playerInfo.get(event.getPlayer().getUniqueId()).getTitle().equals(event.getView().getTitle())) {
                // titles match
                // return items they have place in the inventory
                // (only works for bounty-item-select
                returnGUIItems((Player) event.getPlayer(), event.getInventory(), false);
                playerInfo.remove(event.getPlayer().getUniqueId());
            }

    }

    public static void reopenBountiesGUI() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                if (!playerInfo.containsKey(player.getUniqueId()))
                    continue;
                PlayerGUInfo info = playerInfo.get(player.getUniqueId());
                if (!info.getGuiType().equals("bounty-item-select")) {
                    safeCloseGUI(player, false); // close GUI
                    // reopen GUI
                    openGUI(player, info.getGuiType(), info.getPage(), info.getData());
                }
            }
        }
    }

    /**
     * Closes a player's inventory if they are in a NotBounties GUI.
     * Any items placed in the GUI will be returned
     * @param player Player who may have a GUI open
     */
    public static void safeCloseGUI(Player player, boolean shutdown) {
        if (playerInfo.containsKey(player.getUniqueId())) {
            // return any items placed in the inventory
            returnGUIItems(player, player.getOpenInventory().getTopInventory(), shutdown);
            playerInfo.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    private static void returnGUIItems(Player player, Inventory inventory, boolean shutdown) {
        if (!((Plugin) NotBounties.getInstance()).isEnabled())
            shutdown = true;
        PlayerGUInfo guInfo = playerInfo.get(player.getUniqueId());
        if (guInfo.getGuiType().equals("bounty-item-select")){
            GUIOptions guiOptions = GUI.getGUI("bounty-item-select");
            // give back items
            Object[] data = guInfo.getData();
            List<ItemStack[]> deserializedItems = new ArrayList<>();
            for (int i = 1; i < data.length; i++) {
                String serializedItems = data[i].toString();
                try {
                    ItemStack[] items = SerializeInventory.itemStackArrayFromBase64(serializedItems);
                    deserializedItems.add(items);
                } catch (IOException e) {
                    Bukkit.getLogger().warning("[NotBounties] Couldn't deserialize items: " + serializedItems);
                    Bukkit.getLogger().warning(e.toString());
                }
            }
            // the data doesn't currently hold the correct items for the opened page
            // update current contents
            ItemStack[] currentContents = new ItemStack[guiOptions.getPlayerSlots().size() - 1];
            for (int i = 0; i < currentContents.length; i++) {
                currentContents[i] = inventory.getContents()[guiOptions.getPlayerSlots().get(i + 1)];
            }
            if (deserializedItems.size() >= guInfo.getPage()) {
                deserializedItems.set((int) (guInfo.getPage()-1), currentContents);
            } else {
                deserializedItems.add(currentContents);
            }


            // give items back
            if (shutdown) {
                // plugin is shutting down, add to refund
                for (ItemStack[] items : deserializedItems)
                    BountyManager.refundPlayer(player.getUniqueId(), 0, Arrays.asList(items));
            } else {
                // can give items
                for (ItemStack[] items : deserializedItems)
                    NumberFormatting.givePlayer(player, Arrays.asList(items), true);
            }
        }
    }

    /**
     * Get a gui from the title
     *
     * @param title Title of the GUI
     * @return the GUIOptions if the title matches a NotBounties GUI, or null if the title does not match any GUI
     */
    public static @Nullable GUIOptions getGUIByTitle(String title) {
        GUIOptions gui = null;
        for (Map.Entry<String, GUIOptions> entry : customGuis.entrySet()) {
            if (title.startsWith(entry.getValue().getName())) {
                gui = entry.getValue();
                break;
            }
        }
        return gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!playerInfo.containsKey(event.getWhoClicked().getUniqueId())) // check if they are in a NotBounties GUI
            return;

        PlayerGUInfo info = playerInfo.get(event.getWhoClicked().getUniqueId());
        GUIOptions gui = getGUI(info.getGuiType());

        if (gui == null) // JIC a player has a page number, but they aren't in a gui
            return;
        String guiType = gui.getType();
        event.setCancelled(true);
        boolean bottomInventory = event.getRawSlot() >= event.getView().getTopInventory().getSize();
        if (bottomInventory && !guiType.equals("bounty-item-select")) // make sure it is in the top inventory
            return;
        // check if it is a player slot
        int pageAddition = guiType.equals("select-price") || guiType.equals("confirm-bounty") ? 0 : (int) ((info.getPage() - 1) * gui.getPlayerSlots().size());
        if (gui.getPlayerSlots().contains(event.getRawSlot()) && (event.getCurrentItem() != null && gui.getPlayerSlots().indexOf(event.getRawSlot()) + pageAddition < info.getPlayers().length && event.getCurrentItem().getType() == Material.PLAYER_HEAD)) {
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            assert meta != null;
            OfflinePlayer player = Bukkit.getOfflinePlayer(info.getPlayers()[gui.getPlayerSlots().indexOf(event.getRawSlot()) + pageAddition]);
            String playerName = NotBounties.getPlayerName(player.getUniqueId());
            switch (guiType) {
                case "bounty-gui":
                    // remove, edit, and buy back
                    Bounty bounty = getBounty(player.getUniqueId());
                    if (bounty != null) {
                        if (event.isRightClick()) {
                            if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                                // edit
                                event.getView().close();
                                TextComponent first = new TextComponent(ChatColor.GOLD + "To edit " + bounty.getName() + "'s bounty");
                                TextComponent click = new TextComponent(ChatColor.YELLOW + "" + ChatColor.BOLD + " Click Here ");
                                TextComponent last = new TextComponent(ChatColor.GOLD + "and enter the new amount.");
                                click.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bounty edit " + bounty.getName() + " "));
                                first.addExtra(click);
                                first.addExtra(last);
                                event.getWhoClicked().spigot().sendMessage(first);
                            } else  if (giveOwnMap) {
                                event.getView().close();
                                Bukkit.getServer().dispatchCommand(event.getWhoClicked(), pluginBountyCommands.get(0) + " poster " + playerName);
                            }
                        } else if (event.isLeftClick()) {
                            if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                                // remove
                                openGUI((Player) event.getWhoClicked(), "confirm", 1, player.getUniqueId(), 0);

                            } else if (bounty.getUUID().equals(event.getWhoClicked().getUniqueId()) && buyBack) {
                                double balance = NumberFormatting.getBalance((Player) event.getWhoClicked());
                                if (balance >= (int) (bounty.getTotalDisplayBounty() * buyBackInterest)) {
                                    openGUI((Player) event.getWhoClicked(), "confirm", 1, player.getUniqueId(), (buyBackInterest * bounty.getTotalDisplayBounty()));
                                } else {
                                    event.getWhoClicked().sendMessage(parse(prefix + broke, (bounty.getTotalDisplayBounty() * buyBackInterest), (Player) event.getWhoClicked()));
                                }
                            } else if (BountyTracker.isEnabled() && (BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers()) && event.getWhoClicked().hasPermission("notbounties.tracker")) {
                                event.getView().close();
                                Bukkit.getServer().dispatchCommand(event.getWhoClicked(), pluginBountyCommands.get(0) + " tracker " + playerName);
                            }
                        }
                    } else {
                        if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                            openGUI((Player) event.getWhoClicked(), "bounty-gui", playerInfo.get(event.getWhoClicked().getUniqueId()).getPage());
                            event.getWhoClicked().sendMessage(parse(prefix + noBounty, Objects.requireNonNull(meta.getOwningPlayer()).getName(), meta.getOwningPlayer()));
                        }
                    }
                    break;
                case "set-bounty":
                    if (NumberFormatting.bountyItemsDefaultGUI) {
                        openGUI((Player) event.getWhoClicked(), "bounty-item-select", ConfigOptions.minBounty, player.getUniqueId().toString());
                    } else {
                        openGUI((Player) event.getWhoClicked(), "select-price", ConfigOptions.minBounty, player.getUniqueId().toString());
                    }
                    break;
                case "set-whitelist":
                    List<UUID> whitelist = NotBounties.getPlayerWhitelist(event.getWhoClicked().getUniqueId()).getList();
                    if (!whitelist.remove(player.getUniqueId())) {
                        if (whitelist.size() < 10)
                            whitelist.add(player.getUniqueId());
                        else
                            event.getWhoClicked().sendMessage(parse(prefix + whitelistMax, (Player) event.getWhoClicked()));
                    }
                    openGUI((Player) event.getWhoClicked(), "set-whitelist", 1, info.getData());
                    break;
                case "select-price":
                    Bukkit.dispatchCommand(event.getWhoClicked(),   pluginBountyCommands.get(0) + " " + playerName + " " + playerInfo.get(event.getWhoClicked().getUniqueId()).getPage());
                    if (!confirmation)
                        event.getView().close();
                    break;
                case "bounty-item-select":

                    if (!gui.getPlayerSlots().isEmpty())
                        if (event.getRawSlot() ==  gui.getPlayerSlots().get(0)) {
                            // set bounty
                            ActionCommands.executeCommands((Player) event.getWhoClicked(),  new ArrayList<>(Collections.singletonList("[p] " + pluginBountyCommands.get(0) + " {data} --confirm")));
                        }

                    break;
                default:
                    // no action for the player slots in the custom GUI
                    break;
            }
        } else {
            // not a player slot - custom item or inventory
            // current item may be null
            CustomItem customItem = gui.getCustomItem(event.getRawSlot(), info.getPage(), getGUIValues((Player) event.getWhoClicked(), guiType, info.getPage(), info.getData()).size());
            if (customItem == null) {
                // If in the bounty item select inventory, allow user to move non-custom items around and not the first player slot
                if (guiType.equals("bounty-item-select") && (bottomInventory || (!gui.getPlayerSlots().isEmpty() && event.getRawSlot() !=  gui.getPlayerSlots().get(0)))) {
                    event.setCancelled(false);

                    if (!bottomInventory)
                        // reopen gui to update tax
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                // check to make sure the gui is still open
                                if (!((Player) event.getWhoClicked()).isOnline())
                                    return;
                                if (playerInfo.containsKey(event.getWhoClicked().getUniqueId())) {
                                    PlayerGUInfo newOptions = playerInfo.get(event.getWhoClicked().getUniqueId());
                                    if (info.getPage() == newOptions.getPage() && newOptions.getGuiType().equals(info.getGuiType())) {
                                        // page hasn't changed
                                        GUIOptions guiOptions = GUI.getGUI("bounty-item-select");
                                        // read current inventory
                                        ItemStack[] currentContents = new ItemStack[guiOptions.getPlayerSlots().size() - 1];
                                        for (int i = 0; i < currentContents.length; i++) {
                                            currentContents[i] = event.getInventory().getContents()[guiOptions.getPlayerSlots().get(i + 1)];
                                        }
                                        // create new data
                                        Object[] data = new Object[(int) Math.max(newOptions.getData().length, newOptions.getPage() + 1)];
                                        System.arraycopy(newOptions.getData(), 0, data, 0, newOptions.getData().length);
                                        data[(int) (newOptions.getPage())] = SerializeInventory.itemStackArrayToBase64(currentContents);
                                        // open gui
                                        openGUI((Player) event.getWhoClicked(), "bounty-item-select", newOptions.getPage(), data);
                                    }
                                }
                            }
                        }.runTaskLater(NotBounties.getInstance(), 1);
                }
                return;
            }
            ActionCommands.executeCommands((Player) event.getWhoClicked(), customItem.getCommands());
        }
    }

}

package me.jadenp.notbounties.ui.gui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUI;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUIOptions;
import me.jadenp.notbounties.ui.gui.displayItems.*;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.utils.Whitelist;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.GeyserMCClass;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.*;

public class GUI implements Listener {

    public static final Map<UUID, PlayerGUInfo> playerInfo = new HashMap<>();
    private static final Map<String, GUIOptions> customGuis = new HashMap<>();


    public static CustomItem getGeneralCurrencyItem() {
        if (!customItems.containsKey("general-currency-item")) {
            CustomItem customItem = new CustomItem(Material.SUNFLOWER, 1, -1, "{currency}", new ArrayList<>(), true, true, false, new ArrayList<>());
            customItems.put("general-currency-item", customItem);
            return customItem;
        }
        return customItems.get("general-currency-item");
    }

    public static void addGUI(GUIOptions gui, String name) {
        customGuis.put(name, gui);
    }

    public static GUIOptions getGUI(String guiName) {
        if (customGuis.containsKey(guiName))
            return customGuis.get(guiName);
        return null;
    }

    public static List<DisplayItem> getGUIValues(Player player, String name, long page, Object[] data) {
        List<DisplayItem> displayItems = new ArrayList<>();
        if (!customGuis.containsKey(name))
            return displayItems;
        GUIOptions gui = customGuis.get(name);
        if (page < 1)
            page = 1;

        boolean online = (data.length == 0 || !(data[0] instanceof String) || !((String) data[0]).equalsIgnoreCase("offline"));
        List<UUID> onlinePlayers = getNetworkPlayers().stream().map(OfflinePlayer::getUniqueId).toList();
        switch (name) {
            case "bounty-gui":
                List<Bounty> sortedList = BountyManager.getAllBounties(gui.getSortType());
                for (int i = 0; i < sortedList.size(); i++) {
                    Bounty bounty = sortedList.get(i);
                    double bountyAmount = showWhitelistedBounties || player.hasPermission(NotBounties.getAdminPermission()) ? bounty.getTotalDisplayBounty() : bounty.getTotalDisplayBounty(player);
                    if (bountyAmount > 0) {
                        List<String> additionalLore = GUIClicks.getClickLore(player, buyBack && bounty.getUUID().equals(player.getUniqueId()), (bounty.getTotalDisplayBounty() * buyBackInterest));
                        if (bounty.getAllWhitelists().contains(player.getUniqueId())) {
                            additionalLore.addAll(whitelistNotify);
                        } else if (!bounty.getAllBlacklists().isEmpty() && !bounty.getAllBlacklists().contains(player.getUniqueId())) {
                            additionalLore.addAll(whitelistNotify);
                        } else if (showWhitelistedBounties || player.hasPermission(NotBounties.getAdminPermission())) {
                            // not whitelisted
                            for (Setter setter : bounty.getSetters()) {
                                if (!setter.canClaim(player)) {
                                    additionalLore.addAll(notWhitelistedLore);
                                    break;
                                }
                            }
                        }
                        displayItems.add(new PlayerItem(bounty.getUUID(), bountyAmount, Leaderboard.CURRENT, i, bounty.getLatestSetter(), additionalLore));
                    }
                    if (reducePageCalculations && displayItems.size() > gui.getPlayerSlots().size() * page)
                        break;
                }
                break;
            case "view-bounty":
                if (data.length > 0 && data[0] instanceof UUID uuid) {
                    Bounty viewedBounty = BountyManager.getBounty(uuid);
                    if (viewedBounty != null) {
                        List<Setter> setters = new ArrayList<>(viewedBounty.getSetters());
                        List<String> additionalLore = player.hasPermission(NotBounties.getAdminPermission()) ? new ArrayList<>(adminEditLore) : new ArrayList<>();
                        setters.sort(Comparator.comparing(Setter::getUuid)); // same setters will be next to each other
                        List<ItemStack> concurrentItems = new ArrayList<>();
                        double concurrentAmount = 0;
                        double concurrentDisplay = 0;
                        long latestSet = 0;
                        int settersAdded = 0;
                        for (int i = 0; i < setters.size(); i++) {
                            Setter currentSetter = setters.get(i);
                            concurrentItems.addAll(currentSetter.getItems());
                            concurrentAmount += currentSetter.getAmount();
                            concurrentDisplay += currentSetter.getDisplayAmount();
                            if (currentSetter.getTimeCreated() > latestSet)
                                latestSet = currentSetter.getTimeCreated();
                            if (i == setters.size() - 1 || !currentSetter.getUuid().equals(setters.get(i + 1).getUuid())) {
                                // at the end of the list, or next setter is different
                                displayItems.add(new PlayerItem(currentSetter.getUuid(), concurrentDisplay, Leaderboard.CURRENT, settersAdded++, latestSet, additionalLore)); // add player head item
                                // add items for everything added
                                if (concurrentAmount != 0) {
                                    // add currency

                                    if (currency.size() == 1 && !NumberFormatting.shouldUseDecimals()) {
                                        // change material and amount (possible multiple items) to represent a physical item
                                        ItemStack item = getGeneralCurrencyItem().getFormattedItem(player, null);
                                        try {
                                            Material material = Material.valueOf(currency.get(0).toUpperCase());
                                            item.setType(material);
                                        } catch (IllegalArgumentException ignored) {
                                            // currency is not a placeholder and isn't a currency
                                        }
                                        // split items into groups of max stack size
                                        float valuePerItem = currencyValues.get(currency.get(0));
                                        int stacks = (int) (concurrentAmount / valuePerItem / item.getMaxStackSize());
                                        int remainder = (int) (concurrentAmount / valuePerItem % item.getMaxStackSize());

                                        // add items, ignoring the fact that we are using the unmodified class with a heavily modified item
                                        if (stacks > 0) {
                                            ItemStack parsedItem = item.clone();
                                            parsedItem.setAmount(parsedItem.getMaxStackSize());
                                            ItemMeta meta = parsedItem.getItemMeta();
                                            if (meta != null) {
                                                meta.setDisplayName(meta.getDisplayName().replace("{amount}", currencyPrefix + NumberFormatting.formatNumber(valuePerItem * item.getMaxStackSize()) + currencySuffix));
                                                if (meta.hasLore() && meta.getLore() != null) {
                                                    List<String> lore = meta.getLore();
                                                    meta.setLore(lore.stream().map(string -> LanguageOptions.parse(string, valuePerItem * item.getMaxStackSize(), player)).toList());
                                                }
                                                parsedItem.setItemMeta(meta);
                                            }
                                            for (int j = 0; j < stacks; j++) {
                                                displayItems.add(new UnmodifiedItem(currentSetter.getUuid(), parsedItem, new ArrayList<>()));
                                            }
                                        }
                                        if (remainder > 0) {
                                            ItemStack parsedItem = item.clone();
                                            parsedItem.setAmount(remainder);
                                            ItemMeta meta = parsedItem.getItemMeta();
                                            if (meta != null) {
                                                meta.setDisplayName(meta.getDisplayName().replace("{amount}", currencyPrefix + NumberFormatting.formatNumber(valuePerItem * remainder) + currencySuffix));
                                                if (meta.hasLore() && meta.getLore() != null) {
                                                    List<String> lore = meta.getLore();
                                                    meta.setLore(lore.stream().map(string -> LanguageOptions.parse(string, valuePerItem * remainder, player)).toList());
                                                }
                                                parsedItem.setItemMeta(meta);
                                            }
                                            displayItems.add(new UnmodifiedItem(currentSetter.getUuid(), parsedItem, new ArrayList<>()));
                                        }
                                    } else {
                                        // add default currency items
                                        displayItems.add(new CurrencyItem(currentSetter.getUuid(), concurrentAmount, new ArrayList<>()));
                                    }
                                }
                                for (ItemStack item : concurrentItems) {
                                    displayItems.add(new UnmodifiedItem(currentSetter.getUuid(), item, new ArrayList<>()));
                                }
                                // reset concurrent values
                                concurrentItems.clear();
                                concurrentAmount = 0;
                                concurrentDisplay = 0;
                                latestSet = 0;
                            }
                        }
                    }
                }
                break;
            case "leaderboard":
                Leaderboard leaderboard = data.length > 0 && data[0] instanceof Leaderboard board ? board : Leaderboard.ALL;
                int rankIndex = 0;
                for (Map.Entry<UUID, Double> entry : leaderboard.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet()) {
                    displayItems.add(new PlayerItem(entry.getKey(), entry.getValue(), leaderboard, rankIndex++, System.currentTimeMillis(), new ArrayList<>()));
                }
                break;
            case "set-bounty":
                List<UUID> addedPlayers = new ArrayList<>();
                for (Map.Entry<UUID, Double> entry : Leaderboard.IMMUNITY.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet()) {
                    OfflinePlayer player1 = Bukkit.getOfflinePlayer(entry.getKey());
                    if (online && (!onlinePlayers.contains(entry.getKey()) || (player.isOnline() && (isVanished(Objects.requireNonNull(player1.getPlayer())) || (NotBounties.serverVersion >= 17 && seePlayerList && !player.canSee(player1.getPlayer())))))) {
                        // skip if offline or vanished
                        continue;
                    }
                    displayItems.add(new PlayerItem(entry.getKey(), entry.getValue(), Leaderboard.IMMUNITY, addedPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                    addedPlayers.add(entry.getKey());
                }
                for (Map.Entry<String, UUID> entry : NotBounties.loggedPlayers.entrySet()) {
                    if (reducePageCalculations && displayItems.size() > gui.getPlayerSlots().size() * page)
                        break;
                    if (!addedPlayers.contains(entry.getValue())) {
                        OfflinePlayer player1 = Bukkit.getOfflinePlayer(entry.getValue());
                        if (online && (!onlinePlayers.contains(entry.getValue()) || (player.isOnline() && (isVanished(Objects.requireNonNull(player1.getPlayer())) || (NotBounties.serverVersion >= 17 && seePlayerList && !player.canSee(player1.getPlayer())))))) {
                            // skip if offline or vanished
                            continue;
                        }

                        Immunity.ImmunityType immunityType = Immunity.getAppliedImmunity(player1, 69);
                        if (immunityType == Immunity.ImmunityType.PERMANENT || immunityType == Immunity.ImmunityType.GRACE_PERIOD || immunityType == Immunity.ImmunityType.TIME) {
                            // skip if they are immune
                            continue;
                        }
                        displayItems.add(new PlayerItem(entry.getValue(), 0, Leaderboard.IMMUNITY, addedPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                        addedPlayers.add(entry.getValue());
                    }
                }
                break;
            case "set-whitelist":
                List<UUID> playersAdded = new ArrayList<>();
                Whitelist whitelist = NotBounties.getPlayerWhitelist(player.getUniqueId());
                for (UUID uuid : whitelist.getList()) {
                    List<String> additionalLore = whitelist.isBlacklist() ? blacklistLore : whitelistLore;
                    displayItems.add(new WhitelistedPlayerItem(uuid, Leaderboard.IMMUNITY.getStat(uuid), Leaderboard.IMMUNITY, playersAdded.size(), System.currentTimeMillis(), additionalLore));
                    playersAdded.add(uuid);
                }
                for (Map.Entry<UUID, Double> entry : Leaderboard.IMMUNITY.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet()) {
                    if (!playersAdded.contains(entry.getKey())) {
                        OfflinePlayer player1 = Bukkit.getOfflinePlayer(entry.getKey());
                        if (online && (!onlinePlayers.contains(entry.getKey()) || (player.isOnline() && (isVanished(Objects.requireNonNull(player1.getPlayer())) || (NotBounties.serverVersion >= 17 && seePlayerList && !player.canSee(player1.getPlayer())))))) {
                            // skip if offline or vanished
                            continue;
                        }
                        displayItems.add(new PlayerItem(entry.getKey(), entry.getValue(), Leaderboard.IMMUNITY, playersAdded.size(), System.currentTimeMillis(), new ArrayList<>()));
                        playersAdded.add(entry.getKey());
                    }
                }
                for (Map.Entry<String, UUID> entry : NotBounties.loggedPlayers.entrySet()) {
                    if (reducePageCalculations && playersAdded.size() > gui.getPlayerSlots().size() * page)
                        break;
                    if (!playersAdded.contains(entry.getValue())) {
                        OfflinePlayer player1 = Bukkit.getOfflinePlayer(entry.getValue());
                        if (online && (!onlinePlayers.contains(entry.getValue()) || (player.isOnline() && (isVanished(Objects.requireNonNull(player1.getPlayer())) || (NotBounties.serverVersion >= 17 && seePlayerList && !player.canSee(player1.getPlayer())))))) {
                            // skip if offline or vanished
                            continue;
                        }

                        displayItems.add(new PlayerItem(entry.getValue(), 0, Leaderboard.IMMUNITY, playersAdded.size(), System.currentTimeMillis(), new ArrayList<>()));
                        playersAdded.add(entry.getValue());
                    }
                }

                break;
            case "select-price":
                if (data.length > 0 && data[0] instanceof String string) {
                    displayItems.add(new PlayerItem(UUID.fromString(string), page, Leaderboard.CURRENT, 0, System.currentTimeMillis(), new ArrayList<>()));
                } else {
                    displayItems.add(new PlayerItem(player.getUniqueId(), page, Leaderboard.CURRENT, 0, System.currentTimeMillis(), new ArrayList<>()));
                }
                break;
            case "bounty-item-select":
                UUID uuid3 = data.length > 0 && data[0] instanceof UUID uuid ? uuid : player.getUniqueId();
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
                displayItems.add(new PlayerItem(uuid3, total, Leaderboard.CURRENT, 0, System.currentTimeMillis(), new ArrayList<>()));
                break;
            case "confirm":
                UUID uuid1 = data.length > 0 && data[0] instanceof UUID uuid ? uuid : player.getUniqueId();
                double price = data.length > 1 && data[1] instanceof Double ? (double) data[1] : 0;
                displayItems.add(new PlayerItem(uuid1, price, Leaderboard.CURRENT, 0, System.currentTimeMillis(), new ArrayList<>()));
                break;
            case "confirm-bounty":
                UUID uuid2 = data.length > 0 && data[0] instanceof UUID uuid ? uuid : player.getUniqueId();
                long price2 = data.length > 1 && data[1] instanceof Long ? (long) data[1] : 0;
                displayItems.add(new PlayerItem(uuid2, price2, Leaderboard.CURRENT, 0, System.currentTimeMillis(), new ArrayList<>()));
                break;
            default:
                break;
        }
    return displayItems;

    }

    public static void openGUI(Player player, String name, long page, Object... data) {
        if (page < 1)
            page = 1;

        List<DisplayItem> displayItems = getGUIValues(player, name, page, data);

        if (((floodgateEnabled && new FloodGateClass().isBedrockPlayer(player.getUniqueId())) || (geyserEnabled && new GeyserMCClass().isBedrockPlayer(player.getUniqueId()))) && BedrockGUI.isEnabled() && BedrockGUI.isGUIEnabled(name)) {
            // open bedrock gui
            BedrockGUI.openGUI(player, name, page, displayItems, data);
        } else {
            // open java gui
            if (!customGuis.containsKey(name))
                return;
            GUIOptions gui = customGuis.get(name);
            String title = createTitle(gui, player, page, displayItems, data);
            Inventory inventory = gui.createInventory(player, page, displayItems, title, data);
            if (playerInfo.containsKey(player.getUniqueId()) && gui.getType().equals(playerInfo.get(player.getUniqueId()).guiType()) && player.getOpenInventory().getTitle().equals(playerInfo.get(player.getUniqueId()).title())) {
                // already has the gui type open - update contents
                player.getOpenInventory().getTopInventory().setContents(inventory.getContents());
                if (NotBounties.serverVersion >= 19)
                    player.getOpenInventory().setTitle(title);
            } else {
                player.openInventory(inventory);
            }
            playerInfo.put(player.getUniqueId(), new PlayerGUInfo(page, name, data, displayItems, title));
        }

    }

    public static String createTitle(BedrockGUIOptions guiOptions, Player viewer, long page, List<DisplayItem> displayItems, Object[] data) {
        return createTitle(guiOptions.getName(), guiOptions.getType(), guiOptions.getMaxPlayers(), guiOptions.isAddPage(), viewer, page, displayItems, data);
    }
    private static String createTitle(GUIOptions guiOptions, Player viewer, long page, List<DisplayItem> displayItems, Object[] data) {
        return createTitle(guiOptions.getName(), guiOptions.getType(), guiOptions.getPlayerSlots().size(), guiOptions.isAddPage(), viewer, page, displayItems, data);
    }
    private static String createTitle(String name, String type, int numPlayerSlots, boolean addPage, Player viewer, long page, List<DisplayItem> displayItems, Object[] data) {
        String title = name;
        if (type.equals("view-bounty") && data.length > 0 && data[0] instanceof UUID uuid) {
            double amount = 0;
            Bounty bounty = BountyManager.getBounty(uuid);
            if (bounty != null) {
                amount = showWhitelistedBounties ? bounty.getTotalDisplayBounty() : bounty.getTotalDisplayBounty(viewer);
            }
            String playerName = NotBounties.getPlayerName(uuid);
            title = title.replace("{player}", playerName).replace("{amount}", currencyPrefix + NumberFormatting.formatNumber(amount) + currencySuffix);
        } else {
            for (DisplayItem displayItem : displayItems) {
                if (displayItem instanceof PlayerItem playerItem) {
                    title = playerItem.parseText(title, viewer);
                    break;
                }
            }
        }
        int maxPage;
        if (type.equals("select-price")) {
            maxPage = (int) NumberFormatting.getBalance(viewer);
        } else if (type.equals("bounty-gui")) {
            int numBounties = BountyManager.getAllBounties(-1).size();
            int playerSlots = Math.max(numPlayerSlots, 1);
            maxPage = (int) ((double) numBounties / playerSlots);
            if (numBounties % playerSlots != 0) {
                maxPage++;
            }
        } else if (reducePageCalculations) {
            int totalPlayers = loggedPlayers.size();
            int playerSlots = Math.max(numPlayerSlots, 1);
            maxPage = (int) ((double) totalPlayers / playerSlots);
            if (totalPlayers % playerSlots != 0) {
                maxPage++;
            }
        } else {
            int totalPlayers = displayItems.size();
            int playerSlots = Math.max(numPlayerSlots, 1);
            maxPage = (int) ((double) totalPlayers / playerSlots);
            if (totalPlayers % playerSlots != 0) {
                maxPage++;
            }
        }
        title = title.replace("{page}", (page + ""))
                .replace("{page_max}", (maxPage + ""));
        if (addPage)
            title = title + " " + page;
        return parse(title, viewer);
    }


    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (playerInfo.containsKey(event.getPlayer().getUniqueId()) && playerInfo.get(event.getPlayer().getUniqueId()).title().equals(event.getView().getTitle())) {
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
                if (!info.guiType().equals("bounty-item-select")) {
                    safeCloseGUI(player, false); // close GUI
                    // reopen GUI
                    openGUI(player, info.guiType(), info.page(), info.data());
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
        if (guInfo.guiType().equals("bounty-item-select")){
            GUIOptions guiOptions = GUI.getGUI("bounty-item-select");
            // give back items
            Object[] data = guInfo.data();
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
            if (deserializedItems.size() >= guInfo.page()) {
                deserializedItems.set((int) (guInfo.page()-1), currentContents);
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
        GUIOptions gui = getGUI(info.guiType());

        if (gui == null || event.getRawSlot() == -999)
            return;
        String guiType = gui.getType();
        event.setCancelled(true);
        boolean bottomInventory = event.getRawSlot() >= event.getView().getTopInventory().getSize();
        if (bottomInventory && !guiType.equals("bounty-item-select")) // make sure it is in the top inventory
            return;
        // check if it is a player slot
        int pageAddition = guiType.equals("select-price") || guiType.equals("confirm-bounty") ? 0 : (int) ((info.page() - 1) * gui.getPlayerSlots().size());
        if (gui.getPlayerSlots().contains(event.getRawSlot()) && (event.getCurrentItem() != null && gui.getPlayerSlots().indexOf(event.getRawSlot()) + pageAddition < info.displayItems().size() && event.getCurrentItem().getType() == Material.PLAYER_HEAD)) {
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            assert meta != null;
            DisplayItem displayItem = info.displayItems().get(gui.getPlayerSlots().indexOf(event.getRawSlot()) + pageAddition);
            UUID playerUUID = displayItem instanceof PlayerItem playerItem ? playerItem.getUuid() : event.getWhoClicked().getUniqueId();
            String playerName = NotBounties.getPlayerName(playerUUID);
            switch (guiType) {
                case "bounty-gui":
                    // do click actions
                    Bounty bounty = BountyManager.getBounty(playerUUID);
                    if (bounty != null) {
                        GUIClicks.runClickActions((Player) event.getWhoClicked(), bounty, event.getClick());
                    } else {
                        openGUI((Player) event.getWhoClicked(), "bounty-gui", playerInfo.get(event.getWhoClicked().getUniqueId()).page());
                    }
                    break;
                case "view-bounty":
                    // player is the setter that was clicked
                    if (event.getWhoClicked().hasPermission(NotBounties.getAdminPermission()) && info.data() != null && info.data().length > 0 && info.data()[0] instanceof UUID uuid) {
                        Bounty viewedBounty = BountyManager.getBounty(uuid);
                        if (viewedBounty != null) {
                            // bounty is valid
                            if (event.isRightClick()) {
                                event.getView().close();
                                TextComponent first = new TextComponent(ChatColor.GOLD + "To edit " + playerName + "'s amount on " + viewedBounty.getName() + "'s bounty");
                                TextComponent click = new TextComponent(ChatColor.YELLOW + "" + ChatColor.BOLD + " Click Here ");
                                TextComponent last = new TextComponent(ChatColor.GOLD + "and enter the amount to change " + ChatColor.GRAY + "(e.g. 10, -500, 10k)" + ChatColor.GOLD + ".");
                                click.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + pluginBountyCommands.get(0) + " edit " + viewedBounty.getName() + " from " + playerName + " "));
                                first.addExtra(click);
                                first.addExtra(last);
                                event.getWhoClicked().spigot().sendMessage(first);
                            } else if (event.isLeftClick()) {
                                event.getView().close();
                                Bukkit.dispatchCommand(event.getWhoClicked(), pluginBountyCommands.get(0) + " remove " + viewedBounty.getName() + " from " + playerName);
                            }
                        } else {
                            // no longer has a bounty
                            openGUI((Player) event.getWhoClicked(), "bounty-gui", 1);
                            event.getWhoClicked().sendMessage(LanguageOptions.parse(prefix + noBounty, NotBounties.getPlayerName(uuid), (Player) event.getWhoClicked()));
                        }
                    }
                    break;
                case "set-bounty":
                    if (NumberFormatting.bountyItemsDefaultGUI) {
                        openGUI((Player) event.getWhoClicked(), "bounty-item-select", ConfigOptions.minBounty, playerUUID.toString());
                    } else {
                        openGUI((Player) event.getWhoClicked(), "select-price", ConfigOptions.minBounty, playerUUID.toString());
                    }
                    break;
                case "set-whitelist":
                    List<UUID> whitelist = NotBounties.getPlayerWhitelist(event.getWhoClicked().getUniqueId()).getList();
                    if (!whitelist.remove(playerUUID)) {
                        if (whitelist.size() < 10)
                            whitelist.add(playerUUID);
                        else
                            event.getWhoClicked().sendMessage(parse(prefix + whitelistMax, (Player) event.getWhoClicked()));
                    }
                    openGUI((Player) event.getWhoClicked(), "set-whitelist", 1, info.data());
                    break;
                case "select-price":
                    Bukkit.dispatchCommand(event.getWhoClicked(),   pluginBountyCommands.get(0) + " " + playerName + " " + playerInfo.get(event.getWhoClicked().getUniqueId()).page());
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
            CustomItem customItem = gui.getCustomItem(event.getRawSlot(), info.page(), info.displayItems().size());
            if (guiType.equals("challenges") && gui.getPlayerSlots().contains(event.getRawSlot()) && gui.getPlayerSlots().indexOf(event.getRawSlot()) < ChallengeManager.getConcurrentChallenges()) {
                // clicked on a challenge item - check if they can claim
                if (ChallengeManager.tryClaim((Player) event.getWhoClicked(), gui.getPlayerSlots().indexOf(event.getRawSlot()))) {
                    // reopen gui
                    openGUI((Player) event.getWhoClicked(), "challenges", 1);
                }
                return;
            }
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
                                    if (info.page() == newOptions.page() && newOptions.guiType().equals(info.guiType())) {
                                        // page hasn't changed
                                        GUIOptions guiOptions = GUI.getGUI("bounty-item-select");
                                        // read current inventory
                                        ItemStack[] currentContents = new ItemStack[guiOptions.getPlayerSlots().size() - 1];
                                        for (int i = 0; i < currentContents.length; i++) {
                                            currentContents[i] = event.getInventory().getContents()[guiOptions.getPlayerSlots().get(i + 1)];
                                        }
                                        // create new data
                                        Object[] data = new Object[(int) Math.max(newOptions.data().length, newOptions.page() + 1)];
                                        System.arraycopy(newOptions.data(), 0, data, 0, newOptions.data().length);
                                        data[(int) (newOptions.page())] = SerializeInventory.itemStackArrayToBase64(currentContents);
                                        // open gui
                                        openGUI((Player) event.getWhoClicked(), "bounty-item-select", newOptions.page(), data);
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

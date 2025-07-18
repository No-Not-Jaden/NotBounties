package me.jadenp.notbounties.ui.gui;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.features.settings.display.BountyHunt;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.money.ExcludedItemException;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUI;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUIOptions;
import me.jadenp.notbounties.ui.gui.display_items.*;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.File;
import java.io.IOException;
import java.util.*;

import static me.jadenp.notbounties.features.ConfigOptions.saveConfigurationSection;

public class GUI implements Listener {

    public static final Map<UUID, PlayerGUInfo> playerInfo = new HashMap<>();
    private static final Map<UUID, Long> lastPageSwitch = new HashMap<>();
    private static final Map<String, GUIOptions> customGuis = new HashMap<>();
    private static final String[] allowedPausedGUIs = new String[]{"bounty-gui", "leaderboard", "view-bounty", "selection-gui"};
    private static final String GENERAL_CURRENCY_ITEM_NAME = "general-currency-item";
    private static final Map<String, CustomItem> customItems = new HashMap<>();

    public static void loadConfiguration(YamlConfiguration guiConfig) throws IOException {
        NotBounties bounties = NotBounties.getInstance();
        boolean guiChanges = false;

        customItems.clear();


        if (!guiConfig.isSet("set-whitelist")) {
            guiConfig.set("set-whitelist.sort-type", 3);
            guiConfig.set("set-whitelist.size", 54);
            guiConfig.set("set-whitelist.gui-name", "&d&lSelect &7&lWhitelisted &9&lPlayers");
            guiConfig.set("set-whitelist.add-page", false);
            guiConfig.set("set-whitelist.remove-page-items", true);
            guiConfig.set("set-whitelist.player-slots", Collections.singletonList("0-44"));
            guiConfig.set("set-whitelist.head-name", "&e&l{player}");
            guiConfig.set("set-whitelist.head-lore", Arrays.asList("", "&6Immunity: {amount}", "&7Click to toggle whitelist", ""));
            guiConfig.set("set-whitelist.layout.1.item", "fill");
            guiConfig.set("set-whitelist.layout.1.slot", "45-53");
            guiConfig.set("set-whitelist.layout.2.item", "return");
            guiConfig.set("set-whitelist.layout.2.slot", "49");
            guiConfig.set("set-whitelist.layout.3.item", "next");
            guiConfig.set("set-whitelist.layout.3.slot", "53");
            guiConfig.set("set-whitelist.layout.4.item", "back");
            guiConfig.set("set-whitelist.layout.4.slot", "45");
            guiConfig.set("set-whitelist.layout.5.item", "add-offline-whitelist");
            guiConfig.set("set-whitelist.layout.5.slot", "51");
            guiConfig.set("set-whitelist.layout.6.item", "reset-whitelist");
            guiConfig.set("set-whitelist.layout.6.slot", "47");
            guiConfig.set("custom-items.add-offline-whitelist.material", "LEVER");
            guiConfig.set("custom-items.add-offline-whitelist.amount", 1);
            guiConfig.set("custom-items.add-offline-whitelist.name", "&7See all players");
            guiConfig.set("custom-items.add-offline-whitelist.commands", Collections.singletonList("[gui] set-whitelist 1 offline"));
            guiConfig.set("custom-items.reset-whitelist.material", "MILK_BUCKET");
            guiConfig.set("custom-items.reset-whitelist.amount", 1);
            guiConfig.set("custom-items.reset-whitelist.name", "&fReset whitelist");
            guiConfig.set("custom-items.reset-whitelist.commands", Arrays.asList("[p] bounty whitelist reset", "[gui] set-whitelist 1"));
            guiChanges = true;
        }
        if (!guiConfig.isSet("confirm-bounty.layout")) {
            guiConfig.set("confirm-bounty.sort-type", 1);
            guiConfig.set("confirm-bounty.size", 54);
            guiConfig.set("confirm-bounty.gui-name", "&6&lBounty Cost: &2{amount_tax}");
            guiConfig.set("confirm-bounty.add-page", false);
            guiConfig.set("confirm-bounty.remove-page-items", true);
            guiConfig.set("confirm-bounty.player-slots", Collections.singletonList("13"));
            guiConfig.set("confirm-bounty.head-name", "&e&lSet bounty of {amount}");
            guiConfig.set("confirm-bounty.head-lore", Arrays.asList("", "&7{player}", ""));
            guiConfig.set("confirm-bounty.layout.1.item", "fill");
            guiConfig.set("confirm-bounty.layout.1.slot", "0-53");
            guiConfig.set("confirm-bounty.layout.2.item", "return-select-price");
            guiConfig.set("confirm-bounty.layout.2.slot", "49");
            guiConfig.set("confirm-bounty.layout.3.item", "false");
            guiConfig.set("confirm-bounty.layout.3.slot", "19-21");
            guiConfig.set("confirm-bounty.layout.4.item", "false");
            guiConfig.set("confirm-bounty.layout.4.slot", "37-39");
            guiConfig.set("confirm-bounty.layout.5.item", "false");
            guiConfig.set("confirm-bounty.layout.5.slot", "28-30");
            guiConfig.set("confirm-bounty.layout.6.item", "yes-bounty");
            guiConfig.set("confirm-bounty.layout.6.slot", "23-25");
            guiConfig.set("confirm-bounty.layout.7.item", "yes-bounty");
            guiConfig.set("confirm-bounty.layout.7.slot", "32-34");
            guiConfig.set("confirm-bounty.layout.8.item", "yes-bounty");
            guiConfig.set("confirm-bounty.layout.8.slot", "41-43");
            guiConfig.set("custom-items.yes-bounty.material", "LIME_STAINED_GLASS_PANE");
            guiConfig.set("custom-items.yes-bounty.amount", 1);
            guiConfig.set("custom-items.yes-bounty.name", "&a&lYes");
            guiConfig.set("custom-items.yes-bounty.commands", Arrays.asList("[p] notbounties {slot13} {page} --confirm", "[close]"));
            guiConfig.set("custom-items.return-set-bounty.material", "WHITE_BED");
            guiConfig.set("custom-items.return-set-bounty.amount", 1);
            guiConfig.set("custom-items.return-set-bounty.name", "&6&lReturn");
            guiConfig.set("custom-items.return-set-bounty.lore", Collections.singletonList("&7Return to player selection"));
            guiConfig.set("custom-items.return-set-bounty.commands", Collections.singletonList("[gui] set-bounty 1"));
            guiConfig.set("custom-items.return-select-price.material", "WHITE_BED");
            guiConfig.set("custom-items.return-select-price.amount", 1);
            guiConfig.set("custom-items.return-select-price.name", "&6&lReturn");
            guiConfig.set("custom-items.return-select-price.lore", Collections.singletonList("&7Return to price selection"));
            guiConfig.set("custom-items.return-select-price.commands", Collections.singletonList("[gui] select-price {page}"));
            guiChanges = true;
        }
        if (!guiConfig.isSet("bounty-item-select")) {
            saveConfigurationSection("gui.yml", guiConfig, "bounty-item-select");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.yes-bounty-item");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.cancel");
            guiChanges = true;
        }
        if (!guiConfig.isSet("challenges")) {
            saveConfigurationSection("gui.yml", guiConfig, "challenges");
            guiChanges = true;
        }
        if (!guiConfig.isSet("view-bounty")) {
            saveConfigurationSection("gui.yml", guiConfig, "view-bounty");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.general-currency-item");
            guiChanges = true;
        }
        if (!guiConfig.isSet("confirm-remove-immunity")) {
            saveConfigurationSection("gui.yml", guiConfig, "confirm-remove-immunity");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.remove-immunity");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.yes-remove-immunity");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.no-setting");
            guiChanges = true;
        }
        if (!guiConfig.isSet("bounty-hunt-player")) {
            saveConfigurationSection("gui.yml", guiConfig, "bounty-hunt-player");
            saveConfigurationSection("gui.yml", guiConfig, "bounty-hunt-time");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.enter-hunt-time");
            saveConfigurationSection("gui.yml", guiConfig, "custom-items.return-hunt-player");
            guiChanges = true;
        }
        File guiFile = new File(bounties.getDataFolder() + File.separator + "gui.yml");
        if (guiChanges) {
            guiConfig.save(guiFile);
        }
        for (String key : Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items")).getKeys(false)) {
            CustomItem customItem = new CustomItem(Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items." + key)));
            customItems.put(key, customItem);
        }
    }

    public static CustomItem getGeneralCurrencyItem() {
        return customItems.computeIfAbsent(GENERAL_CURRENCY_ITEM_NAME, k -> new CustomItem(Material.SUNFLOWER, 1, -1, "{currency}", new ArrayList<>(), true, true, false, new ArrayList<>(), null));
    }

    public static Map<String, CustomItem> getCustomItems() {
        return customItems;
    }

    public static void addGUI(GUIOptions gui, String name) {
        customGuis.put(name, gui);
    }

    public static GUIOptions getGUI(String guiName) {
        if (customGuis.containsKey(guiName))
            return customGuis.get(guiName);
        Bukkit.getLogger().warning("[NotBounties] Invalid GUI requested: \"" + guiName + "\"");
        Bukkit.getLogger().warning("[NotBounties] Ensure that you have this GUI configured in the plugins/NotBounties/gui.yml file.");
        return null;
    }

    public static int getMaxBountyItemSlots() {
        if (customGuis.containsKey("bounty-item-select")) {
            return customGuis.get("bounty-item-select").getPlayerSlots().size() - 1;
        }
        return 54;
    }

    public static List<DisplayItem> getGUIValues(Player player, String name, long page, Object[] data) {
        List<DisplayItem> displayItems = new ArrayList<>();
        if (!customGuis.containsKey(name))
            return displayItems;
        GUIOptions gui = customGuis.get(name);
        if (page < 1)
            page = 1;

        boolean online = (data.length == 0 || !(data[0] instanceof String) || !((String) data[0]).equalsIgnoreCase("offline"));
        Set<UUID> onlinePlayers = NotBounties.getNetworkPlayers().keySet();
        switch (name) {
            case "bounty-gui":
                List<Bounty> sortedList = BountyManager.getAllBounties(gui.getSortType());
                for (int i = 0; i < sortedList.size(); i++) {
                    Bounty bounty = sortedList.get(i);
                    double bountyAmount = Whitelist.isShowWhitelistedBounties() || player.hasPermission(NotBounties.getAdminPermission()) ? bounty.getTotalDisplayBounty() : bounty.getTotalDisplayBounty(player);
                    if (bountyAmount > 0) {
                        List<String> additionalLore = GUIClicks.getClickLore(player, ConfigOptions.getMoney().isBuyOwn() && bounty.getUUID().equals(player.getUniqueId()), (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply()));
                        if (bounty.getAllWhitelists().contains(player.getUniqueId()) && Whitelist.isEnableBlacklist()) {
                            additionalLore.addAll(LanguageOptions.getListMessage("whitelist-notify"));
                        } else if (!bounty.getAllBlacklists().isEmpty() && !bounty.getAllBlacklists().contains(player.getUniqueId()) && Whitelist.isEnabled()) {
                            additionalLore.addAll(LanguageOptions.getListMessage("whitelist-notify"));
                        } else if (Whitelist.isShowWhitelistedBounties() || player.hasPermission(NotBounties.getAdminPermission())) {
                            // not whitelisted
                            for (Setter setter : bounty.getSetters()) {
                                if (!setter.canClaim(player)) {
                                    additionalLore.addAll(LanguageOptions.getListMessage("not-whitelisted"));
                                    break;
                                }
                            }
                        }
                        if (onlinePlayers.contains(bounty.getUUID())) {
                            displayItems.add(new WhitelistedPlayerItem(bounty.getUUID(), bountyAmount, Leaderboard.CURRENT, i, bounty.getLatestUpdate(), additionalLore, ""));
                        } else {
                            displayItems.add(new PlayerItem(bounty.getUUID(), bountyAmount, Leaderboard.CURRENT, i, bounty.getLatestUpdate(), additionalLore));
                        }

                    }
                    if (ConfigOptions.isReducePageCalculations() && displayItems.size() > gui.getPlayerSlots().size() * page)
                        break;
                }
                break;
            case "view-bounty":
                if (data.length > 0 && data[0] instanceof UUID uuid) {
                    Bounty viewedBounty = BountyManager.getBounty(uuid);
                    if (viewedBounty != null) {
                        List<Setter> setters = new ArrayList<>(viewedBounty.getSetters());
                        List<String> additionalLore = player.hasPermission(NotBounties.getAdminPermission()) ? new ArrayList<>(LanguageOptions.getListMessage("admin-edit-lore")) : new ArrayList<>();
                        setters.sort(Comparator.comparing(Setter::getUuid)); // same setters will be next to each other
                        List<ItemStack> concurrentItems = new ArrayList<>();
                        double concurrentAmount = 0;
                        double concurrentDisplay = 0;
                        long latestSet = 0;
                        int settersAdded = 0;
                        for (int i = 0; i < setters.size(); i++) {
                            List<String> whitelistLore = new ArrayList<>();
                            Setter currentSetter = setters.get(i);
                            if (!currentSetter.canClaim(player) && !Whitelist.isShowWhitelistedBounties())
                                continue;

                            if (!currentSetter.getWhitelist().isBlacklist() && !currentSetter.getWhitelist().getList().isEmpty() && currentSetter.getWhitelist().getList().contains(player.getUniqueId()) && Whitelist.isEnabled()) {
                                whitelistLore.addAll(LanguageOptions.getListMessage("whitelist-notify"));
                            } else if (currentSetter.getWhitelist().isBlacklist() && !currentSetter.getWhitelist().getList().isEmpty() && !currentSetter.getWhitelist().getList().contains(player.getUniqueId()) && Whitelist.isEnabled()) {
                                whitelistLore.addAll(LanguageOptions.getListMessage("whitelist-notify"));
                            } else if (Whitelist.isShowWhitelistedBounties() || player.hasPermission(NotBounties.getAdminPermission())) {
                                // not whitelisted
                                for (Setter setter : viewedBounty.getSetters()) {
                                    if (!setter.canClaim(player)) {
                                        whitelistLore.addAll(LanguageOptions.getListMessage("not-whitelisted"));
                                        break;
                                    }
                                }
                            }
                            List<String> combinedLore = new ArrayList<>(additionalLore);
                            combinedLore.addAll(whitelistLore);
                            concurrentItems.addAll(currentSetter.getItems());
                            concurrentAmount += currentSetter.getAmount();
                            concurrentDisplay += currentSetter.getDisplayAmount();
                            if (currentSetter.getTimeCreated() > latestSet)
                                latestSet = currentSetter.getTimeCreated();
                            if (i == setters.size() - 1 || !currentSetter.getUuid().equals(setters.get(i + 1).getUuid())) {
                                // at the end of the list, or next setter is different
                                displayItems.add(new PlayerItem(currentSetter.getUuid(), concurrentDisplay, Leaderboard.CURRENT, settersAdded++, latestSet, combinedLore)); // add player head item
                                // add items for everything added
                                if (concurrentAmount != 0) {
                                    // add currency

                                    if (NumberFormatting.getCurrency().size() == 1 && !NumberFormatting.shouldUseDecimals()) {
                                        // change material and amount (possible multiple items) to represent a physical item
                                        ItemStack item = getGeneralCurrencyItem().getFormattedItem(player, null);
                                        try {
                                            Material material = Material.valueOf(NumberFormatting.getCurrency().get(0).toUpperCase());
                                            item.setType(material);
                                        } catch (IllegalArgumentException ignored) {
                                            // currency is not a placeholder and isn't a currency
                                        }
                                        // split items into groups of max stack size
                                        float valuePerItem = NumberFormatting.getCurrencyValues().get(NumberFormatting.getCurrency().get(0));
                                        int stacks = (int) (concurrentAmount / valuePerItem / item.getMaxStackSize());
                                        int remainder = (int) (concurrentAmount / valuePerItem % item.getMaxStackSize());

                                        // add items, ignoring the fact that we are using the unmodified class with a heavily modified item
                                        if (stacks > 0) {
                                            ItemStack parsedItem = item.clone();
                                            parsedItem.setAmount(parsedItem.getMaxStackSize());
                                            ItemMeta meta = parsedItem.getItemMeta();
                                            if (meta != null) {
                                                meta.setDisplayName(meta.getDisplayName().replace("{amount}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(valuePerItem * item.getMaxStackSize()) + NumberFormatting.getCurrencySuffix()));
                                                if (meta.hasLore() && meta.getLore() != null) {
                                                    List<String> lore = meta.getLore();
                                                    meta.setLore(lore.stream().map(string -> LanguageOptions.parse(string, valuePerItem * item.getMaxStackSize(), player)).toList());
                                                }
                                                parsedItem.setItemMeta(meta);
                                            }
                                            for (int j = 0; j < stacks; j++) {
                                                displayItems.add(new UnmodifiedItem(currentSetter.getUuid(), parsedItem, whitelistLore));
                                            }
                                        }
                                        if (remainder > 0) {
                                            ItemStack parsedItem = item.clone();
                                            parsedItem.setAmount(remainder);
                                            ItemMeta meta = parsedItem.getItemMeta();
                                            if (meta != null) {
                                                meta.setDisplayName(meta.getDisplayName().replace("{amount}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(valuePerItem * remainder) + NumberFormatting.getCurrencySuffix()));
                                                if (meta.hasLore() && meta.getLore() != null) {
                                                    List<String> lore = meta.getLore();
                                                    meta.setLore(lore.stream().map(string -> LanguageOptions.parse(string, valuePerItem * remainder, player)).toList());
                                                }
                                                parsedItem.setItemMeta(meta);
                                            }
                                            displayItems.add(new UnmodifiedItem(currentSetter.getUuid(), parsedItem, whitelistLore));
                                        }
                                    } else {
                                        // add default currency items
                                        displayItems.add(new CurrencyItem(currentSetter.getUuid(), concurrentAmount, whitelistLore));
                                    }
                                }
                                for (ItemStack item : concurrentItems) {
                                    displayItems.add(new UnmodifiedItem(currentSetter.getUuid(), item, whitelistLore));
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
                NotBounties.debugMessage("Viewing Online players: " + online + "  Online: " + onlinePlayers, false);
                    Set<UUID> addedPlayers = new HashSet<>();
                    for (Map.Entry<UUID, Double> entry : Leaderboard.IMMUNITY.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet()) {
                        if (online && cantSeePlayer(player, onlinePlayers, entry.getKey())) {
                            // skip if offline or vanished
                            continue;
                        }
                        displayItems.add(new PlayerItem(entry.getKey(), entry.getValue(), Leaderboard.IMMUNITY, addedPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                        addedPlayers.add(entry.getKey());
                    }
                    if (online) {
                        for (UUID uuid : onlinePlayers) {
                            if (ConfigOptions.isReducePageCalculations() && displayItems.size() > gui.getPlayerSlots().size() * page)
                                break;
                            if (!addedPlayers.contains(uuid) && !cantSeePlayer(player, onlinePlayers, uuid)) {
                                if (checkPermImmunity(uuid)) continue;
                                displayItems.add(new PlayerItem(uuid, 0, Leaderboard.IMMUNITY, addedPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                                addedPlayers.add(uuid);
                            }
                        }
                    } else {
                        for (Map.Entry<UUID, String> entry : LoggedPlayers.getLoggedPlayers().entrySet()) {
                            if (ConfigOptions.isReducePageCalculations() && displayItems.size() > gui.getPlayerSlots().size() * page)
                                break;
                            if (!addedPlayers.contains(entry.getKey())) {

                                ImmunityManager.ImmunityType immunityType = ImmunityManager.getAppliedImmunity(entry.getKey(), 69);
                                if (immunityType == ImmunityManager.ImmunityType.PERMANENT
                                        || immunityType == ImmunityManager.ImmunityType.GRACE_PERIOD
                                        || immunityType == ImmunityManager.ImmunityType.TIME
                                        || immunityType == ImmunityManager.ImmunityType.NEW_PLAYER) {
                                    // skip if they are immune
                                    continue;
                                }
                                displayItems.add(new PlayerItem(entry.getKey(), 0, Leaderboard.IMMUNITY, addedPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                                addedPlayers.add(entry.getKey());
                            }
                        }
                    }
                break;
            case "bounty-hunt-player":
                Set<UUID> addedBountyPlayers = new HashSet<>();
                for (Map.Entry<UUID, Double> entry : Leaderboard.CURRENT.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet()) {
                    if (online && cantSeePlayer(player, onlinePlayers, entry.getKey())) {
                        // skip if offline or vanished
                        continue;
                    }
                    displayItems.add(new PlayerItem(entry.getKey(), entry.getValue(), Leaderboard.IMMUNITY, addedBountyPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                    addedBountyPlayers.add(entry.getKey());
                }
                if (online) {
                    for (UUID uuid : onlinePlayers) {
                        if (ConfigOptions.isReducePageCalculations() && displayItems.size() > gui.getPlayerSlots().size() * page)
                            break;
                        if (!addedBountyPlayers.contains(uuid) && !cantSeePlayer(player, onlinePlayers, uuid)) {
                            if (checkPermImmunity(uuid)) continue;
                            displayItems.add(new PlayerItem(uuid, 0, Leaderboard.CURRENT, addedBountyPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                            addedBountyPlayers.add(uuid);
                        }
                    }
                } else {
                    for (Map.Entry<UUID, String> entry : LoggedPlayers.getLoggedPlayers().entrySet()) {
                        if (ConfigOptions.isReducePageCalculations() && displayItems.size() > gui.getPlayerSlots().size() * page)
                            break;
                        if (!addedBountyPlayers.contains(entry.getKey())) {

                            ImmunityManager.ImmunityType immunityType = ImmunityManager.getAppliedImmunity(entry.getKey(), 69);
                            if (immunityType == ImmunityManager.ImmunityType.PERMANENT
                                    || immunityType == ImmunityManager.ImmunityType.GRACE_PERIOD
                                    || immunityType == ImmunityManager.ImmunityType.TIME
                                    || immunityType == ImmunityManager.ImmunityType.NEW_PLAYER) {
                                // skip if they are immune
                                continue;
                            }
                            displayItems.add(new PlayerItem(entry.getKey(), 0, Leaderboard.CURRENT, addedBountyPlayers.size(), System.currentTimeMillis(), new ArrayList<>()));
                            addedBountyPlayers.add(entry.getKey());
                        }
                    }
                }
                break;
            case "set-whitelist":
                List<UUID> playersAdded = new ArrayList<>();
                Whitelist whitelist = DataManager.getPlayerData(player.getUniqueId()).getWhitelist();
                for (UUID uuid : whitelist.getList()) {
                    List<String> additionalLore = whitelist.isBlacklist() ? LanguageOptions.getListMessage("blacklist-lore") : LanguageOptions.getListMessage("whitelist-lore");
                    displayItems.add(new WhitelistedPlayerItem(uuid, Leaderboard.IMMUNITY.getStat(uuid), Leaderboard.IMMUNITY, playersAdded.size(), System.currentTimeMillis(), additionalLore, "&a"));
                    playersAdded.add(uuid);
                }
                for (Map.Entry<UUID, Double> entry : Leaderboard.IMMUNITY.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet()) {
                    if (!playersAdded.contains(entry.getKey())) {
                        if (online && cantSeePlayer(player, onlinePlayers, entry.getKey())) {
                            // skip if offline or vanished
                            continue;
                        }
                        displayItems.add(new PlayerItem(entry.getKey(), entry.getValue(), Leaderboard.IMMUNITY, playersAdded.size(), System.currentTimeMillis(), new ArrayList<>()));
                        playersAdded.add(entry.getKey());
                    }
                }
                for (Map.Entry<UUID, String> entry : LoggedPlayers.getLoggedPlayers().entrySet()) {
                    if (ConfigOptions.isReducePageCalculations() && playersAdded.size() > gui.getPlayerSlots().size() * page)
                        break;
                    if (!playersAdded.contains(entry.getKey())) {
                        if (online && cantSeePlayer(player, onlinePlayers, entry.getKey())) {
                            // skip if offline or vanished
                            continue;
                        }

                        displayItems.add(new PlayerItem(entry.getKey(), 0, Leaderboard.IMMUNITY, playersAdded.size(), System.currentTimeMillis(), new ArrayList<>()));
                        playersAdded.add(entry.getKey());
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
            case "bounty-hunt-time":
                if (data.length > 0 && data[0] instanceof UUID uuid) {
                    displayItems.add(new PlayerItem(uuid, page, Leaderboard.CURRENT, 0, System.currentTimeMillis(), new ArrayList<>()));
                } else {
                    displayItems.add(new PlayerItem(player.getUniqueId(), page, Leaderboard.CURRENT, 0, System.currentTimeMillis(), new ArrayList<>()));
                }
                break;
            case "bounty-item-select":
                UUID uuid3 = data.length > 0 && data[0] instanceof UUID uuid ? uuid : player.getUniqueId();
                double total = 0;
                ItemStack[][] items = data.length > 1 && data[1] instanceof ItemStack[][] itemStacks ? itemStacks : new ItemStack[1][getMaxBountyItemSlots()];
                for (ItemStack[] item : items) {
                    try {
                        total += NumberFormatting.getTotalValue(Arrays.asList(item));
                    } catch (ExcludedItemException ignored) {
                        // cant get value
                    }
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

    private static boolean checkPermImmunity(UUID uuid) {
        ImmunityManager.ImmunityType immunityType = ImmunityManager.getAppliedImmunity(uuid, 69);
        if (immunityType == ImmunityManager.ImmunityType.PERMANENT
                || immunityType == ImmunityManager.ImmunityType.GRACE_PERIOD
                || immunityType == ImmunityManager.ImmunityType.TIME
                || immunityType == ImmunityManager.ImmunityType.NEW_PLAYER) {
            // skip if they would be immune to this bounty regardless of the bounty amount
            NotBounties.debugMessage(uuid + " has immunity.", false);
            return true;
        }
        return false;
    }

    /**
     * Check if a player can see another player.
     * @param guiViewer Player that is viewing the other player.
     * @param onlinePlayers Current online players.
     * @param playerUUID Player to be viewed.
     * @return True if the player is offline, or is vanished.
     */
    private static boolean cantSeePlayer(Player guiViewer, Set<UUID> onlinePlayers, UUID playerUUID) {
        if (!onlinePlayers.contains(playerUUID))
            return true;
        if (NotBounties.getServerVersion() >= 17 && ConfigOptions.isSeePlayerList()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null)
                return true;
            return !guiViewer.canSee(player);
        }
        return false;
    }

    public static void openGUI(Player player, String name, long page, Object... data) {
        if (NotBounties.isPaused()) {
            boolean allowed = false;
            for (String str : allowedPausedGUIs) {
                if (str.equalsIgnoreCase(name)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                if (player.hasPermission(NotBounties.getAdminPermission()))
                    player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("paused"), player));
                return;
            }
        }

        if (page < 1)
            page = 1;

        long finalPage = page;
        NotBounties.getServerImplementation().async().runNow(() -> {
            List<DisplayItem> displayItems = getGUIValues(player, name, finalPage, data);

            if ((ConfigOptions.getIntegrations().isFloodgateEnabled() || ConfigOptions.getIntegrations().isGeyserEnabled()) && NotBounties.isBedrockPlayer(player.getUniqueId()) && BedrockGUI.isEnabled() && BedrockGUI.isGUIEnabled(name)) {
                // open bedrock gui
                BedrockGUI.openGUI(player, name, finalPage, displayItems, data);
            } else {
                // open java gui
                if (!customGuis.containsKey(name))
                    return;
                GUIOptions gui = customGuis.get(name);
                String title = createTitle(gui, player, finalPage, displayItems, data);
                PlayerGUInfo info = new PlayerGUInfo(finalPage, name, data, displayItems, title);
                Inventory inventory = gui.createInventory(player, finalPage, displayItems, title, data);
                NotBounties.getServerImplementation().global().run(() -> {
                    boolean guiOpen = playerInfo.containsKey(player.getUniqueId()) && gui.getType().equals(playerInfo.get(player.getUniqueId()).guiType()) && player.getOpenInventory().getTitle().equals(playerInfo.get(player.getUniqueId()).title());
                    playerInfo.put(player.getUniqueId(), info);
                    if (guiOpen) {
                        // already has the gui type open - update contents
                        player.getOpenInventory().getTopInventory().setContents(inventory.getContents());
                        if (NotBounties.getServerVersion() >= 19)
                            player.getOpenInventory().setTitle(title);
                    } else {
                        player.openInventory(inventory);
                    }
                    playerInfo.put(player.getUniqueId(), info);
                });

            }
        });


    }

    public static String createTitle(BedrockGUIOptions guiOptions, Player viewer, long page, List<DisplayItem> displayItems, Object[] data) {
        return createTitle(guiOptions.getName(), guiOptions.getType(), guiOptions.getMaxPlayers(), guiOptions.isAddPage(), viewer, page, displayItems, data);
    }
    private static String createTitle(GUIOptions guiOptions, Player viewer, long page, List<DisplayItem> displayItems, Object[] data) {
        return createTitle(guiOptions.getName(), guiOptions.getType(), guiOptions.getPlayerSlots().size(), guiOptions.isAddPage(), viewer, page, displayItems, data);
    }
    private static String createTitle(String name, String type, int numPlayerSlots, boolean addPage, Player viewer, long page, List<DisplayItem> displayItems, Object[] data) {
        String title = name;
        if (type.equals("leaderboard") && data.length > 0 && data[0] instanceof String string) {
            try {
                Leaderboard leaderboard = Leaderboard.valueOf(string.toUpperCase());
                title = title.replace("{leaderboard}", leaderboard.toString()).replace("{leaderboard_name}", leaderboard.getDisplayName());
            } catch (IllegalArgumentException ignored) {}
        }
        if (type.equals("view-bounty") && data.length > 0 && data[0] instanceof UUID uuid) {
            double amount = 0;
            Bounty bounty = BountyManager.getBounty(uuid);
            if (bounty != null) {
                amount = Whitelist.isShowWhitelistedBounties() ? bounty.getTotalDisplayBounty() : bounty.getTotalDisplayBounty(viewer);
            }
            String playerName = LoggedPlayers.getPlayerName(uuid);
            title = title.replace("{player}", playerName).replace("{amount}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(amount) + NumberFormatting.getCurrencySuffix());
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
        } else if (type.equals("bounty-hunt-time")) {
            maxPage = (int) (NumberFormatting.getBalance(viewer) * BountyHunt.getCostPerMinute());
        } else if (type.equals("bounty-gui")) {
            int numBounties = BountyManager.getAllBounties(-1).size();
            int playerSlots = Math.max(numPlayerSlots, 1);
            maxPage = (int) ((double) numBounties / playerSlots);
            if (numBounties % playerSlots != 0) {
                maxPage++;
            }
        } else if (ConfigOptions.isReducePageCalculations()) {
            int totalPlayers = LoggedPlayers.getLoggedPlayers().size();
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
        return LanguageOptions.parse(title, viewer);
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
        if (!NotBounties.getInstance().isEnabled())
            shutdown = true;
        PlayerGUInfo guInfo = playerInfo.get(player.getUniqueId());
        if (guInfo.guiType().equals("bounty-item-select")){
            GUIOptions guiOptions = GUI.getGUI("bounty-item-select");
            if (guiOptions == null) {
                Bukkit.getLogger().warning("[NotBounties] bounty-item-select GUI not setup.");
                return;
            }
            // give back items
            if (guInfo.data().length > 1 && guInfo.data()[1] instanceof ItemStack[][] allItems) {
                // the data doesn't currently hold the correct items for the opened page
                // update current contents
                ItemStack[] currentContents = new ItemStack[getMaxBountyItemSlots()];
                for (int i = 0; i < currentContents.length; i++) {
                    currentContents[i] = inventory.getContents()[guiOptions.getPlayerSlots().get(i + 1)];
                }
                if (allItems.length >= guInfo.page()) {
                    // has space for the page
                    allItems[(int) (guInfo.page() - 1)] = currentContents;
                    currentContents = new ItemStack[0]; // clear contents because both lists will be added
                }

                // give items back
                if (shutdown) {
                    // plugin is shutting down, add to refund
                    for (ItemStack[] items : allItems) {
                        List<ItemStack> addedItems = Arrays.stream(items).filter(Objects::nonNull).toList();
                        if (!addedItems.isEmpty())
                            BountyManager.refundPlayer(player.getUniqueId(), 0, addedItems, LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-gui-shutdown"), player));
                    }

                    // add current items (maybe empty)
                    List<ItemStack> addedItems = Arrays.stream(currentContents).filter(Objects::nonNull).toList();
                    if (!addedItems.isEmpty())
                        BountyManager.refundPlayer(player.getUniqueId(), 0, addedItems, LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-gui-shutdown"), player));
                } else {
                    // can give items
                    for (ItemStack[] items : allItems) {
                        List<ItemStack> addedItems = Arrays.stream(items).filter(Objects::nonNull).toList();
                        if (!addedItems.isEmpty())
                            NumberFormatting.givePlayer(player, addedItems, true);
                    }

                    // add current items (maybe empty)
                    List<ItemStack> addedItems = Arrays.stream(currentContents).filter(Objects::nonNull).toList();
                    if (!addedItems.isEmpty())
                        NumberFormatting.givePlayer(player, addedItems, true);
                }
            }
        }
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
        int pageAddition = guiType.equals("select-price") || guiType.equals("confirm-bounty") || guiType.equals("bounty-hunt-time") ? 0 : (int) ((info.page() - 1) * gui.getPlayerSlots().size());
        if (gui.getPlayerSlots().contains(event.getRawSlot()) && (event.getCurrentItem() != null && gui.getPlayerSlots().indexOf(event.getRawSlot()) + pageAddition < info.displayItems().size() && event.getCurrentItem().getType() == Material.PLAYER_HEAD)) {
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            assert meta != null;
            DisplayItem displayItem = info.displayItems().get(gui.getPlayerSlots().indexOf(event.getRawSlot()) + pageAddition);
            UUID playerUUID = displayItem instanceof PlayerItem playerItem ? playerItem.getUuid() : event.getWhoClicked().getUniqueId();
            String playerName = LoggedPlayers.getPlayerName(playerUUID);
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
                                String messageText = LanguageOptions.parse(LanguageOptions.getMessage("edit-setter-clickable").replace("{player}", playerName).replace("{receiver}", viewedBounty.getName()), (OfflinePlayer) event.getWhoClicked());
                                TextComponent message = LanguageOptions.getTextComponent(messageText);
                                TextComponent prefix =  LanguageOptions.getTextComponent(LanguageOptions.parse(LanguageOptions.getPrefix(), (OfflinePlayer) event.getWhoClicked()));
                                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(messageText)));
                                message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " edit " + viewedBounty.getName() + " from " + playerName + " "));
                                prefix.addExtra(message);
                                event.getWhoClicked().spigot().sendMessage(prefix);
                            } else if (event.isLeftClick()) {
                                event.getView().close();
                                Bukkit.dispatchCommand(event.getWhoClicked(), ConfigOptions.getPluginBountyCommands().get(0) + " remove " + viewedBounty.getName() + " from " + playerName);
                            }
                        } else {
                            // no longer has a bounty
                            openGUI((Player) event.getWhoClicked(), "bounty-gui", 1);
                            event.getWhoClicked().sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("no-bounty"), Bukkit.getOfflinePlayer(uuid)));
                        }
                    }
                    break;
                case "set-bounty":
                    if (NumberFormatting.isBountyItemsDefaultGUI()) {
                        openGUI((Player) event.getWhoClicked(), "bounty-item-select", 1, playerUUID);
                    } else {
                        openGUI((Player) event.getWhoClicked(), "select-price", (long) ConfigOptions.getMoney().getMinBounty(), playerUUID.toString());
                    }
                    break;
                case "bounty-hunt-player":
                    openGUI((Player) event.getWhoClicked(), "bounty-hunt-time", BountyHunt.getMinimumMinutes(), playerUUID);
                    break;
                case "set-whitelist":
                    Set<UUID> whitelist = DataManager.getPlayerData(event.getWhoClicked().getUniqueId()).getWhitelist().getList();
                    if (!whitelist.remove(playerUUID)) {
                        if (whitelist.size() < 10)
                            whitelist.add(playerUUID);
                        else
                            event.getWhoClicked().sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("whitelist-max"), (Player) event.getWhoClicked()));
                    }
                    openGUI((Player) event.getWhoClicked(), "set-whitelist", 1, info.data());
                    break;
                case "select-price":
                    Bukkit.dispatchCommand(event.getWhoClicked(),   ConfigOptions.getPluginBountyCommands().get(0) + " " + playerName + " " + playerInfo.get(event.getWhoClicked().getUniqueId()).page());
                    if (!ConfigOptions.isBountyConfirmation())
                        event.getView().close();
                    break;
                case "bounty-hunt-time":
                    Bukkit.dispatchCommand(event.getWhoClicked(),   ConfigOptions.getPluginBountyCommands().get(0) + " hunt " + playerName + " " + playerInfo.get(event.getWhoClicked().getUniqueId()).page());
                    event.getView().close();
                    break;
                case "bounty-item-select":
                    if (!gui.getPlayerSlots().isEmpty() && event.getRawSlot() ==  gui.getPlayerSlots().get(0)) {
                        // set bounty
                        ActionCommands.executeCommands((Player) event.getWhoClicked(),  new ArrayList<>(Collections.singletonList("[p] " + ConfigOptions.getPluginBountyCommands().get(0) + " {data} --confirm")));
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
                    if (event.getCurrentItem() != null) {
                        try {
                            NumberFormatting.getItemValue(event.getCurrentItem());
                        } catch (ExcludedItemException e) {
                            event.getWhoClicked().sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("excluded-bounty-item").replace("{material}", e.getMessage()), (OfflinePlayer) event.getWhoClicked()));
                            event.setCancelled(true);
                            return;
                        }
                    }


                    if (!bottomInventory)
                        // reopen gui to update tax
                        NotBounties.getServerImplementation().entity(event.getWhoClicked()).runDelayed(() -> {
                            // check to make sure the gui is still open
                            if (!((Player) event.getWhoClicked()).isOnline())
                                return;
                            if (playerInfo.containsKey(event.getWhoClicked().getUniqueId())) {
                                PlayerGUInfo newOptions = playerInfo.get(event.getWhoClicked().getUniqueId());
                                if (info.page() == newOptions.page() && newOptions.guiType().equals(info.guiType())) {
                                    // page hasn't changed
                                    GUIOptions guiOptions = GUI.getGUI("bounty-item-select");
                                    if (guiOptions == null)
                                        return; // this shouldn't be reached
                                    // read current inventory
                                    ItemStack[] currentContents = new ItemStack[GUI.getMaxBountyItemSlots()];
                                    for (int i = 0; i < currentContents.length; i++) {
                                        currentContents[i] = event.getInventory().getContents()[guiOptions.getPlayerSlots().get(i + 1)];
                                    }
                                    // get all items
                                    ItemStack[][] allItems = newOptions.data().length > 1 && newOptions.data()[1] instanceof ItemStack[][] itemStacks ? itemStacks : new ItemStack[GUI.getMaxBountyItemSlots()][(int) newOptions.page()];
                                    allItems[(int) newOptions.page()-1] = currentContents; // set current content
                                    // open gui
                                    openGUI((Player) event.getWhoClicked(), "bounty-item-select", newOptions.page(), newOptions.data()[0], allItems);
                                }
                            }
                        }, 1);
                }
                return;
            }
            // check for page switch cooldown
            if (lastPageSwitch.containsKey(event.getWhoClicked().getUniqueId()) && System.currentTimeMillis() - lastPageSwitch.get(event.getWhoClicked().getUniqueId()) < 50) {
                // clicking too fast
                return;
            }
            ActionCommands.executeCommands((Player) event.getWhoClicked(), customItem.getCommands());
            lastPageSwitch.put(event.getWhoClicked().getUniqueId(), System.currentTimeMillis());
        }
    }

}

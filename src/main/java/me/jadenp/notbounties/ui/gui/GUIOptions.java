package me.jadenp.notbounties.ui.gui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.HeadFetcher;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.BountyManager.getBounty;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.*;

public class GUIOptions {
    private final List<Integer> playerSlots; // this size of this is how many player slots per page
    private final int size;
    private final int sortType;
    private final CustomItem[] customItems;
    private final String name;
    private final boolean removePageItems;
    private final String headName;
    private final List<String> headLore;
    private final boolean addPage;
    private final String type;
    private final Map<Integer, CustomItem> pageReplacements = new HashMap<>();

    public GUIOptions(ConfigurationSection settings) {
        type = settings.getName();
        name = settings.isSet("gui-name") ? color(settings.getString("gui-name")) : "Custom GUI";
        playerSlots = new ArrayList<>();
        size = settings.isSet("size") ? settings.getInt("size") : 54;
        addPage = settings.isSet("add-page") && settings.getBoolean("add-page");
        sortType = settings.isSet("sort-type") ? settings.getInt("sort-type") : 1;
        removePageItems = settings.getBoolean("remove-page-items");
        headName = settings.isSet("head-name") ? settings.getString("head-name") : "{player}";
        headLore = settings.isSet("head-lore") ? settings.getStringList("head-lore") : new ArrayList<>();
        customItems = new CustomItem[size];
        if (settings.isConfigurationSection("layout"))
            for (String key : Objects.requireNonNull(settings.getConfigurationSection("layout")).getKeys(false)) {
                String item = settings.getString("layout." + key + ".item");
                int[] slots = new int[0];
                if (settings.isList("layout." + key + ".slot")) {
                    List<String> slotsList = settings.getStringList("layout." + key + ".slot");
                    for (String slot : slotsList) {
                        int[] newSlots = getRange(slot);
                        int[] tempSlots = new int[slots.length + newSlots.length];
                        System.arraycopy(slots, 0, tempSlots, 0, slots.length);
                        System.arraycopy(newSlots, 0, tempSlots, slots.length, newSlots.length);
                        slots = tempSlots;
                    }
                } else {
                    slots = getRange(settings.getString("layout." + key + ".slot"));
                }

                //Bukkit.getLogger().info(item);
                if (ConfigOptions.customItems.containsKey(item)) {
                    CustomItem customItem = ConfigOptions.customItems.get(item);
                    for (int i : slots) {
                        if (i >= customItems.length) {
                            Bukkit.getLogger().warning("[NotBounties] Error loading custom item in GUI: " + type);
                            Bukkit.getLogger().warning("Slot " + i + " is bigger than the inventory size! (max=" + (size-1) + ")");
                        }
                        //Bukkit.getLogger().info(i + "");
                        if (customItems[i] != null) {
                            if (getPageType(customItem.getCommands()) > 0) {
                                pageReplacements.put(i, customItems[i]);
                            }
                        }
                        customItems[i] = customItem;
                    }
                } else {
                    // unknown item
                    Bukkit.getLogger().warning("Unknown item \"" + item + "\" in gui: " + settings.getName());
                }

            }
        for (String bSlots : settings.getStringList("player-slots")) {
            int[] range = getRange(bSlots);
            for (int j : range) {
                playerSlots.add(j);
            }
        }
    }

    public boolean isAddPage() {
        return addPage;
    }

    public CustomItem getCustomItem(int slot, long page, int entryAmount) {
        // Check if slot is in bounds (-999 is a click outside the GUI)
        if (slot >= customItems.length || slot == -999)
            return null;
        CustomItem item = customItems[slot];
        if (item == null)
            return null;
        if (!type.equals("select-price")) {
            // next
            if (getPageType(item.getCommands()) == 1 && page * playerSlots.size() >= entryAmount) {
                if (pageReplacements.containsKey(slot))
                    return pageReplacements.get(slot);
                return null;
            }
            // back
            if (getPageType(item.getCommands()) == 2 && page == 1) {
                if (pageReplacements.containsKey(slot))
                    return pageReplacements.get(slot);
                return null;
            }
        }
        return item;
    }

    public String getTitle(Player player, long page, LinkedHashMap<UUID, String> values, String... replacements) {
        OfflinePlayer[] playerItems = new OfflinePlayer[values.size()];
        String[] amount = new String[values.size()];
        int index = 0;
        for (Map.Entry<UUID, String> entry : values.entrySet()) {
            playerItems[index] = Bukkit.getOfflinePlayer(entry.getKey());
            amount[index] = entry.getValue();
            index++;
        }
        // this is for adding more replacements in the future
        int desiredLength = 1;
        if (replacements.length < desiredLength)
            replacements = new String[desiredLength];
        for (int i = 0; i < replacements.length; i++) {
            if (replacements[i] == null)
                replacements[i] = "";
        }

        if (page < 1) {
            page = 1;
        }
        return getTitle(player, page, amount, playerItems, replacements);
    }

    private String getTitle(Player player, long page, String[] amount, OfflinePlayer[] playerItems, String[] replacements) {
        String name = addPage ? this.name + " " + page : this.name;
        if (amount.length > 0) {
            double tax = parseCurrency(amount[0]) * (bountyTax) + NotBounties.getPlayerWhitelist(player.getUniqueId()).getList().size() * bountyWhitelistCost;
            double totalCost = parseCurrency(amount[0]) + tax;
            String playerName = NotBounties.getPlayerName(playerItems[0].getUniqueId());
            name = name.replace("{amount}", (amount[0]))
                    .replace("{amount_tax}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalCost) + NumberFormatting.currencySuffix))
                    .replace("{player}", playerName)
                    .replace("{tax}", NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(tax) + NumberFormatting.currencySuffix);
            if (type.equals("leaderboard"))
                name = name.replace("{leaderboard}", (replacements[0]));
        }
        name = name.replace("{page}", (page + ""));
        int maxPage = type.equals("select-price") ? (int) NumberFormatting.getBalance(player) : (playerItems.length / Math.max(playerSlots.size(), 1)) + 1;
        name = name.replace("{page_max}", (maxPage + ""));
        name = parse(name, player);

        return name;
    }

    /**
     * Get the formatted custom inventory
     *
     * @param player Player that owns the inventory and will be parsed for any placeholders
     * @param page   Page of gui - This will change the items in player slots and page items if enabled
     * @return Custom GUI Inventory
     */
    public Inventory createInventory(Player player, long page, LinkedHashMap<UUID, String> values, String... replacements) {
        OfflinePlayer[] playerItems = new OfflinePlayer[values.size()];
        String[] amount = new String[values.size()];
        int index = 0;
        for (Map.Entry<UUID, String> entry : values.entrySet()) {
            playerItems[index] = Bukkit.getOfflinePlayer(entry.getKey());
            amount[index] = entry.getValue();
            index++;
        }
        // this is for adding more replacements in the future
        int desiredLength = 1;
        if (replacements.length < desiredLength)
            replacements = new String[desiredLength];
        for (int i = 0; i < replacements.length; i++) {
            if (replacements[i] == null)
                replacements[i] = "";
        }

        if (page < 1) {
            page = 1;
        }

        String title = getTitle(player, page, amount, playerItems, replacements);
        Inventory inventory = Bukkit.createInventory(player, size, title);
        ItemStack[] contents = inventory.getContents();
        // set up regular items
        for (int i = 0; i < contents.length; i++) {
            if (customItems[i] == null)
                continue;
            // check if item is a page item
            String[] leaderboardReplacements = type.equals("leaderboard") ? replacements : new String[0];
            if (removePageItems) {
                // next
                if (getPageType(customItems[i].getCommands()) == 1 && page * playerSlots.size() >= amount.length) {
                    ItemStack replacement = pageReplacements.containsKey(i) ? pageReplacements.get(i).getFormattedItem(player, leaderboardReplacements) : null;
                    contents[i] = replacement;
                    continue;
                }
                // back
                if (getPageType(customItems[i].getCommands()) == 2 && page == 1) {
                    ItemStack replacement = pageReplacements.containsKey(i) ? pageReplacements.get(i).getFormattedItem(player, leaderboardReplacements) : null;
                    contents[i] = replacement;
                    continue;
                }
            }
            contents[i] = customItems[i].getFormattedItem(player, leaderboardReplacements);
        }
        // set up player slots
        LinkedHashMap<OfflinePlayer, ItemStack> unloadedHeads = new LinkedHashMap<>();
        boolean isSinglePlayerSlot = type.equals("select-price") || type.equals("confirm-bounty") || type.equals("bounty-item-select");
        for (int i = isSinglePlayerSlot ? 0 : (int) ((page - 1) * playerSlots.size()); i < Math.min(playerSlots.size() * page, playerItems.length); i++) {
            final OfflinePlayer p = playerItems[i];
            ItemStack skull = HeadFetcher.getUnloadedHead(p.getUniqueId());
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            assert meta != null;

            long time = System.currentTimeMillis();
            Bounty bounty = getBounty(p.getUniqueId());
            if (type.equals("bounty-gui")) {
                amount[i] = currencyPrefix + NumberFormatting.formatNumber(tryParse(amount[i])) + currencySuffix;
            } else {
                amount[i] = formatNumber(amount[i]);
            }
            if (bounty != null) {
                time = bounty.getLatestSetter();
            }
            final String finalAmount = type.equals("bounty-item-select") ? amount[0] : amount[i];
            long finalTime = time;
            final long rank = i + 1L;
            String[] finalReplacements = replacements;
            String playerName = NotBounties.getPlayerName(p.getUniqueId());
            List<String> lore = new ArrayList<>(headLore);
            double tax = parseCurrency(finalAmount) * (bountyTax) + NotBounties.getPlayerWhitelist(player.getUniqueId()).getList().size() * bountyWhitelistCost;
            double total = parseCurrency(finalAmount) + tax;
            try {
                meta.setDisplayName(parse(headName.replace("{tax}", NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(tax) + NumberFormatting.currencySuffix), finalAmount, rank, finalReplacements[0], playerName, total,  finalTime, p));
                lore.replaceAll(s -> parse(s.replace("{tax}", NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(tax) + NumberFormatting.currencySuffix), finalAmount, rank, finalReplacements[0], playerName, total,  finalTime, p));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Error parsing name and lore for player item! This is usually caused by a typo in the config.");
            }
            // extra lore stuff
            if (type.equals("bounty-gui")) {
                if (player.hasPermission("notbounties.admin")) {
                    adminEditLore.stream().map(bbLore -> parse(bbLore, player)).forEach(lore::add);
                } else {
                    if (giveOwnMap) {
                        givePosterLore.stream().map(str -> parse(str, player)).forEach(lore::add);
                    }
                    if (buyBack && p.getUniqueId().equals(player.getUniqueId())) {
                        buyBackLore.stream().map(bbLore -> parse(bbLore, (parseCurrency(finalAmount) * buyBackInterest), player)).forEach(lore::add);
                    } else if (BountyTracker.isEnabled() && (BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers()) && player.hasPermission("notbounties.tracker")) {
                        giveTrackerLore.stream().map(str -> parse(str, player)).forEach(lore::add);
                    }
                }
                // whitelist

                if (bounty != null) {
                    if (bounty.getAllWhitelists().contains(player.getUniqueId())) {
                        whitelistNotify.stream().map(str -> parse(str, player)).forEach(lore::add);
                    } else if (!bounty.getAllBlacklists().isEmpty() && !bounty.getAllBlacklists().contains(player.getUniqueId())) {
                        whitelistNotify.stream().map(str -> parse(str, player)).forEach(lore::add);
                    } else if (showWhitelistedBounties || player.hasPermission("notbounties.admin")) {
                        // not whitelisted
                        for (Setter setter : bounty.getSetters()) {
                            if (!setter.canClaim(player)) {
                                notWhitelistedLore.stream().map(str -> parse(str, player)).forEach(lore::add);
                                break;
                            }
                        }
                    }
                }
            }
            if (type.equalsIgnoreCase("set-whitelist"))
                if (NotBounties.getPlayerWhitelist(player.getUniqueId()).getList().contains(p.getUniqueId())) {
                    if (NotBounties.isAboveVersion(20, 4)) {
                        if (!meta.hasEnchantmentGlintOverride())
                            meta.setEnchantmentGlintOverride(true);
                    } else {
                        skull.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    if (NotBounties.getPlayerWhitelist(player.getUniqueId()).isBlacklist())
                        blacklistLore.stream().map(str -> parse(str, player)).forEach(lore::add);
                    else
                        whitelistLore.stream().map(str -> parse(str, player)).forEach(lore::add);
                }
            meta.setLore(lore);
            skull.setItemMeta(meta);
            unloadedHeads.put(p, skull);
            int slot = isSinglePlayerSlot ? playerSlots.get(i) : (playerSlots.get((int) (i - playerSlots.size() * (page-1))));
            contents[slot] = skull;
        }
        new HeadFetcher().loadHeads(player, new PlayerGUInfo(page, type, new Object[0], values.keySet().toArray(new UUID[0]), title), unloadedHeads);
        if  (type.equals("bounty-item-select") && !replacements[0].isEmpty()) {
            // load in saved items
            ItemStack[] savedContents = new ItemStack[0];
            try {
                savedContents = SerializeInventory.itemStackArrayFromBase64(replacements[0]);
            } catch (IOException e) {
                Bukkit.getLogger().warning("[NotBounties] Couldn't read saved inventory contents: " + replacements[0]);
                Bukkit.getLogger().warning(e.toString());
            }
            for (int i = 0; i < savedContents.length; i++) {
                contents[playerSlots.get(i+1)] = savedContents[i];
            }
        }

        inventory.setContents(contents);
        return inventory;

    }


    public int getSortType() {
        return sortType;
    }

    public List<Integer> getPlayerSlots() {
        return playerSlots;
    }

    public String getName() {
        return name;
    }

    /**
     * Get an array of desired numbers from a string from (x)-(y). Both x and y are inclusive.
     * <p>"1" -> [1]</p>
     * <p>"3-6" -> [3, 4, 5, 6]</p>
     *
     * @param str String to parse
     * @return desired range of numbers sorted numerically or an empty list if there is a formatting error
     */
    private static int[] getRange(String str) {
        try {
            // check if it is a single number
            int i = Integer.parseInt(str);
            // return if an exception was not thrown
            return new int[]{i};
        } catch (NumberFormatException e) {
            // there is a dash we need to get out
        }
        String[] split = str.split("-");
        try {
            int bound1 = Integer.parseInt(split[0]);
            int bound2 = Integer.parseInt(split[1]);
            int[] result = new int[Math.abs(bound1 - bound2) + 1];

            for (int i = Math.min(bound1, bound2); i < Math.max(bound1, bound2) + 1; i++) {
                //Bukkit.getLogger().info(i + "");
                result[i - Math.min(bound1, bound2)] = i;
            }
            return result;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // formatting error
            return new int[0];
        }
    }

    /**
     * Parses through commands for "[next]" and "[back]"
     *
     * @param commands Commands of the CustomItem
     * @return 1 for next, 2 for back, 0 for no page item
     */
    public static int getPageType(List<String> commands) {
        for (String command : commands) {
            if (command.startsWith("[next]"))
                return 1;
            if (command.startsWith("[back]"))
                return 2;
        }
        return 0;
    }

    public String getType() {
        return type;
    }
}


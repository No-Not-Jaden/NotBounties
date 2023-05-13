package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.jadenp.notbounties.ConfigOptions.*;

public class GUI implements Listener {

    public static final Map<UUID, Integer> pageNumber = new HashMap<>();
    private static final Map<String, GUIOptions> customGuis = new HashMap<>();
    public static void addGUI(GUIOptions gui, String name){
        customGuis.put(name, gui);
    }
    public static GUIOptions getGUI(String guiName){
        if (customGuis.containsKey(guiName))
            return customGuis.get(guiName);
        return null;
    }

    public GUI(){}

    public static void openGUI(Player player, String name, int page, Object... data) {
        if (!customGuis.containsKey(name))
            return;
        GUIOptions gui = customGuis.get(name);

        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        NotBounties nb = NotBounties.getInstance();
        String[] replacements = new String[0];
        switch (name){
            case "bounty-gui":
                List<Bounty> sortedList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.sortBounties(gui.getSortType());
                for (Bounty bounty : sortedList)
                    values.put(bounty.getUUID(), bounty.getTotalBounty());
                break;
            case "leaderboard":
                Leaderboard leaderboard = (Leaderboard) data[0];
                values = leaderboard.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                replacements = new String[]{leaderboard.toString()};
                break;
            case "set-bounty":
                values = Leaderboard.IMMUNITY.getSortedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                break;
            case "select-price":
                String uuid = data.length > 0 && data[0] instanceof String ? (String) data[0] : player.getUniqueId().toString();
                values.put(uuid, page);
                break;
        }

        player.openInventory(gui.createInventory(player, page, values, replacements));
        pageNumber.put(player.getUniqueId(), page);
    }




    /*
    public static void openGUI(Player player, int page) {
        NotBounties nb = NotBounties.getInstance();
        Inventory bountyInventory = Bukkit.createInventory(player, guiSize, speakings.get(35) + " " + (page + 1));
        ItemStack[] contents = bountyInventory.getContents();
        List<Bounty> sortedList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.sortBounties();
        for (String[] itemInfo : layout) {
            ItemStack itemStack;
            if (itemInfo[0].equalsIgnoreCase("exit")) {
                itemStack = Item.get("exit");
            } else if (itemInfo[0].equalsIgnoreCase("next")) {
                if (sortedList.size() > (page * bountySlots.size()) + bountySlots.size()) {
                    itemStack = Item.get("next");
                } else {
                    itemStack = null;
                }
            } else if (itemInfo[0].equalsIgnoreCase("back")) {
                if (page > 0) {
                    itemStack = Item.get("back");
                } else {
                    itemStack = null;
                }
            } else {
                try {
                    itemStack = customItems.get(Integer.parseInt(itemInfo[0]));
                    itemStack = nb.formatItem(itemStack, player);
                } catch (NumberFormatException ignored) {
                    itemStack = new ItemStack(Material.STONE);
                }
            }
            if (itemStack != null)
                if (itemInfo[1] != null)
                    if (itemInfo[1].contains("-")) {
                        int num1;
                        try {
                            num1 = Integer.parseInt(itemInfo[1].substring(0, itemInfo[1].indexOf("-")));
                        } catch (NumberFormatException ignored) {
                            continue;
                        }
                        int num2;
                        try {
                            num2 = Integer.parseInt(itemInfo[1].substring(itemInfo[1].indexOf("-") + 1));
                        } catch (NumberFormatException ignored) {
                            continue;
                        }
                        for (int i = Math.min(num1, num2); i < Math.max(num1, num2) + 1; i++) {
                            contents[i] = itemStack;
                        }
                    } else {
                        try {
                            contents[Integer.parseInt(itemInfo[1])] = itemStack;
                        } catch (NumberFormatException ignored) {

                        }
                    }
        }

        for (int i = page * bountySlots.size(); i < (page * bountySlots.size()) + bountySlots.size(); i++) {
            if (sortedList.size() > i) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                assert meta != null;
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(sortedList.get(i).getUUID())));
                if (papiEnabled) {
                    meta.setDisplayName(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(sortedList.get(i).getUUID())), parse(speakings.get(13), sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null)));
                } else {
                    meta.setDisplayName(parse(speakings.get(13), sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null));
                }
                ArrayList<String> lore = new ArrayList<>();
                for (String str : headLore) {
                    if (papiEnabled) {
                        lore.add(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(sortedList.get(i).getUUID())), parse(str, sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null)));
                    } else {
                        lore.add(parse(str, sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null));
                    }
                }


                if (player.hasPermission("notbounties.admin")) {
                    lore.add(ChatColor.RED + "Left Click" + ChatColor.GRAY + " to Remove");
                    lore.add(ChatColor.YELLOW + "Right Click" + ChatColor.GRAY + " to Edit");
                    lore.add("");
                    //lore.add(ChatColor.BLACK + "" + i);
                } else {
                    if (buyBack) {
                        if (sortedList.get(i).getUUID().equals(player.getUniqueId().toString())) {
                            lore.add(parse(buyBackLore, (int) (sortedList.get(i).getTotalBounty() * buyBackInterest), player));
                            lore.add("");
                        }
                    }
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
                contents[bountySlots.get(i - page * bountySlots.size())] = skull;
            } else {
                break;
            }
        }

        bountyInventory.setContents(contents);
        player.openInventory(bountyInventory);
    }

    public static void openTop(Leaderboard leaderboard, Player player){
        // add customizable later
        LinkedHashMap<String, Integer> list = leaderboard.getTop(0,10);
        String name = speakings.get(50).replaceAll("\\{type}", leaderboard.name());
        Inventory inv = Bukkit.createInventory(player, 36, name);
        ItemStack[] contents = inv.getContents();
        ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = fill.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.BLACK + "");
        fill.setItemMeta(meta);
        Arrays.fill(contents, meta);
        contents[31] = Item.get("exit");
        int i = 11;
        for (Map.Entry<String, Integer> entry : list.entrySet()){
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            assert skullMeta != null;
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
            skullMeta.setOwningPlayer(offlinePlayer);

            if (papiEnabled) {
                skullMeta.setDisplayName(PlaceholderAPI.setPlaceholders(offlinePlayer, parse(speakings.get(13), offlinePlayer.getName(), entry.getValue(), null)));
            } else {
                skullMeta.setDisplayName(parse(speakings.get(13), offlinePlayer.getName(), entry.getValue(), null));
            }
            ArrayList<String> lore = new ArrayList<>();
            lore.add(leaderboard.displayStats(offlinePlayer, false));

            skullMeta.setLore(lore);
            skull.setItemMeta(skullMeta);
            contents[i] = skull;
            if (i < 15)
                i+= 2;
            if (i == 15)
                i = 19;
            if (i >= 19)
                i++;
        }
        inv.setContents(contents);
        player.openInventory(inv);
    }*/

    // is this called when server forces the inventory to be closed?
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event){
        pageNumber.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Get a gui from the title
     * @param title Title of the GUI
     * @return the GUIOptions if the title matches a NotBounties GUI, or null if the title does not match any GUI
     */
    public static @Nullable GUIOptions getGUIByTitle(String title){
        GUIOptions gui = null;
        for (Map.Entry<String, GUIOptions> entry : customGuis.entrySet()){
            if (title.startsWith(entry.getValue().getName())){
                gui = entry.getValue();
                break;
            }
        }
        return gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!pageNumber.containsKey(event.getWhoClicked().getUniqueId())) // check if they are in a NotBounties GUI
            return;
        // find the gui - yeah, a linear search
        GUIOptions gui = getGUIByTitle(event.getView().getTitle());

        if (gui == null) // JIC a player has a page number, but they aren't in a gui
            return;
        String guiType = gui.getType();
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) // make sure it is in the top inventory
            return;
        if (event.getCurrentItem() == null)
            return;
        // check if it is a player slot
        if (gui.getPlayerSlots().contains(event.getSlot())){
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            assert meta != null;
            assert meta.getOwningPlayer() != null;
            OfflinePlayer player = meta.getOwningPlayer();
            if (guiType.equals("bounty-gui")){
                // remove, edit, and buy back
                Bounty bounty = NotBounties.getInstance().getBounty(meta.getOwningPlayer());
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
                        }
                    } else if (event.isLeftClick()) {
                        if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                            // remove
                            Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                            ItemStack[] contents = confirmation.getContents();
                            Arrays.fill(contents, Item.get("fill"));
                            contents[29] = Item.get("yes");
                            contents[33] = Item.get("no");
                            contents[40] = Item.get("exit");
                            contents[13] = event.getCurrentItem();
                            confirmation.setContents(contents);
                            event.getWhoClicked().openInventory(confirmation);

                        } else {
                            if (bounty.getUUID().equals(event.getWhoClicked().getUniqueId().toString())) {
                                if (buyBack) {
                                    if (usingPapi) {
                                        // check if papi is enabled - parse to check
                                        if (papiEnabled) {
                                            double balance;
                                            try {
                                                balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) event.getWhoClicked(), currency));
                                            } catch (NumberFormatException ignored) {
                                                Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                return;
                                            }
                                            if (balance >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                                Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                                                ItemStack[] contents = confirmation.getContents();
                                                Arrays.fill(contents, Item.get("fill"));
                                                contents[29] = Item.get("yes");
                                                contents[33] = Item.get("no");
                                                contents[40] = Item.get("exit");
                                                contents[13] = event.getCurrentItem();
                                                confirmation.setContents(contents);
                                                event.getWhoClicked().openInventory(confirmation);
                                            } else {
                                                event.getWhoClicked().sendMessage(parse(speakings.get(0) + speakings.get(6), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) event.getWhoClicked()));
                                            }
                                        } else {
                                            Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                        }
                                    } else {
                                        if (NotBounties.getInstance().checkAmount((Player) event.getWhoClicked(), Material.valueOf(currency)) >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                            Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                                            ItemStack[] contents = confirmation.getContents();
                                            Arrays.fill(contents, Item.get("fill"));
                                            contents[29] = Item.get("yes");
                                            contents[33] = Item.get("no");
                                            contents[40] = Item.get("exit");
                                            contents[13] = event.getCurrentItem();
                                            confirmation.setContents(contents);
                                            event.getWhoClicked().openInventory(confirmation);
                                        } else {
                                            event.getWhoClicked().sendMessage(parse(speakings.get(0) + speakings.get(6), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) event.getWhoClicked()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                        openGUI((Player) event.getWhoClicked(), "bounty-gui", pageNumber.get(event.getWhoClicked().getUniqueId()));
                        event.getWhoClicked().sendMessage(PlaceholderAPI.setPlaceholders(meta.getOwningPlayer(), parse(speakings.get(0) + speakings.get(8), meta.getOwningPlayer().getName(), null)));
                    }
                }
            } else if (guiType.equals("set-bounty")){
                openGUI((Player) event.getWhoClicked(), "select-price", minBounty, player.getUniqueId().toString());
            }
        } else {
            CustomItem customItem = gui.getCustomItems()[event.getSlot()];
            if (customItem == null)
                return;
            for (String command : customItem.getCommands()){
                command = command.replaceAll("\\{player}", event.getWhoClicked().getName());
                if (command.startsWith("[close]"))
                    event.getView().close();
                if (command.startsWith("[p]"))
                    Bukkit.dispatchCommand(event.getWhoClicked(), command.substring(4));
                if (command.startsWith("[next]")) {
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(command.substring(7));
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                    openGUI((Player) event.getWhoClicked(), gui.getName(), pageNumber.get(event.getWhoClicked().getUniqueId()) + amount);
                }
                if (command.startsWith("[back]")) {
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(command.substring(7));
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                    openGUI((Player) event.getWhoClicked(), gui.getName(), pageNumber.get(event.getWhoClicked().getUniqueId()) - amount);
                }
                if (command.startsWith("[gui]")){
                    int amount = 1;
                    String guiName = command.substring(6);
                    if (command.substring(6).contains(" ")) {
                        try {
                            amount = Integer.parseInt(command.substring(6 + command.substring(6).indexOf(" ")));
                            guiName = guiName.substring(0, guiName.indexOf(" "));
                        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                        }
                    }
                    if (guiName.equalsIgnoreCase("leaderboard")) {
                        Leaderboard l = Leaderboard.ALL;
                        try {
                            l = Leaderboard.valueOf(command.substring(command.lastIndexOf(" ") + 1).toUpperCase());
                        } catch (IllegalArgumentException ignored){}
                        openGUI((Player) event.getWhoClicked(), guiName, amount, l);
                    } else {
                        openGUI((Player) event.getWhoClicked(), guiName, amount);
                    }

                }
            }
        }

        /*
        // old gui
        if (event.getView().getTitle().contains(speakings.get(35))) {
            event.setCancelled(true);
            NotBounties nb = NotBounties.getInstance();

            int page = Integer.parseInt(event.getView().getTitle().substring(event.getView().getTitle().lastIndexOf(" ") + 1));
            page -= 1;
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null)
                return;
            if (clickedItem.getType().isAir())
                return;
            if (clickedItem.isSimilar(Item.get("exit"))) {
                event.getView().close();
            } else if (clickedItem.isSimilar(Item.get("back"))) {
                openGUI((Player) event.getWhoClicked(), page - 1);
            } else if (clickedItem.isSimilar(Item.get("next"))) {
                openGUI((Player) event.getWhoClicked(), page + 1);
            } else if (clickedItem.getType() == Material.PLAYER_HEAD) {

                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                assert meta != null;
                assert meta.getOwningPlayer() != null;
                Bounty bounty = nb.getBounty(meta.getOwningPlayer());
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
                        }
                    } else if (event.isLeftClick()) {
                        if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                            // remove
                            Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                            ItemStack[] contents = confirmation.getContents();
                            Arrays.fill(contents, Item.get("fill"));
                            contents[29] = Item.get("yes");
                            contents[33] = Item.get("no");
                            contents[40] = Item.get("exit");
                            contents[13] = clickedItem;
                            confirmation.setContents(contents);
                            event.getWhoClicked().openInventory(confirmation);

                        } else {
                            if (bounty.getUUID().equals(event.getWhoClicked().getUniqueId().toString())) {
                                if (buyBack) {
                                    if (usingPapi) {
                                        // check if papi is enabled - parse to check
                                        if (papiEnabled) {
                                            double balance;
                                            try {
                                                balance = Double.parseDouble(PlaceholderAPI.setPlaceholders((Player) event.getWhoClicked(), currency));
                                            } catch (NumberFormatException ignored) {
                                                Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                                                return;
                                            }
                                            if (balance >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                                Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                                                ItemStack[] contents = confirmation.getContents();
                                                Arrays.fill(contents, Item.get("fill"));
                                                contents[29] = Item.get("yes");
                                                contents[33] = Item.get("no");
                                                contents[40] = Item.get("exit");
                                                contents[13] = clickedItem;
                                                confirmation.setContents(contents);
                                                event.getWhoClicked().openInventory(confirmation);
                                            } else {
                                                event.getWhoClicked().sendMessage(parse(speakings.get(0) + speakings.get(6), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) event.getWhoClicked()));
                                            }
                                        } else {
                                            Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
                                        }
                                    } else {
                                        if (nb.checkAmount((Player) event.getWhoClicked(), Material.valueOf(currency)) >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                            Inventory confirmation = Bukkit.createInventory(event.getWhoClicked(), 54, ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?");
                                            ItemStack[] contents = confirmation.getContents();
                                            Arrays.fill(contents, Item.get("fill"));
                                            contents[29] = Item.get("yes");
                                            contents[33] = Item.get("no");
                                            contents[40] = Item.get("exit");
                                            contents[13] = clickedItem;
                                            confirmation.setContents(contents);
                                            event.getWhoClicked().openInventory(confirmation);
                                        } else {
                                            event.getWhoClicked().sendMessage(parse(speakings.get(0) + speakings.get(6), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) event.getWhoClicked()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                        openGUI((Player) event.getWhoClicked(), page);
                        event.getWhoClicked().sendMessage(PlaceholderAPI.setPlaceholders(meta.getOwningPlayer(), parse(speakings.get(0) + speakings.get(8), meta.getOwningPlayer().getName(), null)));
                    }
                }
            } else {
                for (String[] itemInfo : layout) {
                    int commandIndex;
                    try {
                        commandIndex = Integer.parseInt(itemInfo[0]);
                    } catch (NumberFormatException ignored) {
                        //Bukkit.getLogger().info(itemInfo[0]);
                        continue;
                    }
                    if (itemInfo[1] != null) {
                        if (itemInfo[1].contains("-")) {
                            int num1;
                            try {
                                num1 = Integer.parseInt(itemInfo[1].substring(0, itemInfo[1].indexOf("-")));
                            } catch (NumberFormatException ignored) {
                                continue;
                            }
                            int num2;
                            try {
                                num2 = Integer.parseInt(itemInfo[1].substring(itemInfo[1].indexOf("-") + 1));
                            } catch (NumberFormatException ignored) {
                                continue;
                            }
                            for (int i = Math.min(num1, num2); i < Math.max(num1, num2) + 1; i++) {
                                if (event.getRawSlot() == i) {
                                    for (String cmd : itemCommands.get(commandIndex)) {
                                        while (cmd.contains("{player}")) {
                                            cmd = cmd.replace("{player}", event.getWhoClicked().getName());
                                        }
                                        if (cmd.equalsIgnoreCase("[close]")) {
                                            event.getView().close();
                                        } else {
                                            if (cmd.contains("[p]")) {
                                                if (cmd.indexOf("[p]") == 0) {
                                                    cmd = cmd.substring(4);
                                                    Bukkit.dispatchCommand(event.getWhoClicked(), cmd);
                                                    continue;
                                                }
                                            }
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                                        }

                                    }
                                    return;
                                }
                            }
                        } else {
                            try {
                                //Bukkit.getLogger().info(event.getRawSlot() + ">" + itemInfo[1]);
                                if (event.getRawSlot() == Integer.parseInt(itemInfo[1])) {
                                    //Bukkit.getLogger().info("match");
                                    for (String cmd : itemCommands.get(commandIndex)) {
                                        //Bukkit.getLogger().info(cmd);
                                        while (cmd.contains("{player}")) {
                                            cmd = cmd.replace("{player}", event.getWhoClicked().getName());
                                        }
                                        if (cmd.equalsIgnoreCase("[close]")) {
                                            event.getView().close();
                                        } else {
                                            if (cmd.contains("[p]")) {
                                                if (cmd.indexOf("[p]") == 0) {
                                                    cmd = cmd.substring(4);
                                                    Bukkit.dispatchCommand(event.getWhoClicked(), cmd);
                                                    continue;
                                                }
                                            }
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                                        }

                                    }
                                    return;
                                }
                            } catch (NumberFormatException ignored) {

                            }
                        }
                    }
                }
            }
        } else*/ if (event.getView().getTitle().equals(ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?")) {
            NotBounties nb = NotBounties.getInstance();
            ItemStack skull = event.getInventory().getContents()[13];
            event.setCancelled(true);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            assert meta != null;
            assert meta.getOwningPlayer() != null;
            Bounty bounty = nb.getBounty(meta.getOwningPlayer());
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null)
                return;
            if (clickedItem.getType().isAir())
                return;
            if (bounty != null) {
                if (clickedItem.isSimilar(Item.get("exit"))) {
                    event.getView().close();
                } else if (clickedItem.isSimilar(Item.get("no"))) {
                    openGUI((Player) event.getWhoClicked(),"bounty-gui", 0);
                } else if (clickedItem.isSimilar(Item.get("yes"))) {
                    if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                        if (nb.SQL.isConnected()){
                            nb.data.removeBounty(bounty.getUUID());
                        } else {
                            nb.bountyList.remove(bounty);
                        }
                        openGUI((Player) event.getWhoClicked(),"bounty-gui", 1);
                        event.getWhoClicked().sendMessage(PlaceholderAPI.setPlaceholders(meta.getOwningPlayer(), parse(speakings.get(0) + speakings.get(14), meta.getOwningPlayer().getName(), null)));
                    } else {
                        if (!usingPapi) {
                            nb.removeItem((Player) event.getWhoClicked(), Material.valueOf(currency), (int) (bounty.getTotalBounty() * buyBackInterest));
                        }
                        nb.doRemoveCommands((Player) event.getWhoClicked(), (int) (bounty.getTotalBounty() * buyBackInterest));
                        if (nb.SQL.isConnected()){
                            nb.data.removeBounty(bounty.getUUID());
                        } else {
                            nb.bountyList.remove(bounty);
                        }
                        event.getWhoClicked().sendMessage(parse(speakings.get(0) + speakings.get(14), event.getWhoClicked().getName(), (Player) event.getWhoClicked()));
                        openGUI((Player) event.getWhoClicked(),"bounty-gui", 1);
                    }
                }
            } else {
                openGUI((Player) event.getWhoClicked(),"bounty-gui", 1);
            }
        }
    }

}

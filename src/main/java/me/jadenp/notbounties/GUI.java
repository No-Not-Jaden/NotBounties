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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.jadenp.notbounties.ConfigOptions.*;

public class GUI implements Listener {

    public static final Map<UUID, PlayerGUInfo> playerInfo = new HashMap<>();
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
        if (page < 1)
            page = 1;


        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        NotBounties nb = NotBounties.getInstance();
        String[] replacements = new String[0];
        switch (name){
            case "bounty-gui":
                List<Bounty> sortedList = nb.SQL.isConnected() ? nb.data.getTopBounties() : nb.sortBounties(gui.getSortType());
                for (Bounty bounty : sortedList)
                    values.put(bounty.getUUID(), currencyPrefix + bounty.getTotalBounty() + currencySuffix);
                break;
            case "leaderboard":
                Leaderboard leaderboard = data.length > 0 && data[0] instanceof Leaderboard ? (Leaderboard) data[0] : Leaderboard.ALL;
                values = leaderboard.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                replacements = new String[]{leaderboard.toString()};
                break;
            case "set-bounty":
                values = Leaderboard.IMMUNITY.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                break;
            case "select-price":
                String uuid = data.length > 0 && data[0] instanceof String ? (String) data[0] : player.getUniqueId().toString();
                values.put(uuid, page + "");
                break;
            case "confirm":
                uuid = data.length > 0 && data[0] instanceof UUID ? data[0].toString() : player.getUniqueId().toString();
                int price = data.length > 1 && data[1] instanceof Integer ? (int) data[1] : 0;
                values.put(uuid, price + "");
                break;
        }

        player.openInventory(gui.createInventory(player, page, values, replacements));
        playerInfo.put(player.getUniqueId(), new PlayerGUInfo(page, data));
    }


    // is this called when server forces the inventory to be closed?
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event){
        playerInfo.remove(event.getPlayer().getUniqueId());
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
        if (!playerInfo.containsKey(event.getWhoClicked().getUniqueId())) // check if they are in a NotBounties GUI
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
            switch (guiType) {
                case "bounty-gui":
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
                                openGUI((Player) event.getWhoClicked(), "confirm", 1, player.getUniqueId(), 0);

                            } else {
                                if (bounty.getUUID().equals(event.getWhoClicked().getUniqueId().toString())) {
                                    if (buyBack) {
                                        double balance = getBalance((Player) event.getWhoClicked());
                                        if (balance >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                            openGUI((Player) event.getWhoClicked(), "confirm", 1, player.getUniqueId(), (int) (buyBackInterest * bounty.getTotalBounty()));
                                        } else {
                                            event.getWhoClicked().sendMessage(parse(speakings.get(0) + speakings.get(6), (int) (bounty.getTotalBounty() * buyBackInterest), (Player) event.getWhoClicked()));
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                            openGUI((Player) event.getWhoClicked(), "bounty-gui", playerInfo.get(event.getWhoClicked().getUniqueId()).getPage());
                            event.getWhoClicked().sendMessage(PlaceholderAPI.setPlaceholders(meta.getOwningPlayer(), parse(speakings.get(0) + speakings.get(8), meta.getOwningPlayer().getName(), null)));
                        }
                    }
                    break;
                case "set-bounty":
                    openGUI((Player) event.getWhoClicked(), "select-price", minBounty, player.getUniqueId().toString());
                    break;
                case "select-price":
                    Bukkit.dispatchCommand(event.getWhoClicked(), "bounty " + player.getName() + " " + playerInfo.get(event.getWhoClicked().getUniqueId()).getPage());
                    event.getView().close();
                    break;
            }
        } else {
            CustomItem customItem = gui.getCustomItems()[event.getSlot()];
            if (customItem == null)
                return;
            for (String command : customItem.getCommands()){
                command = command.replaceAll("\\{player}", event.getWhoClicked().getName());
                while (command.contains("{slot") && command.substring(command.indexOf("{slot")).contains("}")){
                    String replacement = "";
                    try {
                        int slot = Integer.parseInt(command.substring(command.indexOf("{slot") + 5, command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length()));
                        ItemStack item = event.getInventory().getContents()[slot];
                        if (item != null) {
                            if (item.getType() == Material.PLAYER_HEAD) {
                                SkullMeta meta = (SkullMeta) item.getItemMeta();
                                assert meta != null;
                                OfflinePlayer player = meta.getOwningPlayer();
                                if (player != null && player.getName() != null) {
                                    replacement = meta.getOwningPlayer().getName();
                                } else {
                                    Bukkit.getLogger().warning("Invalid player for slot " + slot);
                                }
                            }
                            if (replacement == null)
                                replacement = "";
                            if (replacement.equals("")) {
                                ItemMeta meta = item.getItemMeta();
                                assert meta != null;
                                replacement = meta.getDisplayName();
                            }
                        }
                    } catch (NumberFormatException e){
                        Bukkit.getLogger().warning("Error getting slot in command: \n" + command);
                    }
                    command = command.substring(0, command.indexOf("{slot")) + replacement + command.substring(command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length()+ 1);
                }
                if (command.startsWith("@")){
                    String permission = command.substring(1, command.indexOf(" "));
                    if (!event.getWhoClicked().hasPermission(permission))
                        continue;
                    command = command.substring(command.indexOf(" ") + 1);
                } else if (command.startsWith("!@")){
                    String permission = command.substring(2, command.indexOf(" "));
                    if (event.getWhoClicked().hasPermission(permission))
                        continue;
                    command = command.substring(command.indexOf(" ") + 1);
                }
                if (command.startsWith("[close]")) {
                    event.getView().close();
                } else if (command.startsWith("[p]")) {
                    Bukkit.dispatchCommand(event.getWhoClicked(), command.substring(4));
                } else if (command.startsWith("[next]")) {
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(command.substring(7));
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                    PlayerGUInfo info = playerInfo.get(event.getWhoClicked().getUniqueId());
                    if (guiType.equals("select-price")){
                        String uuid = (String) info.getData()[0]; //((SkullMeta) event.getInventory().getContents()[gui.getPlayerSlots().get(0)].getItemMeta()).getOwningPlayer().getUniqueId().toString();
                        openGUI((Player) event.getWhoClicked(), gui.getType(), info.getPage() + amount, uuid);
                    } else if (guiType.equals("leaderboard")){
                        Leaderboard leaderboard = (Leaderboard) info.getData()[0];
                        openGUI((Player) event.getWhoClicked(), gui.getType(), info.getPage() + amount, leaderboard);
                    } else {
                        openGUI((Player) event.getWhoClicked(), gui.getType(), info.getPage() + amount);
                    }

                } else if (command.startsWith("[back]")) {
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(command.substring(7));
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                    PlayerGUInfo info = playerInfo.get(event.getWhoClicked().getUniqueId());
                    if (guiType.equals("select-price")){
                        String uuid = (String) info.getData()[0]; //((SkullMeta) event.getInventory().getContents()[gui.getPlayerSlots().get(0)].getItemMeta()).getOwningPlayer().getUniqueId().toString();
                        openGUI((Player) event.getWhoClicked(), gui.getType(), info.getPage() - amount, uuid);
                    } else if (guiType.equals("leaderboard")){
                        Leaderboard leaderboard = (Leaderboard) info.getData()[0];
                        openGUI((Player) event.getWhoClicked(), gui.getType(), info.getPage() - amount, leaderboard);
                    }  else {
                        openGUI((Player) event.getWhoClicked(), gui.getType(), info.getPage() - amount);
                    }
                } else if (command.startsWith("[gui]")){
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

                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }

    }

}

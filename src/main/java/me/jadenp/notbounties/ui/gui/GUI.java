package me.jadenp.notbounties.ui.gui;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.gui.bedrock.BedrockGUI;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.ActionCommands;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;

public class GUI implements Listener {

    public static final Map<UUID, PlayerGUInfo> playerInfo = new HashMap<>();
    private static final Map<String, GUIOptions> customGuis = new HashMap<>();
    private static final Map<UUID, CommandPrompt> commandPrompts = new HashMap<>();
    public static void addGUI(GUIOptions gui, String name){
        customGuis.put(name, gui);
    }
    public static GUIOptions getGUI(String guiName){
        if (customGuis.containsKey(guiName))
            return customGuis.get(guiName);
        return null;
    }

    public static void addCommandPrompt(UUID uuid, CommandPrompt commandPrompt) {
        commandPrompts.put(uuid, commandPrompt);
    }

    public GUI(){}

    public static LinkedHashMap<UUID, String> getGUIValues(Player player, String name, long page, Object[] data) {
        if (!customGuis.containsKey(name))
            return new LinkedHashMap<>();
        GUIOptions gui = customGuis.get(name);
        if (page < 1)
            page = 1;


        LinkedHashMap<UUID, String> values = new LinkedHashMap<>();
        switch (name){
            case "bounty-gui":
                List<Bounty> sortedList = SQL.isConnected() ? BountyManager.data.getTopBounties(gui.getSortType()) : sortBounties(gui.getSortType());
                for (Bounty bounty : sortedList) {
                    double bountyAmount = showWhitelistedBounties || player.hasPermission("notbounties.admin") ? bounty.getTotalBounty() : bounty.getTotalBounty(player);
                    if (bountyAmount > 0)
                        values.put(bounty.getUUID(), String.format("%f", bountyAmount));
                }
                break;
            case "leaderboard":
                Leaderboard leaderboard = data.length > 0 && data[0] instanceof Leaderboard ? (Leaderboard) data[0] : Leaderboard.ALL;
                values = leaderboard.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                break;
            case "set-bounty":
                values = Leaderboard.IMMUNITY.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType());
                for (Map.Entry<String, UUID> entry : NotBounties.loggedPlayers.entrySet())
                    if (!values.containsKey(entry.getValue()))
                        values.put(entry.getValue(), NumberFormatting.currencyPrefix + "0" + NumberFormatting.currencySuffix);
                if (data.length == 0 || !(data[0] instanceof String) || !((String) data[0]).equalsIgnoreCase("offline")) {
                    // remove offline players
                    values.entrySet().removeIf(e -> !Bukkit.getOfflinePlayer(e.getKey()).isOnline());
                }
                break;
            case "set-whitelist":
                List<UUID> whitelist = NotBounties.getPlayerWhitelist(player.getUniqueId()).getList();
                for (UUID uuid : whitelist)
                    values.put(uuid, NumberFormatting.currencyPrefix + Leaderboard.IMMUNITY.getStat(uuid) + NumberFormatting.currencySuffix);
                for (Map.Entry<UUID, String> entry : Leaderboard.IMMUNITY.getFormattedList(0, gui.getPlayerSlots().size(), gui.getSortType()).entrySet())
                    if (!values.containsKey(entry.getKey()))
                        values.put(entry.getKey(), entry.getValue());
                for (Map.Entry<String, UUID> entry : NotBounties.loggedPlayers.entrySet())
                    if (!values.containsKey(entry.getValue()))
                        values.put(entry.getValue(), NumberFormatting.currencyPrefix + "0" + NumberFormatting.currencySuffix);
                if (data.length == 0 || !(data[0] instanceof String) || !((String) data[0]).equalsIgnoreCase("offline")) {
                    // remove offline players
                    values.entrySet().removeIf(e -> !Bukkit.getOfflinePlayer(e.getKey()).isOnline() && !whitelist.contains(e.getKey()));
                }
                break;
            case "select-price":
                String uuid = data.length > 0 && data[0] instanceof String ? (String) data[0] : player.getUniqueId().toString();
                values.put(UUID.fromString(uuid), page + "");
                break;
            case "confirm":
                String uuid1 = data.length > 0 && data[0] instanceof UUID ? data[0].toString() : player.getUniqueId().toString();
                double price = data.length > 1 && data[1] instanceof Double ? (double) data[1] : 0;
                values.put(UUID.fromString(uuid1), NumberFormatting.getValue(price));
                break;
            case "confirm-bounty":
                String uuid2 = data.length > 0 && data[0] instanceof UUID ? data[0].toString() : player.getUniqueId().toString();
                long price2 = data.length > 1 && data[1] instanceof Long ? (long) data[1] : 0;
                values.put(UUID.fromString(uuid2), price2 + "");
                break;
        }
        return values;

    }

    public static void openGUI(Player player, String name, long page, Object... data) {
        if (page < 1)
            page = 1;

        LinkedHashMap<UUID, String> values = getGUIValues(player, name, page, data);
        String[] replacements = new String[0];
        if ("leaderboard".equals(name)) {
            Leaderboard leaderboard = data.length > 0 && data[0] instanceof Leaderboard ? (Leaderboard) data[0] : Leaderboard.ALL;
            replacements = new String[]{leaderboard.toString()};
        }
        if (geyserEnabled && floodgateEnabled && BedrockGUI.enabled && new FloodGateClass().isBedrockPlayer(player.getUniqueId()) && BedrockGUI.isGUIEnabled(name)) {
            // open bedrock gui
            BedrockGUI.openGUI(player, name, page, values, replacements);
        } else {
            // open java gui
            if (!customGuis.containsKey(name))
                return;
            GUIOptions gui = customGuis.get(name);
            Inventory inventory = gui.createInventory(player, page, values, replacements);
            player.openInventory(inventory);
        }
        playerInfo.put(player.getUniqueId(), new PlayerGUInfo(page, name, data, values.keySet().toArray(new UUID[0]), player.getOpenInventory().getTitle()));
    }


    // is this called when server forces the inventory to be closed?
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event){
        if (playerInfo.containsKey(event.getPlayer().getUniqueId()) && playerInfo.get(event.getPlayer().getUniqueId()).getTitle().equals(event.getView().getTitle()))
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

        PlayerGUInfo info = playerInfo.get(event.getWhoClicked().getUniqueId());
        GUIOptions gui = getGUI(info.getGuiType());

        if (gui == null) // JIC a player has a page number, but they aren't in a gui
            return;
        String guiType = gui.getType();
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) // make sure it is in the top inventory
            return;
        if (event.getCurrentItem() == null)
            return;
        // check if it is a player slot
        if (gui.getPlayerSlots().contains(event.getSlot()) && gui.getPlayerSlots().indexOf(event.getSlot()) < info.getPlayers().length && event.getCurrentItem().getType() == Material.PLAYER_HEAD){
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            assert meta != null;
            //OfflinePlayer player = meta.getOwningPlayer();
            OfflinePlayer player = Bukkit.getOfflinePlayer(info.getPlayers()[gui.getPlayerSlots().indexOf(event.getSlot())]);
            String playerName = NotBounties.getPlayerName(player.getUniqueId());
            switch (guiType) {
                case "bounty-gui":
                    // remove, edit, and buy back
                    Bounty bounty = getBounty(meta.getOwningPlayer());
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
                                if (bounty.getUUID().equals(event.getWhoClicked().getUniqueId())) {
                                    if (buyBack) {
                                        double balance = NumberFormatting.getBalance((Player) event.getWhoClicked());
                                        if (balance >= (int) (bounty.getTotalBounty() * buyBackInterest)) {
                                            openGUI((Player) event.getWhoClicked(), "confirm", 1, player.getUniqueId(), (buyBackInterest * bounty.getTotalBounty()));
                                        } else {
                                            event.getWhoClicked().sendMessage(parse(prefix + broke, (bounty.getTotalBounty() * buyBackInterest), (Player) event.getWhoClicked()));
                                        }
                                    }
                                }
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
                    openGUI((Player) event.getWhoClicked(), "select-price", ConfigOptions.minBounty, player.getUniqueId().toString());
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
                    Bukkit.dispatchCommand(event.getWhoClicked(), "bounty " + playerName + " " + playerInfo.get(event.getWhoClicked().getUniqueId()).getPage());
                    if (!confirmation)
                        event.getView().close();
                    break;
            }
        } else {
            // custom item
            CustomItem customItem = gui.getCustomItem(event.getSlot(), info.getPage(), getGUIValues((Player) event.getWhoClicked(), guiType, info.getPage(), info.getData()).size());
            if (customItem == null)
                return;
            ActionCommands.executeGUI((Player) event.getWhoClicked(), customItem.getCommands());
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void asyncChatEvent(AsyncPlayerChatEvent event) {
        if (commandPrompts.containsKey(event.getPlayer().getUniqueId())) {
            CommandPrompt commandPrompt = commandPrompts.get(event.getPlayer().getUniqueId());
            commandPrompts.remove(event.getPlayer().getUniqueId());
            String command = commandPrompt.getCommand().replace("<~placeholder~>", ChatColor.stripColor(event.getMessage()));
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (commandPrompt.isPlayerPrompt()) {
                        Bukkit.dispatchCommand(event.getPlayer(), command);
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }
            }.runTaskLater(NotBounties.getInstance(), 1);

        }
    }

}

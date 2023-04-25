package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.jadenp.notbounties.ConfigOptions.*;
import static me.jadenp.notbounties.ConfigOptions.bountySlots;

public class GUI implements Listener {
    public GUI(){}
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

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
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
        } else if (event.getView().getTitle().equals(ChatColor.BLUE + "" + ChatColor.BOLD + "Are you sure?")) {
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
                    openGUI((Player) event.getWhoClicked(), 0);
                } else if (clickedItem.isSimilar(Item.get("yes"))) {
                    if (event.getWhoClicked().hasPermission("notbounties.admin")) {
                        if (nb.SQL.isConnected()){
                            nb.data.removeBounty(bounty.getUUID());
                        } else {
                            nb.bountyList.remove(bounty);
                        }
                        openGUI((Player) event.getWhoClicked(), 0);
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
                        openGUI((Player) event.getWhoClicked(), 0);
                    }
                }
            } else {
                openGUI((Player) event.getWhoClicked(), 0);
            }
        }
    }

}

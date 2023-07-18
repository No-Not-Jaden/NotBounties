package me.jadenp.notbounties;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static me.jadenp.notbounties.ConfigOptions.*;

public class CurrencySetup implements Listener {
    public static UUID currencySetupPlayer = null;
    public static int currencySetupStage = 0;

    public static void onCommand(@NotNull CommandSender sender, @NotNull String[] args) {

            Player admin = (Player) sender;

            if (currencySetupPlayer != null && !currencySetupPlayer.equals(admin.getUniqueId())) {
                Player player = Bukkit.getPlayer(currencySetupPlayer);
                if (player != null)
                    player.sendMessage(parse(speakings.get(0) + ChatColor.RED + sender.getName() + " has started currency setup. This setup instance is now canceled.", player));
                currencySetupStage = 0;
            }

            if (args.length > 1){
                try {
                    currencySetupStage = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored){}
            }

            if (currencySetupStage == 7) {
                NotBounties nb = NotBounties.getInstance();
                nb.getConfig().set("currency.add-commands", Collections.singletonList("eco give {player} {amount}"));
                nb.getConfig().set("currency.remove-commands", Collections.singletonList("eco take {player} {amount}"));
                nb.saveConfig();
                admin.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Currency add and remove commands set to Vault.", admin));
                if (papiEnabled) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "papi ecloud download Vault");
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "papi reload");
                        }
                    }.runTaskLater(NotBounties.getInstance(), 20);
                }
                if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.RED + "Vault must be installed to use this!", admin));
                currencySetupStage = 4;
            }
            // 0 - currency type
            // 1 - Ask to edit add/remove commands
            // 2 - edit add command
            // 3 - edit remove command
            // 4 - Ask to edit prefix/suffix
            // 5 - edit prefix
            // 6 - edit suffix
            // 7 - set vault add/remove commands
             TextComponent space = new TextComponent("               ");
            switch (currencySetupStage) {
                case -1:
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Completed currency setup. Do" + ChatColor.WHITE + " /bounty reload" + ChatColor.YELLOW + " for your changes to take affect.", admin));
                    currencySetupStage = 0;
                    currencySetupPlayer = null;
                    break;
                case 0:
                    sender.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Type in chat the type of currency you want.", null));
                    sender.sendMessage(parse(speakings.get(0) + ChatColor.GRAY + "Type \"item\" to use the item in your hand.", null));
                    sender.sendMessage(parse(speakings.get(0) + ChatColor.GRAY + "Type a material name to use that item. Ex: \"diamond\".", null));
                    sender.sendMessage(parse(speakings.get(0) + ChatColor.GRAY + "Type a placeholder to use another plugin. Ex: \"%vault_eco_balance%\".", null));
                    sender.sendMessage(parse(speakings.get(0) + ChatColor.GRAY + "Type \"cancel\" to cancel currency setup.", null));
                    admin.sendMessage(" ");
                    currencySetupPlayer = ((Player) sender).getUniqueId();
                    break;
                case 1:
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Do you want to change the add and remove commands?" + ChatColor.GRAY + " (Click)", admin));
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.GRAY + "If an item is set as the currency, you do not need add or remove commands.", admin));
                    TextComponent no = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE + "No");
                    no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties currency 4"));
                    TextComponent yes = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE + "Yes");
                    yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties currency 2"));
                    BaseComponent[] message = new BaseComponent[]{space, no, space, yes};
                    admin.spigot().sendMessage(message);
                    admin.sendMessage(" ");
                    break;
                case 2:
                    TextComponent start = new TextComponent(parse(speakings.get(0) + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "add " + ChatColor.YELLOW + "command without the \"/\" or click", admin));
                    TextComponent here = new TextComponent(ChatColor.GOLD + "" + ChatColor.ITALIC + " here ");
                    here.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties currency 7"));
                    here.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "eco give/take {player} {amount}")));
                    TextComponent end = new TextComponent(ChatColor.YELLOW + "if you are using Vault.");
                    BaseComponent[] addCommand = new BaseComponent[]{start, here, end};
                    admin.spigot().sendMessage(addCommand);
                    admin.sendMessage(speakings.get(0) + ChatColor.GRAY + "Substitute the player for {player} and the amount for {amount}. Type \"skip\" to not have an add command.");
                    admin.sendMessage(" ");
                    break;
                case 3:
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "remove " + ChatColor.YELLOW + "command without the \"/\".", admin));
                    admin.sendMessage(speakings.get(0) + ChatColor.GRAY + "Substitute the player for {player} and the amount for {amount}. Type \"skip\" to not have a remove command.");
                    admin.sendMessage(" ");
                    break;
                case 4:
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Do you want to change the prefix and suffix?" + ChatColor.GRAY + " (Click)", admin));
                    TextComponent noPS = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE + "No");
                    noPS.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties currency -1"));
                    TextComponent yesPS = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE + "Yes");
                    yesPS.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties currency 5"));
                    BaseComponent[] messagePS = new BaseComponent[]{space, noPS, space, yesPS};
                    admin.spigot().sendMessage(messagePS);
                    admin.sendMessage(" ");
                    break;
                case 5:
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "prefix " + ChatColor.YELLOW + " for the currency.", admin));
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.GRAY + "Type \"skip\" to leave the prefix blank.", admin));
                    admin.sendMessage(" ");
                    break;
                case 6:
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "suffix " + ChatColor.YELLOW + " for the currency.", admin));
                    admin.sendMessage(parse(speakings.get(0) + ChatColor.GRAY + "Type \"skip\" to leave the suffix blank.", admin));
                    admin.sendMessage(" ");
                    break;
            }



    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event){
        if (currencySetupPlayer == null || currencySetupPlayer != event.getPlayer().getUniqueId())
            return;
        if (event.getMessage().equalsIgnoreCase("cancel")) {
            currencySetupPlayer = null;
            currencySetupStage = 0;
            event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Exited currency setup.", event.getPlayer()));
            event.setCancelled(true);
            return;
        }
        switch (currencySetupStage) {
            case 0:
                String currency;
                if (event.getMessage().equalsIgnoreCase("item")) {
                    Material hand = event.getPlayer().getInventory().getItemInMainHand().getType();
                    if (hand.isAir()) {
                        event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "You cannot set air as a currency! Try again.", event.getPlayer()));
                        event.setCancelled(true);
                        return;
                    }
                    currency = hand.toString();
                    event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Currency is now set to the item: " + ChatColor.WHITE + currency, event.getPlayer()));
                } else if (event.getMessage().contains("%")) {
                    currency = event.getMessage();
                    event.getPlayer().sendMessage(speakings.get(0) + ChatColor.YELLOW + "Currency is now set to the placeholder: " + ChatColor.WHITE + currency);
                    if (!papiEnabled)
                        event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.RED + "PlaceholderAPI must be installed to use this!", event.getPlayer()));
                    currencySetupStage++;
                } else {
                    Material material;
                    try {
                        material = Material.valueOf(event.getMessage().toUpperCase());
                    } catch (IllegalArgumentException | NullPointerException e) {
                        event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Could not find that material! Try again.", event.getPlayer()));
                        event.setCancelled(true);
                        return;
                    }
                    currency = material.toString();
                    event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Currency is now set to the item: " + ChatColor.WHITE + currency, event.getPlayer()));
                }
                NotBounties.getInstance().reloadConfig();
                NotBounties.getInstance().getConfig().set("currency.object", currency);
                NotBounties.getInstance().saveConfig();
                break;
            case 2:
                List<String> aList = event.getMessage().equalsIgnoreCase("skip") ? new ArrayList<>() : Collections.singletonList(event.getMessage());
                NotBounties.getInstance().reloadConfig();
                NotBounties.getInstance().getConfig().set("currency.add-commands", aList);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(speakings.get(0) + ChatColor.YELLOW + "Add command is now set to: " + ChatColor.WHITE + aList);
                break;
            case 3:
                List<String> rList = event.getMessage().equalsIgnoreCase("skip") ? new ArrayList<>() : Collections.singletonList(event.getMessage());
                NotBounties.getInstance().reloadConfig();
                NotBounties.getInstance().getConfig().set("currency.remove-commands", rList);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(speakings.get(0) + ChatColor.YELLOW + "Remove command is now set to: " + ChatColor.WHITE + rList);
                break;
            case 5:
                String prefix = event.getMessage();
                if (prefix.equalsIgnoreCase("skip"))
                    prefix = "";
                NotBounties.getInstance().reloadConfig();
                NotBounties.getInstance().getConfig().set("currency.prefix", prefix);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Prefix is now set to: '" + ChatColor.WHITE + prefix + ChatColor.YELLOW + "'", event.getPlayer()));
                break;
            case 6:
                String suffix = event.getMessage();
                if (suffix.equalsIgnoreCase("skip"))
                    suffix = "";
                NotBounties.getInstance().reloadConfig();
                NotBounties.getInstance().getConfig().set("currency.suffix", suffix);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.YELLOW + "Suffix is now set to: '" + ChatColor.WHITE + suffix + ChatColor.YELLOW + "'", event.getPlayer()));
                currencySetupStage = -2;
                break;
            default:
                return;
        }
        currencySetupStage++;
        event.setCancelled(true);
        onCommand(event.getPlayer(), new String[]{"currency"});

    }
}

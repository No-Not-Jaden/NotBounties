package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


import static me.jadenp.notbounties.features.LanguageOptions.*;

public class CurrencySetup implements Listener {
    public static UUID currencySetupPlayer = null;
    public static int currencySetupStage = 0;

    public static void onCommand(@NotNull CommandSender sender, @NotNull String[] args) {

        Player admin = (Player) sender;

        if (currencySetupPlayer != null && !currencySetupPlayer.equals(admin.getUniqueId())) {
            Player player = Bukkit.getPlayer(currencySetupPlayer);
            if (player != null)
                player.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.RED + sender.getName() + " has started currency setup. This setup instance is now canceled.", player));
            currencySetupStage = 0;
        }
        if (currencySetupPlayer == null)
            currencySetupPlayer = admin.getUniqueId();

        if (args.length > 1) {
            try {
                currencySetupStage = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        } else {
            if (NumberFormatting.isVaultEnabled()) {
                currencySetupStage = 9;
            } else {
                currencySetupStage = 0;
            }
        }
        YamlConfiguration config;
        try {
            config = ConfigOptions.getMoney().getConfig();
        } catch (IOException e) {
            admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.RED + "Cannot edit the settings/money.yml file! There are YAML formatting errors you must fix before continuing.", admin));
            return;
        }
        if (currencySetupStage == 7) {

            config.set("currency.add-commands", Collections.singletonList("eco give {player} {amount}"));
            config.set("currency.remove-commands", Collections.singletonList("eco take {player} {amount}"));
            ConfigOptions.getMoney().saveConfig(config);
            admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Currency add and remove commands set to Essentials.", admin));
            currencySetupStage = 4;
        }
        if (currencySetupStage == 8) {
            config.set("currency.add-commands", new ArrayList<>());
            config.set("currency.remove-commands", new ArrayList<>());
            ConfigOptions.getMoney().saveConfig(config);
            admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Cleared add and remove commands.", admin));
            currencySetupStage = 4;
        }
        if (currencySetupStage == 10) {
            if (config.getBoolean("currency.override-vault")) {
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Vault is staying overridden.", admin));
            } else {
                config.set("currency.override-vault", true);
                ConfigOptions.getMoney().saveConfig(config);
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Now overriding Vault.", admin));
            }
            currencySetupStage = 0;
        }
        if (currencySetupStage == 11) {
            if (!config.getBoolean("currency.override-vault")) {
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Vault is staying used.", admin));
            } else {
                config.set("currency.override-vault", false);
                ConfigOptions.getMoney().saveConfig(config);
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Now using vault.", admin));
            }
            currencySetupStage = 1;
        }
        // 0 - currency type
        // 1 - Ask to edit add/remove commands
        // 2 - edit add command
        // 3 - edit remove command
        // 4 - Ask to edit prefix/suffix
        // 5 - edit prefix
        // 6 - edit suffix
        // 7 - set vault add/remove commands
        // 8 - clear add/remove commands
        // 9 - vault detected
        // 10 - override vault
        // 11 - use vault
        TextComponent space = new TextComponent("               ");
        switch (currencySetupStage) {
            case -1:
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Completed currency setup. Do" + ChatColor.WHITE + " /bounty reload" + ChatColor.YELLOW + " for your changes to take affect.", admin));
                currencySetupStage = 0;
                currencySetupPlayer = null;
                break;
            case 0:
                sender.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Type in chat the type of currency you want.", null));
                sender.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.GRAY + "Type \"item\" to use the item in your hand.", null));
                sender.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.GRAY + "Type a material name to use that item. Ex: \"diamond\".", null));
                sender.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.GRAY + "Type a placeholder to use another plugin. Ex: \"%vault_eco_balance%\".", null));
                sender.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.GRAY + "Type \"cancel\" to cancel currency setup.", null));
                admin.sendMessage(" ");
                currencySetupPlayer = ((Player) sender).getUniqueId();
                break;
            case 1:
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Do you want to change the add and remove commands?" + ChatColor.GRAY + " (Click)", admin));
                TextComponent start2 = (TextComponent) new TextComponent(LanguageOptions.parse(getPrefix() + ChatColor.GRAY + "If an item is set as the currency or you are hooked into vault, you do not need add or remove commands. Click", admin));
                TextComponent here2 = new TextComponent(ChatColor.WHITE + "" + ChatColor.ITALIC + " here ");
                here2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency 8"));
                here2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Clear add and remove commands")));
                TextComponent end2 = new TextComponent(ChatColor.GRAY + "to clear add and remove commands.");
                BaseComponent[] addCommand = new BaseComponent[]{start2, here2, end2};
                admin.spigot().sendMessage(addCommand);
                TextComponent no = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + ChatColor.UNDERLINE + "No");
                no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency 4"));
                TextComponent yes = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + ChatColor.UNDERLINE + "Yes");
                yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency 2"));
                BaseComponent[] message = new BaseComponent[]{space, no, space, yes};
                admin.spigot().sendMessage(message);
                admin.sendMessage(" ");
                break;
            case 2:
                TextComponent start = (TextComponent) new TextComponent(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "add " + ChatColor.YELLOW + "command without the \"/\" or click", admin));
                TextComponent here = new TextComponent(ChatColor.GOLD + "" + ChatColor.ITALIC + " here ");
                here.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency 7"));
                here.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "eco give/take {player} {amount}")));
                TextComponent end = new TextComponent(ChatColor.YELLOW + "if you are using Essentials.");
                BaseComponent[] addCommand2 = new BaseComponent[]{start, here, end};
                admin.spigot().sendMessage(addCommand2);
                admin.sendMessage(getPrefix() + ChatColor.GRAY + "Substitute the player for {player} and the amount for {amount}. Type \"skip\" to not have an add command.");
                admin.sendMessage(" ");
                break;
            case 3:
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "remove " + ChatColor.YELLOW + "command without the \"/\".", admin));
                admin.sendMessage(getPrefix() + ChatColor.GRAY + "Substitute the player for {player} and the amount for {amount}. Type \"skip\" to not have a remove command.");
                admin.sendMessage(" ");
                break;
            case 4:
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Do you want to change the prefix and suffix?" + ChatColor.GRAY + " (Click)", admin));
                TextComponent noPS = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + ChatColor.UNDERLINE + "No");
                noPS.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency -1"));
                TextComponent yesPS = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + ChatColor.UNDERLINE + "Yes");
                yesPS.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency 5"));
                BaseComponent[] messagePS = new BaseComponent[]{space, noPS, space, yesPS};
                admin.spigot().sendMessage(messagePS);
                admin.sendMessage(" ");
                break;
            case 5:
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "prefix " + ChatColor.YELLOW + "for the currency.", admin));
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.GRAY + "Type \"skip\" to leave the prefix blank.", admin));
                admin.sendMessage(" ");
                break;
            case 6:
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Type in chat the " + ChatColor.BOLD + "suffix " + ChatColor.YELLOW + "for the currency.", admin));
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.GRAY + "Type \"skip\" to leave the suffix blank.", admin));
                admin.sendMessage(" ");
                break;
            case 9:
                String isOverridden = NumberFormatting.isOverrideVault() ? "IS" : "is NOT";
                admin.sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.GREEN + "Vault " + ChatColor.YELLOW + "was detected! Do you want to override this connection and use a placeholder or item? Currently, Vault " + ChatColor.ITALIC + isOverridden + ChatColor.YELLOW + " overridden." + ChatColor.GRAY + " (Click)", admin));
                TextComponent noVault = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + ChatColor.UNDERLINE + "No");
                noVault.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency 11"));
                TextComponent yesVault = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + ChatColor.UNDERLINE + "Yes");
                yesVault.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " currency 10"));
                BaseComponent[] messageVault = new BaseComponent[]{space, noVault, space, yesVault};
                admin.spigot().sendMessage(messageVault);
                admin.sendMessage(" ");
                break;
        }


    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (currencySetupPlayer == null || currencySetupPlayer != event.getPlayer().getUniqueId())
            return;
        if (event.getMessage().equalsIgnoreCase("cancel")) {
            currencySetupPlayer = null;
            currencySetupStage = 0;
            event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Exited currency setup.", event.getPlayer()));
            event.setCancelled(true);
            return;
        }
        NotBounties.getInstance().reloadConfig();
        FileConfiguration config = NotBounties.getInstance().getConfig();
        if (config.getKeys(true).size() <= 2) {
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the config.yml file. Fix the YAML formatting errors, or the plugin may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            currencySetupPlayer = null;
            currencySetupStage = 0;
            event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.RED + "Cannot edit the config.yml file! There are YAML formatting errors you must fix before continuing.", event.getPlayer()));
            event.setCancelled(true);
            return;
        }
        switch (currencySetupStage) {
            case 0:
                String currency;
                if (event.getMessage().equalsIgnoreCase("item")) {
                    Material hand = event.getPlayer().getInventory().getItemInMainHand().getType();
                    if (hand.isAir()) {
                        event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "You cannot set air as a currency! Try again.", event.getPlayer()));
                        event.setCancelled(true);
                        return;
                    }
                    currency = hand.toString();
                    event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Currency is now set to the item: " + ChatColor.WHITE + currency, event.getPlayer()));
                } else if (event.getMessage().contains("%")) {
                    currency = event.getMessage();
                    event.getPlayer().sendMessage(getPrefix() + ChatColor.YELLOW + "Currency is now set to the placeholder: " + ChatColor.WHITE + currency);
                    if (!ConfigOptions.getIntegrations().isPapiEnabled())
                        event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.RED + "PlaceholderAPI must be installed to use this!", event.getPlayer()));
                    if (currency.equalsIgnoreCase("%vault_eco_balance%")) {
                        if (ConfigOptions.getIntegrations().isPapiEnabled()) {
                            NotBounties.getServerImplementation().global().run(task -> {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "papi ecloud download Vault");
                            });
                            NotBounties.getServerImplementation().global().runDelayed(task -> {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "papi reload");
                            }, 20);
                        }
                        if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
                            event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.RED + "Vault must be installed to use this!", event.getPlayer()));
                    }
                    currencySetupStage++;
                } else {
                    Material material;
                    try {
                        material = Material.valueOf(event.getMessage().toUpperCase());
                    } catch (IllegalArgumentException | NullPointerException e) {
                        event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Could not find that material! Try again.", event.getPlayer()));
                        event.setCancelled(true);
                        return;
                    }
                    currency = material.toString();
                    event.getPlayer().sendMessage(LanguageOptions.parse(getPrefix() + ChatColor.YELLOW + "Currency is now set to the item: " + ChatColor.WHITE + currency, event.getPlayer()));
                }

                config.set("currency.object", currency);
                NotBounties.getInstance().saveConfig();
                break;
            case 2:
                List<String> aList = event.getMessage().equalsIgnoreCase("skip") ? new ArrayList<>() : Collections.singletonList(event.getMessage());
                config.set("currency.add-commands", aList);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(getPrefix() + ChatColor.YELLOW + "Add command is now set to: " + ChatColor.WHITE + aList);
                break;
            case 3:
                List<String> rList = event.getMessage().equalsIgnoreCase("skip") ? new ArrayList<>() : Collections.singletonList(event.getMessage());
                config.set("currency.remove-commands", rList);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(getPrefix() + ChatColor.YELLOW + "Remove command is now set to: " + ChatColor.WHITE + rList);
                break;
            case 5:
                String prefix = event.getMessage();
                if (prefix.equalsIgnoreCase("skip"))
                    prefix = "";
                config.set("currency.prefix", prefix);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + ChatColor.YELLOW + "Prefix is now set to: '" + ChatColor.WHITE + prefix + ChatColor.YELLOW + "'", event.getPlayer()));
                break;
            case 6:
                String suffix = event.getMessage();
                if (suffix.equalsIgnoreCase("skip"))
                    suffix = "";
                config.set("currency.suffix", suffix);
                NotBounties.getInstance().saveConfig();
                event.getPlayer().sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + ChatColor.YELLOW + "Suffix is now set to: '" + ChatColor.WHITE + suffix + ChatColor.YELLOW + "'", event.getPlayer()));
                currencySetupStage = -2;
                break;
            default:
                return;
        }
        currencySetupStage++;
        event.setCancelled(true);
        onCommand(event.getPlayer(), new String[]{"currency", currencySetupStage + ""});

    }
}

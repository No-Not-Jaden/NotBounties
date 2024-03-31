package me.jadenp.notbounties.utils.configuration;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.CommandPrompt;
import me.jadenp.notbounties.utils.externalAPIs.PlaceholderAPIClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.ui.gui.GUI.openGUI;
import static me.jadenp.notbounties.ui.gui.GUI.playerInfo;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.papiEnabled;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.prefix;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.checkAmount;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.tryParse;

public class ActionCommands {
    private static List<String> bountyClaimCommands;
    private static List<String> bigBountyCommands;

    public static void loadConfiguration(List<String> bountyClaimCommands, List<String> bigBountyCommands) {
        ActionCommands.bountyClaimCommands = bountyClaimCommands;
        ActionCommands.bigBountyCommands = bigBountyCommands;
    }

    public static void executeBountyClaim(Player player, Player killer, double totalBounty) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String command : bountyClaimCommands) {
                    execute(player, killer, totalBounty, command);
                }
            }
        }.runTaskLater(NotBounties.getInstance(), 10);
    }

    public static void executeGUI(Player player, List<String> commands) {
        Bounty bounty = BountyManager.getBounty(player);
        double totalBounty = bounty != null ? bounty.getTotalBounty() : 0;
        for (String command : commands) {
            execute(player, player, totalBounty, command);
        }
    }

    public static void executeBigBounty(Player player, double totalBounty) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String command : bigBountyCommands) {
                    execute(player, player, totalBounty, command);
                }
            }
        }.runTaskLater(NotBounties.getInstance(), 10);
    }

    private static void execute(Player player, Player killer, double totalBounty, String command) {
        if (command.isEmpty())
            return;
        PlayerGUInfo info = playerInfo.containsKey(player.getUniqueId()) ? playerInfo.get(player.getUniqueId()) : new PlayerGUInfo(1, "", new Object[0], new UUID[0], "");

        command = command.replaceAll("\\{player}", Matcher.quoteReplacement(player.getName()));
        command = command.replaceAll("\\{killer}", Matcher.quoteReplacement(killer.getName()));
        command = command.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.getValue(totalBounty)));
        command = command.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.formatNumber(totalBounty)));
        command = command.replaceAll("\\{cost}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.currencySuffix));
        command = command.replaceAll("\\{page}", Matcher.quoteReplacement(info.getPage() + ""));

        // check for {slot<x>}
        while (player.getOpenInventory().getType() != InventoryType.CRAFTING && command.contains("{slot") && command.substring(command.indexOf("{slot")).contains("}")) {
            String replacement = "";
            try {
                int slot = Integer.parseInt(command.substring(command.indexOf("{slot") + 5, command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length()));
                ItemStack item = player.getOpenInventory().getTopInventory().getContents()[slot];
                if (item != null) {
                    if (item.getType() == Material.PLAYER_HEAD) {
                        SkullMeta meta = (SkullMeta) item.getItemMeta();
                        assert meta != null;
                        OfflinePlayer p = meta.getOwningPlayer();
                        if (p != null && p.getName() != null) {
                            replacement = p.getName();
                        } else {
                            Bukkit.getLogger().warning("Invalid player for slot " + slot);
                        }
                    }
                    if (replacement == null)
                        replacement = "";
                    if (replacement.isEmpty()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null)
                            replacement = meta.getDisplayName();
                    }
                }
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Error getting slot in command: \n" + command);
            }
            command = command.substring(0, command.indexOf("{slot")) + replacement + command.substring(command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length() + 1);
        }

        // check for {player<x>}
        while (command.contains("{player") && command.substring(command.indexOf("{player")).contains("}")) {
            String replacement = "";
            String slotString = command.substring(command.indexOf("{player") + 7, command.substring(command.indexOf("{player")).indexOf("}") + command.substring(0, command.indexOf("{player")).length());
            try {
                int slot = Integer.parseInt(slotString);
                if (info.getPlayers().length > slot-1) {
                    replacement = NotBounties.getPlayerName(info.getPlayers()[slot-1]);
                }
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Error getting player in command: \n" + command);
            }
            command = command.replace(Matcher.quoteReplacement("{player" + slotString + "}"), Matcher.quoteReplacement(replacement));
            //command = command.substring(0, command.indexOf("{player")) + replacement + command.substring(command.substring(command.indexOf("{player")).indexOf("}") + command.substring(0, command.indexOf("{player")).length() + 1);
        }

        boolean canceled = false;
        int loops = 100; // to stop an infinite loop if the command isn't formatted correctly
        while (command.startsWith("<(") || command.startsWith(">(") || command.startsWith("@(") || command.startsWith("!@(") || command.startsWith("~player(") || command.startsWith("~killer(") || (player.getUniqueId().equals(killer.getUniqueId()) && (command.startsWith("@") || command.startsWith("!@")))) {
            if (command.startsWith("<(") && command.contains(") ")) {
                double amount;
                try {
                    amount = NumberFormatting.tryParse(command.substring(2, command.indexOf(") ")));
                } catch (NumberFormatException e) {
                    amount = 0;
                }
                command = command.substring(command.indexOf(") ") + 2);
                if (totalBounty >= amount)
                    canceled = true;
            } else if (command.startsWith(">(") && command.contains(") ")) {
                double amount;
                try {
                    amount = NumberFormatting.tryParse(command.substring(2, command.indexOf(") ")));
                } catch (NumberFormatException e) {
                    amount = 0;
                }
                command = command.substring(command.indexOf(") ") + 2);
                if (totalBounty <= amount)
                    canceled = true;
            }

            if (command.startsWith("@(player)")) {
                String permission = command.substring(9, command.indexOf(" "));
                if (!player.hasPermission(permission))
                    canceled = true;
                command = command.substring(command.indexOf(" ") + 1);
            } else if (command.startsWith("!@(player)")) {
                String permission = command.substring(10, command.indexOf(" "));
                if (player.hasPermission(permission))
                    canceled = true;
                command = command.substring(command.indexOf(" ") + 1);
            } else if (command.startsWith("@(killer)")) {
                String permission = command.substring(9, command.indexOf(" "));
                if (!killer.hasPermission(permission))
                    canceled = true;
                command = command.substring(command.indexOf(" ") + 1);
            } else if (command.startsWith("!@(killer)")) {
                String permission = command.substring(10, command.indexOf(" "));
                if (killer.hasPermission(permission))
                    canceled = true;
                command = command.substring(command.indexOf(" ") + 1);
            } else if (player.getUniqueId().equals(killer.getUniqueId())) {
                if (command.startsWith("@")) {
                    String permission = command.substring(1, command.indexOf(" "));
                    if (!player.hasPermission(permission))
                        canceled = true;
                    command = command.substring(command.indexOf(" ") + 1);
                } else if (command.startsWith("!@")) {
                    String permission = command.substring(2, command.indexOf(" "));
                    if (player.hasPermission(permission))
                        canceled = true;
                    command = command.substring(command.indexOf(" ") + 1);
                }
            }

            if (command.startsWith("~player(") && command.contains(") ")) {
                String requirement = command.substring(8, command.indexOf(") "));
                if (isRequirementCanceled(requirement, player))
                    canceled = true;
                command = command.substring(command.indexOf(") ") + 2);
            } else if (command.startsWith("~killer(") && command.contains(") ")) {
                String requirement = command.substring(8, command.indexOf(") "));
                if (isRequirementCanceled(requirement, killer))
                    canceled = true;
                command = command.substring(command.indexOf(") ") + 2);
            }

            if (loops == 0) {
                Bukkit.getLogger().warning("[NotBounties] Could not complete bounty claim command! A conditional is not formatted correctly!");
                Bukkit.getLogger().warning("here -> " + command);
                canceled = true;
            }
            loops--;
            if (canceled)
                break;
        }
        if (canceled)
            return;

        //Bukkit.getLogger().info("Parsed Action: \"" + command + "\"");

        if (command.startsWith("[player] ") || command.startsWith("[p] ")) {
            if (papiEnabled)
                command = new PlaceholderAPIClass().parse(player, command);
            if (command.startsWith("[player] "))
                command = command.substring(9);
            else
                command = command.substring(4);
            Bukkit.dispatchCommand(player,command);
        } else if (command.startsWith("[killer] ")) {
            if (papiEnabled)
                command = new PlaceholderAPIClass().parse(killer, command);
            Bukkit.dispatchCommand(killer, command.substring(9));
        } else if (command.startsWith("[message_player] ")) {
            String message = command.substring(17);
            player.sendMessage(LanguageOptions.parse(prefix + message, player));
        } else if (command.startsWith("[message_killer] ")) {
            String message = command.substring(17);
            killer.sendMessage(LanguageOptions.parse(prefix + message, killer));
        } else if (command.startsWith("[broadcast] ")) {
            String message = command.substring(12);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!NotBounties.disableBroadcast.contains(p.getUniqueId())) {
                    p.sendMessage(LanguageOptions.parse(prefix + message, killer));
                }
            }
        } else if (command.startsWith("[sound_player] ")) {
            command = command.substring(8);
            double volume = 1;
            double pitch = 1;
            String sound;
            if (command.contains(" ")) {
                sound = command.substring(0, command.indexOf(" "));
                command = command.substring(sound.length() + 1);
                try {
                    if (command.contains(" ")) {
                        volume = NumberFormatting.tryParse(command.substring(0, command.indexOf(" ")));
                        command = command.substring(command.indexOf(" ") + 1);
                        pitch = NumberFormatting.tryParse(command);
                    } else {
                        volume = NumberFormatting.tryParse(command);
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[NotBounties] Unknown number for [sound_player] command in bounty-claim-commands : " + command);
                    return;
                }
            } else {
                sound = command;
            }
            Sound realSound;
            try {
                realSound = Sound.valueOf(sound.toUpperCase());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound_player] command in bounty-claim-commands : " + sound);
                return;
            }
            player.playSound(player.getEyeLocation(), realSound, (float) volume, (float) pitch);
        } else if (command.startsWith("[sound_killer] ")) {
            command = command.substring(8);
            double volume = 1;
            double pitch = 1;
            String sound;
            if (command.contains(" ")) {
                sound = command.substring(0, command.indexOf(" "));
                command = command.substring(sound.length() + 1);
                try {
                    if (command.contains(" ")) {
                        volume = NumberFormatting.tryParse(command.substring(0, command.indexOf(" ")));
                        command = command.substring(command.indexOf(" ") + 1);
                        pitch = NumberFormatting.tryParse(command);
                    } else {
                        volume = NumberFormatting.tryParse(command);
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[NotBounties] Unknown number for [sound_killer] command in bounty-claim-commands : " + command);
                    return;
                }
            } else {
                sound = command;
            }
            Sound realSound;
            try {
                realSound = Sound.valueOf(sound.toUpperCase());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound_killer] command in bounty-claim-commands : " + sound);
                return;
            }
            killer.playSound(killer.getEyeLocation(), realSound, (float) volume, (float) pitch);
        } else if (command.startsWith("[sound] ")) {
            command = command.substring(8);
            double volume = 1;
            double pitch = 1;
            String sound;
            if (command.contains(" ")) {
                sound = command.substring(0, command.indexOf(" "));
                command = command.substring(sound.length() + 1);
                try {
                    if (command.contains(" ")) {
                        volume = NumberFormatting.tryParse(command.substring(0, command.indexOf(" ")));
                        command = command.substring(command.indexOf(" ") + 1);
                        pitch = NumberFormatting.tryParse(command);
                    } else {
                        volume = NumberFormatting.tryParse(command);
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[NotBounties] Unknown number for [sound] command in bounty-claim-commands : " + command);
                    return;
                }
            } else {
                sound = command;
            }
            Sound realSound;
            try {
                realSound = Sound.valueOf(sound.toUpperCase());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound] command in bounty-claim-commands : " + sound);
                return;
            }
            killer.getWorld().playSound(killer.getLocation(), realSound, (float) volume, (float) pitch);
        } else if (command.startsWith("[gui]")) {
            int amount = 1; // default page
            String guiName = command.substring(6); // remove "[gui] "
            if (guiName.contains(" ")) {
                // has additional parameters
                try {
                    String number = guiName.substring(guiName.indexOf(" ") + 1); // get everything after the gui name
                    if (number.contains(" ")) {
                        // has more parameters than just page number
                        amount = Integer.parseInt(number.substring(0, number.indexOf(" "))); // parse number
                    } else {
                        amount = Integer.parseInt(number); // parse number
                    }
                } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    // could not parse number or there was only a space and no number
                }
                guiName = guiName.substring(0, guiName.indexOf(" ")); // change gui name
            }
            if (guiName.equalsIgnoreCase("leaderboard")) {
                // this GUI can have the leaderboard type parameter
                // ex: [gui] leaderboard 1 kills
                Leaderboard l = Leaderboard.ALL; // default leaderboard
                try {
                    // try to parse leaderboard type from last parameter
                    l = Leaderboard.valueOf(command.substring(command.lastIndexOf(" ") + 1).toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // could not parse leaderboard type - default leaderboard is used
                }
                openGUI(player, guiName, amount, l); // open gui
            } else if (guiName.equalsIgnoreCase("set-bounty") || guiName.equalsIgnoreCase("set-whitelist")) {
                // these GUIs can have the offline parameter
                // ex: [gui] set-bounty 1 offline
                int length = 7 + guiName.length(); // length of [gui] and the gui name
                if (command.length() > length && command.substring(length + 1).contains(" ")) {
                    // additional parameters present
                    String afterName = command.substring(length + 1); // string after name
                    // set last argument - remove the page number if there is one
                    String lastArg = afterName.contains(" ") ? afterName.substring(afterName.indexOf(" ") + 1) : afterName;
                    // check if the current GUI already has the offline parameter
                    // if so, set the last argument to nothing to toggle it
                    if (lastArg.equalsIgnoreCase("offline") && info.getData().length > 0 && info.getData()[0] instanceof String && ((String) info.getData()[0]).equalsIgnoreCase("offline")) {
                        lastArg = "";
                    }
                    openGUI(player, guiName, amount, lastArg);
                } else {
                    openGUI(player, guiName, amount);
                }
            } else {
                openGUI(player, guiName, amount);
            }
        } else if (command.startsWith("[cprompt] ") || command.startsWith("[pprompt] ")) {
            boolean playerPrompt = command.startsWith("[pprompt] ");
            command = command.substring(10);
            player.closeInventory();
            Prompt.addCommandPrompt(player.getUniqueId(), new CommandPrompt(player, command, playerPrompt));
        } else if (command.startsWith("[close]")) {
            player.getOpenInventory().close();
            playerInfo.remove(player.getUniqueId());  // would only do something for bedrock players
        } else if (command.startsWith("[next]")) {
            int amount = 1;
            try {
                amount = Integer.parseInt(command.substring(7));
            } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            }
            if (info.getGuiType().equals("select-price")) {
                GUIOptions gui = GUI.getGUI("select-price");
                String uuid;
                if (info.getData().length > 0) {
                    uuid = (String) info.getData()[0];
                } else {
                    if (gui != null)
                        uuid = Objects.requireNonNull(((SkullMeta) Objects.requireNonNull(player.getOpenInventory().getTopInventory().getContents()[gui.getPlayerSlots().get(0)].getItemMeta())).getOwningPlayer()).getUniqueId().toString();
                    else
                        // select-price GUI hasn't been set up
                        uuid = new UUID(0,0).toString();
                }
                openGUI(player, info.getGuiType(), info.getPage() + amount, uuid);
            } else if (info.getGuiType().equals("leaderboard")) {
                Leaderboard leaderboard = (Leaderboard) info.getData()[0];
                openGUI(player, info.getGuiType(), info.getPage() + amount, leaderboard);
            } else if (!info.getGuiType().isEmpty()){
                openGUI(player, info.getGuiType(), info.getPage() + amount, info.getData());
            }

        } else if (command.startsWith("[back]")) {
            int amount = 1;
            try {
                amount = Integer.parseInt(command.substring(7));
            } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            }
            if (info.getGuiType().equals("select-price")) {
                String uuid = (String) info.getData()[0]; //((SkullMeta) event.getInventory().getContents()[gui.getPlayerSlots().get(0)].getItemMeta()).getOwningPlayer().getUniqueId().toString();
                openGUI(player, info.getGuiType(), info.getPage() - amount, uuid);
            } else if (info.getGuiType().equals("leaderboard")) {
                Leaderboard leaderboard = (Leaderboard) info.getData()[0];
                openGUI(player, info.getGuiType(), info.getPage() - amount, leaderboard);
            } else {
                openGUI(player, info.getGuiType(), info.getPage() - amount, info.getData());
            }
        } else if (command.equalsIgnoreCase("[offline]")) {
            if (info.getData().length > 0 && info.getData()[0] instanceof String && ((String) info.getData()[0]).equalsIgnoreCase("offline"))
                openGUI(player, info.getGuiType(), info.getPage());
            else
                openGUI(player, info.getGuiType(), info.getPage(), "offline");

        } else if (command.equalsIgnoreCase("<respawn>")) {
            if (player.isDead())
                player.spigot().respawn();
        } else {
            if (papiEnabled)
                command = new PlaceholderAPIClass().parse(player, command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }


    private static boolean isRequirementCanceled(String requirement, OfflinePlayer player) {
        try {
            String placeholder = requirement.substring(0, requirement.indexOf(" "));
            String operator = requirement.substring(requirement.indexOf(" ") + 1, requirement.lastIndexOf(" "));
            String value = requirement.substring(requirement.lastIndexOf(" ") + 1);
            Object parsedValue = parseValue(value);

            if (placeholder.contains("%") && papiEnabled) {
                String parsed = LanguageOptions.parse(placeholder, player);
                Object parsedPlaceholder = parseValue(parsed);

                // value types don't match
                if (parsedValue instanceof Boolean && !(parsedPlaceholder instanceof Boolean)) {
                    return true;
                } else if (parsedValue instanceof Integer && !(parsedPlaceholder instanceof Integer)) {
                    return true;
                } else if (parsedValue instanceof Double && !(parsedPlaceholder instanceof Double)) {
                    return true;
                } else if (parsedValue instanceof String && !(parsedPlaceholder instanceof String)) {
                    return true;
                }
                return !compareObjects(parsedValue, parsedPlaceholder, operator);
            } else {
                int customModelData = -1;
                if (placeholder.contains("<") && placeholder.contains(">"))
                    try {
                        customModelData = (int) tryParse(placeholder.substring(placeholder.indexOf("<") + 1, placeholder.indexOf(">")));
                        placeholder = placeholder.substring(0, placeholder.indexOf("<"));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotRanks] Could not get custom model data from " + placeholder);
                        Bukkit.getLogger().warning(e.toString());
                    }
                // check if it is a material
                if (player.isOnline()) {
                    assert player.getPlayer() != null;
                    Material m = Material.getMaterial(placeholder);
                    if (m != null) {
                        if (parsedValue instanceof Integer || parsedValue instanceof Double) {
                            int reqValue = parsedValue instanceof Double ? ((Double) parsedValue).intValue() : (int) parsedValue;
                            int playerValue = checkAmount(player.getPlayer(), m, customModelData);
                            return !compareObjects(reqValue, playerValue, operator);
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning("Could not check requirement: " + requirement + "\nIs it formatted properly?");
        }
        return true;
    }

    private static Object parseValue(String str) {
        if (str.equalsIgnoreCase("true"))
            return true;
        if (str.equalsIgnoreCase("false"))
            return false;

        try {
            return tryParse(str);
        } catch (NumberFormatException e) {
            return str;
        }
    }

    private static boolean compareObjects(Object parsedValue, Object parsedPlaceholder, String operator) {
        if (parsedValue instanceof Boolean) {
            boolean a = (boolean) parsedValue;
            boolean b = (boolean) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return a != b;
            }
            return a == b;
        } else if (parsedValue instanceof Integer) {
            int a = (int) parsedValue;
            int b = (int) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return a != b;
            }
            if (operator.equalsIgnoreCase("=")) {
                return a == b;
            }
            if (operator.equalsIgnoreCase(">=")) {
                return b >= a;
            }
            if (operator.equalsIgnoreCase("<=")) {
                return b <= a;
            }
            if (operator.equalsIgnoreCase(">")) {
                return b > a;
            }
            if (operator.equalsIgnoreCase("<")) {
                return b < a;
            }
            return b >= a;
        } else if (parsedValue instanceof Double) {
            double a = (double) parsedValue;
            double b = (double) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return a != b;
            }
            if (operator.equalsIgnoreCase("=")) {
                return a == b;
            }
            if (operator.equalsIgnoreCase(">=")) {
                return b >= a;
            }
            if (operator.equalsIgnoreCase("<=")) {
                return b <= a;
            }
            if (operator.equalsIgnoreCase(">")) {
                return b > a;
            }
            if (operator.equalsIgnoreCase("<")) {
                return b < a;
            }
            return b >= a;
        } else if (parsedValue instanceof String) {
            String a = (String) parsedValue;
            String b = (String) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return !a.equalsIgnoreCase(b);
            }
            return a.equalsIgnoreCase(b);
        }
        return false;
    }
}

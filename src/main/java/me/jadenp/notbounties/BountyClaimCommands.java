package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.LanguageOptions;
import me.jadenp.notbounties.utils.NumberFormatting;
import me.jadenp.notbounties.utils.PlaceholderAPIClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.utils.ConfigOptions.papiEnabled;
import static me.jadenp.notbounties.utils.LanguageOptions.prefix;
import static me.jadenp.notbounties.utils.NumberFormatting.checkAmount;
import static me.jadenp.notbounties.utils.NumberFormatting.tryParse;

public class BountyClaimCommands {
    private static List<String> bountyClaimCommands;
    public static void loadConfiguration(List<String> bountyClaimCommands) {
        BountyClaimCommands.bountyClaimCommands = bountyClaimCommands;
    }

    public static void execute(Player player, Player killer, double totalBounty) {

        new BukkitRunnable() {
            @Override
            public void run() {
                // config.yml
                for (String command : bountyClaimCommands) {
                    command = command.replaceAll("\\{player}", Matcher.quoteReplacement(player.getName()));
                    command = command.replaceAll("\\{killer}", Matcher.quoteReplacement(killer.getName()));
                    command = command.replaceAll("\\{amount}", Matcher.quoteReplacement(NumberFormatting.getValue(totalBounty)));
                    command = command.replaceAll("\\{bounty}", Matcher.quoteReplacement(NumberFormatting.formatNumber(totalBounty)));
                    command = command.replaceAll("\\{cost}", Matcher.quoteReplacement(NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.currencySuffix));

                    boolean canceled = false;
                    int loops = 100; // to stop an infinite loop if the command isn't formatted correctly
                    while (command.startsWith("<(") || command.startsWith(">(") || command.startsWith("@(") || command.startsWith("!@(") || command.startsWith("~player(") || command.startsWith("~killer(")) {
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
                            Bukkit.getLogger().warning( "here -> " + command);
                            canceled = true;
                        }
                        loops--;
                        if (canceled)
                            break;
                    }
                    if (canceled)
                        continue;





                    if (command.startsWith("[player] ")) {
                        if (papiEnabled)
                            command = new PlaceholderAPIClass().parse(player, command);
                        Bukkit.dispatchCommand(player, command.substring(9));
                    } else if (command.startsWith("[killer] ")) {
                        if (papiEnabled)
                            command = new PlaceholderAPIClass().parse(killer, command);
                        Bukkit.dispatchCommand(killer, command.substring(9));
                    } else if (command.startsWith("[message_player] ")) {
                        String message = command.substring(17);
                        player.sendMessage(LanguageOptions.parse(prefix + message, player));
                    }  else if (command.startsWith("[message_killer] ")) {
                        String message = command.substring(17);
                        killer.sendMessage(LanguageOptions.parse(prefix + message, killer));
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
                                continue;
                            }
                        } else {
                            sound = command;
                        }
                        Sound realSound;
                        try {
                            realSound = Sound.valueOf(sound.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound_player] command in bounty-claim-commands : " + sound);
                            continue;
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
                                continue;
                            }
                        } else {
                            sound = command;
                        }
                        Sound realSound;
                        try {
                            realSound = Sound.valueOf(sound.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound_killer] command in bounty-claim-commands : " + sound);
                            continue;
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
                                continue;
                            }
                        } else {
                            sound = command;
                        }
                        Sound realSound;
                        try {
                            realSound = Sound.valueOf(sound.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound] command in bounty-claim-commands : " + sound);
                            continue;
                        }
                        killer.getWorld().playSound(killer.getLocation(), realSound, (float) volume, (float) pitch);
                    } else if (command.equalsIgnoreCase("<respawn>")) {
                        if (player.isDead())
                            player.spigot().respawn();
                    } else {
                        if (papiEnabled)
                            command = new PlaceholderAPIClass().parse(player, command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }

            }
        }.runTaskLater(NotBounties.getInstance(), 10);
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
        } catch (IndexOutOfBoundsException e){
            Bukkit.getLogger().warning("Could not check requirement: " + requirement + "\nIs it formatted properly?");
        }
        return true;
    }

    private static Object parseValue(String str) {
        if (str.equalsIgnoreCase("true"))
            return true;
        if (str.equalsIgnoreCase("false"))
            return false;

        try{
            return tryParse(str);
        } catch (NumberFormatException e){
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

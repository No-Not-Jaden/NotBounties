package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.configuration.Prompt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import static me.jadenp.notbounties.utils.configuration.LanguageOptions.parse;

public class CommandPrompt {
    private final String command;
    private final boolean playerPrompt;
    private BukkitTask expireTask = null;
    private boolean silentCancel = false;
    private boolean expired = false;
    private final Player player;
    private final String prompt;
    private int attemptsUsed = 1;
    private enum ResponseType {
        PLAYER, NUMBER, ANY
    }
    private ResponseType responseType = ResponseType.ANY;

    public CommandPrompt(Player player, String command, boolean playerPrompt) {
        String prompt = "&eEnter anything in chat.";
        if (command.contains("<") && command.substring(command.indexOf("<")).contains(">")) {
            prompt = command.substring(command.indexOf("<") + 1, command.substring(0, command.indexOf("<")).length() + command.substring(command.indexOf("<")).indexOf(">"));
        }

        if (command.contains(prompt))
            command = command.replace(prompt, "~placeholder~");

        if (prompt.startsWith("NUMBER~")) {
            responseType = ResponseType.NUMBER;
            prompt = prompt.substring(7);
        } else if (prompt.startsWith("PLAYER~")) {
            responseType = ResponseType.PLAYER;
            prompt = prompt.substring(7);
        }

        player.sendMessage(parse(prompt, player));

        this.command = command;
        this.playerPrompt = playerPrompt;
        this.player = player;
        this.prompt = prompt;

        refreshExpireTask();
    }

    private void refreshExpireTask(){
        if (expireTask != null)
            expireTask.cancel();
        expireTask = new BukkitRunnable() {
            @Override
            public void run() {
                expired = true;
                if (!silentCancel)
                    if (player.isOnline())
                        player.sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.promptExpire, player));
            }
        }.runTaskLater(NotBounties.getInstance(), Prompt.timeLimit * 20L);
    }

    public String getCommand() {
        return command;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isSilentCancel() {
        return silentCancel;
    }

    public void expire() {
        expired = true;
        if (expireTask != null)
            expireTask.cancel();
    }

    public void reprompt(){
        if (silentCancel) {
            player.sendMessage(parse(prompt, player));
            attemptsUsed++;
            silentCancel = false;
            refreshExpireTask();
        }
    }

    public int getAttemptsUsed() {
        return attemptsUsed;
    }

    public void executeCommand(String message) {
        message = ChatColor.stripColor(message);
        String command = this.command.replace("<~placeholder~>", message);
        String finalMessage = message;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerPrompt) {
                    Bukkit.dispatchCommand(player, command);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }.runTaskLater(NotBounties.getInstance(), 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (responseType == ResponseType.NUMBER) {
                    try {
                        NumberFormatting.tryParse(finalMessage);
                    } catch (NumberFormatException e) {
                        Prompt.failExecute(player.getUniqueId());
                    }
                } else if (responseType == ResponseType.PLAYER) {
                    if (NotBounties.getPlayer(finalMessage) == null) {
                        Prompt.failExecute(player.getUniqueId());
                    }
                }
            }
        }.runTaskLater(NotBounties.getInstance(), 2);

        silentCancel = true;


    }
}

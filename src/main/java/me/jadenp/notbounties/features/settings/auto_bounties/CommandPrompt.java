package me.jadenp.notbounties.features.settings.auto_bounties;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static me.jadenp.notbounties.features.LanguageOptions.parse;

public class CommandPrompt {
    private final String command;
    private final boolean playerPrompt;
    private TaskImplementation<Void> expireTask = null;
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
        String promptString = "&eEnter anything in chat."; // default prompt string
        if (command.contains("<") && command.substring(command.indexOf("<")).contains(">")) {
            promptString = command.substring(command.indexOf("<") + 1, command.substring(0, command.indexOf("<")).length() + command.substring(command.indexOf("<")).indexOf(">"));
        }

        if (command.contains(promptString))
            command = command.replace(promptString, "~placeholder~");

        if (promptString.startsWith("NUMBER~")) {
            responseType = ResponseType.NUMBER;
            promptString = promptString.substring(7);
        } else if (promptString.startsWith("PLAYER~")) {
            responseType = ResponseType.PLAYER;
            promptString = promptString.substring(7);
        }

        player.sendMessage(parse(promptString, player));

        this.command = command;
        this.playerPrompt = playerPrompt;
        this.player = player;
        this.prompt = promptString;

        refreshExpireTask();
    }

    private void refreshExpireTask(){
        if (expireTask != null)
            expireTask.cancel();
        expireTask = NotBounties.getServerImplementation().entity(player).runDelayed(task -> {
            expired = true;
            if (!silentCancel && player.isOnline())
                    player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("prompt-expire"), player));
        }, Prompt.getTimeLimit() * 20L);

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
        String commandString = this.command.replace("<~placeholder~>", message);
        String finalMessage = message;
        NotBounties.getServerImplementation().entity(player).runDelayed(task -> {
            if (playerPrompt) {
                Bukkit.dispatchCommand(player, commandString);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandString);
            }
        }, 1);
        NotBounties.getServerImplementation().entity(player).runDelayed(task -> {
            if (responseType == ResponseType.NUMBER) {
                try {
                    NumberFormatting.tryParse(finalMessage);
                } catch (NumberFormatException e) {
                    Prompt.failExecute(player.getUniqueId());
                }
            } else if (responseType == ResponseType.PLAYER) {
                if (!LoggedPlayers.isLogged(finalMessage)) {
                    Prompt.failExecute(player.getUniqueId());
                }
            }
        }, 1);

        silentCancel = true;


    }
}

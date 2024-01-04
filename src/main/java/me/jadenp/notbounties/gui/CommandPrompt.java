package me.jadenp.notbounties.gui;

public class CommandPrompt {
    private final String command;
    private final boolean playerPrompt;

    public CommandPrompt(String command, boolean playerPrompt) {

        this.command = command;
        this.playerPrompt = playerPrompt;
    }

    public String getCommand() {
        return command;
    }

    public boolean isPlayerPrompt() {
        return playerPrompt;
    }
}

package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.Immunity;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static me.jadenp.notbounties.utils.LanguageOptions.*;


public class Tutorial {                                                              //

    public static void onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1 || args[1].equals("1")) {
            for (int i = 0; i < 10; i++)
                sender.sendMessage("");
            sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
            sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Bounty Broadcast");
            sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
            sender.sendMessage(ChatColor.WHITE + "Getting messages for all the bounties set and claimed");
            sender.sendMessage(ChatColor.WHITE + "can be annoying. You can disable messages for bounties");
            sendCommandMessage(sender, "not concerning you with {command}.", "/bounty bdc", true);
            sendPageMessage(sender, 1);
            sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
            return;
        }
        int page;
        try {
            page = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(parse(prefix + unknownCommand, null));
            return;
        }
        for (int i = 0; i < 10; i++)
            sender.sendMessage("");
        sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
        switch (page) {
            case 2:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Bounty Information");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sendCommandMessage(sender, "To view bounties, open the bounty GUI with {command}. To", "/bounty", true);
                sendCommandMessage(sender, "view bounties in chat, do {command}. To get more", "/bounty list", true);
                sender.sendMessage(ChatColor.WHITE + "information on a bounty, you can run the command");
                sendCommandMessage(sender, "{command}. This command will show you everyone", "/bounty check (player)", false);
                sender.sendMessage(ChatColor.WHITE + "who has set a bounty on that specific player.");
                break;
            case 3:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Bounty Statistics");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sender.sendMessage(ChatColor.WHITE + "A variety of statistics are recorded to be apart of the");
                sender.sendMessage(ChatColor.WHITE + "bounty leaderboards. Recorded values are:");
                sender.sendMessage(ChatColor.WHITE + "- Total bounty accumulated. (All)");
                sender.sendMessage(ChatColor.WHITE + "- Bounty kills. (Kills)");
                sender.sendMessage(ChatColor.WHITE + "- Claimed money from bounties. (Claimed)");
                sender.sendMessage(ChatColor.WHITE + "- Deaths with a bounty. (Deaths)");
                sender.sendMessage(ChatColor.WHITE + "- Current immunity. (Immunity)");
                sendCommandMessage(sender, "You can view any of your values with {command}.", "/bounty stat (keyword)", false);
                sender.sendMessage(ChatColor.WHITE + "The keywords are in parenthesis. To open the leaderboard");
                sendCommandMessage(sender, "GUI, do {command}. You can view the leaderboard", "/bounty top (keyword)", false);
                sender.sendMessage(ChatColor.WHITE + "in chat by adding \"list\" to the end of the command.");
                break;
            case 4:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Bounty Whitelist");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sender.sendMessage(ChatColor.WHITE + "You can pick up to 10 players to be whitelisted to a bounty");
                sender.sendMessage(ChatColor.WHITE + "you set. Each additional player on your whitelist makes");
                sender.sendMessage(ChatColor.WHITE + "bounties cost "+ NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(ConfigOptions.bountyWhitelistCost) + NumberFormatting.currencySuffix + ChatColor.WHITE + " more. You can open the whitelist GUI");
                sendCommandMessage(sender, "with {command}. To edit your whitelist, do", "/bounty whitelist", true);
                sendCommandMessage(sender, "{command}.", "/bounty whitelist (add/remove/set) (whitelisted players)", false);
                sender.sendMessage(ChatColor.WHITE + "You can specify multiple players separated by spaces if");
                sender.sendMessage(ChatColor.WHITE + "necessary. To view your whitelisted players in chat, do");
                sendCommandMessage(sender, "{command}. You can clear your whitelist by", "/bounty whitelist view", true);
                sendCommandMessage(sender, "doing {command}.", "/bounty whitelist reset", true);
                break;
            case 5:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Bounty Whitelist cont.");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                if (variableWhitelist) {
                    sender.sendMessage(ChatColor.WHITE + "Don't worry if you forget a few players. You can change");
                    sender.sendMessage(ChatColor.WHITE + "your whitelist, and all bounties set by you will have");
                    sender.sendMessage(ChatColor.WHITE + "their whitelists changed.");
                } else {
                    sender.sendMessage(ChatColor.WHITE + "You have to set your whitelist before you set a bounty.");
                    sender.sendMessage(ChatColor.WHITE + "Changing your whitelist after the bounty has been set");
                    sender.sendMessage(ChatColor.WHITE + "will not do anything.");
                }
                break;
            case 6:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Setting a Bounty");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sender.sendMessage(ChatColor.WHITE + "Setting a bounty is fairly simple. You only need 2 parts:");
                sender.sendMessage(ChatColor.WHITE + "the name of your target, and the price. You can set a");
                sendCommandMessage(sender, "bounty through the GUI with {command} or " + ChatColor.GOLD + "/bounty (player)" + ChatColor.WHITE + ".", "/bounty set", true);
                sender.sendMessage(ChatColor.WHITE + "Bounties also have a tax of " + ConfigOptions.bountyTax * 100 + "%. You can preview the");
                sender.sendMessage(ChatColor.WHITE + "cost in the GUI. To set a bounty with only a command,");
                sendCommandMessage(sender, "use {command}.", "/bounty (player) (amount)", false);
                break;
            case 7:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Buying Your Bounty");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sender.sendMessage(ChatColor.WHITE + "Don't like a bounty set on you? You can buy back your");
                sender.sendMessage(ChatColor.WHITE + "own bounties for " + ConfigOptions.buyBackInterest * 100 + "% of the bounty cost. To make");
                sender.sendMessage(ChatColor.WHITE + "the purchase, left click your bounty in the GUI or do");
                sendCommandMessage(sender, "{command}.", "/bounty buy", true);
                break;
            case 8:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Immunity");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sender.sendMessage(ChatColor.WHITE + "To avoid having to pay the hefty interest rate when");
                sender.sendMessage(ChatColor.WHITE + "buying your own bounty, you can buy immunity with");
                switch (Immunity.immunityType) {
                    case DISABLE:
                        sender.sendMessage(ChatColor.WHITE + "nothing. (disabled)");
                        break;
                    case PERMANENT:
                        sendCommandMessage(sender, "{command}. Immunity is permanent and costs " + NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(Immunity.getPermanentCost()) + NumberFormatting.currencySuffix + ChatColor.WHITE + ".", "/bounty immunity", true);
                        break;
                    case SCALING:
                        sendCommandMessage(sender, "{command}. Immunity will protect you", "/bounty immunity (price)", false);
                        sender.sendMessage(ChatColor.WHITE + "from bounties set under " + NumberFormatting.formatNumber(Immunity.getScalingRatio()) + "x the price you spend.");
                        break;
                    case TIME:
                        sendCommandMessage(sender, "{command}. Immunity will protect you", "/bounty immunity (price)", false);
                        sender.sendMessage(ChatColor.WHITE + "for price x " + Immunity.getTime() + " seconds.");
                        break;
                }
            break;
            case 9:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Remove Immunity");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sender.sendMessage(ChatColor.WHITE + "If your immunity is bothersome, you can remove it with");
                sendCommandMessage(sender, "{command}. You will not get a refund.", "/bounty immunity remove", true);
                break;
            case 10:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Wanted Posters");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sender.sendMessage(ChatColor.WHITE + "Wanted posters are helpful for letting people know of a");
                sendCommandMessage(sender, "bounty you've set. You can get one with {command}.", "/bounty poster (player)", false);
                sender.sendMessage(ChatColor.WHITE + "Sometimes finding players can be difficult, but with");
                sender.sendMessage(ChatColor.WHITE + "the bounty tracker, you need not worry! To get one,");
                sendCommandMessage(sender, "do {command}.", "/bounty tracker (player)", false);
                break;
            case 11:
                sender.sendMessage("                    " + ChatColor.YELLOW + ChatColor.BOLD + "Commands");
                sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");
                sendCommandMessage(sender, "To view the commands you have access to, do {command}.", "/bounty help", false);
                break;
        }
        sendPageMessage(sender, page);
        sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "                                        ");


    }

    private static void sendCommandMessage(CommandSender sender, String message, String command, boolean runCommand) {
        TextComponent before = new TextComponent(ChatColor.WHITE + message.substring(0, message.indexOf("{command}")));
        TextComponent middle = new TextComponent(ChatColor.GOLD + command);
        TextComponent end = new TextComponent(ChatColor.WHITE + message.substring(message.indexOf("{command}") + 9));
        String hover = runCommand ? ChatColor.YELLOW + "Run Command" : ChatColor.YELLOW + "Prompt Command";
        middle.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
        if (runCommand)
            middle.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        else {
            if (command.contains("("))
                command = command.substring(0, command.indexOf("("));
            middle.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        }
        BaseComponent[] baseComponents = new BaseComponent[]{before, middle, end};
        sender.spigot().sendMessage(baseComponents);
    }

    private static void sendPageMessage(CommandSender sender, int page) {
        int maxPage = 11;
        TextComponent space = new TextComponent("     ");
        TextComponent back = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋘⋘⋘");
        TextComponent middle = new TextComponent("      " + ChatColor.GRAY + "Page " + page + "      ");
        TextComponent next = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋙⋙⋙");
        back.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Previous Page")));
        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Next Page")));
        back.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties tutorial " + (page - 1)));
        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties tutorial " + (page + 1)));
        BaseComponent[] baseComponents = new BaseComponent[]{space, back, middle, next};
        if (page == 1)
            baseComponents[1] = space;
        if (page == maxPage)
            baseComponents[3] = space;
        sender.spigot().sendMessage(baseComponents);
    }
}

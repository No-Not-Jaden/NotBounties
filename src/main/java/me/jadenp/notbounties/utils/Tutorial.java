package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.utils.configuration.Immunity;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;


public class Tutorial {

    private Tutorial(){}

    private static final List<List<String>> pages = new ArrayList<>(16);
    private static String runCommandHover;
    private static String suggestCommandHover;
    private static String nextPageHover;
    private static String previousPageHover;

    public static void loadConfiguration(ConfigurationSection configuration) {
        pages.clear();
        pages.add(configuration.getStringList("broadcast"));
        pages.add(configuration.getStringList("bounty-information"));
        pages.add(configuration.getStringList("bounty-statistics"));
        pages.add(configuration.getStringList("bounty-whitelist"));
        pages.add(configuration.getStringList("variable-whitelist"));
        pages.add(configuration.getStringList("fixed-whitelist"));
        pages.add(configuration.getStringList("bounty-set"));
        pages.add(configuration.getStringList("bounty-buy"));
        pages.add(configuration.getStringList("immunity-general"));
        pages.add(configuration.getStringList("permanent-immunity"));
        pages.add(configuration.getStringList("scaling-immunity"));
        pages.add(configuration.getStringList("time-immunity"));
        pages.add(configuration.getStringList("remove-immunity"));
        pages.add(configuration.getStringList("bounty-trackers"));
        pages.add(configuration.getStringList("wanted-posters"));
        pages.add(configuration.getStringList("commands"));

        runCommandHover = configuration.getString("run-command-hover");
        suggestCommandHover = configuration.getString("suggest-command-hover");
        nextPageHover = configuration.getString("next-page-hover");
        previousPageHover = configuration.getString("previous-page-hover");
    }

    /**
     * Returns the first page that the players have access to starting with the inputed page.
     * @param page Page to start at.
     * @return The first valid page.
     */
    private static int getNextValidPage(int page) {
        if (page > 16)
            page = 1;
        if (page == 4 && !bountyWhitelistEnabled)
            page++;
        if (page == 5 && (!bountyWhitelistEnabled || !variableWhitelist))
            page++;
        if (page == 6 && (!bountyWhitelistEnabled || variableWhitelist))
            page++;
        if (page == 8 && !buyBack)
            page++;
        if (page == 9 && Immunity.immunityType == Immunity.ImmunityType.DISABLE)
            page++;
        if (page == 10 && Immunity.immunityType != Immunity.ImmunityType.PERMANENT)
            page++;
        if (page == 11 && Immunity.immunityType != Immunity.ImmunityType.SCALING)
            page++;
        if (page == 12 && Immunity.immunityType != Immunity.ImmunityType.TIME)
            page++;
        if (page == 13 && Immunity.immunityType == Immunity.ImmunityType.DISABLE)
            page++;
        if (page == 14 && !BountyTracker.isEnabled())
            page++;
        if (page == 15 && !craftPoster)
            page++;
        return page;
    }

    private static void sendPage(@NotNull CommandSender sender, int page) {
        if (pages.size() < page)
            return;
        Player parser = sender instanceof Player player ? player : null;
        List<TextComponent[]> text = new ArrayList<>();
        int maxTextLength = 0;
        int firstLineLength = 0;
        List<String> get = pages.get(page - 1);
        for (int i = 0; i < get.size(); i++) {
            String line = get.get(i);
            TextComponent[] components = parseLine(line, parser);
            int lineLength = Arrays.stream(components).mapToInt(component -> ChatColor.stripColor(component.getText()).length()).sum();
            if (lineLength > maxTextLength)
                maxTextLength = lineLength;
            text.add(components);
            if (i == 0)
                firstLineLength = lineLength;
        }
        if (maxTextLength > 0) {
            String prefix = parse(getPrefix(), parser);
            sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + " ".repeat(maxTextLength));
            int spacerLength = (maxTextLength - firstLineLength) / 2;
            TextComponent[] textComponents = new TextComponent[text.get(0).length + 1];
            System.arraycopy(text.get(0), 0, textComponents, 1, text.get(0).length);
            textComponents[0] = new TextComponent(" ".repeat(spacerLength));
            sender.spigot().sendMessage(textComponents);
            for (int i = 1; i < text.size(); i++) {
                sender.spigot().sendMessage(text.get(i));
            }

            sendPageMessage(sender, page, maxTextLength);
        }

    }

    private static TextComponent[] parseLine(String text, @Nullable OfflinePlayer parser) {
        List<TextComponent> baseComponents = new ArrayList<>();
        StringBuilder builder = new StringBuilder(text);
        final String RUN_COMMAND_PREFIX = "{run_command=";
        while (builder.indexOf(RUN_COMMAND_PREFIX) >= 0 && builder.indexOf("}", builder.indexOf(RUN_COMMAND_PREFIX)) >= 0) {
            String before = parse(builder.substring(0, builder.indexOf(RUN_COMMAND_PREFIX)), parser);
            String lastColors = ChatColor.getLastColors(before);
            baseComponents.add(new TextComponent(before));
            builder.delete(0, builder.indexOf(RUN_COMMAND_PREFIX));
            String commandText = builder.substring(13, builder.indexOf("}"));
            TextComponent command = new TextComponent(parse(lastColors + commandText, parser));

            command.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(runCommandHover, parser))));
            command.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandText));
            baseComponents.add(command);
            builder.delete(0, builder.indexOf("}") + 1);
        }

        final String SUGGEST_COMMAND_PREFIX = "{suggest_command=";
        while (builder.indexOf(SUGGEST_COMMAND_PREFIX) >= 0 && builder.indexOf("}", builder.indexOf(SUGGEST_COMMAND_PREFIX)) >= 0) {
            String before = parse(builder.substring(0, builder.indexOf(SUGGEST_COMMAND_PREFIX)), parser);
            String lastColors = ChatColor.getLastColors(before);
            baseComponents.add(new TextComponent(before));
            builder.delete(0, builder.indexOf(SUGGEST_COMMAND_PREFIX));
            String commandText = builder.substring(17, builder.indexOf("}"));
            TextComponent command = new TextComponent(parse(lastColors + commandText, parser));
            // don't send parenthesis options in command
            if (commandText.contains("("))
                commandText = commandText.substring(0, commandText.indexOf("("));
            command.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(suggestCommandHover, parser))));
            command.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandText));
            baseComponents.add(command);
            builder.delete(0, builder.indexOf("}") + 1);
        }
        baseComponents.add(new TextComponent(parse(builder.toString(), parser)));
        return baseComponents.toArray(new TextComponent[0]);
    }

    public static void onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        int page = 1;
        if (args.length > 1)
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                // not a valid page
            }
        page = getNextValidPage(page);
        sendPage(sender, page);

    }

    private static void sendPageMessage(CommandSender sender, int page, int maxLength) {
        int maxPage = 16;
        TextComponent space = new TextComponent("        ");
        TextComponent extraSpace = new TextComponent(" ".repeat(Math.max(maxLength / 2 - 13, 3)));
        TextComponent back = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋘⋘⋘");
        TextComponent middle = new TextComponent("        " + ChatColor.GRAY + " [" + page + "]        ");
        TextComponent next = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋙⋙⋙");
        back.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(previousPageHover, null))));
        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(nextPageHover, null))));
        back.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " tutorial " + (page - 1)));
        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " tutorial " + (page + 1)));
        BaseComponent[] baseComponents = new BaseComponent[]{extraSpace, back, middle, next};
        if (page == 1)
            baseComponents[1] = space;
        if (page == maxPage)
            baseComponents[3] = space;
        sender.spigot().sendMessage(baseComponents);
    }
}

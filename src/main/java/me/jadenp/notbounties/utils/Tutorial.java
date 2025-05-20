package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.map.BountyMap;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntPredicate;


import static me.jadenp.notbounties.features.LanguageOptions.*;


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
     * Returns the first page that the players have access to starting with the inputted page.
     * @param page Page to start at.
     * @return The first valid page.
     */
    private static int getNextValidPage(int page, int step) {
        if (page < 0)
            page = 0;
        while (isInvalidPage(page))
            page += step;
        return page;
    }

    /**
     * Gets the page that the viewer sees.
     * @param page The page number with respect to all pages.
     * @return The page that the viewer sees.
     */
    private static int getViewerPage(int page) {
        int viewerPage = 1;
        int pageTracker = 1;
        while (pageTracker < page) {
            pageTracker = getNextValidPage(pageTracker + 1, 1);
            viewerPage++;
        }
        return viewerPage;
    }

    private static final Map<Integer, IntPredicate> PAGE_VALIDATORS = new HashMap<>();

    static {
        // page, condition in which the page should be skipped
        PAGE_VALIDATORS.put(4, page -> !Whitelist.isEnabled());
        PAGE_VALIDATORS.put(5, page -> !Whitelist.isEnabled() || !Whitelist.isVariableWhitelist());
        PAGE_VALIDATORS.put(6, page -> !Whitelist.isEnabled() || Whitelist.isVariableWhitelist());
        PAGE_VALIDATORS.put(8, page -> !ConfigOptions.getMoney().isBuyOwn());
        PAGE_VALIDATORS.put(9, page -> ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.DISABLE);
        PAGE_VALIDATORS.put(10, page -> ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.PERMANENT);
        PAGE_VALIDATORS.put(11, page -> ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.SCALING);
        PAGE_VALIDATORS.put(12, page -> ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.TIME);
        PAGE_VALIDATORS.put(13, page -> ImmunityManager.getImmunityType() == ImmunityManager.ImmunityType.DISABLE);
        PAGE_VALIDATORS.put(14, page -> !BountyTracker.isEnabled());
        PAGE_VALIDATORS.put(15, page -> !BountyMap.isCraftPoster());
    }

    private static boolean isInvalidPage(int page) {
        IntPredicate validator = PAGE_VALIDATORS.get(page);
        return validator != null && validator.test(page);
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
        String prefix = parse(getPrefix(), parser);
        sender.sendMessage(prefix + ChatColor.GRAY + ChatColor.STRIKETHROUGH + " ".repeat(Math.max((int) (maxTextLength * 1.5), 15)));
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
            TextComponent command = (TextComponent) TextComponent.fromLegacy(parse(lastColors + commandText, parser));

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
            TextComponent command = (TextComponent) TextComponent.fromLegacy(parse(lastColors + commandText, parser));
            // don't send parenthesis options in command
            if (commandText.contains("("))
                commandText = commandText.substring(0, commandText.indexOf("("));
            command.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(suggestCommandHover, parser))));
            command.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandText));
            baseComponents.add(command);
            builder.delete(0, builder.indexOf("}") + 1);
        }
        baseComponents.add((TextComponent) TextComponent.fromLegacy(parse(builder.toString(), parser)));
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
        page = getNextValidPage(page, 1);
        sendPage(sender, page);

    }

    private static void sendPageMessage(CommandSender sender, int page, int maxLength) {
        int maxPage = 16;
        TextComponent space = new TextComponent("        ");
        TextComponent extraSpace = new TextComponent(" ".repeat(Math.max(maxLength / 2 - 13, 3)));
        TextComponent back = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋘⋘⋘");
        TextComponent middle = new TextComponent("        " + ChatColor.GRAY + " [" + getViewerPage(page) + "]        ");
        TextComponent next = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "⋙⋙⋙");
        back.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(previousPageHover, null))));
        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(parse(nextPageHover, null))));
        int nextPage = getNextValidPage(page + 1, 1);
        int lastPage = getNextValidPage(page - 1, -1);
        back.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " tutorial " + lastPage));
        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " tutorial " + nextPage));
        BaseComponent[] baseComponents = new BaseComponent[]{extraSpace, back, middle, next};
        if (lastPage == 0)
            baseComponents[1] = space;
        if (nextPage > maxPage)
            baseComponents[3] = space;
        sender.spigot().sendMessage(baseComponents);
    }
}

package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.IntPredicate;


import static me.jadenp.notbounties.features.LanguageOptions.*;


public class Tutorial {

    private Tutorial(){}

    private static final List<List<String>> pages = new ArrayList<>(16);
    private static String runCommandHover;
    private static String suggestCommandHover;

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
        NotBounties.debugMessage("Sending Tutorial Page " + page + " to " + sender.getName(), false);
        if (pages.size() < page)
            return;
        Player parser = sender instanceof Player player ? player : null;
        List<String> get = pages.get(page - 1);

        Messages.parse(getMessage("help.title"), MessageContext.builder().receiver(parser).withPrefix(false).build()).thenAccept(str -> {
            List<CompletableFuture<TextComponent[]>> lines = get.stream().map(line -> parseLine(line, parser)).toList();
            CompletableFuture.allOf(lines.toArray(new CompletableFuture[0])).thenAccept(v -> NotBounties.getServerImplementation().global().run(() -> {
                sender.sendMessage(str);
                for (CompletableFuture<TextComponent[]> line : lines) {
                    sender.spigot().sendMessage(line.join());
                }
                sendPageLine(sender, page);
            }));
        });
    }

    private static CompletableFuture<TextComponent[]> parseLine(String text, @Nullable OfflinePlayer parser) {
        return CompletableFuture.supplyAsync(() -> {
            List<TextComponent> baseComponents = new ArrayList<>();
            StringBuilder builder = new StringBuilder(text);
            final String RUN_COMMAND_PREFIX = "{run_command=";
            while (builder.indexOf(RUN_COMMAND_PREFIX) >= 0 && builder.indexOf("}", builder.indexOf(RUN_COMMAND_PREFIX)) >= 0) {
                String before = Messages.parse(builder.substring(0, builder.indexOf(RUN_COMMAND_PREFIX)), MessageContext.builder().receiver(parser).withPrefix(false).build()).join();
                String lastColors = ChatColor.getLastColors(before);
                baseComponents.add(Messages.getTextComponent(before, MessageContext.builder().receiver(parser).withPrefix(false).build()).join());
                builder.delete(0, builder.indexOf(RUN_COMMAND_PREFIX));
                String commandText = builder.substring(13, builder.indexOf("}"));
                TextComponent command = Messages.getTextComponent(lastColors + commandText, MessageContext.builder().receiver(parser).withPrefix(false).build()).join();

                command.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Messages.parse(runCommandHover, MessageContext.builder().receiver(parser).withPrefix(false).build()).join())));
                command.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandText));
                baseComponents.add(command);
                builder.delete(0, builder.indexOf("}") + 1);
            }

            final String SUGGEST_COMMAND_PREFIX = "{suggest_command=";
            while (builder.indexOf(SUGGEST_COMMAND_PREFIX) >= 0 && builder.indexOf("}", builder.indexOf(SUGGEST_COMMAND_PREFIX)) >= 0) {
                String before = Messages.parse(builder.substring(0, builder.indexOf(SUGGEST_COMMAND_PREFIX)), MessageContext.builder().receiver(parser).withPrefix(false).build()).join();
                String lastColors = ChatColor.getLastColors(before);
                baseComponents.add(Messages.getTextComponent(before, MessageContext.builder().receiver(parser).withPrefix(false).build()).join());
                builder.delete(0, builder.indexOf(SUGGEST_COMMAND_PREFIX));
                String commandText = builder.substring(17, builder.indexOf("}"));
                TextComponent command = Messages.getTextComponent(lastColors + commandText, MessageContext.builder().receiver(parser).withPrefix(false).build()).join();
                // don't send parenthesis options in command
                if (commandText.contains("("))
                    commandText = commandText.substring(0, commandText.indexOf("("));
                command.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Messages.parse(suggestCommandHover, MessageContext.builder().receiver(parser).withPrefix(false).build()).join())));
                command.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandText));
                baseComponents.add(command);
                builder.delete(0, builder.indexOf("}") + 1);
            }
            baseComponents.add(Messages.getTextComponent(builder.toString(), MessageContext.builder().receiver(parser).withPrefix(false).build()).join());
            return baseComponents.toArray(new TextComponent[0]);
        });

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

    private static void sendPageLine(CommandSender sender, int currentPage) {
        Player parser = sender instanceof Player player ? player : null;
        int previousPage = getNextValidPage(currentPage - 1, -1);
        int nextPage = getNextValidPage(currentPage + 1, 1);


        sendUnifiedPageLine(sender, getViewerPage(currentPage), parser, previousPage, nextPage, "tutorial", 17);
    }

    public static void sendUnifiedPageLine(CommandSender sender, int currentPage, Player parser, int previousPage, int nextPage, String changePageCommand, int maxPage) {
        String footer = LanguageOptions.getMessage("help.footer");
        String before = "";
        String middle = "";
        if (footer.contains("{prev}"))
            before = footer.substring(0, footer.indexOf("{prev}"));
        if (footer.contains("{next}"))
            middle = footer.substring(before.length() + 6, footer.indexOf("{next}"));
        String after = footer.substring(before.length() + middle.length() + 12).replace("{page}", currentPage + "").replace("{page}", currentPage + "");

        MessageContext context = MessageContext.builder().receiver(parser).build();

        CompletableFuture<TextComponent> beforeFuture = Messages.getTextComponent(before.replace("{page}", String.valueOf(currentPage)), context);

        CompletableFuture<TextComponent> middleFuture = Messages.getTextComponent(middle.replace("{page}", String.valueOf(currentPage)), context);

        CompletableFuture<TextComponent> afterFuture = Messages.getTextComponent(after, context);

        CompletableFuture<TextComponent> prevFuture;
        CompletableFuture<String> prevHoverFuture;

        if (previousPage <= 0) {
            prevHoverFuture = null;
            prevFuture = Messages.getTextComponent(LanguageOptions.getMessage("help.prev-deny"), context);
        } else {
            prevFuture = Messages.getTextComponent(LanguageOptions.getMessage("help.prev-text"), context);

            prevHoverFuture = Messages.parse(LanguageOptions.getMessage("help.previous-page"), MessageContext.builder().build());
        }

        CompletableFuture<TextComponent> nextFuture;
        CompletableFuture<String> nextHoverFuture;

        if (nextPage >= maxPage || nextPage <= previousPage) {
            nextHoverFuture = null;
            nextFuture = Messages.getTextComponent(LanguageOptions.getMessage("help.next-deny"), context);
        } else {
            nextFuture = Messages.getTextComponent(LanguageOptions.getMessage("help.next-text"), context);

            nextHoverFuture = Messages.parse(LanguageOptions.getMessage("help.next-page"), MessageContext.builder().build());
        }

        List<CompletableFuture<?>> futures = new ArrayList<>();

        futures.add(beforeFuture);
        futures.add(middleFuture);
        futures.add(afterFuture);
        futures.add(prevFuture);
        futures.add(nextFuture);

        if (prevHoverFuture != null)
            futures.add(prevHoverFuture);

        if (nextHoverFuture != null)
            futures.add(nextHoverFuture);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            TextComponent prevComponent = prevFuture.join();
            TextComponent nextComponent = nextFuture.join();

            if (prevHoverFuture != null) {
                prevComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(prevHoverFuture.join())));

                prevComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().getFirst() + " " + changePageCommand + " " + previousPage));
            }

            if (nextHoverFuture != null) {
                nextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(nextHoverFuture.join())));

                nextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().getFirst() + " " + changePageCommand + " " + nextPage));
            }

            BaseComponent[] baseComponents = new BaseComponent[]{
                    beforeFuture.join(),
                    prevComponent,
                    middleFuture.join(),
                    nextComponent,
                    afterFuture.join()
            };

            NotBounties.getServerImplementation().global().run(() -> sender.spigot().sendMessage(baseComponents));

        });
    }
}

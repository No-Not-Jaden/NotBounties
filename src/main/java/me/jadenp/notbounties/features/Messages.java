package me.jadenp.notbounties.features;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.player_data.ImpersistentPlayerData;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.integrations.external_api.PlaceholderAPIClass;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.PlayerGUInfo;
import me.jadenp.notbounties.ui.gui.display_items.PlayerItem;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.LoggedPlayers;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime.formatTime;

public class Messages {

    public static void send(Player player, String message, MessageContext context) {
        parse(message, context).thenAccept(parsed -> NotBounties.getServerImplementation().entity(player).run(() -> player.sendMessage(parsed)));
    }

    public static void send(CommandSender sender, String message, MessageContext context) {
        parse(message, context).thenAccept(parsed -> {
            if (sender instanceof Player player) {
                NotBounties.getServerImplementation().entity(player).run(() -> player.sendMessage(parsed));
            } else {
                NotBounties.getServerImplementation().global().run(() -> sender.sendMessage(parsed));
            }
        });
    }

    @FunctionalInterface
    public interface ExcludePlayersOperation {
        boolean isExcluded(UUID uuid);
    }

    /**
     * Broadcast a message to the server.
     *
     * @param message   Message to broadcast.
     * @param operation Players to exclude from the broadcast.
     */
    public static void broadcastMessage(String message, MessageContext context, ExcludePlayersOperation operation) {
        parse(message, context).thenAccept(parsedMessage -> NotBounties.getServerImplementation().global().run(() -> {
            Bukkit.getConsoleSender().sendMessage(parsedMessage);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!operation.isExcluded(p.getUniqueId())) {
                    DataManager.getPlayerDataAsync(p.getUniqueId()).thenAccept(playerData -> {
                        if (playerData.getBroadcastSettings() != PlayerData.BroadcastSettings.DISABLE) {
                            NotBounties.getServerImplementation().entity(p).run(() -> p.sendMessage(parsedMessage));
                        }
                    });
                }
            }
        }));

    }

    public static CompletableFuture<String> parse(String message, MessageContext context) {
        if (message == null) {
            return CompletableFuture.completedFuture("");
        }

        return CompletableFuture.supplyAsync(() -> {


        MessageContext safeContext = context == null ? MessageContext.builder().build() : context;
        String parsed = safeContext.isAddPrefix() ? LanguageOptions.getMessage("prefix") + message : message;

        if (safeContext.getTime() != null && safeContext.getTimeFormat() != null && parsed.contains("{time}")) {
            parsed = parsed.replace("{time}", formatTime(safeContext.getTime(), safeContext.getTimeFormat(), getPlayer(safeContext.getReceiver())));
        }

        if (safeContext.getAmount() != null) {
            parsed = parsed.replace("{amount}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(safeContext.getAmount()) + NumberFormatting.getCurrencySuffix())
                    .replace("{amount_plain}", NumberFormatting.formatNumber(safeContext.getAmount()));
        }

        if (safeContext.getBountyAmount() != null) {
            parsed = parsed.replace("{bounty}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(safeContext.getBountyAmount()) + NumberFormatting.getCurrencySuffix())
                    .replace("{bounty_plain}", NumberFormatting.formatNumber(safeContext.getBountyAmount()));
        }

        if (safeContext.getReceiver() != null) {
            parsed = parseReceiver(parsed, safeContext.getReceiver());
            parsed = parseBounty(parsed, safeContext.getReceiver().getUniqueId()).join();
        }


        // parse for players
        if (safeContext.getPlayer() != null) {
            parsed = parsePlayerName(
                    parsed,
                    LoggedPlayers.getPlayerName(safeContext.getPlayer()),
                    LoggedPlayers.getDisplayName(safeContext.getPlayer()),
                    safeContext.getReceiver() != null ? LoggedPlayers.getPlayerName(safeContext.getReceiver()) : null,
                    safeContext.getReceiver() != null ? LoggedPlayers.getDisplayName(safeContext.getReceiver()) : null,
                    safeContext.isPlayerPrefix(), safeContext.isPlayerSuffix()
            );
        } else {
            parsed = parsePlayerName(
                    parsed,
                    safeContext.getPlayerUUID() != null ? LoggedPlayers.getPlayerName(safeContext.getPlayerUUID()) : null,
                    safeContext.getPlayerUUID() != null ? LoggedPlayers.getDisplayName(safeContext.getPlayerUUID()) : null,
                    safeContext.getReceiver() != null ? LoggedPlayers.getPlayerName(safeContext.getReceiver()) : null,
                    safeContext.getReceiver() != null ? LoggedPlayers.getDisplayName(safeContext.getReceiver()) : null,
                    safeContext.isPlayerPrefix(), safeContext.isPlayerSuffix()
            );
        }

        parsed = parsePlayerData(parsed, safeContext.getPlayerUUID()).join();


        for (var entry : safeContext.getPlaceholders().entrySet()) {
            parsed = parsed.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        // papi parse
        if (ConfigOptions.getIntegrations().isPapiEnabled()) {
            parsed = new PlaceholderAPIClass().parse(safeContext.getReceiver(), parsed);
        }

        return LanguageOptions.color(parsed);
        });
    }

    private static String parseReceiver(String str, @NotNull OfflinePlayer receiver) {
        if (str.contains("{balance}"))
            str = str.replace("{balance}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(NumberFormatting.getBalance(receiver)) + NumberFormatting.getCurrencySuffix()));
        str = str.replace("{sort_type_name}", GUI.getActiveSortTypeName(receiver.getUniqueId()))
                .replace("{sort_type}", GUI.getActiveSortType(receiver.getUniqueId()) + "")
                .replace("{immunity}", NumberFormatting.formatNumber(ImmunityManager.getImmunity(receiver.getUniqueId()).join()));

        // {sort_type_(gui)} turns into the name of the sort type in the GUI
        while (str.contains("{sort_type_") && str.substring(str.indexOf("{sort_type_")).contains("}")) {
            String stringValue = str.substring(str.indexOf("{sort_type_") + 11, str.indexOf("{sort_type_") + str.substring(str.indexOf("{sort_type_")).indexOf("}"));
            str = str.replace("{sort_type_" + stringValue + "}", GUI.parseSortType(stringValue, ImpersistentPlayerData.get(receiver.getUniqueId()).getGUISortType(stringValue)));
        }

        // parsing for GUI
        if (receiver.isOnline() && GUI.playerInfo.containsKey(receiver.getUniqueId())) {
            PlayerGUInfo info = GUI.playerInfo.get(receiver.getUniqueId());
            str = str.replace("{page}", info.page() + "")
                    .replace("{page_max}", info.maxPage() + "")
                    .replace("{gui}", info.guiType());

            // check for {player<x>}
            while (str.contains("{player") && str.substring(str.indexOf("{player")).contains("}")) {
                String replacement = "";
                String slotString = str.substring(str.indexOf("{player") + 7, str.substring(str.indexOf("{player")).indexOf("}") + str.substring(0, str.indexOf("{player")).length());
                try {
                    int slot = Integer.parseInt(slotString);
                    if (info.displayItems().size() > slot-1 && info.displayItems().get(slot-1) instanceof PlayerItem playerItem) {
                        replacement = playerItem.getName();
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Error getting player in command: \n" + str);
                }
                str = str.replace(("{player" + slotString + "}"), (replacement));
            }
        }

        return str;
    }

    private static String parseConstants(String str) {
        return str.replace("{next_challenges}", formatTime(ChallengeManager.getNextChallengeChange() - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE))
                .replace("{min_bounty}", (NumberFormatting.getValue(ConfigOptions.getMoney().getMinBounty())))
                .replace("{c_prefix}", (NumberFormatting.getCurrencyPrefix()))
                .replace("{c_suffix}", (NumberFormatting.getCurrencySuffix()))
                .replace("{whitelist_cost}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(Whitelist.getCost()) + NumberFormatting.getCurrencySuffix())
                .replace("{tax}", (NumberFormatting.formatNumber(ConfigOptions.getMoney().getBountyTax() * 100)))
                .replace("{buy_back_interest}", (NumberFormatting.formatNumber(ConfigOptions.getMoney().getBuyOwnCostMultiply() * 100)))
                .replace("{permanent_cost}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(ImmunityManager.getPermanentCost()) + NumberFormatting.getCurrencySuffix()))
                .replace("{scaling_ratio}", (NumberFormatting.formatNumber(ImmunityManager.getScalingRatio())))
                .replace("{time_immunity}", (formatTime((long) (ImmunityManager.getTime() * 1000L), LocalTime.TimeFormat.RELATIVE)));
    }

    private static CompletableFuture<String> parsePlayerData(String str, UUID playerUUID) {
        final String temp = str;
        return DataManager.getPlayerDataAsync(playerUUID).thenApply(playerData -> {
            String parsed = temp;
            Whitelist whitelist = playerData.getWhitelist();
            String mode = whitelist.isBlacklist() ? "Blacklist" : "Whitelist";
            String notification = playerData.getBroadcastSettings().toString();
            parsed = parsed.replace("{whitelist}", (whitelist.toString()))
                    .replace("{mode}", mode).replace("{mode_raw}", (Boolean.toString(!whitelist.isBlacklist())))
                    .replace("{notification}", notification);
            // {whitelist2} turns into the name of the second player in the receiver's whitelist
            while (parsed.contains("{whitelist") && parsed.substring(parsed.indexOf("{whitelist")).contains("}")) {
                int num;
                String stringValue = parsed.substring(parsed.indexOf("{whitelist") + 10, parsed.indexOf("{whitelist") + parsed.substring(parsed.indexOf("{whitelist")).indexOf("}"));
                try {
                    num = Integer.parseInt(stringValue);
                } catch (NumberFormatException e) {
                    parsed = parsed.replace("{whitelist" + stringValue + "}", "<Error>");
                    continue;
                }
                if (num < 1)
                    num = 1;
                if (whitelist.getList().size() > num)
                    parsed = parsed.replace("{whitelist" + stringValue + "}", "");
                else
                    parsed = parsed.replace("{whitelist" + stringValue + "}", LoggedPlayers.getPlayerName(whitelist.getList().last()));
            }
            return parsed;
        });
    }

    private static CompletableFuture<String> parseBounty(String str, UUID bountyUUID) {
        final String temp = str;
        return DataManager.getBountyAsync(bountyUUID).thenApply(bounty -> {
            String parsed = temp;
            if (bounty != null) {
                parsed = parsed.replace("{min_expire}", (formatTime(BountyExpire.getLowestExpireTime(bounty), LocalTime.TimeFormat.RELATIVE)))
                        .replace("{max_expire}", (formatTime(BountyExpire.getHighestExpireTime(bounty), LocalTime.TimeFormat.RELATIVE)))
                        .replace("{bounty}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(bounty.getTotalDisplayBounty()) + NumberFormatting.getCurrencySuffix())
                        .replace("{bounty_value}", NumberFormatting.getValue(bounty.getTotalDisplayBounty()) );
            } else {
                parsed = str.replace("{min_expire}", "")
                        .replace("{max_expire}", "");
            }
            return parsed;
        });
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private static String parsePlayerName(String str, String playerName, String playerDisplayName, String receiverName, String receiverDisplayName, boolean prefix, boolean suffix) {
        String nameFallback = firstNonNull(
                playerName,
                receiverName,
                playerDisplayName,
                receiverDisplayName
        );

        if (nameFallback == null) {
            return str;
        }

        String displayFallback = firstNonNull(
                playerName,
                receiverName,
                playerDisplayName,
                receiverDisplayName
        );

        playerName = firstNonNull(playerName, nameFallback);
        receiverName = firstNonNull(receiverName, nameFallback);

        playerDisplayName = firstNonNull(playerDisplayName, displayFallback);
        receiverDisplayName = firstNonNull(receiverDisplayName, displayFallback);

        if (playerName == null || receiverName == null || playerDisplayName == null || receiverDisplayName == null) {
            // should never happen, but just in case
            return str;
        }

        if (prefix) {
            playerName = LanguageOptions.getMessage("player-prefix") + playerName;
            playerDisplayName = LanguageOptions.getMessage("player-prefix") + playerDisplayName;
            receiverName = LanguageOptions.getMessage("player-prefix") + receiverName;
            receiverDisplayName = LanguageOptions.getMessage("player-prefix") + receiverDisplayName;
        }
        if (suffix) {
            playerName = playerName + LanguageOptions.getMessage("player-suffix");
            playerDisplayName = playerDisplayName + LanguageOptions.getMessage("player-suffix");
            receiverName = receiverName + LanguageOptions.getMessage("player-suffix");
            receiverDisplayName = receiverDisplayName + LanguageOptions.getMessage("player-suffix");
        }

        return str.replace("{player}", playerName).replace("{player_displayname}", playerDisplayName)
                .replace("{viewer}", playerName).replace("{viewer_displayname}", playerDisplayName)
                .replace("{receiver}", receiverName).replace("{receiver_displayname}", receiverDisplayName);

    }

    private static org.bukkit.entity.Player getPlayer(OfflinePlayer offlinePlayer) {
        return offlinePlayer != null ? offlinePlayer.getPlayer() : null;
    }
}

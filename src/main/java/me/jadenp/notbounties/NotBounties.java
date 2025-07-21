package me.jadenp.notbounties;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.features.settings.auto_bounties.*;
import me.jadenp.notbounties.features.settings.databases.AsyncDatabaseWrapper;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyDatabase;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import me.jadenp.notbounties.features.settings.display.BountyHunt;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.BountyClaimRequirements;
import me.jadenp.notbounties.features.settings.integrations.Integrations;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.Commands;
import me.jadenp.notbounties.ui.Events;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.features.settings.display.map.BountyBoard;
import me.jadenp.notbounties.features.settings.display.map.BountyMap;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.features.challenges.ChallengeListener;
import me.jadenp.notbounties.features.challenges.ChallengeManager;
import me.jadenp.notbounties.features.*;
import me.jadenp.notbounties.features.webhook.WebhookOptions;
import me.jadenp.notbounties.features.settings.integrations.external_api.*;
import me.jadenp.notbounties.features.settings.integrations.external_api.bedrock.FloodGateClass;
import me.jadenp.notbounties.features.settings.integrations.external_api.bedrock.GeyserMCClass;
import me.jadenp.notbounties.features.settings.integrations.external_api.worldguard.WorldGuardClass;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.*;


import static me.jadenp.notbounties.features.LanguageOptions.*;

/**
 * Go through wiki for outdated materials
 * Update front page
 * Separate messages sent to the proxy if they are too big. (Velocity version too)
 * Team bounties
 * Bungee support.
 * Better SQL and Redis config with connection string and address options to replace others.
 * Redo vouchers with persistent data, give items, & reward delay
 * Redis Pub Sub messages for player data storage. - proxy messaging too
 * database message table with server IDs
 * fast database option to use directly instead of an update interval
 * Multiple proxy databases
 * 1.21.6 dialog
 */
public final class NotBounties extends JavaPlugin {

    private static NotBounties instance;
    private static String latestVersion;
    private static boolean updateAvailable = false;

    public static final String SESSION_KEY = UUID.randomUUID().toString();
    private static NamespacedKey namespacedKey;
    private static int serverVersion = 20;
    private static int serverSubVersion = 0;
    private static boolean debug = false;
    private static boolean paused = false;
    private static Events events;
    private boolean started = false;
    private static ServerImplementation serverImplementation;

    public static NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public static void setNamespacedKey(NamespacedKey namespacedKey) {
        NotBounties.namespacedKey = namespacedKey;
    }

    private static void setInstance(NotBounties instance) {
        NotBounties.instance = instance;
    }

    public static void setEvents(Events events) {
        NotBounties.events = events;
    }

    @Override
    public void onLoad() {
        setInstance(this);
        Integrations.onLoad(this);
    }


    @Override
    public void onEnable() {
        setServerImplementation(new FoliaCompatibility(this).getServerImplementation());
        Random random = new Random(System.currentTimeMillis());
        // Plugin startup logic
        if (random.nextInt(10) == 3) {
            // 10% chance to receive this Easter egg
            String display = """
                                      \s
                    ████████████████████████████████████████████████████████████████████
                    █▄─▀█▄─▄█─▄▄─█─▄─▄─█▄─▄─▀█─▄▄─█▄─██─▄█▄─▀█▄─▄█─▄─▄─█▄─▄█▄─▄▄─█─▄▄▄▄█
                    ██─█▄▀─██─██─███─████─▄─▀█─██─██─██─███─█▄▀─████─████─███─▄█▀█▄▄▄▄─█
                    ▀▄▄▄▀▀▄▄▀▄▄▄▄▀▀▄▄▄▀▀▄▄▄▄▀▀▄▄▄▄▀▀▄▄▄▄▀▀▄▄▄▀▀▄▄▀▀▄▄▄▀▀▄▄▄▀▄▄▄▄▄▀▄▄▄▄▄▀""";
            getLogger().info(display);
        }

        // load commands and events
        Commands commands = new Commands();
        Objects.requireNonNull(this.getCommand("notbounties")).setExecutor(commands);
        Objects.requireNonNull(this.getCommand("notbountiesadmin")).setExecutor(commands);
        setEvents(new Events());
        Bukkit.getServer().getPluginManager().registerEvents(events, this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new CurrencySetup(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyMap(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PVPRestrictions(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new WebhookOptions(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Prompt(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyTracker(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ChallengeListener(), this);

        BountyMap.initialize(this);
        setNamespacedKey(new NamespacedKey(this, "bounty-entity"));

        readVersion(this);

        Bukkit.getServer().getPluginManager().registerEvents(new RemovePersistentEntitiesEvent(), this);

        try {
            DataManager.loadData(this);
            loadConfig();
            if (ChallengeManager.isEnabled()) {
                ChallengeManager.readChallengeData();
            }

            ImmunityManager.loadPlayerData();
        } catch (IOException e) {
            getLogger().severe("[NotBounties] Failed to read player data!");
            getLogger().severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stack -> getLogger().severe("       at " + stack.toString()));
        }

        if (ConfigOptions.isSendBStats()) {
            int pluginId = 20776;
            Metrics metrics = new Metrics(this, pluginId);
            metrics.addCustomChart(new Metrics.SingleLineChart("active_bounties", () -> BountyManager.getAllBounties(-1).size()));
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BountyExpansion().register();
        }

        // load skins for bounties
        for (Bounty bounty : BountyManager.getAllBounties(-1))
            SkinManager.saveSkin(bounty.getUUID());

        // force login players that are already on the server - this will happen if the plugin is loaded without a restart
        for (Player player : Bukkit.getOnlinePlayers()) {
            Events.login(player);
        }

        if (ConfigOptions.getIntegrations().isWorldGuardEnabled())
            WorldGuardClass.registerHandlers();

        checkForUpdate(this);

        // check permission immunity every 5 mins
        // sync player data if there is only 1 person online (for proxy)
        getServerImplementation().global().runAtFixedRate(() ->
        {
            ImmunityManager.checkOnlinePermissionImmunity();
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            if (players.size() == 1 && ProxyDatabase.isEnabled() && ProxyDatabase.isDatabaseSynchronization() && ProxyMessaging.hasConnectedBefore()) {
                DataManager.syncPlayerData(players.iterator().next().getUniqueId(), null);
            }

        }, 3611, 3600);

        // make bounty tracker work & big bounty particle & time immunity
        getServerImplementation().global().runAtFixedRate(() -> {
            if (paused)
                return;
            // update bounty tracker
            BountyTracker.update();

            // big bounty particle
            BigBounty.displayParticle();
        }, 100, 40);
        getServerImplementation().global().runAtFixedRate(() -> {
            if (paused)
                return;

            ImmunityManager.update();
            RandomBounties.update();
            TimedBounties.update();

            PVPRestrictions.checkCombatExpiry();
            ChallengeManager.checkChallengeChange();

            BountyBoard.update();
            BountyHunt.updateBossBars();
        }, 120, 40);
        // auto save bounties & do some ram cleaning
        getServerImplementation().async().runAtFixedRate(() -> {
            if (paused)
                return;
            MurderBounties.cleanPlayerKills();
            SkinManager.removeOldData();
            RemovePersistentEntitiesEvent.checkRemovedEntities();

            try {
                SaveManager.save(this);
            } catch (IOException e) {
                getLogger().severe("Error autosaving saving data!");
                getLogger().severe(e.toString());
            }
        }, ConfigOptions.getAutoSaveInterval() * 60 * 20L + 69, ConfigOptions.getAutoSaveInterval() * 60 * 20L);


        // this needs to be in a 5-minute interval cuz that's the lowest time specified in the config for expiration
        getServerImplementation().async().runAtFixedRate(BountyExpire::removeExpiredBounties, 5 * 60 * 20L + 2007, 5 * 60 * 20L);

        // Check for banned players
        //         * Runs every hour and will check a few players at a time
        //         * Every player will be guaranteed to be checked after 12 hours
        getServerImplementation().global().runAtFixedRate(new Runnable() {
            List<Bounty> bountyListCopy = new ArrayList<>();
            int playersPerRun = 1;
            @Override
            public void run() {
                if (!ConfigOptions.isRemoveBannedPlayers() || paused)
                    return;
                if (bountyListCopy.isEmpty()) {
                    bountyListCopy = BountyManager.getAllBounties(-1);
                    playersPerRun = bountyListCopy.size() / 12 + 1;
                }
                for (int i = 0; i < playersPerRun; i++) {
                    if (i >= bountyListCopy.size())
                        break;
                    Bounty bounty = bountyListCopy.get(i);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(bounty.getUUID());
                    getServerImplementation().async().runNow(() -> {
                        if (NotBounties.isPlayerBanned(player)) {
                            getServerImplementation().global().run(() -> BountyManager.removeBounty(bounty.getUUID()));
                        }
                    });
                }
                if (playersPerRun > 0) {
                    if (bountyListCopy.size() > playersPerRun)
                        bountyListCopy.subList(0, playersPerRun).clear();
                    else
                        bountyListCopy.clear();
                }
            }
        }, 101, 72006);

        // wanted text
        getServerImplementation().global().runAtFixedRate(new Runnable() {
            static final int MAX_UPDATE_TIME = 10;
            int lastUpdateTime = 0; // time it took for the stands to update last
            long lastRunTime = 0;
            @Override
            public void run() {
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    if (lastUpdateTime > MAX_UPDATE_TIME
                        //    (the amount of ms since last update)      (2 x the amount of ms last update took)
                        && System.currentTimeMillis() - lastRunTime < (lastUpdateTime - MAX_UPDATE_TIME) * 2L) {
                        return;
                    }
                    long startTime = System.currentTimeMillis();
                    WantedTags.update();
                    lastUpdateTime = (int) (System.currentTimeMillis() - startTime);
                    if (lastUpdateTime > MAX_UPDATE_TIME) {
                        lastRunTime = System.currentTimeMillis();
                        if (debug)
                            getLogger().info("[NotBountiesDebug] Took " + lastUpdateTime + "ms to update wanted tags. Pausing for a few updates.");
                    }
                }
            }
        }, 20, 1);

        // plugin was enabled successfully
        started = true;
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static NotBounties getInstance() {
        return instance;
    }

    private static void checkForUpdate(Plugin plugin) {
        // update checker
        if (ConfigOptions.getUpdateNotification().equalsIgnoreCase("false"))
            return;
        new UpdateChecker((JavaPlugin) plugin, 104484).getVersion(version -> {
            latestVersion = version;
            if (plugin.getDescription().getVersion().contains("dev")
                    || plugin.getDescription().getVersion().equals(version))
                return;
            // split the numbers
            String[] versionSplit = version.split("\\.");
            String[] currentSplit = plugin.getDescription().getVersion().split("\\.");
            // convert to integers
            int[] versionNumbers = convertStringArrayToInt(versionSplit);
            int[] currentNumbers = convertStringArrayToInt(currentSplit);
            for (int i = 0; i < currentNumbers.length; i++) {
                if (currentNumbers[i] < versionNumbers[i]) {
                    updateAvailable = true;
                    break;
                }
            }
            if (!updateAvailable && currentNumbers.length < versionNumbers.length)
                updateAvailable = true;
            if (updateAvailable && !version.equals(ConfigOptions.getUpdateNotification())) {
                plugin.getLogger().info(ChatColor.stripColor(parse(getMessage("update-notification").replace("{current}", NotBounties.getInstance().getDescription().getVersion()).replace("{latest}", version), null)));
            }
        });

    }

    private static int @NotNull [] convertStringArrayToInt(String[] versionSplit) {
        int[] versionNumbers = new int[versionSplit.length];
        for (int i = 0; i < versionSplit.length; i++) {
            try {
                versionNumbers[i] = Integer.parseInt(versionSplit[i]);
            } catch (NumberFormatException ignored) {
                // not a number
            }
        }
        return versionNumbers;
    }

    private static void readVersion(Plugin plugin) {
        try {
            // get the text version - ex: 1.20.3
            String fullServerVersion = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf("-"));
            fullServerVersion = fullServerVersion.substring(2); // remove the '1.' in the version
            if (fullServerVersion.contains(".")) {
                // get the subversion - ex: 3
                serverSubVersion = Integer.parseInt(fullServerVersion.substring(fullServerVersion.indexOf(".") + 1));
                fullServerVersion = fullServerVersion.substring(0, fullServerVersion.indexOf(".")); // remove the subversion
            }
            serverVersion = Integer.parseInt(fullServerVersion);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            plugin.getLogger().warning("Could not get the server version. Some features may not function properly.");
            serverVersion = 20;
            serverSubVersion = 0;
        }
    }



    public void loadConfig() throws IOException {
        // close gui

        ConfigOptions.reloadOptions(this);

        if (!NotBounties.isPaused()) {
            // check players to display particles
            getServerImplementation().global().runDelayed(BigBounty::refreshParticlePlayers, 40);

        }


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (ConfigOptions.getIntegrations().isMmoLibEnabled())
            MMOLibClass.removeAllModifiers();
        SkinManager.shutdown();
        BountyMap.shutdown();

        // remove bounty entities
        WantedTags.disableWantedTags();

        if (!started) {
            // Plugin failed to start.
            // Returning, so save data isn't overwritten.
            BountyBoard.clearBoard();
            DataManager.shutdown();
            return;
        }

        // close all GUIs
        for (Player player : Bukkit.getOnlinePlayers()) {
            GUI.safeCloseGUI(player, true);
        }
        // save data

        try {
            SaveManager.save(this);
            if (ChallengeManager.isEnabled()) {
                ChallengeManager.saveChallengeData();
            }
        } catch (IOException e) {
            getLogger().severe("Error saving data!");
            getLogger().severe(e.toString());
        }

        BountyBoard.clearBoard();
        DataManager.shutdown();



    }

    private static String status(String name, boolean enabled) {
        return (enabled ? ChatColor.GREEN : ChatColor.RED) + name;
    }

    public void sendDebug(CommandSender sender) {
        sender.sendMessage(parse(getPrefix() + ChatColor.WHITE + "NotBounties debug info:", null));
        long numConnected = DataManager.getDatabases().stream().filter(AsyncDatabaseWrapper::isConnected).count();
        String connected = numConnected > 0 ? ChatColor.GREEN + "" + numConnected : ChatColor.RED + "" + numConnected;
        String numConfigured = ChatColor.WHITE + "" + DataManager.getDatabases().size();
        int bounties = BountyManager.getAllBounties(-1).size();
        sender.sendMessage(ChatColor.GOLD + "Databases > " + ChatColor.YELLOW + "Configured: " + numConfigured
                + ChatColor.YELLOW + " Connected: " + connected);

        sender.sendMessage(ChatColor.GOLD + "General > "
                + ChatColor.YELLOW + "Author: " + ChatColor.GRAY + "Not_Jaden"
                + ChatColor.YELLOW + " Current Plugin Version: " + ChatColor.WHITE + getDescription().getVersion()
                + ChatColor.YELLOW + " Latest Plugin Version: " + ChatColor.WHITE + getLatestVersion()
                + ChatColor.YELLOW + " Server Version: " + ChatColor.WHITE
                + "1." + serverVersion + "." + serverSubVersion
                + ChatColor.YELLOW + " Debug Mode: " + ChatColor.WHITE + debug
                + ChatColor.YELLOW + " Online Mode: " + ChatColor.WHITE + Bukkit.getOnlineMode());

        sender.sendMessage(ChatColor.GOLD + "Stats > " + ChatColor.YELLOW + "Bounties: " + ChatColor.WHITE + bounties
                + ChatColor.YELLOW + " Tracked Bounties: " + ChatColor.WHITE + BountyTracker.getTrackedBounties().size()
                + ChatColor.YELLOW + " Bounty Boards: " + ChatColor.WHITE + BountyBoard.getBountyBoards().size());

        List<String> statuses = List.of(
                status("Vault", NumberFormatting.isVaultEnabled()),
                status("PlaceholderAPI", ConfigOptions.getIntegrations().isPapiEnabled()),
                status("HeadDataBase", ConfigOptions.getIntegrations().isHeadDataBaseEnabled()),
                status("LiteBans", ConfigOptions.getIntegrations().isLiteBansEnabled()),
                status("SkinsRestorer", ConfigOptions.getIntegrations().isSkinsRestorerEnabled()),
                status("BetterTeams", BountyClaimRequirements.isBetterTeamsEnabled()),
                status("TownyAdvanced", BountyClaimRequirements.isTownyAdvancedEnabled()),
                status("Floodgate", ConfigOptions.getIntegrations().isFloodgateEnabled()),
                status("GeyserMC", ConfigOptions.getIntegrations().isGeyserEnabled()),
                status("Kingdoms", BountyClaimRequirements.isKingdomsXEnabled()),
                status("Lands", BountyClaimRequirements.isLandsEnabled()),
                status("WorldGuard", ConfigOptions.getIntegrations().isWorldGuardEnabled()),
                status("SuperiorSkyblock2", BountyClaimRequirements.isSuperiorSkyblockEnabled()),
                status("MMOLib", ConfigOptions.getIntegrations().isMmoLibEnabled()),
                status("SimpleClans", BountyClaimRequirements.isSimpleClansEnabled()),
                status("Factions", BountyClaimRequirements.isFactionsEnabled()),
                status("Duels", ConfigOptions.getIntegrations().isDuelsEnabled()),
                status("PacketEvents", ConfigOptions.getIntegrations().isPacketEventsEnabled())
        );
        String joined = String.join(ChatColor.GRAY + "|", statuses);
        sender.sendMessage(ChatColor.GOLD + "Plugin Hooks > " + ChatColor.GRAY + "[" + joined + ChatColor.GRAY + "]");
        sender.sendMessage(ChatColor.GRAY + "Reloading the plugin will refresh connections.");

        TextComponent discord = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(114, 137, 218))
                + "Support Discord: " + ChatColor.GRAY + ChatColor.UNDERLINE + "https://discord.gg/zEsUzwYEx7");
        discord.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/zEsUzwYEx7"));
        TextComponent spigot = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(240, 149, 45))
                + "Spigot: " + ChatColor.GRAY + ChatColor.UNDERLINE + "https://www.spigotmc.org/resources/notbounties.104484/");
        spigot.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/notbounties.104484/"));
        TextComponent github = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(230, 237, 243))
                + "Github: " + ChatColor.GRAY + ChatColor.UNDERLINE + "https://github.com/No-Not-Jaden/NotBounties");
        github.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/No-Not-Jaden/NotBounties"));
        sender.spigot().sendMessage(discord);
        sender.spigot().sendMessage(spigot);
        sender.spigot().sendMessage(github);
        sender.sendMessage("");
    }



    public static Map<UUID, String> getNetworkPlayers() {
        return DataManager.getNetworkPlayers();
    }

    public static boolean isVanished(Player player) {
        if (ConfigOptions.isHideInvisiblePlayers() && player.isInvisible())
            return true;
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        for (MetadataValue meta : player.getMetadata("vanish")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    /**
     * Returns if the server version is above the specified version
     *
     * @param majorVersion Major version of the server. In 1.20.4, the major version is 20
     * @param subVersion   Subversion of the server. In 1.20.4, the subversion is 4
     * @return True if the current server version is higher than the specified one
     */
    public static boolean isAboveVersion(int majorVersion, int subVersion) {
        return serverVersion > majorVersion || (majorVersion == serverVersion && subVersion < serverSubVersion);
    }

    public static void debugMessage(String message, boolean warning) {
        if (!debug)
            return;
        message = "<Debug> " + message;
        NotBounties notBounties = NotBounties.getInstance();
        if (notBounties.isEnabled()) {
            String finalMessage = message;
            getServerImplementation().global().run(() -> consoleMessage(notBounties, finalMessage, warning));
        } else {
            consoleMessage(notBounties, message, warning);
        }
    }

    public static boolean isPaused() {
        return paused;
    }

    public static void setPaused(boolean paused) {
        if (paused) {
            for (BountyBoard board : BountyBoard.getBountyBoards())
                board.remove();
            // close all GUIs
            for (Player player : Bukkit.getOnlinePlayers()) {
                GUI.safeCloseGUI(player, true);
            }
            // remove wanted tags
            WantedTags.disableWantedTags();
            for (Player player : Bukkit.getOnlinePlayers())
                events.onQuit(new PlayerQuitEvent(player, ""));
            NotBounties.paused = true;
        } else {
            NotBounties.paused = false;
            for (Player player : Bukkit.getOnlinePlayers())
                events.onJoin(new PlayerJoinEvent(player, ""));
        }
    }

    private static void consoleMessage(Plugin plugin, String message, boolean warning) {
        if (warning)
            plugin.getLogger().warning(message);
        else
            plugin.getLogger().info(message);
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            if (ConfigOptions.getIntegrations().isFloodgateEnabled()) {
                FloodGateClass floodGateClass = new FloodGateClass();
                return floodGateClass.isBedrockPlayer(uuid);
            } else if (ConfigOptions.getIntegrations().isGeyserEnabled()) {
                GeyserMCClass geyserMCClass = new GeyserMCClass();
                return geyserMCClass.isBedrockPlayer(uuid);
            }
        }
        String first = uuid.toString().substring(0, 18);
        return first.equals("00000000-0000-0000");
    }

    public static String getXuid(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            if (ConfigOptions.getIntegrations().isFloodgateEnabled()) {
                FloodGateClass floodGateClass = new FloodGateClass();
                if (floodGateClass.isBedrockPlayer(uuid)) {
                    return floodGateClass.getXuid(uuid);
                }
            } else if (ConfigOptions.getIntegrations().isGeyserEnabled()) {
                GeyserMCClass geyserMCClass = new GeyserMCClass();
                if (geyserMCClass.isBedrockPlayer(uuid)) {
                    return geyserMCClass.getXuid(uuid);
                }
            }
        }
        if (isBedrockPlayer(uuid)) {
            String last = uuid.toString().replace("-", "").substring(16);
            try {
                return Long.parseLong(last, 16) + "";
            } catch (NumberFormatException ignored) {
                // Not parsable hex
            }
        }
        return "";
    }

    private static final String ADMIN_PERMISSION = "notbounties.admin";

    public static String getAdminPermission() {
        return ADMIN_PERMISSION;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean debug) {
        NotBounties.debug = debug;
    }

    public static int getServerVersion() {
        return serverVersion;
    }

    public static ServerImplementation getServerImplementation() {
        return serverImplementation;
    }

    private static void setServerImplementation(ServerImplementation serverImplementation) {
        NotBounties.serverImplementation = serverImplementation;
    }

    public static boolean isPlayerBanned(OfflinePlayer player) {
        if (player.isBanned())
            return true;
        try {
            return ConfigOptions.getIntegrations().isLiteBansEnabled() && !new LiteBansClass().isPlayerNotBanned(player.getUniqueId());
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }
}

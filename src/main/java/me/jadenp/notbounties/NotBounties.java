package me.jadenp.notbounties;

import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.Commands;
import me.jadenp.notbounties.ui.Events;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.ui.map.BountyMap;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.utils.challenges.ChallengeManager;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.configuration.autoBounties.MurderBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.RandomBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import me.jadenp.notbounties.utils.configuration.webhook.WebhookOptions;
import me.jadenp.notbounties.utils.externalAPIs.*;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.FloodGateClass;
import me.jadenp.notbounties.utils.externalAPIs.bedrock.GeyserMCClass;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.vaultEnabled;

/**
 * Proxy Messaging
 * Challenges
 * Save to json file instead of yaml
 * Redis
 * Folia
 * Team bounties
 *
 */
public final class NotBounties extends JavaPlugin {

    /**
     * Name (lower case), UUID
     */
    public static final Map<String, UUID> loggedPlayers = new HashMap<>();

    public static List<String> immunePerms = new ArrayList<>();
    public static List<String> autoImmuneMurderPerms = new ArrayList<>();
    public static List<String> autoImmuneRandomPerms = new ArrayList<>();
    public static List<String> autoImmuneTimedPerms = new ArrayList<>();
    public static final List<UUID> disableBroadcast = new ArrayList<>();
    /**
     * Player UUID, Whitelist UUIDs
     */
    public static final Map<UUID, Whitelist> playerWhitelist = new HashMap<>();
    public static final Map<String, Long> repeatBuyCommand = new HashMap<>();
    public static final Map<String, Long> repeatBuyCommand2 = new HashMap<>();


    public static final List<UUID> displayParticle = new ArrayList<>();
    public static final Map<UUID, AboveNameText> wantedText = new HashMap<>();
    private static NotBounties instance;
    private static BukkitTask autoConnectTask = null;
    private boolean firstConnect = true;
    public static boolean latestVersion = true;
    public static final List<BountyBoard> bountyBoards = new ArrayList<>();
    public static final Map<UUID, Integer> boardSetup = new HashMap<>();
    public static long lastBountyBoardUpdate = System.currentTimeMillis();
    public static final String sessionKey = UUID.randomUUID().toString();
    public static NamespacedKey namespacedKey;
    public static int serverVersion = 20;
    public static int serverSubVersion = 0;
    public static boolean debug = false;

    @Override
    public void onLoad() {
        instance = this;
        // register api flags
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null)
            new WorldGuardClass().registerFlags();
        if (getServer().getPluginManager().getPlugin("Lands") != null)
            new LandsClass().registerClaimFlag();
    }


    @Override
    public void onEnable() {
        // Plugin startup logic
        Objects.requireNonNull(this.getCommand("notbounties")).setExecutor(new Commands());
        Bukkit.getServer().getPluginManager().registerEvents(new Events(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new CurrencySetup(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyMap(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PVPRestrictions(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new WebhookOptions(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Prompt(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyTracker(), this);

        this.saveDefaultConfig();


        BountyMap.initialize();
        namespacedKey = new NamespacedKey(this, "bounty-entity");

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
            Bukkit.getLogger().warning("[NotBounties] Could not get the server version. Some features may not function properly.");
            serverVersion = 20;
            serverSubVersion = 0;
        }

        if (serverVersion >= 17)
            Bukkit.getServer().getPluginManager().registerEvents(new RemovePersistentEntitiesEvent(), this);

        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loadBounties();

        if (ChallengeManager.isEnabled()) {
            try {
                ChallengeManager.readChallengeData();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (sendBStats) {
            int pluginId = 20776;
            new Metrics(this, pluginId);
        }


        // clean out posters that aren't supposed to be there
        if (!saveTemplates) {
            BountyMap.cleanPosters();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BountyExpansion().register();
        }


        if (!tryToConnect()) {
            Bukkit.getLogger().info("[NotBounties] Database not connected, using internal storage");
        }

        // load skins for bounties
        for (Bounty bounty : BountyManager.getAllBounties(-1))
            SkinManager.saveSkin(bounty.getUUID());

        // register plugin messaging to a proxy
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bungeecord:main");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "bungeecord:main", new ProxyMessaging());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "notbounties:main");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "notbounties:main", new ProxyMessaging());

        // update checker
        if (updateNotification) {
            new UpdateChecker(this, 104484).getVersion(version -> {
                if (getDescription().getVersion().contains("dev"))
                    return;
                if (getDescription().getVersion().equals(version))
                    return;
                // split the numbers
                String[] versionSplit = version.split("\\.");
                String[] currentSplit = getDescription().getVersion().split("\\.");
                // convert to integers
                int[] versionNumbers = new int[versionSplit.length];
                for (int i = 0; i < versionSplit.length; i++) {
                    try {
                        versionNumbers[i] = Integer.parseInt(versionSplit[i]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                int[] currentNumbers = new int[currentSplit.length];
                for (int i = 0; i < currentNumbers.length; i++) {
                    try {
                        currentNumbers[i] = Integer.parseInt(currentSplit[i]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                boolean needsUpdate = false;
                for (int i = 0; i < currentNumbers.length; i++) {
                    if (currentNumbers[i] < versionNumbers[i]) {
                        needsUpdate = true;
                        break;
                    }
                }
                if (!needsUpdate && currentNumbers.length < versionNumbers.length)
                    needsUpdate = true;
                latestVersion = !needsUpdate;

                if (needsUpdate) {
                    Bukkit.getLogger().info("[NotBounties] A new update is available. Current version: " + getDescription().getVersion() + " Latest version: " + version);
                    Bukkit.getLogger().info("[NotBounties] Download a new version here:" + " https://www.spigotmc.org/resources/104484/");
                }
            });
        }

        // make bounty tracker work & big bounty particle & time immunity
        new BukkitRunnable() {
            List<BountyBoard> queuedBoards = new ArrayList<>();

            @Override
            public void run() {
                // update bounty tracker
                BountyTracker.update();

                // big bounty particle
                if (BigBounty.getThreshold() != -1) {
                    if (BigBounty.isParticle())
                        for (UUID uuid : displayParticle) {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                            if (player.isOnline()) {
                                for (Player viewer : Bukkit.getOnlinePlayers()) {
                                    if ((NotBounties.serverVersion >= 17 && viewer.canSee(Objects.requireNonNull(player.getPlayer()))) && viewer.getWorld().equals(player.getPlayer().getWorld()) && viewer.getLocation().distance(player.getPlayer().getLocation()) < 256 && !isVanished(player.getPlayer()))
                                        viewer.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getPlayer().getEyeLocation().add(0, 1, 0), 0, 0, 0, 0);
                                }
                            }
                        }
                }

                Immunity.update();
                RandomBounties.update();
                TimedBounties.update();

                PVPRestrictions.checkCombatExpiry();

                if (lastBountyBoardUpdate + boardUpdate * 1000 < System.currentTimeMillis() && !Bukkit.getOnlinePlayers().isEmpty()) {
                    // update bounty board
                    if (queuedBoards.isEmpty()) {
                        queuedBoards = new ArrayList<>(bountyBoards);
                    }
                    int minUpdate = boardStaggeredUpdate == 0 ? queuedBoards.size() : boardStaggeredUpdate;
                    List<Bounty> bountyCopy = getPublicBounties(boardType);
                    for (int i = 0; i < Math.min(queuedBoards.size(), minUpdate); i++) {
                        BountyBoard board = queuedBoards.get(i);
                        if (bountyCopy.size() >= board.getRank()) {
                            board.update(bountyCopy.get(board.getRank() - 1));
                        } else {
                            board.update(null);
                        }
                    }
                    if (Math.min(queuedBoards.size(), minUpdate) > 0) {
                        queuedBoards.subList(0, Math.min(queuedBoards.size(), minUpdate)).clear();
                    }
                    lastBountyBoardUpdate = System.currentTimeMillis();
                }

            }
        }.runTaskTimer(this, 100, 40);
        // auto save bounties every 5 min & do some ram cleaning
        new BukkitRunnable() {
            @Override
            public void run() {
                MurderBounties.cleanPlayerKills();
                // if they have expire-time enabled
                BountyExpire.removeExpiredBounties();

                save();
                try {
                    BountyMap.save();
                } catch (IOException e) {
                    Bukkit.getLogger().severe(e.toString());
                }


            }
        }.runTaskTimer(this, 6000, 6000);
        // Check for banned players
        //         * Runs every hour and will check a few players at a time
        //         * Every player will be guaranteed to be checked after 12 hours
        new BukkitRunnable() {
            List<Bounty> bountyListCopy = new ArrayList<>();
            int playersPerRun = 1;

            @Override
            public void run() {
                if (!removeBannedPlayers)
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
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isBanned() || (liteBansEnabled && !(new LiteBansClass().isPlayerNotBanned(bounty.getUUID())))) {
                                BountyManager.removeBounty(bounty.getUUID());
                            }
                        }
                    }.runTaskAsynchronously(NotBounties.getInstance());
                }
                if (playersPerRun > 0) {
                    if (bountyListCopy.size() > playersPerRun)
                        bountyListCopy.subList(0, playersPerRun).clear();
                    else
                        bountyListCopy.clear();
                }
            }
        }.runTaskTimer(NotBounties.getInstance(), 101, 72006);

        // wanted text
        new BukkitRunnable() {
            static final int maxUpdateTime = 10;
            int lastUpdateTime = 0; // time it took for the stands to update last
            long lastRunTime = 0;
            @Override
            public void run() {
                if (lastUpdateTime > maxUpdateTime) {
                    //    (the amount of ms since last update)      (2 x the amount of ms last update took)
                    if (System.currentTimeMillis() - lastRunTime < (lastUpdateTime - maxUpdateTime) * 2L) {
                        return;
                    }
                }
                long startTime = System.currentTimeMillis();
                for (Map.Entry<UUID, AboveNameText> entry : wantedText.entrySet()) {
                    entry.getValue().updateArmorStand();
                }
                lastUpdateTime = (int) (System.currentTimeMillis() - startTime);
                if (lastUpdateTime > maxUpdateTime) {
                    lastRunTime = System.currentTimeMillis();
                    if (debug)
                        Bukkit.getLogger().info("[NotBountiesDebug] Took " + lastUpdateTime + "ms to update wanted tags. Pausing for a few updates.");
                }
            }
        }.runTaskTimer(this, 20, 1);

    }


    public static NotBounties getInstance() {
        return instance;
    }


    private void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("immune-permissions", immunePerms);
        configuration.set("immunity-murder", autoImmuneMurderPerms);
        configuration.set("immunity-random", autoImmuneMurderPerms);
        configuration.set("immunity-timed", autoImmuneMurderPerms);
        int i = 0;
        for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            configuration.set("logged-players." + i + ".name", key);
            configuration.set("logged-players." + i + ".uuid", value);
            i++;
        }
        if (!SQL.isConnected()) {
            i = 0;
            for (Bounty bounty : BountyManager.getAllBounties(-1)) {
                configuration.set("bounties." + i + ".uuid", bounty.getUUID().toString());
                configuration.set("bounties." + i + ".name", bounty.getName());
                int f = 0;
                for (Setter setters : bounty.getSetters()) {
                    configuration.set("bounties." + i + "." + f + ".name", setters.getName());
                    configuration.set("bounties." + i + "." + f + ".uuid", setters.getUuid().toString());
                    configuration.set("bounties." + i + "." + f + ".amount", setters.getAmount());
                    configuration.set("bounties." + i + "." + f + ".time-created", setters.getTimeCreated());
                    configuration.set("bounties." + i + "." + f + ".notified", setters.isNotified());
                    List<String> whitelist = setters.getWhitelist().getList().stream().map(UUID::toString).collect(Collectors.toList());
                    configuration.set("bounties." + i + "." + f + ".whitelist", whitelist);
                    configuration.set("bounties." + i + "." + f + ".blacklist", setters.getWhitelist().isBlacklist());
                    configuration.set("bounties." + i + "." + f + ".playtime", setters.getReceiverPlaytime());
                    if (!setters.getItems().isEmpty())
                        configuration.set("bounties." + i + "." + f + ".items", SerializeInventory.itemStackArrayToBase64(setters.getItems().toArray(new ItemStack[0])));
                    f++;
                }
                i++;
            }
            for (Map.Entry<UUID, Double> mapElement : killBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".kills", mapElement.getValue().longValue());
                }
            }
            for (Map.Entry<UUID, Double> mapElement : setBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".set", mapElement.getValue().longValue());
                }
            }
            for (Map.Entry<UUID, Double> mapElement : deathBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".deaths", mapElement.getValue().longValue());
                }
            }
            for (Map.Entry<UUID, Double> mapElement : allTimeBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".all-time", mapElement.getValue());
                }
            }
            for (Map.Entry<UUID, Double> mapElement : allClaimedBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".all-claimed", mapElement.getValue());
                }
            }
            for (Map.Entry<UUID, Double> mapElement : immunitySpent.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".immunity", mapElement.getValue());
                }
            }
        }
        for (Map.Entry<UUID, Long> entry : TimedBounties.getNextBounties().entrySet()) {
            configuration.set("data." + entry.getKey() + ".next-bounty", entry.getValue());
        }
        for (Map.Entry<UUID, TimeZone> entry : LocalTime.getSavedTimeZones().entrySet()) {
            configuration.set("data." + entry.getKey() + ".time-zone", entry.getValue().getID());
        }
        if (variableWhitelist) {
            for (Map.Entry<UUID, Whitelist> mapElement : playerWhitelist.entrySet()) {
                List<String> stringList = mapElement.getValue().getList().stream().map(UUID::toString).collect(Collectors.toList());
                configuration.set("data." + mapElement.getKey().toString() + ".whitelist", stringList);
                if (mapElement.getValue().isBlacklist())
                    configuration.set("data." + mapElement.getKey().toString() + ".blacklist", true);
            }
        }
        for (Map.Entry<UUID, Double> entry : refundedBounties.entrySet()) {
            configuration.set("data." + entry.getKey().toString() + ".refund", entry.getValue());
        }
        for (Map.Entry<UUID, List<ItemStack>> entry : refundedItems.entrySet()) {
            configuration.set("data." + entry.getKey().toString() + ".refund-items", SerializeInventory.itemStackArrayToBase64(entry.getValue().toArray(new ItemStack[0])));
        }
        configuration.set("disable-broadcast", disableBroadcast.stream().map(UUID::toString).collect(Collectors.toList()));
        i = 0;
        for (Map.Entry<UUID, List<RewardHead>> mapElement : headRewards.entrySet()) {
            configuration.set("head-rewards." + i + ".setter", mapElement.getKey().toString());
            List<String> encodedText = new ArrayList<>();
            for (RewardHead rewardHead : mapElement.getValue()) {
                encodedText.add(encodeRewardHead(rewardHead));
            }
            configuration.set("head-rewards." + i + ".uuid", encodedText);
            i++;
        }
        i = 0;
        for (Map.Entry<Integer, UUID> mapElement : BountyTracker.getTrackedBounties().entrySet()) {
            configuration.set("tracked-bounties." + i + ".number", mapElement.getKey());
            configuration.set("tracked-bounties." + i + ".uuid", mapElement.getValue().toString());
            i++;
        }
        if (RandomBounties.isRandomBountiesEnabled())
            configuration.set("next-random-bounty", RandomBounties.getNextRandomBounty());
        i = 0;
        for (BountyBoard board : bountyBoards) {
            Location location = board.getLocation();
            // can't save a null world or location
            if (location == null || location.getWorld() == null)
                continue;
            configuration.set("bounty-boards." + i + ".location.world", location.getWorld().getUID().toString());
            configuration.set("bounty-boards." + i + ".location.x", location.getX());
            configuration.set("bounty-boards." + i + ".location.y", location.getY());
            configuration.set("bounty-boards." + i + ".location.z", location.getZ());
            configuration.set("bounty-boards." + i + ".location.yaw", location.getYaw());
            configuration.set("bounty-boards." + i + ".location.pitch", location.getPitch());
            configuration.set("bounty-boards." + i + ".rank", board.getRank());
            configuration.set("bounty-boards." + i + ".direction", board.getDirection().toString());
            i++;
        }
        File bounties = new File(NotBounties.getInstance().getDataFolder() + File.separator + "bounties.yml");
        try {
            configuration.save(bounties);
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
        }
    }

    public void loadConfig() throws IOException {
        // close gui

        ConfigOptions.reloadOptions();

        new BukkitRunnable() {
            @Override
            public void run() {
                // check players to display particles
                displayParticle.clear();
                List<Bounty> topBounties = getAllBounties(2);

                for (Bounty bounty : topBounties) {
                    if (bounty.getTotalDisplayBounty() >= BigBounty.getThreshold()) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(bounty.getUUID());
                        if (player.isOnline()) {
                            displayParticle.add(Objects.requireNonNull(player.getPlayer()).getUniqueId());
                        }
                    } else {
                        break;
                    }
                }
            }
        }.runTaskLater(NotBounties.getInstance(), 40);


        if (autoConnectTask != null) {
            autoConnectTask.cancel();
        }
        try {
            if (!firstConnect && (SQL.isConnected() || autoConnect)) {
                SQL.reconnect();
            }
        } catch (SQLException ignored) {

        }
        if (firstConnect) {
            firstConnect = false;
        }
        if (autoConnect) {
            autoConnectTask = new BukkitRunnable() {
                @Override
                public void run() {
                    tryToConnect();
                }
            }.runTaskTimer(this, 600, 600);
        }


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // close all GUIs
        for (Player player : Bukkit.getOnlinePlayers()) {
            GUI.safeCloseGUI(player, true);
        }
        // remove wanted tags
        for (Map.Entry<UUID, AboveNameText> entry : wantedText.entrySet()) {
            entry.getValue().removeStand();
        }
        wantedText.clear();
        // save bounties & logged players
        save();
        try {
            BountyMap.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (BountyBoard board : bountyBoards) {
            board.remove();
        }
        bountyBoards.clear();
    }

    /**
     * Returns an OfflinePlayer from their logged name
     * @param name Name of the player
     * @return The OfflinePlayer or null if one hasn't been logged yet.
     */
    public static OfflinePlayer getPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null)
            return player;
        if (loggedPlayers.containsKey(name.toLowerCase(Locale.ROOT)))
            return Bukkit.getOfflinePlayer(loggedPlayers.get(name.toLowerCase(Locale.ROOT)));
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(name));
        } catch (IllegalArgumentException e) {
            UUID closest = getClosestPlayer(name);
            if (closest != null)
                return Bukkit.getOfflinePlayer(closest);
            return null;
        }
    }

    private static UUID getClosestPlayer(String playerName) {
        List<String> viableNames = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(playerName.toLowerCase()))
                viableNames.add(entry.getKey());
        }
        if (viableNames.isEmpty())
            return null;
        Collections.sort(viableNames);
        return loggedPlayers.get(viableNames.get(0));
    }

    public static @NotNull String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null)
            return name;
        if (loggedPlayers.containsValue(uuid)) {
            for (Map.Entry<String, UUID> entry : loggedPlayers.entrySet()) {
                if (entry.getValue().equals(uuid))
                    return entry.getKey();
            }
        }
        return uuid.toString();
    }

    public static Whitelist getPlayerWhitelist(UUID uuid) {
        if (playerWhitelist.containsKey(uuid)) {
            return playerWhitelist.get(uuid);
        }
        Whitelist whitelist = new Whitelist(new ArrayList<>(), false);
        playerWhitelist.put(uuid, whitelist);

        return whitelist;
    }


    public void sendDebug(CommandSender sender) throws SQLException {
        sender.sendMessage(parse(prefix + ChatColor.WHITE + "NotBounties debug info:", null));
        String connected = SQL.isConnected() ? ChatColor.GREEN + "true" : ChatColor.RED + "false";
        int localBounties = BountyManager.getAllBounties(-1).size();
        sender.sendMessage(ChatColor.GOLD + "SQL > " + ChatColor.YELLOW + "Connected: " + connected + ChatColor.YELLOW + " Type: " + ChatColor.WHITE + SQL.getDatabaseType() + ChatColor.YELLOW + " ID: " + ChatColor.WHITE + SQL.getServerID() + ChatColor.YELLOW + " Local Bounties: " + ChatColor.WHITE + localBounties);
        sender.sendMessage(ChatColor.GOLD + "General > " + ChatColor.YELLOW + "Author: " + ChatColor.GRAY + "Not_Jaden" + ChatColor.YELLOW + " Plugin Version: " + ChatColor.WHITE + getDescription().getVersion() + ChatColor.YELLOW + " Server Version: " + ChatColor.WHITE + "1." + serverVersion + "." + serverSubVersion + ChatColor.YELLOW + " Debug Mode: " + ChatColor.WHITE + debug);
        int bounties = SQL.isConnected() ? data.getTopBounties(-1).size() : localBounties;
        sender.sendMessage(ChatColor.GOLD + "Stats > " + ChatColor.YELLOW + "Bounties: " + ChatColor.WHITE + bounties + ChatColor.YELLOW + " Tracked Bounties: " + ChatColor.WHITE + BountyTracker.getTrackedBounties().size() + ChatColor.YELLOW + " Bounty Boards: " + ChatColor.WHITE + bountyBoards.size());
        String vault = vaultEnabled ? ChatColor.GREEN + "Vault" : ChatColor.RED + "Vault";
        String papi = papiEnabled ? ChatColor.GREEN + "PlaceholderAPI" : ChatColor.RED + "PlaceholderAPI";
        String hdb = HDBEnabled ? ChatColor.GREEN + "HeadDataBase" : ChatColor.RED + "HeadDataBase";
        String liteBans = liteBansEnabled ? ChatColor.GREEN + "LiteBans" : ChatColor.RED + "LiteBans";
        String skinsRestorer = skinsRestorerEnabled ? ChatColor.GREEN + "SkinsRestorer" : ChatColor.RED + "SkinsRestorer";
        String betterTeams = BountyClaimRequirements.betterTeamsEnabled ? ChatColor.GREEN + "BetterTeams" : ChatColor.RED + "BetterTeams";
        String townyAdvanced = BountyClaimRequirements.townyAdvancedEnabled ? ChatColor.GREEN + "TownyAdvanced" : ChatColor.RED + "TownyAdvanced";
        String floodgate = floodgateEnabled ? ChatColor.GREEN + "Floodgate" : ChatColor.RED + "Floodgate";
        String geyser = geyserEnabled ? ChatColor.GREEN + "GeyserMC" : ChatColor.RED + "GeyserMC";
        String kingdoms = BountyClaimRequirements.kingdomsXEnabled ? ChatColor.GREEN + "Kingdoms" : ChatColor.RED + "Kingdoms";
        String lands = BountyClaimRequirements.landsEnabled ? ChatColor.GREEN + "Lands" : ChatColor.RED + "Lands";
        String worldGuard = BountyClaimRequirements.worldGuardEnabled ? ChatColor.GREEN + "WorldGuard" : ChatColor.RED + "WorldGuard";
        sender.sendMessage(ChatColor.GOLD + "Plugin Hooks > " + ChatColor.GRAY + "[" + vault + ChatColor.GRAY + "|" + papi + ChatColor.GRAY + "|" + hdb + ChatColor.GRAY + "|" + liteBans + ChatColor.GRAY + "|" + skinsRestorer + ChatColor.GRAY + "|" + betterTeams + ChatColor.GRAY + "|" + townyAdvanced + ChatColor.GRAY + "|" + geyser + ChatColor.GRAY + "|" + floodgate + ChatColor.GRAY + "|" + kingdoms + ChatColor.GRAY + "|" + lands + ChatColor.GRAY + "|" + worldGuard + ChatColor.GRAY + "]");
        sender.sendMessage(ChatColor.GRAY + "Reloading the plugin will refresh connections.");
        TextComponent discord = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(114, 137, 218)) + "Discord: " + ChatColor.GRAY + "https://discord.gg/zEsUzwYEx7");
        discord.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/zEsUzwYEx7"));
        TextComponent spigot = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(240, 149, 45)) + "Spigot: " + ChatColor.GRAY + "https://www.spigotmc.org/resources/notbounties.104484/");
        spigot.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/notbounties.104484/"));
        TextComponent github = new TextComponent(net.md_5.bungee.api.ChatColor.of(new Color(230, 237, 243)) + "Github: " + ChatColor.GRAY + "https://github.com/No-Not-Jaden/NotBounties");
        github.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/No-Not-Jaden/NotBounties"));
        sender.spigot().sendMessage(discord);
        sender.spigot().sendMessage(spigot);
        sender.spigot().sendMessage(github);
        sender.sendMessage("");
    }

    public static void removeBountyBoard() {
        for (BountyBoard board : bountyBoards) {
            board.remove();
        }
        bountyBoards.clear();
    }


    public static void addBountyBoard(BountyBoard board) {
        bountyBoards.add(board);
    }


    public static int removeSpecificBountyBoard(ItemFrame frame) {
        ListIterator<BountyBoard> bountyBoardListIterator = bountyBoards.listIterator();
        int removes = 0;
        while (bountyBoardListIterator.hasNext()) {
            BountyBoard board = bountyBoardListIterator.next();
            if (frame.equals(board.getFrame())) {
                board.remove();
                bountyBoardListIterator.remove();
                removes++;
            }
        }
        return removes;
    }

    public static void removeWantedTag(UUID uuid) {
        if (!wantedText.containsKey(uuid))
            return;
        wantedText.get(uuid).removeStand();
        wantedText.remove(uuid);
    }


    public static RewardHead decodeRewardHead(String input) {
        try {
            if (!input.contains(",")) {
                UUID uuid = UUID.fromString(input);
                return new RewardHead(getPlayerName(uuid), uuid, 0);
            } else {
                UUID uuid = UUID.fromString(input.substring(0, input.indexOf(",")));
                input = input.substring(input.indexOf(',') + 1);
                String playerName = input.substring(0, input.indexOf(","));
                input = input.substring(input.indexOf(",") + 1);
                double amount = NumberFormatting.tryParse(input);
                return new RewardHead(playerName, uuid, amount);
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning(e.toString());
        }
        return null;
    }

    public static String encodeRewardHead(RewardHead rewardHead) {
        return rewardHead.getUuid().toString() + "," + rewardHead.getPlayerName() + "," + NumberFormatting.getValue(rewardHead.getAmount());
    }

    public static List<OfflinePlayer> getNetworkPlayers() {
        if (SQL.isConnected())
            return data.getOnlinePlayers();
        return Bukkit.getOnlinePlayers().stream().filter(player -> !isVanished(player)).collect(Collectors.toList());
    }

    public static boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    /**
     * Returns if the server version is above the specified version
     * @param majorVersion Major version of the server. In 1.20.4, the major version is 20
     * @param subVersion Sub version of the server. In 1.20.4, the sub version is 4
     * @return True if the current server version is higher than the specified one
     */
    public static boolean isAboveVersion(int majorVersion, int subVersion) {
        return serverVersion > majorVersion || (majorVersion == serverVersion && subVersion < serverSubVersion);
    }

    public static void debugMessage(String message, boolean warning) {
        if (!debug)
            return;
        message = "[NotBountiesDebug] " + message;
        NotBounties notBounties = NotBounties.getInstance();
        if (notBounties.isEnabled()) {
            String finalMessage = message;
            new BukkitRunnable() {
                @Override
                public void run() {
                    consoleMessage(finalMessage, warning);
                }
            }.runTask(notBounties);
        } else {
            consoleMessage(message, warning);
        }
    }

    private static void consoleMessage(String message, boolean warning) {
        if (warning)
            Bukkit.getLogger().warning(message);
        else
            Bukkit.getLogger().info(message);
    }

    public static boolean isBedrockPlayer(UUID uuid) {
         if (ConfigOptions.floodgateEnabled) {
            FloodGateClass floodGateClass = new FloodGateClass();
            return floodGateClass.isBedrockPlayer(uuid);
        } else if  (ConfigOptions.geyserEnabled) {
            GeyserMCClass geyserMCClass = new GeyserMCClass();
            return geyserMCClass.isBedrockPlayer(uuid);
        } else {
             String first = uuid.toString().substring(0, 18);
             return first.equals("00000000-0000-0000");
         }
    }

    public static String getXuid(UUID uuid) {
        if (ConfigOptions.floodgateEnabled) {
            FloodGateClass floodGateClass = new FloodGateClass();
            if (floodGateClass.isBedrockPlayer(uuid)) {
                return floodGateClass.getXuid(uuid);
            }
        } else if  (ConfigOptions.geyserEnabled) {
            GeyserMCClass geyserMCClass = new GeyserMCClass();
            if (geyserMCClass.isBedrockPlayer(uuid)) {
                return geyserMCClass.getXuid(uuid);
            }
        } else if (isBedrockPlayer(uuid)) {
            String last = uuid.toString().replace("-", "").substring(16);
            try {
                return Long.parseLong(last, 16) + "";
            } catch (NumberFormatException ignored){
                // Not parsable hex
            }
        }
        return "";
    }



}

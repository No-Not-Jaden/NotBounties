package me.jadenp.notbounties;

import me.jadenp.notbounties.autoBounties.MurderBounties;
import me.jadenp.notbounties.autoBounties.RandomBounties;
import me.jadenp.notbounties.autoBounties.TimedBounties;
import me.jadenp.notbounties.ui.Commands;
import me.jadenp.notbounties.ui.Events;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.ui.map.BountyMap;
import me.jadenp.notbounties.utils.*;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.utils.BountyManager.*;
import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static me.jadenp.notbounties.utils.NumberFormatting.manualEconomy;
import static me.jadenp.notbounties.utils.NumberFormatting.vaultEnabled;
import static me.jadenp.notbounties.utils.LanguageOptions.*;

/**
 * Proxy Messaging
 * Webhooks
 * Challenges
 * bimodal currency - x
 * partial manual economy - x
 * after claim order - x
 * replace help messages in usage - x
 * Immunity expire time - x
 * bounty expire time for auto bounties
 * add skinsrestorer file?
 * bounty expire offline tracking
 * edit bounty stats
 * view other's stats
 */

public final class NotBounties extends JavaPlugin {

    /**
     * Name (lower case), UUID
     */
    public static final Map<String, UUID> loggedPlayers = new HashMap<>();

    public static List<String> immunePerms = new ArrayList<>();
    public static final List<UUID> disableBroadcast = new ArrayList<>();
    /**
     * Player UUID, Whitelist UUIDs
     */
    public static final Map<UUID, Whitelist> playerWhitelist = new HashMap<>();
    public static final Map<String, Long> repeatBuyCommand = new HashMap<>();
    public static final Map<String, Long> repeatBuyCommand2 = new HashMap<>();


    public static final List<Player> displayParticle = new ArrayList<>();
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


    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Objects.requireNonNull(this.getCommand("notbounties")).setExecutor(new Commands());
        Bukkit.getServer().getPluginManager().registerEvents(new Events(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new CurrencySetup(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyMap(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PVPRestrictions(), this);

        this.saveDefaultConfig();

        BountyManager.loadBounties();
        BountyMap.initialize();
        namespacedKey = new NamespacedKey(this, "bounty-entity");

        try {
            // get the text version - ex: 1.20.3
            String fullServerVersion = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf("-"));
            fullServerVersion = fullServerVersion.substring(2); // remove the '1.' in the version
            if (fullServerVersion.contains("."))
                fullServerVersion = fullServerVersion.substring(0, fullServerVersion.indexOf(".")); // remove the last digits if any - ex .3
            serverVersion = Integer.parseInt(fullServerVersion);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning("[NotBounties] Could not get the server version. Some features may not function properly.");
        }

        if (serverVersion >= 17)
            Bukkit.getServer().getPluginManager().registerEvents(new RemovePersistentEntitiesEvent(), this);

        try {
            loadConfig();
        } catch (IOException e) {
            Bukkit.getLogger().warning("NotBounties is having trouble loading saved bounties.");
            Bukkit.getLogger().warning(e.toString());
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
                if (tracker)
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item.getType() == Material.COMPASS) {
                            if (item.getItemMeta() != null) {
                                if (item.getItemMeta().getLore() != null) {
                                    if (!item.getItemMeta().getLore().isEmpty()) {
                                        String lastLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
                                        if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                            if (player.hasPermission("notbounties.tracker")) {
                                                int number;
                                                try {
                                                    number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                                } catch (NumberFormatException ignored) {
                                                    return;
                                                }
                                                if (trackedBounties.containsKey(number)) {
                                                    if (hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                        CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                                                        Location lodeStoneLoc = compassMeta.getLodestone();
                                                        String actionBar;
                                                        Bounty bounty = getBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))));
                                                        assert bounty != null;
                                                        OfflinePlayer p1 = Bukkit.getOfflinePlayer(bounty.getUUID());

                                                        if (p1.isOnline() && player.canSee(Objects.requireNonNull(p1.getPlayer()))) {
                                                            Player p = p1.getPlayer();
                                                            assert p != null;
                                                            if (compassMeta.getLodestone() == null) {
                                                                compassMeta.setLodestone(p.getLocation().getBlock().getLocation());
                                                                compassMeta.setLodestoneTracked(false);
                                                            } else if (Objects.equals(compassMeta.getLodestone().getWorld(), p.getWorld())) {
                                                                if (compassMeta.getLodestone().distance(p.getLocation()) > 2) {
                                                                    compassMeta.setLodestone(p.getLocation().getBlock().getLocation());
                                                                    compassMeta.setLodestoneTracked(false);
                                                                }
                                                            } else {
                                                                compassMeta.setLodestone(p.getLocation().getBlock().getLocation());
                                                                compassMeta.setLodestoneTracked(false);
                                                            }

                                                            if (trackerGlow > 0) {
                                                                if (p.getWorld().equals(player.getWorld())) {
                                                                    if (player.getLocation().distance(p.getLocation()) < trackerGlow) {
                                                                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 45, 0));
                                                                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(parse(trackedNotify, p)));
                                                                    }
                                                                }
                                                            }
                                                            actionBar = ChatColor.DARK_GRAY + "|";
                                                            if (TABPlayerName) {
                                                                actionBar += " " + ChatColor.YELLOW + p.getName() + ChatColor.DARK_GRAY + " |";
                                                            }
                                                            if (TABDistance) {
                                                                if (p.getWorld().equals(player.getWorld())) {
                                                                    actionBar += " " + ChatColor.GOLD + ((int) player.getLocation().distance(p.getLocation())) + "m" + ChatColor.DARK_GRAY + " |";
                                                                } else {
                                                                    actionBar += " ?m |";
                                                                }
                                                            }
                                                            if (TABPosition) {
                                                                actionBar += " " + ChatColor.RED + p.getLocation().getBlockX() + "x " + p.getLocation().getBlockY() + "y " + p.getLocation().getBlockZ() + "z" + ChatColor.DARK_GRAY + " |";
                                                            }
                                                            if (TABWorld) {
                                                                actionBar += " " + ChatColor.LIGHT_PURPLE + p.getWorld().getName() + ChatColor.DARK_GRAY + " |";
                                                            }
                                                        } else {
                                                            actionBar = ChatColor.GRAY + "*offline*";
                                                            if (compassMeta.getLodestone() != null)
                                                                if (!Objects.equals(compassMeta.getLodestone().getWorld(), player.getWorld()))
                                                                    if (Bukkit.getWorlds().size() > 1) {
                                                                        for (World world : Bukkit.getWorlds()) {
                                                                            if (!world.equals(player.getWorld())) {
                                                                                compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), world.getSpawnLocation().getY(), world.getSpawnLocation().getZ()));
                                                                            }
                                                                        }
                                                                    } else {
                                                                        //compassMeta.setLodestone(null);
                                                                        compassMeta.setLodestoneTracked(true);
                                                                    }
                                                        }
                                                        if (lodeStoneLoc != null && compassMeta.getLodestone() != null) {
                                                            if (!lodeStoneLoc.equals(compassMeta.getLodestone())) {
                                                                item.setItemMeta(compassMeta);
                                                            }
                                                        } else {
                                                            if ((lodeStoneLoc == null && compassMeta.getLodestone() != null) || lodeStoneLoc != null && compassMeta.getLodestone() == null) {
                                                                item.setItemMeta(compassMeta);
                                                            }
                                                        }

                                                        // display action bar
                                                        if (trackerActionBar && TABShowAlways) {
                                                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (trackerActionBar && TABShowAlways) {
                                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No Permission."));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                // big bounty particle
                if (bBountyThreshold != -1) {
                    if (bBountyParticle)
                        for (Player player : displayParticle) {
                            if (player.isOnline()) {
                                for (Player viewer : Bukkit.getOnlinePlayers()) {
                                    if (viewer.canSee(player) && viewer.getWorld().equals(player.getWorld()) && viewer.getLocation().distance(player.getLocation()) < 256)
                                        viewer.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getEyeLocation().add(0, 1, 0), 0, 0, 0, 0);
                                }
                            }
                        }
                }

                Immunity.update();
                RandomBounties.update();
                TimedBounties.update();

                if (lastBountyBoardUpdate + boardUpdate * 1000 < System.currentTimeMillis()) {
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
                            //Bukkit.getLogger().info(board.getRank() + ": " + bountyCopy.get(board.getRank()-1).getName());
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
        // auto save bounties every 5 min & remove bounty tracker
        new BukkitRunnable() {
            @Override
            public void run() {
                MurderBounties.cleanPlayerKills();
                // if they have expire-time enabled
                if (bountyExpire > 0) {
                    removeExpiredBounties();
                }

                save();
                try {
                    BountyMap.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (trackerRemove > 1) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        ItemStack[] contents = player.getInventory().getContents();
                        boolean change = false;
                        for (int x = 0; x < contents.length; x++) {
                            if (contents[x] != null) {
                                if (contents[x].getType() == Material.COMPASS) {
                                    if (contents[x].getItemMeta() != null) {
                                        if (Objects.requireNonNull(contents[x].getItemMeta()).getLore() != null) {
                                            if (!Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).isEmpty()) {
                                                String lastLine = Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).get(Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).size() - 1);
                                                if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                                    int number;
                                                    try {
                                                        number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                                    } catch (NumberFormatException ignored) {
                                                        return;
                                                    }
                                                    if (trackedBounties.containsKey(number)) {
                                                        if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                            contents[x] = null;
                                                            change = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (change) {
                            player.getInventory().setContents(contents);
                        }
                    }
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
                    bountyListCopy = SQL.isConnected() ? data.getTopBounties(2) : new ArrayList<>(bountyList);
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
                                if (SQL.isConnected()) {
                                    data.removeBounty(bounty.getUUID());
                                } else {
                                    bountyList.remove(bounty);
                                }
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
            @Override
            public void run() {
                for (Map.Entry<UUID, AboveNameText> entry : wantedText.entrySet()) {
                    entry.getValue().updateArmorStand();
                }
            }
        }.runTaskTimer(this, 20, 1);

    }


    public static NotBounties getInstance() {
        return instance;
    }


    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("immune-permissions", immunePerms);
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
            for (Bounty bounty : bountyList) {
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
            for (Map.Entry<UUID, Long> entry : TimedBounties.getNextBounties().entrySet()) {
                configuration.set("data." + entry.getKey() + ".next-bounty", entry.getValue());
            }

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
        configuration.set("disable-broadcast", disableBroadcast);
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
        for (Map.Entry<Integer, String> mapElement : trackedBounties.entrySet()) {
            configuration.set("tracked-bounties." + i + ".number", mapElement.getKey());
            configuration.set("tracked-bounties." + i + ".uuid", mapElement.getValue());
            i++;
        }
        if (RandomBounties.isRandomBountiesEnabled())
            configuration.set("next-random-bounty", RandomBounties.getNextRandomBounty());
        i = 0;
        for (BountyBoard board : bountyBoards) {
            configuration.set("bounty-boards." + i + ".location", board.getLocation());
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


        // check players to display particles
        displayParticle.clear();
        List<Bounty> topBounties;
        if (SQL.isConnected()) {
            topBounties = data.getTopBounties(2);
        } else {
            Collections.sort(bountyList);
            topBounties = new ArrayList<>(bountyList);
        }
        for (Bounty bounty : topBounties) {
            if (bounty.getTotalBounty() >= bBountyThreshold) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(bounty.getUUID());
                if (player.isOnline()) {
                    displayParticle.add(player.getPlayer());
                }
            } else {
                break;
            }
        }

        if (autoConnectTask != null) {
            autoConnectTask.cancel();
        }
        try {
            if ((SQL.isConnected() || autoConnect) && !firstConnect) {
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

    public static OfflinePlayer getPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null)
            return player;
        if (loggedPlayers.containsKey(name.toLowerCase(Locale.ROOT)))
            return Bukkit.getOfflinePlayer(loggedPlayers.get(name.toLowerCase(Locale.ROOT)));
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(name));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getPlayerName(UUID uuid) {
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


    public static String formatTime(long ms) {
        long days = (long) (ms / (8.64 * Math.pow(10,7)));
        ms = (long) (ms % (8.64 * Math.pow(10,7)));
        long hours = ms / 3600000L;
        ms = ms % 3600000L;
        long minutes = ms / 60000L;
        ms = ms % 60000L;
        long seconds = ms / 1000L;
        String time = "";
        if (days > 0) time += days + "d ";
        if (hours > 0) time += hours + "h ";
        if (minutes > 0) time += minutes + "m ";
        if (seconds > 0) time += seconds + "s";
        return time;
    }



    public void sendDebug(CommandSender sender) throws SQLException {
        sender.sendMessage(parse(prefix + ChatColor.WHITE + "NotBounties debug info:", null));
        String connected = SQL.isConnected() ? ChatColor.GREEN + "true" : ChatColor.RED + "false";
        sender.sendMessage(ChatColor.GOLD + "SQL > " + ChatColor.YELLOW + "Connected: " + connected + ChatColor.YELLOW + " Type: " + ChatColor.WHITE + SQL.getDatabaseType() + ChatColor.YELLOW + " ID: " + ChatColor.WHITE + SQL.getServerID() + ChatColor.YELLOW + " Local Bounties: " + ChatColor.WHITE + bountyList.size());
        sender.sendMessage(ChatColor.GOLD + "General > " + ChatColor.YELLOW + "Author: " + ChatColor.GRAY + "Not_Jaden" + ChatColor.YELLOW + " Version: " + ChatColor.WHITE + getDescription().getVersion());
        int bounties = SQL.isConnected() ? data.getTopBounties(2).size() : bountyList.size();
        sender.sendMessage(ChatColor.GOLD + "Stats > " + ChatColor.YELLOW + "Bounties: " + ChatColor.WHITE + bounties + ChatColor.YELLOW + " Tracked Bounties: " + ChatColor.WHITE + trackedBounties.size() + ChatColor.YELLOW + " Bounty Boards: " + ChatColor.WHITE + bountyBoards.size());
        String vault = vaultEnabled ? ChatColor.GREEN + "Vault" : ChatColor.RED + "Vault";
        String papi = papiEnabled ? ChatColor.GREEN + "PlaceholderAPI" : ChatColor.RED + "PlaceholderAPI";
        String hdb = HDBEnabled ? ChatColor.GREEN + "HeadDataBase" : ChatColor.RED + "HeadDataBase";
        String liteBans = liteBansEnabled ? ChatColor.GREEN + "LiteBans" : ChatColor.RED + "LiteBans";
        String skinsRestorer = skinsRestorerEnabled ? ChatColor.GREEN + "SkinsRestorer" : ChatColor.RED + "SkinsRestorer";
        String betterTeams = betterTeamsEnabled ? ChatColor.GREEN + "BetterTeams" : ChatColor.RED + "BetterTeams";
        String townyAdvanced = townyAdvancedEnabled ? ChatColor.GREEN + "TownyAdvanced" : ChatColor.RED + "TownyAdvanced";
        sender.sendMessage(ChatColor.GOLD + "Plugin Hooks > " + ChatColor.GRAY + "[" + vault + ChatColor.GRAY + "|" + papi + ChatColor.GRAY + "|" + hdb + ChatColor.GRAY + "|" + liteBans + ChatColor.GRAY + "|" + skinsRestorer + ChatColor.GRAY + "|" + betterTeams + ChatColor.GRAY + "|" + townyAdvanced + ChatColor.GRAY + "]");
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
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    public static void removeExpiredBounties() {
        // go through all the bounties and remove setters if it has been more than expire time
        if (SQL.isConnected()) {
            List<Setter> setters = data.removeOldBounties();
            for (Setter setter : setters) {
                refundSetter(setter);
            }
        } else {
            ListIterator<Bounty> bountyIterator = bountyList.listIterator();
            while (bountyIterator.hasNext()) {
                Bounty bounty = bountyIterator.next();
                List<Setter> expiredSetters = new ArrayList<>();
                ListIterator<Setter> setterIterator = bounty.getSetters().listIterator();
                while (setterIterator.hasNext()) {
                    Setter setter = setterIterator.next();
                    if (System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire) {
                        if (setter.getUuid().equals(new UUID(0,0))) {
                            setterIterator.remove();
                            continue;
                        }

                        // check if setter is online
                        Player player = Bukkit.getPlayer(setter.getUuid());
                        if (player != null) {
                            if (manualEconomy != NumberFormatting.ManualEconomy.PARTIAL)
                                NumberFormatting.doAddCommands(player, setter.getAmount());
                            player.sendMessage(parse(prefix + expiredBounty, bounty.getName(), setter.getAmount(), player));
                        } else {
                            expiredSetters.add(setter);
                        }
                        setterIterator.remove();
                    }
                }
                // add bounty to expired bounties if some have expired
                if (!expiredSetters.isEmpty()) {
                    expiredBounties.add(new Bounty(bounty.getUUID(), expiredSetters, bounty.getName()));
                }
                //bounty.getSetters().removeIf(setter -> System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire);
                // remove bounty if all the setters have been removed
                if (bounty.getSetters().isEmpty()) {
                    bountyIterator.remove();
                    if (wantedText.containsKey(bounty.getUUID())) {
                        wantedText.get(bounty.getUUID()).removeStand();
                        wantedText.remove(bounty.getUUID());
                    }
                }
            }
        }
    }


}

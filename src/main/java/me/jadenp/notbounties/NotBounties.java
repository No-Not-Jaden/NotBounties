package me.jadenp.notbounties;

import me.jadenp.notbounties.gui.GUI;
import me.jadenp.notbounties.gui.GUIOptions;
import me.jadenp.notbounties.map.BountyMap;
import me.jadenp.notbounties.sql.MySQL;
import me.jadenp.notbounties.sql.SQLGetter;
import me.jadenp.notbounties.utils.ConfigOptions;
import me.jadenp.notbounties.utils.CurrencySetup;
import me.jadenp.notbounties.utils.NumberFormatting;
import me.jadenp.notbounties.utils.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
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
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.utils.ConfigOptions.*;

/**
 * custom heads work with base64 and hdb - x
 * Big bounty and bounty tracker don't work when player is hidden - x
 * page items don't work when there aren't more pages - x
 * multiple lines in buy own bounty lore - x
 * buy back is formatted correctly with divisions - x
 * option to only have one bounty set at a time - x
 * admins can see whitelisted bounties - x
 * save maps when created
 * time can be used -
 *
 * 2 messages
 */

public final class NotBounties extends JavaPlugin {

    /**
     * Name (lower case), UUID
     */
    public Map<String, String> loggedPlayers = new HashMap<>();
    public List<Bounty> bountyList = new ArrayList<>();
    public List<Bounty> expiredBounties = new ArrayList<>();
    public List<String> immunePerms = new ArrayList<>();
    public List<String> disableBroadcast = new ArrayList<>();
    /**
     * Player UUID, Whitelist UUIDs
     */
    public Map<UUID, List<UUID>> playerWhitelist = new HashMap<>();

    public File bounties = new File(this.getDataFolder() + File.separator + "bounties.yml");

    public Map<String, Double> killBounties = new HashMap<>();
    public Map<String, Double> setBounties = new HashMap<>();
    public Map<String, Double> deathBounties = new HashMap<>();
    public Map<String, Double> allTimeBounties = new HashMap<>();
    public Map<String, Double> allClaimedBounties = new HashMap<>();
    public Map<String, List<String>> headRewards = new HashMap<>();
    public Map<String, Long> repeatBuyCommand = new HashMap<>();
    public Map<String, Long> repeatBuyCommand2 = new HashMap<>();
    public Map<String, Double> immunitySpent = new HashMap<>();
    public Map<UUID, Long> immunityTimeTracker = new HashMap<>();
    public Map<String, Long> gracePeriod = new HashMap<>();
    public Map<Integer, String> trackedBounties = new HashMap<>();
    public List<Player> displayParticle = new ArrayList<>();
    private static NotBounties instance;
    public MySQL SQL;
    public SQLGetter data;

    private BukkitTask autoConnectTask = null;

    private boolean firstConnect = true;


    /**
     * This method attempts to connect to the MySQL database.
     * If a connection is successful, local storage will be migrated if that option is enabled
     *
     * @return true if a connection was successful
     */
    public boolean tryToConnect() {
        if (!SQL.isConnected()) {
            try {
                SQL.connect();
            } catch (ClassNotFoundException | SQLException e) {
                //e.printStackTrace();
                return false;
            }

            if (SQL.isConnected()) {
                Bukkit.getLogger().info("Database is connected!");
                data.createTable();
                data.createDataTable();
                if (bountyList.size() > 0 && migrateLocalData) {
                    Bukkit.getLogger().info("Migrating local storage to database");
                    // add entries to database
                    for (Bounty bounty : bountyList) {
                        if (bounty.getTotalBounty() != 0) {
                            for (Setter setter : bounty.getSetters()) {
                                data.addBounty(bounty, setter);
                            }
                        }
                    }
                    bountyList.clear();
                    YamlConfiguration configuration = new YamlConfiguration();
                    try {
                        configuration.save(bounties);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (killBounties.size() > 0 && migrateLocalData) {
                    // add entries to database
                    for (Map.Entry<String, Double> entry : killBounties.entrySet()) {
                        data.addData(entry.getKey(), entry.getValue().longValue(), setBounties.get(entry.getKey()).longValue(), deathBounties.get(entry.getKey()).longValue(), allTimeBounties.get(entry.getKey()), immunitySpent.get(entry.getKey()), allClaimedBounties.get(entry.getKey()));
                    }
                    killBounties.clear();
                    setBounties.clear();
                    deathBounties.clear();
                    allClaimedBounties.clear();
                    allTimeBounties.clear();
                    immunitySpent.clear();
                    YamlConfiguration configuration = new YamlConfiguration();
                    try {
                        configuration.save(bounties);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Bukkit.getLogger().info("Cleared up " + data.removeExtraData() + " unused rows in the database!");
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Objects.requireNonNull(this.getCommand("notbounties")).setExecutor(new Commands(this));
        Bukkit.getServer().getPluginManager().registerEvents(new Events(this), this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new CurrencySetup(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BountyMap(), this);

        this.saveDefaultConfig();

        this.SQL = new MySQL(this);
        this.data = new SQLGetter();

        BountyMap.initialize();

        try {
            loadConfig();
        } catch (IOException | SQLException | ClassNotFoundException e) {
            Bukkit.getLogger().warning("NotBounties is having trouble loading saved bounties.");
            e.printStackTrace();
        }

        // create bounties file if one doesn't exist
        try {
            if (bounties.createNewFile()) {
                Bukkit.getLogger().info("Created new storage file.");
            } else {
                // get existing bounties file
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(bounties);
                // add all previously logged on players to a map
                int i = 0;
                while (configuration.getString("logged-players." + i + ".name") != null) {
                    loggedPlayers.put(Objects.requireNonNull(configuration.getString("logged-players." + i + ".name")).toLowerCase(Locale.ROOT), configuration.getString("logged-players." + i + ".uuid"));
                    i++;
                }
                immunePerms = configuration.getStringList("immune-permissions");
                // go through bounties in file
                i = 0;
                while (configuration.getString("bounties." + i + ".uuid") != null) {
                    List<Setter> setters = new ArrayList<>();
                    List<Setter> expiredSetters = new ArrayList<>();
                    int l = 0;
                    while (configuration.getString("bounties." + i + "." + l + ".uuid") != null) {
                        List<String> whitelistUUIDs = new ArrayList<>();
                        if (configuration.isSet("bounties." + i + "." + l + ".whitelist"))
                            whitelistUUIDs = configuration.getStringList("bounties." + i + "." + l + ".whitelist");
                        List<UUID> convertedUUIDs = new ArrayList<>();
                        for (String uuid : whitelistUUIDs)
                            convertedUUIDs.add(UUID.fromString(uuid));
                        UUID setterUUID = Objects.requireNonNull(configuration.getString("bounties." + i + "." + l + ".uuid")).equalsIgnoreCase("CONSOLE") ? new UUID(0, 0) : UUID.fromString(Objects.requireNonNull(configuration.getString("bounties." + i + "." + l + ".uuid")));
                        Setter setter = new Setter(configuration.getString("bounties." + i + "." + l + ".name"), setterUUID, configuration.getDouble("bounties." + i + "." + l + ".amount"), configuration.getLong("bounties." + i + "." + l + ".time-created"), configuration.getBoolean("bounties." + i + "." + l + ".notified"), convertedUUIDs);
                        if (bountyExpire > 0) {
                            if (System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire) {
                                expiredSetters.add(setter);
                            } else {
                                setters.add(setter);
                            }
                        } else {
                            setters.add(setter);
                        }
                        l++;
                    }
                    if (!setters.isEmpty()) {
                        bountyList.add(new Bounty(UUID.fromString(Objects.requireNonNull(configuration.getString("bounties." + i + ".uuid"))), setters, configuration.getString("bounties." + i + ".name")));
                    }
                    if (!expiredSetters.isEmpty()) {
                        expiredBounties.add(new Bounty(UUID.fromString(Objects.requireNonNull(configuration.getString("bounties." + i + ".uuid"))), expiredSetters, configuration.getString("bounties." + i + ".name")));
                    }
                    i++;
                }

                // go through player logs - old version
                i = 0;
                while (configuration.isSet("data." + i + ".uuid")) {
                    String uuid = configuration.getString("data." + i + ".uuid");
                    if (configuration.isSet("data." + i + ".claimed"))
                        killBounties.put(uuid, (double) configuration.getLong("data." + i + ".claimed"));
                    if (configuration.isSet("data." + i + ".set"))
                        setBounties.put(uuid, (double) configuration.getLong("data." + i + ".set"));
                    if (configuration.isSet("data." + i + ".received"))
                        deathBounties.put(uuid, (double) configuration.getLong("data." + i + ".received"));
                    if (configuration.isSet("data." + i + ".all-time")) {
                        allTimeBounties.put(uuid, configuration.getDouble("data." + i + ".all-time"));
                    } else {
                        for (Bounty bounty : bountyList) {
                            // if they have a bounty already
                            if (bounty.getUUID().toString().equals(uuid)) {
                                allTimeBounties.put(uuid, bounty.getTotalBounty());
                                Bukkit.getLogger().info("Missing all time bounty for " + bounty.getName() + ". Setting as current bounty.");
                                break;
                            }
                        }
                    }
                    if (configuration.isSet("data." + i + ".all-claimed")) {
                        allClaimedBounties.put(uuid, configuration.getDouble("data." + i + ".all-claimed"));
                    }
                    immunitySpent.put(uuid, configuration.getDouble("data." + i + ".immunity"));
                    if (configuration.isSet("data." + i + ".broadcast")) {
                        disableBroadcast.add(uuid);
                    }
                    i++;
                }
                // end old version ^^^
                // new version vvv
                if (configuration.isConfigurationSection("data"))
                    for (String uuid : Objects.requireNonNull(configuration.getConfigurationSection("data")).getKeys(false)) {
                        // old data protection
                        if (uuid.length() < 10)
                            continue;
                        if (configuration.isSet("data." + uuid + ".kills"))
                            killBounties.put(uuid, (double) configuration.getLong("data." + uuid + ".kills"));
                        if (configuration.isSet("data." + uuid + ".set"))
                            setBounties.put(uuid, (double) configuration.getLong("data." + uuid + ".set"));
                        if (configuration.isSet("data." + uuid + ".deaths"))
                            deathBounties.put(uuid, (double) configuration.getLong("data." + uuid + ".deaths"));
                        if (configuration.isSet("data." + uuid + ".all-time"))
                            allTimeBounties.put(uuid, configuration.getDouble("data." + uuid + ".all-time"));
                        if (configuration.isSet("data." + uuid + ".all-claimed"))
                            allClaimedBounties.put(uuid, configuration.getDouble("data." + uuid + ".all-claimed"));
                        if (configuration.isSet("data." + uuid + ".immunity"))
                            immunitySpent.put(uuid, configuration.getDouble("data." + uuid + ".immunity"));
                    }
                if (configuration.isSet("disable-broadcast"))
                    disableBroadcast.addAll(configuration.getStringList("disable-broadcast"));

                i = 0;
                while (configuration.getString("head-rewards." + i + ".setter") != null) {
                    headRewards.put(configuration.getString("head-rewards." + i + ".setter"), configuration.getStringList("head-rewards." + i + ".uuid"));
                    i++;
                }
                i = 0;
                while (configuration.getString("tracked-bounties." + i + ".uuid") != null) {
                    trackedBounties.put(configuration.getInt("tracked-bounties." + i + ".number"), configuration.getString("tracked-bounties." + i + ".uuid"));
                    i++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // create language file if it doesn't exist
        if (!language.exists()) {
            saveResource("language.yml", false);
        }


        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BountyExpansion(this).register();
        }


        if (!tryToConnect()) {
            Bukkit.getLogger().info("Database not connected, using internal storage");
        }

        // update checker
        if (updateNotification) {
            new UpdateChecker(this, 104484).getVersion(version -> {
                if (this.getDescription().getVersion().equals(version) || this.getDescription().getVersion().contains("dev")) {
                    getLogger().info("Running latest version of NotBounties.");
                } else {
                    getLogger().info("A new update is available for NotBounties. Current version: " + this.getDescription().getVersion() + " Latest version: " + version);
                    getLogger().info("Download a new version here: https://www.spigotmc.org/resources/104484/");
                }
            });
        }

        // register immunity times
        if (immunityType == 3) {
            Map<String, Double> immunity = SQL.isConnected() ? data.getTopStats(Leaderboard.IMMUNITY, new ArrayList<>(), -1, -1) : immunitySpent;
            for (Map.Entry<String, Double> entry : immunity.entrySet()) {
                immunityTimeTracker.put(UUID.fromString(entry.getKey()), (long) ((entry.getValue() * timeImmunity * 1000) + System.currentTimeMillis()));
            }
        }

        // make bounty tracker work & big bounty particle & time immunity
        new BukkitRunnable() {
            int sqlTimer = 0;

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
                                                                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(parse(speakings.get(35), p)));
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
                if (immunityType == 3) {
                    // iterate through immunity and update it
                    if (SQL.isConnected() && sqlTimer >= 150) {
                        // check if there is immunity there that isn't recorded
                        LinkedHashMap<String, Double> immunityData = data.getTopStats(Leaderboard.IMMUNITY, new ArrayList<>(), -1, -1);
                        for (Map.Entry<String, Double> entry : immunityData.entrySet()) {
                            UUID uuid = UUID.fromString(entry.getKey());
                            if (entry.getValue() > 0)
                                if (!immunityTimeTracker.containsKey(uuid)) {
                                    // add to time tracker
                                    immunityTimeTracker.put(uuid, (long) ((entry.getValue() * timeImmunity * 1000) + System.currentTimeMillis()));
                                } else if (Math.abs(immunityTimeTracker.get(uuid) - ((long) ((entry.getValue() * timeImmunity) + System.currentTimeMillis()))) > 3000) {
                                    // values are farther than 3 seconds apart, update
                                    immunityTimeTracker.put(uuid, (long) ((entry.getValue() * timeImmunity * 1000) + System.currentTimeMillis()));
                                }
                        }
                        sqlTimer = 0;
                    }
                    sqlTimer++;

                    // iterate through time tracker to find any expired immunity.
                    List<UUID> expiredImmunity = new ArrayList<>();
                    for (Map.Entry<UUID, Long> entry : immunityTimeTracker.entrySet()) {
                        if (System.currentTimeMillis() > entry.getValue())
                            expiredImmunity.add(entry.getKey());
                        else if (SQL.isConnected())
                            data.setImmunity(entry.getKey().toString(), (double) (entry.getValue() - System.currentTimeMillis()) / 1000);
                        else
                            immunitySpent.put(entry.getKey().toString(), (double) (entry.getValue() - System.currentTimeMillis()) / (1000 * timeImmunity));
                    }
                    for (UUID uuid : expiredImmunity) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                        if (player.isOnline())
                            Objects.requireNonNull(player.getPlayer()).sendMessage(parse(speakings.get(0) + speakings.get(60), player));
                        immunityTimeTracker.remove(uuid);
                        if (SQL.isConnected())
                            data.setImmunity(uuid.toString(), 0);
                        else
                            immunitySpent.put(uuid.toString(), 0.0);
                    }
                }
            }
        }.runTaskTimer(this, 100, 40);
        // auto save bounties every 5 min & remove bounty tracker
        new BukkitRunnable() {
            @Override
            public void run() {
                // if they have expire-time enabled
                if (bountyExpire > 0) {
                    // go through all the bounties and remove setters if it has been more than expire time
                    if (SQL.isConnected()) {
                        data.removeOldBounties();
                    } else {
                        ListIterator<Bounty> bountyIterator = bountyList.listIterator();
                        while (bountyIterator.hasNext()) {
                            Bounty bounty = bountyIterator.next();
                            List<Setter> expiredSetters = new ArrayList<>();
                            ListIterator<Setter> setterIterator = bounty.getSetters().listIterator();
                            while (setterIterator.hasNext()) {
                                Setter setter = setterIterator.next();
                                if (System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire) {
                                    // check if setter is online
                                    Player player = Bukkit.getPlayer(setter.getUuid());
                                    if (player != null) {
                                        NumberFormatting.doAddCommands(player, setter.getAmount());
                                        player.sendMessage(parse(speakings.get(0) + speakings.get(31), bounty.getName(), setter.getAmount(), player));
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
                            if (bounty.getSetters().size() == 0) {
                                bountyIterator.remove();
                            }
                        }
                    }
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
        }.runTaskTimer(this, 3600, 3600);
    }

    public static NotBounties getInstance() {
        return instance;
    }


    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("immune-permissions", immunePerms);
        int i = 0;
        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
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
                    List<String> whitelist = setters.getWhitelist().stream().map(UUID::toString).collect(Collectors.toList());
                    configuration.set("bounties." + i + "." + f + ".whitelist", whitelist);
                    f++;
                }
                i++;
            }
            for (Map.Entry<String, Double> mapElement : killBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".kills", mapElement.getValue().longValue());
                }
            }
            for (Map.Entry<String, Double> mapElement : setBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".set", mapElement.getValue().longValue());
                }
            }
            for (Map.Entry<String, Double> mapElement : deathBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".deaths", mapElement.getValue().longValue());
                }
            }
            for (Map.Entry<String, Double> mapElement : allTimeBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".all-time", mapElement.getValue());
                }
            }
            for (Map.Entry<String, Double> mapElement : allClaimedBounties.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".all-claimed", mapElement.getValue());
                }
            }
            for (Map.Entry<String, Double> mapElement : immunitySpent.entrySet()) {
                if (mapElement.getValue() != 0.0) {
                    configuration.set("data." + mapElement.getKey() + ".immunity", mapElement.getValue());
                }
            }
        }
        configuration.set("disable-broadcast", disableBroadcast);
        i = 0;
        for (Map.Entry<String, List<String>> mapElement : headRewards.entrySet()) {
            configuration.set("head-rewards." + i + ".setter", mapElement.getKey());
            configuration.set("head-rewards." + i + ".uuid", mapElement.getValue());
            i++;
        }
        i = 0;
        for (Map.Entry<Integer, String> mapElement : trackedBounties.entrySet()) {
            configuration.set("tracked-bounties." + i + ".number", mapElement.getKey());
            configuration.set("tracked-bounties." + i + ".uuid", mapElement.getValue());
            i++;
        }

        try {
            configuration.save(bounties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() throws IOException, SQLException, ClassNotFoundException {
        // close gui


        speakings.clear();


        ConfigOptions.reloadOptions();


        // check players to display particles
        displayParticle.clear();
        List<Bounty> topBounties;
        if (SQL.isConnected()) {
            topBounties = data.getTopBounties();
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
        if ((SQL.isConnected() || autoConnect) && !firstConnect) {
            SQL.reconnect();
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
        // save bounties & logged players
        save();
        try {
            BountyMap.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Bounty> sortBounties(int sortType) {
        // how bounties are sorted
        List<Bounty> sortedList = new ArrayList<>(bountyList);
        Bounty temp;
        for (int i = 0; i < sortedList.size(); i++) {
            for (int j = i + 1; j < sortedList.size(); j++) {
                if ((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                        (sortedList.get(i).getSetters().get(0).getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 1) || // newest bounties at top
                        (sortedList.get(i).getTotalBounty() < sortedList.get(j).getTotalBounty() && sortType == 2) || // more expensive bounties at top
                        (sortedList.get(i).getTotalBounty() > sortedList.get(j).getTotalBounty() && sortType == 3)) { // less expensive bounties at top
                    temp = sortedList.get(i);
                    sortedList.set(i, sortedList.get(j));
                    sortedList.set(j, temp);
                }
            }
        }
        return sortedList;
    }

    int length = 10;

    public void listBounties(CommandSender sender, int page) {
        GUIOptions guiOptions = GUI.getGUI("bounty-gui");
        String title = "";
        if (guiOptions != null) {
            title = guiOptions.getName();
            if (guiOptions.isAddPage())
                title += " " + (page + 1);
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               " + ChatColor.RESET + " " + title + " " + ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               ");

        List<Bounty> sortedList = SQL.isConnected() ? data.getTopBounties() : sortBounties(Objects.requireNonNull(GUI.getGUI("bounty-gui")).getSortType());
        for (int i = page * length; i < (page * length) + length; i++) {
            if (sortedList.size() > i) {
                sender.sendMessage(parse(speakings.get(11), sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), Bukkit.getOfflinePlayer(sortedList.get(i).getUUID())));
            } else {
                break;
            }
        }

        TextComponent rightArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋙⋙⋙");
        rightArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties list " + page + 2));
        rightArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(net.md_5.bungee.api.ChatColor.of(new Color(232, 26, 225)) + "Next Page")));
        TextComponent leftArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋘⋘⋘");
        leftArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/notbounties list " + page));
        leftArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(net.md_5.bungee.api.ChatColor.of(new Color(232, 26, 225)) + "Last Page")));
        TextComponent space = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                        ");
        StringBuilder builder = new StringBuilder(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH);
        for (int i = 0; i < title.length(); i++) {
            builder.append(" ");
        }
        TextComponent titleFill = new TextComponent(builder.toString());
        TextComponent replacement = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "    ");

        TextComponent start = new TextComponent("");
        if (page > 0) {
            start.addExtra(leftArrow);
        } else {
            start.addExtra(replacement);
        }
        start.addExtra(space);
        start.addExtra(titleFill);
        //getLogger().info("size: " + bountyList.size() + " page: " + page + " calc: " + ((page * length) + length));
        if (sortedList.size() > (page * length) + length) {
            start.addExtra(rightArrow);
        } else {
            start.addExtra(replacement);
        }
        sender.spigot().sendMessage(start);
        //sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                        ");
    }


    public void addBounty(Player setter, OfflinePlayer receiver, double amount, List<UUID> whitelist) {
        // add to all time bounties

        Bounty bounty = null;
        if (SQL.isConnected()) {
            bounty = data.getBounty(receiver.getUniqueId());
            if (bounty == null) {
                bounty = new Bounty(setter, receiver, amount, whitelist);
            }
            data.addBounty(new Bounty(setter, receiver, amount, whitelist), new Setter(setter.getName(), setter.getUniqueId(), amount, System.currentTimeMillis(), receiver.isOnline(), whitelist));
            bounty.addBounty(setter, amount, whitelist);
            data.addData(receiver.getUniqueId().toString(), 0, 0, 0, amount, 0, 0);
        } else {
            allTimeBounties.replace(receiver.getUniqueId().toString(), Leaderboard.ALL.getStat(receiver.getUniqueId()) + amount);
            for (Bounty bountySearch : bountyList) {
                // if they have a bounty already
                if (bountySearch.getUUID().equals(receiver.getUniqueId())) {
                    bounty = bountySearch;
                    bounty.addBounty(setter, amount, whitelist);
                    break;
                }
            }
            if (bounty == null) {
                // create new bounty
                bounty = new Bounty(setter, receiver, amount, whitelist);
                bountyList.add(bounty);
            }
        }

        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            if (bounty.getTotalBounty() > bBountyThreshold && bounty.getTotalBounty() - amount < bBountyThreshold) {
                onlineReceiver.sendMessage(parse(speakings.get(0) + speakings.get(43), receiver));
                displayParticle.add(onlineReceiver);
                if (bBountyCommands != null && !bBountyCommands.isEmpty()) {
                    for (String command : bBountyCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{player}", Matcher.quoteReplacement(onlineReceiver.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(bounty.getTotalBounty() + "")));
                    }
                }
            }
            // send messages
            onlineReceiver.sendMessage(parse(speakings.get(0) + speakings.get(42), setter.getName(), amount, bounty.getTotalBounty(), receiver));
            onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
        }
        // send messages
        setter.sendMessage(parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, bounty.getTotalBounty(), receiver));
        setter.playSound(setter.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);

        String message = parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, bounty.getTotalBounty(), receiver);

        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.isEmpty()) {
            if (amount >= minBroadcast)
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!disableBroadcast.contains(player.getUniqueId().toString()) && !player.getUniqueId().equals(receiver.getUniqueId()) && !player.getUniqueId().equals(setter.getUniqueId())) {
                        player.sendMessage(message);
                    }
                }
        } else {
            for (UUID uuid : whitelist) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || player.getUniqueId().equals(receiver.getUniqueId()) || player.getUniqueId().equals(setter.getUniqueId()))
                    continue;
                player.sendMessage(message);
                player.sendMessage(parse(speakings.get(0) + speakings.get(63), player));
            }
        }

    }

    public void addBounty(OfflinePlayer receiver, double amount, List<UUID> whitelist) {
        // add to all time bounties

        Bounty bounty = null;
        if (SQL.isConnected()) {
            bounty = data.getBounty(receiver.getUniqueId());
            if (bounty == null) {
                bounty = new Bounty(receiver, amount, whitelist);
            }
            data.addBounty(new Bounty(receiver, amount, whitelist), new Setter("CONSOLE", new UUID(0, 0), amount, System.currentTimeMillis(), receiver.isOnline(), whitelist));
            bounty.addBounty(amount, whitelist);
            data.addData(receiver.getUniqueId().toString(), 0, 0, 0, amount, 0, 0);
        } else {
            allTimeBounties.put(receiver.getUniqueId().toString(), Leaderboard.ALL.getStat(receiver.getUniqueId()) + amount);
            for (Bounty bountySearch : bountyList) {
                // if they have a bounty already
                if (bountySearch.getUUID().equals(receiver.getUniqueId())) {
                    bounty = bountySearch;
                    bounty.addBounty(amount, whitelist);
                    break;
                }
            }
            if (bounty == null) {
                // create new bounty
                bounty = new Bounty(receiver, amount, whitelist);
                bountyList.add(bounty);
            }
        }
        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            if (bounty.getTotalBounty() > bBountyThreshold && bounty.getTotalBounty() - amount < bBountyThreshold) {
                onlineReceiver.sendMessage(parse(speakings.get(0) + speakings.get(43), receiver));
                displayParticle.add(onlineReceiver);
                if (bBountyCommands != null && !bBountyCommands.isEmpty()) {
                    for (String command : bBountyCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{player}", Matcher.quoteReplacement(onlineReceiver.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(bounty.getTotalBounty() + "")));
                    }
                }
            }
            // send messages
            onlineReceiver.sendMessage(parse(speakings.get(0) + speakings.get(42), "CONSOLE", amount, bounty.getTotalBounty(), receiver));
            onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
        }
        // send messages
        String message = parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, bounty.getTotalBounty(), receiver);
        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.isEmpty()) {
            if (amount >= minBroadcast)
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!disableBroadcast.contains(player.getUniqueId().toString()) && !player.getUniqueId().equals(receiver.getUniqueId())) {
                        player.sendMessage(message);
                    }
                }
        } else {
            for (UUID uuid : whitelist) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || player.getUniqueId().equals(receiver.getUniqueId()))
                    continue;
                player.sendMessage(message);
                player.sendMessage(parse(speakings.get(0) + speakings.get(63), player));
            }
        }

    }

    public OfflinePlayer getPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null)
            return player;
        if (loggedPlayers.containsKey(name.toLowerCase(Locale.ROOT)))
            return Bukkit.getOfflinePlayer(UUID.fromString(loggedPlayers.get(name.toLowerCase(Locale.ROOT))));
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(name));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null)
            return name;
        if (loggedPlayers.containsValue(uuid.toString())) {
            for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                if (entry.getValue().equals(uuid.toString()))
                    return entry.getKey();
            }
        }
        return uuid.toString();
    }

    public List<UUID> getPlayerWhitelist(UUID uuid) {
        return playerWhitelist.containsKey(uuid) ? playerWhitelist.get(uuid) : new ArrayList<>();
    }


    public void updateBounty(Bounty bounty) {
        if (SQL.isConnected()) {
            if (bounty.getSetters().isEmpty())
                data.removeBounty(bounty.getUUID());
            else
                data.updateBounty(bounty);
        } else {
            if (bounty.getSetters().isEmpty())
                bountyList.removeIf(bounty1 -> bounty1.getUUID().equals(bounty.getUUID()));
            else {
                ListIterator<Bounty> bountyListIterator = bountyList.listIterator();
                while (bountyListIterator.hasNext()) {
                    Bounty compareBounty = bountyListIterator.next();
                    if (compareBounty.getUUID().equals(bounty.getUUID())) {
                        bountyListIterator.set(bounty);
                        return;
                    }
                }
            }

        }
    }

    public boolean compareBounties(Bounty b1, Bounty b2) {
        if (b1.getUUID() != b2.getUUID())
            return false;
        if (b1.getTotalBounty() != b2.getTotalBounty())
            return false;
        List<Setter> s1 = b1.getSetters();
        List<Setter> s2 = b2.getSetters();
        Collections.sort(s1);
        Collections.sort(s2);
        if (s1.size() != s2.size())
            return false;
        for (int i = 0; i < s1.size(); i++) {
            if (!compareSetters(s1.get(i), s2.get(i)))
                return false;
        }
        return true;
    }

    public boolean compareSetters(Setter s1, Setter s2) {
        if (s1.getUuid() != s2.getUuid())
            return false;
        if (s1.getAmount() != s2.getAmount())
            return false;
        for (UUID uuid : s1.getWhitelist())
            if (!s2.getWhitelist().contains(uuid))
                return false;
        return true;
    }

    public boolean hasBounty(OfflinePlayer receiver) {
        if (SQL.isConnected()) {
            return data.getBounty(receiver.getUniqueId()) != null;
        } else {
            for (Bounty bounty : bountyList) {
                if (bounty.getUUID().equals(receiver.getUniqueId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String formatTime(long ms) {

        long hours = ms / 3600000L;
        ms = ms % 3600000L;
        long minutes = ms / 60000L;
        ms = ms % 60000L;
        long seconds = ms / 1000L;
        String time = "";
        if (hours > 0) time += hours + "h ";
        if (minutes > 0) time += minutes + "m ";
        if (seconds > 0) time += seconds + "s";
        return time;
    }


    public Bounty getBounty(OfflinePlayer receiver) {
        if (SQL.isConnected()) {
            return data.getBounty(receiver.getUniqueId());
        } else {
            for (Bounty bounty : bountyList) {
                if (bounty.getUUID().equals(receiver.getUniqueId())) {
                    return bounty;
                }
            }
        }
        return null;
    }


}

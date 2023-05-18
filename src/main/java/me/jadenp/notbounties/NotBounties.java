package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
import me.jadenp.notbounties.sql.MySQL;
import me.jadenp.notbounties.sql.SQLGetter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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

import static me.jadenp.notbounties.ConfigOptions.*;

public final class NotBounties extends JavaPlugin {
    /**
     * set bounties with command - x
     * language file - x
     * receive rewards when killed -
     * list bounties in chat - paged list - x
     * check bounty - x
     * file to save bounties - x
     * tab complete - x
     * on join add to logged players and check if they have a bounty - x
     * offline bounty set - config option to change expire time - x
     * save logged players to file - x
     * bounty immunity - with permission - & after a bounty was recently claimed on them w/ option in config
     * buy immunity - x
     * placeholder api support - x
     * notbounties_bounty - what their bounty is worth - x
     * notbounties_bounties_claimed - how many claimed bounties - x
     * not_bounties_bounties_set - how many set bounties - x
     * notbounties_bounties_received - how many received bounties - x
     * notbounties_immunity_spent - x
     * usable in language.yml - x
     * player heads option in config to receive heads when fulfilling a bounty -
     * item based currency - x
     * admins can edit or remove bounties with command or gui - x
     * gui to view/edit/remove bounties - edit fill in command in chat - x
     * config option to buy your own bounty with optional interest - command or rc in gui - x
     * minimum bounty - x
     * confirmation menu for removing bounties - also right click for removing same for buy back - x
     * add 1 space to help line bottom - x
     * -1 on check - x
     * rc to buy back not translated color codes - x
     * success edit & remove to green - x
     * amount not showing in gui - head lore - x
     * name fill - x
     * ignore-case when comparing in tab complete - x
     * only admins can set bounties on themselves - nah, this is funny
     * bounty buy works even if u don't have a bounty on yourself - x
     * minimum bounty doesn't work - x
     * bounty scaling immunity isn't parsed for player and doesn't stop - x
     * can kill yourself to get the bounty - x
     * add how much it costs for bounty buy - x
     * bounty broadcast is still double - x
     * removing a setter from bounty doesn't work - - x
     * change to right click to remove bounty from buy back - or match before - x
     * buy back displayed wrong in lore - x
     * no reload command *facepalm* - x
     * unknown number when editing with from - x
     * offline bounty set is case sensitive x
     * bounty broadcast says the setter x
     * placeholders work for immunity and buy back x
     * bounties expire x
     * reward don't give if false - - x
     * say unknown command for bounty buy when disabled - x
     * confirm buy for immunity - x
     * permanent immunity is still scaling immunity in practice - doesn't work at all now - x
     * can buy perm immunity infinitely - x
     * remove immunity command w/ permission  - x
     * bounty tax% - cost extra to place a bounty that rewards less - x
     * tab complete with offline players - - x
     * {amount} to broke - x
     * add immunity remove to tab complete and fix where autocomplete for names is - x
     * need another repeat command for buying immunity - x
     * check immunity permission
     * bounty check offline player doesn't work - x
     * hex color codes - x &#e03ff2 x
     * message and payback money for expired bounties x
     * do add commands is for expired bounties too*
     * offline set acc works not case sensitive x
     * advanced gui config x
     * use hex and placeholders x
     * give bounty tracker - x
     * remove tracker -
     * tracker works - x
     * rc works - x
     * change name - x
     * tab complete - x
     * all time bounty - x
     * bounty top cmd - x
     * disable bounty broadcast - x
     * tab autocomplete bdc - x
     * send message to player when they get bounty - x
     * all time top doesn't display correct numbers - x
     * bounty tracker permission -
     * tracker right click works even with show always off -
     * claim bounties later with paper - x
     * reward heads for claimed or setters - x
     * bounty redeemed broadcast also effected by bdc disabled -
     * fix typos - x
     * remove voucher - x
     * don't give double skull if a setter is the claimer - x
     * detects if add/remove commands are entered incorrectly - x
     * sort bounty gui and command - x
     * default config change for sort - x
     * min bounty broadcast - x
     * startup message fixed & wont say with dev - x f
     * send message to player receiving the bounty (new language) - x
     * remove bounty broadcast for console under min - x
     * minimize messages so no duplicates - x
     * bounty-success add {bounty} for total bounty - x f
     * bounty notification works with new bounties and old ones - x
     * bounty receiver sounds - x
     * redeem voucher sound - x
     * bounty complete sound - x
     * offline bounty notification amount in language - x
     * bounties over a certain amount give perks - x
     * ^ particle - - x
     * ^ command & works offline when they join - (not offline) - x
     * fixed a bug with claiming a bounty that was edited by console - x
     * logging in tells you bounties set and combines them after - x
     * add big bounty message - x
     * MySQL - x
     * Bounties top (args) - - x
     * not sorted correctly - x
     * autocomplete - x
     * claimed bounty amount - x
     * currency prefix/suffix - x
     * check your own stats - x
     * new placeholder - x
     * gui for bounty top - x
     * > new bounty top command - x
     * button in the gui to set a bounty - x
     * option to disable update notification - x
     * Change getting top stats to limit to 10 - x
     * hidden players from stats - x
     * move gui stuff to another config file - x
     * tab complete is null sometimes x
     * Removing bounty doesn't tell you in lore - x
     * Are you sure GUI - not a valid gui (no page number) - x
     * can buy back bounty or immunity with --confirm - x
     * make a getBalance() method - x
     * @ and !@ work in gui - x
     * {slot13} works - x
     *
     * remove and add commands in config moved - x
     * select price & leaderboard keeps info - x
     * leaderboard next page not working
     */

    /**
     * Name (lower case), UUID
     */
    public Map<String, String> loggedPlayers = new HashMap<>();
    public List<Bounty> bountyList = new ArrayList<>();
    public List<Bounty> expiredBounties = new ArrayList<>();
    public List<String> immunePerms = new ArrayList<>();
    public List<String> disableBroadcast = new ArrayList<>();

    public File bounties = new File(this.getDataFolder() + File.separator + "bounties.yml");






    public Map<String, Integer> bountiesClaimed = new HashMap<>();
    public Map<String, Integer> bountiesSet = new HashMap<>();
    public Map<String, Integer> bountiesReceived = new HashMap<>();
    public Map<String, Integer> allTimeBounty = new HashMap<>();
    public Map<String, Integer> allClaimed = new HashMap<>();
    public Map<String, List<String>> headRewards = new HashMap<>();
    public Map<String, Long> repeatBuyCommand = new HashMap<>();
    public Map<String, Long> repeatBuyCommand2 = new HashMap<>();
    public Map<String, Integer> immunitySpent = new HashMap<>();
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
     * @return true if a connection was successful
     */
    public boolean tryToConnect(){
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
                    for (Bounty bounty : bountyList){
                        if (bounty.getTotalBounty() != 0){
                            for (Setter setter : bounty.getSetters()){
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
                if (bountiesClaimed.size() > 0 && migrateLocalData) {
                    // add entries to database
                    for (Map.Entry<String, Integer> entry : bountiesClaimed.entrySet()){
                        data.addData(entry.getKey(), entry.getValue(), bountiesSet.get(entry.getKey()), bountiesReceived.get(entry.getKey()), allTimeBounty.get(entry.getKey()), immunitySpent.get(entry.getKey()), allClaimed.get(entry.getKey()));
                    }
                    bountiesClaimed.clear();
                    bountiesSet.clear();
                    bountiesReceived.clear();
                    allClaimed.clear();
                    allTimeBounty.clear();
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

        this.saveDefaultConfig();

        this.SQL = new MySQL(this);
        this.data = new SQLGetter();

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
                        Setter setter = new Setter(configuration.getString("bounties." + i + "." + l + ".name"), configuration.getString("bounties." + i + "." + l + ".uuid"), configuration.getInt("bounties." + i + "." + l + ".amount"), configuration.getLong("bounties." + i + "." + l + ".time-created"), configuration.getBoolean("bounties." + i + "." + l + ".notified"));
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
                        bountyList.add(new Bounty(configuration.getString("bounties." + i + ".uuid"), setters, configuration.getString("bounties." + i + ".name")));
                    }
                    if (!expiredSetters.isEmpty()) {
                        expiredBounties.add(new Bounty(configuration.getString("bounties." + i + ".uuid"), expiredSetters, configuration.getString("bounties." + i + ".name")));
                    }
                    i++;
                }

                // go through player logs
                i = 0;
                while (configuration.isSet("data." + i + ".uuid")) {
                    String uuid = configuration.getString("data." + i + ".uuid");
                    bountiesClaimed.put(uuid, configuration.getInt("data." + i + ".claimed"));
                    bountiesSet.put(uuid, configuration.getInt("data." + i + ".set"));
                    bountiesReceived.put(uuid, configuration.getInt("data." + i + ".received"));
                    if (configuration.isSet("data." + i + ".all-time")) {
                        allTimeBounty.put(uuid, configuration.getInt("data." + i + ".all-time"));
                    } else {
                        boolean hasABounty = false;
                        for (Bounty bounty : bountyList) {
                            // if they have a bounty already
                            if (bounty.getUUID().equals(uuid)) {
                                hasABounty = true;
                                allTimeBounty.put(uuid, bounty.getTotalBounty());
                                Bukkit.getLogger().info("Missing all time bounty for " + bounty.getName() + ". Setting as current bounty.");
                                break;
                            }
                        }
                        if (!hasABounty)
                            allTimeBounty.put(uuid, 0);
                    }
                    if (configuration.isSet("data." + i + ".all-claimed")){
                        allClaimed.put(uuid, configuration.getInt("data." + i + ".all-claimed"));
                    } else {
                        allClaimed.put(uuid, 0);
                    }
                    immunitySpent.put(uuid, configuration.getInt("data." + i + ".immunity"));
                    if (configuration.isSet("data." + i + ".broadcast")){
                        disableBroadcast.add(uuid);
                    }
                    i++;
                }

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



        if (!tryToConnect()){
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

        // make bounty tracker work
        new BukkitRunnable() {
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
                                                        Player p = Bukkit.getPlayer(UUID.fromString(bounty.getUUID()));
                                                        if (p != null) {
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
                if (bBountyThreshold != -1){

                    for (Player player : displayParticle){
                        if (player.isOnline()) {
                                if (bBountyParticle) {
                                    player.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getEyeLocation().add(0,1,0), 0, 0,0,0);
                                }
                                // other repeating perks would go here vvv
                        }
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
                    if (SQL.isConnected()){
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
                                    Player player = Bukkit.getPlayer(UUID.fromString(setter.getUuid()));
                                    if (player != null) {
                                        if (!usingPapi) {
                                            addItem(player, Material.valueOf(currency), setter.getAmount());
                                        }
                                        doAddCommands(player, setter.getAmount());
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

    public static NotBounties getInstance(){
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
                configuration.set("bounties." + i + ".uuid", bounty.getUUID());
                configuration.set("bounties." + i + ".name", bounty.getName());
                int f = 0;
                for (Setter setters : bounty.getSetters()) {
                    configuration.set("bounties." + i + "." + f + ".name", setters.getName());
                    configuration.set("bounties." + i + "." + f + ".uuid", setters.getUuid());
                    configuration.set("bounties." + i + "." + f + ".amount", setters.getAmount());
                    configuration.set("bounties." + i + "." + f + ".time-created", setters.getTimeCreated());
                    configuration.set("bounties." + i + "." + f + ".notified", setters.isNotified());
                    f++;
                }
                i++;
            }
            i = 0;
            for (Map.Entry<String, Integer> mapElement : bountiesClaimed.entrySet()) {
                if (mapElement.getValue() + bountiesSet.get(mapElement.getKey()) + bountiesReceived.get(mapElement.getKey()) + allTimeBounty.get(mapElement.getKey()) + allClaimed.get(mapElement.getKey()) + immunitySpent.get(mapElement.getKey()) == 0 && !disableBroadcast.contains(mapElement.getKey()))
                    continue;
                configuration.set("data." + i + ".uuid", mapElement.getKey());
                configuration.set("data." + i + ".claimed", mapElement.getValue());
                configuration.set("data." + i + ".set", bountiesSet.get(mapElement.getKey()));
                configuration.set("data." + i + ".received", bountiesReceived.get(mapElement.getKey()));
                configuration.set("data." + i + ".all-time", allTimeBounty.get(mapElement.getKey()));
                configuration.set("data." + i + ".all-claimed", allClaimed.get(mapElement.getKey()));
                configuration.set("data." + i + ".immunity", immunitySpent.get(mapElement.getKey()));
                if (disableBroadcast.contains(mapElement.getKey())) {
                    configuration.set("data." + i + ".broadcast", false);
                }
                i++;
            }
        }
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
        if (SQL.isConnected()){
            topBounties = data.getTopBounties();
        } else {
            Collections.sort(bountyList);
            topBounties = new ArrayList<>(bountyList);
        }
        for (Bounty bounty : topBounties){
            if (bounty.getTotalBounty() >= bBountyThreshold){
                Player player = Bukkit.getPlayer(bounty.getUUID());
                if (player != null){
                    displayParticle.add(player);
                }
            } else {
                break;
            }
        }

        if (autoConnectTask != null){
            autoConnectTask.cancel();
        }
        if ((SQL.isConnected() || autoConnect) && !firstConnect){
            SQL.reconnect();
        }
        if (firstConnect){
            firstConnect = false;
        }
        if (autoConnect){
            autoConnectTask = new BukkitRunnable(){
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
    }


    public ItemStack formatItem(ItemStack itemStack, Player player) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        ItemStack stack = new ItemStack(itemStack.getType(), itemStack.getAmount());
        ItemMeta meta = stack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color(parse(itemMeta.getDisplayName(), player)));
        if (itemMeta.hasCustomModelData())
            meta.setCustomModelData(itemMeta.getCustomModelData());
        ArrayList<String> lore = new ArrayList<>();
        if (itemMeta.getLore() != null)
            for (String str : itemMeta.getLore()) {
                lore.add(color(parse(str, player)));
            }
        meta.setLore(lore);
        if (itemStack.getEnchantments().size() > 0) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        stack.setItemMeta(meta);
        if (itemStack.getEnchantments().size() > 0) {
            stack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        return stack;
    }




    public List<Bounty> sortBounties(int sortType){
        // how bounties are sorted
        List<Bounty> sortedList = new ArrayList<>(bountyList);
        Bounty temp;
        for (int i = 0; i < sortedList.size(); i++) {
            for (int j = i + 1; j < sortedList.size(); j++) {
                if((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
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

    public void doRemoveCommands(Player p, int amount) {
        if (usingPapi) {
            if (removeCommands == null || removeCommands.isEmpty()){
                Bukkit.getLogger().warning("NotBounties detected a placeholder as currency, but there are no remove commands to take away money! (Is it formatted correctly?)");
            }
        } else {
            removeItem(p, Material.valueOf(currency), amount);
        }
            for (String str : removeCommands) {
                while (str.contains("{player}")) {
                    str = str.replace("{player}", p.getName());
                }
                while (str.contains("{amount}")) {
                    str = str.replace("{amount}", amount + "");
                }
                if (papiEnabled) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(p, str));
                } else {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), str);
                }
            }

    }

    int length = 10;

    public void listBounties(CommandSender sender, int page) {
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               " + ChatColor.RESET + " " + speakings.get(35) + " " + (page + 1) + " " + ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               ");

        List<Bounty> sortedList = SQL.isConnected() ? data.getTopBounties() : sortBounties(GUI.getGUI("bounty-gui").getSortType());
        for (int i = page * length; i < (page * length) + length; i++) {
            if (sortedList.size() > i) {
                if (papiEnabled) {
                    sender.sendMessage(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(sortedList.get(i).getUUID())), parse(speakings.get(11), sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null)));
                } else {
                    sender.sendMessage(parse(speakings.get(11), sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null));
                }
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
        TextComponent space = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                 ");
        TextComponent replacement = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "    ");

        TextComponent start = new TextComponent("");
        if (page > 0) {
            start.addExtra(leftArrow);
        } else {
            start.addExtra(replacement);
        }
        start.addExtra(space);
        //getLogger().info("size: " + bountyList.size() + " page: " + page + " calc: " + ((page * length) + length));
        if (sortedList.size() > (page * length) + length) {
            start.addExtra(rightArrow);
        } else {
            start.addExtra(replacement);
        }
        sender.spigot().sendMessage(start);
        //sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                        ");
    }

    public void doAddCommands(Player p, long amount) {
            if (usingPapi) {
                if (addCommands == null){
                    Bukkit.getLogger().warning("We detected a placeholder as currency, but there are no add commands to give players there reward! (Is it formatted correctly?)");
                }
                if (addCommands.isEmpty()){
                    Bukkit.getLogger().warning("We detected a placeholder as currency, but there are no add commands to give players there reward! (Is it formatted correctly?)");
                }
            }
                for (String str : addCommands) {
                    while (str.contains("{player}")) {
                        str = str.replace("{player}", p.getName());
                    }
                    while (str.contains("{amount}")) {
                        str = str.replace("{amount}", amount + "");
                    }
                    if (papiEnabled) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(p, str));
                    } else {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), str);
                    }

                }


    }



    public void removeItem(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                if (contents[i].getType().equals(material)) {
                    if (contents[i].getAmount() > amount) {
                        contents[i] = new ItemStack(contents[i].getType(), contents[i].getAmount() - amount);
                        break;
                    } else if (contents[i].getAmount() < amount) {
                        amount -= contents[i].getAmount();
                        contents[i] = null;
                    } else {
                        contents[i] = null;
                        break;
                    }
                }
            }
        }
        player.getInventory().setContents(contents);
        player.updateInventory();
    }

    // use this instead?
    public void givePlayer(Player p, ItemStack itemStack) {
        HashMap<Integer, ItemStack> leftOver = new HashMap<>((p.getInventory().addItem(itemStack)));
        if (!leftOver.isEmpty()) {
            Location loc = p.getLocation();
            p.getWorld().dropItem(loc, leftOver.get(0));
        }
    }

    public void addItem(Player player, Material material, long amount) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                if (amount > material.getMaxStackSize()) {
                    contents[i] = new ItemStack(material, material.getMaxStackSize());
                    amount -= material.getMaxStackSize();
                } else {
                    contents[i] = new ItemStack(material, (int) amount);
                    amount = 0;
                    break;
                }
            }
        }
        if (amount > 0) {
            for (int i = 0; i < amount / material.getMaxStackSize(); i++) {
                player.getWorld().dropItem(player.getLocation(), new ItemStack(material, material.getMaxStackSize()));
                amount-= material.getMaxStackSize();
            }
            player.getWorld().dropItem(player.getLocation(), new ItemStack(material, (int) amount));
        }
        player.getInventory().setContents(contents);
        player.updateInventory();
    }

    public void addItem(Player player, ItemStack itemStack) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean given = false;
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                contents[i] = itemStack;
                given = true;
                break;
            }
        }
        if (!given) {
            player.getWorld().dropItem(player.getLocation(), itemStack);
        } else {
            player.getInventory().setContents(contents);
            player.updateInventory();
        }
    }

    public void addBounty(Player setter, Player receiver, int amount) {
        // add to all time bounties


        Bounty bounty = null;
        if (SQL.isConnected()){
            bounty = data.getBounty(receiver.getUniqueId().toString());
            if (bounty == null){
                bounty = new Bounty(setter, receiver, amount);
            }
            data.addBounty(new Bounty(setter, receiver, amount), new Setter(setter.getName(), setter.getUniqueId().toString(), amount, System.currentTimeMillis(), true));
            bounty.addBounty(setter, amount);
            data.addData(receiver.getUniqueId().toString(),0,0,0,amount,0, 0);
        } else {
            allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);
            for (Bounty bountySearch : bountyList) {
                // if they have a bounty already
                if (bountySearch.getUUID().equals(receiver.getUniqueId().toString())) {
                    bounty = bountySearch;
                    bounty.addBounty(setter, amount);
                    break;
                }
            }
            if (bounty == null){
                // create new bounty
                bounty = new Bounty(setter, receiver, amount);
                bountyList.add(new Bounty(setter, receiver, amount));
            }
        }

        // check for big bounty
        if (bounty.getTotalBounty() > bBountyThreshold && bounty.getTotalBounty() - amount < bBountyThreshold){
            receiver.sendMessage(parse(speakings.get(0) + speakings.get(43), receiver));
            displayParticle.add(receiver);
            if (bBountyCommands != null && !bBountyCommands.isEmpty()){
                for (String command : bBountyCommands){
                    while (command.contains("{player}")){
                        command = command.replace("{player}", receiver.getName());
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
        // send messages
        setter.sendMessage(parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, bounty.getTotalBounty(), receiver));
        setter.playSound(setter.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT,1,1);
        receiver.sendMessage(parse(speakings.get(0) + speakings.get(42), setter.getName(), amount, bounty.getTotalBounty(), receiver));
        receiver.playSound(receiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL,1,1);
        String message = parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, bounty.getTotalBounty(), receiver);
        if (amount >= minBroadcast) {
            Bukkit.getConsoleSender().sendMessage(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!disableBroadcast.contains(player.getUniqueId().toString()) && !player.getUniqueId().equals(receiver.getUniqueId()) && !player.getUniqueId().equals(setter.getUniqueId())) {
                    player.sendMessage(message);
                }
            }
        }
    }

    public void addBounty(Player receiver, int amount) {
        // add to all time bounties

        Bounty bounty = null;
        if (SQL.isConnected()){
            bounty = data.getBounty(receiver.getUniqueId().toString());
            if (bounty == null){
                bounty = new Bounty(receiver, amount);
            }
            data.addBounty(new Bounty(receiver, amount), new Setter("CONSOLE", UUID.randomUUID().toString(), amount, System.currentTimeMillis(), true));
            bounty.addBounty(amount);
            data.addData(receiver.getUniqueId().toString(),0,0,0,amount,0, 0);
        } else {
            allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);
            for (Bounty bountySearch : bountyList) {
                // if they have a bounty already
                if (bountySearch.getUUID().equals(receiver.getUniqueId().toString())) {
                    bounty = bountySearch;
                    bounty.addBounty(amount);
                    break;
                }
            }
            if (bounty == null) {
                // create new bounty
                bounty = new Bounty(receiver, amount);
                bountyList.add(new Bounty(receiver, amount));
            }
        }
        // check for big bounty
        if (bounty.getTotalBounty() > bBountyThreshold && bounty.getTotalBounty() - amount < bBountyThreshold){
            receiver.sendMessage(parse(speakings.get(0) + speakings.get(43), receiver));
            displayParticle.add(receiver);
            if (bBountyCommands != null && !bBountyCommands.isEmpty()){
                for (String command : bBountyCommands){
                    while (command.contains("{player}")){
                        command = command.replace("{player}", receiver.getName());
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
        // send messages
        receiver.sendMessage(parse(speakings.get(0) + speakings.get(42), "CONSOLE", amount, bounty.getTotalBounty(), receiver));
        receiver.playSound(receiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL,1,1);
        String message = parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, bounty.getTotalBounty(), receiver);
        if (amount >= minBroadcast) {
            Bukkit.getConsoleSender().sendMessage(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!disableBroadcast.contains(player.getUniqueId().toString()) && !player.getUniqueId().equals(receiver.getUniqueId())) {
                    player.sendMessage(message);
                }
            }
        }

    }

    public void addBounty(Player setter, OfflinePlayer receiver, int amount) {
        // add to all time bounties

        Bounty bounty = null;

        if (SQL.isConnected()){
            bounty = data.getBounty(receiver.getUniqueId().toString());
            if (bounty == null){
                bounty = new Bounty(setter, receiver, amount);
            }
            data.addBounty(new Bounty(setter, receiver, amount), new Setter(setter.getName(), setter.getUniqueId().toString(), amount, System.currentTimeMillis(), false));
            bounty.addBounty(setter, amount);
            data.addData(receiver.getUniqueId().toString(),0,0,0,amount,0, 0);
        } else {
            allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);
            for (Bounty bountySearch : bountyList) {
                // if they have a bounty already
                if (bountySearch.getUUID().equals(receiver.getUniqueId().toString())) {
                    bounty = bountySearch;
                    bounty.addBounty(setter, amount);
                    break;
                }
            }
            if (bounty == null) {
                // create new bounty
                bounty = new Bounty(setter, receiver, amount);
                bountyList.add(new Bounty(setter, receiver, amount));
            }
        }
        // send messages
        if (papiEnabled) {
            setter.sendMessage(PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, bounty.getTotalBounty(), null)));
        } else {
            setter.sendMessage(parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, bounty.getTotalBounty(), null));
        }
        setter.playSound(setter.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT,1,1);
        String message = papiEnabled ? PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, bounty.getTotalBounty(), null)) : parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, bounty.getTotalBounty(), null);
        if (amount >= minBroadcast) {
            Bukkit.getConsoleSender().sendMessage(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!disableBroadcast.contains(player.getUniqueId().toString()) && !player.getUniqueId().equals(setter.getUniqueId())) {
                    player.sendMessage(message);
                }
            }
        }
    }

    public void addBounty(OfflinePlayer receiver, int amount) {
        // add to all time bounties

        Bounty bounty = null;
        if (SQL.isConnected()){
            bounty = data.getBounty(receiver.getUniqueId().toString());
            if (bounty == null){
                bounty = new Bounty(receiver, amount);
            }
            data.addBounty(new Bounty(receiver, amount), new Setter("CONSOLE", UUID.randomUUID().toString(), amount, System.currentTimeMillis(), false));
            bounty.addBounty(amount);
            data.addData(receiver.getUniqueId().toString(),0,0,0,amount,0, 0);
        } else {
            allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);
            for (Bounty bountySearch : bountyList) {
                // if they have a bounty already
                if (bountySearch.getUUID().equals(receiver.getUniqueId().toString())) {
                    bounty = bountySearch;
                    bounty.addBounty(amount);
                    break;
                }
            }
            if (bounty == null) {
                // create new bounty
                bounty = new Bounty(receiver, amount);
                bountyList.add(new Bounty(receiver, amount));
            }
        }
        // send messages
        String message = papiEnabled ? PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, bounty.getTotalBounty(), null)) : parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, bounty.getTotalBounty(), null);
        if (amount >= minBroadcast) {
            Bukkit.getConsoleSender().sendMessage(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!disableBroadcast.contains(player.getUniqueId().toString())) {
                    player.sendMessage(message);
                }
            }
        }
    }


    public boolean hasBounty(OfflinePlayer receiver) {
        if (SQL.isConnected()){
            return data.getBounty(receiver.getUniqueId().toString()) != null;
        } else {
            for (Bounty bounty : bountyList) {
                if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                    return true;
                }
            }
        }
        return false;
    }


    public Bounty getBounty(OfflinePlayer receiver) {
        if (SQL.isConnected()){
            return data.getBounty(receiver.getUniqueId().toString());
        } else {
            for (Bounty bounty : bountyList) {
                if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                    return bounty;
                }
            }
        }
        return null;
    }





}

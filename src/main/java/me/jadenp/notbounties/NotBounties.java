package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
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
import org.bukkit.event.inventory.InventoryType;
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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

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
     * <p>
     * bounty tracker permission -
     * tracker right click works even with show always off -
     * claim bounties later with paper - x
     * reward heads for claimed or setters - x
     * bounty redeemed broadcast also effected by bdc disabled -
     * fix typos - x
     * remove voucher - x
     * don't give double skull if a setter is the claimer - x
     * detects if add/remove commands are entered incorrectly ?
     * sort bounty gui and command - x
     *
     */

    public Map<String, String> loggedPlayers = new HashMap<>();
    public List<String> speakings = new ArrayList<>();
    public List<String> headLore = new ArrayList<>();
    public List<Bounty> bountyList = new ArrayList<>();
    public List<Bounty> expiredBounties = new ArrayList<>();
    public List<String> immunePerms = new ArrayList<>();
    public List<String> disableBroadcast = new ArrayList<>();

    public File bounties = new File(this.getDataFolder() + File.separator + "bounties.yml");
    public File language = new File(this.getDataFolder() + File.separator + "language.yml");

    public boolean usingPapi;
    public String currency;
    public List<String> removeCommands;
    public List<String> addCommands;
    public int bountyExpire;
    public boolean papiEnabled;
    public boolean rewardHeadSetter;
    public boolean buyBack;
    public double buyBackInterest;
    public String buyBackLore;
    public boolean buyImmunity;
    public boolean permanentImmunity;
    public int permanentCost;
    public double scalingRatio;
    public int graceTime;
    public int minBounty;
    public double bountyTax;
    public boolean rewardHeadClaimed;
    public boolean redeemRewardLater;

    public List<ItemStack> customItems = new ArrayList<>();
    public List<List<String>> itemCommands = new ArrayList<>();
    public List<Integer> bountySlots = new ArrayList<>();
    public List<String[]> layout = new ArrayList<>();
    public int guiSize;

    public boolean tracker;
    public int trackerRemove;
    public int trackerGlow;
    public boolean trackerActionBar;
    public boolean TABShowAlways;
    public boolean TABPlayerName;
    public boolean TABDistance;
    public boolean TABPosition;
    public boolean TABWorld;
    public int menuSorting;
    public List<String> trackerLore = new ArrayList<>();
    public List<String> voucherLore = new ArrayList<>();

    public Map<String, Integer> bountiesClaimed = new HashMap<>();
    public Map<String, Integer> bountiesSet = new HashMap<>();
    public Map<String, Integer> bountiesReceived = new HashMap<>();
    public Map<String, Integer> allTimeBounty = new HashMap<>();
    public Map<String, List<String>> headRewards = new HashMap<>();
    public Map<String, Long> repeatBuyCommand = new HashMap<>();
    public Map<String, Long> repeatBuyCommand2 = new HashMap<>();
    public Map<String, Integer> immunitySpent = new HashMap<>();
    public Map<String, Long> gracePeriod = new HashMap<>();
    public Map<Integer, String> trackedBounties = new HashMap<>();


    Item item = new Item();

    @Override
    public void onEnable() {
        // Plugin startup logic
        Objects.requireNonNull(this.getCommand("notbounties")).setExecutor(new Commands(this));
        Bukkit.getServer().getPluginManager().registerEvents(new Events(this), this);

        this.saveDefaultConfig();

        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // create bounties file if one doesn't exist
        if (!bounties.exists()) {
            try {
                bounties.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                    Setter setter = new Setter(configuration.getString("bounties." + i + "." + l + ".name"), configuration.getString("bounties." + i + "." + l + ".uuid"), configuration.getInt("bounties." + i + "." + l + ".amount"), configuration.getLong("bounties." + i + "." + l + ".time-created"));
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
                    bountyList.add(new Bounty(configuration.getString("bounties." + i + ".uuid"), setters, configuration.getString("bounties." + i + ".name"), configuration.getBoolean("bounties." + i + ".notified")));
                }
                if (!expiredSetters.isEmpty()) {
                    expiredBounties.add(new Bounty(configuration.getString("bounties." + i + ".uuid"), expiredSetters, configuration.getString("bounties." + i + ".name"), configuration.getBoolean("bounties." + i + ".notified")));
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

        // create language file if it doesn't exist
        if (!language.exists()) {
            saveResource("language.yml", false);
        }


        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BountyExpansion(this).register();
        }

        // update checker
        new UpdateChecker(this, 104484).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version)) {
                getLogger().info("[NotBounties] Running latest version.");
            } else {
                getLogger().info("[NotBounties] A new update is available. Current version: " + this.getDescription().getVersion() + " Latest version: " + version);
                getLogger().info("[NotBounties] Download a new version here: https://www.spigotmc.org/resources/104484/");
            }
        });

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
            }
        }.runTaskTimer(this, 100, 40);
        // auto save bounties every 5 min & remove bounty tracker
        new BukkitRunnable() {
            @Override
            public void run() {
                // if they have expire-time enabled
                if (bountyExpire > 0) {
                    // go through all the bounties and remove setters if it has been more than expire time
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
                            expiredBounties.add(new Bounty(bounty.getUUID(), expiredSetters, bounty.getName(), bounty.isNotified()));
                        }
                        //bounty.getSetters().removeIf(setter -> System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire);
                        // remove bounty if all the setters have been removed
                        if (bounty.getSetters().size() == 0) {
                            bountyIterator.remove();
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
        i = 0;
        for (Bounty bounty : bountyList) {
            configuration.set("bounties." + i + ".uuid", bounty.getUUID());
            configuration.set("bounties." + i + ".name", bounty.getName());
            configuration.set("bounties." + i + ".notified", bounty.isNotified());
            int f = 0;
            for (Setter setters : bounty.getSetters()) {
                configuration.set("bounties." + i + "." + f + ".name", setters.getName());
                configuration.set("bounties." + i + "." + f + ".uuid", setters.getUuid());
                configuration.set("bounties." + i + "." + f + ".amount", setters.getAmount());
                configuration.set("bounties." + i + "." + f + ".time-created", setters.getTimeCreated());
                f++;
            }
            i++;
        }
        i = 0;
        for (Map.Entry<String, Integer> mapElement : bountiesClaimed.entrySet()) {
            configuration.set("data." + i + ".uuid", mapElement.getKey());
            configuration.set("data." + i + ".claimed", mapElement.getValue());
            configuration.set("data." + i + ".set", bountiesSet.get(mapElement.getKey()));
            configuration.set("data." + i + ".received", bountiesReceived.get(mapElement.getKey()));
            configuration.set("data." + i + ".all-time", allTimeBounty.get(mapElement.getKey()));
            configuration.set("data." + i + ".immunity", immunitySpent.get(mapElement.getKey()));
            if (disableBroadcast.contains(mapElement.getKey())){
                configuration.set("data." + i + ".broadcast", false);
            }
            i++;
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

    public void loadConfig() throws IOException {
        // close gui
        if (!speakings.isEmpty())
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                    if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                        player.closeInventory();
                    }
                }
            }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(language);

        if (!configuration.isSet("prefix")) {
            configuration.set("prefix", "&7[&9Not&dBounties&7] &8» &r");
        }
        if (!configuration.isSet("unknown-number")) {
            configuration.set("unknown-number", "&cUnknown number!");
        }
        if (!configuration.isSet("bounty-success")) {
            configuration.set("bounty-success", "&aBounty placed on &e{player}&a for &e{amount}&a!");
        }
        if (!configuration.isSet("unknown-player")) {
            configuration.set("unknown-player", "&cCould not find the player &4{player}&c!");
        }
        if (!configuration.isSet("bounty-broadcast")) {
            configuration.set("bounty-broadcast", "&e{player}&6 has placed a bounty of &f{amount}&6 on &e{receiver}&6! Total Bounty: &f{bounty}");
        }
        if (!configuration.isSet("no-permission")) {
            configuration.set("no-permission", "&cYou do not have permission to execute this command!");
        }
        if (!configuration.isSet("broke")) {
            configuration.set("broke", "&cYou do not have enough currency for this! &8Required: &7{amount}");
        }
        if (!configuration.isSet("claim-bounty-broadcast")) {
            configuration.set("claim-bounty-broadcast", "&e{player}&6 has claimed the bounty of &f{amount}&6 on &e{receiver}&6!");
        }
        if (!configuration.isSet("no-bounty")) {
            configuration.set("no-bounty", "&4{receiver} &cdoesn't have a bounty!");
        }
        if (!configuration.isSet("check-bounty")) {
            configuration.set("check-bounty", "&e{receiver}&a has a bounty of &e{amount}&a.");
        }
        if (!configuration.isSet("list-setter")) {
            configuration.set("list-setter", "&e{player} &7> &a{amount}");
        }
        if (!configuration.isSet("list-total")) {
            configuration.set("list-total", "&e{player} &7> &a{amount}");
        }
        if (!configuration.isSet("offline-bounty")) {
            configuration.set("offline-bounty", "&e{player}&6 has set a bounty on you while you were offline!");
        }
        if (!configuration.isSet("bounty-item-name")) {
            configuration.set("bounty-item-name", "&3&l{player}");
        }
        if (!configuration.isSet("bounty-item-lore")) {
            configuration.set("bounty-item-lore", Arrays.asList("&aBounty: &f{amount}", "&2&oKill this player to", "&2&oreceive this reward", ""));
        }
        if (!configuration.isSet("success-remove-bounty")) {
            configuration.set("success-remove-bounty", "&cSuccessfully removed &4{receiver}'s &cbounty.");
        }
        if (!configuration.isSet("success-edit-bounty")) {
            configuration.set("success-edit-bounty", "&cSuccessfully edited &4{receiver}'s &cbounty.");
        }
        if (!configuration.isSet("no-setter")) {
            configuration.set("no-setter", "&4{player} &chas not set a bounty on {receiver}");
        }
        if (!configuration.isSet("repeat-command-bounty")) {
            configuration.set("repeat-command-bounty", "&6Please type this command in again in the next 30 seconds to confirm buying your bounty for &e{amount}&6.");
        }
        if (!configuration.isSet("repeat-command-immunity")) {
            configuration.set("repeat-command-immunity", "&6Please type this command in again in the next 30 seconds to confirm buying immunity for &e{amount}&6.");
        }
        if (!configuration.isSet("permanent-immunity")) {
            configuration.set("permanent-immunity", "&6{player} &eis immune to bounties!");
        }
        if (!configuration.isSet("scaling-immunity")) {
            configuration.set("scaling-immunity", "&6{player} &eis immune to bounties less than &e{amount}&6.");
        }
        if (!configuration.isSet("buy-permanent-immunity")) {
            configuration.set("buy-permanent-immunity", "&aYou have bought immunity from bounties.");
        }
        if (!configuration.isSet("buy-scaling-immunity")) {
            configuration.set("buy-scaling-immunity", "&aYou have bought immunity from bounties under the amount of &2{amount}&a.");
        }
        if (!configuration.isSet("grace-period")) {
            configuration.set("grace-period", "&cA bounty had just been claimed on &4{player}&c. Please wait &4{time}&c until you try again.");
        }
        if (!configuration.isSet("min-bounty")) {
            configuration.set("min-bounty", "&cThe bounty must be at least &4{amount}&c.");
        }
        if (!configuration.isSet("unknown-command")) {
            configuration.set("unknown-command", "&dUse &9/bounty help &dfor a list of commands.");
        }
        if (!configuration.isSet("already-bought-perm")) {
            configuration.set("already-bought-perm", "&cYou already have permanent immunity!");
        }
        if (!configuration.isSet("removed-immunity")) {
            configuration.set("removed-immunity", "&aSuccessfully removed your immunity to bounties.");
        }
        if (!configuration.isSet("removed-other-immunity")) {
            configuration.set("removed-other-immunity", "&aSuccessfully removed &2{receiver}''s &aimmunity to bounties.");
        }
        if (!configuration.isSet("no-immunity")) {
            configuration.set("no-immunity", "&cYou do not have purchased immunity!");
        }
        if (!configuration.isSet("no-immunity-other")) {
            configuration.set("no-immunity-other", "&4{receiver} &cdoes not have purchased immunity!");
        }
        if (!configuration.isSet("expired-bounty")) {
            configuration.set("expired-bounty", "&eYour bounty on &6{player}&e has expired. You have been refunded &2{amount}&e.");
        }
        if (!configuration.isSet("bounty-tracker-name")) {
            configuration.set("bounty-tracker-name", "&eBounty Tracker: &6&l{player}");
        }
        if (!configuration.isSet("bounty-tracker-lore")) {
            configuration.set("bounty-tracker-lore", Arrays.asList("", "&7Follow this compass", "&7to find {player}", ""));
        }
        if (!configuration.isSet("tracker-give")) {
            configuration.set("tracker-give", "&eYou have given &6{receiver}&e a compass that tracks &6{player}&e.");
        }
        if (!configuration.isSet("tracker-receive")) {
            configuration.set("tracker-receive", "&eYou have been given a bounty tracker for &6{player}&e.");
        }
        if (!configuration.isSet("gui-name")) {
            configuration.set("gui-name", "&dBounties &9Page");
        }
        if (!configuration.isSet("bounty-top")) {
            configuration.set("bounty-top", "&9&l{rank}. &d{player} &7> &a{amount}");
        }
        if (!configuration.isSet("bounty-top-title")) {
            configuration.set("bounty-top-title", "&7&m               &r &d&lBounties &9&lTop &7&m               ");
        }
        if (!configuration.isSet("enable-broadcast")) {
            configuration.set("enable-broadcast", "&eYou have &aenabled &ebounty broadcast!");
        }
        if (!configuration.isSet("disable-broadcast")) {
            configuration.set("disable-broadcast", "&eYou have &cdisabled &ebounty broadcast!");
        }
        if (!configuration.isSet("bounty-voucher-name")){
            configuration.set("bounty-voucher-name", "&6{player}'s&e claimed bounty of &a{amount}&e.");
        }
        if (!configuration.isSet("bounty-voucher-lore")){
            configuration.set("bounty-voucher-lore", Arrays.asList("", "&2Awarded to {receiver}", "&7Right click to redeem", "&7this player's bounty", ""));
        }
        if (!configuration.isSet("redeem-voucher")){
            configuration.set("redeem-voucher", "&aSuccessfully redeemed voucher for {amount}!");
        }


        configuration.save(language);

        speakings.clear();
        // 0 prefix
        speakings.add(color(Objects.requireNonNull(configuration.getString("prefix"))));
        // 1 unknown-number
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-number"))));
        // 2 bounty-success
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-success"))));
        // 3 unknown-player
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-player"))));
        // 4 bounty-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-broadcast"))));
        // 5 no-permission
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-permission"))));
        // 6 broke
        speakings.add(color(Objects.requireNonNull(configuration.getString("broke"))));
        // 7 claim-bounty-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("claim-bounty-broadcast"))));
        // 8 no-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-bounty"))));
        // 9 check-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("check-bounty"))));
        // 10 list-setter
        speakings.add(color(Objects.requireNonNull(configuration.getString("list-setter"))));
        // 11 list-total
        speakings.add(color(Objects.requireNonNull(configuration.getString("list-total"))));
        // 12 offline-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("offline-bounty"))));
        // 13 bounty-item-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-item-name"))));
        // 14 success-remove-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("success-remove-bounty"))));
        // 15 success-edit-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("success-edit-bounty"))));
        // 16 no-setter
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-setter"))));
        // 17 repeat-command-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("repeat-command-bounty"))));
        // 18 permanent-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("permanent-immunity"))));
        // 19 scaling-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("scaling-immunity"))));
        // 20 buy-permanent-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("buy-permanent-immunity"))));
        // 21 buy-scaling-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("buy-scaling-immunity"))));
        // 22 grace-period
        speakings.add(color(Objects.requireNonNull(configuration.getString("grace-period"))));
        // 23 min-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("min-bounty"))));
        // 24 unknown-command
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-command"))));
        // 25 already-bought-perm
        speakings.add(color(Objects.requireNonNull(configuration.getString("already-bought-perm"))));
        // 26 removed-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("removed-immunity"))));
        // 27 removed-other-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("removed-other-immunity"))));
        // 28 no-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-immunity"))));
        // 29 no-immunity-other
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-immunity-other"))));
        // 30 repeat-command-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("repeat-command-immunity"))));
        // 31 expired-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("expired-bounty"))));
        // 32 bounty-tracker-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-tracker-name"))));
        // 33 tracker-give
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracker-give"))));
        // 34 tracker-receive
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracker-receive"))));
        // 35 gui-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("gui-name"))));
        // 36 bounty-top
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-top"))));
        // 37 bounty-top-title
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-top-title"))));
        // 38 enable-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("enable-broadcast"))));
        // 39 disable-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("disable-broadcast"))));
        // 40 bounty-voucher-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-voucher-name"))));
        // 41 redeem-voucher
        speakings.add(color(Objects.requireNonNull(configuration.getString("redeem-voucher"))));

        voucherLore.clear();
        for (String str : configuration.getStringList("bounty-voucher-lore")){
            voucherLore.add(color(str));
        }
        headLore.clear();
        for (String str : configuration.getStringList("bounty-item-lore")) {
            headLore.add(color(str));
        }
        trackerLore.clear();
        for (String str : configuration.getStringList("bounty-tracker-lore")) {
            trackerLore.add(color(str));
        }

        papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        this.reloadConfig();

        if (!this.getConfig().isSet("currency")) {
            this.getConfig().set("currency", "DIAMOND");
        }
        if (!this.getConfig().isSet("minimum-bounty")) {
            this.getConfig().set("minimum-bounty", 1);
        }
        if (!this.getConfig().isSet("bounty-tax")) {
            this.getConfig().set("bounty-tax", 0.0);
        }
        if (!this.getConfig().isSet("add-currency-commands")) {
            this.getConfig().set("add-currency-commands", new ArrayList<String>());
        }
        if (!this.getConfig().isSet("remove-currency-commands")) {
            this.getConfig().set("remove-currency-commands", new ArrayList<String>());
        }
        if (!this.getConfig().isSet("bounty-expire")) {
            this.getConfig().set("bounty-expire", -1);
        }
        if (this.getConfig().isBoolean("reward-heads")){
            boolean prevOption = this.getConfig().getBoolean("reward-heads");
            this.getConfig().set("reward-heads", null);
            this.getConfig().set("reward-heads.setters", prevOption);
        }
        if (!this.getConfig().isSet("reward-heads.setters")) {
            this.getConfig().set("reward-heads.setters", false);
        }
        if (!this.getConfig().isSet("reward-heads.claimed")) {
            this.getConfig().set("reward-heads.claimed", false);
        }
        if (!this.getConfig().isSet("buy-own-bounties.enabled")) {
            this.getConfig().set("buy-own-bounties.enabled", false);
        }
        if (!this.getConfig().isSet("buy-own-bounties.cost-multiply")) {
            this.getConfig().set("buy-own-bounties.cost-multiply", 1.25);
        }
        if (!this.getConfig().isSet("buy-own-bounties.lore-addition")) {
            this.getConfig().set("buy-own-bounties.lore-addition", "&9Left Click &7to buy back for &a{amount}");
        }
        if (!this.getConfig().isSet("immunity.buy-immunity")) {
            this.getConfig().set("immunity.buy-immunity", false);
        }
        if (!this.getConfig().isSet("immunity.permanent-immunity.enabled")) {
            this.getConfig().set("immunity.permanent-immunity.enabled", false);
        }
        if (!this.getConfig().isSet("immunity.permanent-immunity.cost")) {
            this.getConfig().set("immunity.permanent-immunity.cost", 128);
        }
        if (!this.getConfig().isSet("immunity.scaling-immunity.ratio")) {
            this.getConfig().set("immunity.scaling-immunity.ratio", 1.0);
        }
        if (!this.getConfig().isSet("immunity.grace-period")) {
            this.getConfig().set("immunity.grace-period", 10);
        }
        if (!this.getConfig().isSet("advanced-gui")) {
            this.getConfig().set("advanced-gui.custom-items.fill.material", "GRAY_STAINED_GLASS_PANE");
            this.getConfig().set("advanced-gui.custom-items.fill.amount", 1);
            this.getConfig().set("advanced-gui.custom-items.fill.name", "&r");
            this.getConfig().set("advanced-gui.custom-items.fill.custom-model-data", 10);
            this.getConfig().set("advanced-gui.custom-items.fill.lore", new ArrayList<String>());
            this.getConfig().set("advanced-gui.custom-items.fill.enchanted", false);
            this.getConfig().set("advanced-gui.custom-items.fill.commands", new ArrayList<String>());
            this.getConfig().set("advanced-gui.bounty-slots", Collections.singletonList("0-44"));
            this.getConfig().set("advanced-gui.layout.1.item", "fill");
            this.getConfig().set("advanced-gui.layout.1.slot", "45-53");
            this.getConfig().set("advanced-gui.layout.2.item", "exit");
            this.getConfig().set("advanced-gui.layout.2.slot", "49");
            this.getConfig().set("advanced-gui.layout.3.item", "back");
            this.getConfig().set("advanced-gui.layout.3.slot", "45");
            this.getConfig().set("advanced-gui.layout.4.item", "next");
            this.getConfig().set("advanced-gui.layout.4.slot", "53");
            this.getConfig().set("advanced-gui.size", 54);
        }
        if (!this.getConfig().isSet("advanced-gui.bounty-slots")) {
            this.getConfig().set("advanced-gui.bounty-slots", Collections.singletonList("0-44"));
        }
        if (!this.getConfig().isSet("advanced-gui.size")) {
            this.getConfig().set("advanced-gui.size", 54);
        }
        if (!this.getConfig().isSet("bounty-tracker.enabled")) {
            this.getConfig().set("bounty-tracker.enabled", true);
        }
        if (!this.getConfig().isSet("bounty-tracker.remove")) {
            this.getConfig().set("bounty-tracker.remove", 2);
        }
        if (!this.getConfig().isSet("bounty-tracker.glow")) {
            this.getConfig().set("bounty-tracker.glow", 10);
        }
        if (!this.getConfig().isSet("bounty-tracker.action-bar.enabled")) {
            this.getConfig().set("bounty-tracker.action-bar.enabled", true);
        }
        if (!this.getConfig().isSet("bounty-tracker.action-bar.show-always")) {
            this.getConfig().set("bounty-tracker.action-bar.show-always", true);
        }
        if (!this.getConfig().isSet("bounty-tracker.action-bar.player-name")) {
            this.getConfig().set("bounty-tracker.action-bar.player-name", true);
        }
        if (!this.getConfig().isSet("bounty-tracker.action-bar.distance")) {
            this.getConfig().set("bounty-tracker.action-bar.distance", true);
        }
        if (!this.getConfig().isSet("bounty-tracker.action-bar.position")) {
            this.getConfig().set("bounty-tracker.action-bar.position", false);
        }
        if (!this.getConfig().isSet("bounty-tracker.action-bar.world")) {
            this.getConfig().set("bounty-tracker.action-bar.world", false);
        }
        if (!this.getConfig().isSet("redeem-reward-later")) {
            this.getConfig().set("redeem-reward-later", false);
        }
        if (!this.getConfig().isSet("advanced-gui.sort-type")) {
            this.getConfig().set("advanced-gui.sort-type", 2);
        }

        this.saveConfig();

        usingPapi = Objects.requireNonNull(this.getConfig().getString("currency")).contains("%");
        currency = Objects.requireNonNull(this.getConfig().getString("currency"));
        addCommands = this.getConfig().getStringList("add-currency-commands");
        removeCommands = this.getConfig().getStringList("remove-currency-commands");
        bountyExpire = this.getConfig().getInt("bounty-expire");
        rewardHeadSetter = this.getConfig().getBoolean("reward-heads.setters");
        rewardHeadClaimed = this.getConfig().getBoolean("reward-heads.claimed");
        buyBack = this.getConfig().getBoolean("buy-own-bounties.enabled");
        buyBackInterest = this.getConfig().getDouble("buy-own-bounties.cost-multiply");
        buyBackLore = color(Objects.requireNonNull(this.getConfig().getString("buy-own-bounties.lore-addition")));
        buyImmunity = this.getConfig().getBoolean("immunity.buy-immunity");
        permanentImmunity = this.getConfig().getBoolean("immunity.permanent-immunity.enabled");
        permanentCost = this.getConfig().getInt("immunity.permanent-immunity.cost");
        scalingRatio = this.getConfig().getDouble("immunity.scaling-immunity.ratio");
        graceTime = this.getConfig().getInt("immunity.grace-period");
        minBounty = this.getConfig().getInt("minimum-bounty");
        bountyTax = this.getConfig().getDouble("bounty-tax");
        guiSize = this.getConfig().getInt("advanced-gui.size");
        tracker = this.getConfig().getBoolean("bounty-tracker.enabled");
        trackerRemove = this.getConfig().getInt("bounty-tracker.remove");
        trackerGlow = this.getConfig().getInt("bounty-tracker.glow");
        trackerActionBar = this.getConfig().getBoolean("bounty-tracker.action-bar.enabled");
        TABShowAlways = this.getConfig().getBoolean("bounty-tracker.action-bar.show-always");
        TABPlayerName = this.getConfig().getBoolean("bounty-tracker.action-bar.player-name");
        TABDistance = this.getConfig().getBoolean("bounty-tracker.action-bar.distance");
        TABPosition = this.getConfig().getBoolean("bounty-tracker.action-bar.position");
        TABWorld = this.getConfig().getBoolean("bounty-tracker.action-bar.world");
        redeemRewardLater = this.getConfig().getBoolean("redeem-reward-later");
        menuSorting = this.getConfig().getInt("advanced-gui.sort-type");

        customItems.clear();
        itemCommands.clear();
        bountySlots.clear();
        layout.clear();

        for (String bSlots : this.getConfig().getStringList("advanced-gui.bounty-slots")) {
            if (bSlots.contains("-")) {
                int num1;
                try {
                    num1 = Integer.parseInt(bSlots.substring(0, bSlots.indexOf("-")));
                } catch (NumberFormatException ignored) {
                    continue;
                }
                int num2;
                try {
                    num2 = Integer.parseInt(bSlots.substring(bSlots.indexOf("-") + 1));
                } catch (NumberFormatException ignored) {
                    continue;
                }
                for (int i = Math.min(num1, num2); i < Math.max(num1, num2) + 1; i++) {
                    bountySlots.add(i);
                }
            } else {
                try {
                    bountySlots.add(Integer.parseInt(bSlots));
                } catch (NumberFormatException ignored) {

                }
            }
        }

        if (this.getConfig().isConfigurationSection("advanced-gui.layout"))
            for (String key : Objects.requireNonNull(this.getConfig().getConfigurationSection("advanced-gui.layout")).getKeys(false)) {
                String item = this.getConfig().getString("advanced-gui.layout." + key + ".item");
                //Bukkit.getLogger().info(item);
                if (this.getConfig().isConfigurationSection("advanced-gui.custom-items." + item)) {
                    Material material = Material.STONE;
                    try {
                        material = Material.valueOf(Objects.requireNonNull(this.getConfig().getString("advanced-gui.custom-items." + item + ".material")).toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException | NullPointerException ignored) {

                    }
                    int amount = 1;
                    try {
                        amount = this.getConfig().getInt("advanced-gui.custom-items." + item + ".amount");
                    } catch (NullPointerException ignored) {

                    }
                    ItemStack itemStack = new ItemStack(material, amount);
                    ItemMeta meta = itemStack.getItemMeta();
                    assert meta != null;
                    if (this.getConfig().isSet("advanced-gui.custom-items." + item + ".name")) {
                        meta.setDisplayName(this.getConfig().getString("advanced-gui.custom-items." + item + ".name"));
                    }
                    if (this.getConfig().isSet("advanced-gui.custom-items." + item + ".custom-model-data")) {
                        meta.setCustomModelData(this.getConfig().getInt("advanced-gui.custom-items." + item + ".custom-model-data"));
                    }
                    if (this.getConfig().isSet("advanced-gui.custom-items." + item + ".lore")) {
                        meta.setLore(this.getConfig().getStringList("advanced-gui.custom-items." + item + ".lore"));
                    }
                    if (this.getConfig().isSet("advanced-gui.custom-items." + item + ".enchanted")) {
                        if (this.getConfig().getBoolean("advanced-gui.custom-items." + item + ".enchanted")) {
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        }
                    }
                    itemStack.setItemMeta(meta);
                    if (this.getConfig().isSet("advanced-gui.custom-items." + item + ".enchanted")) {
                        if (this.getConfig().getBoolean("advanced-gui.custom-items." + item + ".enchanted")) {
                            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                        }
                    }
                    customItems.add(itemStack);
                    if (this.getConfig().isSet("advanced-gui.custom-items." + item + ".commands")) {
                        itemCommands.add(this.getConfig().getStringList("advanced-gui.custom-items." + item + ".commands"));
                    } else {
                        itemCommands.add(new ArrayList<>());
                    }
                    layout.add(new String[]{(customItems.size() - 1) + "", this.getConfig().getString("advanced-gui.layout." + key + ".slot")});
                } else {
                    layout.add(new String[]{item, this.getConfig().getString("advanced-gui.layout." + key + ".slot")});
                }
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


    public void openGUI(Player player, int page) {
        Inventory bountyInventory = Bukkit.createInventory(player, guiSize, speakings.get(35) + " " + (page + 1));
        ItemStack[] contents = bountyInventory.getContents();
        for (String[] itemInfo : layout) {
            ItemStack itemStack;
            if (itemInfo[0].equalsIgnoreCase("exit")) {
                itemStack = item.get("exit");
            } else if (itemInfo[0].equalsIgnoreCase("next")) {
                if (bountyList.size() > (page * bountySlots.size()) + bountySlots.size()) {
                    itemStack = item.get("next");
                } else {
                    itemStack = null;
                }
            } else if (itemInfo[0].equalsIgnoreCase("back")) {
                if (page > 0) {
                    itemStack = item.get("back");
                } else {
                    itemStack = null;
                }
            } else {
                try {
                    itemStack = customItems.get(Integer.parseInt(itemInfo[0]));
                    itemStack = formatItem(itemStack, player);
                } catch (NumberFormatException ignored) {
                    itemStack = new ItemStack(Material.STONE);
                }
            }
            if (itemStack != null)
                if (itemInfo[1] != null)
                    if (itemInfo[1].contains("-")) {
                        int num1;
                        try {
                            num1 = Integer.parseInt(itemInfo[1].substring(0, itemInfo[1].indexOf("-")));
                        } catch (NumberFormatException ignored) {
                            continue;
                        }
                        int num2;
                        try {
                            num2 = Integer.parseInt(itemInfo[1].substring(itemInfo[1].indexOf("-") + 1));
                        } catch (NumberFormatException ignored) {
                            continue;
                        }
                        for (int i = Math.min(num1, num2); i < Math.max(num1, num2) + 1; i++) {
                            contents[i] = itemStack;
                        }
                    } else {
                        try {
                            contents[Integer.parseInt(itemInfo[1])] = itemStack;
                        } catch (NumberFormatException ignored) {

                        }
                    }
        }
        List<Bounty> sortedList = sortBounties();
        for (int i = page * bountySlots.size(); i < (page * bountySlots.size()) + bountySlots.size(); i++) {
            if (sortedList.size() > i) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                assert meta != null;
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(sortedList.get(i).getUUID())));
                if (papiEnabled) {
                    meta.setDisplayName(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(sortedList.get(i).getUUID())), parse(speakings.get(13), sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null)));
                } else {
                    meta.setDisplayName(parse(speakings.get(13), sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null));
                }
                ArrayList<String> lore = new ArrayList<>();
                for (String str : headLore) {
                    if (papiEnabled) {
                        lore.add(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(UUID.fromString(sortedList.get(i).getUUID())), parse(str, sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null)));
                    } else {
                        lore.add(parse(str, sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), null));
                    }
                }


                if (player.hasPermission("notbounties.admin")) {
                    lore.add(ChatColor.RED + "Left Click" + ChatColor.GRAY + " to Remove");
                    lore.add(ChatColor.YELLOW + "Right Click" + ChatColor.GRAY + " to Edit");
                    lore.add("");
                    //lore.add(ChatColor.BLACK + "" + i);
                } else {
                    if (buyBack) {
                        if (sortedList.get(i).getUUID().equals(player.getUniqueId().toString())) {
                            lore.add(parse(buyBackLore, (int) (sortedList.get(i).getTotalBounty() * buyBackInterest), player));
                            lore.add("");
                        }
                    }
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
                contents[bountySlots.get(i - page * bountySlots.size())] = skull;
            } else {
                break;
            }
        }

        bountyInventory.setContents(contents);
        player.openInventory(bountyInventory);
    }

    public List<Bounty> sortBounties(){
        // how bounties are sorted
        List<Bounty> sortedList = new ArrayList<>(bountyList);
        Bounty temp;
        for (int i = 0; i < sortedList.size(); i++) {
            for (int j = i + 1; j < sortedList.size(); j++) {
                if((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && menuSorting == 0) || // oldest bounties at top
                        (sortedList.get(i).getSetters().get(0).getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && menuSorting == 1) || // newest bounties at top
                        (sortedList.get(i).getTotalBounty() < sortedList.get(j).getTotalBounty() && menuSorting == 2) || // more expensive bounties at top
                        (sortedList.get(i).getTotalBounty() > sortedList.get(j).getTotalBounty() && menuSorting == 3)) { // less expensive bounties at top
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
            if (removeCommands == null){
                Bukkit.getLogger().warning("[NotBounties] We detected a placeholder as currency, but there are no remove commands to take away money! (Is it formatted correctly?)");
            }
            if (removeCommands.isEmpty()){
                Bukkit.getLogger().warning("[NotBounties] We detected a placeholder as currency, but there are no remove commands to take away money! (Is it formatted correctly?)");
            }
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

        List<Bounty> sortedList = sortBounties();
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
        if (bountyList.size() > (page * length) + length) {
            start.addExtra(rightArrow);
        } else {
            start.addExtra(replacement);
        }
        sender.spigot().sendMessage(start);
        //sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                        ");
    }

    public void doAddCommands(Player p, int amount) {
            if (usingPapi) {
                if (addCommands == null){
                    Bukkit.getLogger().warning("[NotBounties] We detected a placeholder as currency, but there are no add commands to give players there reward! (Is it formatted correctly?)");
                }
                if (addCommands.isEmpty()){
                    Bukkit.getLogger().warning("[NotBounties] We detected a placeholder as currency, but there are no add commands to give players there reward! (Is it formatted correctly?)");
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

    public int checkAmount(Player player, Material material) {
        int amount = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack content : contents) {
            if (content != null) {
                if (content.getType().equals(material)) {
                    amount += content.getAmount();
                }
            }
        }
        return amount;
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

    public void addItem(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                if (amount > material.getMaxStackSize()) {
                    contents[i] = new ItemStack(material, material.getMaxStackSize());
                    amount -= material.getMaxStackSize();
                } else {
                    contents[i] = new ItemStack(material, amount);
                    amount = 0;
                    break;
                }
            }
        }
        if (amount > 0) {
            player.getWorld().dropItem(player.getLocation(), new ItemStack(material, amount));
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
        allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);

        for (Bounty bounty : bountyList) {
            // if they have a bounty already
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                bounty.addBounty(setter, amount);
                setter.sendMessage(parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, receiver));
                String message = parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, bounty.getTotalBounty(), receiver);
                Bukkit.getConsoleSender().sendMessage(message);
                for (Player player : Bukkit.getOnlinePlayers()){
                    if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                        player.sendMessage(message);
                    }
                }
                return;
            }
        }
        // create new bounty
        bountyList.add(new Bounty(setter, receiver, amount));
        setter.sendMessage(parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, receiver));
        String message = parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, amount, receiver);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()){
            if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                player.sendMessage(message);
            }
        }
    }

    public void addBounty(Player receiver, int amount) {
        // add to all time bounties
        allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);
        for (Bounty bounty : bountyList) {
            // if they have a bounty already
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                bounty.addBounty(amount);
                String message = parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, bounty.getTotalBounty(), receiver);
                Bukkit.getConsoleSender().sendMessage(message);
                for (Player player : Bukkit.getOnlinePlayers()){
                    if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                        player.sendMessage(message);
                    }
                }

                return;
            }
        }
        // create new bounty
        bountyList.add(new Bounty(receiver, amount));
        String message = parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, amount, receiver);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()){
            if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                player.sendMessage(message);
            }
        }
    }

    public void addBounty(Player setter, OfflinePlayer receiver, int amount) {
        // add to all time bounties
        allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);
        for (Bounty bounty : bountyList) {
            // if they have a bounty already
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                bounty.addBounty(setter, amount);
                bounty.setNotified(false);
                if (papiEnabled) {
                    setter.sendMessage(PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, null)));
                } else {
                    setter.sendMessage(parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, null));
                }
                String message = papiEnabled ? PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, amount, null)) : parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, bounty.getTotalBounty(), null);
                Bukkit.getConsoleSender().sendMessage(message);
                for (Player player : Bukkit.getOnlinePlayers()){
                    if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                        player.sendMessage(message);
                    }
                }
                return;
            }
        }
        // create new bounty
        bountyList.add(new Bounty(setter, receiver, amount));
        if (papiEnabled) {
            setter.sendMessage(PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, null)));
        } else {
            setter.sendMessage(parse(speakings.get(0) + speakings.get(2), receiver.getName(), amount, null));
        }
        String message = papiEnabled ? PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, amount, null)) : parse(speakings.get(0) + speakings.get(4), setter.getName(), receiver.getName(), amount, amount, null);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()){
            if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                player.sendMessage(message);
            }
        }
    }

    public void addBounty(OfflinePlayer receiver, int amount) {
        // add to all time bounties
        allTimeBounty.replace(receiver.getUniqueId().toString(), allTimeBounty.get(receiver.getUniqueId().toString()) + amount);
        for (Bounty bounty : bountyList) {
            // if they have a bounty already
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                bounty.addBounty(amount);
                bounty.setNotified(false);
                String message = papiEnabled ? PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, bounty.getTotalBounty(), null)) : parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, bounty.getTotalBounty(), null);
                Bukkit.getConsoleSender().sendMessage(message);
                for (Player player : Bukkit.getOnlinePlayers()){
                    if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                        player.sendMessage(message);
                    }
                }
                return;
            }
        }
        // create new bounty
        bountyList.add(new Bounty(receiver, amount));
        String message = papiEnabled ? PlaceholderAPI.setPlaceholders(receiver, parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, amount, null)) : parse(speakings.get(0) + speakings.get(4), "CONSOLE", receiver.getName(), amount, amount, null);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()){
            if (!disableBroadcast.contains(player.getUniqueId().toString()) || player.getUniqueId().equals(receiver.getUniqueId())){
                player.sendMessage(message);
            }
        }
    }

    public boolean hasBounty(Player receiver) {
        for (Bounty bounty : bountyList) {
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBounty(OfflinePlayer receiver) {
        for (Bounty bounty : bountyList) {
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                return true;
            }
        }
        return false;
    }

    public Bounty getBounty(Player receiver) {
        for (Bounty bounty : bountyList) {
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                return bounty;
            }
        }
        return null;
    }

    public Bounty getBounty(OfflinePlayer receiver) {
        for (Bounty bounty : bountyList) {
            if (bounty.getUUID().equals(receiver.getUniqueId().toString())) {
                return bounty;
            }
        }
        return null;
    }

    public String parse(String str, String player, Player receiver) {
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{player}")) {
            str = str.replace("{player}", player);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public String parse(String str, Player receiver) {
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public String parse(String str, int amount, Player receiver) {
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", amount + "");
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public String parse(String str, String player, int amount, Player receiver) {
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{player}")) {
            str = str.replace("{player}", player);
        }
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", amount + "");
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public String parse(String str, String sender, String player, int amount, Player receiver) {
        while (str.contains("{player}")) {
            str = str.replace("{player}", sender);
        }
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", amount + "");
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public String parse(String str, String sender, String player, Player receiver) {
        while (str.contains("{player}")) {
            str = str.replace("{player}", sender);
        }
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }


    public String parse(String str, String sender, String player, int amount, int totalBounty, Player receiver) {
        while (str.contains("{player}")) {
            str = str.replace("{player}", sender);
        }
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", amount + "");
        }
        while (str.contains("{bounty}")) {
            str = str.replace("{bounty}", totalBounty + "");
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }


    public String color(String str) {
        str = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', str);
        return translateHexColorCodes("&#", "", str);
    }

    public String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
}

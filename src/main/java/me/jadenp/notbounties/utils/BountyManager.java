package me.jadenp.notbounties.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.bountyEvents.BountyClaimEvent;
import me.jadenp.notbounties.bountyEvents.BountySetEvent;
import me.jadenp.notbounties.sql.MySQL;
import me.jadenp.notbounties.sql.SQLGetter;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.Commands;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.configuration.autoBounties.MurderBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.RandomBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.*;

public class BountyManager {
    public static MySQL SQL;
    public static SQLGetter data;
    public static final Map<UUID, Double> refundedBounties = new HashMap<>();
    public static final Map<UUID, List<ItemStack>> refundedItems = new HashMap<>();
    public static final Map<UUID, Double> immunitySpent = new HashMap<>();
    public static final Map<UUID, Double> killBounties = new HashMap<>();
    public static final Map<UUID, Double> setBounties = new HashMap<>();
    public static final Map<UUID, Double> deathBounties = new HashMap<>();
    public static final Map<UUID, Double> allTimeBounties = new HashMap<>();
    public static final Map<UUID, Double> allClaimedBounties = new HashMap<>();
    public static final Map<UUID, List<RewardHead>> headRewards = new HashMap<>();
    private static final List<Bounty> bountyList = new ArrayList<>();

    public static void loadBounties(){
        SQL = new MySQL(NotBounties.getInstance());
        data = new SQLGetter();
        File bounties = new File(NotBounties.getInstance().getDataFolder() + File.separator + "bounties.yml");


        // create bounties file if one doesn't exist
        try {
            if (bounties.createNewFile()) {
                Bukkit.getLogger().info("[NotBounties] Created new storage file.");
            } else {
                // get existing bounties file
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(bounties);
                // add all previously logged on players to a map
                int i = 0;
                while (configuration.getString("logged-players." + i + ".name") != null) {
                    loggedPlayers.put(Objects.requireNonNull(configuration.getString("logged-players." + i + ".name")).toLowerCase(Locale.ROOT), UUID.fromString(Objects.requireNonNull(configuration.getString("logged-players." + i + ".uuid"))));
                    i++;
                }
                immunePerms = configuration.isSet("immune-permissions") ? configuration.getStringList("immune-permissions") : new ArrayList<>();
                autoImmuneMurderPerms = configuration.isSet("immunity-murder") ? configuration.getStringList("immunity-murder") : new ArrayList<>();
                autoImmuneRandomPerms = configuration.isSet("immunity-random") ? configuration.getStringList("immunity-random") : new ArrayList<>();
                autoImmuneTimedPerms = configuration.isSet("immunity-timed") ? configuration.getStringList("immunity-timed") : new ArrayList<>();
                // go through bounties in file
                i = 0;
                while (configuration.getString("bounties." + i + ".uuid") != null) {
                    List<Setter> setters = new ArrayList<>();
                    int l = 0;
                    while (configuration.getString("bounties." + i + "." + l + ".uuid") != null) {
                        List<String> whitelistUUIDs = new ArrayList<>();
                        if (configuration.isSet("bounties." + i + "." + l + ".whitelist"))
                            whitelistUUIDs = configuration.getStringList("bounties." + i + "." + l + ".whitelist");
                        boolean blacklist = configuration.isSet("bounties." + i + "." + l + ".blacklist") && configuration.getBoolean("bounties." + i + "." + l + ".blacklist");

                        List<UUID> convertedUUIDs = new ArrayList<>();
                        for (String uuid : whitelistUUIDs)
                            convertedUUIDs.add(UUID.fromString(uuid));
                        // check for old CONSOLE UUID
                        UUID setterUUID = Objects.requireNonNull(configuration.getString("bounties." + i + "." + l + ".uuid")).equalsIgnoreCase("CONSOLE") ? new UUID(0, 0) : UUID.fromString(Objects.requireNonNull(configuration.getString("bounties." + i + "." + l + ".uuid")));
                        // check for no playtime
                        long playTime = configuration.isSet("bounties." + i + "." + l + ".playtime") ? configuration.getLong("bounties." + i + "." + l + ".playtime") : 0;
                        ArrayList<ItemStack> items = configuration.isString("bounties." + i + "." + l + ".items") ? new ArrayList<>(List.of(SerializeInventory.itemStackArrayFromBase64(configuration.getString("bounties." + i + "." + l + ".items")))) : new ArrayList<>();
                        Setter setter = new Setter(configuration.getString("bounties." + i + "." + l + ".name"), setterUUID, configuration.getDouble("bounties." + i + "." + l + ".amount"), items, configuration.getLong("bounties." + i + "." + l + ".time-created"), configuration.getBoolean("bounties." + i + "." + l + ".notified"), new Whitelist(convertedUUIDs, blacklist), playTime);
                        setters.add(setter);
                        l++;
                    }
                    if (!setters.isEmpty()) {
                        bountyList.add(new Bounty(UUID.fromString(Objects.requireNonNull(configuration.getString("bounties." + i + ".uuid"))), setters, configuration.getString("bounties." + i + ".name")));
                    }
                    i++;
                }
                // go through player logs - old version
                i = 0;
                while (configuration.isSet("data." + i + ".uuid")) {
                    UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString("data." + i + ".uuid")));
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
                            if (bounty.getUUID().equals(uuid)) {
                                allTimeBounties.put(uuid, bounty.getTotalDisplayBounty());
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
                        NotBounties.disableBroadcast.add(uuid);
                    }
                    i++;
                }
                // end old version ^^^
                // new version vvv
                Map<UUID, Long> timedBounties = new HashMap<>();
                if (configuration.isConfigurationSection("data"))
                    for (String uuidString : Objects.requireNonNull(configuration.getConfigurationSection("data")).getKeys(false)) {
                        UUID uuid = UUID.fromString(uuidString);
                        // old data protection
                        if (uuidString.length() < 10)
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
                        if (configuration.isSet("data." + uuid + ".next-bounty"))
                            timedBounties.put(uuid, configuration.getLong("data." + uuid + ".next-bounty"));
                        if (configuration.isSet("data." + uuid + ".bedrock-player"))
                            bedrockPlayers.put(uuid, configuration.getString("data." + uuid + ".bedrock-player"));
                        if (variableWhitelist && configuration.isSet("data." + uuid + ".whitelist"))
                            try {
                                playerWhitelist.put(uuid, new Whitelist(configuration.getStringList("data." + uuid + ".whitelist").stream().map(UUID::fromString).collect(Collectors.toList()), configuration.getBoolean("data." + uuid + ".blacklist")));
                            } catch (IllegalArgumentException e) {
                                Bukkit.getLogger().warning("Failed to get whitelisted uuids from: " + uuid + "\nThis list will be overwritten in 5 minutes");
                            }
                        if (configuration.isSet("data." + uuid + ".refund"))
                            refundedBounties.put(uuid, configuration.getDouble("data." + uuid + ".refund"));
                        if (configuration.isSet("data." + uuid + ".refund-items"))
                            try {
                                refundedItems.put(uuid, new ArrayList<>(Arrays.asList(SerializeInventory.itemStackArrayFromBase64(configuration.getString("data." + uuid + ".refund-items")))));
                            } catch (IOException e) {
                                Bukkit.getLogger().warning("[NotBounties] Unable to load item refund for player using this encoded data: " + configuration.getString("data." + uuid + ".refund-items"));
                                Bukkit.getLogger().warning(e.toString());
                            }
                        if (configuration.isSet("data." + uuid + "time-zone"))
                            LocalTime.addTimeZone(uuid, configuration.getString("data." + uuid + ".time-zone"));
                    }
                if (!timedBounties.isEmpty())
                    TimedBounties.setNextBounties(timedBounties);
                if (configuration.isSet("disable-broadcast"))
                    configuration.getStringList("disable-broadcast").forEach(s -> NotBounties.disableBroadcast.add(UUID.fromString(s)));

                i = 0;
                while (configuration.getString("head-rewards." + i + ".setter") != null) {
                    try {
                        List<RewardHead> rewardHeads = new ArrayList<>();
                        for (String str : configuration.getStringList("head-rewards." + i + ".uuid")) {
                            rewardHeads.add(decodeRewardHead(str));
                        }
                        headRewards.put(UUID.fromString(Objects.requireNonNull(configuration.getString("head-rewards." + i + ".setter"))), rewardHeads);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        Bukkit.getLogger().warning("Invalid UUID for head reward #" + i);
                    }
                    i++;
                }
                BiMap<Integer, UUID> trackedBounties = HashBiMap.create();
                i = 0;
                while (configuration.getString("tracked-bounties." + i + ".uuid") != null) {
                    try {
                        UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString("tracked-bounties." + i + ".uuid")));
                        if (hasBounty(uuid))
                            trackedBounties.put(configuration.getInt("tracked-bounties." + i + ".number"), uuid);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("[NotBounties] Could not convert tracked string to uuid: " + configuration.getString("tracked-bounties." + i + ".uuid"));
                    }
                    i++;
                }
                BountyTracker.setTrackedBounties(trackedBounties);

                if (configuration.isSet("next-random-bounty"))
                    RandomBounties.setNextRandomBounty(configuration.getLong("next-random-bounty"));
                if (!RandomBounties.isRandomBountiesEnabled()) {
                    RandomBounties.setNextRandomBounty(0);
                } else if (RandomBounties.getNextRandomBounty() == 0) {
                    RandomBounties.setNextRandomBounty();
                }
                if (configuration.isConfigurationSection("bounty-boards"))
                    for (String str : Objects.requireNonNull(configuration.getConfigurationSection("bounty-boards")).getKeys(false)) {
                        Location location;
                        if (configuration.isLocation("bounty-boards." + str + ".location"))
                            location = configuration.getLocation("bounty-boards." + str + ".location");
                        else
                            location = deserializeLocation(Objects.requireNonNull(configuration.getConfigurationSection("bounty-boards." + str + ".location")));
                        if (location == null)
                            continue;
                        bountyBoards.add(new BountyBoard(location, BlockFace.valueOf(configuration.getString("bounty-boards." + str + ".direction")), configuration.getInt("bounty-boards." + str + ".rank")));
                    }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(bountyList);
        tryToConnect();
    }


    /**
     * This method attempts to connect to the MySQL database.
     * If a connection is successful, local storage will be migrated if that option is enabled
     *
     * @return true if a connection was successful
     */
    public static boolean tryToConnect() {
        if (!SQL.isConnected()) {
            try {
                SQL.connect();
            } catch (SQLException e) {
                //e.printStackTrace();
                return false;
            }

            if (SQL.isConnected()) {
                Bukkit.getLogger().info("[NotBounties] Database is connected!");
                data.createTable();
                data.createDataTable();
                data.createOnlinePlayerTable();
                if (!bountyList.isEmpty() && migrateLocalData) {
                    Bukkit.getLogger().info("[NotBounties] Migrating local storage to database");
                    // add entries to database
                    for (Bounty bounty : bountyList) {
                        if (bounty.getTotalDisplayBounty() != 0) {
                            for (Setter setter : bounty.getSetters()) {
                                data.addBounty(bounty, setter);
                            }
                        }
                    }
                }
                Map<UUID, Double[]> stats = new HashMap<>();
                for (Map.Entry<UUID, Double> entry : killBounties.entrySet()) {
                    stats.put(entry.getKey(), new Double[]{entry.getValue(),0.0,0.0,0.0,0.0,0.0});
                }
                for (Map.Entry<UUID, Double> entry : setBounties.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
                    values[1] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : deathBounties.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
                    values[2] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : allClaimedBounties.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
                    values[3] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : allTimeBounties.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
                    values[4] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : immunitySpent.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
                    values[5] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                if (!stats.isEmpty() && migrateLocalData) {
                    // add entries to database
                    for (Map.Entry<UUID, Double[]> entry : stats.entrySet()) {
                        Double[] values = entry.getValue();
                        data.addData(String.valueOf(entry.getKey()), values[0].longValue(), values[1].longValue(), values[2].longValue(), values[3], values[4], values[5]);
                    }
                    killBounties.clear();
                    setBounties.clear();
                    deathBounties.clear();
                    allClaimedBounties.clear();
                    allTimeBounties.clear();
                    immunitySpent.clear();
                }
                Bukkit.getLogger().info("[NotBounties] Cleared up " + data.removeExtraData() + " unused rows in the database!");

                data.refreshOnlinePlayers();
            } else {
                return false;
            }
        }
        return true;
    }
    private static @Nullable Location deserializeLocation(ConfigurationSection configuration) {
        String worldUUID = configuration.getString("world");
        if (worldUUID == null)
            return null;
        double x = configuration.getDouble("x");
        double y = configuration.getDouble("y");
        double z = configuration.getDouble("z");
        double pitch = configuration.getDouble("pitch");
        double yaw = configuration.getDouble("yaw");
        try {
            World world = Bukkit.getWorld(UUID.fromString(worldUUID));
            if (world == null)
                return null;
            return new Location(world, x, y, z, (float) pitch, (float) yaw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public static List<Bounty> sortBounties(int sortType) {
        // how bounties are sorted
        List<Bounty> sortedList = new ArrayList<>(bountyList);
        if (sortType == -1)
            return sortedList;
        if (sortType == 2)
            return sortedList.reversed();
        if (sortType == 3)
            return sortedList;
        if (sortedList.isEmpty())
            return sortedList;
        Bounty temp;
        for (int i = 0; i < sortedList.size(); i++) {
            for (int j = i + 1; j < sortedList.size(); j++) {
                if ((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                        (sortedList.get(i).getSetters().get(0).getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 1)){// newest bounties at top
                    temp = sortedList.get(i);
                    sortedList.set(i, sortedList.get(j));
                    sortedList.set(j, temp);
                }
            }
        }
        return sortedList;
    }

    public static boolean hasBounty(UUID receiver) {
        return getBounty(receiver) != null;
    }

    private static final int length = 10;

    public static void listBounties(CommandSender sender, int page) {
        GUIOptions guiOptions = GUI.getGUI("bounty-gui");
        String title = "";
        if (guiOptions != null) {
            title = guiOptions.getName();
            if (guiOptions.isAddPage())
                title += " " + (page + 1);
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               " + ChatColor.RESET + " " + title + " " + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "               ");
        int sortType = Objects.requireNonNull(GUI.getGUI("bounty-gui")).getSortType();
        List<Bounty> sortedList = getPublicBounties(sortType);
        for (int i = page * length; i < (page * length) + length; i++) {
            if (sortedList.size() > i) {
                sender.sendMessage(parse(listTotal, sortedList.get(i).getName(), sortedList.get(i).getTotalDisplayBounty(), Bukkit.getOfflinePlayer(sortedList.get(i).getUUID())));
            } else {
                break;
            }
        }

        TextComponent rightArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋙⋙⋙");
        rightArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " list " + page + 2));
        rightArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(net.md_5.bungee.api.ChatColor.of(new Color(232, 26, 225)) + "Next Page")));
        TextComponent leftArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋘⋘⋘");
        leftArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " list " + page));
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


    public static void addBounty(Player setter, OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        double displayAmount = amount;
        // you can only set bounties by items or amount, if there are items, the amount is just the value of the items
        if (!items.isEmpty())
            amount = 0;
        // add to all time bounties
        BountySetEvent event = new BountySetEvent(new Bounty(setter, receiver, amount, items, whitelist));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            refundBounty(new Bounty(setter, receiver, amount + amount * bountyTax + bountyWhitelistCost * whitelist.getList().size(), items, whitelist));
            return;
        }
        // unlock recipies
        if (!setter.hasDiscoveredRecipe(BountyTracker.getBountyTrackerRecipe()))
            setter.discoverRecipe(BountyTracker.getBountyTrackerRecipe());

        if (SQL.isConnected()) {
            data.addData(receiver.getUniqueId().toString(), 0, 0, 0, displayAmount, 0, 0);
        } else {
            allTimeBounties.put(receiver.getUniqueId(), Leaderboard.ALL.getStat(receiver.getUniqueId()) + displayAmount);
        }
        Bounty bounty = insertBounty(setter, receiver, amount, items, whitelist);

        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            BigBounty.setBounty(onlineReceiver, bounty, displayAmount);
            // send messages
            onlineReceiver.sendMessage(parse(prefix + bountyReceiver, setter.getName(), displayAmount, bounty.getTotalDisplayBounty(), receiver));

            if (serverVersion <= 16) {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
            }
            // add wanted tag
            if (wanted && bounty.getTotalDisplayBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(onlineReceiver.getUniqueId())) {
                    NotBounties.wantedText.put(onlineReceiver.getUniqueId(), new AboveNameText(onlineReceiver));
                }
            }
        }
        // send messages
        setter.sendMessage(parse(prefix + bountySuccess, getPlayerName(receiver.getUniqueId()), displayAmount, bounty.getTotalDisplayBounty(), receiver));

        if (serverVersion <= 16) {
            setter.playSound(setter.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
        } else {
            setter.playSound(setter.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
        }

        String message = parse(prefix + bountyBroadcast, getPlayerName(receiver.getUniqueId()), setter.getName(), displayAmount, bounty.getTotalDisplayBounty(), receiver);

        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.getList().isEmpty()) {
            if (displayAmount >= minBroadcast)
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!NotBounties.disableBroadcast.contains(player.getUniqueId()) && !player.getUniqueId().equals(receiver.getUniqueId()) && !player.getUniqueId().equals(setter.getUniqueId())) {
                        player.sendMessage(message);
                    }
                }
        } else {
            if (whitelist.isBlacklist()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getUniqueId().equals(receiver.getUniqueId()) || player.getUniqueId().equals(setter.getUniqueId()) || whitelist.getList().contains(player.getUniqueId()))
                        continue;
                    whitelistNotify.stream().filter(str -> !str.isEmpty()).forEach(str -> player.sendMessage(parse(prefix + str, player)));
                }
            } else {
                for (UUID uuid : whitelist.getList()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || player.getUniqueId().equals(receiver.getUniqueId()) || player.getUniqueId().equals(setter.getUniqueId()))
                        continue;
                    player.sendMessage(message);
                    whitelistNotify.stream().filter(str -> !str.isEmpty()).forEach(str -> player.sendMessage(parse(prefix + str, player)));
                }
            }
        }


    }

    public static void addBounty(OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        double displayAmount = amount;
        // you can only set bounties by items or amount, if there are items, the amount is just the value of the items
        if (!items.isEmpty())
            amount = 0;
        // add to all time bounties
        BountySetEvent event = new BountySetEvent(new Bounty(receiver, amount, items, whitelist));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            refundBounty(new Bounty(receiver, amount + amount * bountyTax + bountyWhitelistCost * whitelist.getList().size(), items, whitelist));
            return;
        }


        if (SQL.isConnected()) {
            data.addData(receiver.getUniqueId().toString(), 0, 0, 0, displayAmount, 0, 0);
        } else {
            allTimeBounties.put(receiver.getUniqueId(), Leaderboard.ALL.getStat(receiver.getUniqueId()) + displayAmount);
        }
        Bounty bounty = insertBounty(null, receiver, amount, items, whitelist);

        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            BigBounty.setBounty(onlineReceiver, bounty, displayAmount);
            // send messages
            onlineReceiver.sendMessage(parse(prefix + bountyReceiver, consoleName, displayAmount, bounty.getTotalDisplayBounty(), receiver));

            if (serverVersion <= 16) {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
            }
            // add wanted tag
            if (wanted && bounty.getTotalDisplayBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(onlineReceiver.getUniqueId())) {
                    NotBounties.wantedText.put(onlineReceiver.getUniqueId(), new AboveNameText(onlineReceiver));
                }
            }
        }
        // send messages
        String message = parse(prefix + bountyBroadcast, getPlayerName(receiver.getUniqueId()), consoleName, displayAmount, bounty.getTotalDisplayBounty(), receiver);
        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.getList().isEmpty()) {
            if (displayAmount >= minBroadcast)
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!NotBounties.disableBroadcast.contains(player.getUniqueId()) && !player.getUniqueId().equals(receiver.getUniqueId())) {
                        player.sendMessage(message);
                    }
                }
        } else {
            for (UUID uuid : whitelist.getList()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || player.getUniqueId().equals(receiver.getUniqueId()))
                    continue;
                player.sendMessage(message);
                player.sendMessage(parse(prefix + whitelistToggle, player));
            }
        }

    }

    public static void updateBounty(Bounty bounty) {
        if (SQL.isConnected()) {
            if (bounty.getSetters().isEmpty() || bounty.getTotalDisplayBounty() < 0.01)
                data.removeBounty(bounty.getUUID());
            else
                data.updateBounty(bounty);
        } else {
            if (bounty.getSetters().isEmpty()  || bounty.getTotalDisplayBounty() < 0.01)
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

    public static Bounty getBounty(UUID receiver) {
        if (SQL.isConnected()) {
            return data.getBounty(receiver);
        } else {
            for (Bounty bounty : bountyList) {
                if (bounty.getUUID().equals(receiver)) {
                    return bounty;
                }
            }
        }
        return null;
    }
    public static void refundBounty(Bounty bounty) {
        for (Setter setter : bounty.getSetters()) {
            refundSetter(setter);
        }
    }

    public static void refundSetter(Setter setter) {
        refundPlayer(setter.getUuid(), setter.getAmount(), setter.getItems());
    }

    public static void refundPlayer(UUID uuid, double amount, List<ItemStack> items) {
        items = new ArrayList<>(items); // make the arraylist modifiable
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (amount > 0) {
            if (vaultEnabled && !overrideVault) {
                if (!NumberFormatting.getVaultClass().deposit(player, amount)) {
                    Bukkit.getLogger().warning("[NotBounties] Error depositing currency with vault!");
                    addRefund(player.getUniqueId(), amount);
                }
            } else {
                if (player.isOnline() && manualEconomy != ManualEconomy.PARTIAL && ((Plugin) NotBounties.getInstance()).isEnabled()) {
                    NumberFormatting.doAddCommands(player.getPlayer(), amount);
                } else {
                    addRefund(uuid, amount);
                }
            }
        }
        items.removeIf(Objects::isNull);
        if (!items.isEmpty() && manualEconomy == ManualEconomy.AUTOMATIC) {
                if (player.isOnline() && ((Plugin) NotBounties.getInstance()).isEnabled()) {
                    NumberFormatting.givePlayer(player.getPlayer(), items, false);
                } else {
                    addRefund(uuid, items);
                }
            }


    }

    private static void addRefund(UUID uuid, double amount) {
        if (refundedBounties.containsKey(uuid)){
            refundedBounties.replace(uuid, refundedBounties.get(uuid) + amount);
        } else {
            refundedBounties.put(uuid, amount);
        }
    }

    private static void addRefund(UUID uuid, List<ItemStack> items) {
        if (items.isEmpty())
            return;
        if (refundedItems.containsKey(uuid)){
            refundedItems.get(uuid).addAll(items);
        } else {
            refundedItems.put(uuid, items);
        }
    }

    public static List<Bounty> getPublicBounties(int sortType) {
        List<Bounty> bounties = getAllBounties(sortType);
        return bounties.stream().filter(bounty -> !hiddenNames.contains(bounty.getName())).collect(Collectors.toList());
    }

    /**
     * Get a copy of all bounties on the server.
     * @param sortType
     *   <p>-1 : unsorted</p>
     *   <p> 0 : newer bounties at top</p>
     *   <p> 1 : older bounties at top</p>
     *   <p> 2 : more expensive bounties at top</p>
     *   <p> 3 : less expensive bounties at top</p>
     * @return A copy of all bounties on the server.
     */
    public static List<Bounty> getAllBounties(int sortType) {
        // Check if the SQL object has been loaded
        // If not, no bounties should be loaded locally either
        if (SQL == null)
            return new ArrayList<>();
        return SQL.isConnected() ? data.getTopBounties(sortType) : sortBounties(sortType);
    }

    /**
     * Inserts a bounty into the sorted bountyList
     * @return The new bounty. This bounty will be the combination of all bounties on the same person.
     */
    public static Bounty insertBounty(@Nullable Player setter, @NotNull OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        Bounty prevBounty;
        Bounty newBounty = setter == null ? new Bounty(receiver, amount, items, whitelist) : new Bounty(setter, receiver, amount, items, whitelist);
        if (SQL.isConnected()) {
            prevBounty = data.getBounty(receiver.getUniqueId());
            data.addBounty(newBounty, newBounty.getLastSetter());
            if (prevBounty == null) {
                prevBounty = newBounty;
            } else {
                if (setter == null) {
                    prevBounty.addBounty(amount, items, whitelist);
                } else {
                    prevBounty.addBounty(new Setter(setter.getName(), setter.getUniqueId(), amount, items, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
                }
            }
        } else {
            prevBounty = getBounty(receiver.getUniqueId());
            if (prevBounty == null) {
                // insert new bounty for this player
                int index = Collections.binarySearch(bountyList, newBounty);
                if (index < 0) {
                    index = -index - 1;
                }
                bountyList.add(index, newBounty);
                prevBounty = newBounty;
            } else {
                // combine with previous bounty
                if (setter == null) {
                    prevBounty.addBounty(amount, items, whitelist);
                } else {
                    prevBounty.addBounty(new Setter(setter.getName(), setter.getUniqueId(), amount, items, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
                }
                Collections.sort(bountyList);
            }
        }
        return prevBounty;
    }


    public static void removeBounty(UUID uuid) {
        BountyTracker.stopTracking(uuid);
        for (Player player : Bukkit.getOnlinePlayers())
            BountyTracker.removeTracker(player);
        NotBounties.removeWantedTag(uuid);
        displayParticle.remove(uuid);
        if (SQL.isConnected()) {
            data.removeBounty(uuid);
        } else {
            bountyList.removeIf(bounty -> bounty.getUUID().equals(uuid));
        }
    }

    public static void removeSetters(UUID bountyUUID, List<Setter> setters) {
        Bounty bounty = getBounty(bountyUUID);
        if (bounty == null)
            return;
        bounty.getSetters().removeIf(setters::contains);
        if (bounty.getSetters().isEmpty())
            removeBounty(bounty.getUUID());
    }


    public static void claimBounty(@NotNull Player player, Player killer, List<ItemStack> drops, boolean forceEditDrops) {
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Received a bounty claim request.");
        Item droppedHead = null;
        if (rewardHeadAnyKill)
            droppedHead = player.getWorld().dropItemNaturally(player.getLocation(), Head.createPlayerSkull(player.getUniqueId(), SkinManager.getSkin(player.getUniqueId()).getUrl()));
        // possible remove this later when the other functions allow null killers
        if (killer == null)
            return;
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] " + killer.getName() + " killed " + player.getName());
        TimedBounties.onDeath(player);
        // check if a bounty can be claimed
        if (!BountyClaimRequirements.canClaim(player, killer)) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] An external plugin, world filter, or a shared team is preventing this bounty from being claimed.");
            return;
        }
        MurderBounties.killPlayer(player, killer);

        if (player == killer) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Player killed themself.");
            return;
        }

        // check if killer can steal a bounty
        if (hasBounty(killer.getUniqueId()) && stealBounties) {
            Bounty bounty = getBounty(killer.getUniqueId());
            assert bounty != null;
            Bounty stolenBounty = bounty.getBounty(player.getUniqueId());
            bounty.removeBounty(player.getUniqueId());
            // update the bounty
            updateBounty(bounty);
            if (!stolenBounty.getSetters().isEmpty()) {
                // bounty has been stolen
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Killer stole a bounty!");
                if (manualEconomy == ManualEconomy.AUTOMATIC) {
                    // give rewards
                    if (debug)
                        Bukkit.getLogger().info("[NotBountiesDebug] Giving stolen bounty.");
                    NumberFormatting.doAddCommands(killer, stolenBounty.getTotalBounty());
                    NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(), false);
                }
                // send messages
                killer.sendMessage(parse(prefix + LanguageOptions.stolenBounty, getPlayerName(player.getUniqueId()), stolenBounty.getTotalDisplayBounty(), killer));
                // send messages
                String message = parse(prefix + stolenBountyBroadcast, getPlayerName(killer.getUniqueId()), getPlayerName(player.getUniqueId()), stolenBounty.getTotalDisplayBounty(), bounty.getTotalDisplayBounty(), killer);
                Bukkit.getConsoleSender().sendMessage(message);
                if (stolenBounty.getTotalDisplayBounty() >= minBroadcast)
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!NotBounties.disableBroadcast.contains(p.getUniqueId()) && !p.getUniqueId().equals(killer.getUniqueId())) {
                            p.sendMessage(message);
                        }
                    }
                // play sound
                killer.getWorld().playSound(player.getLocation(), Sound.ENTITY_CAT_HISS, 1, 1);
            }
        }

        if (!hasBounty(player.getUniqueId())) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Player doesn't have a bounty.");
            return;
        }

        // check if it is a npc
        if (!npcClaim && killer.hasMetadata("NPC")) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] This is an NPC which bounty claiming is disabled for in the config.");
            return;
        }
        Bounty bounty = getBounty(player.getUniqueId());
        assert bounty != null;
        // check if killer can claim it
        if (bounty.getTotalDisplayBounty(killer) < 0.01) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] This bounty is too small!");
            return;
        }
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Bounty to be claimed: " + bounty.getTotalDisplayBounty(killer));
        BountyClaimEvent event1 = new BountyClaimEvent(killer, new Bounty(bounty));
        Bukkit.getPluginManager().callEvent(event1);
        if (event1.isCancelled()) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] The bounty event got canceled by an external plugin.");
            return;
        }
        PVPRestrictions.onBountyClaim(player);
        Bounty bountyCopy = new Bounty(bounty, killer.getUniqueId());

        List<Setter> claimedBounties = new ArrayList<>(bounty.getSetters());
        claimedBounties.removeIf(setter -> !setter.canClaim(killer));

        displayParticle.remove(player.getUniqueId());

        // broadcast message
        String message = parse(prefix + claimBountyBroadcast, player.getName(), killer.getName(), bounty.getTotalDisplayBounty(killer), player);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ((!NotBounties.disableBroadcast.contains(p.getUniqueId()) && bounty.getTotalDisplayBounty(killer) >= minBroadcast) || p.getUniqueId().equals(player.getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(killer).getUniqueId())) {
                p.sendMessage(message);
            }
        }
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Claim messages sent to all players.");

        // reward head
        RewardHead rewardHead = new RewardHead(player.getName(), player.getUniqueId(), bounty.getTotalDisplayBounty(killer));

        if (rewardHeadSetter) {
            for (Setter setter : claimedBounties) {
                if (!setter.getUuid().equals(new UUID(0, 0))) {
                    Player p = Bukkit.getPlayer(setter.getUuid());
                    if (p != null) {
                        if (!rewardHeadClaimed || !Objects.requireNonNull(killer).getUniqueId().equals(setter.getUuid())) {
                            NumberFormatting.givePlayer(p, rewardHead.getItem(), 1);
                            if (debug)
                                Bukkit.getLogger().info("[NotBountiesDebug] Gave "  + p.getName() + " a player skull for the bounty.");
                        }
                    } else {
                        if (headRewards.containsKey(setter.getUuid())) {
                            // I think this could be replaced with headRewards.get(setter.getUuid()).add(rewardHead)
                            List<RewardHead> heads = new ArrayList<>(headRewards.get(setter.getUuid()));
                            heads.add(rewardHead);
                            headRewards.replace(setter.getUuid(), heads);
                        } else {
                            headRewards.put(setter.getUuid(), Collections.singletonList(rewardHead));
                        }
                        if (debug)
                            Bukkit.getLogger().info("[NotBountiesDebug] Will give " + NotBounties.getPlayerName(setter.getUuid()) + " a player skull when they log on next for the bounty.");
                    }
                }
            }
        }
        if (rewardHeadClaimed) {
            if (droppedHead != null)
                droppedHead.remove();
            NumberFormatting.givePlayer(killer, rewardHead.getItem(), 1);
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Gave " + killer.getName() + " a player skull for the bounty.");
        }
        // death tax
        if (ConfigOptions.deathTax > 0 && NumberFormatting.manualEconomy != NumberFormatting.ManualEconomy.PARTIAL) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Removing " + bounty.getTotalDisplayBounty(killer) * ConfigOptions.deathTax + " currency for death tax");
            Map<Material, Long> removedItems = NumberFormatting.doRemoveCommands(player, bounty.getTotalDisplayBounty(killer) * ConfigOptions.deathTax, drops);
            if (!removedItems.isEmpty()) {
                // send message
                long totalLoss = 0;
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<Material, Long> entry : removedItems.entrySet()) {
                    builder.append(entry.getValue()).append("x").append(entry.getKey().toString()).append(", ");
                    totalLoss += entry.getValue();
                }
                builder.replace(builder.length() - 2, builder.length(), "");
                if (totalLoss > 0) {
                    if (debug)
                        Bukkit.getLogger().info("[NotBountiesDebug] Removing " + totalLoss + " currency for the death tax.");
                    player.sendMessage(parse(prefix + LanguageOptions.deathTax.replace("{items}", (builder.toString())), player));
                    // modify drops
                    if (forceEditDrops)
                        for (Map.Entry<Material, Long> entry : removedItems.entrySet())
                            NumberFormatting.removeItem(player, entry.getKey(), entry.getValue(), -1);
                    ListIterator<ItemStack> dropsIterator = drops.listIterator();
                    while (dropsIterator.hasNext()) {
                        ItemStack drop = dropsIterator.next();
                        if (removedItems.containsKey(drop.getType())) {
                            if (removedItems.get(drop.getType()) > drop.getAmount()) {
                                removedItems.replace(drop.getType(), removedItems.get(drop.getType()) - drop.getAmount());
                                dropsIterator.remove();
                            } else if (removedItems.get(drop.getType()) == drop.getAmount()) {
                                removedItems.remove(drop.getType());
                                dropsIterator.remove();
                            } else {
                                drop.setAmount((int) (drop.getAmount() - removedItems.get(drop.getType())));
                                dropsIterator.set(drop);
                                removedItems.remove(drop.getType());
                            }
                        }
                    }
                }
            }
        }
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Redeeming Reward: ");
        if (!redeemRewardLater) {
            if (NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL) {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Directly giving auto-bounty reward.");
                NumberFormatting.doAddCommands(killer, bounty.getBounty(new UUID(0,0)).getTotalBounty(killer));
            } else {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Directly giving total claimed bounty.");
                NumberFormatting.doAddCommands(killer, bounty.getTotalBounty(killer));
                if (manualEconomy == ManualEconomy.AUTOMATIC)
                    NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(killer), false); // give bountied items
            }
        } else {
            // will add these to a voucher later
            if (manualEconomy == ManualEconomy.AUTOMATIC)
                NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(killer), false); // give bountied items
            // give voucher
            if (NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL) {
                NumberFormatting.doAddCommands(killer, bounty.getBounty(new UUID(0,0)).getTotalBounty(killer));
            }
            if (RRLVoucherPerSetter) {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Handing out vouchers. ");
                // multiple vouchers
                for (Setter setter : bounty.getSetters()) {
                    if (!setter.canClaim(killer))
                        continue;
                    if (setter.getAmount() <= 0.01)
                        continue;
                    if (setter.getUuid().equals(new UUID(0,0)) && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL)
                        continue;
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    assert meta != null;
                    ArrayList<String> lore = new ArrayList<>();
                    for (String str : voucherLore) {
                        lore.add(parse(str.replace("{bounty}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalBounty(killer)) + NumberFormatting.currencySuffix)), player.getName(), Objects.requireNonNull(player).getName(),setter.getAmount(), player));
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.setDisplayName(parse(bountyVoucherName.replace("{bounty}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(bounty.getTotalBounty(killer)) + NumberFormatting.currencySuffix)), player.getName(), Objects.requireNonNull(killer).getName(), setter.getAmount(), player));
                    ArrayList<String> setterLore = new ArrayList<>(lore);
                    if (!RRLSetterLoreAddition.isEmpty()) {
                        setterLore.add(parse(RRLSetterLoreAddition, setter.getName(), setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                    setterLore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + setter.getAmount());
                    meta.setLore(setterLore);
                    item.setItemMeta(meta);
                    item.addUnsafeEnchantment(Enchantment.CHANNELING, 0);
                    NumberFormatting.givePlayer(killer, item, 1);
                }
            } else {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Handing out a voucher. ");
                // one voucher
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                assert meta != null;
                ArrayList<String> lore = new ArrayList<>();
                for (String str : voucherLore) {
                    lore.add(parse(str, player.getName(), Objects.requireNonNull(player.getKiller()).getName(), bounty.getTotalBounty(killer), player));
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.setDisplayName(parse(bountyVoucherName, player.getName(), Objects.requireNonNull(killer).getName(), bounty.getTotalBounty(killer), player));
                if (!RRLSetterLoreAddition.isEmpty()) {
                    for (Setter setter : bounty.getSetters()) {
                        if (!setter.canClaim(killer))
                            continue;
                        if (setter.getAmount() <= 0.01)
                            continue;
                        if (setter.getUuid().equals(new UUID(0,0)) && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL)
                            continue;
                        lore.add(parse(RRLSetterLoreAddition, setter.getName(), setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                }
                lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + bounty.getTotalBounty(killer));
                meta.setLore(lore);
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(Enchantment.CHANNELING, 0);
                NumberFormatting.givePlayer(killer, item, 1);
            }
        }
        if (SQL.isConnected()) {
            data.addData(player.getUniqueId().toString(), 0, 0, 1, bounty.getTotalDisplayBounty(killer), 0, 0);
            data.addData(killer.getUniqueId().toString(), 1, 0, 0, 0, 0, bounty.getTotalDisplayBounty(killer));
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Updated SQL Database.");
        } else {
            allTimeBounties.put(player.getUniqueId(), Leaderboard.ALL.getStat(player.getUniqueId()) + bounty.getTotalDisplayBounty(killer));
            killBounties.put(killer.getUniqueId(), Leaderboard.KILLS.getStat(killer.getUniqueId()) + 1);
            deathBounties.put(player.getUniqueId(), Leaderboard.DEATHS.getStat(player.getUniqueId()) + 1);
            allClaimedBounties.put(killer.getUniqueId(), Leaderboard.CLAIMED.getStat(killer.getUniqueId()) + bounty.getTotalDisplayBounty(killer));
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Updated local stats. ");
        }
        bounty.claimBounty(killer);
        updateBounty(bounty);

        if (bounty.getTotalDisplayBounty() < minWanted) {
            // remove bounty tag
            NotBounties.removeWantedTag(bounty.getUUID());
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Removed wanted tag. ");
        }

        for (Setter setter : claimedBounties) {
            if (!setter.getUuid().equals(new UUID(0, 0))) {
                if (SQL.isConnected()) {
                    data.addData(setter.getUuid().toString(), 0, 1, 0, 0, 0, 0);
                } else {
                    setBounties.put(setter.getUuid(), Leaderboard.SET.getStat(setter.getUuid()) + 1);
                }
                Player p = Bukkit.getPlayer(setter.getUuid());
                if (p != null) {
                    p.playSound(p.getEyeLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1, 1);
                }
            }
        }
        Immunity.startGracePeriod(player);
        BountyTracker.stopTracking(player.getUniqueId());
        for (Player p : Bukkit.getOnlinePlayers()) {
            BountyTracker.removeTracker(p);
        }
        GUI.reopenBountiesGUI();
        ActionCommands.executeBountyClaim(player, killer, bountyCopy);
    }

}

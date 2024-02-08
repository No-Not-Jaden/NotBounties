package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.autoBounties.RandomBounties;
import me.jadenp.notbounties.autoBounties.TimedBounties;
import me.jadenp.notbounties.bountyEvents.BountySetEvent;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.sql.MySQL;
import me.jadenp.notbounties.sql.SQLGetter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.ConfigOptions.*;
import static me.jadenp.notbounties.utils.LanguageOptions.*;
import static me.jadenp.notbounties.utils.NumberFormatting.*;

public class BountyManager {
    public static MySQL SQL;
    public static SQLGetter data;
    public static final Map<Integer, String> trackedBounties = new HashMap<>();
    public static final Map<UUID, Double> refundedBounties = new HashMap<>();
    public static final Map<UUID, Double> immunitySpent = new HashMap<>();
    public static final Map<UUID, Double> killBounties = new HashMap<>();
    public static final Map<UUID, Double> setBounties = new HashMap<>();
    public static final Map<UUID, Double> deathBounties = new HashMap<>();
    public static final Map<UUID, Double> allTimeBounties = new HashMap<>();
    public static final Map<UUID, Double> allClaimedBounties = new HashMap<>();
    public static final Map<UUID, List<RewardHead>> headRewards = new HashMap<>();
    public static final List<Bounty> bountyList = new ArrayList<>();

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
                immunePerms = configuration.getStringList("immune-permissions");
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
                        Setter setter = new Setter(configuration.getString("bounties." + i + "." + l + ".name"), setterUUID, configuration.getDouble("bounties." + i + "." + l + ".amount"), configuration.getLong("bounties." + i + "." + l + ".time-created"), configuration.getBoolean("bounties." + i + "." + l + ".notified"), new Whitelist(convertedUUIDs, blacklist), playTime);
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
                        if (variableWhitelist && configuration.isSet("data." + uuid + ".whitelist"))
                            try {
                                playerWhitelist.put(uuid, new Whitelist(configuration.getStringList("data." + uuid + ".whitelist").stream().map(UUID::fromString).collect(Collectors.toList()), configuration.getBoolean("data." + uuid + ".blacklist")));
                            } catch (IllegalArgumentException e) {
                                Bukkit.getLogger().warning("Failed to get whitelisted uuids from: " + uuid + "\nThis list will be overwritten in 5 minutes");
                            }
                        if (configuration.isSet("data." + uuid + ".refund"))
                            refundedBounties.put(uuid, configuration.getDouble("data." + uuid + ".refund"));

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
                i = 0;
                while (configuration.getString("tracked-bounties." + i + ".uuid") != null) {
                    trackedBounties.put(configuration.getInt("tracked-bounties." + i + ".number"), configuration.getString("tracked-bounties." + i + ".uuid"));
                    i++;
                }
                if (configuration.isSet("next-random-bounty"))
                    RandomBounties.setNextRandomBounty(configuration.getLong("next-random-bounty"));
                if (!RandomBounties.isRandomBountiesEnabled()) {
                    RandomBounties.setNextRandomBounty(0);
                } else if (RandomBounties.getNextRandomBounty() == 0) {
                    RandomBounties.setNextRandomBounty();
                }
                if (configuration.isConfigurationSection("bounty-boards"))
                    for (String str : Objects.requireNonNull(configuration.getConfigurationSection("bounty-boards")).getKeys(false)) {
                        bountyBoards.add(new BountyBoard(Objects.requireNonNull(configuration.getLocation("bounty-boards." + str + ".location")), BlockFace.valueOf(configuration.getString("bounty-boards." + str + ".direction")), configuration.getInt("bounty-boards." + str + ".rank")));
                    }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                        if (bounty.getTotalBounty() != 0) {
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
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[6];
                    values[1] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : deathBounties.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[6];
                    values[2] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : allClaimedBounties.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[6];
                    values[3] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : allTimeBounties.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[6];
                    values[4] = entry.getValue();
                    stats.put(entry.getKey(), values);
                }
                for (Map.Entry<UUID, Double> entry : immunitySpent.entrySet()) {
                    Double[] values = stats.containsKey(entry.getKey()) ? stats.get(entry.getKey()) : new Double[6];
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
    public static List<Bounty> sortBounties(int sortType) {
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

    public static boolean hasBounty(OfflinePlayer receiver) {
        return getBounty(receiver) != null;
    }

    final private static int length = 10;

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
        List<Bounty> sortedList = SQL.isConnected() ? data.getTopBounties(sortType) : sortBounties(sortType);
        for (int i = page * length; i < (page * length) + length; i++) {
            if (sortedList.size() > i) {
                sender.sendMessage(parse(listTotal, sortedList.get(i).getName(), sortedList.get(i).getTotalBounty(), Bukkit.getOfflinePlayer(sortedList.get(i).getUUID())));
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


    public static void addBounty(Player setter, OfflinePlayer receiver, double amount, Whitelist whitelist) {
        // add to all time bounties
        BountySetEvent event = new BountySetEvent(new Bounty(setter, receiver, amount, whitelist));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            refundBounty(new Bounty(setter, receiver, amount + amount * bountyTax, whitelist));
            return;
        }

        Bounty bounty = null;
        if (SQL.isConnected()) {
            bounty = data.getBounty(receiver.getUniqueId());
            if (bounty == null) {
                bounty = new Bounty(setter, receiver, amount, whitelist);
            }
            data.addBounty(new Bounty(setter, receiver, amount, whitelist), new Setter(setter.getName(), setter.getUniqueId(), amount, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
            bounty.addBounty(setter, amount, whitelist);
            data.addData(receiver.getUniqueId().toString(), 0, 0, 0, amount, 0, 0);
        } else {
            allTimeBounties.replace(receiver.getUniqueId(), Leaderboard.ALL.getStat(receiver.getUniqueId()) + amount);
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
                onlineReceiver.sendMessage(parse(prefix + bigBounty, receiver));
                displayParticle.add(onlineReceiver);
                if (bBountyCommands != null && !bBountyCommands.isEmpty()) {
                    for (String command : bBountyCommands) {
                        if (papiEnabled)
                            command = new PlaceholderAPIClass().parse(receiver, command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{player}", Matcher.quoteReplacement(onlineReceiver.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(bounty.getTotalBounty() + "")));
                    }
                }
            }
            // send messages
            onlineReceiver.sendMessage(parse(prefix + bountyReceiver, setter.getName(), amount, bounty.getTotalBounty(), receiver));

            if (serverVersion <= 16) {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
            }
            // add wanted tag
            if (wanted && bounty.getTotalBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(onlineReceiver.getUniqueId())) {
                    NotBounties.wantedText.put(onlineReceiver.getUniqueId(), new AboveNameText(onlineReceiver));
                }
            }
        }
        // send messages
        setter.sendMessage(parse(prefix + bountySuccess, getPlayerName(receiver.getUniqueId()), amount, bounty.getTotalBounty(), receiver));

        if (serverVersion <= 16) {
            setter.playSound(setter.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
        } else {
            setter.playSound(setter.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
        }

        String message = parse(prefix + bountyBroadcast, getPlayerName(receiver.getUniqueId()), setter.getName(), amount, bounty.getTotalBounty(), receiver);

        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.getList().isEmpty()) {
            if (amount >= minBroadcast)
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

    public static void addBounty(OfflinePlayer receiver, double amount, Whitelist whitelist) {
        // add to all time bounties
        BountySetEvent event = new BountySetEvent(new Bounty(receiver, amount, whitelist));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            refundBounty(new Bounty(receiver, amount + amount * bountyTax, whitelist));
            return;
        }

        Bounty bounty = null;
        if (SQL.isConnected()) {
            bounty = data.getBounty(receiver.getUniqueId());
            if (bounty == null) {
                bounty = new Bounty(receiver, amount, whitelist);
            }
            data.addBounty(new Bounty(receiver, amount, whitelist), new Setter(consoleName, new UUID(0, 0), amount, System.currentTimeMillis(), receiver.isOnline(), whitelist, BountyExpire.getTimePlayed(receiver.getUniqueId())));
            bounty.addBounty(amount, whitelist);
            data.addData(receiver.getUniqueId().toString(), 0, 0, 0, amount, 0, 0);
        } else {
            allTimeBounties.put(receiver.getUniqueId(), Leaderboard.ALL.getStat(receiver.getUniqueId()) + amount);
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
                onlineReceiver.sendMessage(parse(prefix + bigBounty, receiver));
                displayParticle.add(onlineReceiver);
                if (bBountyCommands != null && !bBountyCommands.isEmpty()) {
                    for (String command : bBountyCommands) {
                        if (papiEnabled)
                            command = new PlaceholderAPIClass().parse(receiver, command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{player}", Matcher.quoteReplacement(onlineReceiver.getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(bounty.getTotalBounty() + "")));
                    }
                }
            }
            // send messages
            onlineReceiver.sendMessage(parse(prefix + bountyReceiver, consoleName, amount, bounty.getTotalBounty(), receiver));

            if (serverVersion <= 16) {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
            }
            // add wanted tag
            if (wanted && bounty.getTotalBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(onlineReceiver.getUniqueId())) {
                    NotBounties.wantedText.put(onlineReceiver.getUniqueId(), new AboveNameText(onlineReceiver));
                }
            }
        }
        // send messages
        String message = parse(prefix + bountyBroadcast, getPlayerName(receiver.getUniqueId()), consoleName, amount, bounty.getTotalBounty(), receiver);
        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.getList().isEmpty()) {
            if (amount >= minBroadcast)
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

    public static Bounty getBounty(OfflinePlayer receiver) {
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
    public static void refundBounty(Bounty bounty) {
        for (Setter setter : bounty.getSetters()) {
            refundSetter(setter);
        }
    }

    public static boolean refundSetter(Setter setter) {
        if (vaultEnabled && !overrideVault) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(setter.getUuid());
            if (!NumberFormatting.getVaultClass().deposit(player, setter.getAmount())) {
                Bukkit.getLogger().warning("Error depositing currency with vault!");
                addRefund(player.getUniqueId(), setter.getAmount());
                return false;
            }
            return true;
        } else {
            Player player = Bukkit.getPlayer(setter.getUuid());
            if (player != null && manualEconomy != ManualEconomy.PARTIAL) {
                NumberFormatting.doAddCommands(player, setter.getAmount());
                return true;
            }
            addRefund(setter.getUuid(), setter.getAmount());
            return false;

        }
    }

    private static void addRefund(UUID uuid, double amount) {
        if (refundedBounties.containsKey(uuid)){
            refundedBounties.replace(uuid, refundedBounties.get(uuid) + amount);
        } else {
            refundedBounties.put(uuid, amount);
        }
    }

    public static List<Bounty> getPublicBounties(int sortType) {
        List<Bounty> bounties = SQL.isConnected() ? data.getTopBounties(sortType) : sortBounties(sortType);
        return bounties.stream().filter(bounty -> !hiddenNames.contains(bounty.getName())).collect(Collectors.toList());
    }

}

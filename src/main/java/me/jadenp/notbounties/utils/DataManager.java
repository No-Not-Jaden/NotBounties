package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.data.player_data.*;
import me.jadenp.notbounties.features.ActionCommands;
import me.jadenp.notbounties.features.settings.databases.*;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.auto_bounties.BigBounty;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.integrations.external_api.MMOLibClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


import static me.jadenp.notbounties.features.LanguageOptions.*;

// TODO: make non async methods public

/**
 * Manage the stored data.
 */
public class DataManager {

    private DataManager() {
    }


    public static final UUID GLOBAL_SERVER_ID = new UUID(0, 0);

    private static final Map<UUID, String> onlinePlayers = new HashMap<>();
    private static final long NETWORK_PLAYERS_REFRESH_INTERVAL_MS = 10000;
    private static long lastNetworkPlayersRefresh = 0;
    private static final Map<UUID, String> networkPlayers = new HashMap<>();

    private static Plugin plugin;

    public static void loadData(Plugin plugin) throws IOException {
        DataManager.plugin = plugin;
        // load modern data
        SaveManager.read(plugin);
    }

    /**
     * Get players on the network
     *
     * @return All players online in the SQL database, or all server players if the database isn't connected
     */
    public static synchronized Map<UUID, String> getNetworkPlayers() {
        if (System.currentTimeMillis() - lastNetworkPlayersRefresh < NETWORK_PLAYERS_REFRESH_INTERVAL_MS) {
            lastNetworkPlayersRefresh = System.currentTimeMillis();
            ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getOnlinePlayersAsync().thenAccept(players -> {
                synchronized (networkPlayers) {
                    networkPlayers.clear();
                    networkPlayers.putAll(players);
                }
            }).join();
        }
        synchronized (networkPlayers) {
            return new HashMap<>(networkPlayers);
        }
    }

    /**
     * Get a player stat.
     *
     * @param uuid        UUID of the player.
     * @param leaderboard Leaderboard of the stat to get.
     * @return The stat of the player.
     */
    public static CompletableFuture<Double> getStatAsync(UUID uuid, Leaderboard leaderboard) {
        if (leaderboard == Leaderboard.CURRENT) {
            return getBountyAsync(uuid).thenApply(bounty -> bounty == null ? 0.0 : bounty.getTotalDisplayBounty());
        }
        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getStatsAsync(uuid).thenApply(stats -> stats != null ? stats.leaderboardType(leaderboard) : 0.0);

    }

    /**
     * Get a player stat.
     *
     * @param uuid        UUID of the player.
     * @param leaderboard Leaderboard of the stat to get.
     * @return The stat of the player.
     * @deprecated Use {@link #getStatAsync(UUID, Leaderboard)} instead.
     */
    @Deprecated(since = "1.23.0")
    public static double getStat(UUID uuid, Leaderboard leaderboard) {
        if (leaderboard == Leaderboard.CURRENT) {
            Bounty bounty = getBounty(uuid);
            return bounty == null ? 0.0 : bounty.getTotalDisplayBounty();
        }
        PlayerStat stat = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getStats(uuid);
        return stat != null ? stat.leaderboardType(leaderboard) : 0.0;
    }

    public static List<String> locationListToStringList(List<Location> locations) {
        List<String> stringList = new ArrayList<>(locations.size());
        for (Location location : locations) {
            if (location == null || location.getWorld() == null)
                continue;
            stringList.add(location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getWorld().getUID());
        }
        return stringList;
    }

    public static List<Location> stringListToLocationList(List<String> stringList) {
        List<Location> locations = new ArrayList<>(stringList.size());
        for (String str : stringList) {
            String[] split = str.split(",");
            if (split.length != 4)
                continue;
            try {
                locations.add(new Location(Bukkit.getWorld(UUID.fromString(split[3])), Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2])));
            } catch (IllegalArgumentException ignored) {
                // couldn't parse location from string
            }
        }
        return locations;
    }

    /**
     * Change stats of the player. {@link Leaderboard#CURRENT} stat cannot be changed here.
     *
     * @param uuid        UUID of the player.
     * @param leaderboard Stat to change.
     * @param change      The amount the stat should change by.
     */
    public static void changeStat(UUID uuid, Leaderboard leaderboard, double change) {
        PlayerStat statChange = PlayerStat.fromLeaderboard(leaderboard, change);
        changeStats(uuid, statChange);
    }

    public static void changeStats(UUID uuid, PlayerStat changes) {
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().addStats(uuid, changes);
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

    /**
     * Get the bounty of a player.
     *
     * @param receiver UUID of the player.
     * @return The bounty of the player or null if one doesn't exist.
     */
    public static CompletableFuture<Bounty> getBountyAsync(UUID receiver) {
        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getBountyAsync(receiver);
    }

    /**
     * Get the bounty of a player.
     *
     * @param receiver UUID of the player.
     * @return The bounty of the player or null if one doesn't exist.
     * @deprecated Use {@link #getBountyAsync(UUID)} instead.
     */
    @Deprecated(since = "1.23.0")
    public static @Nullable Bounty getBounty(UUID receiver) {
        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getBounty(receiver);
    }

    /**
     * Check if a player has a bounty.
     *
     * @param receiver UUID of the player to check.
     * @return True if the player has a bounty.
     * @apiNote This is equivalent to checking if the result of {@link #getBountyAsync(UUID)} is not null.
     */
    public static CompletableFuture<Boolean> hasBountyAsync(UUID receiver) {
        return getBountyAsync(receiver).thenApply(Objects::nonNull);
    }

    /**
     * Check if a player has a bounty.
     *
     * @param receiver UUID of the player to check.
     * @return True if the player has a bounty.
     * @deprecated Use {@link #hasBountyAsync(UUID)} instead.
     */
    @Deprecated(since = "1.23.0")
    public static boolean hasBounty(UUID receiver) {
        return getBounty(receiver) != null;
    }

    /**
     * Get the cached bounties on this server.
     *
     * @deprecated
     */
    @Deprecated(since = "1.23.0")
    public static List<Bounty> getAllBounties(int sortType) {
        LocalData localData = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getDatabase(LocalData.class);
        if (localData != null)
            return localData.getCachedBounties();
        return new ArrayList<>();
    }

    /**
     * Get the top bounties in the database.
     *
     * @param sortType      How the bounties are sorted.
     * @param offset        The number of entries skipped.
     * @param limit         The maximum number of entries to be returned.
     * @param excludedPlayers Player excluded from the results.
     * @return A sorted list of the top bounties.
     */
    public static CompletableFuture<List<Bounty>> getTopBountiesAsync(BountySortType sortType, long offset, long limit, Set<UUID> excludedPlayers) {
        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getBountiesAsync(sortType, offset, limit, excludedPlayers);
    }

    public static CompletableFuture<List<Bounty>> getPublicBountiesAsync(BountySortType sortType, long offset, long limit) {
        // could add something that fetches more bounties if some were removed

        return DataManager.getTopBountiesAsync(sortType, offset, limit, ConfigOptions.getHiddenNames().stream().map(LoggedPlayers::getPlayer).filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    public static CompletableFuture<Map<UUID, PlayerStat>> getPublicStatsAsync(Leaderboard sortStat, StatSortType sortType, long offset, long limit) {
        // could add something that fetches more bounties if some were removed

        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getStatsAsync(sortStat, sortType, offset, limit, ConfigOptions.getHiddenNames().stream().map(LoggedPlayers::getPlayer).filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    /**
     * Remove a bounty from the active bounties
     *
     * @param uuid UUID of the player to remove
     */
    public static void deleteBounty(UUID uuid) {
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().removeBounty(uuid);
        ActionCommands.executeBountyRemove(uuid);
    }

    /**
     * Shuts down database connections
     */
    public static void shutdown() {
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().shutdown();
    }

    /**
     * Returns a bounty guaranteed to be active.
     * This should be run asynchronously.
     *
     * @param uuid UUID of the bountied player
     * @return The bounty for the player or null if the player doesn't have one
     */
    public static @Nullable Bounty getGuarrenteedBounty(UUID uuid) {
        // remove any cached bounty
        LocalData localData = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getDatabase(LocalData.class);
        if (localData != null)
            localData.removeBounty(uuid);
        // request bounty from database.
        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getBounty(uuid);
    }

    /**
     * Edit a bounty
     *
     * @param bounty     Bounty to be edited
     * @param setterUUID UUID of the setter to be edited
     * @param change     The amount the bounty should change
     * @return The new bounty or null if the bounty is not active
     */
    public static Bounty editBounty(@NotNull Bounty bounty, @Nullable UUID setterUUID, double change) {
        Setter lastSetter = null;
        if (setterUUID != null) {
            // edit a specific setter
            ListIterator<Setter> setterListIterator = bounty.getSetters().listIterator();
            while (setterListIterator.hasNext()) {
                if (change == 0)
                    break;
                Setter setter = setterListIterator.next();
                if (setter.getUuid().equals(setterUUID)) {
                    if (change < 0) {
                        // amount could go negative with change
                        // remove if change causes that and there are no items
                        if (setter.getDisplayAmount() + change < 0 && !setter.hasItems()) {
                            setterListIterator.remove();
                            change += setter.getDisplayAmount();

                            lastSetter = new Setter(setter.getBountyId().orElse(null), setterUUID, 0, setter.getTimeCreated(), false, setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), 0, setter.getTags());
                        } else if (setter.getAmount() > 0) {
                            // update amount (minimum 0)
                            double newAmount = Math.min(0, setter.getAmount() + change);
                            change += setter.getAmount() - newAmount; // update amount needed to be changed
                            Setter newSetter = copySetter(newAmount - setter.getAmount(), setter);
                            setterListIterator.set(newSetter);
                            lastSetter = newSetter;
                        }
                    } else {
                        // setter amount won't go negative
                        // update setter amount
                        Setter newSetter = copySetter(change, setter);
                        change = 0;
                        setterListIterator.set(newSetter);
                        lastSetter = newSetter;
                    }

                }
            }
        }
        if (change != 0) {
            // did not complete the desired amount change
            if (lastSetter == null) {
                // either no setter uuid was specified, or the specified uuid was not in the bounty
                // add a new setter
                bounty.addBounty(change, new ArrayList<>(), new Whitelist(new TreeSet<>(), false));
            } else {
                // edit last setter with the remaining change
                // this may cause the amount to be negative
                bounty.getSetters().remove(lastSetter);
                bounty.getSetters().add(copySetter(change, lastSetter));
            }
        }
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().replaceBounty(bounty.getUUID(), bounty);
        return bounty;
    }

    private static Setter copySetter(double change, Setter setter) {
        return setter.hasItems() && setter.isItemsLoaded()
                ? new Setter(setter.getBountyId().orElse(null), setter.getUuid(), setter.getAmount() + change, setter.getTimeCreated(), setter.getItems().join(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + change, setter.getTags())
                : new Setter(setter.getBountyId().orElse(null), setter.getUuid(), setter.getAmount() + change, setter.getTimeCreated(), setter.hasItems(), setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), setter.getDisplayAmount() + change, setter.getTags());
    }

    public static void setBountyCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getPlayerDataAsync(uuid).thenAccept(playerData -> {
            playerData.setBountyCooldown(now);
            updatePlayerData(playerData);
        });
    }

    public static void addRefund(UUID uuid, OnlineRefund<?> refund) {
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().addRefunds(uuid, Collections.singletonList(refund));
    }

    /**
     * Checks if the player has any new bounties to be notified of and sends the offlineBounty message and updates their big bounty status.
     *
     * @param player Player to notify.
     */
    public static void notifyBounty(Player player, Bounty bounty) {
        if (bounty != null) {
            double addedAmount = 0;
            boolean beenNotified = false;
            for (Setter setter : bounty.getSetters()) {
                if (!setter.isNotified()) {
                    beenNotified = true;
                    player.sendMessage(parse(getPrefix() + getMessage("offline-bounty"), setter.getDisplayAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    setter.setNotified(true);
                    addedAmount += setter.getDisplayAmount();
                }
            }
            if (beenNotified) {
                ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().notifyBounty(player.getUniqueId());
                if (addedAmount > 0)
                    BigBounty.setBounty(player, bounty, addedAmount);
            }

            if (bounty.getTotalDisplayBounty() > BigBounty.getThreshold()) {
                BigBounty.addParticle(player.getUniqueId());
            }
        }
    }

    public static void login(@NotNull Player player) {
        onlinePlayers.put(player.getUniqueId(), player.getName());
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().login(player.getUniqueId(), player.getName());
    }

    public static void logout(@NotNull Player player) {
        onlinePlayers.remove(player.getUniqueId());
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().logout(player.getUniqueId());
    }

    public static Map<UUID, String> getLocalOnlinePlayers() {
        return onlinePlayers;
    }

    /**
     * Get the top players.
     * @param sortType How to sort the returned entries.
     * @param offset Offset of the entries from the top.
     * @param limit Number of entries to be returned.
     * @param excludedPlayers Players to be excluded from the returned entries.
     * @return The top players.
     */
    public static CompletableFuture<List<PlayerData>> getTopPlayerDataAsync(PlayerSortType sortType, long offset, long limit, Set<UUID> excludedPlayers) {
        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getPlayerDataAsync(sortType, offset, limit, excludedPlayers);
    }

    /**
     * Updates the player data entry in the database.
     * @param playerData Player data to update.
     */
    public static void updatePlayerData(PlayerData playerData) {
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().updatePlayerData(playerData);
    }

    @FunctionalInterface
    public interface PlayerDataOperation {
        void run(PlayerData playerData);
    }

    public static void iterateAllPlayerData(PlayerDataOperation op) {
        iteratePlayerData(0, op);
    }

    private static void iteratePlayerData(long offset, PlayerDataOperation op) {
        final long limit = 100;
        // getting player data from the db will automatically update the impersistent name
        DataManager.getTopPlayerDataAsync(PlayerSortType.UUID, offset, limit, Collections.emptySet()).thenAccept(playerData -> {
            for (PlayerData data : playerData) {
                op.run(data);
            }
            if (playerData.size() >= limit) {
                iteratePlayerData(offset + limit, op);
            }
        });
    }

    /**
     * Get the player data of a player.
     *
     * @param uuid UUID of the player.
     * @return The player's NotBounties data.
     */
    public static CompletableFuture<PlayerData> getPlayerDataAsync(@NotNull UUID uuid) {
        return ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getPlayerDataAsync(uuid);
    }

    /**
     * Get the player data of a player.
     *
     * @param uuid UUID of the player.
     * @return The player's NotBounties data.
     * @deprecated Use {@link #getPlayerDataAsync(UUID)} instead.
     */
    @Deprecated(since = "1.23.0")
    public static @NotNull PlayerData getPlayerData(@NotNull UUID uuid) {
        PlayerData playerData = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getPlayerData(uuid);
        if (playerData == null) {
            playerData = new PlayerData();
            playerData.setUuid(uuid);
        }
        return playerData;
    }

    /**
     * Give a player any refunds they may have.
     *
     * @param player player to give refunds to.
     */
    public static void handleRefund(Player player) {
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getAndRemoveRefundsAsync(player.getUniqueId()).thenAccept(refunds -> {
            if (refunds != null && !refunds.isEmpty()) {
                NotBounties.getServerImplementation().entity(player).run(() -> { // will this throw anything if the player isn't online anymore?
                    if (player.isOnline()) {
                        NotBounties.debugMessage("Giving " + player.getName() + " a refund.", false);
                        refunds.forEach(refund -> refund.giveRefund(player));
                    } else {
                        // add refund back to db
                        NotBounties.debugMessage(player.getName() + " logged off before a refund could be given.", false);
                        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().addRefunds(player.getUniqueId(), refunds);
                    }
                });
            }
        });
    }

    @Deprecated(since = "1.23.0")
    public static List<PlayerData> getAllPlayerData() {
        LocalData localData = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getDatabase(LocalData.class);
        if (localData != null) {
            return localData.getCachedPlayerData();
        }
        return Collections.emptyList();
    }

    /**
     * A utility to modify both parameters to remove any setters that are similar.
     * For any setter with the same uuid and whitelist, amounts will be canceled out, and similar items will be removed.
     * The order of the parameters doesn't matter.
     */
    private static void removeSimilarSetters(List<Setter> masterSetterList, List<Setter> setterList) {
        // iterate through setters
        ListIterator<Setter> masterSetters = masterSetterList.listIterator();
        while (masterSetters.hasNext()) {
            Setter masterSetter = masterSetters.next();
            ListIterator<Setter> setters = setterList.listIterator();
            while (setters.hasNext()) {
                Setter setter = setters.next();
                // if master setters match, remove matching amount and items from both lists
                if (setter.getUuid().equals(masterSetter.getUuid()) && setter.getWhitelist().equals(masterSetter.getWhitelist())) {
                    double newSetterAmount = 0;
                    double newMasterAmount = masterSetter.getAmount() - setter.getAmount();
                    if (newMasterAmount < 0) {
                        // trying to remove too much from this setter
                        newSetterAmount = -1 * newMasterAmount;
                        newMasterAmount = 0;
                    }
                    List<ItemStack> masterItems = masterSetter.getItems().join();
                    List<ItemStack> setterItems = setter.getItems().join();
                    removeSimilarItems(masterItems, setterItems);
                    // if setter is empty, remove
                    if (newSetterAmount == 0 && setterItems.isEmpty()) {
                        // empty setter
                        setters.remove();
                    } else {
                        // replace with new setter
                        setters.set(new Setter(setter.getBountyId().orElse(null), setter.getUuid(), newSetterAmount, setter.getTimeCreated(), setterItems, setter.isNotified(), setter.getWhitelist(), setter.getReceiverPlaytime(), -1, setter.getTags())); // display bounty doesn't matter because this setter will no longer exist outside this loop
                    }
                    if (newMasterAmount == 0 && masterItems.isEmpty()) {
                        // empty setter
                        masterSetters.remove();
                        break;
                    } else {
                        // replace with new setter
                        masterSetter = new Setter(masterSetter.getBountyId().orElse(null), masterSetter.getUuid(), newMasterAmount, masterSetter.getTimeCreated(), masterItems, masterSetter.isNotified(), masterSetter.getWhitelist(), masterSetter.getReceiverPlaytime(), -1, masterSetter.getTags());
                        masterSetters.set(masterSetter);
                    }
                }
            }
        }
    }

    private static void removeSimilarItems(List<ItemStack> masterItemsList, List<ItemStack> setterItemsList) {
        ListIterator<ItemStack> masterItems = masterItemsList.listIterator();
        while (masterItems.hasNext()) {
            ItemStack masterItem = masterItems.next();
            ListIterator<ItemStack> setterItems = setterItemsList.listIterator();
            while (setterItems.hasNext()) {
                ItemStack setterItem = setterItems.next();
                if (masterItem.isSimilar(setterItem)) {
                    int setterAmount = 0;
                    int masterAmount = masterItem.getAmount() - setterItem.getAmount();
                    if (masterAmount < 0) {
                        setterAmount = -1 * masterAmount;
                        masterAmount = 0;
                    }
                    if (setterAmount == 0) {
                        setterItems.remove();
                    } else {
                        setterItem.setAmount(setterAmount);
                        setterItems.set(setterItem);
                    }
                    if (masterAmount == 0) {
                        masterItems.remove();
                        break;
                    } else {
                        masterItem.setAmount(masterAmount);
                        masterItems.set(masterItem);
                    }
                }
            }
        }
    }

    /**
     * Removes setters from all databases if the bounty contains them.
     *
     * @param bounty  Bounty that contains the setters.
     * @param setters Setters to be removed.
     */
    public static void removeSetters(@NotNull Bounty bounty, List<Setter> setters) {
        if (setters.isEmpty())
            return;
        // create copies of objects so the originals aren't modified
        Bounty bountyCopy = new Bounty(bounty);
        List<Setter> originalSetters = new ArrayList<>(bounty.getSetters());
        List<Setter> settersToRemove = new ArrayList<>(setters);
        if (Bukkit.isPrimaryThread()) {
            NotBounties.getServerImplementation().async().runNow(() -> {
                removeSetters(bounty, bountyCopy, settersToRemove);
                updateEditedBounty(bounty, setters, bountyCopy, originalSetters);
            });
        } else {
            removeSetters(bounty, bountyCopy, settersToRemove);
            updateEditedBounty(bounty, setters, bountyCopy, originalSetters);
        }

    }

    private static void removeSetters(@NotNull Bounty bounty, Bounty bountyCopy, List<Setter> settersToRemove) {
        // remove similar setters in the lists
        // any setters that weren't on the bounty are left in the settersToRemove list
        DataManager.removeSimilarSetters(bountyCopy.getSetters(), settersToRemove);
        // modify features that depend on a bounty change (this should probably be extracted to a method)
        if (bountyCopy.getTotalBounty() < BigBounty.getThreshold())
            BigBounty.removeParticle(bounty.getUUID());
        if (bountyCopy.getTotalDisplayBounty() < WantedTags.getMinWanted()) {
            // remove bounty tag
            NotBounties.getServerImplementation().global().run(() -> WantedTags.removeWantedTag(bounty.getUUID()));
            NotBounties.debugMessage("Removed wanted tag.", false);
        }
        if (ConfigOptions.getIntegrations().isMmoLibEnabled()) {
            Player player = Bukkit.getPlayer(bountyCopy.getUUID());
            if (player != null) {
                MMOLibClass.removeStats(player);
                MMOLibClass.addStats(player, bountyCopy.getTotalDisplayBounty());
            }
        }
    }


    private static void updateEditedBounty(@NotNull Bounty originalBounty, List<Setter> removedSetters, Bounty updatedBounty, List<Setter> originalSetters) {
        // update the bounty in the databases
        if (updatedBounty.getSetters().isEmpty()) {
            // no more setters left in the bounty - delete from databases
            deleteBounty(originalBounty.getUUID());
            BountyTracker.stopTracking(originalBounty.getUUID());
            for (Player p : Bukkit.getOnlinePlayers()) {
                BountyTracker.removeTracker(p);
            }
        } else {
            // there are setters remaining
            // check if the setter amounts were modified in the removeSimilarSetters method
            boolean allMatch = true;
            for (Setter setter : updatedBounty.getSetters()) {
                if (!originalSetters.contains(setter)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                // the individual amounts were not modified, so these setters can just be removed from the databases
                Bounty removedBounty = new Bounty(originalBounty.getUUID(), removedSetters, originalBounty.getServerID());
                // setters that remain were unmodified.
                ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().removeBounty(removedBounty);
            } else {
                // Setters that remain had their amounts modified from their original values.
                // The bounty has to be replaced with the new setter amounts.
                ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().replaceBounty(originalBounty.getUUID(), updatedBounty);
            }
            ActionCommands.executeBountyRemove(originalBounty.getUUID());
        }
    }

    /**
     * Inserts a bounty into the sorted bountyList
     *
     * @return The new bounty. This bounty will be the combination of all bounties on the same person.
     * @deprecated Use {@link #insertBountyAsync(Player, OfflinePlayer, double, List, Whitelist)} instead.
     */
    @Deprecated(since = "1.23.0")
    public static Bounty insertBounty(@Nullable Player setter, @NotNull OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        Bounty newBounty = setter == null ? new Bounty(receiver, amount, items, whitelist) : new Bounty(setter, receiver, amount, items, whitelist);

        Bounty curBounty = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getBounty(receiver.getUniqueId());
        if (curBounty != null) {
            curBounty.getSetters().addAll(newBounty.getSetters());
            return curBounty;
        }
        return newBounty;
    }

    /**
     * Inserts a bounty into the database.
     *
     * @param setter    Player who set the bounty. Null for console.
     * @param receiver  Player who the bounty is being set on.
     * @param amount    Amount of currency the bounty was placed for.
     * @param items     Items on the bounty.
     * @param whitelist Whitelist of the bounty.
     * @return The total bounty on the player.
     */
    public static CompletableFuture<Bounty> insertBountyAsync(@Nullable Player setter, @NotNull OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        Bounty newBounty = setter == null ? new Bounty(receiver, amount, items, whitelist) : new Bounty(setter, receiver, amount, items, whitelist);
        CompletableFuture<Bounty> combinedBounty = ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().getBountyAsync(receiver.getUniqueId()).thenApply(bounty -> {
            if (bounty != null) {
                bounty.getSetters().addAll(newBounty.getSetters());
                return bounty;
            }
            return newBounty;
        });
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().addBounty(newBounty);
        return combinedBounty;
    }

    /**
     * Adds a bounty to the active bounties.
     *
     * @param bounty Bounty to be added
     */
    public static void addBounty(Bounty bounty) {
        ConfigOptions.getDatabases().getConfiguredDatabases().getFirst().addBounty(bounty);
    }

}

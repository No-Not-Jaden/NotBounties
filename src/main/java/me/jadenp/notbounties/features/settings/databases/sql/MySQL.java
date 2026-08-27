package me.jadenp.notbounties.features.settings.databases.sql;

import com.google.gson.stream.JsonReader;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.utils.LoggedPlayers;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.data.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class MySQL {
    private Connection connection;
    private Plugin plugin;

    public MySQL(Plugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
    }

    public Map<UUID, PlayerStat> getAllStats() throws SQLException {
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, claimed, sets, received, alltime, immunity, allclaimed FROM bounty_data;")) {
                try (ResultSet rs = ps.executeQuery()) {

                    Map<UUID, PlayerStat> stats = new HashMap<>();
                    while (rs.next()) {
                        rs.findColumn("uuid");
                        UUID uuid = UUID.fromString(rs.getString(1));
                        long kills = rs.getLong(2);
                        long set = rs.getLong(3);
                        long deaths = rs.getLong(4);
                        double all = rs.getDouble(5);
                        double immunity = rs.getDouble(6);
                        double claimed = rs.getDouble(7);
                        stats.put(uuid, new PlayerStat(kills, set, deaths, all, immunity, claimed, new UUID(0, 0)));
                    }
                    return stats;
                }
            }

    }

    /**
     * Get a list of UUIDs from a string separating UUIDs with ','
     * @param whitelist Single string of uuids
     * @return a list of UUIDs. Returns an empty list if whitelist is null or empty
     */
    public Whitelist decodeWhitelist(@Nullable String whitelist){
        if (whitelist == null || whitelist.isEmpty())
            return new Whitelist(new TreeSet<>(), false);
        boolean blacklist = whitelist.contains(".");
        String[] split = blacklist ? whitelist.split("\\.") : whitelist.split(",");
        SortedSet<UUID> uuids = new TreeSet<>();
        for (String uuidString : split) {
            try {
                uuids.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                NotBounties.debugMessage(e.toString(), true);
            }
        }
        return new Whitelist(uuids, blacklist);
    }

    /**
     * Converts a blob of encoded ItemStacks into a list. The blob is freed afterward.
     * @param encodedItems Blob to be converted
     * @return An ArrayList of ItemStacks that the blob represented.
     */
    private @NotNull List<ItemStack> convertEncodedItems(@Nullable Blob encodedItems) {
        List<ItemStack> items;
        try {
            if (encodedItems != null) {
                items = new ArrayList<>(Arrays.asList(SerializeInventory.itemStackArrayFromBinaryStream(encodedItems.getBinaryStream())));
                encodedItems.free();
            } else {
                items = new ArrayList<>();
            }
        } catch (SQLException | IOException e) {
            // items haven't been set yet
            items = new ArrayList<>();
        }
        return items;
    }

    public List<Bounty> getAllBounties(int sortType) throws SQLException {
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, name, setter, suuid, amount, notified, created, whitelist, playtime, items, display FROM notbounties;");
                 ResultSet resultSet = ps.executeQuery()) {

                Map<String, Bounty> bountyAmounts = new HashMap<>();
                while (resultSet.next()) {
                    String uuid = resultSet.getString("uuid");
                    if (uuid != null) {
                        try {
                            Blob encodedItems = resultSet.getBlob("items");
                            List<ItemStack> items;
                            try {
                                items = encodedItems != null ? new ArrayList<>(Arrays.asList(SerializeInventory.itemStackArrayFromBinaryStream(encodedItems.getBinaryStream()))) : new ArrayList<>();
                            } catch (StreamCorruptedException e) {
                                items = new ArrayList<>();
                            }
                            long timeCreated = resultSet.getLong("created");
                            Setter setter = new Setter(
                                    null,
                                    UUID.fromString(resultSet.getString("suuid")),
                                    resultSet.getDouble("amount"),
                                    timeCreated,
                                    items,
                                    resultSet.getBoolean("notified"),
                                    decodeWhitelist(resultSet.getString("whitelist")),
                                    resultSet.getLong("playtime"),
                                    resultSet.getDouble("display"),
                                    Collections.emptySet()
                            );
                            if (bountyAmounts.containsKey(uuid)) {
                                bountyAmounts.get(uuid).addBounty(setter);
                            } else {
                                bountyAmounts.put(uuid, new Bounty(UUID.fromString(uuid), new ArrayList<>(Collections.singletonList(setter))));
                            }
                        } catch (IOException e) {
                            // error parsing encoded items
                            Bukkit.getLogger().warning("[NotBounties] Error decoding items from SQL");
                            Bukkit.getLogger().warning(e.toString());
                        }
                    }
                }
                List<Bounty> sortedList = new ArrayList<>(bountyAmounts.values());
                if (sortType == -1)
                    return sortedList;
                Bounty temp;
                for (int i = 0; i < sortedList.size(); i++) {
                    for (int j = i + 1; j < sortedList.size(); j++) {
                        if (!sortedList.get(i).getSetters().isEmpty()
                                && ((sortedList.get(i).getSetters().getFirst().getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                                (sortedList.get(i).getSetters().getFirst().getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 1) || // newest bounties at top
                                (sortedList.get(i).getTotalDisplayBounty() < sortedList.get(j).getTotalDisplayBounty() && sortType == 2) || // more expensive bounties at top
                                (sortedList.get(i).getTotalDisplayBounty() > sortedList.get(j).getTotalDisplayBounty() && sortType == 3))) { // less expensive bounties at top
                            temp = sortedList.get(i);
                            sortedList.set(i, sortedList.get(j));
                            sortedList.set(j, temp);
                        }
                    }
                }
                return sortedList;
            }
    }

    private Connection getConnection() {
        return connection;
    }

    public OnlineRefund<?> refundFromInputStream(InputStream inputStream, Class<? extends OnlineRefund<?>> clazz) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             JsonReader jsonReader = new JsonReader(reader)) {
             return PlayerData.readRefund(jsonReader, clazz);
        }
    }

    private void immunityFromByte(PlayerData playerData, byte b) {
        playerData.setGeneralImmunity((b & 1) == 1);
        playerData.setMurderImmunity((b & 2) == 2);
        playerData.setRandomImmunity((b & 4) == 4);
        playerData.setTimedImmunity((b & 8) == 8);
    }

    private Map<UUID, List<OnlineRefund<?>>> getRefunds() throws SQLException {
        Map<UUID, List<OnlineRefund<?>>> refunds = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, refundtype, refund FROM bounty_refunds;");
        ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                OnlineRefund<?> refund = getRefundFromResult(rs);
                if (refund != null) {
                    refunds.computeIfAbsent(uuid, k -> new ArrayList<>()).add(refund);
                }
            }
        }
        return refunds;
    }

    private OnlineRefund<?> getRefundFromResult(ResultSet rs) throws SQLException {
        String className = rs.getString("refundtype");
        try (InputStream inputStream = rs.getBlob("refund").getBinaryStream()) {
            // Use reflection to get the class object
            Class<?> clazz = Class.forName(className);

            // Check if it's a subclass of OnlineRefund
            if (OnlineRefund.class.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends OnlineRefund<?>> refundClass = (Class<? extends OnlineRefund<?>>) clazz;

                return refundFromInputStream(inputStream, refundClass);
            } else {
                Bukkit.getLogger().warning("[NotBounties] Invalid refund class: " + className);
            }
        } catch (IOException | ReflectiveOperationException e) {
            Bukkit.getLogger().warning("[NotBounties] Error decoding refund: " + e.getMessage());
        }
        return null;
    }

    public List<PlayerData> getPlayerData() throws SQLException {

        Map<UUID, List<OnlineRefund<?>>> refunds = getRefunds();
        List<PlayerData> playerDataList = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, name, immunity, lastclaim, broadcastsetting, whitelist, bountycooldown, newplayer, lastseen, timezone FROM bounty_player_data ORDER BY uuid ASC;");
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                PlayerData playerData = new PlayerData();
                playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
                playerData.setUuid(uuid);
                readPlayerDataResult(rs, playerData);

                // Load refund data
                if (refunds.containsKey(uuid)) {
                    playerData.setRefund(refunds.get(uuid));
                }

                playerDataList.add(playerData);
            }

        }

        return playerDataList;
    }

    private void readPlayerDataResult(ResultSet rs, PlayerData playerData) throws SQLException {
        String name = rs.getString("name");
        if (name != null) {
            playerData.setPlayerName(name);
        } else {
            playerData.setPlayerName(LoggedPlayers.getPlayerName(playerData.getUuid()));
            NotBounties.debugMessage("SQL Playerdata has null name for " + playerData.getUuid(), true);
        }
        immunityFromByte(playerData, rs.getByte("immunity"));
        playerData.setLastClaim(rs.getLong("lastclaim"));
        playerData.setBroadcastSettings(PlayerData.BroadcastSettings.values()[rs.getByte("broadcastsetting")]);
        playerData.setWhitelist(decodeWhitelist(rs.getString("whitelist")));
        playerData.setBountyCooldown(rs.getLong("bountycooldown"));
        playerData.setNewPlayer(rs.getBoolean("newplayer"));
        playerData.setLastSeen(rs.getLong("lastseen"));
        String timeZone = rs.getString("timezone");
        if (timeZone != null && !timeZone.isEmpty()) {
            playerData.setTimeZone(TimeZone.getTimeZone(timeZone));
        }

    }
}

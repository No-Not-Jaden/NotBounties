package me.jadenp.notbounties.sql;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.utils.configuration.BountyExpire;
import me.jadenp.notbounties.utils.BountyManager;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static me.jadenp.notbounties.NotBounties.debug;
import static me.jadenp.notbounties.NotBounties.isVanished;
import static me.jadenp.notbounties.utils.BountyManager.SQL;
import static me.jadenp.notbounties.utils.BountyManager.tryToConnect;

public class SQLGetter {

    private long nextReconnectAttempt;
    private int reconnectAttempts;
    public SQLGetter (){
        nextReconnectAttempt = System.currentTimeMillis();
        reconnectAttempts = 0;
    }

    public void createTable(){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS notbounties" +
                "(" +
                "    uuid CHAR(36) NOT NULL," +
                "    name VARCHAR(16) NOT NULL," +
                "    setter VARCHAR(16) NOT NULL," +
                "    suuid CHAR(36) NOT NULL," +
                "    amount FLOAT(53) DEFAULT 0 NOT NULL," +
                "    notified BOOLEAN DEFAULT TRUE NOT NULL," +
                "    time BIGINT NOT NULL," +
                "    whitelist VARCHAR(369)," +
                "    playtime BIGINT DEFAULT 0 NOT NULL," +
                "    items BLOB," +
                "    display FLOAT(53) DEFAULT -1 NOT NULL" +
                ");");
            PreparedStatement checkAmount = SQL.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'notbounties' and column_name = 'amount';");
            PreparedStatement alterAmount = SQL.getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN amount FLOAT(53);");
            PreparedStatement checkWhitelist = SQL.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'whitelist';");
            PreparedStatement addWhitelist = SQL.getConnection().prepareStatement("ALTER TABLE notbounties ADD whitelist VARCHAR(369);");
            PreparedStatement checkPlaytime = SQL.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'playtime';");
            PreparedStatement addPlaytime = SQL.getConnection().prepareStatement("ALTER TABLE notbounties ADD playtime BIGINT DEFAULT 0 NOT NULL;");
            PreparedStatement checkItems = SQL.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'items';");
            PreparedStatement addItems = SQL.getConnection().prepareStatement("ALTER TABLE notbounties ADD items BLOB;");
            PreparedStatement checkItemsType = SQL.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'notbounties' and column_name = 'items';");
            PreparedStatement alterItems = SQL.getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN items BLOB;");
            PreparedStatement checkDisplay = SQL.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'display';");
            PreparedStatement addDisplay = SQL.getConnection().prepareStatement("ALTER TABLE notbounties ADD display FLOAT(53) DEFAULT -1 NOT NULL;");){
            
            ps.executeUpdate();
            
            checkAmount.setString(1, SQL.getDatabase());
            ResultSet rs = checkAmount.executeQuery();
            if (rs.next() && rs.getString("data_type").equalsIgnoreCase("bigint")){
                    Bukkit.getLogger().info("Updating data type for bounties");
                    alterAmount.executeUpdate();
                }
            rs.close();

            rs = checkWhitelist.executeQuery();
            if (!rs.next()){
                addWhitelist.executeUpdate();
            }
            rs.close();
            
            rs = checkPlaytime.executeQuery();
            if (!rs.next()){
                addPlaytime.executeUpdate();
            }
            rs.close();
            
            rs = checkItems.executeQuery();
            if (!rs.next()){
                addItems.executeUpdate();
            }
            rs.close();

            checkItemsType.setString(1, SQL.getDatabase());
            rs = checkItemsType.executeQuery();
            if (rs.next() && rs.getString("data_type").equalsIgnoreCase("char")){
                alterItems.executeUpdate();
            }
            rs.close();

            rs = checkDisplay.executeQuery();
            if (!rs.next()) {
                addDisplay.executeUpdate();
            }
            rs.close();

        } catch (SQLException e){
            Bukkit.getLogger().warning(e.toString());
        }
    }
    public void createDataTable(){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS bounty_data" +
                "(" +
                "    uuid CHAR(36) NOT NULL," +
                "    claimed BIGINT DEFAULT 0 NOT NULL," +
                "    sets BIGINT DEFAULT 0 NOT NULL," +
                "    received BIGINT DEFAULT 0 NOT NULL," +
                "    alltime FLOAT(53) DEFAULT 0 NOT NULL," +
                "    immunity FLOAT(53) DEFAULT 0 NOT NULL," +
                "    allclaimed FLOAT(53) DEFAULT 0 NOT NULL," +
                "    PRIMARY KEY (uuid)" +
                ");");
            PreparedStatement ps1 = SQL.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'bounty_data' and column_name = 'claimed';");
            PreparedStatement ps2 = SQL.getConnection().prepareStatement("ALTER TABLE bounty_data MODIFY COLUMN claimed BIGINT, MODIFY sets BIGINT, MODIFY received BIGINT, MODIFY alltime FLOAT(53), MODIFY immunity FLOAT(53), MODIFY allclaimed FLOAT(53);");) {
            
            ps.executeUpdate();
            
            ps1.setString(1, SQL.getDatabase());
            ResultSet rs = ps1.executeQuery();
            if (rs.next() && rs.getString("data_type").equalsIgnoreCase("int")){
                    Bukkit.getLogger().info("Updating data type for statistics");
                    ps2.executeUpdate();
                }

        } catch (SQLException e){
            Bukkit.getLogger().warning(e.toString());
        }
    }
    public void createOnlinePlayerTable() {
        try(PreparedStatement ps = SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS bounty_players" +
                "(" +
                "    uuid CHAR(36) NOT NULL," +
                "    name VARCHAR(16) NOT NULL," +
                "    id INT DEFAULT 0 NOT NULL," +
                "    PRIMARY KEY (uuid)" +
                ");")) {
            
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.toString());
        }
    }
    private long lastPlayerListRequest = 0;
    private final List<OfflinePlayer> onlinePlayers = new ArrayList<>();
    public List<OfflinePlayer> getOnlinePlayers() {
        if (lastPlayerListRequest + 5000 > System.currentTimeMillis()) {
            if (onlinePlayers.isEmpty())
                onlinePlayers.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> !isVanished(player)).collect(Collectors.toList()));
            return onlinePlayers;
        }
        lastPlayerListRequest = System.currentTimeMillis();
        return getNetworkPlayers();
    }
    private List<OfflinePlayer> getNetworkPlayers() {
        List<OfflinePlayer> networkPlayers = new ArrayList<>();
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT uuid FROM bounty_players;")){
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                try {
                    networkPlayers.add(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[NotBounties] Removing invalid UUID on database: " + uuid);
                    try (PreparedStatement ps1 = SQL.getConnection().prepareStatement("DELETE FROM bounty_players WHERE uuid = ?;")) {
                        ps1.setString(1, uuid);
                        ps1.executeUpdate();
                    } catch (SQLException e2) {
                        if (NotBounties.debug) {
                            Bukkit.getLogger().warning("[NotBountiesNotBounties.debug] Failed to remove uuid from database.");
                            Bukkit.getLogger().warning(e.toString());
                        }
                    }
                }
            }
        } catch (SQLException e){
            if (reconnect()){
                return getNetworkPlayers();
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        onlinePlayers.clear();
        onlinePlayers.addAll(networkPlayers);
        return networkPlayers;
    }
    public void refreshOnlinePlayers() {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("DELETE FROM bounty_players WHERE id = ?;")){
            ps.setInt(1, SQL.getServerID());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                refreshOnlinePlayers();
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        for (Player player : Bukkit.getOnlinePlayers())
            if (!isVanished(player))
                login(player);
    }
    public void logout(Player player) {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("DELETE FROM bounty_players WHERE uuid = ? AND id = ?;")){
            ps.setString(1, player.getUniqueId().toString());
            ps.setInt(2, SQL.getServerID());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                logout(player);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void login(Player player) {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("INSERT INTO bounty_players(uuid, name, id) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE id = ?;")){
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());
            ps.setInt(3, SQL.getServerID());
            ps.setInt(4, SQL.getServerID());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                login(player);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void addData(String uuid, long claimed, long set, long received, double allTime, double immunity, double allClaimed){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("INSERT INTO bounty_data(uuid, claimed, sets, received, alltime, immunity, allclaimed) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE claimed = claimed + ?, sets = sets + ?, received = received + ?, alltime = alltime + ?, immunity = immunity + ?, allclaimed = allclaimed + ?;")){
            ps.setString(1, uuid);
            ps.setLong(2, claimed);
            ps.setLong(3, set);
            ps.setLong(4, received);
            ps.setDouble(5, allTime);
            ps.setDouble(6, immunity);
            ps.setDouble(7, allClaimed);
            ps.setLong(8, claimed);
            ps.setLong(9, set);
            ps.setLong(10, received);
            ps.setDouble(11, allTime);
            ps.setDouble(12, immunity);
            ps.setDouble(13, allClaimed);
            ps.executeUpdate();
        } catch (SQLException e){
            reconnect();
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }

    public long getClaimed(String uuid){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT claimed FROM bounty_data WHERE uuid = ?;")){
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getLong("claimed");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getClaimed(uuid);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public double getTotalClaimed(String uuid){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT allclaimed FROM bounty_data WHERE uuid = ?;")){
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getDouble("allclaimed");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getTotalClaimed(uuid);
            }
        }
        return 0;
    }

    /**
     * Get the top stats from the database
     * @param leaderboard what stat is being requested
     * @param hiddenNames which names should be hidden from the result
     * @param skip how many rows will be skipped from the top of the list
     * @param results maximum amount of results that could be returned
     * @return A descending ordered list of entries of a leaderboard stat
     */
    public LinkedHashMap<UUID, Double> getTopStats(Leaderboard leaderboard, List<String> hiddenNames, int skip, int results){
        LinkedHashMap<UUID, Double> data = new LinkedHashMap<>();
        String listName = "";
        switch (leaderboard){
            case ALL:
                listName = "alltime";
                break;
            case KILLS:
                listName = "claimed";
                break;
            case CLAIMED:
                listName = "allclaimed";
                break;
            case DEATHS:
                listName = "received";
                break;
            case SET:
                listName = "sets";
                break;
            case IMMUNITY:
                listName = "immunity";
                break;
            case CURRENT:
                for (Bounty bounty : getTopBounties(2)) {
                    if (hiddenNames.contains(bounty.getName()))
                        continue;
                    if (results == 0)
                        break;
                    if (skip == 0) {
                        data.put(bounty.getUUID(), bounty.getTotalDisplayBounty());
                        results--;
                    } else {
                        skip--;
                    }
                }
                return data;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hiddenNames.size(); i++) {
            OfflinePlayer player = NotBounties.getPlayer(hiddenNames.get(i));
            if (player == null) {
                builder = new StringBuilder();
                if (debug)
                    Bukkit.getLogger().warning("[NotBounties] Error getting player: " + hiddenNames.get(i) + " from hidden players! Has this player logged into the server?");
                continue;
            } else {
                player.getUniqueId();
            }
            String uuid = player.getUniqueId().toString();
           
            if (i < hiddenNames.size() - 1)
                builder.append(uuid).append("' AND uuid != '");
            else
                builder.append(uuid);
        }
        String end = ";";
        if (skip + results > 0) {
            end = " LIMIT " + skip + "," + (skip + results) + ";";
        }

        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT uuid, `" + listName + "` FROM bounty_data WHERE uuid != '" + builder + "' ORDER BY `" + listName + "` DESC" + end)){
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                String uuid = rs.getString("uuid");
                if (leaderboard.isMoney()) {
                    data.put(UUID.fromString(uuid), rs.getDouble(2));
                } else {
                    data.put(UUID.fromString(uuid), (double) rs.getLong(2));
                }
            }
        } catch (SQLException e){
            if (reconnect()){
                return getTopStats(leaderboard, hiddenNames, skip, results);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return data;
    }

    /**
     * Get a list of UUIDs from a string separating UUIDs with ','
     * @param whitelist Single string of uuids
     * @return a list of UUIDs. Returns an empty list if whitelist is null or empty
     */
    public Whitelist decodeWhitelist(@Nullable String whitelist){
        if (whitelist == null || whitelist.isEmpty())
            return new Whitelist(new ArrayList<>(), false);
        boolean blacklist = whitelist.contains(".");
        String[] split = blacklist ? whitelist.split("\\.") : whitelist.split(",");
        List<UUID> uuids = new ArrayList<>();
        for (String uuidString : split) {
            try {
                uuids.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                if (NotBounties.debug)
                    Bukkit.getLogger().warning(e.toString());
            }
        }
        return new Whitelist(uuids, blacklist);
    }

    /**
     * Get a single string separating all the UUIDs with ','
     * @param whitelist list of UUIDs to encode
     * @return a string with separated UUIDs or an empty string if the list is empty
     */
    public String encodeWhitelist(Whitelist whitelist){
        StringBuilder combinedUuids = new StringBuilder();
        for (UUID uuid : whitelist.getList()){
            combinedUuids.append(uuid);
            if (whitelist.isBlacklist())
                combinedUuids.append(".");
            else
                combinedUuids.append(",");
        }
        if (combinedUuids.length() > 0 && combinedUuids.length() > 37)
            combinedUuids.deleteCharAt(combinedUuids.length()-1);
        return combinedUuids.toString();
    }

    public long getSet(String uuid){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT sets FROM bounty_data WHERE uuid = ?;")){
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getLong("set");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getSet(uuid);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public long getReceived(String uuid){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT received FROM bounty_data WHERE uuid = ?;")){
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getLong("received");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getReceived(uuid);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public double getAllTime(String uuid){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT alltime FROM bounty_data WHERE uuid = ?;")){
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getDouble("alltime");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getAllTime(uuid);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public double getImmunity(String uuid){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT immunity FROM bounty_data WHERE uuid = ?;");){
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getDouble("immunity");
            }
        } catch (SQLException e){
            if (reconnect()){
                getImmunity(uuid);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }


    public void setImmunity(String uuid, double amount){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("UPDATE bounty_data SET immunity = ? WHERE uuid = ?;")){
            ps.setDouble(1, amount);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                setImmunity(uuid, amount);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }



    public void addBounty(Bounty bounty, Setter setter){
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("INSERT INTO notbounties(uuid, name, setter, suuid, amount, notified, time, whitelist, playtime, items, display) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")){
            ps.setString(1, bounty.getUUID().toString());
            ps.setString(2, bounty.getName());
            ps.setString(3, setter.getName());
            ps.setString(4, setter.getUuid().toString());
            ps.setDouble(5, setter.getAmount());
            ps.setBoolean(6, setter.isNotified());
            ps.setLong(7, setter.getTimeCreated());
            ps.setString(8, encodeWhitelist(setter.getWhitelist()));
            ps.setLong(9, setter.getReceiverPlaytime());
            ps.setBlob(10, SerializeInventory.itemStackArrayToBinaryStream(setter.getItems().toArray(new ItemStack[0])));
            ps.setDouble(11, setter.getDisplayAmount());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                addBounty(bounty, setter);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void addBounty(Bounty bounty){
        for (Setter setter : bounty.getSetters())
            addBounty(bounty, setter);
    }

    public Bounty getBounty(UUID uuid) {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT * FROM `notbounties` WHERE uuid = ?;")){
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            List<Setter> setters = new ArrayList<>();
            String name = "";
            while (rs.next()){
                if (name.isEmpty()){
                    name = rs.getString("name");
                }
                Blob encodedItems = rs.getBlob("items");
                List<ItemStack> items;
                try {
                    items = encodedItems != null ? new ArrayList<>(Arrays.asList(SerializeInventory.itemStackArrayFromBinaryStream(encodedItems.getBinaryStream()))) : new ArrayList<>();
                } catch (StreamCorruptedException e) {
                    // items haven't been set yet
                    items = new ArrayList<>();
                }
                UUID setterUUID = rs.getString("suuid").equalsIgnoreCase("CONSOLE") ? new UUID(0,0) : UUID.fromString(rs.getString("suuid"));
                Setter setter = new Setter(rs.getString("setter"), setterUUID, rs.getDouble("amount"), items, rs.getLong("time"), rs.getBoolean("notified"), decodeWhitelist(rs.getString("whitelist")), rs.getLong("playtime"), rs.getDouble("display"));
                setters.add(setter);
            }
            if (setters.isEmpty())
                return null;
            return new Bounty(uuid, setters, name);
        } catch (SQLException e){
            if (reconnect()){
                return getBounty(uuid);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        } catch (IOException e) {
            // error parsing encoded items
            Bukkit.getLogger().warning("[NotBounties] Error decoding items from SQL");
            Bukkit.getLogger().warning(e.toString());
        }
        return null;
    }
    public void updateBounty(Bounty bounty) {
        removeBounty(bounty.getUUID());
        addBounty(bounty);
    }
    public void editBounty(UUID uuid, UUID setterUUID, double amount) {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("UPDATE notbounties SET amount = amount + ? WHERE uuid = ? AND suuid = ?;")){
            ps.setDouble(1, amount);
            ps.setString(2, uuid.toString());
            ps.setString(3, setterUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                editBounty(uuid, setterUUID, amount);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void removeBounty(UUID uuid) {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeBounty(uuid);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void removeSetter(UUID uuid, UUID setterUUID) {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("DELETE notbounties WHERE uuid = ? AND suuid = ?;")){
            ps.setString(1, uuid.toString());
            ps.setString(2, setterUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeSetter(uuid, setterUUID);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public Map<UUID, List<Setter>> removeOldPlaytimeBounties() {
        List<Bounty> bounties = getTopBounties(-1);
        Map<UUID, List<Setter>> expiredBounties = new HashMap<>();
        for (Bounty bounty : bounties) {
            for (Setter setter : bounty.getSetters()) {
                if (BountyExpire.isExpired(bounty.getUUID(), setter)) {
                    if (expiredBounties.containsKey(bounty.getUUID())) {
                        expiredBounties.get(bounty.getUUID()).add(setter);
                    } else {
                        expiredBounties.put(bounty.getUUID(), new ArrayList<>(List.of(setter)));
                    }
                    removeSetter(bounty.getUUID(), setter.getUuid());
                }
            }
        }
        return expiredBounties;
    }
    public Map<UUID, List<Setter>> removeOldBounties() {
        long minTime = (long) (System.currentTimeMillis() - 1000L * 60 * 60 * 24 * BountyExpire.getTime());
        double autoExpire = ConfigOptions.autoBountyExpireTime == -1 ? BountyExpire.getTime() : ConfigOptions.autoBountyExpireTime;
        long minTimeAuto = (long) (System.currentTimeMillis() - 1000L * 60 * 60 * 24 * autoExpire);
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("DELETE notbounties WHERE time <= ? AND suuid != ?;");
             PreparedStatement ps1 = SQL.getConnection().prepareStatement("SELECT * FROM notbounties WHERE time <= ? AND suuid != ?;");
             PreparedStatement ps2 = SQL.getConnection().prepareStatement("DELETE notbounties WHERE time <= ? AND suuid == ?;");
             PreparedStatement ps3 = SQL.getConnection().prepareStatement("SELECT * FROM notbounties WHERE time <= ? AND suuid=!= ?;");) {

            ps.setLong(1, minTime);
            ps1.setLong(1, minTime);
            ps2.setLong(1, minTimeAuto);
            ps3.setLong(1, minTimeAuto);
            String consoleUUID = new UUID(0,0).toString();
            ps.setString(2, consoleUUID);
            ps1.setString(2, consoleUUID);
            ps2.setString(2, consoleUUID);
            ps3.setString(2, consoleUUID);
            Map<UUID, List<Setter>> expiredBounties = new HashMap<>();
            if (BountyExpire.getTime() > 0) {
                ResultSet rs = ps1.executeQuery();
                while (rs.next()) {
                    Blob encodedItems = rs.getBlob("items");
                    try {
                        List<ItemStack> items;
                        try {
                            items = encodedItems != null ? new ArrayList<>(Arrays.asList(SerializeInventory.itemStackArrayFromBinaryStream(encodedItems.getBinaryStream()))) : new ArrayList<>();
                        } catch (StreamCorruptedException e) {
                            items = new ArrayList<>();
                        }
                        UUID receiver = UUID.fromString(rs.getString("uuid"));
                        UUID setterUUID = rs.getString("suuid").equalsIgnoreCase("CONSOLE") ? new UUID(0, 0) : UUID.fromString(rs.getString("suuid"));
                        Setter setter = new Setter(rs.getString("setter"), setterUUID, rs.getDouble("amount"), items, rs.getLong("time"), rs.getBoolean("notified"), decodeWhitelist(rs.getString("whitelist")), rs.getLong("playtime"));
                        if (expiredBounties.containsKey(receiver)) {
                            expiredBounties.get(receiver).add(setter);
                        } else {
                            expiredBounties.put(receiver, new ArrayList<>(List.of(setter)));
                        }
                    } catch (IOException e) {
                        // error parsing encoded items
                        Bukkit.getLogger().warning("[NotBounties] Error decoding items from SQL");
                        Bukkit.getLogger().warning(e.toString());
                    }
                }
                ps.executeUpdate();
            }
            if (autoExpire > -1) {
                ResultSet rs = ps3.executeQuery();
                while (rs.next()) {
                    Blob encodedItems = rs.getBlob("items");
                    try {
                        List<ItemStack> items;
                        try {
                            items = encodedItems != null ? new ArrayList<>(Arrays.asList(SerializeInventory.itemStackArrayFromBinaryStream(encodedItems.getBinaryStream()))) : new ArrayList<>();
                        } catch (StreamCorruptedException e) {
                            items = new ArrayList<>();
                        }
                        UUID receiver = UUID.fromString(rs.getString("uuid"));
                        UUID setterUUID = rs.getString("suuid").equalsIgnoreCase("CONSOLE") ? new UUID(0, 0) : UUID.fromString(rs.getString("suuid"));
                        Setter setter = new Setter(rs.getString("setter"), setterUUID, rs.getDouble("amount"), items, rs.getLong("time"), rs.getBoolean("notified"), decodeWhitelist(rs.getString("whitelist")), rs.getLong("playtime"));
                        if (expiredBounties.containsKey(receiver)) {
                            expiredBounties.get(receiver).add(setter);
                        } else {
                            expiredBounties.put(receiver, new ArrayList<>(List.of(setter)));
                        }
                    } catch (IOException e) {
                        // error parsing encoded items
                        Bukkit.getLogger().warning("[NotBounties] Error decoding items from SQL");
                        Bukkit.getLogger().warning(e.toString());
                    }
                }
                ps2.executeUpdate();
            }
            return expiredBounties;
        } catch (SQLException e){
            if (reconnect()){
               return removeOldBounties();
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return new HashMap<>();
    }

    public List<Bounty> getTopBounties(int sortType) {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("SELECT * FROM notbounties;")){

            ResultSet resultSet = ps.executeQuery();

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
                        Setter setter = new Setter(resultSet.getString("setter"), UUID.fromString(resultSet.getString("suuid")), resultSet.getDouble("amount"), items, resultSet.getLong("time"), resultSet.getBoolean("notified"), decodeWhitelist(resultSet.getString("whitelist")), resultSet.getLong("playtime"), resultSet.getDouble("display"));
                        if (bountyAmounts.containsKey(uuid)){
                            bountyAmounts.get(uuid).addBounty(setter);
                        } else {
                            bountyAmounts.put(uuid, new Bounty(UUID.fromString(uuid), new ArrayList<>(Collections.singletonList(setter)), resultSet.getString("name")));
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
                    if ((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                            (sortedList.get(i).getSetters().get(0).getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 1) || // newest bounties at top
                            (sortedList.get(i).getTotalDisplayBounty() < sortedList.get(j).getTotalDisplayBounty() && sortType == 2) || // more expensive bounties at top
                            (sortedList.get(i).getTotalDisplayBounty() > sortedList.get(j).getTotalDisplayBounty() && sortType == 3)) { // less expensive bounties at top
                        temp = sortedList.get(i);
                        sortedList.set(i, sortedList.get(j));
                        sortedList.set(j, temp);
                    }
                }
            }
            return sortedList;
        } catch (SQLException e){
            if (reconnect()){
                return getTopBounties(sortType);
            }
            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return new ArrayList<>();

    }

    public int removeExtraData() {
        try (PreparedStatement ps = SQL.getConnection().prepareStatement("DELETE FROM notbounties WHERE amount = ?;")){
            ps.setDouble(1, 0L);
            return ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                return removeExtraData();
            }

            if (NotBounties.debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    private boolean reconnect() {
        if (System.currentTimeMillis() > nextReconnectAttempt) {
            reconnectAttempts++;
            SQL.disconnect();
            if (reconnectAttempts < 2) {
                Bukkit.getLogger().warning("Lost connection with database, will try to reconnect.");
            }
            if (reconnectAttempts >= 3) {
                reconnectAttempts = 0;
                nextReconnectAttempt = System.currentTimeMillis() + 5000L;
            }

            if (!tryToConnect()) {
                if (reconnectAttempts < 2)
                    Bukkit.getScheduler().runTaskLater(NotBounties.getInstance(), BountyManager::tryToConnect, 20L);
                return false;
            }
            return true;
        }
        return false;
    }

}

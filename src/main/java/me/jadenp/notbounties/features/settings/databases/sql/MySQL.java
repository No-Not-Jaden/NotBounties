package me.jadenp.notbounties.features.settings.databases.sql;

import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.features.settings.databases.NotBountiesDatabase;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.utils.SerializeInventory;
import me.jadenp.notbounties.data.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.sql.*;
import java.util.*;

// remove bounty server id
public class MySQL extends NotBountiesDatabase {
    private Connection connection;
    private boolean hasConnected = false;
    private long nextReconnectAttempt;
    private int reconnectAttempts;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    public MySQL(Plugin plugin, String name){
        super(plugin, name);

        nextReconnectAttempt = System.currentTimeMillis();
        reconnectAttempts = 0;
    }

    @Override
    protected ConfigurationSection readConfig() {
        ConfigurationSection configuration = super.readConfig();
        if (configuration == null)
            return null;
        host = configuration.isSet( "host") ? configuration.getString( "host") : "localhost";
        port = configuration.isSet( "port") ? configuration.getInt( "port") : 3306;
        database = configuration.isSet( "database") ? configuration.getString( "database") : "db";
        username = configuration.isSet( "user") ? configuration.getString( "user") : "user";
        password = configuration.isSet( "password") ? configuration.getString( "password") : "";
        useSSL = configuration.isSet( "use-ssl") && configuration.getBoolean( "use-ssl");
        return configuration;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(host, port, database, username, password, useSSL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MySQL mySQL = (MySQL) o;
        return hasConnected == mySQL.hasConnected && nextReconnectAttempt == mySQL.nextReconnectAttempt
                && reconnectAttempts == mySQL.reconnectAttempts && port == mySQL.port && useSSL == mySQL.useSSL
                && Objects.equals(connection, mySQL.connection) && Objects.equals(host, mySQL.host)
                && Objects.equals(database, mySQL.database) && Objects.equals(username, mySQL.username)
                && Objects.equals(password, mySQL.password);
    }

    public String getDatabase() {
        return database;
    }

    public boolean connect() {
        if (isConnected())
            try {
                connection.close();
            } catch (SQLException ignored) {
                // unable to access database
                return false;
            }
        connection = null;
        readConfig();
        return makeConnection();
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO bounty_data(uuid, claimed, sets, received, alltime, immunity, allclaimed) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE claimed = claimed + ?, sets = sets + ?, received = received + ?, alltime = alltime + ?, immunity = immunity + ?, allclaimed = allclaimed + ?;")){
            ps.setString(1, uuid.toString());
            ps.setLong(2, stats.kills());
            ps.setLong(3, stats.set());
            ps.setLong(4, stats.deaths());
            ps.setDouble(5, stats.all());
            ps.setDouble(6, stats.immunity());
            ps.setDouble(7, stats.claimed());
            ps.setLong(8, stats.kills());
            ps.setLong(9, stats.set());
            ps.setLong(10, stats.deaths());
            ps.setDouble(11, stats.all());
            ps.setDouble(12, stats.immunity());
            ps.setDouble(13, stats.claimed());
            ps.executeUpdate();

        } catch (SQLException e){
            reconnect(e);
        }
    }

    @Override
    public @NotNull PlayerStat getStats(UUID uuid) throws IOException {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT claimed, sets, received, alltime, immunity, allclaimed FROM bounty_data WHERE uuid = ?;")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                long kills = rs.getLong("claimed");
                long set = rs.getLong("sets");
                long deaths = rs.getLong("received");
                double all = rs.getDouble("alltime");
                double immunity = rs.getDouble("immunity");
                double claimed = rs.getDouble("allclaimed");
                return new PlayerStat(kills, set, deaths, all, immunity, claimed, DataManager.GLOBAL_SERVER_ID);
            } catch (SQLException e) {
                if (reconnect(e)) {
                    return getStats(uuid);
                }
            }
        }
        throw notConnectedException;
    }

    @Override
    public Map<UUID, PlayerStat> getAllStats() throws IOException{
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, claimed, sets, received, alltime, immunity, allclaimed FROM bounty_data;")) {
                ResultSet rs = ps.executeQuery();

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
            } catch (SQLException e) {
                if (reconnect(e)) {
                    return getAllStats();
                }
            }
        }
        throw notConnectedException;
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO bounty_data(uuid, claimed, sets, received, alltime, immunity, allclaimed) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE claimed = claimed + ?, sets = sets + ?, received = received + ?, alltime = alltime + ?, immunity = immunity + ?, allclaimed = allclaimed + ?;")) {
                for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet()) {
                    PlayerStat stats = entry.getValue();
                    ps.setString(1, entry.getKey().toString());
                    ps.setLong(2, stats.kills());
                    ps.setLong(3, stats.set());
                    ps.setLong(4, stats.deaths());
                    ps.setDouble(5, stats.all());
                    ps.setDouble(6, stats.immunity());
                    ps.setDouble(7, stats.claimed());
                    ps.setLong(8, stats.kills());
                    ps.setLong(9, stats.set());
                    ps.setLong(10, stats.deaths());
                    ps.setDouble(11, stats.all());
                    ps.setDouble(12, stats.immunity());
                    ps.setDouble(13, stats.claimed());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                reconnect(e);
            }
        }

    }

    @Override
    public void addBounty(List<Bounty> bounties) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO notbounties(uuid, name, setter, suuid, amount, notified, created, whitelist, playtime, items, display) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                for (Bounty bounty : bounties) {
                    for (Setter setter : bounty.getSetters()) {
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
                        ps.addBatch();
                    }
                }
                ps.executeBatch();

            } catch (SQLException e) {
                if (reconnect(e)) {
                    addBounty(bounties);
                }
            }
        }
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ? AND suuid = ? AND created = ?;")) {
                for (Bounty bounty : bounties) {
                    ps.setString(1, bounty.getUUID().toString());
                    for (Setter setter : bounty.getSetters()) {
                        ps.setString(2, setter.getUuid().toString());
                        ps.setLong(3, setter.getTimeCreated());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            } catch (SQLException e) {
                if (reconnect(e)) {
                    removeBounty(bounties);
                }
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
            return new Whitelist(new ArrayList<>(), false);
        boolean blacklist = whitelist.contains(".");
        String[] split = blacklist ? whitelist.split("\\.") : whitelist.split(",");
        List<UUID> uuids = new ArrayList<>();
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
        if (!combinedUuids.isEmpty() && combinedUuids.length() > 37)
            combinedUuids.deleteCharAt(combinedUuids.length()-1);
        return combinedUuids.toString();
    }

    @Override
    public Bounty addBounty(@NotNull Bounty bounty) throws IOException {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO notbounties(uuid, name, setter, suuid, amount, notified, created, whitelist, playtime, items, display) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                for (Setter setter : bounty.getSetters()) {
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
                    ps.addBatch();
                }
                ps.executeBatch();

            } catch (SQLException e) {
                if (reconnect(e)) {
                    return addBounty(bounty);
                }
            }
            try {
                return getBounty(bounty.getUUID());
            } catch (IOException ignored) {
                // database has lost connection
            }
        }
        throw notConnectedException;
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

    @Override
    public @Nullable Bounty getBounty(UUID uuid) throws IOException{
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM `notbounties` WHERE uuid = ?;")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                List<Setter> setters = new ArrayList<>();
                String bountyName = "";
                while (rs.next()) {
                    if (bountyName.isEmpty()) {
                        bountyName = rs.getString("name");
                    }
                    Blob encodedItems = rs.getBlob("items");
                    List<ItemStack> items = convertEncodedItems(encodedItems);

                    UUID setterUUID = rs.getString("suuid").equalsIgnoreCase("CONSOLE") ? new UUID(0, 0) : UUID.fromString(rs.getString("suuid"));
                    Setter setter = new Setter(rs.getString("setter"), setterUUID, rs.getDouble("amount"), items, rs.getLong("created"), rs.getBoolean("notified"), decodeWhitelist(rs.getString("whitelist")), rs.getLong("playtime"), rs.getDouble("display"));
                    setters.add(setter);
                }
                if (setters.isEmpty())
                    return null;
                return new Bounty(uuid, setters, bountyName);
            } catch (SQLException e) {
                if (reconnect(e)) {
                    return getBounty(uuid);
                }
            }
        }
        throw new IOException("Database not connected!");
    }

    @Override
    public void removeBounty(UUID uuid) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                if (reconnect(e)) {
                    removeBounty(uuid);
                }
            }
        }
    }

    @Override
    public void removeBounty(Bounty bounty) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ? AND suuid = ? AND created = ?;")) {
                ps.setString(1, bounty.getUUID().toString());
                for (Setter setter : bounty.getSetters()) {
                    ps.setString(2, setter.getUuid().toString());
                    ps.setLong(3, setter.getTimeCreated());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                if (reconnect(e)) {
                    removeBounty(bounty);
                }
            }
        }
    }

    @Override
    public List<Bounty> getAllBounties(int sortType) throws IOException {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, name, setter, suuid, amount, notified, created, whitelist, playtime, items, display FROM notbounties;")) {
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
                            long timeCreated = resultSet.getLong("created");
                            Setter setter = new Setter(resultSet.getString("setter"), UUID.fromString(resultSet.getString("suuid")), resultSet.getDouble("amount"), items, timeCreated, resultSet.getBoolean("notified"), decodeWhitelist(resultSet.getString("whitelist")), resultSet.getLong("playtime"), resultSet.getDouble("display"));
                            if (bountyAmounts.containsKey(uuid)) {
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
                        if (!sortedList.get(i).getSetters().isEmpty()
                                && ((sortedList.get(i).getSetters().get(0).getTimeCreated() > sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                                (sortedList.get(i).getSetters().get(0).getTimeCreated() < sortedList.get(j).getSetters().get(0).getTimeCreated() && sortType == 1) || // newest bounties at top
                                (sortedList.get(i).getTotalDisplayBounty() < sortedList.get(j).getTotalDisplayBounty() && sortType == 2) || // more expensive bounties at top
                                (sortedList.get(i).getTotalDisplayBounty() > sortedList.get(j).getTotalDisplayBounty() && sortType == 3))) { // less expensive bounties at top
                            temp = sortedList.get(i);
                            sortedList.set(i, sortedList.get(j));
                            sortedList.set(j, temp);
                        }
                    }
                }
                return sortedList;
            } catch (SQLException e) {
                if (reconnect(e)) {
                    return getAllBounties(sortType);
                }
            }
        }
        throw notConnectedException;
    }

    public boolean isConnected() {
        if (connection == null)
            return false;
        try {
            if (connection.isClosed())
                return false;
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    public boolean hasConnectedBefore() {
        return hasConnected;
    }

    private boolean makeConnection(){
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&allowMultiQueries=true",
                    username, password);
            if (!hasConnected) {
                createTable();
                createDataTable();
                createOnlinePlayerTable();
            }
            DataManager.databaseConnect(this);
            hasConnected = true;
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean reconnect(SQLException e) {
        if (e instanceof SQLSyntaxErrorException) {
            Bukkit.getLogger().warning("[NotBounties] SQL Syntax Error! Please report this to Not_Jaden.");
            Bukkit.getLogger().warning(e::toString);
            return false;
        }
        NotBounties.debugMessage(e.toString(), true);
        if (System.currentTimeMillis() > nextReconnectAttempt) {
            reconnectAttempts++;
            disconnect();
            if (reconnectAttempts >= 3) {
                reconnectAttempts = 0;
                nextReconnectAttempt = System.currentTimeMillis() + 5000L;
            }

            if (!connect()) {
                if (reconnectAttempts < 2)
                    Bukkit.getScheduler().runTaskLater(NotBounties.getInstance(), this::connect, 20L);
                return false;
            }
            return true;
        }
        return false;
    }

    public void createTable(){
        try (PreparedStatement ps = getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS notbounties" +
                "(" +
                "    uuid CHAR(36) NOT NULL," +
                "    name VARCHAR(16) NOT NULL," +
                "    setter VARCHAR(16) NOT NULL," +
                "    suuid CHAR(36) NOT NULL," +
                "    amount FLOAT(53) DEFAULT 0 NOT NULL," +
                "    notified BOOLEAN DEFAULT TRUE NOT NULL," +
                "    created BIGINT NOT NULL," +
                "    whitelist VARCHAR(369)," +
                "    playtime BIGINT DEFAULT 0 NOT NULL," +
                "    items BLOB," +
                "    display FLOAT(53) DEFAULT -1 NOT NULL" +
                ");");
             PreparedStatement checkAmount = getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'notbounties' and column_name = 'amount';");
             PreparedStatement alterAmount = getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN amount FLOAT(53);");
             PreparedStatement checkWhitelist = getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'whitelist';");
             PreparedStatement addWhitelist = getConnection().prepareStatement("ALTER TABLE notbounties ADD whitelist VARCHAR(369);");
             PreparedStatement checkPlaytime = getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'playtime';");
             PreparedStatement addPlaytime = getConnection().prepareStatement("ALTER TABLE notbounties ADD playtime BIGINT DEFAULT 0 NOT NULL;");
             PreparedStatement checkItems = getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'items';");
             PreparedStatement addItems = getConnection().prepareStatement("ALTER TABLE notbounties ADD items BLOB;");
             PreparedStatement checkItemsType = getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'notbounties' and column_name = 'items';");
             PreparedStatement alterItems = getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN items BLOB;");
             PreparedStatement checkDisplay = getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'display';");
             PreparedStatement addDisplay = getConnection().prepareStatement("ALTER TABLE notbounties ADD display FLOAT(53) DEFAULT -1 NOT NULL;");
             PreparedStatement checkTimeName = getConnection().prepareStatement("select column_name from information_schema.columns where table_schema = ? and table_name = 'notbounties' and column_name = 'time';");
             PreparedStatement alterTimeName = getConnection().prepareStatement("ALTER TABLE notbounties CHANGE time created BIGINT;")){


            ps.executeUpdate();

            checkAmount.setString(1, getDatabase());
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

            checkItemsType.setString(1, getDatabase());
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

            checkTimeName.setString(1, getDatabase());
            rs = checkTimeName.executeQuery();
            if (rs.next()) {
                alterTimeName.executeUpdate();
            }
            rs.close();

        } catch (SQLException e){
            Bukkit.getLogger().warning(e.toString());
        }
    }
    public void createDataTable(){
        try (PreparedStatement ps = getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS bounty_data" +
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
             PreparedStatement ps1 = getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'bounty_data' and column_name = 'claimed';");
             PreparedStatement ps2 = getConnection().prepareStatement("ALTER TABLE bounty_data MODIFY COLUMN claimed BIGINT, MODIFY sets BIGINT, MODIFY received BIGINT, MODIFY alltime FLOAT(53), MODIFY immunity FLOAT(53), MODIFY allclaimed FLOAT(53);")) {

            ps.executeUpdate();

            ps1.setString(1, getDatabase());
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
        try(PreparedStatement ps = getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS bounty_players" +
                "(" +
                "    uuid CHAR(36) NOT NULL," +
                "    name VARCHAR(16) NOT NULL," +
                "    id CHAR(36) NOT NULL," +
                "    PRIMARY KEY (uuid)" +
                ");");
            PreparedStatement checkServerID = getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'bounty_players' and column_name = 'id';");
            PreparedStatement alterServerID = getConnection().prepareStatement("ALTER TABLE bounty_players MODIFY COLUMN id CHAR(36);")) {

            ps.executeUpdate();

            checkServerID.setString(1, getDatabase());
            ResultSet rs = checkServerID.executeQuery();
            if (rs.next() && rs.getString("data_type").equalsIgnoreCase("int")){
                Bukkit.getLogger().info("Updating data type for online players.");
                alterServerID.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.toString());
        }
    }

    public void disconnect(){
        if (isConnected())
            try {
                connection.close();
            } catch (SQLException e) {
                Bukkit.getLogger().warning(e.toString());
            }
        connection = null;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() {
        Map<UUID, String> networkPlayers = new HashMap<>();
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, name FROM bounty_players;")) {
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    String playerName = rs.getString("name");
                    networkPlayers.put(UUID.fromString(uuid), playerName);
                }
            } catch (SQLException e) {
                if (reconnect(e)) {
                    return getOnlinePlayers();
                }
            }
        }
        return networkPlayers;
    }

    @Override
    public void shutdown() {
        disconnect();
    }

    @Override
    public void notifyBounty(UUID uuid) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("UPDATE notbounties SET notified = 1 WHERE uuid = ?;")) {
                ps.setString(1, uuid.toString());
                // if a player joins after a bounty is placed, but before the next sync, then it won't be updated if the time is included, and they will get a duplicate notification
                // without the time, if a player joins after a bounty is placed on another server and this server, then they won't get the notification for the other bounty.
                //ps.setLong(2, getLastSync());
                ps.executeUpdate();

            } catch (SQLException e) {
                if (reconnect(e)) {
                    notifyBounty(uuid);
                }
            }
        }
    }

    @Override
    public void logout(UUID uuid) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM bounty_players WHERE uuid = ? AND id = ?;")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, DataManager.getDatabaseServerID(false).toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                if (reconnect(e)) {
                    logout(uuid);
                }
            }
        }
    }

    @Override
    public void login(UUID uuid, String playerName) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO bounty_players(uuid, name, id) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE id = ?;")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setString(3, DataManager.getDatabaseServerID(false).toString());
                ps.setString(4, DataManager.getDatabaseServerID(false).toString());
                ps.executeUpdate();

            } catch (SQLException e) {
                if (reconnect(e)) {
                    login(uuid, playerName);
                }
            }
        }
    }
}

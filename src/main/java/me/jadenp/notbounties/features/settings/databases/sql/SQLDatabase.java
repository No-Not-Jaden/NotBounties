package me.jadenp.notbounties.features.settings.databases.sql;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.settings.databases.BountySortType;
import me.jadenp.notbounties.features.settings.databases.DatabaseConnectionException;
import me.jadenp.notbounties.features.settings.databases.NotBountiesDatabase;
import me.jadenp.notbounties.features.settings.databases.StatSortType;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.SerializeInventory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// TODO: remove isConnected checksq (done in reconnectWrapper)
// TODO: replace uuid.toString() with convertToBinary and setBytes
public class SQLDatabase extends NotBountiesDatabase {

    @FunctionalInterface
    private interface TransactionalOperation {
        void run() throws SQLException;
    }

    private void executeTransaction(TransactionalOperation operation) throws SQLException {
        try {
            connection.setAutoCommit(false);

            operation.run();

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private Connection connection;
    private final Logger logger;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    public SQLDatabase(Plugin plugin, String name) {
        super(plugin, name);
        this.logger = plugin.getLogger();
    }

    @Override
    protected ConfigurationSection readConfig() {
        ConfigurationSection configuration = super.readConfig();
        if (configuration == null)
            return null;
        host = configuration.isSet("host") ? configuration.getString("host") : "localhost";
        port = configuration.isSet("port") ? configuration.getInt("port") : 3306;
        database = configuration.isSet("database") ? configuration.getString("database") : "db";
        username = configuration.isSet("user") ? configuration.getString("user") : "user";
        password = configuration.isSet("password") ? configuration.getString("password") : "";
        useSSL = configuration.isSet("use-ssl") && configuration.getBoolean("use-ssl");
        return configuration;
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

    @Override
    public void disconnect() {
        if (isConnected())
            try {
                connection.close();
            } catch (SQLException e) {
                Bukkit.getLogger().warning(e.toString());
            }
        connection = null;
    }

    private boolean makeConnection(boolean syncData) {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&allowMultiQueries=true",
                    username, password);
            if (!hasConnected) {
                // first connection
                connection.setAutoCommit(false);
                initDatabase();
            }
            connection.setAutoCommit(true);
            if (syncData) {
                DataManager.databaseConnect(this);
            }
            hasConnected = true;
            return true;
        } catch (SQLException e) {
            // could not connect to database
            return false;
        }
    }

    private void initDatabase() throws SQLException {
        // read schema.sql and execute
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream("sql/schema.sql");
             Statement statement = connection.createStatement()) {

            if (in == null) {
                return;
            }

            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            String[] statements = sql.split(";");

            for (String stmt : statements) {
                if (!stmt.isBlank()) {
                    statement.execute(stmt);
                }
            }
        } catch (IOException | SQLException e) {
            throw new SQLException(e);
        }
    }

    private Connection getConnection() {
        return connection;
    }

    private byte[] convertToBinary(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private UUID bytesToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO stat(uuid, b_claimed, b_set, b_received, b_all_time, immunity, b_claim_amt) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE b_claimed = b_claimed + ?, b_sets = b_sets + ?, b_received = b_received + ?, b_all_time = b_all_time + ?, immunity = immunity + ?, b_claim_amt = b_claim_amt + ?;"
        )) {
            ps.setBytes(1, convertToBinary(uuid));
            prepareStatInsert(ps, stats);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public @NotNull PlayerStat getStats(UUID uuid) throws DatabaseConnectionException {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "SELECT b_claimed, b_set, b_received, b_all_time, immunity, b_claim_amt FROM stat WHERE uuid = ?;"
            )) {
                ps.setBytes(1, convertToBinary(uuid));
                try (ResultSet rs = ps.executeQuery()) {
                    return new PlayerStat(
                            rs.getLong(1),
                            rs.getLong(2),
                            rs.getLong(3),
                            rs.getDouble(4),
                            rs.getDouble(5),
                            rs.getDouble(6),
                            DataManager.GLOBAL_SERVER_ID
                    );
                }
            } catch (SQLException e) {
                throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
            }
        }
        throw notConnectedException;
    }

    @Override
    public Map<UUID, PlayerStat> getStats(Leaderboard sortStat, StatSortType sortType, UUID lastUuid, Object lastVal, int limit) throws DatabaseConnectionException {
        Map<UUID, PlayerStat> stats = new HashMap<>();
        String sql = buildStatPageQuery(sortStat, sortType);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (sortStat.isMoney()) {
                stmt.setDouble(1, (double) lastVal);
            } else {
                stmt.setInt(1, (int) lastVal);
            }
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(bytesToUUID(rs.getBytes("uuid")), parseStatResult(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
        return stats;
    }

    private static String buildStatPageQuery(
            Leaderboard stat,
            StatSortType sort
    ) {

        StringBuilder sql = new StringBuilder();

        sql.append("""
                    SELECT
                        stat.uuid,
                        b_claimed,
                        b_set,
                        b_received,
                        b_all_time,
                        immunity,
                        b_claim_amt
                    FROM stat
                """);

        if (sort.requiresPlayerJoin()) {
            sql.append("""
                        JOIN player
                            ON stat.uuid = player.uuid
                    """);
        }

        String sortColumn = sort.sqlColumn() != null ? sort.sqlColumn() : stat.getDatabaseName();

        sql.append("""
                    WHERE %s %s ?
                    ORDER BY %s %s
                    LIMIT ?
                """.formatted(
                sortColumn,
                sort.comparison(),
                sortColumn,
                sort.order()
        ));

        return sql.toString();
    }

    private PlayerStat parseStatResult(ResultSet rs) throws SQLException {
        return new PlayerStat(
                rs.getInt("b_claimed"),
                rs.getInt("b_set"),
                rs.getInt("b_received"),
                rs.getDouble("b_all_time"),
                rs.getDouble("immunity"),
                rs.getDouble("b_claim_amt"),
                DataManager.GLOBAL_SERVER_ID
        );
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) {
        if (isConnected()) {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO stat(uuid, b_claimed, b_set, b_received, b_all_time, immunity, b_claim_amt) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE b_claimed = b_claimed + ?, b_sets = b_sets + ?, b_received = b_received + ?, b_all_time = b_all_time + ?, immunity = immunity + ?, b_claim_amt = b_claim_amt + ?;"
            )) {
                for (Map.Entry<UUID, PlayerStat> entry : playerStats.entrySet()) {
                    PlayerStat stats = entry.getValue();
                    ps.setBytes(1, convertToBinary(entry.getKey()));
                    prepareStatInsert(ps, stats);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
            }
        } else {
            throw notConnectedException;
        }

    }

    private void prepareStatInsert(PreparedStatement ps, PlayerStat stats) throws SQLException {
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
    }

    @Override
    public void addBounty(List<Bounty> bounties) {
        try {
            executeTransaction(() -> insertBountiesBatch(bounties));
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    private void deleteBounties(List<Bounty> bounties) throws SQLException {
        List<Integer> itemIds = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM bounty WHERE bounty_id = ?;")) {
            for (Bounty bounty : bounties) {
                for (Setter setter : bounty.getSetters()) {
                    if (setter.getID() != null) {
                        ps.setInt(1, setter.getID());
                        if (setter.getItemsId() != null) {
                            itemIds.add(setter.getItemsId());
                        }
                    }
                }
            }
            ps.executeBatch();
        }
        deleteItemIds(itemIds);
    }

    @Override
    public void removeBounty(List<Bounty> bounties) {
        try {
            executeTransaction(() -> deleteBounties(bounties));
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    /**
     * Adds the items in the setters to the database and sets the setter's item id value.
     * @param setters Setters to add to the database.
     * @throws SQLException If an error occurred while accessing the database.
     */
    private void addItemsBatch(List<Setter> setters) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO item (item_list)
                        VALUES (?)
                        """,
                Statement.RETURN_GENERATED_KEYS
        )) {
            for (Setter setter : setters) {
                if (setter.hasItems()) {
                    stmt.setBlob(1, SerializeInventory.itemStackArrayToBinaryStream(setter.getItems().toArray(new ItemStack[0])));
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                ListIterator<Setter> iterator = setters.listIterator();
                while (keys.next() && iterator.hasNext()) {
                    Setter setter = iterator.next();
                    setter.setItemsId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Adds the whitelist of each setter to the database.
     * @param setters Setters to add.
     * @throws SQLException If an error occurs while accessing the database.
     */
    private void addBountyWhitelistsBatch(List<Setter> setters) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO bounty_whitelist (bounty_id, uuid)
                        VALUES (?, ?);
                        """
        )) {
            for (Setter setter : setters) {
                stmt.setInt(1, setter.getID());
                for (UUID uuid : setter.getWhitelist().getList()) {
                    stmt.setBytes(2, convertToBinary(uuid));
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    /**
     * Adds the bounty objects to the database and sets the bounty id for each setter.
     * This does not add any items to the database.
     * @param bounties Bounties to add.
     * @throws SQLException If an error occurs when accessing the database.
     */
    private void addBountiesBatch(List<Bounty> bounties) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO bounty (
                            setter,
                            receiver,
                            item_id,
                            amount,
                            display,
                            notified,
                            time_placed,
                            playtime
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                Statement.RETURN_GENERATED_KEYS
        )) {
            for (Bounty bounty : bounties) {
                for (Setter setter : bounty.getSetters()) {
                    stmt.setBytes(1, convertToBinary(setter.getUuid()));
                    stmt.setBytes(2, convertToBinary(bounty.getUUID()));
                    if (setter.getItemsId() == null) {
                        stmt.setNull(3, Types.INTEGER);
                    } else {
                        stmt.setInt(3, setter.getItemsId());
                    }
                    stmt.setDouble(4, setter.getAmount());
                    stmt.setDouble(5, setter.getDisplayAmount());
                    stmt.setBoolean(6, setter.isNotified());
                    stmt.setLong(7, setter.getTimeCreated());
                    stmt.setLong(8, setter.getReceiverPlaytime());

                    stmt.addBatch();
                }
            }

            stmt.executeBatch();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                for (Bounty bounty : bounties) {
                    ListIterator<Setter> setterListIterator = bounty.getSetters().listIterator();
                    while (keys.next() && setterListIterator.hasNext()) {
                        setterListIterator.next().setId(keys.getInt(1));
                    }
                }
            }
        }
    }

    private void insertBountiesBatch(List<Bounty> bounties) throws SQLException {
        List<Setter> allSetters = new ArrayList<>();
        bounties.forEach(bounty -> allSetters.addAll(bounty.getSetters()));
        addItemsBatch(allSetters);
        addBountiesBatch(bounties);
        addBountyWhitelistsBatch(allSetters);
        addTagsBatch(allSetters);
    }

    private void addTagsBatch(List<Setter> setters) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO tag(
                                        bounty_id,
                                        tag_value
                        )
                        VALUES (?, ?);
                        """
        )) {
            for (Setter setter : setters) {
                stmt.setInt(1, setter.getID());
                for (String tag : setter.getTags()) {
                    stmt.setString(2, tag);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    private void insertBounty(@NotNull Bounty bounty) throws SQLException {
        insertBountiesBatch(Collections.singletonList(bounty));
    }

    @Override
    public Bounty addBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        try {
            executeTransaction(() -> insertBounty(bounty));
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }

        return getBounty(bounty.getUUID());
    }

    /**
     * Converts a blob of encoded ItemStacks into a list. The blob is freed afterward.
     *
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
            items = Collections.emptyList();
        }
        return items;
    }

    private List<ItemStack> getItems(int itemId) throws SQLException {
        if (itemId <= 0) {
            return new ArrayList<>();
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        SELECT item_list FROM item
                        WHERE item_id = ?;
                        """
        )) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return convertEncodedItems(rs.getBlob("item_list"));
                } else {
                    return new ArrayList<>();
                }
            }
        }
    }

    private Map<Integer, List<ItemStack>> getItems(List<Integer> itemIds) throws SQLException {
        String placeholders = itemIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql =
                """
                        SELECT item_id, item_list
                        FROM item
                        WHERE item_id IN (%s);
                        """.formatted(placeholders);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < itemIds.size(); i++) {
                stmt.setInt(i + 1, itemIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                Map<Integer, List<ItemStack>> items = new HashMap<>();
                while (rs.next()) {
                    items.put(rs.getInt("item_id"), convertEncodedItems(rs.getBlob("item_list")));
                }
                return items;
            }
        }
    }

    private SortedSet<UUID> getWhitelist(int bountyId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        SELECT uuid FROM bounty_whitelist
                        WHERE bounty_id = ?;
                        """
        )) {
            stmt.setInt(1, bountyId);
            return readWhitelist(stmt);
        }
    }

    private SortedSet<UUID> getWhitelist(UUID uuid) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        SELECT uuid FROM whitelist
                        WHERE owner = ?;
                        """
        )) {
            stmt.setBytes(1, convertToBinary(uuid));
            return readWhitelist(stmt);
        }
    }

    /**
     * Get whitelists of many bounty ids.
     * @param bountyIds Ids of bounties.
     * @return A map of bounty_id, whitelisted uuids
     * @throws SQLException If an error occurred with the database.
     */
    private Map<Integer, SortedSet<UUID>> getWhitelists(List<Integer> bountyIds) throws SQLException {
        String placeholders = bountyIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql =
                """
                        SELECT bounty_id, uuid
                        FROM bounty_whitelist
                        WHERE bounty_id IN (%s);
                        """.formatted(placeholders);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < bountyIds.size(); i++) {
                stmt.setInt(i + 1, bountyIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                Map<Integer, SortedSet<UUID>> whitelist = new HashMap<>();
                while (rs.next()) {
                    whitelist.put(rs.getInt("bounty_id"), readWhitelist(stmt));
                }
                return whitelist;
            }
        }
    }

    private SortedSet<UUID> readWhitelist(PreparedStatement stmt) throws SQLException {
        SortedSet<UUID> whitelist = new TreeSet<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                whitelist.add(UUID.fromString(rs.getString("uuid")));
            }
        }
        return whitelist;
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) throws DatabaseConnectionException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        SELECT
                               bounty_id,
                               setter,
                               item_id,
                               amount,
                               display,
                               notified,
                               time_placed,
                               playtime,
                               whitelist_mode
                        FROM bounty
                        WHERE receiver = ?;
                        """
        )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {

                List<Setter> setters = new LinkedList<>();
                List<Integer> bountyIds = new LinkedList<>();
                while (rs.next()) {
                    Setter setter = readSetter(rs);
                    setters.add(setter);
                    bountyIds.add(setter.getID());
                }
                if (setters.isEmpty())
                    return null;
                Map<Integer, SortedSet<UUID>> whitelists = getWhitelists(bountyIds);
                for (Setter setter : setters) {
                    setter.getWhitelist().setList(whitelists.getOrDefault(setter.getID(), Collections.emptySortedSet()));
                }

                return new Bounty(uuid, setters);
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    private Map<UUID, Bounty> getBounties(List<UUID> uuids) throws SQLException {
        if (uuids.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = uuids.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql =
                """
                SELECT
                       receiver,
                       bounty_id,
                       setter,
                       item_id,
                       amount,
                       display,
                       notified,
                       time_placed,
                       playtime,
                       whitelist_mode
                FROM bounty
                WHERE receiver IN (%s);
                """.formatted(placeholders);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < uuids.size(); i++) {
                stmt.setBytes(i + 1, convertToBinary(uuids.get(i)));
            }
            Map<UUID, Bounty> bounties = new HashMap<>();
            List<Integer> bountyIds = new LinkedList<>();
            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID receiverUUID = bytesToUUID(rs.getBytes("receiver"));
                    Setter setter = readSetter(rs);
                    if (bounties.containsKey(receiverUUID)) {
                        bounties.get(receiverUUID).getSetters().add(setter);
                    } else {
                        bounties.put(receiverUUID, new Bounty(receiverUUID, new LinkedList<>(Collections.singletonList(setter))));
                    }
                    bountyIds.add(setter.getID());
                }
            }
            if (bounties.isEmpty())
                return Collections.emptyMap();
            Map<Integer, SortedSet<UUID>> whitelists = getWhitelists(bountyIds);
            for (Bounty bounty : bounties.values()) {
                for (Setter setter : bounty.getSetters()) {
                    setter.getWhitelist().setList(whitelists.getOrDefault(setter.getID(), Collections.emptySortedSet()));
                }
            }
            return bounties;
        }
    }

    private Setter readSetter(ResultSet rs) throws SQLException {
        int bountyId = rs.getInt("bounty_id");
        UUID setterUUID = bytesToUUID(rs.getBytes("setter"));
        int tempItemId = rs.getInt("item_id");
        Integer itemId = rs.wasNull() ? null : tempItemId;
        double amount = rs.getDouble("amount");
        double display = rs.getDouble("display");
        boolean notified = rs.getBoolean("notified");
        long time = rs.getLong("time_placed");
        long playtime = rs.getLong("playtime");
        boolean whitelistMode = rs.getBoolean("whitelist_mode");
        Whitelist whitelist = new Whitelist(Collections.emptySortedSet(), whitelistMode);
        return new Setter(bountyId, setterUUID, amount, itemId, time, notified, whitelist, playtime, display, Collections.emptySet());
    }

    private List<Integer> getItemIds(UUID uuid) throws SQLException {
        List<Integer> itemIds = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        SELECT item_id
                        FROM bounty
                        WHERE receiver = ?
                        AND item_id IS NOT NULL;
                        """
        )) {

            stmt.setBytes(1, convertToBinary(uuid));

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    itemIds.add(rs.getInt("item_id"));
                }
            }
        }
        return itemIds;
    }

    public Map<Integer, Integer> getItemIds(List<Integer> bountyIds) throws SQLException {

        if (bountyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = bountyIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql =
                """
                        SELECT bounty_id, item_id
                        FROM bounty
                        WHERE bounty_id IN (%s)
                        """.formatted(placeholders);

        Map<Integer, Integer> result = new HashMap<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            for (int i = 0; i < bountyIds.size(); i++) {
                stmt.setInt(i + 1, bountyIds.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    int bountyId = rs.getInt("bounty_id");

                    Integer itemId = rs.getObject("item_id", Integer.class);

                    result.put(bountyId, itemId);
                }
            }
        }

        return result;
    }

    private void deleteItemIds(UUID uuid) throws SQLException {
            try (PreparedStatement stmt = connection.prepareStatement(
                    """
                            DELETE FROM item
                            WHERE item_id IN (
                                SELECT item_id
                                FROM bounty
                                WHERE receiver = ?
                            );
                            """
            )) {
                stmt.setBytes(1, convertToBinary(uuid));
                stmt.executeUpdate();
            }
    }

    private void deleteItemIds(List<Integer> itemIds) throws SQLException {
        if (itemIds.isEmpty())
            return;
        String placeholders = itemIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql =
                """
                DELETE FROM item
                WHERE item_id IN (%s);
                """.formatted(placeholders);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a bounty from the database.
     *
     * @param uuid UUID of the bounty to be deleted.
     * @throws SQLException If an error occurred while deleting.
     */
    private void deleteBounty(UUID uuid) throws SQLException {
        deleteItemIds(uuid);
        // Delete bounties
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        DELETE FROM bounty
                        WHERE receiver = ?
                        """
        )) {

            stmt.setBytes(1, convertToBinary(uuid));
            stmt.executeUpdate();
        }
    }

    private void deleteBounty(Bounty bounty) throws SQLException {
        deleteBounties(Collections.singletonList(bounty));
    }

    @Override
    public void removeBounty(UUID uuid) throws DatabaseConnectionException {
        try {
            executeTransaction(() -> deleteBounty(uuid));
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public void removeBounty(Bounty bounty) throws DatabaseConnectionException {
            try {
                executeTransaction(() -> deleteBounty(bounty));
            } catch (SQLException e) {
                throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
            }
    }

    @Override
    public List<Bounty> getBounties(BountySortType sortType, UUID lastUuid, Object lastVal, int limit) throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(buildBountyPageQuery(sortType))) {
            stmt.setObject(1, lastVal);
            stmt.setObject(2, lastVal);
            stmt.setBytes(3, convertToBinary(lastUuid));
            stmt.setInt(4, limit);

            List<UUID> uuids = new LinkedList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = bytesToUUID(rs.getBytes("receiver"));
                    uuids.add(uuid);
                }
            }
            if (uuids.isEmpty())
                return Collections.emptyList();
            Map<UUID, Bounty> bounties = getBounties(uuids);
            List<Bounty> result = new LinkedList<>();
            for (UUID uuid : uuids) {
                result.add(bounties.get(uuid));
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    private static @NotNull String buildBountyPageQuery(BountySortType sortType) {
        return switch (sortType) {
            case OLDEST,NEWEST -> buildTimePlacedPageQuery(sortType);
            case HIGHEST, LOWEST -> buildDisplayAmountPageQuery(sortType);
            case ALPHABETICAL, REVERSE_ALPHABETICAL -> buildNamePageQuery(sortType);
        };
    }

    private static @NotNull String buildDisplayAmountPageQuery(BountySortType sortType) {
        return """
                SELECT
                    receiver,
                    SUM(display) AS total_display
                FROM bounty
                GROUP BY receiver
                HAVING total_display %s ?
                    OR (total_display = ? AND receiver > ?)
                ORDER BY total_display %s, receiver ASC
                LIMIT ?;
                """.formatted(sortType.comparison(), sortType.order());
    }

    private static @NotNull String buildTimePlacedPageQuery(BountySortType sortType) {
        String order = sortType.ascending() ? "MIN" : "MAX";
        return """
                SELECT
                    receiver,
                    %s(time_placed) AS agg_time_placed
                FROM bounty
                GROUP BY receiver
                HAVING agg_time_placed %s ?
                    OR (agg_time_placed = ? AND receiver > ?)
                ORDER BY agg_time_placed %s, receiver ASC
                LIMIT ?;
                """.formatted(order, sortType.comparison(), sortType.order());
    }

    private static @NotNull String buildNamePageQuery(BountySortType sortType) {
        // names are unique
        return """
                SELECT
                    uuid
                FROM player
                WHERE name %s ?
                    OR (name = ? AND uuid > ?)
                ORDER BY name %s, uuid ASC
                LIMIT ?;
                """.formatted(sortType.comparison(), sortType.order());
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                    SELECT uuid, name
                    FROM player
                    WHERE online = TRUE;
                    """
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                Map<UUID, String> networkPlayers = new HashMap<>();
                while (rs.next()) {
                    networkPlayers.put(bytesToUUID(rs.getBytes("uuid")), rs.getString("name"));
                }
                return networkPlayers;
            }
        } catch (SQLException ex) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, ex);
        }
    }

    @Override
    public void updatePlayerData(PlayerData playerData) throws DatabaseConnectionException {
        if (playerData.getPlayerName() == null) {
            // guard against data for players who have never joined the server
            return;
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                    INSERT INTO player(
                                       uuid,
                                       name,
                                       online,
                                       immunity_types,
                                       broadcast_setting,
                                       last_claim,
                                       b_cooldown,
                                       playtime,
                                       last_seen,
                                       time_zone,
                                       texture_id,
                                       whitelist_mode
                    )
                    VALUES ()
                    ON DUPLICATE KEY UPDATE
                                     name = ?,
                                     online = ?,
                                     immunity_types = ?,
                                     broadcast_setting = ?,
                                     last_claim = ?,
                                     b_cooldown = ?,
                                     playtime = ?,
                                     last_seen = ?,
                                     time_zone = COALESCE(?, time_zone),
                                     texture_id = COALESCE(?, texture_id),
                                     whitelist_mode = ?;
                    """
        )) {
            preparePlacyerDataStatement()

            setRefund(playerData.getUuid(), playerData.getRefund());
        } catch (SQLException ex) {

        }

    }

    private void preparePlayerDataStatement(PreparedStatement ps, PlayerData playerData) throws SQLException {
        // insert
        ps.setBytes(1, convertToBinary(playerData.getUuid()));
        ps.setString(2, playerData.getPlayerName());
        ps.setBoolean(3, playerData.isOnline()); // TODO: finish this

        // update
    }

    private void setRefund(UUID uuid, List<OnlineRefund> refunds) {
        if (!isConnected())
            return;
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM bounty_refunds WHERE uuid = ?;");
             PreparedStatement addRefund = getConnection().prepareStatement("INSERT INTO bounty_refunds(uuid, refundtype, refund) VALUES(?, ?, ?);")) {
            ps.setBytes(1, convertToBinary(uuid));
            ps.executeUpdate();

            addRefund.setBytes(1, convertToBinary(uuid));
            addRefundBatch(refunds, addRefund);
            addRefund.executeBatch();
        } catch (SQLException e) {
            if (reconnect(e)) {
                setRefund(uuid, refunds);
            }
        }
    }

    private void addRefundBatch(List<OnlineRefund> refunds, PreparedStatement addRefund) throws SQLException {
        for (OnlineRefund onlineRefund : refunds) {
            try {
                addRefund.setString(2, onlineRefund.getClass().getName());
                addRefund.setBlob(3, refundToInputStream(onlineRefund));
                addRefund.addBatch();
            } catch (IOException e) {
                Bukkit.getLogger().warning("[NotBounties] Error encoding refund to SQL");
            }
        }
    }

    public InputStream refundToInputStream(OnlineRefund onlineRefund) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(byteOut, StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter(writer)) {
            PlayerData.writeRefund(jsonWriter, onlineRefund);
        }
        return new ByteArrayInputStream(byteOut.toByteArray());
    }

    public OnlineRefund refundFromInputStream(InputStream inputStream, Class<? extends OnlineRefund> clazz) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             JsonReader jsonReader = new JsonReader(reader)) {
            return PlayerData.readRefund(jsonReader, clazz);
        }
    }

    private byte immunityToByte(PlayerData playerData) {
        byte immunity = 0;
        if (playerData.hasGeneralImmunity())
            immunity |= 1;
        if (playerData.hasMurderImmunity())
            immunity |= 2;
        if (playerData.hasRandomImmunity())
            immunity |= 4;
        if (playerData.hasTimedImmunity())
            immunity |= 8;
        return immunity;
    }

    private void immunityFromByte(PlayerData playerData, byte b) {
        playerData.setGeneralImmunity((b & 1) == 1);
        playerData.setMurderImmunity((b & 2) == 2);
        playerData.setRandomImmunity((b & 4) == 4);
        playerData.setTimedImmunity((b & 8) == 8);
    }

    @Override
    public PlayerData getPlayerData(@NotNull UUID uuid) throws IOException {
        if (!isConnected())
            throw notConnectedException;

        try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, name, immunity, lastclaim, broadcastsetting, whitelist, bountycooldown, newplayer, lastseen, timezone FROM bounty_player_data WHERE uuid = ?;")) {
            ps.setBytes(1, convertToBinary(uuid));
            PlayerData playerData = new PlayerData();
            playerData.setUuid(uuid);
            playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    playerData.setUuid(UUID.fromString(rs.getString("uuid")));
                    readPlayerDataResult(rs, playerData);
                }
            }
            playerData.setRefund(getRefund(uuid));
            return playerData;
        } catch (SQLException e) {
            if (reconnect(e)) {
                return getPlayerData(uuid);
            }
        }
        return null;
    }

    private Map<UUID, List<OnlineRefund>> getRefunds() throws IOException {
        if (!isConnected())
            throw notConnectedException;
        Map<UUID, List<OnlineRefund>> refunds = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, refundtype, refund FROM bounty_refunds;");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                OnlineRefund refund = getRefundFromResult(rs);
                if (refund != null) {
                    refunds.computeIfAbsent(uuid, k -> new ArrayList<>()).add(refund);
                }
            }
        } catch (SQLException e) {
            if (reconnect(e)) {
                return getRefunds();
            }
        }
        return refunds;
    }

    private List<OnlineRefund> getRefund(UUID uuid) throws IOException {
        if (!isConnected())
            throw notConnectedException;
        List<OnlineRefund> refunds = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT uuid, refundtype, refund FROM bounty_refunds WHERE uuid = ?;")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OnlineRefund refund = getRefundFromResult(rs);
                    if (refund != null)
                        refunds.add(refund);
                }
            }
        } catch (SQLException e) {
            if (reconnect(e)) {
                return getRefund(uuid);
            }
        }
        return refunds;
    }

    private OnlineRefund getRefundFromResult(ResultSet rs) throws SQLException {
        String className = rs.getString("refundtype");
        try (InputStream inputStream = rs.getBlob("refund").getBinaryStream()) {
            // Use reflection to get the class object
            Class<?> clazz = Class.forName(className);

            // Check if it's a subclass of OnlineRefund
            if (OnlineRefund.class.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends OnlineRefund> refundClass = (Class<? extends OnlineRefund>) clazz;

                return refundFromInputStream(inputStream, refundClass);
            } else {
                Bukkit.getLogger().warning("[NotBounties] Invalid refund class: " + className);
            }
        } catch (IOException | ReflectiveOperationException e) {
            Bukkit.getLogger().warning("[NotBounties] Error decoding refund: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataList) {
        if (!isConnected() || playerDataList.isEmpty())
            return;

        try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO bounty_player_data(" +
                "uuid, name, immunity, lastclaim, broadcastsetting, whitelist, bountycooldown, newplayer, lastseen, zone" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "name = ?, immunity = ?, lastclaim = ?, broadcastsetting = ?, whitelist = ?, bountycooldown = ?, newplayer = ?, lastseen = ?, timezone = ?;")) {
            for (PlayerData playerData : playerDataList) {
                preparePlayerDataStatement(ps, playerData);
                ps.addBatch();
            }

            ps.executeBatch();

            // Set refunds separately
            for (PlayerData playerData : playerDataList) {
                setRefund(playerData.getUuid(), playerData.getRefund());
            }

        } catch (SQLException e) {
            if (reconnect(e)) {
                addPlayerData(playerDataList);
            }
        }
    }

    @Override
    public List<PlayerData> getPlayerData() throws IOException {
        if (!isConnected())
            throw notConnectedException;

        Map<UUID, List<OnlineRefund>> refunds = getRefunds();
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

        } catch (SQLException e) {
            if (reconnect(e)) {
                return getPlayerData();
            }
        }

        return playerDataList;
    }

    private void readPlayerDataResult(ResultSet rs, PlayerData playerData) throws SQLException {
        playerData.setPlayerName(rs.getString("name"));
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

    @Override
    public boolean isPermDatabase() {
        return true;
    }


}
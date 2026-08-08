package me.jadenp.notbounties.features.settings.databases.sql;

import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.data.Whitelist;
import me.jadenp.notbounties.data.player_data.AmountRefund;
import me.jadenp.notbounties.data.player_data.ItemRefund;
import me.jadenp.notbounties.data.player_data.OnlineRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.features.settings.databases.*;
import me.jadenp.notbounties.features.settings.databases.wrappers.NotBountiesDatabase;
import me.jadenp.notbounties.ui.PlayerSkin;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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

    private String url;
    private final Map<String, String> options = new HashMap<>();

    public SQLDatabase(Plugin plugin, String name) {
        super(plugin, name);
    }

    @Override
    protected synchronized ConfigurationSection readConfig() {
        ConfigurationSection configuration = super.readConfig();
        if (configuration == null)
            return null;
        for (String key : configuration.getKeys(false)) {
            if (key.equalsIgnoreCase("url"))
                continue;
            options.put(key, configuration.getString(key));
        }
        url = configuration.getString("url", "jdbc:mysql://{host}:{port}/{database}?useSSL={ssl}&allowMultiQueries=true");
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
    public boolean connect(boolean syncData) {
        try {
            String parsedConnection = url;
            for (Map.Entry<String, String> entry : options.entrySet()) {
                parsedConnection = parsedConnection.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            NotBounties.debugMessage("Attempting to connect to " + parsedConnection, false);
            connection = DriverManager.getConnection(parsedConnection, options.getOrDefault("user", ""), options.getOrDefault("password", ""));
            if (!hasConnected) {
                // first connection
                connection.setAutoCommit(false);
                initDatabase();
            }
            connection.setAutoCommit(true);
            hasConnected = true;
            return true;
        } catch (SQLException e) {
            // could not connect to database
            NotBounties.debugMessage("Could not connect to database. " + e.getMessage(), false);
            return false;
        }
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
    public @Nullable PlayerStat getStats(UUID uuid) throws DatabaseConnectionException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT b_claimed, b_set, b_received, b_all_time, immunity, b_claim_amt FROM stat WHERE uuid = ?;"
        )) {
            ps.setBytes(1, convertToBinary(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public Map<UUID, PlayerStat> getStats(Leaderboard sortStat, StatSortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        Map<UUID, PlayerStat> stats = new HashMap<>();
        String sql = buildStatPageQuery(sortStat, sortType, excludedPlayers);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int i = 1;
            for (UUID uuid : excludedPlayers) {
                stmt.setBytes(i++, convertToBinary(uuid));
            }
            stmt.setLong(i++, offset);
            stmt.setLong(i, limit);

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
            StatSortType sort,
            Set<UUID> excludedPlayers
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

        if (!excludedPlayers.isEmpty()) {
            sql.append(' ').append("""
                        WHERE stat.uuid NOT IN (%s)
                    """.formatted(excludedPlayers.stream().map(u -> "?").collect(Collectors.joining(", "))));
        }

        if (sort.requiresPlayerJoin()) {
            sql.append(' ').append("""
                        JOIN player
                            ON stat.uuid = player.uuid
                    """);
        }

        String sortColumn = sort.sqlColumn() != null ? sort.sqlColumn() : stat.getColumnName();

        sql.append(' ').append("""
                    ORDER BY %s %s
                    OFFSET ?
                    LIMIT ?;
                """.formatted(
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

    }

    @Override
    public void deleteStats(UUID uuid) throws DatabaseConnectionException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                DELETE FROM stat
                WHERE uuid = ?;
                """
        )) {
            ps.setBytes(1, convertToBinary(uuid));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public void setStats(UUID uuid, PlayerStat stat) throws DatabaseConnectionException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                """
                     INSERT INTO stat(uuid, b_claimed, b_set, b_received, b_all_time, immunity, b_claim_amt)
                     VALUES(?, ?, ?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                         b_claimed = ?,
                         b_sets = ?,
                         b_received = ?,
                         b_all_time = ?,
                         immunity = ?,
                         b_claim_amt = ?;
                     """
        )) {
            prepareStatInsert(ps, stat);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
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
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM bounty WHERE bounty_id = ?;")) {
            for (Bounty bounty : bounties) {
                for (Setter setter : bounty.getSetters()) {
                    Optional<Integer> id = setter.getBountyId();
                    if (id.isPresent()) {
                        ps.setInt(1, id.get());
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
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
    private void addSetterItemsBatch(List<Setter> setters) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO bounty_item (item_list, bounty_id)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE item_list = ?;
                        """
        )) {
            for (Setter setter : setters) {
                Optional<Integer> id = setter.getBountyId();
                if (setter.hasItems() && id.isPresent() && setter.isItemsLoaded()) {
                    ItemStack[] items = setter.getItems().join().toArray(new ItemStack[0]);
                    stmt.setBlob(1, SerializeInventory.itemStackArrayToBinaryStream(items));
                    stmt.setInt(2, id.get());
                    stmt.setBlob(3, SerializeInventory.itemStackArrayToBinaryStream(items));
                    stmt.addBatch();
                }
            }
        }
    }

    /**
     * Adds the items in the refunds to the database and sets the items id for the refunds.
     * @param refunds Refunds to be added.
     * @throws SQLException If an error occurred while accessing the database.
     */
    private void addRefundItemsBatch(List<OnlineRefund<?>> refunds) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO refund_item (item_list, refund_id)
                        VALUES (?, ?)
                        """
        )) {
            for (OnlineRefund<?> refund : refunds) {
                if (refund instanceof ItemRefund itemRefund) {
                    Optional<List<ItemStack>> encodedRefund = itemRefund.getRefund();
                    Optional<Integer> id = refund.getId();
                    if (id.isPresent() && encodedRefund.isPresent()) {
                        stmt.setBlob(1, SerializeInventory.itemStackArrayToBinaryStream(encodedRefund.get().toArray(new ItemStack[0])));
                        stmt.setInt(2, id.get());
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }
    }

    /**
     * Adds the whitelist of each player to the database.
     * @param playerDataList Players to add.
     * @throws SQLException If an error occurs while accessing the database.
     */
    private void setPlayerWhitelistsBatch(List<PlayerData> playerDataList) throws SQLException {

        try (PreparedStatement deleteWhitelist = connection.prepareStatement(
                """
                    DELETE FROM whitelist
                    WHERE owner = ?;
                    """
        ); PreparedStatement stmt = connection.prepareStatement(
                """
                    INSERT INTO whitelist (owner, uuid)
                    VALUES (?, ?);
                    """
        )) {
            for (PlayerData playerData : playerDataList) {
                deleteWhitelist.setBytes(1, convertToBinary(playerData.getUuid()));
                deleteWhitelist.addBatch();

                stmt.setBytes(1, convertToBinary(playerData.getUuid()));
                for (UUID uuid : playerData.getWhitelist().getList()) {
                    stmt.setBytes(2, convertToBinary(uuid));
                    stmt.addBatch();
                }
            }
            deleteWhitelist.executeBatch();
            stmt.executeBatch();
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
                Optional<Integer> id = setter.getBountyId();
                if (id.isPresent()) {
                    stmt.setInt(1, id.get());
                    for (UUID uuid : setter.getWhitelist().getList()) {
                        stmt.setBytes(2, convertToBinary(uuid));
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }
    }

    /**
     * Adds the bounty objects to the database and sets the bounty id for each setter.
     * This does not add any items, whitelists, or modify any other tables.
     * @param bounties Bounties to add.
     * @throws SQLException If an error occurs when accessing the database.
     */
    private void addBountiesBatch(List<Bounty> bounties) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO bounty (
                            setter,
                            receiver,
                            amount,
                            display,
                            notified,
                            time_placed,
                            playtime
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                Statement.RETURN_GENERATED_KEYS
        )) {
            for (Bounty bounty : bounties) {
                for (Setter setter : bounty.getSetters()) {
                    stmt.setBytes(1, convertToBinary(setter.getUuid()));
                    stmt.setBytes(2, convertToBinary(bounty.getUUID()));
                    stmt.setDouble(3, setter.getAmount());
                    stmt.setDouble(4, setter.getDisplayAmount());
                    stmt.setBoolean(5, setter.isNotified());
                    stmt.setObject(6, LocalDateTime.ofInstant(Instant.ofEpochMilli(setter.getTimeCreated()), ZoneId.systemDefault()));
                    stmt.setLong(7, setter.getReceiverPlaytime());

                    stmt.addBatch();
                }
            }

            stmt.executeBatch();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                for (Bounty bounty : bounties) {
                    ListIterator<Setter> setterListIterator = bounty.getSetters().listIterator();
                    while (setterListIterator.hasNext() && keys.next()) {
                        setterListIterator.next().setId(keys.getInt(1));
                    }
                }
            }
        }
    }

    private void insertBountiesBatch(List<Bounty> bounties) throws SQLException {
        List<Setter> allSetters = new ArrayList<>();
        bounties.forEach(bounty -> allSetters.addAll(bounty.getSetters()));
        addBountiesBatch(bounties); // Must be first. Sets the ids for each setter.
        addSetterItemsBatch(allSetters);
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
                Optional<Integer> id = setter.getBountyId();
                if (id.isPresent()) {
                    stmt.setInt(1, id.get());
                    for (String tag : setter.getTags()) {
                        stmt.setString(2, tag);
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }
    }

    private void insertBounty(@NotNull Bounty bounty) throws SQLException {
        insertBountiesBatch(Collections.singletonList(bounty));
    }

    @Override
    public void addBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        try {
            executeTransaction(() -> insertBounty(bounty));
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public void setBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        try {
            executeTransaction(() -> {
                deleteBounty(bounty.getUUID());
                insertBounty(bounty);
            });
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
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
                               b.bounty_id,
                               b.setter,
                               b.amount,
                               b.display,
                               b.notified,
                               b.time_placed,
                               b.playtime,
                               b.whitelist_mode,
                               i.bounty_id IS NOT NULL AS has_item
                        FROM bounty b
                        WHERE b.receiver = ?
                        LEFT JOIN bounty_item i
                            ON i.bounty_id = b.bounty_id;
                        """
        )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {

                List<Setter> setters = new LinkedList<>();
                List<Integer> bountyIds = new LinkedList<>();
                while (rs.next()) {
                    Setter setter = readSetter(rs);
                    setters.add(setter);
                    Optional<Integer> id = setter.getBountyId();
                    id.ifPresent(bountyIds::add);
                }
                if (setters.isEmpty())
                    return null;
                Map<Integer, SortedSet<UUID>> whitelists = getWhitelists(bountyIds);
                for (Setter setter : setters) {
                    setter.getWhitelist().setList(whitelists.getOrDefault(setter.getBountyId().orElse(-1), Collections.emptySortedSet()));
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
                       b.receiver,
                       b.bounty_id,
                       b.setter,
                       b.amount,
                       b.display,
                       b.notified,
                       b.time_placed,
                       b.playtime,
                       b.whitelist_mode,
                       i.bounty_id IS NOT NULL AS has_item
                FROM bounty b
                WHERE b.receiver IN (%s)
                LEFT JOIN bounty_item i
                    ON i.bounty_id = b.bounty_id;
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
                    Optional<Integer> id = setter.getBountyId();
                    id.ifPresent(bountyIds::add);
                }
            }
            if (bounties.isEmpty())
                return Collections.emptyMap();
            Map<Integer, SortedSet<UUID>> whitelists = getWhitelists(bountyIds);
            for (Bounty bounty : bounties.values()) {
                for (Setter setter : bounty.getSetters()) {
                    setter.getWhitelist().setList(whitelists.getOrDefault(setter.getBountyId().orElse(-1), Collections.emptySortedSet()));
                }
            }
            return bounties;
        }
    }

    private Setter readSetter(ResultSet rs) throws SQLException {
        int bountyId = rs.getInt("bounty_id");
        UUID setterUUID = bytesToUUID(rs.getBytes("setter"));
        boolean hasItems = rs.getBoolean("has_item");
        double amount = rs.getDouble("amount");
        double display = rs.getDouble("display");
        boolean notified = rs.getBoolean("notified");
        long time = rs.getObject("time_placed", LocalDateTime.class).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long playtime = rs.getLong("playtime");
        boolean whitelistMode = rs.getBoolean("whitelist_mode");
        Whitelist whitelist = new Whitelist(Collections.emptySortedSet(), whitelistMode);
        return new Setter(bountyId, setterUUID, amount, time, hasItems, notified, whitelist, playtime, display, Collections.emptySet());
    }



    /**
     * Deletes a bounty from the database.
     *
     * @param uuid UUID of the bounty to be deleted.
     * @throws SQLException If an error occurred while deleting.
     */
    private void deleteBounty(UUID uuid) throws SQLException {
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
    public List<Bounty> getBounties(BountySortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(buildBountyPageQuery(sortType, excludedPlayers))) {
            int i = 1;
            for (UUID uuid : excludedPlayers) {
                stmt.setBytes(i++, convertToBinary(uuid));
            }
            stmt.setLong(i++, offset);
            stmt.setLong(i, limit);

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

    @Override
    public long getNumBounties() throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                    SELECT COUNT(*) AS total_bounties
                    FROM bounty;
                    """
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_bounties");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
        return 0;
    }

    @Override
    public long getNumUniqueBounties() throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                    SELECT COUNT(*) AS total_bounties
                    FROM bounty
                    GROUP BY receiver;
                    """
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_bounties");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
        return 0;
    }

    private @Nullable List<ItemStack> readItems(ResultSet resultSet) throws SQLException, IOException {
        if (resultSet.next()) {
            return Arrays.asList(SerializeInventory.itemStackArrayFromBinaryStream(resultSet.getBlob("item_list").getBinaryStream()));
        }
        return null;
    }

    @Override
    public List<ItemStack> getBountyItems(int bountyId) throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                SELECT item_list
                FROM bounty_item
                WHERE bounty_id = ?;
                """
        )) {
            stmt.setInt(1, bountyId);
            ResultSet resultSet = stmt.executeQuery();
            return readItems(resultSet);
        } catch (SQLException | IOException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public List<ItemStack> getRefundItems(int refundId) throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                SELECT item_list
                FROM refund_item
                WHERE refund_id = ?;
                """
        )) {
            stmt.setInt(1, refundId);
            ResultSet resultSet = stmt.executeQuery();
            return readItems(resultSet);
        } catch (SQLException | IOException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public void setBountyItems(int bountyId, @NotNull List<ItemStack> items) throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO bounty_item (item_list, bounty_id)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE item_list = ?;
                        """
        )) {
            stmt.setBlob(1, SerializeInventory.itemStackArrayToBinaryStream(items.toArray(new ItemStack[0])));
            stmt.setInt(2, bountyId);
            stmt.setBlob(3, SerializeInventory.itemStackArrayToBinaryStream(items.toArray(new ItemStack[0])));
            stmt.executeUpdate();
        } catch (SQLException ignored) {
            // could be rejected by the db if bountyId doesn't exist
        }

    }

    @Override
    public void setRefundItems(int refundId, @NotNull List<ItemStack> items) throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                        INSERT INTO refund_item (item_list, refund_id)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE item_list = ?;
                        """
        )) {
            stmt.setBlob(1, SerializeInventory.itemStackArrayToBinaryStream(items.toArray(new ItemStack[0])));
            stmt.setInt(2, refundId);
            stmt.setBlob(3, SerializeInventory.itemStackArrayToBinaryStream(items.toArray(new ItemStack[0])));
            stmt.executeUpdate();
        } catch (SQLException ignored) {
            // could be rejected by the db if bountyId doesn't exist
        }
    }

    private static @NotNull String buildBountyPageQuery(BountySortType sortType, Set<UUID> excludedPlayers) {
        return switch (sortType) {
            case OLDEST,NEWEST -> buildTimePlacedPageQuery(sortType, excludedPlayers);
            case HIGHEST, LOWEST -> buildDisplayAmountPageQuery(sortType, excludedPlayers);
            case ALPHABETICAL, REVERSE_ALPHABETICAL -> buildNamePageQuery(sortType, excludedPlayers);
        };
    }

    private static @NotNull String buildDisplayAmountPageQuery(BountySortType sortType, Set<UUID> excludedPlayers) {
        return """
                SELECT
                    receiver,
                    SUM(display) AS total_display
                FROM bounty
                %s
                GROUP BY receiver
                ORDER BY total_display %s, receiver ASC
                OFFSET ?
                LIMIT ?;
                """.formatted(getExcludedBountyPlayersClause(excludedPlayers), sortType.order());
    }

    private static @NotNull String getExcludedBountyPlayersClause(Set<UUID> excludedPlayers) {
        return excludedPlayers.isEmpty() ? "" : "WHERE receiver NOT IN (" + excludedPlayers.stream().map(u -> "?").collect(Collectors.joining(",")) + ")";
    }

    private static @NotNull String buildTimePlacedPageQuery(BountySortType sortType, Set<UUID> excludedPlayers) {
        final String order = sortType.ascending() ? "MIN" : "MAX";
        return """
                SELECT
                    receiver,
                    %s(time_placed) AS agg_time_placed
                FROM bounty
                %s
                GROUP BY receiver
                ORDER BY agg_time_placed %s, receiver ASC
                OFFSET ?
                LIMIT ?;
                """.formatted(order, getExcludedBountyPlayersClause(excludedPlayers), sortType.order());
    }

    private static @NotNull String buildNamePageQuery(BountySortType sortType, Set<UUID> excludedPlayers) {
        // names are unique
        final String excludedPlayersClause = excludedPlayers.isEmpty() ? "" : "WHERE receiver NOT IN (" + excludedPlayers.stream().map(u -> "?").collect(Collectors.joining(",")) + ")";
        return """
                SELECT
                    uuid
                FROM player
                %s
                ORDER BY name %s, uuid ASC
                OFFSET ?
                LIMIT ?;
                """.formatted(excludedPlayersClause, sortType.order());
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                    SELECT uuid, name
                    FROM player
                    WHERE server_id <> ?;
                    """
        )) {
            stmt.setBytes(1, convertToBinary(DataManager.GLOBAL_SERVER_ID));
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
        try {
            executeTransaction(() -> {
                try (PreparedStatement stmt = getPlayerDataInsertStatement()) {
                    preparePlayerDataInsertStatement(playerData, stmt);
                    stmt.executeUpdate();
                }
                // Not setting refund
                setPlayerWhitelistsBatch(Collections.singletonList(playerData));
            });
        } catch (SQLException ex) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, ex);
        }


    }

    private PreparedStatement getPlayerDataInsertStatement() throws SQLException {
        return connection.prepareStatement(
                """
                    INSERT INTO player(
                                       uuid,
                                       name,
                                       server_id,
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
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                                     name = ?,
                                     server_id = ?,
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
        );
    }

    private void preparePlayerDataInsertStatement(PlayerData playerData, PreparedStatement ps) throws SQLException {

        // insert
        ps.setBytes(1, convertToBinary(playerData.getUuid()));
        ps.setString(2, playerData.getPlayerName());
        ps.setBytes(3, convertToBinary(playerData.getOnlineServerID()));
        ps.setByte(4, immunityToByte(playerData));
        ps.setByte(5, (byte) playerData.getBroadcastSettings().ordinal());
        ps.setObject(6, LocalDateTime.ofInstant(Instant.ofEpochMilli(playerData.getLastClaim()), ZoneId.systemDefault()));
        ps.setObject(7, LocalDateTime.ofInstant(Instant.ofEpochMilli(playerData.getBountyCooldown()), ZoneId.systemDefault()));
        ps.setLong(8, playerData.getPlayTime());
        ps.setObject(9, LocalDateTime.ofInstant(Instant.ofEpochMilli(playerData.getLastSeen()), ZoneId.systemDefault()));
        if (playerData.getTimeZone() != null) {
            ps.setString(10, playerData.getTimeZone().getID());
        } else {
            ps.setNull(10, Types.VARCHAR);
        }
        if (playerData.getSkin().missing()) {
            ps.setNull(11, Types.CHAR);
        } else {
            ps.setString(11, playerData.getSkin().id());
        }
        ps.setBoolean(12, playerData.getWhitelist().isBlacklist());

        // update
        ps.setBytes(13, convertToBinary(playerData.getUuid()));
        ps.setString(14, playerData.getPlayerName());
        ps.setBytes(15, convertToBinary(playerData.getOnlineServerID()));
        ps.setByte(16, immunityToByte(playerData));
        ps.setByte(17, (byte) playerData.getBroadcastSettings().ordinal());
        ps.setObject(18, LocalDateTime.ofInstant(Instant.ofEpochMilli(playerData.getLastClaim()), ZoneId.systemDefault()));
        ps.setObject(19, LocalDateTime.ofInstant(Instant.ofEpochMilli(playerData.getBountyCooldown()), ZoneId.systemDefault()));
        ps.setLong(20, playerData.getPlayTime());
        ps.setObject(21, LocalDateTime.ofInstant(Instant.ofEpochMilli(playerData.getLastSeen()), ZoneId.systemDefault()));
        if (playerData.getTimeZone() != null) {
            ps.setString(22, playerData.getTimeZone().getID());
        } else {
            ps.setNull(22, Types.VARCHAR);
        }
        if (playerData.getSkin().missing()) {
            ps.setNull(23, Types.CHAR);
        } else {
            ps.setString(23, playerData.getSkin().id());
        }
        ps.setBoolean(24, playerData.getWhitelist().isBlacklist());

    }

    private PreparedStatement getRefundInsertStatement() throws SQLException {
        return connection.prepareStatement("""
                INSERT INTO refund(
                                   uuid,
                                   refund_time,
                                   refund_amount,
                                   reason
                            )
                VALUES(?, ?, ?, ?);
            """,
                Statement.RETURN_GENERATED_KEYS
        );
    }

    /**
     * Prepares the refund statement from {@link #getRefundInsertStatement()} to be added to the database.
     * Does not set the first parameter (uuid of the player that the refund is for).
     * @param refund Refund to be added to the database
     * @param ps The refund insert statement.
     * @throws SQLException If an error occurs while accessing the database.
     */
    private void prepareRefundInsertStatement(OnlineRefund<?> refund, PreparedStatement ps) throws SQLException {
        ps.setObject(2, LocalDateTime.ofInstant(Instant.ofEpochMilli(refund.getTimeCreated()), ZoneId.systemDefault()));
        switch (refund) {
            case ItemRefund itemRefund -> ps.setDouble(3, 0);
            case AmountRefund amountRefund -> ps.setDouble(3, amountRefund.getRefund().orElse((double) 0));
            default -> {
                logger.warning("Unknown refund type " + refund.getClass().getName());
                ps.setDouble(3, 0);
            }
        }
        ps.setString(4, refund.getReason());
    }

    private void deleteRefund(UUID uuid) throws SQLException {
        try (PreparedStatement deleteRefunds = connection.prepareStatement("DELETE FROM refund WHERE uuid = ?;")) {
            deleteRefunds.setBytes(1, convertToBinary(uuid));
            deleteRefunds.executeUpdate();
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
    public PlayerData getPlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {

        try {
            PlayerData playerData = new PlayerData();
            playerData.setUuid(uuid);
            executeTransaction(() -> {
                readPlayerData(playerData);
                playerData.getWhitelist().setList(getWhitelist(uuid));
            });
            return playerData;
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    /**
     * Read the player data from the database into a {@link PlayerData} object.
     * @param playerData Object to read the player data into.
     * @return True if player data exists for this player.
     * @throws SQLException If an error occurs while accessing the database.
     */
    private boolean readPlayerData(PlayerData playerData) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement("""
                SELECT name,
                       server_id,
                       immunity_types,
                       broadcast_setting,
                       last_claim,
                       b_cooldown,
                       playtime,
                       last_seen,
                       time_zone,
                       texture_id,
                       whitelist_mode
                FROM player
                WHERE uuid = ?;
                """)) {
            ps.setBytes(1, convertToBinary(playerData.getUuid()));
            playerData.setServerID(DataManager.GLOBAL_SERVER_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    parsePlayerDataResult(playerData, rs);
                    return true;
                }
            }
        }
        return false;
    }

    private void parsePlayerDataResult(PlayerData playerData, ResultSet rs) throws SQLException {
        playerData.setPlayerName(rs.getString("name"));
        playerData.setOnlineServerID(bytesToUUID(rs.getBytes("server_id")));
        immunityFromByte(playerData, rs.getByte("immunity_types"));
        playerData.setBroadcastSettings(PlayerData.BroadcastSettings.values()[rs.getInt("broadcast_setting")]);
        playerData.setLastClaim(rs.getObject("last_claim", LocalDateTime.class).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        playerData.setBountyCooldown(rs.getObject("b_cooldown", LocalDateTime.class).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        playerData.setPlayTime(rs.getLong("playtime"));
        playerData.setLastSeen(rs.getObject("last_seen", LocalDateTime.class).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        String timezone = rs.getString("time_zone");
        if (timezone != null) {
            playerData.setTimeZone(TimeZone.getTimeZone(timezone));
        }
        String textureId = rs.getString("texture_id");
        if (textureId != null) {
            playerData.setSkin(new PlayerSkin(textureId, false));
        }
        playerData.getWhitelist().setBlacklist(rs.getBoolean("whitelist_mode"));
    }

    /**
     * Read the players' refunds from the database.
     * @param uuid UUID of the player.
     * @return The refunds.
     * @throws SQLException If there was an error accessing the database.
     */
    private List<OnlineRefund<?>> readRefunds(UUID uuid) throws SQLException {
        String sql =
                """
                SELECT refund_time,
                       refund_id,
                       refund_amount,
                       reason
                FROM refund
                WHERE uuid = ?;
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, convertToBinary(uuid));
            List<OnlineRefund<?>> refunds = new LinkedList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("refund_id");
                long timeCreated = rs.getObject("refund_time", LocalDateTime.class).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                String reason = rs.getString("reason");
                double amount = rs.getDouble("refund_amount");
                OnlineRefund<?> refund;
                if (amount == 0) {
                    refund = new ItemRefund(id, reason, timeCreated);
                } else {
                    refund = new AmountRefund(id, amount, timeCreated, reason);
                }
                refund.setId(id);
                refunds.add(refund);
            }

            return refunds;
        }
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataList) throws DatabaseConnectionException{
        if (playerDataList.isEmpty())
            return;

        try {
            executeTransaction(() -> {
                try (PreparedStatement ps = getPlayerDataInsertStatement()) {
                    for (PlayerData playerData : playerDataList) {

                        preparePlayerDataInsertStatement(playerData, ps);
                        ps.addBatch();
                    }

                    ps.executeBatch();

                    setPlayerWhitelistsBatch(playerDataList);
                }
            });
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }

    }

    @Override
    public void addRefunds(UUID uuid, List<OnlineRefund<?>> refunds) throws DatabaseConnectionException {
        try (PreparedStatement addRefund = getRefundInsertStatement()) {
            addRefund.setBytes(1, convertToBinary(uuid));
            for (OnlineRefund<?> onlineRefund : refunds) {
                prepareRefundInsertStatement(onlineRefund, addRefund);
                addRefund.addBatch();
            }
            addRefund.executeBatch();

            ResultSet rs = addRefund.getGeneratedKeys();
            ListIterator<OnlineRefund<?>> iterator = refunds.listIterator();
            while (iterator.hasNext() && rs.next()) {
                OnlineRefund<?> onlineRefund = iterator.next();
                onlineRefund.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public List<PlayerData> getPlayerData(PlayerSortType sortType, long offset, long limit, Set<UUID> excludedPlayers) throws DatabaseConnectionException {
        List<PlayerData> playerDataList = new ArrayList<>();
        String sql = buildPlayerPageQuery(sortType, excludedPlayers);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int i = 1;
            for (UUID excludedPlayer : excludedPlayers) {
                stmt.setBytes(i++, convertToBinary(excludedPlayer));
            }
            stmt.setLong(i++, offset);
            stmt.setLong(i, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PlayerData playerData = new PlayerData();
                    playerData.setUuid(bytesToUUID(rs.getBytes("uuid")));
                    parsePlayerDataResult(playerData, rs);
                    playerDataList.add(playerData);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
        return playerDataList;
    }

    @Override
    public void deletePlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                DELETE FROM player
                WHERE uuid = ?;
                """
        )) {
            stmt.setBytes(1, convertToBinary(uuid));
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, ex);
        }
    }

    @Override
    public long getNumPlayers() throws DatabaseConnectionException {
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                    SELECT COUNT(*) AS total_players
                    FROM player;
                    """
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_players");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
        return 0;
    }

    private static String buildPlayerPageQuery(
            PlayerSortType sort,
            Set<UUID> excludedPlayers
    ) {

        StringBuilder sql = new StringBuilder();

        sql.append("""
                    SELECT
                       uuid,
                       name,
                       server_id,
                       immunity_types,
                       broadcast_setting,
                       last_claim,
                       b_cooldown,
                       playtime,
                       last_seen,
                       time_zone,
                       texture_id,
                       whitelist_mode
                    FROM player
                """);

        if (!excludedPlayers.isEmpty()) {
            sql.append(" WHERE uuid NOT IN (");
            sql.append(excludedPlayers.stream().map(u -> "?").collect(Collectors.joining(",")));
            sql.append(")");
        }

        String sortColumn = sort.sqlColumn() != null ? sort.sqlColumn() : "name";

        sql.append(' ').append("""
                    ORDER BY %s %s
                    OFFSET ?
                    LIMIT ?;
                """.formatted(
                sortColumn,
                sort.order()
        ));

        return sql.toString();
    }

    @Override
    public synchronized void shutdown() {
        // remove all players from being online
        try (PreparedStatement stmt = connection.prepareStatement(
                """
                    UPDATE player
                    SET server_id = ?,
                        playtime = playtime + TIMESTAMPDIFF(MICROSECOND, last_seen, NOW(3)) / 1000,
                        last_seen = NOW(3)
                    WHERE server_id = ?;
                    """
        )){
            stmt.setBytes(1, convertToBinary(DataManager.GLOBAL_SERVER_ID));
            stmt.setBytes(2, convertToBinary(Databases.getDatabaseServerID()));
            stmt.executeUpdate();
        } catch (SQLException ex) {
            logger.warning("Database shutdown partially failed. " + ex.getMessage());
        }
        disconnect();
    }

    @Override
    public void notifyBounty(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
        """
            UPDATE bounty
            SET notified = true
            WHERE uuid = ?;
            """)) {
            stmt.setBytes(1, convertToBinary(uuid));
        } catch (SQLException e) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, e);
        }
    }

    @Override
    public void logout(UUID uuid) throws DatabaseConnectionException {
        try {
            executeTransaction(() -> logoutPlayer(uuid));
        } catch (SQLException ex) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, ex);
        }
    }

    private void logoutPlayer(UUID uuid) throws SQLException {
        try (PreparedStatement logout = connection.prepareStatement(
                """
                    UPDATE player
                    SET server_id = ?,
                        playtime = playtime + TIMESTAMPDIFF(MICROSECOND, last_seen, NOW(3)) / 1000,
                        last_seen = NOW(3)
                    WHERE uuid = ? AND server_id = ?;
                    """
        )) {
            logout.setBytes(1, convertToBinary(DataManager.GLOBAL_SERVER_ID));
            logout.setBytes(2, convertToBinary(uuid));
            logout.setBytes(3, convertToBinary(Databases.getDatabaseServerID()));
            logout.executeUpdate();
        }
    }

    @Override
    public void login(UUID uuid, String playerName) {
        try {
            executeTransaction(() -> loginPlayer(uuid, playerName));
        } catch (SQLException ex) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, ex);
        }
    }

    private void loginPlayer(UUID uuid, String playerName) throws SQLException {
        PlayerData playerData = new PlayerData();
        playerData.setUuid(uuid);
        if (!readPlayerData(playerData)) {
            // player data does not exist in DB
            playerData.setPlayerName(playerName);
            playerData.setLastSeen(System.currentTimeMillis());
            playerData.setOnlineServerID(Databases.getDatabaseServerID());
            addPlayerData(Collections.singletonList(playerData));
        } else {
            boolean playtimeNeedsUpdating = playerData.getOnlineServerID().equals(DataManager.GLOBAL_SERVER_ID);
            String sql =
                    """
                    UPDATE player
                    SET server_id = ?,
                        name = ?,
                        %s
                        last_seen = NOW(3)
                    WHERE uuid = ?;
                    """.formatted(
                            playtimeNeedsUpdating
                            ? "" : "playtime = playtime + TIMESTAMPDIFF(MICROSECOND, last_seen, NOW(3)) / 1000,"
                    );
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setBytes(1, convertToBinary(Databases.getDatabaseServerID()));
                ps.setString(2, playerName);
                ps.setBytes(3, convertToBinary(uuid));
                ps.executeUpdate();
            }
        }

    }

    @Override
    public List<OnlineRefund<?>> getAndRemoveRefunds(UUID uuid) throws DatabaseConnectionException {
        List<OnlineRefund<?>> onlineRefunds = new LinkedList<>();
        try {
            executeTransaction(() -> {
                onlineRefunds.addAll(readRefunds(uuid));
                deleteRefund(uuid);
            });
        } catch (SQLException ex) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, ex);
        }
        return onlineRefunds;
    }

    @Override
    public @Nullable NotBountiesDatabase getWrappedDatabase() {
        return null;
    }

    @Override
    public void setAllBroadcastSetting(PlayerData.BroadcastSettings broadcastSetting) throws DatabaseConnectionException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                    UPDATE player
                    SET broadcast_setting = ?;
                    """
        )) {
            ps.setByte(1, (byte) broadcastSetting.ordinal());
        } catch (SQLException ex) {
            throw new DatabaseConnectionException(DISCONNECTED_MESSAGE, ex);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SQLDatabase that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(connection, that.connection) && Objects.equals(url, that.url) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), connection, url, options);
    }
}
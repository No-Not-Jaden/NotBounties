package me.jadenp.notbounties.sql;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.utils.*;
import me.jadenp.notbounties.utils.configuration.BountyExpire;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.*;
import java.util.*;

import static me.jadenp.notbounties.NotBounties.isVanished;
import static me.jadenp.notbounties.utils.DataManager.tryToConnect;

public class SQLGetter {

    private long nextReconnectAttempt;
    private int reconnectAttempts;
    private final MySQL sql;
    public SQLGetter (MySQL sql){
        nextReconnectAttempt = System.currentTimeMillis();
        reconnectAttempts = 0;
        this.sql = sql;
    }

    private static String getEditBountyProcedure() throws IOException {
        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(NotBounties.getInstance().getResource("editBounty.sql"))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }
        }
        return sql.toString();
    }

    public void createTable(){
        try (PreparedStatement ps = sql.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS notbounties" +
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
             PreparedStatement createEditBountyProcedure = sql.getConnection().prepareStatement(getEditBountyProcedure());
             PreparedStatement checkAmount = sql.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'notbounties' and column_name = 'amount';");
             PreparedStatement alterAmount = sql.getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN amount FLOAT(53);");
             PreparedStatement checkWhitelist = sql.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'whitelist';");
             PreparedStatement addWhitelist = sql.getConnection().prepareStatement("ALTER TABLE notbounties ADD whitelist VARCHAR(369);");
             PreparedStatement checkPlaytime = sql.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'playtime';");
             PreparedStatement addPlaytime = sql.getConnection().prepareStatement("ALTER TABLE notbounties ADD playtime BIGINT DEFAULT 0 NOT NULL;");
             PreparedStatement checkItems = sql.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'items';");
             PreparedStatement addItems = sql.getConnection().prepareStatement("ALTER TABLE notbounties ADD items BLOB;");
             PreparedStatement checkItemsType = sql.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'notbounties' and column_name = 'items';");
             PreparedStatement alterItems = sql.getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN items BLOB;");
             PreparedStatement checkDisplay = sql.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'display';");
             PreparedStatement addDisplay = sql.getConnection().prepareStatement("ALTER TABLE notbounties ADD display FLOAT(53) DEFAULT -1 NOT NULL;")){
            
            ps.executeUpdate();
            createEditBountyProcedure.execute();
            
            checkAmount.setString(1, sql.getDatabase());
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

            checkItemsType.setString(1, sql.getDatabase());
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

        } catch (SQLException | IOException e){
            Bukkit.getLogger().warning(e.toString());
        }
    }
    public void createDataTable(){
        try (PreparedStatement ps = sql.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS bounty_data" +
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
            PreparedStatement ps1 = sql.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = ? and table_name = 'bounty_data' and column_name = 'claimed';");
            PreparedStatement ps2 = sql.getConnection().prepareStatement("ALTER TABLE bounty_data MODIFY COLUMN claimed BIGINT, MODIFY sets BIGINT, MODIFY received BIGINT, MODIFY alltime FLOAT(53), MODIFY immunity FLOAT(53), MODIFY allclaimed FLOAT(53);")) {
            
            ps.executeUpdate();
            
            ps1.setString(1, sql.getDatabase());
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
        try(PreparedStatement ps = sql.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS bounty_players" +
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
        if (lastPlayerListRequest + 10000 < System.currentTimeMillis()) {
            lastPlayerListRequest = System.currentTimeMillis();
            new BukkitRunnable() {
                @Override
                public void run() {
                    getNetworkPlayers();
                }
            }.runTaskAsynchronously(NotBounties.getInstance());
        }
        if (onlinePlayers.isEmpty())
            onlinePlayers.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> !isVanished(player)).toList());
        return onlinePlayers;
    }
    private List<OfflinePlayer> getNetworkPlayers() {
        List<OfflinePlayer> networkPlayers = new ArrayList<>();
        try (PreparedStatement ps = sql.getConnection().prepareStatement("SELECT uuid FROM bounty_players;")){
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                try {
                    networkPlayers.add(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[NotBounties] Removing invalid UUID on database: " + uuid);
                    try (PreparedStatement ps1 = sql.getConnection().prepareStatement("DELETE FROM bounty_players WHERE uuid = ?;")) {
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
            if (reconnect(e)){
                return getNetworkPlayers();
            }
        }
        onlinePlayers.clear();
        onlinePlayers.addAll(networkPlayers);
        return networkPlayers;
    }
    public void refreshOnlinePlayers() {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM bounty_players WHERE id = ?;")){
            ps.setInt(1, sql.getServerID());
            ps.executeUpdate();

        } catch (SQLException e){
            if (reconnect(e)){
                refreshOnlinePlayers();
            }
        }
        for (Player player : Bukkit.getOnlinePlayers())
            if (!isVanished(player))
                login(player);
    }
    public void logout(Player player) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM bounty_players WHERE uuid = ? AND id = ?;")){
            ps.setString(1, player.getUniqueId().toString());
            ps.setInt(2, sql.getServerID());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect(e)){
                logout(player);
            }
        }
    }
    public void login(Player player) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("INSERT INTO bounty_players(uuid, name, id) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE id = ?;")){
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());
            ps.setInt(3, sql.getServerID());
            ps.setInt(4, sql.getServerID());
            ps.executeUpdate();

        } catch (SQLException e){
            if (reconnect(e)){
                login(player);
            }
        }
    }
    public void addData(String uuid, long claimed, long set, long received, double allTime, double immunity, double allClaimed){
        try (PreparedStatement ps = sql.getConnection().prepareStatement("INSERT INTO bounty_data(uuid, claimed, sets, received, alltime, immunity, allclaimed) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE claimed = claimed + ?, sets = sets + ?, received = received + ?, alltime = alltime + ?, immunity = immunity + ?, allclaimed = allclaimed + ?;")){
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
            reconnect(e);
        }
    }

    public void addData(UUID uuid, Double[] stats) {
        addData(uuid.toString(), stats[0].longValue(), stats[1].longValue(), stats[2].longValue(), stats[3], stats[4], stats[5]);
    }

    public Map<UUID, Double[]> getAllStats() {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("SELECT uuid, claimed, sets, received, alltime, immunity, allclaimed FROM bounty_data;")){
            ResultSet rs = ps.executeQuery();

            Map<UUID, Double[]> stats = new HashMap<>();
            while (rs.next()) {
                rs.findColumn("uuid");
                UUID uuid = UUID.fromString(rs.getString(1));
                double kills = rs.getLong(2);
                double set = rs.getLong(3);
                double deaths = rs.getLong(4);
                double all = rs.getDouble(5);
                double immunity = rs.getDouble(6);
                double claimed = rs.getDouble(7);
                stats.put(uuid, new Double[] {kills, set, deaths, all, immunity, claimed});
            }
            return stats;
        } catch (SQLException e) {
            if (reconnect(e)){
                return getAllStats();
            }
        }
        return new HashMap<>();
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

    public void addBounty(Bounty bounty, Setter setter){
        new BukkitRunnable() {
            @Override
            public void run() {
                try (PreparedStatement ps = sql.getConnection().prepareStatement("INSERT INTO notbounties(uuid, name, setter, suuid, amount, notified, time, whitelist, playtime, items, display) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")){
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
                    if (reconnect(e)){
                        addBounty(bounty, setter);
                    }
                }
            }
        }.runTaskAsynchronously(NotBounties.getInstance());

    }
    public void addBounty(Bounty bounty){
        // most of the time there is only 1 setter being added
        for (Setter setter : bounty.getSetters())
            addBounty(bounty, setter);
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

    public Bounty getBounty(UUID uuid) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("SELECT * FROM `notbounties` WHERE uuid = ?;")){
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            
            List<Setter> setters = new ArrayList<>();
            String name = "";
            while (rs.next()){
                if (name.isEmpty()){
                    name = rs.getString("name");
                }
                Blob encodedItems = rs.getBlob("items");
                List<ItemStack> items = convertEncodedItems(encodedItems);

                UUID setterUUID = rs.getString("suuid").equalsIgnoreCase("CONSOLE") ? new UUID(0,0) : UUID.fromString(rs.getString("suuid"));
                Setter setter = new Setter(rs.getString("setter"), setterUUID, rs.getDouble("amount"), items, rs.getLong("time"), rs.getBoolean("notified"), decodeWhitelist(rs.getString("whitelist")), rs.getLong("playtime"), rs.getDouble("display"));
                setters.add(setter);
            }
            if (setters.isEmpty())
                return null;
            return new Bounty(uuid, setters, name);
        } catch (SQLException e){
            if (reconnect(e)){
                return getBounty(uuid);
            }
        }
        return null;
    }

    public boolean removeBounty(UUID uuid) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e){
            if (reconnect(e)){
                return removeBounty(uuid);
            }
        }
        return false;
    }
    public boolean removeBounty(Bounty bounty) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ? AND suuid = ? AND time = ?;")){
            ps.setString(1, bounty.getUUID().toString());
            for (Setter setter : bounty.getSetters()) {
                ps.setString(2, setter.getUuid().toString());
                ps.setLong(3, setter.getTimeCreated());
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            if (reconnect(e)) {
                return removeBounty(bounty);
            }
        }
        return false;
    }
    public boolean removeSetter(UUID uuid, Setter setter) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ? AND suuid = ? AND time = ?;")){
            ps.setString(1, uuid.toString());
            ps.setString(2, setter.getUuid().toString());
            ps.setLong(3, setter.getTimeCreated());
            ps.executeUpdate();
            return true;
        } catch (SQLException e){
            if (reconnect(e)){
                return removeSetter(uuid, setter);
            }
        }
        return false;
    }
    public void removeOldPlaytimeBounties() {
        new BukkitRunnable() {
            @Override
            public void run() {
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
                            removeSetter(bounty.getUUID(), setter);
                        }
                    }
                }
            }
        }.runTaskAsynchronously(NotBounties.getInstance());
    }
    public void removeOldBounties() {
        NotBounties.debugMessage("Removing old SQL bounties.", false);
        long minTime = (long) (System.currentTimeMillis() - 1000L * 60 * 60 * 24 * BountyExpire.getTime());
        double autoExpire = ConfigOptions.autoBountyExpireTime == -1 ? BountyExpire.getTime() : ConfigOptions.autoBountyExpireTime;
        long minTimeAuto = (long) (System.currentTimeMillis() - 1000L * 60 * 60 * 24 * autoExpire);
        try (PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM notbounties WHERE time <= ? AND suuid != ?;");
             PreparedStatement ps2 = sql.getConnection().prepareStatement("DELETE FROM notbounties WHERE time <= ? AND suuid = ?;")) {

            ps.setLong(1, minTime);
            ps2.setLong(1, minTimeAuto);
            String consoleUUID = new UUID(0,0).toString();
            ps.setString(2, consoleUUID);
            ps2.setString(2, consoleUUID);
            ps.executeUpdate();
            ps2.executeUpdate();

        } catch (SQLException e){
            if (reconnect(e)){
               removeOldBounties();
            }
        }
    }

    public void notifyPlayer(UUID uuid, long lastUpdate) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("UPDATE notbounties SET notified = 1 WHERE uuid = ? AND time < ?;")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, lastUpdate);
            ps.executeUpdate();
            
        } catch (SQLException e) {
            if (reconnect(e)){
                notifyPlayer(uuid, lastUpdate);
            }
        }
    }

    /**
     * Push a series of changes to the mysql database.
     * @param bountyChanges The bounty changes to be pushed to the database
     * @return True if the request was successful
     */
    public boolean pushChanges(List<BountyChange> bountyChanges, Map<UUID, Double[]> statChanges, long lastUpdate) {
        // construct a series of statements
        try (Statement simpleBatch = sql.getConnection().createStatement();
        PreparedStatement addBountyBatch = sql.getConnection().prepareStatement("INSERT INTO notbounties VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        CallableStatement editBountyBatch = sql.getConnection().prepareCall("{CALL edit_bounty(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
        PreparedStatement statBatch = sql.getConnection().prepareStatement("INSERT INTO bounty_data(uuid, claimed, sets, received, alltime, immunity, allclaimed) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE claimed = claimed + ?, sets = sets + ?, received = received + ?, alltime = alltime + ?, immunity = immunity + ?, allclaimed = allclaimed + ?;")) {
            int simpleBatchCount = 0;
            int addBountyBatchCount = 0;
            int editBountyBatchCount = 0;
            for (BountyChange bountyChange : bountyChanges) {
                switch (bountyChange.changeType()) {
                    case NOTIFY -> {
                        // 0: bounty uuid
                        // 1: setter uuid
                        UUID[] uuids = (UUID[]) bountyChange.change();
                        // uuids must match and set time must be before lastSQLLoad
                        simpleBatch.addBatch("UPDATE notbounties SET notified = 1 WHERE uuid = '" + uuids[0].toString() + "' AND suuid = '" + uuids[1].toString() + "' AND time < " + lastUpdate + ";");
                        simpleBatchCount++;
                    }
                    case DELETE_BOUNTY -> {
                        Bounty toRemove = (Bounty) bountyChange.change();
                        for (Setter setter : toRemove.getSetters()) {
                            simpleBatch.addBatch("DELETE FROM notbounties WHERE uuid = '" + toRemove.getUUID().toString() + "' AND suuid = '" + setter.getUuid().toString() + "' AND time < "  + lastUpdate + ";");
                            simpleBatchCount++;
                        }
                    }
                    case ADD_BOUNTY -> {
                        Bounty toAdd = (Bounty) bountyChange.change();
                        for (Setter setter : toAdd.getSetters()) {
                            addBountyBatch.setString(1, toAdd.getUUID().toString());
                            addBountyBatch.setString(2, toAdd.getName());
                            addBountyBatch.setString(3, setter.getName());
                            addBountyBatch.setString(4, setter.getUuid().toString());
                            addBountyBatch.setDouble(5, setter.getAmount());
                            addBountyBatch.setBoolean(6, setter.isNotified());
                            addBountyBatch.setLong(7, setter.getTimeCreated());
                            addBountyBatch.setString(8, encodeWhitelist(setter.getWhitelist()));
                            addBountyBatch.setLong(9, setter.getReceiverPlaytime());
                            addBountyBatch.setBlob(10, SerializeInventory.itemStackArrayToBinaryStream(setter.getItems().toArray(new ItemStack[0])));
                            addBountyBatch.setDouble(11, setter.getDisplayAmount());
                            addBountyBatch.addBatch();
                            addBountyBatchCount++;
                        }
                    }
                    case EDIT_BOUNTY -> {
                        Bounty toEdit = (Bounty) bountyChange.change();
                        Setter original = toEdit.getSetters().get(0);
                        Setter change = toEdit.getSetters().get(1);
                        editBountyBatch.setString(1, toEdit.getName());
                        editBountyBatch.setString(2, toEdit.getUUID().toString());
                        editBountyBatch.setString(3, original.getName());
                        editBountyBatch.setString(4, original.getUuid().toString());
                        editBountyBatch.setBoolean(5, original.isNotified());
                        editBountyBatch.setLong(6, original.getTimeCreated());
                        editBountyBatch.setString(7, encodeWhitelist(original.getWhitelist()));
                        editBountyBatch.setLong(8, original.getReceiverPlaytime());
                        editBountyBatch.setBlob(9, SerializeInventory.itemStackArrayToBinaryStream(original.getItems().toArray(new ItemStack[0])));
                        editBountyBatch.setDouble(10, original.getDisplayAmount());
                        editBountyBatch.setDouble(11, change.getAmount());
                        editBountyBatch.addBatch();
                        editBountyBatchCount++;
                    }
                }
            }
            for (Map.Entry<UUID, Double[]> entry : statChanges.entrySet()) {
                statBatch.setString(1, entry.getKey().toString());
                statBatch.setLong(2, entry.getValue()[0].longValue());
                statBatch.setLong(3, entry.getValue()[1].longValue());
                statBatch.setLong(4, entry.getValue()[2].longValue());
                statBatch.setDouble(5, entry.getValue()[3]);
                statBatch.setDouble(6, entry.getValue()[4]);
                statBatch.setDouble(7, entry.getValue()[5]);
                statBatch.setLong(8, entry.getValue()[0].longValue());
                statBatch.setLong(9, entry.getValue()[1].longValue());
                statBatch.setLong(10, entry.getValue()[2].longValue());
                statBatch.setDouble(11, entry.getValue()[3]);
                statBatch.setDouble(12, entry.getValue()[4]);
                statBatch.setDouble(13, entry.getValue()[5]);
                statBatch.addBatch();
            }
            if (simpleBatchCount > 0) {
                NotBounties.debugMessage("Executing SQL Update: \n" + simpleBatch.toString(), false);
                NotBounties.debugMessage("Rows Updated: " + Arrays.toString(simpleBatch.executeBatch()), false);
            }
            if (addBountyBatchCount > 0) {
                NotBounties.debugMessage("Executing SQL Update: \n" + addBountyBatch.toString(), false);
                NotBounties.debugMessage("Rows Updated: " + Arrays.toString(addBountyBatch.executeBatch()), false);
            }
            if (editBountyBatchCount > 0) {
                NotBounties.debugMessage("Executing SQL Update: \n" + editBountyBatch.toString(), false);
                NotBounties.debugMessage("Rows Updated: " + Arrays.toString(editBountyBatch.executeBatch()), false);
            }
            if (!statChanges.isEmpty()) {
                NotBounties.debugMessage("Executing SQL Update: \n" + statBatch.toString(), false);
                NotBounties.debugMessage("Rows Updated: " + Arrays.toString(statBatch.executeBatch()), false);
            }
            
            return true;
        } catch (SQLException e) {
            if (reconnect(e)){
                return pushChanges(bountyChanges, statChanges, lastUpdate);
            }
        }
        return false;
    }

    public List<Bounty> getTopBounties(int sortType) {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("SELECT uuid, name, setter, suuid, amount, notified, time, whitelist, playtime, items, display FROM notbounties;")){
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
            if (reconnect(e)){
                return getTopBounties(sortType);
            }
        }
        return new ArrayList<>();

    }

    public int removeExtraData() {
        try (PreparedStatement ps = sql.getConnection().prepareStatement("DELETE FROM notbounties WHERE amount = ?;")){
            ps.setDouble(1, 0L);
            return ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect(e)){
                return removeExtraData();
            }
        }
        return 0;
    }

    private boolean reconnect(SQLException e) {
        if (e instanceof SQLSyntaxErrorException) {
            Bukkit.getLogger().warning("[NotBounties] SQL Syntax Error! Please report this to Not_Jaden.");
            Bukkit.getLogger().warning(e.toString());
            return false;
        }
        NotBounties.debugMessage(e.toString(), true);
        if (System.currentTimeMillis() > nextReconnectAttempt) {
            reconnectAttempts++;
            sql.disconnect();
            if (reconnectAttempts < 2) {
                Bukkit.getLogger().warning("Lost connection with database, will try to reconnect.");
            }
            if (reconnectAttempts >= 3) {
                reconnectAttempts = 0;
                nextReconnectAttempt = System.currentTimeMillis() + 5000L;
            }

            if (!tryToConnect()) {
                if (reconnectAttempts < 2)
                    Bukkit.getScheduler().runTaskLater(NotBounties.getInstance(), DataManager::tryToConnect, 20L);
                return false;
            }
            return true;
        }
        return false;
    }

}

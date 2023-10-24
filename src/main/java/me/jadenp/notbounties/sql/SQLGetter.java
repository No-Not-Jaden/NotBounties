package me.jadenp.notbounties.sql;

import me.jadenp.notbounties.*;
import me.jadenp.notbounties.utils.ConfigOptions;
import me.jadenp.notbounties.utils.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLGetter {

    private long nextReconnectAttempt;
    private int reconnectAttempts;
    private final boolean debug = false;
    public SQLGetter (){
        nextReconnectAttempt = System.currentTimeMillis();
        reconnectAttempts = 0;
    }

    public void createTable(){
        PreparedStatement ps;
        try {
            ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS notbounties" +
                    "(" +
                    "    uuid CHAR(36) NOT NULL," +
                    "    name VARCHAR(16) NOT NULL," +
                    "    setter VARCHAR(16) NOT NULL," +
                    "    suuid CHAR(36) NOT NULL," +
                    "    amount FLOAT(53) DEFAULT 0 NOT NULL," +
                    "    notified BOOLEAN DEFAULT TRUE NOT NULL," +
                    "    time BIGINT NOT NULL," +
                    "    whitelist VARCHAR(369)" +
                    ");");
            ps.executeUpdate();
            ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = 'jadenplugins' and table_name = 'notbounties' and column_name = 'amount';");
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                if (rs.getString("data_type").equalsIgnoreCase("bigint")){
                    Bukkit.getLogger().info("Updating data type for bounties");
                    ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN amount FLOAT(53);");
                    ps.executeUpdate();
                }
            }
            ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SHOW COLUMNS FROM `notbounties` LIKE 'whitelist';");
            rs = ps.executeQuery();
            if (!rs.next()){
                ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("ALTER TABLE notbounties ADD whitelist VARCHAR(369);");
                ps.executeUpdate();
            }

        } catch (SQLException e){
            Bukkit.getLogger().warning(e.toString());
        }
    }
    public void createDataTable(){
        PreparedStatement ps;
        try {
            ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS bounty_data" +
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
            ps.executeUpdate();
            ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = 'jadenplugins' and table_name = 'bounty_data' and column_name = 'claimed';");
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                if (rs.getString("data_type").equalsIgnoreCase("int")){
                    Bukkit.getLogger().info("Updating data type for statistics");
                    ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("ALTER TABLE bounty_data MODIFY COLUMN claimed BIGINT, MODIFY sets BIGINT, MODIFY received BIGINT, MODIFY alltime FLOAT(53), MODIFY immunity FLOAT(53), MODIFY allclaimed FLOAT(53);");
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e){
            Bukkit.getLogger().warning(e.toString());
        }
    }
    public void addData(String uuid, long claimed, long set, long received, double allTime, double immunity, double allClaimed){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("INSERT INTO bounty_data(uuid, claimed, sets, received, alltime, immunity, allclaimed) VALUES(?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE claimed = claimed + ?, sets = sets + ?, received = received + ?, alltime = alltime + ?, immunity = immunity + ?, allclaimed = allclaimed + ?;");
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
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }

    public long getClaimed(String uuid){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT claimed FROM bounty_data WHERE uuid = ?;");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getLong("claimed");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getClaimed(uuid);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public double getTotalClaimed(String uuid){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT allclaimed FROM bounty_data WHERE uuid = ?;");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getDouble("allclaimed");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getTotalClaimed(uuid);
            }
            //e.printStackTrace();
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
    public LinkedHashMap<String, Double> getTopStats(Leaderboard leaderboard, List<String> hiddenNames, int skip, int results){
        LinkedHashMap<String, Double> data = new LinkedHashMap<>();
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
                    data.put(bounty.getUUID().toString(), bounty.getTotalBounty());
                }
                return data;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hiddenNames.size(); i++) {
            OfflinePlayer player = NotBounties.getInstance().getPlayer(hiddenNames.get(i));
            if (player == null) {
                builder = new StringBuilder();
                Bukkit.getLogger().warning("[NotBounties] Error getting player: " + hiddenNames.get(i) + " from hidden players!");
                break;
            }
            String uuid = player.getUniqueId().toString();
            if (uuid == null)
                continue;
            if (i < hiddenNames.size() - 1)
                builder.append(uuid).append("' AND uuid != '");
            else
                builder.append(uuid);
        }
        String end = ";";
        if (skip + results > 0) {
            end = " LIMIT " + skip + "," + (skip + results) + ";";
        }

        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT uuid, `" + listName + "` FROM bounty_data WHERE uuid != '" + builder + "' ORDER BY ? DESC" + end);
            ps.setString(1, listName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                String uuid = rs.getString("uuid");
                if (leaderboard.isMoney())
                      data.put(uuid, rs.getDouble(2));
                else
                    data.put(uuid, (double) rs.getLong(2));

            }
        } catch (SQLException e){
            if (reconnect()){
                return getTopStats(leaderboard, hiddenNames, skip, results);
            }
            if (debug)
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
                if (debug)
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
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT sets FROM bounty_data WHERE uuid = ?;");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getLong("set");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getSet(uuid);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public long getReceived(String uuid){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT received FROM bounty_data WHERE uuid = ?;");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getLong("received");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getReceived(uuid);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public double getAllTime(String uuid){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT alltime FROM bounty_data WHERE uuid = ?;");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getDouble("alltime");
            }
        } catch (SQLException e){
            if (reconnect()){
                return getAllTime(uuid);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    public double getImmunity(String uuid){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT immunity FROM bounty_data WHERE uuid = ?;");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getDouble("immunity");
            }
        } catch (SQLException e){
            if (reconnect()){
                getImmunity(uuid);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }


    public void setImmunity(String uuid, double amount){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("UPDATE bounty_data SET immunity = ? WHERE uuid = ?;");
            ps.setDouble(1, amount);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                setImmunity(uuid, amount);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }



    public void addBounty(Bounty bounty, Setter setter){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("INSERT INTO notbounties(uuid, name, setter, suuid, amount, notified, time, whitelist) VALUES(?, ?, ?, ?, ?, ?, ?, ?);");
            ps.setString(1, bounty.getUUID().toString());
            ps.setString(2, bounty.getName());
            ps.setString(3, setter.getName());
            ps.setString(4, setter.getUuid().toString());
            ps.setDouble(5, setter.getAmount());
            ps.setBoolean(6, setter.isNotified());
            ps.setLong(7, setter.getTimeCreated());
            ps.setString(8, encodeWhitelist(setter.getWhitelist()));
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                addBounty(bounty, setter);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void addBounty(Bounty bounty){
        for (Setter setter : bounty.getSetters())
            addBounty(bounty, setter);
    }

    public Bounty getBounty(UUID uuid) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT * FROM notbounties WHERE uuid = ?;");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            List<Setter> setters = new ArrayList<>();
            String name = "";
            while (rs.next()){
                if (name.isEmpty()){
                    name = rs.getString("name");
                }
                UUID setterUUID = rs.getString("suuid").equalsIgnoreCase("CONSOLE") ? new UUID(0,0) : UUID.fromString(rs.getString("suuid"));
                setters.add(new Setter(rs.getString("setter"), setterUUID, rs.getDouble("amount"), rs.getLong("time"), rs.getBoolean("notified"), decodeWhitelist(rs.getString("whitelist"))));
            }
            return new Bounty(uuid, setters, name);
        } catch (SQLException e){
            if (reconnect()){
                return getBounty(uuid);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return null;
    }
    public void updateBounty(Bounty bounty) {
        removeBounty(bounty.getUUID());
        addBounty(bounty);
    }
    public void editBounty(UUID uuid, UUID setterUUID, double amount) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("UPDATE notbounties SET amount = amount + ? WHERE uuid = ? AND suuid = ?;");
            ps.setDouble(1, amount);
            ps.setString(2, uuid.toString());
            ps.setString(3, setterUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                editBounty(uuid, setterUUID, amount);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void removeBounty(UUID uuid) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeBounty(uuid);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public void removeSetter(UUID uuid, UUID setterUUID) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("DELETE notbounties WHERE uuid = ? AND suuid = ?;");
            ps.setString(1, uuid.toString());
            ps.setString(2, setterUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeSetter(uuid, setterUUID);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
    }
    public List<Setter> removeOldBounties() {
        long minTime = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * ConfigOptions.bountyExpire;
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("DELETE notbounties WHERE time <= ?;");
            PreparedStatement ps1 = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT * FROM notbounties WHERE time <= ?;");
            ps.setLong(1, minTime);
            ps1.setLong(1, minTime);
            List<Setter> setters = new ArrayList<>();
            ResultSet rs = ps1.executeQuery();
            while (rs.next()){
                UUID setterUUID = rs.getString("suuid").equalsIgnoreCase("CONSOLE") ? new UUID(0,0) : UUID.fromString(rs.getString("suuid"));
                setters.add(new Setter(rs.getString("setter"), setterUUID, rs.getDouble("amount"), rs.getLong("time"), rs.getBoolean("notified"), decodeWhitelist(rs.getString("whitelist"))));
            }
            ps.executeUpdate();
            return setters;
        } catch (SQLException e){
            if (reconnect()){
               return removeOldBounties();
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return new ArrayList<>();
    }
    /*
    public void setBounty(String uuid, String setterUUID, long amount) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("UPDATE notbounties SET amount = ? WHERE uuid = ? AND suuid = ?;");
            ps.setLong(1, amount);
            ps.setString(2, uuid);
            ps.setString(3, setterUUID);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                setBounty(uuid, setterUUID, amount);
            } else {
                NotBounties.getInstance().
            }
            //e.printStackTrace();
        }
    }*/

    public List<Bounty> getTopBounties(int sortType) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT * FROM notbounties;");
            ResultSet resultSet = ps.executeQuery();

            Map<String, Bounty> bountyAmounts = new HashMap<>();
            while (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                if (uuid != null) {
                    if (bountyAmounts.containsKey(uuid)){
                       bountyAmounts.get(uuid).addBounty(resultSet.getDouble("amount"), decodeWhitelist(resultSet.getString("whitelist")));
                    } else {
                        bountyAmounts.put(uuid, new Bounty(UUID.fromString(uuid), new ArrayList<>(Collections.singletonList(new Setter(resultSet.getString("setter"), UUID.fromString(resultSet.getString("suuid")), resultSet.getDouble("amount"), resultSet.getLong("time"), resultSet.getBoolean("notified"), decodeWhitelist(resultSet.getString("whitelist"))))), resultSet.getString("name")));
                    }
                }
            }
            List<Bounty> sortedList = new ArrayList<>(bountyAmounts.values());
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
        } catch (SQLException e){
            if (reconnect()){
                return getTopBounties(sortType);
            }
            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return new ArrayList<>();

    }

    public int removeExtraData() {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("DELETE FROM notbounties WHERE amount = ?;");
            ps.setDouble(1, 0L);
            return ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                return removeExtraData();
            }

            if (debug)
                Bukkit.getLogger().warning(e.toString());
        }
        return 0;
    }

    private boolean reconnect() {
        if (System.currentTimeMillis() > nextReconnectAttempt) {
            reconnectAttempts++;
            NotBounties.getInstance().SQL.disconnect();
            if (reconnectAttempts < 2) {
                Bukkit.getLogger().warning("Lost connection with database, will try to reconnect.");
            }
            if (reconnectAttempts >= 3) {
                reconnectAttempts = 0;
                nextReconnectAttempt = System.currentTimeMillis() + 5000L;
            }

            if (!NotBounties.getInstance().tryToConnect()) {
                if (reconnectAttempts < 2)
                    Bukkit.getScheduler().runTaskLater(NotBounties.getInstance(), () -> NotBounties.getInstance().tryToConnect(), 20L);
                return false;
            }
            return true;
        }
        return false;
    }

}

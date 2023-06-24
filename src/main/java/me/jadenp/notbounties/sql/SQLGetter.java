package me.jadenp.notbounties.sql;

import me.jadenp.notbounties.*;
import org.bukkit.Bukkit;

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
                    "    time BIGINT NOT NULL" +
                    ");");
            ps.executeUpdate();
            ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("select column_name,data_type from information_schema.columns where table_schema = 'jadenplugins' and table_name = 'notbounties' and column_name = 'amount';");
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                if (rs.getString("data_type").equalsIgnoreCase("bigint")){
                    Bukkit.getLogger().info("Updating data type for bounties");
                    ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("ALTER TABLE notbounties MODIFY COLUMN amount FLOAT(53)");
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
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
                    ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("ALTER TABLE bounty_data MODIFY COLUMN claimed BIGINT, MODIFY sets BIGINT, MODIFY received BIGINT, MODIFY alltime FLOAT(53), MODIFY immunity FLOAT(53), MODIFY allclaimed FLOAT(53)");
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
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
                e.printStackTrace();
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
                e.printStackTrace();
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
        }
        LinkedHashMap<String, Double> data = new LinkedHashMap<>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hiddenNames.size(); i++) {
            String uuid = NotBounties.getInstance().loggedPlayers.get(hiddenNames.get(i).toLowerCase());
            if (uuid == null)
                continue;
            if (i < hiddenNames.size() - 1)
                builder.append(uuid).append("' AND uuid != '");
            else
                builder.append(uuid);
        }

        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT uuid, `" + listName + "` FROM bounty_data WHERE uuid != '" + builder + "' ORDER BY ? DESC LIMIT ?,?;");
            ps.setString(1, listName);
            ps.setInt(2, skip);
            ps.setInt(3, skip + results);
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
                e.printStackTrace();
        }
        return data;
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
                e.printStackTrace();
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
                e.printStackTrace();
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
                e.printStackTrace();
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
                e.printStackTrace();
        }
        return 0;
    }


    public void setImmunity(String uuid, double amount){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("REPLACE bounty_data(uuid, immunity) VALUES(? ,?);");
            ps.setString(1, uuid);
            ps.setDouble(2, amount);
            ps.executeQuery();
        } catch (SQLException e){
            if (reconnect()){
                setImmunity(uuid, amount);
            }
            if (debug)
                e.printStackTrace();
        }
    }



    public void addBounty(Bounty bounty, Setter setter){
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("INSERT INTO notbounties(uuid, name, setter, suuid, amount, notified, time) VALUES(?, ?, ?, ?, ?, ?, ?);");
            ps.setString(1, bounty.getUUID());
            ps.setString(2, bounty.getName());
            ps.setString(3, setter.getName());
            ps.setString(4, setter.getUuid());
            ps.setDouble(5, setter.getAmount());
            ps.setBoolean(6, setter.isNotified());
            ps.setLong(7, setter.getTimeCreated());
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                addBounty(bounty, setter);
            }
            if (debug)
                e.printStackTrace();
        }
    }

    public Bounty getBounty(String uuid) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT * FROM notbounties WHERE uuid = ?;");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            List<Setter> setters = new ArrayList<>();
            String name = "";
            while (rs.next()){
                if (name.equals("")){
                    name = rs.getString("name");
                }
                setters.add(new Setter(rs.getString("setter"), rs.getString("suuid"), rs.getDouble("amount"), rs.getLong("time"), rs.getBoolean("notified")));
            }
            return new Bounty(uuid, setters, name);
        } catch (SQLException e){
            if (reconnect()){
                return getBounty(uuid);
            }
            if (debug)
                e.printStackTrace();
        }
        return null;
    }
    public void editBounty(String uuid, String setterUUID, double amount) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("UPDATE notbounties SET amount = amount + ? WHERE uuid = ? AND suuid = ?;");
            ps.setDouble(1, amount);
            ps.setString(2, uuid);
            ps.setString(3, setterUUID);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                editBounty(uuid, setterUUID, amount);
            }
            if (debug)
                e.printStackTrace();
        }
    }
    public void removeBounty(String uuid) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("DELETE FROM notbounties WHERE uuid = ?");
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeBounty(uuid);
            }
            if (debug)
                e.printStackTrace();
        }
    }
    public void removeSetter(String uuid, String setterUUID) {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("DELETE notbounties WHERE uuid = ? AND suuid = ?;");
            ps.setString(1, uuid);
            ps.setString(2, setterUUID);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeSetter(uuid, setterUUID);
            }
            if (debug)
                e.printStackTrace();
        }
    }
    public void removeOldBounties() {
        long minTime = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * ConfigOptions.bountyExpire;
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("DELETE notbounties WHERE time <= ?;");
            ps.setLong(1, minTime);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeOldBounties();
            }
            if (debug)
                e.printStackTrace();
        }
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

    public List<Bounty> getTopBounties() {
        try {
            PreparedStatement ps = NotBounties.getInstance().SQL.getConnection().prepareStatement("SELECT uuid, name, amount FROM notbounties;");
            ResultSet resultSet = ps.executeQuery();

            Map<String, Bounty> bountyAmounts = new HashMap<>();
            while (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                if (uuid != null) {
                    if (bountyAmounts.containsKey(uuid)){
                       bountyAmounts.get(uuid).addBounty(resultSet.getDouble("amount"));
                    } else {
                        bountyAmounts.put(uuid, new Bounty(uuid, new ArrayList<>(Collections.singletonList(new Setter("", "", resultSet.getDouble("amount"), 0, true))), resultSet.getString("name")));
                    }
                }
            }
            List<Bounty> bounties = new ArrayList<>(bountyAmounts.values());
            Collections.sort(bounties);
            return bounties;
        } catch (SQLException e){
            if (reconnect()){
                return getTopBounties();
            }
            if (debug)
                e.printStackTrace();
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
                e.printStackTrace();
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

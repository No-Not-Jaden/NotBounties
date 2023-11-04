package me.jadenp.notbounties.sql;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {
    private final Plugin plugin;
    private Connection connection;

    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    public MySQL(Plugin plugin){
        this.plugin = plugin;
        host = (plugin.getConfig().isSet("database.host") ? plugin.getConfig().getString("database.host") : "localhost");
        port = (plugin.getConfig().isSet("database.port") ? plugin.getConfig().getString("database.port") : "3306");
        database = (plugin.getConfig().isSet("database.database") ? plugin.getConfig().getString("database.database") : "db");
        username = (plugin.getConfig().isSet("database.user") ? plugin.getConfig().getString("database.user") : "user");
        password = (plugin.getConfig().isSet("database.password") ? plugin.getConfig().getString("database.password") : "");
        useSSL = (plugin.getConfig().isSet("database.use-ssl") && plugin.getConfig().getBoolean("database.use-ssl"));
    }

    public void reconnect() throws SQLException {
        if (isConnected())
            connection.close();
        connection = null;
        host = (plugin.getConfig().isSet("database.host") ? plugin.getConfig().getString("database.host") : "localhost");
        port = (plugin.getConfig().isSet("database.port") ? plugin.getConfig().getString("database.port") : "3306");
        database = (plugin.getConfig().isSet("database.database") ? plugin.getConfig().getString("database.database") : "db");
        username = (plugin.getConfig().isSet("database.user") ? plugin.getConfig().getString("database.user") : "user");
        password = (plugin.getConfig().isSet("database.password") ? plugin.getConfig().getString("database.password") : "");
        useSSL = (plugin.getConfig().isSet("database.use-ssl") && plugin.getConfig().getBoolean("database.use-ssl"));
        connect();
    }

    public boolean isConnected() {
        return connection != null;
    }

    public void connect() throws SQLException {
        if (!isConnected())
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database + "?useSSL=" + useSSL,
                    username, password);
    }

    public String getDatabaseType() throws SQLException {
        if (isConnected()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName();
        } else {
            return "N/A";
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


}

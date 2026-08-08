package me.jadenp.notbounties.features.settings.databases.proxy.driver;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class ProxyDriver implements Driver {

    static {
        try {
            DriverManager.registerDriver(new ProxyDriver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) {

        if (!acceptsURL(url))
            return null;

        return new ProxyConnection(url, info);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url.startsWith("jdbc:mcproxy:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

}
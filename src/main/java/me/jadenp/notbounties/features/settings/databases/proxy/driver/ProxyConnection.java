package me.jadenp.notbounties.features.settings.databases.proxy.driver;

import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;
import me.jadenp.notbounties.features.settings.databases.proxy.ProxySettings;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;

public class ProxyConnection implements Connection {
    private final String url;
    private final Properties info;
    private boolean closed = false;
    private boolean autoCommit = true;

    // Pending ops buffered when autoCommit is false
    private static final class PendingOp {
        final String sql; final List<ProxyMessaging.DBParam> params;
        PendingOp(String sql, List<ProxyMessaging.DBParam> params) { this.sql = sql; this.params = params; }
    }
    private final List<PendingOp> pending = new ArrayList<>();

    public ProxyConnection(String url, Properties info) {
        this.url = url;
        this.info = info == null ? new Properties() : info;
    }

    private void ensureOpen() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
    }

    @Override
    public Statement createStatement() throws SQLException {
        ensureOpen();
        return new ProxyStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        ensureOpen();
        return new ProxyPreparedStatement(this, sql);
    }

    // Transaction controls: buffer updates when autoCommit is false, flush on commit or when switching to true
    @Override public synchronized void setAutoCommit(boolean autoCommit) throws SQLException {
        boolean was = this.autoCommit;
        this.autoCommit = autoCommit;
        if (!was && autoCommit) {
            // transitioning to auto-commit -> flush pending
            flushPending();
        }
    }
    @Override public boolean getAutoCommit() { return autoCommit; }

    @Override public synchronized void commit() throws SQLException { flushPending(); }
    @Override public synchronized void rollback() { pending.clear(); }

    synchronized void enqueueUpdate(String sql, List<ProxyMessaging.DBParam> params) throws SQLException {
        ensureOpen();
        // copy params defensively
        List<ProxyMessaging.DBParam> copy;
        if (params == null || params.isEmpty()) copy = Collections.emptyList();
        else copy = new ArrayList<>(params);
        pending.add(new PendingOp(sql, copy));
    }

    private void flushPending() throws SQLException {
        if (pending.isEmpty()) return;
        // Build ops list for messaging
        List<ProxyMessaging.DBOp> ops = new ArrayList<>(pending.size());
        for (PendingOp p : pending) ops.add(new ProxyMessaging.DBOp(p.sql, p.params));
        pending.clear();
        // Send as single transaction to proxy
        ProxyMessaging.DBResponse resp = ProxyMessaging.requestExecuteTransaction(ops, 10_000);
        if (resp.error != null) throw new SQLException(resp.error);
    }

    @Override
    public void close() { this.closed = true; pending.clear(); }

    @Override
    public boolean isClosed() { return closed || !ProxySettings.isConnected(); }

    @Override public DatabaseMetaData getMetaData() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setReadOnly(boolean readOnly) throws SQLException { }
    @Override public boolean isReadOnly() throws SQLException { return true; }
    @Override public void setCatalog(String catalog) throws SQLException { }
    @Override public String getCatalog() throws SQLException { return null; }
    @Override public void setTransactionIsolation(int level) throws SQLException { }
    @Override public int getTransactionIsolation() throws SQLException { return Connection.TRANSACTION_READ_COMMITTED; }
    @Override public SQLWarning getWarnings() throws SQLException { return null; }
    @Override public void clearWarnings() throws SQLException { }

    @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return createStatement(); }
    @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return prepareStatement(sql); }
    @Override public CallableStatement prepareCall(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public String nativeSQL(String sql) throws SQLException { return sql; }
    @Override public Map<String, Class<?>> getTypeMap() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setHoldability(int holdability) throws SQLException { }
    @Override public int getHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
    @Override public Savepoint setSavepoint() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Savepoint setSavepoint(String name) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void rollback(Savepoint savepoint) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return createStatement(); }
    @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return prepareStatement(sql); }
    @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return prepareStatement(sql); }
    @Override public Clob createClob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Blob createBlob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public NClob createNClob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public SQLXML createSQLXML() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean isValid(int timeout) { return !closed; }
    @Override public void setClientInfo(String name, String value) { }
    @Override public void setClientInfo(Properties properties) { }
    @Override public String getClientInfo(String name) { return null; }
    @Override public Properties getClientInfo() { return info; }
    @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setSchema(String schema) throws SQLException { }
    @Override public String getSchema() throws SQLException { return null; }
    @Override public void abort(Executor executor) throws SQLException { close(); }
    @Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException { }
    @Override public int getNetworkTimeout() throws SQLException { return 0; }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
}

package me.jadenp.notbounties.features.settings.databases.proxy.driver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

/**
 * Factory for lightweight, forward-only, read-only ResultSet instances backed by in-memory data.
 * Only a small subset of methods are supported; all others throw SQLFeatureNotSupportedException.
 */
public final class ProxyResultSets {
    private ProxyResultSets() {}

    public static ResultSet create(List<Map<String, Object>> rows, List<String> columns) {
        List<Map<String, Object>> safeRows = rows == null ? Collections.emptyList() : rows;
        List<String> safeCols = columns == null ? Collections.emptyList() : columns;
        class State { int cursor = -1; boolean closed = false; boolean wasNull = false; }
        State state = new State();

        InvocationHandler h = (proxy, method, args) -> {
            String name = method.getName();
            // Handle Object methods
            if (name.equals("toString")) return "ProxyResultSet{" + safeRows.size() + " rows}";
            if (name.equals("hashCode")) return System.identityHashCode(proxy);
            if (name.equals("equals")) return proxy == args[0];

            // Utility lambdas
            Runnable ensureOpen = () -> { if (state.closed) throw new RuntimeException(new SQLException("ResultSet is closed")); };
            java.util.function.Function<Integer, Object> getByIndex = (idx1) -> {
                int idx = idx1 - 1;
                if (state.cursor < 0 || state.cursor >= safeRows.size()) throw new RuntimeException(new SQLException("Cursor not on a valid row"));
                if (idx < 0 || idx >= safeCols.size()) throw new RuntimeException(new SQLException("Invalid column index"));
                Object v = safeRows.get(state.cursor).get(safeCols.get(idx));
                state.wasNull = (v == null);
                return v;
            };
            java.util.function.Function<String, Object> getByLabel = (label) -> {
                if (state.cursor < 0 || state.cursor >= safeRows.size()) throw new RuntimeException(new SQLException("Cursor not on a valid row"));
                Object v = safeRows.get(state.cursor).get(label);
                state.wasNull = (v == null);
                return v;
            };
            java.util.function.Function<Object, byte[]> toBytes = (v) -> {
                if (v instanceof byte[] b) return b;
                if (v instanceof String s) return s.getBytes();
                return null;
            };
            java.util.function.Function<Object, Integer> toInt = (v) -> {
                if (v instanceof Number n) return n.intValue();
                if (v == null) return 0;
                return Integer.parseInt(String.valueOf(v));
            };
            java.util.function.Function<Object, Long> toLong = (v) -> {
                if (v instanceof Number n) return n.longValue();
                if (v == null) return 0L;
                return Long.parseLong(String.valueOf(v));
            };
            java.util.function.Function<Object, Double> toDouble = (v) -> {
                if (v instanceof Number n) return n.doubleValue();
                if (v == null) return 0d;
                return Double.parseDouble(String.valueOf(v));
            };

            switch (name) {
                case "next" -> { ensureOpen.run(); if (state.cursor + 1 < safeRows.size()) { state.cursor++; state.wasNull = false; return true; } return false; }
                case "close" -> { state.closed = true; return null; }
                case "isClosed" -> { return state.closed; }
                case "wasNull" -> { return state.wasNull; }
                case "getString" -> {
                    ensureOpen.run();
                    if (args[0] instanceof Integer i) return Objects.toString(getByIndex.apply(i), null);
                    else return Objects.toString(getByLabel.apply((String) args[0]), null);
                }
                case "getInt" -> { ensureOpen.run(); Object v = (args[0] instanceof Integer i) ? getByIndex.apply(i) : getByLabel.apply((String) args[0]); return toInt.apply(v); }
                case "getLong" -> { ensureOpen.run(); Object v = (args[0] instanceof Integer i) ? getByIndex.apply(i) : getByLabel.apply((String) args[0]); return toLong.apply(v); }
                case "getDouble" -> { ensureOpen.run(); Object v = (args[0] instanceof Integer i) ? getByIndex.apply(i) : getByLabel.apply((String) args[0]); return toDouble.apply(v); }
                case "getBytes" -> { ensureOpen.run(); Object v = (args[0] instanceof Integer i) ? getByIndex.apply(i) : getByLabel.apply((String) args[0]); return toBytes.apply(v); }
                case "getBlob" -> {
                    ensureOpen.run();
                    Object v = (args[0] instanceof Integer i) ? getByIndex.apply(i) : getByLabel.apply((String) args[0]);
                    if (v instanceof Blob b) return b;
                    byte[] data = toBytes.apply(v);
                    if (data == null) return null;
                    return new SimpleBlob(data);
                }
                case "findColumn" -> { return Math.max(0, safeCols.indexOf((String) args[0])) + 1; }
                case "getRow" -> { return state.cursor + 1; }
                case "getFetchDirection" -> { return ResultSet.FETCH_FORWARD; }
                case "getType" -> { return ResultSet.TYPE_FORWARD_ONLY; }
                case "getConcurrency" -> { return ResultSet.CONCUR_READ_ONLY; }
                case "getHoldability" -> { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
                default -> {
                    // Unsupported -> throw SQLFeatureNotSupportedException
                    throw new SQLFeatureNotSupportedException("Method not supported by ProxyResultSet: " + name);
                }
            }
        };
        return (ResultSet) Proxy.newProxyInstance(ProxyResultSets.class.getClassLoader(), new Class[]{ResultSet.class}, h);
    }

    private static final class SimpleBlob implements Blob {
        private byte[] data;
        SimpleBlob(byte[] data) { this.data = data; }
        @Override public long length() { return data == null ? 0 : data.length; }
        @Override public byte[] getBytes(long pos, int length) { if (data == null) return null; int start = (int)Math.max(0, pos - 1); int end = Math.min(data.length, start + length); return Arrays.copyOfRange(data, start, end); }
        @Override public InputStream getBinaryStream() { return new ByteArrayInputStream(data == null ? new byte[0] : data); }
        @Override public long position(byte[] pattern, long start) { return -1; }
        @Override public long position(Blob pattern, long start) { return -1; }
        @Override public int setBytes(long pos, byte[] bytes) { throw new UnsupportedOperationException(); }
        @Override public int setBytes(long pos, byte[] bytes, int offset, int len) { throw new UnsupportedOperationException(); }
        @Override public java.io.OutputStream setBinaryStream(long pos) { throw new UnsupportedOperationException(); }
        @Override public void truncate(long len) { throw new UnsupportedOperationException(); }
        @Override public void free() { data = null; }
        @Override public InputStream getBinaryStream(long pos, long length) { return new ByteArrayInputStream(getBytes(pos, (int)length)); }
    }
}

package me.jadenp.notbounties.features.settings.databases;

public enum StatSortType {
    HIGHEST(null, false, false),
    LOWEST(null, true, false),

    NEWEST("player.last_seen", false, true),
    OLDEST("player.last_seen", true, true),

    ALPHABETICAL("player.name", true, true),
    REVERSE_ALPHABETICAL("player.name", false, true);

    private final String sqlColumn;
    private final boolean ascending;
    private final boolean requiresPlayerJoin;

    StatSortType(
            String sqlColumn,
            boolean ascending,
            boolean requiresPlayerJoin
    ) {
        this.sqlColumn = sqlColumn;
        this.ascending = ascending;
        this.requiresPlayerJoin = requiresPlayerJoin;
    }

    public String sqlColumn() {
        return sqlColumn;
    }

    public boolean ascending() {
        return ascending;
    }

    public boolean requiresPlayerJoin() {
        return requiresPlayerJoin;
    }

    public String order() {
        return ascending ? "ASC" : "DESC";
    }

    public String comparison() {
        return ascending ? ">" : "<";
    }
}

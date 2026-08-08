package me.jadenp.notbounties.features.settings.databases;

public enum PlayerSortType {
    ALPHABETICAL("name", true),
    REVERSE_ALPHABETICAL("name", false),
    NEWEST("last_seen", false),
    OLDEST("last_seen", true),
    UUID("uuid", true);

    private final String sqlColumn;
    private final boolean ascending;
    PlayerSortType(String sqlColumn, boolean ascending) {
        this.sqlColumn = sqlColumn;
        this.ascending = ascending;
    }

    public String sqlColumn() {
        return sqlColumn;
    }

    public String order() {
        return ascending ? "ASC" : "DESC";
    }

    public String comparison() {
        return ascending ? ">" : "<";
    }
}

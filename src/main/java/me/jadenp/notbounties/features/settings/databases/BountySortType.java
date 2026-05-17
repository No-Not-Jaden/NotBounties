package me.jadenp.notbounties.features.settings.databases;

public enum BountySortType {

    HIGHEST(false),
    LOWEST(true),

    NEWEST(false),
    OLDEST(true),

    ALPHABETICAL(true),
    REVERSE_ALPHABETICAL(false);

    private final boolean ascending;

    BountySortType(
            boolean ascending
    ) {
        this.ascending = ascending;
    }

    public boolean ascending() {
        return ascending;
    }

    public String order() {
        return ascending ? "ASC" : "DESC";
    }

    public String comparison() {
        return ascending ? ">" : "<";
    }
}
package me.jadenp.notbounties.features.settings.databases;

@FunctionalInterface
public interface VoidDatabaseOperation {
    void run() throws DatabaseConnectionException;
}

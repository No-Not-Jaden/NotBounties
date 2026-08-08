package me.jadenp.notbounties.features.settings.databases;

@FunctionalInterface
public interface DatabaseOperation<T> {
    T run() throws DatabaseConnectionException;
}

package me.jadenp.notbounties.features.settings.databases;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.PlayerStat;
import me.jadenp.notbounties.data.player_data.PlayerData;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLSyntaxErrorException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Attempt to reconnect to the database when disconnected.
 */
public class ReconnectWrapper extends NotBountiesDatabase {

    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T run() throws DatabaseConnectionException;
    }

    @FunctionalInterface
    private interface VoidDatabaseOperation {
        void run() throws DatabaseConnectionException;
    }

    private long nextReconnectAttempt;
    private int reconnectAttempts;
    private final NotBountiesDatabase database;

    private synchronized boolean reconnect(Exception e) {
        if (e instanceof SQLSyntaxErrorException) {
            Bukkit.getLogger().warning("[NotBounties] SQL Syntax Error! Please report this to Not_Jaden.");
            Bukkit.getLogger().warning(e::toString);
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> Bukkit.getLogger().warning(stackTraceElement::toString));
            return false;
        }

        NotBounties.debugMessage(e.toString(), true);
        Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> NotBounties.debugMessage(stackTraceElement.toString(), true));
        if (System.currentTimeMillis() > nextReconnectAttempt) {
            reconnectAttempts++;
            disconnect();
            if (reconnectAttempts >= 3) {
                reconnectAttempts = 0;
                nextReconnectAttempt = System.currentTimeMillis() + 5000L;
            }
            if (NotBounties.getInstance().isEnabled()) {
                if (!connect(false)) {
                    if (reconnectAttempts < 2)
                        NotBounties.getServerImplementation().async().runDelayed(() -> connect(true), 20L);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public ReconnectWrapper(NotBountiesDatabase database) {
        super();
        nextReconnectAttempt = System.currentTimeMillis();
        reconnectAttempts = 0;
        this.database = database;
    }

    private <T> T execute(DatabaseOperation<T> operation) throws DatabaseConnectionException {
        if (!isConnected())
            throw notConnectedException;
        try {
            return operation.run();
        } catch (DatabaseConnectionException e) {
            if (reconnect(e)) {
                return operation.run();
            }

            throw e;
        }
    }

    private void executeVoid(VoidDatabaseOperation operation) throws DatabaseConnectionException {
        if (!isConnected())
            throw notConnectedException;
        try {
            operation.run();
        } catch (DatabaseConnectionException e) {
            if (reconnect(e)) {
                operation.run();
                return;
            }

            throw e;
        }
    }

    @Override
    public void addStats(UUID uuid, PlayerStat stats) throws DatabaseConnectionException {

        executeVoid(() -> database.addStats(uuid, stats));
    }

    @Override
    public @NotNull PlayerStat getStats(UUID uuid) throws DatabaseConnectionException {

        return execute(() -> database.getStats(uuid));
    }

    @Override
    public Map<UUID, PlayerStat> getAllStats() throws DatabaseConnectionException {
        return execute(database::getAllStats);
    }

    @Override
    public void addStats(Map<UUID, PlayerStat> playerStats) throws DatabaseConnectionException {
        executeVoid(() -> database.addStats(playerStats));
    }

    @Override
    public void addBounty(List<Bounty> bounties) throws DatabaseConnectionException {
        executeVoid(() -> database.addBounty(bounties));
    }

    @Override
    public void removeBounty(List<Bounty> bounties) throws DatabaseConnectionException {
        executeVoid(() -> database.removeBounty(bounties));
    }

    @Override
    public Bounty addBounty(@NotNull Bounty bounty) throws DatabaseConnectionException {
        return execute(() -> database.addBounty(bounty));
    }

    @Override
    public @Nullable Bounty getBounty(UUID uuid) throws DatabaseConnectionException {
        return execute(() -> database.getBounty(uuid));
    }

    @Override
    public void removeBounty(UUID uuid) throws DatabaseConnectionException {
        executeVoid(() -> database.removeBounty(uuid));
    }

    @Override
    public void removeBounty(Bounty bounty) throws DatabaseConnectionException {
        executeVoid(() -> database.removeBounty(bounty));
    }

    @Override
    public List<Bounty> getAllBounties(int sortType) throws DatabaseConnectionException {
        return execute(() -> database.getAllBounties(sortType));
    }

    @Override
    public boolean isConnected() {
        return database.isConnected();
    }

    @Override
    public boolean connect(boolean force) {
        return database.connect(force);
    }

    @Override
    public void disconnect() {
        database.disconnect();
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() throws DatabaseConnectionException {
        return database.getOnlinePlayers();
    }

    @Override
    public void updatePlayerData(PlayerData playerData) throws DatabaseConnectionException {
        executeVoid(() -> database.updatePlayerData(playerData));
    }

    @Override
    public PlayerData getPlayerData(@NotNull UUID uuid) throws DatabaseConnectionException {
        return execute(() -> database.getPlayerData(uuid));
    }

    @Override
    public void addPlayerData(List<PlayerData> playerDataMap) throws DatabaseConnectionException {
        executeVoid(() -> database.addPlayerData(playerDataMap));
    }

    @Override
    public List<PlayerData> getPlayerData() throws DatabaseConnectionException {
        return execute(() -> database.getPlayerData());
    }

    @Override
    public void notifyBounty(UUID uuid) throws DatabaseConnectionException {
        executeVoid(() -> database.notifyBounty(uuid));
    }

    @Override
    public void login(UUID uuid, String playerName) throws DatabaseConnectionException {
        executeVoid(() -> database.login(uuid, playerName));
    }

    @Override
    public void logout(UUID uuid) throws DatabaseConnectionException {
        executeVoid(() -> database.logout(uuid));
    }

    @Override
    public boolean isPermDatabase() {
        return database.isPermDatabase();
    }
}

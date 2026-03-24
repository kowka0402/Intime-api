package net.rj.intime.core.database;

import net.rj.intime.core.InTimeCorePlugin;
import net.rj.intime.core.model.PlayerTimeData;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class SQLiteManager {

    private final InTimeCorePlugin plugin;
    private final File databaseFile;
    private Connection connection;

    public SQLiteManager(InTimeCorePlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "intime.db");
    }

    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IllegalStateException("Could not create plugin data folder");
            }

            openConnection();
            createTableIfNeeded();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to initialize SQLite database", exception);
        }
    }

    private void openConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return;
        }

        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        this.connection = DriverManager.getConnection(url);
    }

    private void createTableIfNeeded() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS intime_players (
                    uuid TEXT PRIMARY KEY,
                    last_name TEXT NOT NULL,
                    remaining_seconds INTEGER NOT NULL,
                    initialized INTEGER NOT NULL DEFAULT 1
                );
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public List<PlayerTimeData> loadAll() {
        String sql = "SELECT uuid, last_name, remaining_seconds, initialized FROM intime_players";
        List<PlayerTimeData> list = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                list.add(new PlayerTimeData(
                        UUID.fromString(resultSet.getString("uuid")),
                        resultSet.getString("last_name"),
                        resultSet.getLong("remaining_seconds"),
                        resultSet.getInt("initialized") == 1
                ));
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to load player data from SQLite: " + exception.getMessage());
        }

        return list;
    }

    public void save(PlayerTimeData data) {
        String sql = """
                INSERT INTO intime_players (uuid, last_name, remaining_seconds, initialized)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    last_name = excluded.last_name,
                    remaining_seconds = excluded.remaining_seconds,
                    initialized = excluded.initialized
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, data.getUuid().toString());
            statement.setString(2, data.getLastName() == null ? "UNKNOWN" : data.getLastName());
            statement.setLong(3, data.getRemainingSeconds());
            statement.setInt(4, data.isInitialized() ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save player data for " + data.getUuid() + ": " + exception.getMessage());
        }
    }

    public void saveAll(Collection<PlayerTimeData> snapshot) {
        String sql = """
                INSERT INTO intime_players (uuid, last_name, remaining_seconds, initialized)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    last_name = excluded.last_name,
                    remaining_seconds = excluded.remaining_seconds,
                    initialized = excluded.initialized
                """;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (PlayerTimeData data : snapshot) {
                    statement.setString(1, data.getUuid().toString());
                    statement.setString(2, data.getLastName() == null ? "UNKNOWN" : data.getLastName());
                    statement.setLong(3, data.getRemainingSeconds());
                    statement.setInt(4, data.isInitialized() ? 1 : 0);
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            connection.commit();
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                plugin.getLogger().severe("Failed to rollback SQLite transaction: " + rollbackException.getMessage());
            }
            plugin.getLogger().severe("Failed to bulk save player data: " + exception.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to reset SQLite auto-commit: " + exception.getMessage());
            }
        }
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to close SQLite connection: " + exception.getMessage());
        }
    }
}

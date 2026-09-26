package dev.herbio.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.herbio.config.DatabaseSettings;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Owns the MySQL connection pool and creates the schema on startup. */
public final class Database implements SqlDatabase {

    private static final String CREATE_PLAYERS = """
            CREATE TABLE IF NOT EXISTS herbio_players (
                uuid CHAR(36) NOT NULL,
                level_index INT NOT NULL DEFAULT 0,
                xp BIGINT NOT NULL DEFAULT 0,
                selected_herb VARCHAR(16) NOT NULL DEFAULT 'GREEN',
                dark_unlocked BIGINT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (uuid)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
            """;

    private static final String CREATE_PLOTS = """
            CREATE TABLE IF NOT EXISTS herbio_plots (
                uuid CHAR(36) NOT NULL,
                plot_index INT NOT NULL,
                herb VARCHAR(16) NOT NULL,
                ready_at BIGINT NOT NULL,
                fertilized INT NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, herb, plot_index),
                CONSTRAINT fk_herbio_plots_player FOREIGN KEY (uuid)
                    REFERENCES herbio_players (uuid) ON DELETE CASCADE
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
            """;

    private static final String UPSERT_PLAYER = """
            INSERT INTO herbio_players (uuid, level_index, xp, selected_herb, dark_unlocked)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE level_index = VALUES(level_index), xp = VALUES(xp),
                                    selected_herb = VALUES(selected_herb),
                                    dark_unlocked = VALUES(dark_unlocked)
            """;

    private final HikariDataSource dataSource;

    public Database(DatabaseSettings settings) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("Herbio-MySQL");
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setJdbcUrl(settings.jdbcUrl());
        hikari.setUsername(settings.username());
        hikari.setPassword(settings.password());
        hikari.setMaximumPoolSize(settings.poolSize());
        hikari.setMinimumIdle(Math.min(2, settings.poolSize()));
        hikari.setConnectionTimeout(10_000L);
        hikari.setMaxLifetime(600_000L);
        this.dataSource = new HikariDataSource(hikari);
    }

    @Override
    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void createSchema() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_PLAYERS);
            statement.executeUpdate(CREATE_PLOTS);
            migrate(statement);
        }
    }

    @Override
    public String upsertPlayerSql() {
        return UPSERT_PLAYER;
    }

    /** Brings older tables up to date; every statement fails harmlessly once it has been applied. */
    private static void migrate(Statement statement) {
        runQuietly(statement, "ALTER TABLE herbio_plots ADD COLUMN fertilized INT NOT NULL DEFAULT 0");
        runQuietly(statement, "ALTER TABLE herbio_players ADD COLUMN dark_unlocked BIGINT NOT NULL DEFAULT 0");
        // One field per herb type: the same plot index now exists once per field.
        runQuietly(statement, "ALTER TABLE herbio_plots DROP PRIMARY KEY, ADD PRIMARY KEY (uuid, herb, plot_index)");
    }

    private static void runQuietly(Statement statement, String sql) {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException ignored) {
            // Already applied.
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}

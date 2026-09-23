package dev.herbio.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.herbio.config.DatabaseSettings;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Owns the MySQL connection pool and creates the schema on startup. */
public final class Database implements AutoCloseable {

    private static final String CREATE_PLAYERS = """
            CREATE TABLE IF NOT EXISTS herbio_players (
                uuid CHAR(36) NOT NULL,
                level_index INT NOT NULL DEFAULT 0,
                xp BIGINT NOT NULL DEFAULT 0,
                selected_herb VARCHAR(16) NOT NULL DEFAULT 'GREEN',
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
                PRIMARY KEY (uuid, plot_index),
                CONSTRAINT fk_herbio_plots_player FOREIGN KEY (uuid)
                    REFERENCES herbio_players (uuid) ON DELETE CASCADE
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
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

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    public void createSchema() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_PLAYERS);
            statement.executeUpdate(CREATE_PLOTS);
            addFertilizedColumn(statement);
        }
    }

    /** Upgrades tables created before the fertilizer limit existed; already there = nothing to do. */
    private static void addFertilizedColumn(Statement statement) {
        try {
            statement.executeUpdate("ALTER TABLE herbio_plots ADD COLUMN fertilized INT NOT NULL DEFAULT 0");
        } catch (SQLException ignored) {
            // Duplicate column: the schema is already up to date.
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}

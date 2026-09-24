package dev.herbio.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Single-file storage for servers without MySQL. One writer, so the pool holds one connection. */
public final class SqliteDatabase implements SqlDatabase {

    private static final String CREATE_PLAYERS = """
            CREATE TABLE IF NOT EXISTS herbio_players (
                uuid TEXT NOT NULL PRIMARY KEY,
                level_index INTEGER NOT NULL DEFAULT 0,
                xp INTEGER NOT NULL DEFAULT 0,
                selected_herb TEXT NOT NULL DEFAULT 'GREEN'
            )
            """;

    private static final String CREATE_PLOTS = """
            CREATE TABLE IF NOT EXISTS herbio_plots (
                uuid TEXT NOT NULL,
                plot_index INTEGER NOT NULL,
                herb TEXT NOT NULL,
                ready_at INTEGER NOT NULL,
                fertilized INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, plot_index)
            )
            """;

    private static final String UPSERT_PLAYER = """
            INSERT INTO herbio_players (uuid, level_index, xp, selected_herb)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET level_index = excluded.level_index, xp = excluded.xp,
                                            selected_herb = excluded.selected_herb
            """;

    private final HikariDataSource dataSource;

    public SqliteDatabase(File file) {
        File directory = file.getParentFile();
        if (directory != null) {
            directory.mkdirs();
        }
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("Herbio-SQLite");
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setConnectionTimeout(10_000L);
        this.dataSource = new HikariDataSource(hikari);
    }

    @Override
    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void createSchema() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            // Write-ahead logging keeps the autosave from blocking reads.
            statement.execute("PRAGMA journal_mode = WAL");
            statement.executeUpdate(CREATE_PLAYERS);
            statement.executeUpdate(CREATE_PLOTS);
        }
    }

    @Override
    public String upsertPlayerSql() {
        return UPSERT_PLAYER;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}

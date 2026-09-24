package dev.herbio.storage;

import java.sql.Connection;
import java.sql.SQLException;

/** A JDBC backend for {@link SqlPlayerRepository}; the dialect differences live here. */
public interface SqlDatabase extends AutoCloseable {

    Connection connection() throws SQLException;

    void createSchema() throws SQLException;

    /** Upsert for herbio_players, bound as (uuid, level_index, xp, selected_herb). */
    String upsertPlayerSql();

    @Override
    void close();
}

package dev.herbio.storage;

import dev.herbio.garden.PlotState;
import dev.herbio.herb.HerbType;
import dev.herbio.player.ProfileSnapshot;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Blocking JDBC access for {@link ProfileSnapshot}; always called from an async thread. */
public final class SqlPlayerRepository implements PlayerRepository {

    private static final String SELECT_PLAYER =
            "SELECT level_index, xp, selected_herb FROM herbio_players WHERE uuid = ?";
    private static final String SELECT_PLOTS =
            "SELECT plot_index, herb, ready_at, fertilized FROM herbio_plots WHERE uuid = ?";
    private static final String DELETE_PLOTS = "DELETE FROM herbio_plots WHERE uuid = ?";
    private static final String INSERT_PLOT =
            "INSERT INTO herbio_plots (uuid, plot_index, herb, ready_at, fertilized) VALUES (?, ?, ?, ?, ?)";

    private final SqlDatabase database;
    private final String upsertPlayer;

    public SqlPlayerRepository(SqlDatabase database) {
        this.database = database;
        this.upsertPlayer = database.upsertPlayerSql();
    }

    @Override
    public @Nullable ProfileSnapshot load(UUID uuid) throws StorageException {
        try (Connection connection = database.connection()) {
            int levelIndex;
            long xp;
            HerbType selected;
            try (PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }
                    levelIndex = result.getInt("level_index");
                    xp = result.getLong("xp");
                    selected = HerbType.byIdOrDefault(result.getString("selected_herb"), HerbType.GREEN);
                }
            }
            List<PlotState> plots = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(SELECT_PLOTS)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        HerbType herb = HerbType.byIdOrDefault(result.getString("herb"), null);
                        if (herb != null) {
                            plots.add(new PlotState(result.getInt("plot_index"), herb, result.getLong("ready_at"),
                                        result.getInt("fertilized")));
                        }
                    }
                }
            }
            return new ProfileSnapshot(uuid, levelIndex, xp, selected, List.copyOf(plots));
        } catch (SQLException failure) {
            throw new StorageException("Could not read profile " + uuid, failure);
        }
    }

    @Override
    public void save(ProfileSnapshot snapshot) throws StorageException {
        try (Connection connection = database.connection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsertPlayer)) {
                    statement.setString(1, snapshot.uuid().toString());
                    statement.setInt(2, snapshot.levelIndex());
                    statement.setLong(3, snapshot.xp());
                    statement.setString(4, snapshot.selectedHerb().name());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(DELETE_PLOTS)) {
                    statement.setString(1, snapshot.uuid().toString());
                    statement.executeUpdate();
                }
                if (!snapshot.plots().isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement(INSERT_PLOT)) {
                        for (PlotState plot : snapshot.plots()) {
                            statement.setString(1, snapshot.uuid().toString());
                            statement.setInt(2, plot.index());
                            statement.setString(3, plot.herb().name());
                            statement.setLong(4, plot.readyAtMillis());
                            statement.setInt(5, plot.fertilizerUses());
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                }
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException failure) {
            throw new StorageException("Could not write profile " + snapshot.uuid(), failure);
        }
    }
}

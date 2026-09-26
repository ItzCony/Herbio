package dev.herbio;

import dev.herbio.command.HerbCommand;
import dev.herbio.config.HerbioConfig;
import dev.herbio.config.Messages;
import dev.herbio.economy.VaultEconomy;
import dev.herbio.garden.GardenService;
import dev.herbio.gui.GuiManager;
import dev.herbio.gui.HerbGuiListener;
import dev.herbio.gui.ScrollUseListener;
import dev.herbio.herb.HerbItems;
import dev.herbio.player.PlayerManager;
import dev.herbio.player.PlayerSessionListener;
import dev.herbio.storage.Database;
import dev.herbio.storage.PlayerRepository;
import dev.herbio.storage.SqlDatabase;
import dev.herbio.storage.SqlPlayerRepository;
import dev.herbio.storage.SqliteDatabase;
import dev.herbio.storage.StorageType;
import dev.herbio.storage.YamlPlayerRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;

/** Wires the plugin together; every subsystem is constructed here and nowhere else. */
public final class HerbioPlugin extends JavaPlugin {

    private SqlDatabase database;
    private PlayerManager players;
    private GuiManager guis;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        HerbioConfig config = HerbioConfig.load(getConfig());
        Messages messages = Messages.load(getConfig());

        PlayerRepository repository;
        switch (config.storageType()) {
            case MYSQL, SQLITE -> {
                try {
                    this.database = config.storageType() == StorageType.MYSQL
                            ? new Database(config.database())
                            : new SqliteDatabase(new File(getDataFolder(), "herbio.db"));
                    database.createSchema();
                } catch (SQLException | RuntimeException failure) {
                    getLogger().log(Level.SEVERE, "Could not open the " + config.storageType()
                            + " storage, disabling Herbio.", failure);
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                repository = new SqlPlayerRepository(database);
            }
            case YAML -> {
                repository = new YamlPlayerRepository(new File(getDataFolder(), "players"));
                getLogger().info("Profiles are stored in plugins/Herbio/players.");
            }
            default -> throw new IllegalStateException("Unknown storage type");
        }

        HerbItems items = new HerbItems(this);
        this.players = new PlayerManager(this, config, repository);
        GardenService gardens = new GardenService(config, items);
        this.guis = new GuiManager(this, config, messages, players, gardens, items, new VaultEconomy(getServer()));

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(players), this);
        getServer().getPluginManager().registerEvents(new HerbGuiListener(guis), this);
        getServer().getPluginManager().registerEvents(new ScrollUseListener(guis, items), this);

        PluginCommand command = getCommand("herb");
        if (command != null) {
            HerbCommand executor = new HerbCommand(this, messages, guis, items, players);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        players.startAutosave();
        guis.startRefreshTask();

        // Covers /reload and plugin managers: players are already online at this point.
        for (Player online : getServer().getOnlinePlayers()) {
            players.load(online.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        if (guis != null) {
            guis.shutdown();
        }
        if (players != null) {
            players.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }
}

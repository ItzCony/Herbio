package dev.herbio.storage;

import dev.herbio.garden.PlotState;
import dev.herbio.herb.HerbType;
import dev.herbio.player.ProfileSnapshot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Storage used when MySQL is switched off: one YAML file per player. */
public final class YamlPlayerRepository implements PlayerRepository {

    private final File directory;

    public YamlPlayerRepository(File directory) {
        this.directory = directory;
    }

    @Override
    public @Nullable ProfileSnapshot load(UUID uuid) {
        File file = fileOf(uuid);
        if (!file.isFile()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<PlotState> plots = new ArrayList<>();
        ConfigurationSection fields = yaml.getConfigurationSection("plots");
        if (fields != null) {
            for (String fieldKey : fields.getKeys(false)) {
                HerbType field = HerbType.byIdOrDefault(fieldKey, null);
                ConfigurationSection section = fields.getConfigurationSection(fieldKey);
                if (field == null || section == null) {
                    continue;
                }
                for (String key : section.getKeys(false)) {
                    int index = plotIndex(key);
                    if (index >= 0) {
                        plots.add(new PlotState(index, field, section.getLong(key + ".ready-at"),
                                section.getInt(key + ".fertilized")));
                    }
                }
            }
        }
        return new ProfileSnapshot(uuid,
                yaml.getInt("level-index"),
                yaml.getLong("xp"),
                HerbType.byIdOrDefault(yaml.getString("selected-herb"), HerbType.GREEN),
                yaml.getLong("dark-unlocked"),
                List.copyOf(plots));
    }

    @Override
    public void save(ProfileSnapshot snapshot) throws StorageException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("level-index", snapshot.levelIndex());
        yaml.set("xp", snapshot.xp());
        yaml.set("selected-herb", snapshot.selectedHerb().name());
        yaml.set("dark-unlocked", snapshot.darkUnlockedPlots());
        for (PlotState plot : snapshot.plots()) {
            String path = "plots." + plot.herb().name() + "." + plot.index();
            yaml.set(path + ".ready-at", plot.readyAtMillis());
            yaml.set(path + ".fertilized", plot.fertilizerUses());
        }
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Could not create directory " + directory);
            }
            yaml.save(fileOf(snapshot.uuid()));
        } catch (IOException failure) {
            throw new StorageException("Could not write profile file for " + snapshot.uuid(), failure);
        }
    }

    private File fileOf(UUID uuid) {
        return new File(directory, uuid + ".yml");
    }

    private static int plotIndex(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}

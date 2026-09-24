package dev.herbio.config;

import dev.herbio.herb.HerbType;
import dev.herbio.storage.StorageType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Immutable snapshot of config.yml. Reloading the plugin builds a new instance. */
public final class HerbioConfig {

    private final StorageType storageType;
    private final DatabaseSettings database;
    private final long autosaveIntervalSeconds;
    private final String guiTitle;
    private final String scrollTitle;
    private final long guiRefreshTicks;
    private final Map<HerbType, Long> growthMillis;
    private final Map<HerbType, Integer> harvestYield;
    private final Map<HerbType, Long> xpReward;
    private final int fertilizerReductionPercent;
    private final int fertilizerMaxUses;
    private final int vipGrowthReductionPercent;
    private final long baseXp;
    private final double xpGrowth;

    private HerbioConfig(FileConfiguration configuration) {
        this.storageType = StorageType.byId(configuration.getString("storage",
                configuration.getBoolean("database.enabled", false) ? "mysql" : "yaml"));
        this.database = DatabaseSettings.load(requireSection(configuration, "database"));
        this.autosaveIntervalSeconds = Math.max(30L, configuration.getLong("database.autosave-interval", 300L));
        this.guiTitle = configuration.getString("gui.title", "<dark_green>Herbio <dark_gray>| <white><herb>");
        this.scrollTitle = configuration.getString("gui.scroll-title", "<dark_green>Gardening permit");
        this.guiRefreshTicks = Math.max(1L, configuration.getLong("gui.refresh-ticks", 20L));
        this.growthMillis = readPerHerb(configuration, "growth-seconds", 300L, seconds -> TimeUnit.SECONDS.toMillis(Math.max(1L, seconds)));
        this.harvestYield = readPerHerb(configuration, "harvest-yield", 1L, value -> (int) Math.max(1L, value));
        this.xpReward = readPerHerb(configuration, "xp-reward", 5L, value -> Math.max(0L, value));
        this.fertilizerReductionPercent = Math.max(1, Math.min(100, configuration.getInt("fertilizer.reduction-percent", 50)));
        this.fertilizerMaxUses = Math.max(0, configuration.getInt("fertilizer.max-per-plot", 4));
        this.vipGrowthReductionPercent = Math.max(0, Math.min(99, configuration.getInt("vip.growth-reduction-percent", 50)));
        this.baseXp = Math.max(1L, configuration.getLong("level.base-xp", 100L));
        this.xpGrowth = Math.max(1.0D, configuration.getDouble("level.growth", 1.35D));
    }

    public static HerbioConfig load(FileConfiguration configuration) {
        return new HerbioConfig(configuration);
    }

    private static ConfigurationSection requireSection(FileConfiguration configuration, String path) {
        ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            section = configuration.createSection(path);
        }
        return section;
    }

    private static <T> Map<HerbType, T> readPerHerb(FileConfiguration configuration,
                                                    String path,
                                                    long fallback,
                                                    java.util.function.LongFunction<T> mapper) {
        Map<HerbType, T> values = new EnumMap<>(HerbType.class);
        for (HerbType herb : HerbType.values()) {
            values.put(herb, mapper.apply(configuration.getLong(path + "." + herb.id(), fallback)));
        }
        return Map.copyOf(values);
    }

    public StorageType storageType() {
        return storageType;
    }

    public DatabaseSettings database() {
        return database;
    }

    public long autosaveIntervalSeconds() {
        return autosaveIntervalSeconds;
    }

    public String guiTitle() {
        return guiTitle;
    }

    public String scrollTitle() {
        return scrollTitle;
    }

    public long guiRefreshTicks() {
        return guiRefreshTicks;
    }

    public long growthMillis(HerbType herb) {
        return growthMillis.get(herb);
    }

    public int harvestYield(HerbType herb) {
        return harvestYield.get(herb);
    }

    public long xpReward(HerbType herb) {
        return xpReward.get(herb);
    }

    public int fertilizerMaxUses() {
        return fertilizerMaxUses;
    }

    /** Growth time a {@code herbio.vip} player gets on top of {@link #growthMillis(HerbType)}. */
    public long growthMillis(HerbType herb, boolean vip) {
        long base = growthMillis(herb);
        return vip ? Math.max(1L, base * (100L - vipGrowthReductionPercent) / 100L) : base;
    }

    public int vipGrowthReductionPercent() {
        return vipGrowthReductionPercent;
    }

    public int fertilizerReductionPercent() {
        return fertilizerReductionPercent;
    }

    public long baseXp() {
        return baseXp;
    }

    public double xpGrowth() {
        return xpGrowth;
    }
}

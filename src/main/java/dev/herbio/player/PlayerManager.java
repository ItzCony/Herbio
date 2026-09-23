package dev.herbio.player;

import dev.herbio.config.HerbioConfig;
import dev.herbio.storage.PlayerRepository;
import dev.herbio.storage.StorageException;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * In-memory cache of loaded profiles. Profiles are only published to the cache once storage
 * has answered, so no interaction can ever run against a half-loaded garden.
 */
public final class PlayerManager {

    private final Plugin plugin;
    private final HerbioConfig config;
    private final PlayerRepository repository;
    private final Map<UUID, HerbioProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> loading = new ConcurrentHashMap<>();

    private @Nullable ScheduledTask autosaveTask;

    public PlayerManager(Plugin plugin, HerbioConfig config, PlayerRepository repository) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
    }

    public @Nullable HerbioProfile get(UUID uuid) {
        return profiles.get(uuid);
    }

    /** Loads a profile off the main threads; a second call while loading is ignored. */
    public void load(UUID uuid) {
        if (profiles.containsKey(uuid) || loading.putIfAbsent(uuid, Boolean.TRUE) != null) {
            return;
        }
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            HerbioProfile profile = new HerbioProfile(uuid);
            try {
                ProfileSnapshot snapshot = repository.load(uuid);
                if (snapshot != null) {
                    profile.apply(snapshot);
                }
            } catch (StorageException failure) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load Herbio profile " + uuid, failure);
            } finally {
                loading.remove(uuid);
            }
            profiles.put(uuid, profile);
        });
    }

    /** Saves and evicts a profile; used on quit. */
    public void unload(UUID uuid) {
        loading.remove(uuid);
        HerbioProfile profile = profiles.remove(uuid);
        if (profile == null) {
            return;
        }
        ProfileSnapshot snapshot = profile.snapshot();
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> write(snapshot));
    }

    public void startAutosave() {
        long interval = config.autosaveIntervalSeconds();
        this.autosaveTask = plugin.getServer().getAsyncScheduler()
                .runAtFixedRate(plugin, task -> saveDirty(), interval, interval, TimeUnit.SECONDS);
    }

    private void saveDirty() {
        for (HerbioProfile profile : profiles.values()) {
            if (profile.isDirty()) {
                profile.markClean();
                write(profile.snapshot());
            }
        }
    }

    /** Flushes everything on the calling thread; used during shutdown. */
    public void shutdown() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        for (HerbioProfile profile : profiles.values()) {
            write(profile.snapshot());
        }
        profiles.clear();
        loading.clear();
    }

    private void write(ProfileSnapshot snapshot) {
        try {
            repository.save(snapshot);
        } catch (StorageException failure) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save Herbio profile " + snapshot.uuid(), failure);
        }
    }
}

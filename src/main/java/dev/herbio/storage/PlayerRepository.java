package dev.herbio.storage;

import dev.herbio.player.ProfileSnapshot;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Profile persistence; every call is made from an async thread and may block. */
public interface PlayerRepository {

    /** @return the stored snapshot, or {@code null} for a player that has never played */
    @Nullable ProfileSnapshot load(UUID uuid) throws StorageException;

    void save(ProfileSnapshot snapshot) throws StorageException;
}

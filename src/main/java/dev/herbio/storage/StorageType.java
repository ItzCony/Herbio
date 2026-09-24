package dev.herbio.storage;

import java.util.Locale;

/** Where profiles are persisted; chosen by the {@code storage} config key. */
public enum StorageType {

    MYSQL,
    SQLITE,
    YAML;

    /** Unknown values fall back to {@code YAML} so a typo never disables the plugin. */
    public static StorageType byId(String raw) {
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return YAML;
        }
    }
}

package dev.herbio.herb;

import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The four herb varieties. Each one is unlocked by a herbalism level and owns the
 * stained glass colour used for its plots and for its GUI selector button.
 */
public enum HerbType {

    GREEN("green", "Green Herb", "<green>", 0, Material.GREEN_STAINED_GLASS_PANE),
    BLUE("blue", "Blue Herb", "<aqua>", LevelScale.TIER_M_START, Material.BLUE_STAINED_GLASS_PANE),
    PURPLE("purple", "Purple Herb", "<red>", LevelScale.TIER_G_START, Material.RED_STAINED_GLASS_PANE),
    DARK("dark", "Dark Herb", "<dark_gray>", LevelScale.MAX_INDEX, Material.BLACK_STAINED_GLASS_PANE);

    /** Order of the selector buttons in the right GUI column, top to bottom. */
    public static final HerbType[] SELECTOR_ORDER = {GREEN, BLUE, PURPLE, DARK};

    private final String id;
    private final String displayName;
    private final String colorTag;
    private final int minLevelIndex;
    private final Material paneMaterial;

    HerbType(String id, String displayName, String colorTag, int minLevelIndex, Material paneMaterial) {
        this.id = id;
        this.displayName = displayName;
        this.colorTag = colorTag;
        this.minLevelIndex = minLevelIndex;
        this.paneMaterial = paneMaterial;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** MiniMessage open tag matching the herb colour, e.g. {@code <green>}. */
    public String colorTag() {
        return colorTag;
    }

    /** Herb name already wrapped in its colour tag. */
    public String coloredName() {
        return colorTag + displayName + "</" + colorTag.substring(1);
    }

    public int minLevelIndex() {
        return minLevelIndex;
    }

    public Material paneMaterial() {
        return paneMaterial;
    }

    public boolean isUnlockedAt(int levelIndex) {
        return levelIndex >= minLevelIndex;
    }

    public static @Nullable HerbType byId(String raw) {
        for (HerbType type : values()) {
            if (type.id.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return null;
    }

    /** Lenient lookup used when reading persisted values. */
    public static HerbType byIdOrDefault(@Nullable String raw, HerbType fallback) {
        if (raw == null) {
            return fallback;
        }
        HerbType byId = byId(raw);
        if (byId != null) {
            return byId;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}

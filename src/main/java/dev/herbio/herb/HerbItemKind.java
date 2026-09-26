package dev.herbio.herb;

import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

/** The item families handed out by {@code /herb give}. */
public enum HerbItemKind {

    SEED("seed", "Seed", Material.WHEAT_SEEDS),
    HERB("herb", "Herb", Material.FERN),
    FERTILIZER("fert", "Fertilizer", Material.BONE_MEAL),
    /** Herb-less: one permit works for every herb the player has unlocked. */
    SCROLL("scroll", "Gardening Permit", Material.BORDURE_INDENTED_BANNER_PATTERN);

    private final String id;
    private final String displayName;
    private final Material material;

    HerbItemKind(String id, String displayName, Material material) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }

    public static @Nullable HerbItemKind byId(String raw) {
        for (HerbItemKind kind : values()) {
            if (kind.id.equalsIgnoreCase(raw) || kind.name().equalsIgnoreCase(raw)) {
                return kind;
            }
        }
        return null;
    }
}

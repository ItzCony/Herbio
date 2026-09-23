package dev.herbio.herb;

import java.util.Locale;

/**
 * Herbalism ranks are stored as a flat index and rendered as the server's rank labels:
 * {@code 0..10}, then {@code M1..M10}, then {@code G1..G10}, then the final rank {@code P}.
 */
public final class LevelScale {

    public static final int TIER_M_START = 11;
    public static final int TIER_G_START = 21;
    public static final int MAX_INDEX = 31;

    private LevelScale() {
    }

    public static String display(int levelIndex) {
        int index = clamp(levelIndex);
        if (index < TIER_M_START) {
            return Integer.toString(index);
        }
        if (index < TIER_G_START) {
            return "M" + (index - TIER_M_START + 1);
        }
        if (index < MAX_INDEX) {
            return "G" + (index - TIER_G_START + 1);
        }
        return "P";
    }

    /** Inverse of {@link #display(int)}; {@code -1} when the label is not a rank. */
    public static int parse(String label) {
        String raw = label.trim().toUpperCase(Locale.ROOT);
        if (raw.equals("P")) {
            return MAX_INDEX;
        }
        try {
            if (raw.startsWith("M")) {
                int rank = Integer.parseInt(raw.substring(1));
                return rank >= 1 && rank <= 10 ? TIER_M_START + rank - 1 : -1;
            }
            if (raw.startsWith("G")) {
                int rank = Integer.parseInt(raw.substring(1));
                return rank >= 1 && rank <= 10 ? TIER_G_START + rank - 1 : -1;
            }
            int rank = Integer.parseInt(raw);
            return rank >= 0 && rank < TIER_M_START ? rank : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static int clamp(int levelIndex) {
        return Math.max(0, Math.min(MAX_INDEX, levelIndex));
    }

    public static boolean isMax(int levelIndex) {
        return levelIndex >= MAX_INDEX;
    }

    /** Experience required to advance from {@code levelIndex} to the next rank. */
    public static long requiredXp(int levelIndex, long baseXp, double growth) {
        if (isMax(levelIndex)) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, Math.round(baseXp * Math.pow(growth, clamp(levelIndex))));
    }
}

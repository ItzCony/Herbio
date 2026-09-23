package dev.herbio.garden;

import dev.herbio.herb.HerbType;
import org.jspecify.annotations.Nullable;

/** A single 1x1 tile of the virtual garden. Growth is purely time based. */
public final class Plot {

    private @Nullable HerbType herb;
    private long readyAtMillis;
    private int fertilizerUses;

    public boolean isEmpty() {
        return herb == null;
    }

    public boolean isReady() {
        return herb != null && System.currentTimeMillis() >= readyAtMillis;
    }

    public boolean isGrowing() {
        return herb != null && !isReady();
    }

    public @Nullable HerbType herb() {
        return herb;
    }

    public long readyAtMillis() {
        return readyAtMillis;
    }

    /** How many fertilizers were already used on the currently planted herb. */
    public int fertilizerUses() {
        return fertilizerUses;
    }

    public long remainingMillis() {
        return herb == null ? 0L : Math.max(0L, readyAtMillis - System.currentTimeMillis());
    }

    public void plant(HerbType herb, long growthMillis) {
        this.herb = herb;
        this.readyAtMillis = System.currentTimeMillis() + growthMillis;
        this.fertilizerUses = 0;
    }

    /** Restores persisted state without recomputing the timer. */
    public void restore(HerbType herb, long readyAtMillis, int fertilizerUses) {
        this.herb = herb;
        this.readyAtMillis = readyAtMillis;
        this.fertilizerUses = Math.max(0, fertilizerUses);
    }

    /** @return {@code true} when another fertilizer may still be used on this plot */
    public boolean canFertilize(int maxUses) {
        return isGrowing() && fertilizerUses < maxUses;
    }

    /**
     * Cuts the remaining growth time.
     *
     * @return {@code true} when the plot was growing and therefore actually fertilized
     */
    public boolean fertilize(int reductionPercent, int maxUses) {
        if (!canFertilize(maxUses)) {
            return false;
        }
        readyAtMillis -= remainingMillis() * reductionPercent / 100L;
        fertilizerUses++;
        return true;
    }

    public @Nullable HerbType clear() {
        HerbType previous = herb;
        herb = null;
        readyAtMillis = 0L;
        fertilizerUses = 0;
        return previous;
    }
}

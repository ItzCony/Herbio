package dev.herbio.player;

import dev.herbio.garden.Garden;
import dev.herbio.garden.PlotState;
import dev.herbio.herb.HerbType;
import dev.herbio.herb.LevelScale;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per player herbalism progress plus one garden per herb type. */
public final class HerbioProfile {

    private final UUID uuid;
    private final Map<HerbType, Garden> gardens = new EnumMap<>(HerbType.class);

    private volatile int levelIndex;
    private volatile long xp;
    private volatile HerbType selectedHerb = HerbType.GREEN;
    private volatile long darkUnlockedPlots;
    private volatile boolean dirty;

    public HerbioProfile(UUID uuid) {
        this.uuid = uuid;
        for (HerbType herb : HerbType.values()) {
            gardens.put(herb, new Garden());
        }
    }

    public UUID uuid() {
        return uuid;
    }

    /** Each herb type owns its own 6x6 field. */
    public Garden garden(HerbType field) {
        return gardens.get(field);
    }

    public int levelIndex() {
        return levelIndex;
    }

    public String levelDisplay() {
        return LevelScale.display(levelIndex);
    }

    public long xp() {
        return xp;
    }

    public HerbType selectedHerb() {
        return selectedHerb;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    /** @return {@code true} when this dark field plot has already been bought */
    public boolean isDarkPlotBought(int plotIndex) {
        return (darkUnlockedPlots & (1L << plotIndex)) != 0L;
    }

    public void buyDarkPlot(int plotIndex) {
        this.darkUnlockedPlots |= 1L << plotIndex;
        markDirty();
    }

    public long darkUnlockedPlots() {
        return darkUnlockedPlots;
    }

    /** Admin override: jumps to a rank and clears the progress into the next one. */
    public void setLevelIndex(int index) {
        this.levelIndex = LevelScale.clamp(index);
        this.xp = 0L;
        markDirty();
    }

    public void selectHerb(HerbType herb) {
        this.selectedHerb = herb;
        markDirty();
    }

    /**
     * Adds experience and consumes it into rank ups.
     *
     * @return how many ranks were gained
     */
    public int addXp(long amount, long baseXp, double growth) {
        if (amount <= 0L) {
            return 0;
        }
        int gained = 0;
        xp += amount;
        while (!LevelScale.isMax(levelIndex)) {
            long required = LevelScale.requiredXp(levelIndex, baseXp, growth);
            if (xp < required) {
                break;
            }
            xp -= required;
            levelIndex++;
            gained++;
        }
        if (LevelScale.isMax(levelIndex)) {
            xp = 0L;
        }
        markDirty();
        return gained;
    }

    /** Experience still needed for the next rank, or {@code -1} at the highest rank. */
    public long xpToNextLevel(long baseXp, double growth) {
        if (LevelScale.isMax(levelIndex)) {
            return -1L;
        }
        return Math.max(0L, LevelScale.requiredXp(levelIndex, baseXp, growth) - xp);
    }

    public synchronized ProfileSnapshot snapshot() {
        List<PlotState> plots = new ArrayList<>();
        for (Garden garden : gardens.values()) {
            plots.addAll(garden.snapshot());
        }
        return new ProfileSnapshot(uuid, levelIndex, xp, selectedHerb, darkUnlockedPlots, List.copyOf(plots));
    }

    public synchronized void apply(ProfileSnapshot snapshot) {
        this.levelIndex = LevelScale.clamp(snapshot.levelIndex());
        this.xp = Math.max(0L, snapshot.xp());
        this.selectedHerb = snapshot.selectedHerb();
        this.darkUnlockedPlots = snapshot.darkUnlockedPlots();
        Map<HerbType, List<PlotState>> byField = new EnumMap<>(HerbType.class);
        for (HerbType herb : HerbType.values()) {
            byField.put(herb, new ArrayList<>());
        }
        for (PlotState state : snapshot.plots()) {
            byField.get(state.herb()).add(state);
        }
        gardens.forEach((field, garden) -> garden.restore(byField.get(field)));
        markClean();
    }
}

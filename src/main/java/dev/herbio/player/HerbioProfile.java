package dev.herbio.player;

import dev.herbio.garden.Garden;
import dev.herbio.herb.HerbType;
import dev.herbio.herb.LevelScale;

import java.util.UUID;

/** Per player herbalism progress plus their garden. */
public final class HerbioProfile {

    private final UUID uuid;
    private final Garden garden = new Garden();

    private volatile int levelIndex;
    private volatile long xp;
    private volatile HerbType selectedHerb = HerbType.GREEN;
    private volatile boolean dirty;

    public HerbioProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public Garden garden() {
        return garden;
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
        return new ProfileSnapshot(uuid, levelIndex, xp, selectedHerb, garden.snapshot());
    }

    public synchronized void apply(ProfileSnapshot snapshot) {
        this.levelIndex = LevelScale.clamp(snapshot.levelIndex());
        this.xp = Math.max(0L, snapshot.xp());
        this.selectedHerb = snapshot.selectedHerb();
        this.garden.restore(snapshot.plots());
        markClean();
    }
}

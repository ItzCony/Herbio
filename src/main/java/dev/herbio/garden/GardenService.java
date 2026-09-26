package dev.herbio.garden;

import dev.herbio.config.HerbioConfig;
import dev.herbio.herb.HerbItemKind;
import dev.herbio.herb.HerbItems;
import dev.herbio.herb.HerbType;
import dev.herbio.player.HerbioProfile;
import dev.herbio.util.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * All planting, harvesting and fertilizing rules: game rules, item costs, plot locks and the
 * VIP growth bonus. Every action works on the field of the herb the player currently has picked.
 * Access permissions (who may open what) stay in the GUI layer.
 */
public final class GardenService {

    private final HerbioConfig config;
    private final HerbItems items;

    public GardenService(HerbioConfig config, HerbItems items) {
        this.config = config;
        this.items = items;
    }

    /**
     * Dark field plots are bought with money, every other field opens its last plots rank by rank.
     */
    public boolean isPlotUnlocked(HerbioProfile profile, HerbType field, int plotIndex) {
        if (field == HerbType.DARK) {
            return plotIndex < Garden.SIZE - config.darkLockedPlots() || profile.isDarkPlotBought(plotIndex);
        }
        return plotIndex < Garden.SIZE - rankLockedPlots(field, profile.levelIndex());
    }

    /**
     * Plots still closed by rank. A field starts with {@code progressive.locked-plots} closed and
     * opens one more with every rank above the one that unlocked the field, so the field is full
     * once the player leaves that tier.
     *
     * <p>Green is the starter field and always full; dark has no rank above {@code P} and is
     * money-gated instead.</p>
     */
    public int rankLockedPlots(HerbType field, int levelIndex) {
        if (field == HerbType.GREEN || field == HerbType.DARK) {
            return 0;
        }
        int ranksAboveUnlock = Math.max(0, levelIndex - field.minLevelIndex());
        return Math.max(0, config.progressiveLockedPlots() - ranksAboveUnlock);
    }

    /** Rank at which a rank-locked plot opens; only meaningful for green, blue and purple. */
    public int rankUnlockLevelIndex(HerbType field, int plotIndex) {
        return field.minLevelIndex() + config.progressiveLockedPlots() - (Garden.SIZE - 1 - plotIndex);
    }

    public GardenResult plant(Player player, HerbioProfile profile, int plotIndex) {
        HerbType herb = profile.selectedHerb();
        Plot plot = profile.garden(herb).plot(plotIndex);
        if (!plot.isEmpty()) {
            return GardenResult.failure(ActionOutcome.PLOT_OCCUPIED);
        }
        if (!herb.isUnlockedAt(profile.levelIndex())) {
            return GardenResult.failure(ActionOutcome.HERB_LOCKED);
        }
        if (!isPlotUnlocked(profile, herb, plotIndex)) {
            return GardenResult.failure(ActionOutcome.PLOT_LOCKED);
        }
        if (items.remove(player.getInventory(), HerbItemKind.SEED, herb, 1) < 1) {
            return GardenResult.failure(ActionOutcome.NO_SEEDS);
        }
        plot.plant(herb, growthMillis(player, herb));
        profile.markDirty();
        return GardenResult.success(1);
    }

    public GardenResult harvest(Player player, HerbioProfile profile, int plotIndex) {
        Plot plot = profile.garden(profile.selectedHerb()).plot(plotIndex);
        if (plot.isEmpty()) {
            return GardenResult.failure(ActionOutcome.PLOT_EMPTY);
        }
        if (!plot.isReady()) {
            return GardenResult.failure(ActionOutcome.PLOT_NOT_READY);
        }
        HerbType herb = plot.clear();
        profile.markDirty();
        items.give(player, HerbItemKind.HERB, herb, config.harvestYield(herb));
        long xp = config.xpReward(herb);
        int levels = profile.addXp(xp, config.baseXp(), config.xpGrowth());
        return new GardenResult(ActionOutcome.SUCCESS, 1, xp, levels);
    }

    /** Gardening permit: matures and harvests every plot of one field. */
    public GardenResult harvestInstant(Player player, HerbioProfile profile, HerbType field) {
        Garden garden = profile.garden(field);
        int harvested = 0;
        long xp = 0L;
        for (int index = 0; index < Garden.SIZE; index++) {
            Plot plot = garden.plot(index);
            if (plot.isEmpty()) {
                continue;
            }
            HerbType herb = plot.clear();
            items.give(player, HerbItemKind.HERB, herb, config.harvestYield(herb));
            xp += config.xpReward(herb);
            harvested++;
        }
        if (harvested == 0) {
            return GardenResult.success(0);
        }
        profile.markDirty();
        int levels = profile.addXp(xp, config.baseXp(), config.xpGrowth());
        return new GardenResult(ActionOutcome.SUCCESS, harvested, xp, levels);
    }

    public GardenResult fertilize(Player player, HerbioProfile profile, int plotIndex) {
        Plot plot = profile.garden(profile.selectedHerb()).plot(plotIndex);
        HerbType herb = plot.herb();
        if (herb == null || !plot.isGrowing()) {
            return GardenResult.failure(ActionOutcome.PLOT_EMPTY);
        }
        if (!plot.canFertilize(config.fertilizerMaxUses())) {
            return GardenResult.failure(ActionOutcome.FERTILIZER_LIMIT);
        }
        if (items.remove(player.getInventory(), HerbItemKind.FERTILIZER, herb, 1) < 1) {
            return GardenResult.failure(ActionOutcome.NO_FERTILIZER);
        }
        plot.fertilize(config.fertilizerReductionPercent(), config.fertilizerMaxUses());
        profile.markDirty();
        return GardenResult.success(1);
    }

    public GardenResult plantAll(Player player, HerbioProfile profile) {
        HerbType herb = profile.selectedHerb();
        if (!herb.isUnlockedAt(profile.levelIndex())) {
            return GardenResult.failure(ActionOutcome.HERB_LOCKED);
        }
        Garden garden = profile.garden(herb);
        Inventory inventory = player.getInventory();
        int empty = countPlantablePlots(profile, herb);
        if (empty == 0) {
            return GardenResult.success(0);
        }
        int seeds = items.count(inventory, HerbItemKind.SEED, herb);
        if (seeds == 0) {
            return GardenResult.failure(ActionOutcome.NO_SEEDS);
        }
        int taken = items.remove(inventory, HerbItemKind.SEED, herb, Math.min(empty, seeds));
        int planted = 0;
        for (int index = 0; index < Garden.SIZE && planted < taken; index++) {
            Plot plot = garden.plot(index);
            if (plot.isEmpty() && isPlotUnlocked(profile, herb, index)) {
                plot.plant(herb, growthMillis(player, herb));
                planted++;
            }
        }
        profile.markDirty();
        return GardenResult.success(planted);
    }

    public GardenResult harvestAll(Player player, HerbioProfile profile) {
        Garden garden = profile.garden(profile.selectedHerb());
        int harvested = 0;
        long xp = 0L;
        for (int index = 0; index < Garden.SIZE; index++) {
            Plot plot = garden.plot(index);
            if (!plot.isReady()) {
                continue;
            }
            HerbType herb = plot.clear();
            items.give(player, HerbItemKind.HERB, herb, config.harvestYield(herb));
            xp += config.xpReward(herb);
            harvested++;
        }
        if (harvested == 0) {
            return GardenResult.success(0);
        }
        profile.markDirty();
        int levels = profile.addXp(xp, config.baseXp(), config.xpGrowth());
        return new GardenResult(ActionOutcome.SUCCESS, harvested, xp, levels);
    }

    public GardenResult fertilizeAll(Player player, HerbioProfile profile) {
        Garden garden = profile.garden(profile.selectedHerb());
        Inventory inventory = player.getInventory();
        int fertilized = 0;
        boolean missingFertilizer = false;
        boolean limitReached = false;
        for (int index = 0; index < Garden.SIZE; index++) {
            Plot plot = garden.plot(index);
            HerbType herb = plot.herb();
            if (herb == null || !plot.isGrowing()) {
                continue;
            }
            if (!plot.canFertilize(config.fertilizerMaxUses())) {
                limitReached = true;
                continue;
            }
            if (items.remove(inventory, HerbItemKind.FERTILIZER, herb, 1) < 1) {
                missingFertilizer = true;
                continue;
            }
            plot.fertilize(config.fertilizerReductionPercent(), config.fertilizerMaxUses());
            fertilized++;
        }
        if (fertilized == 0) {
            if (missingFertilizer) {
                return GardenResult.failure(ActionOutcome.NO_FERTILIZER);
            }
            return GardenResult.failure(limitReached ? ActionOutcome.FERTILIZER_LIMIT : ActionOutcome.PLOT_EMPTY);
        }
        profile.markDirty();
        return GardenResult.success(fertilized);
    }

    /** VIP players grow everything faster; the bonus is applied once, when the seed is planted. */
    private long growthMillis(Player player, HerbType herb) {
        return config.growthMillis(herb, player.hasPermission(Permissions.VIP));
    }

    private int countPlantablePlots(HerbioProfile profile, HerbType field) {
        Garden garden = profile.garden(field);
        int empty = 0;
        for (int index = 0; index < Garden.SIZE; index++) {
            if (garden.plot(index).isEmpty() && isPlotUnlocked(profile, field, index)) {
                empty++;
            }
        }
        return empty;
    }
}

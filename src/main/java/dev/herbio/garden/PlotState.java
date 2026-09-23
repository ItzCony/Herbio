package dev.herbio.garden;

import dev.herbio.herb.HerbType;

/** Persistable state of one planted plot. Empty plots are simply not stored. */
public record PlotState(int index, HerbType herb, long readyAtMillis, int fertilizerUses) {
}

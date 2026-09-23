package dev.herbio.player;

import dev.herbio.garden.PlotState;
import dev.herbio.herb.HerbType;

import java.util.List;
import java.util.UUID;

/** Consistent copy of a profile, safe to hand to the asynchronous storage layer. */
public record ProfileSnapshot(UUID uuid,
                              int levelIndex,
                              long xp,
                              HerbType selectedHerb,
                              List<PlotState> plots) {
}

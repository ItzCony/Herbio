package dev.herbio.player;

import dev.herbio.garden.PlotState;
import dev.herbio.herb.HerbType;

import java.util.List;
import java.util.UUID;

/**
 * Consistent copy of a profile, safe to hand to the asynchronous storage layer.
 *
 * @param darkUnlockedPlots bit set of the dark field plots the player has bought
 * @param plots             planted plots of every field; {@link PlotState#herb()} names the field
 */
public record ProfileSnapshot(UUID uuid,
                              int levelIndex,
                              long xp,
                              HerbType selectedHerb,
                              long darkUnlockedPlots,
                              List<PlotState> plots) {
}

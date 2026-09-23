package dev.herbio.garden;

import dev.herbio.herb.HerbType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The player's 6x6 field. Mutated from the owning player's region thread and read by the
 * asynchronous save task, so every access is synchronized on the garden itself.
 */
public final class Garden {

    public static final int ROWS = 6;
    public static final int COLUMNS = 6;
    public static final int SIZE = ROWS * COLUMNS;

    private final Plot[] plots = new Plot[SIZE];

    public Garden() {
        for (int index = 0; index < SIZE; index++) {
            plots[index] = new Plot();
        }
    }

    public synchronized Plot plot(int index) {
        if (index < 0 || index >= SIZE) {
            throw new IndexOutOfBoundsException("Plot index out of range: " + index);
        }
        return plots[index];
    }

    public synchronized List<PlotState> snapshot() {
        List<PlotState> states = new ArrayList<>();
        for (int index = 0; index < SIZE; index++) {
            Plot plot = plots[index];
            HerbType herb = plot.herb();
            if (herb != null) {
                states.add(new PlotState(index, herb, plot.readyAtMillis(), plot.fertilizerUses()));
            }
        }
        return states;
    }

    public synchronized void restore(Collection<PlotState> states) {
        for (Plot plot : plots) {
            plot.clear();
        }
        for (PlotState state : states) {
            if (state.index() >= 0 && state.index() < SIZE) {
                plots[state.index()].restore(state.herb(), state.readyAtMillis(), state.fertilizerUses());
            }
        }
    }
}

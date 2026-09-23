package dev.herbio.garden;

/**
 * Outcome of a garden action.
 *
 * @param outcome      why the action failed, or {@link ActionOutcome#SUCCESS}
 * @param count        plots affected
 * @param xp           experience granted
 * @param levelsGained ranks gained by the granted experience
 */
public record GardenResult(ActionOutcome outcome, int count, long xp, int levelsGained) {

    public static GardenResult failure(ActionOutcome outcome) {
        return new GardenResult(outcome, 0, 0L, 0);
    }

    public static GardenResult success(int count) {
        return new GardenResult(ActionOutcome.SUCCESS, count, 0L, 0);
    }

    public boolean isSuccess() {
        return outcome.isSuccess() && count > 0;
    }
}

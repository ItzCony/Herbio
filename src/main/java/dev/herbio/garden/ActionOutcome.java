package dev.herbio.garden;

import org.jspecify.annotations.Nullable;

/** Result of a single plot interaction; the message key is {@code null} on success. */
public enum ActionOutcome {

    SUCCESS(null),
    PLOT_OCCUPIED("plot-occupied"),
    PLOT_NOT_READY("plot-not-ready"),
    PLOT_EMPTY(null),
    HERB_LOCKED("herb-locked"),
    NO_SEEDS("no-seeds"),
    NO_FERTILIZER("no-fertilizer"),
    FERTILIZER_LIMIT("fertilizer-limit"),
    PLOT_LOCKED("plot-locked");

    private final @Nullable String messageKey;

    ActionOutcome(@Nullable String messageKey) {
        this.messageKey = messageKey;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public @Nullable String messageKey() {
        return messageKey;
    }
}

package dev.herbio.gui;

import dev.herbio.garden.Garden;
import dev.herbio.herb.HerbType;

/**
 * Slot map of the 6x9 chest GUI.
 *
 * <pre>
 * # . . . . . . # H      #  border filler        H  fertilize-all hoe
 * # . . . . . . # 1      .  6x6 garden plot      1..4 herb selectors
 * # . . . . . . # 2      L  herbalism level book
 * L . . . . . . # 3
 * # . . . . . . # 4
 * # . . . . . . # B      B  bulk plant/harvest hoe
 * </pre>
 */
public final class GuiLayout {

    public static final int COLUMNS = 9;
    public static final int SIZE = COLUMNS * Garden.ROWS;

    public static final int FIRST_PLOT_COLUMN = 1;
    public static final int LAST_PLOT_COLUMN = FIRST_PLOT_COLUMN + Garden.COLUMNS - 1;

    public static final int FERTILIZE_ALL_SLOT = 8;
    public static final int BULK_HOE_SLOT = 53;
    /** Left border slot that carries the herbalism progress book instead of a filler pane. */
    public static final int LEVEL_BOOK_SLOT = 27;
    public static final int[] SELECTOR_SLOTS = {17, 26, 35, 44};

    public static final int[] BORDER_SLOTS = borderSlots();

    private GuiLayout() {
    }

    /** @return the garden index for a GUI slot, or {@code -1} when the slot is not a plot */
    public static int plotIndex(int slot) {
        if (slot < 0 || slot >= SIZE) {
            return -1;
        }
        int column = slot % COLUMNS;
        if (column < FIRST_PLOT_COLUMN || column > LAST_PLOT_COLUMN) {
            return -1;
        }
        return (slot / COLUMNS) * Garden.COLUMNS + (column - FIRST_PLOT_COLUMN);
    }

    public static int plotSlot(int plotIndex) {
        int row = plotIndex / Garden.COLUMNS;
        int column = plotIndex % Garden.COLUMNS;
        return row * COLUMNS + FIRST_PLOT_COLUMN + column;
    }

    /** @return the herb bound to a selector slot, or {@code null} when the slot is not a selector */
    public static HerbType selectorHerb(int slot) {
        for (int index = 0; index < SELECTOR_SLOTS.length; index++) {
            if (SELECTOR_SLOTS[index] == slot) {
                return HerbType.SELECTOR_ORDER[index];
            }
        }
        return null;
    }

    private static int[] borderSlots() {
        int[] slots = new int[Garden.ROWS * 2];
        int cursor = 0;
        for (int row = 0; row < Garden.ROWS; row++) {
            slots[cursor++] = row * COLUMNS;
            slots[cursor++] = row * COLUMNS + LAST_PLOT_COLUMN + 1;
        }
        return slots;
    }
}

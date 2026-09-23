package dev.herbio.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marks an inventory as a Herbio GUI and links it back to its view object. */
public final class HerbGuiHolder implements InventoryHolder {

    private final HerbGui gui;
    private Inventory inventory;

    HerbGuiHolder(HerbGui gui) {
        this.gui = gui;
    }

    public HerbGui gui() {
        return gui;
    }

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

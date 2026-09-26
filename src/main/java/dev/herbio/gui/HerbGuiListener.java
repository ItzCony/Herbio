package dev.herbio.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;

/** Makes the garden GUI read-only and forwards its clicks to {@link GuiManager}. */
public final class HerbGuiListener implements Listener {

    private final GuiManager guis;

    public HerbGuiListener(GuiManager guis) {
        this.guis = guis;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ConfirmGui confirm) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player && event.getClickedInventory() == event.getInventory()) {
                guis.handleConfirmClick(confirm, event.getRawSlot());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof ScrollGui scroll) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player && event.getClickedInventory() == event.getInventory()) {
                guis.handleScrollClick(scroll, event.getRawSlot());
            }
            return;
        }
        HerbGui gui = guiOf(event.getInventory());
        if (gui == null) {
            return;
        }
        // Cancel first: this also blocks shift-clicks and number keys coming from the player inventory.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getClickedInventory() != event.getInventory()) {
            return;
        }
        guis.handleClick(gui, event.getRawSlot(), event.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (guiOf(event.getInventory()) != null
                || event.getInventory().getHolder() instanceof ScrollGui
                || event.getInventory().getHolder() instanceof ConfirmGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HerbGui gui = guiOf(event.getInventory());
        if (gui != null) {
            guis.handleClose(gui);
        }
    }

    private static HerbGui guiOf(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof HerbGuiHolder herbHolder ? herbHolder.gui() : null;
    }
}

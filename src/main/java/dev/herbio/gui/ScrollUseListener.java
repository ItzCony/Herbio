package dev.herbio.gui;

import dev.herbio.herb.HerbItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Opens the permit menu when a player right clicks while holding a gardening permit. */
public final class ScrollUseListener implements Listener {

    private final GuiManager guis;
    private final HerbItems items;

    public ScrollUseListener(GuiManager guis, HerbItems items) {
        this.guis = guis;
        this.items = items;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!items.isScroll(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        guis.openScroll(event.getPlayer());
    }
}

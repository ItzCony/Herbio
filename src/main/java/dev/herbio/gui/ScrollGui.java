package dev.herbio.gui;

import dev.herbio.config.HerbioConfig;
import dev.herbio.garden.Garden;
import dev.herbio.herb.HerbType;
import dev.herbio.player.HerbioProfile;
import dev.herbio.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Menu opened by a gardening permit. It lists the herb types the player has unlocked;
 * picking one harvests that herb across the whole garden at once.
 */
public final class ScrollGui implements InventoryHolder {

    /** One button per herb, in the selector order of the garden view. */
    private static final int[] SLOTS = {1, 3, 5, 7};

    private final Player player;
    private final HerbioProfile profile;
    private final Inventory inventory;

    ScrollGui(HerbioConfig config, Player player, HerbioProfile profile) {
        this.player = player;
        this.profile = profile;
        this.inventory = Bukkit.createInventory(this, 9, Text.of(config.scrollTitle()));
        render();
    }

    public Player player() {
        return player;
    }

    public HerbioProfile profile() {
        return profile;
    }

    void open() {
        player.openInventory(inventory);
    }

    /** @return the herb bound to a slot, or {@code null} when the slot holds no unlocked button */
    @Nullable HerbType herbAt(int slot) {
        for (int index = 0; index < SLOTS.length; index++) {
            if (SLOTS[index] != slot) {
                continue;
            }
            HerbType herb = HerbType.SELECTOR_ORDER[index];
            return herb.isUnlockedAt(profile.levelIndex()) ? herb : null;
        }
        return null;
    }

    private void render() {
        for (int index = 0; index < SLOTS.length; index++) {
            HerbType herb = HerbType.SELECTOR_ORDER[index];
            if (!herb.isUnlockedAt(profile.levelIndex())) {
                continue;
            }
            inventory.setItem(SLOTS[index], HerbGui.item(herb.paneMaterial(),
                    Text.item(herb.coloredName()),
                    List.of(Text.item("<gray>Planted plots: <white><count>",
                                    Placeholder.unparsed("count", Integer.toString(planted(herb)))),
                            Text.item("<gray>Click to harvest all of them at once."))));
        }
    }

    private int planted(HerbType herb) {
        int count = 0;
        for (int index = 0; index < Garden.SIZE; index++) {
            if (profile.garden().plot(index).herb() == herb) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

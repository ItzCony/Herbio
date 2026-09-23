package dev.herbio.herb;

import dev.herbio.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Creates and recognises Herbio items. Identity lives in the persistent data container,
 * so renamed or otherwise similar vanilla items are never accepted as plugin items.
 */
public final class HerbItems {

    private final NamespacedKey kindKey;
    private final NamespacedKey herbKey;

    public HerbItems(Plugin plugin) {
        this.kindKey = new NamespacedKey(plugin, "item_kind");
        this.herbKey = new NamespacedKey(plugin, "herb_type");
    }

    public ItemStack create(HerbItemKind kind, HerbType herb, int amount) {
        ItemStack stack = new ItemStack(kind.material(), Math.max(1, Math.min(kind.material().getMaxStackSize(), amount)));
        stack.editMeta(meta -> {
            meta.displayName(Text.item("<herb> <gray><kind>",
                    Placeholder.parsed("herb", herb.coloredName()),
                    Placeholder.unparsed("kind", kind.displayName())));
            meta.lore(List.of(Text.item("<dark_gray>Herbio")));
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(kindKey, PersistentDataType.STRING, kind.name());
            data.set(herbKey, PersistentDataType.STRING, herb.name());
        });
        return stack;
    }

    /** The gardening permit carries no herb; the player picks one when using it. */
    public ItemStack createScroll() {
        ItemStack stack = new ItemStack(HerbItemKind.SCROLL.material(), 1);
        stack.editMeta(meta -> {
            meta.displayName(Text.item("<gold>Gardening Permit"));
            meta.lore(List.of(
                    Text.item("<gray>Right click to instantly harvest"),
                    Text.item("<gray>one herb type in your garden."),
                    Text.item("<dark_gray>Herbio")));
            meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, HerbItemKind.SCROLL.name());
        });
        return stack;
    }

    public boolean isScroll(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() != HerbItemKind.SCROLL.material() || !stack.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer data = stack.getItemMeta().getPersistentDataContainer();
        return HerbItemKind.SCROLL.name().equals(data.get(kindKey, PersistentDataType.STRING));
    }

    public boolean matches(@Nullable ItemStack stack, HerbItemKind kind, HerbType herb) {
        if (stack == null || stack.getType() != kind.material() || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return kind.name().equals(data.get(kindKey, PersistentDataType.STRING))
                && herb.name().equals(data.get(herbKey, PersistentDataType.STRING));
    }

    public int count(Inventory inventory, HerbItemKind kind, HerbType herb) {
        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (matches(stack, kind, herb)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Removes up to {@code amount} matching items.
     *
     * @return how many were actually removed
     */
    public int remove(Inventory inventory, HerbItemKind kind, HerbType herb, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!matches(stack, kind, herb)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getAmount());
            remaining -= taken;
            if (taken >= stack.getAmount()) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - taken);
                inventory.setItem(slot, stack);
            }
        }
        return amount - remaining;
    }

    /** Adds the stack to the inventory and drops whatever does not fit at the player's feet. */
    public void give(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    /** Gives {@code amount} items, split into full stacks. */
    public void give(Player player, HerbItemKind kind, HerbType herb, int amount) {
        int remaining = amount;
        int stackSize = kind.material().getMaxStackSize();
        while (remaining > 0) {
            int size = Math.min(stackSize, remaining);
            give(player, create(kind, herb, size));
            remaining -= size;
        }
    }
}

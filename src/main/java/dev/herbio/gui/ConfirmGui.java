package dev.herbio.gui;

import dev.herbio.config.HerbioConfig;
import dev.herbio.economy.VaultEconomy;
import dev.herbio.player.HerbioProfile;
import dev.herbio.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/** Buy-or-cancel prompt shown before a locked dark field plot is paid for. */
public final class ConfirmGui implements InventoryHolder {

    static final int CONFIRM_SLOT = 2;
    static final int CANCEL_SLOT = 6;

    private final Player player;
    private final HerbioProfile profile;
    private final int plotIndex;
    private final Inventory inventory;

    ConfirmGui(HerbioConfig config, VaultEconomy economy, Player player, HerbioProfile profile, int plotIndex) {
        this.player = player;
        this.profile = profile;
        this.plotIndex = plotIndex;
        this.inventory = Bukkit.createInventory(this, 9, Text.of(config.confirmTitle()));
        String price = economy.format(config.darkPlotPrice());
        inventory.setItem(CONFIRM_SLOT, HerbGui.item(Material.EMERALD,
                Text.item("<green>Unlock this plot"),
                List.of(Text.item("<gray>Price: <white><price>", Placeholder.unparsed("price", price)),
                        Text.item("<gray>Plot <white>#<index>",
                                Placeholder.unparsed("index", Integer.toString(plotIndex + 1))))));
        inventory.setItem(CANCEL_SLOT, HerbGui.item(Material.BARRIER,
                Text.item("<red>Cancel"),
                List.of(Text.item("<gray>Back to the garden."))));
    }

    public Player player() {
        return player;
    }

    public HerbioProfile profile() {
        return profile;
    }

    public int plotIndex() {
        return plotIndex;
    }

    void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

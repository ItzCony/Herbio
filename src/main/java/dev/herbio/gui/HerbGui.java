package dev.herbio.gui;

import dev.herbio.config.HerbioConfig;
import dev.herbio.garden.Garden;
import dev.herbio.garden.Plot;
import dev.herbio.herb.HerbType;
import dev.herbio.herb.LevelScale;
import dev.herbio.player.HerbioProfile;
import dev.herbio.util.Permissions;
import dev.herbio.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * One open garden view. The inventory belongs to a single player and is only ever
 * touched from that player's region thread.
 */
public final class HerbGui {

    private static final Material EMPTY_PLOT = Material.WHITE_STAINED_GLASS_PANE;
    private static final Material READY_PLOT = Material.LIME_STAINED_GLASS_PANE;
    private static final Material BORDER = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material HOE = Material.WOODEN_HOE;
    private static final Material LEVEL_BOOK = Material.BOOK;
    private static final int PROGRESS_BAR_WIDTH = 20;

    private final HerbioConfig config;
    private final Player player;
    private final HerbioProfile profile;
    private final Inventory inventory;

    HerbGui(HerbioConfig config, Player player, HerbioProfile profile) {
        this.config = config;
        this.player = player;
        this.profile = profile;
        HerbGuiHolder holder = new HerbGuiHolder(this);
        this.inventory = Bukkit.createInventory(holder, GuiLayout.SIZE, title(profile.selectedHerb()));
        holder.bind(inventory);
        renderStatic();
        renderPlots();
    }

    public Player player() {
        return player;
    }

    public HerbioProfile profile() {
        return profile;
    }

    public void open() {
        player.openInventory(inventory);
    }

    private Component title(HerbType herb) {
        return Text.of(config.guiTitle(), Placeholder.parsed("herb", herb.coloredName()));
    }

    /** Border, hoes and selectors; these only change when the selected herb changes. */
    private void renderStatic() {
        ItemStack border = item(BORDER, Text.item("<dark_gray>|"), List.of());
        for (int slot : GuiLayout.BORDER_SLOTS) {
            inventory.setItem(slot, border);
        }

        inventory.setItem(GuiLayout.FERTILIZE_ALL_SLOT, item(HOE,
                Text.item("<yellow>Fertilize everything"),
                List.of(Text.item("<gray>Click to fertilize every growing plot."),
                        Text.item("<gray>Costs one matching fertilizer per plot."))));

        boolean vip = player.hasPermission(Permissions.VIP);
        List<Component> bulkLore = new ArrayList<>();
        bulkLore.add(Text.item("<gray>Left click <dark_gray>- <white>harvest everything"));
        bulkLore.add(Text.item("<gray>Right click <dark_gray>- <white>plant everything"));
        bulkLore.add(Component.empty());
        bulkLore.add(vip
                ? Text.item("<green>Unlocked by your VIP rank.")
                : Text.item("<red>Requires VIP."));
        inventory.setItem(GuiLayout.BULK_HOE_SLOT, item(HOE, Text.item("<gold>Bulk tools"), bulkLore));

        for (int index = 0; index < GuiLayout.SELECTOR_SLOTS.length; index++) {
            HerbType herb = HerbType.SELECTOR_ORDER[index];
            inventory.setItem(GuiLayout.SELECTOR_SLOTS[index], selectorItem(herb));
        }
    }

    private ItemStack selectorItem(HerbType herb) {
        boolean unlocked = herb.isUnlockedAt(profile.levelIndex());
        boolean selected = profile.selectedHerb() == herb;
        List<Component> lore = new ArrayList<>();
        if (unlocked) {
            lore.add(selected
                    ? Text.item("<green>Currently selected.")
                    : Text.item("<gray>Click to plant this herb."));
        } else {
            lore.add(Text.item("<red>Requires herbalism level <white><level>",
                    Placeholder.unparsed("level", LevelScale.display(herb.minLevelIndex()))));
        }
        lore.add(Component.empty());
        lore.add(Text.item("<dark_gray>Growth: <gray><time>",
                Placeholder.unparsed("time", formatDuration(config.growthMillis(herb, vipGrowth())))));
        lore.add(Text.item("<dark_gray>Experience: <gray><xp>",
                Placeholder.unparsed("xp", Long.toString(config.xpReward(herb)))));

        ItemStack stack = item(unlocked ? herb.paneMaterial() : Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                Text.item(herb.coloredName()), lore);
        if (selected) {
            stack.editMeta(meta -> {
                meta.setEnchantmentGlintOverride(true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            });
        }
        return stack;
    }

    /** Plot tiles and the progress book; re-rendered on every refresh tick. */
    public void renderPlots() {
        Garden garden = profile.garden();
        for (int index = 0; index < Garden.SIZE; index++) {
            inventory.setItem(GuiLayout.plotSlot(index), plotItem(garden.plot(index)));
        }
        inventory.setItem(GuiLayout.LEVEL_BOOK_SLOT, levelItem());
    }

    /** Herbalism rank card: current rank plus the experience left for the next one. */
    private ItemStack levelItem() {
        int levelIndex = profile.levelIndex();
        List<Component> lore = new ArrayList<>();
        lore.add(Text.item("<gray>Rank <white><level> <dark_gray>(<gray><index><dark_gray>/<gray><max><dark_gray>)",
                Placeholder.unparsed("level", LevelScale.display(levelIndex)),
                Placeholder.unparsed("index", Integer.toString(levelIndex)),
                Placeholder.unparsed("max", Integer.toString(LevelScale.MAX_INDEX))));
        lore.add(Component.empty());
        if (LevelScale.isMax(levelIndex)) {
            lore.add(Text.item("<gold>Highest herbalism rank reached."));
        } else {
            long required = LevelScale.requiredXp(levelIndex, config.baseXp(), config.xpGrowth());
            long current = Math.min(profile.xp(), required);
            lore.add(Text.item("<gray>Experience <white><xp><dark_gray>/<white><required>",
                    Placeholder.unparsed("xp", Long.toString(current)),
                    Placeholder.unparsed("required", Long.toString(required))));
            lore.add(Text.item("<gray>Missing <white><missing> <gray>XP for rank <white><next>",
                    Placeholder.unparsed("missing", Long.toString(required - current)),
                    Placeholder.unparsed("next", LevelScale.display(levelIndex + 1))));
            lore.add(progressBar(current, required));
        }
        lore.add(Component.empty());
        lore.add(Text.item("<dark_gray>Experience comes from harvesting herbs."));
        return item(LEVEL_BOOK, Text.item("<green>Herbalism"), lore);
    }

    private static Component progressBar(long current, long required) {
        int filled = (int) Math.min(PROGRESS_BAR_WIDTH, current * PROGRESS_BAR_WIDTH / Math.max(1L, required));
        return Text.item("<green>" + "|".repeat(filled)
                + "<dark_gray>" + "|".repeat(PROGRESS_BAR_WIDTH - filled)
                + " <gray><percent>%",
                Placeholder.unparsed("percent", Long.toString(current * 100L / Math.max(1L, required))));
    }

    /** Full re-render, used after the selected herb or the player's level changed. */
    public void renderAll() {
        renderStatic();
        renderPlots();
    }

    private ItemStack plotItem(Plot plot) {
        HerbType herb = plot.herb();
        if (herb == null) {
            return item(EMPTY_PLOT, Text.item("<white>Empty plot"),
                    List.of(Text.item("<gray>Click to plant <herb><gray>.",
                            Placeholder.parsed("herb", profile.selectedHerb().coloredName()))));
        }
        if (plot.isReady()) {
            return item(READY_PLOT, Text.item("<green>Ready to harvest"),
                    List.of(Text.item("<gray>Crop: <herb>", Placeholder.parsed("herb", herb.coloredName())),
                            Text.item("<gray>Click to harvest.")));
        }
        int maxUses = config.fertilizerMaxUses();
        return item(herb.paneMaterial(), Text.item(herb.coloredName()),
                List.of(Text.item("<gray>Ready in <white><time>",
                                Placeholder.unparsed("time", formatDuration(plot.remainingMillis()))),
                        Text.item("<dark_gray>Fertilizer: <gray><used><dark_gray>/<gray><max>",
                                Placeholder.unparsed("used", Integer.toString(plot.fertilizerUses())),
                                Placeholder.unparsed("max", Integer.toString(maxUses))),
                        plot.canFertilize(maxUses)
                                ? Text.item("<gray>Fertilize to speed this up.")
                                : Text.item("<red>Fertilizer limit reached.")));
    }

    private boolean vipGrowth() {
        return player.hasPermission(Permissions.VIP);
    }

    static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(name);
            meta.lore(lore);
        });
        return stack;
    }

    static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}

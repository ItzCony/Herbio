package dev.herbio.gui;

import dev.herbio.config.HerbioConfig;
import dev.herbio.config.Messages;
import dev.herbio.garden.ActionOutcome;
import dev.herbio.garden.GardenResult;
import dev.herbio.garden.GardenService;
import dev.herbio.herb.HerbItems;
import dev.herbio.herb.HerbType;
import dev.herbio.player.HerbioProfile;
import dev.herbio.player.PlayerManager;
import dev.herbio.util.Permissions;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Opens garden views, routes their clicks and keeps their growth timers on screen. */
public final class GuiManager {

    private final Plugin plugin;
    private final HerbioConfig config;
    private final Messages messages;
    private final PlayerManager players;
    private final GardenService gardens;
    private final HerbItems items;
    private final Map<UUID, HerbGui> openGuis = new ConcurrentHashMap<>();

    private @Nullable ScheduledTask refreshTask;

    public GuiManager(Plugin plugin,
                      HerbioConfig config,
                      Messages messages,
                      PlayerManager players,
                      GardenService gardens,
                      HerbItems items) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.players = players;
        this.gardens = gardens;
        this.items = items;
    }

    public void open(Player player) {
        HerbioProfile profile = players.get(player.getUniqueId());
        if (profile == null) {
            messages.send(player, "profile-loading");
            players.load(player.getUniqueId());
            return;
        }
        HerbGui gui = new HerbGui(config, player, profile);
        openGuis.put(player.getUniqueId(), gui);
        gui.open();
    }

    /** Opens the herb picker of a gardening permit. */
    public void openScroll(Player player) {
        HerbioProfile profile = players.get(player.getUniqueId());
        if (profile == null) {
            messages.send(player, "profile-loading");
            players.load(player.getUniqueId());
            return;
        }
        new ScrollGui(config, player, profile).open();
    }

    /** Redraws an open garden view, e.g. after an admin changed the player's rank. */
    public void refresh(Player player) {
        HerbGui gui = openGuis.get(player.getUniqueId());
        if (gui != null) {
            player.getScheduler().run(plugin, task -> gui.renderAll(), null);
        }
    }

    void handleScrollClick(ScrollGui gui, int slot) {
        Player player = gui.player();
        HerbioProfile profile = gui.profile();
        HerbType herb = gui.herbAt(slot);
        if (herb == null) {
            return;
        }
        ItemStack permit = player.getInventory().getItemInMainHand();
        if (!items.isScroll(permit)) {
            messages.send(player, "scroll-missing");
            player.closeInventory();
            return;
        }
        GardenResult result = gardens.harvestInstant(player, profile, herb);
        if (result.count() == 0) {
            messages.send(player, "nothing-to-harvest");
            player.closeInventory();
            return;
        }
        permit.setAmount(permit.getAmount() - 1);
        messages.send(player, "scroll-used",
                Placeholder.unparsed("amount", Integer.toString(result.count())),
                Placeholder.unparsed("xp", Long.toString(result.xp())),
                Placeholder.parsed("herb", herb.coloredName()));
        if (result.levelsGained() > 0) {
            messages.send(player, "level-up", Placeholder.unparsed("level", profile.levelDisplay()));
        }
        // Reopen the garden on the picked herb so the player can replant straight away.
        profile.selectHerb(herb);
        player.getScheduler().run(plugin, task -> open(player), null);
    }

    void handleClose(HerbGui gui) {
        openGuis.remove(gui.player().getUniqueId(), gui);
    }

    void handleClick(HerbGui gui, int slot, ClickType click) {
        Player player = gui.player();
        HerbioProfile profile = gui.profile();

        HerbType selector = GuiLayout.selectorHerb(slot);
        if (selector != null) {
            selectHerb(gui, selector);
            return;
        }
        if (slot == GuiLayout.FERTILIZE_ALL_SLOT) {
            report(gui, gardens.fertilizeAll(player, profile), "fertilized", "nothing-to-fertilize");
            return;
        }
        if (slot == GuiLayout.BULK_HOE_SLOT) {
            if (!player.hasPermission(Permissions.VIP)) {
                messages.send(player, "no-permission");
                return;
            }
            if (click.isRightClick()) {
                report(gui, gardens.plantAll(player, profile), "planted", "nothing-to-plant");
            } else {
                report(gui, gardens.harvestAll(player, profile), "harvested", "nothing-to-harvest");
            }
            return;
        }

        int plotIndex = GuiLayout.plotIndex(slot);
        if (plotIndex < 0) {
            return;
        }
        if (profile.garden().plot(plotIndex).isEmpty()) {
            report(gui, gardens.plant(player, profile, plotIndex), null, null);
        } else if (profile.garden().plot(plotIndex).isReady()) {
            report(gui, gardens.harvest(player, profile, plotIndex), "harvested", null);
        } else if (click.isRightClick()) {
            report(gui, gardens.fertilize(player, profile, plotIndex), null, null);
        } else {
            messages.send(player, "plot-not-ready");
        }
    }

    private void selectHerb(HerbGui gui, HerbType herb) {
        Player player = gui.player();
        HerbioProfile profile = gui.profile();
        if (!herb.isUnlockedAt(profile.levelIndex())) {
            messages.send(player, "herb-locked",
                    Placeholder.parsed("herb", herb.coloredName()),
                    Placeholder.unparsed("level", dev.herbio.herb.LevelScale.display(herb.minLevelIndex())));
            return;
        }
        if (profile.selectedHerb() == herb) {
            return;
        }
        profile.selectHerb(herb);
        messages.send(player, "selected", Placeholder.parsed("herb", herb.coloredName()));
        // The herb name is part of the inventory title, so the view has to be rebuilt.
        HerbGui replacement = new HerbGui(config, player, profile);
        openGuis.put(player.getUniqueId(), replacement);
        player.getScheduler().run(plugin, task -> replacement.open(), null);
    }

    /**
     * Sends feedback for a garden action and refreshes the view.
     *
     * @param successKey message for a successful action, or {@code null} to stay silent
     * @param emptyKey   message when the action was valid but affected nothing
     */
    private void report(HerbGui gui, GardenResult result, @Nullable String successKey, @Nullable String emptyKey) {
        Player player = gui.player();
        if (result.isSuccess()) {
            if (successKey != null) {
                messages.send(player, successKey,
                        Placeholder.unparsed("amount", Integer.toString(result.count())),
                        Placeholder.unparsed("xp", Long.toString(result.xp())));
            }
            if (result.levelsGained() > 0) {
                messages.send(player, "level-up", Placeholder.unparsed("level", profileLevel(gui)));
                gui.renderAll();
            }
        } else if (result.count() == 0 && result.outcome() == ActionOutcome.SUCCESS) {
            if (emptyKey != null) {
                messages.send(player, emptyKey);
            }
        } else {
            sendFailure(gui, result.outcome(), emptyKey);
        }
        gui.renderPlots();
    }

    private void sendFailure(HerbGui gui, ActionOutcome outcome, @Nullable String emptyKey) {
        Player player = gui.player();
        String key = outcome.messageKey();
        if (key == null) {
            if (emptyKey != null) {
                messages.send(player, emptyKey);
            }
            return;
        }
        HerbType herb = gui.profile().selectedHerb();
        messages.send(player, key,
                Placeholder.parsed("herb", herb.coloredName()),
                Placeholder.unparsed("level", dev.herbio.herb.LevelScale.display(herb.minLevelIndex())));
    }

    private String profileLevel(HerbGui gui) {
        return gui.profile().levelDisplay();
    }

    public void startRefreshTask() {
        this.refreshTask = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, task -> refreshAll(), config.guiRefreshTicks(), config.guiRefreshTicks());
    }

    private void refreshAll() {
        for (HerbGui gui : openGuis.values()) {
            Player player = gui.player();
            if (!player.isOnline()) {
                openGuis.remove(player.getUniqueId(), gui);
                continue;
            }
            // Inventory contents must be touched on the region thread owning the player.
            player.getScheduler().run(plugin, task -> gui.renderPlots(), null);
        }
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        openGuis.clear();
    }
}

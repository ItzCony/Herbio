package dev.herbio.command;

import dev.herbio.config.Messages;
import dev.herbio.gui.GuiManager;
import dev.herbio.herb.HerbItemKind;
import dev.herbio.herb.HerbItems;
import dev.herbio.herb.HerbType;
import dev.herbio.herb.LevelScale;
import dev.herbio.player.HerbioProfile;
import dev.herbio.player.PlayerManager;
import dev.herbio.util.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@code /herb gui}, {@code /herb give <fert|herb|seed> <herb> [amount]},
 * {@code /herb give scroll <player>} and {@code /herb set <player> <level>}.
 */
public final class HerbCommand implements CommandExecutor, TabCompleter {

    /** A full player inventory, so a typo cannot flood the world with dropped items. */
    private static final int MAX_GIVE_AMOUNT = 2304;

    private final Messages messages;
    private final GuiManager guis;
    private final HerbItems items;
    private final PlayerManager players;
    private final Plugin plugin;

    public HerbCommand(Plugin plugin, Messages messages, GuiManager guis, HerbItems items, PlayerManager players) {
        this.plugin = plugin;
        this.messages = messages;
        this.guis = guis;
        this.items = items;
        this.players = players;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {
        if (args.length == 0) {
            messages.send(sender, "usage");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "gui" -> handleGui(sender);
            case "give" -> handleGive(sender, args);
            case "set" -> handleSet(sender, args);
            default -> {
                messages.send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission(Permissions.GUI)) {
            messages.send(player, "no-permission");
            return true;
        }
        guis.open(player);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission(Permissions.ADMIN)) {
            messages.send(player, "no-permission");
            return true;
        }
        if (args.length < 3) {
            messages.send(player, "usage");
            return true;
        }
        if (args[1].equalsIgnoreCase(HerbItemKind.SCROLL.id())) {
            return handleGiveScroll(player, args[2]);
        }
        HerbItemKind kind = HerbItemKind.byId(args[1]);
        if (kind == null) {
            messages.send(player, "unknown-item", Placeholder.unparsed("input", args[1]));
            return true;
        }
        HerbType herb = HerbType.byId(args[2]);
        if (herb == null) {
            messages.send(player, "unknown-herb", Placeholder.unparsed("input", args[2]));
            return true;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                amount = -1;
            }
            if (amount < 1 || amount > MAX_GIVE_AMOUNT) {
                messages.send(player, "invalid-amount",
                        Placeholder.unparsed("max", Integer.toString(MAX_GIVE_AMOUNT)));
                return true;
            }
        }
        items.give(player, kind, herb, amount);
        messages.send(player, "given",
                Placeholder.unparsed("amount", Integer.toString(amount)),
                Placeholder.parsed("herb", herb.coloredName()),
                Placeholder.unparsed("kind", kind.displayName()));
        return true;
    }

    private boolean handleGiveScroll(Player sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messages.send(sender, "player-not-found", Placeholder.unparsed("input", targetName));
            return true;
        }
        // Folia: another player's inventory may only be touched from their own region thread.
        target.getScheduler().run(plugin, task -> items.give(target, items.createScroll()), null);
        messages.send(sender, "scroll-given", Placeholder.unparsed("player", target.getName()));
        if (!target.equals(sender)) {
            messages.send(target, "scroll-received");
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found", Placeholder.unparsed("input", args[1]));
            return true;
        }
        int levelIndex = LevelScale.parse(args[2]);
        if (levelIndex < 0) {
            messages.send(sender, "invalid-level", Placeholder.unparsed("input", args[2]));
            return true;
        }
        HerbioProfile profile = players.get(target.getUniqueId());
        if (profile == null) {
            messages.send(sender, "profile-loading");
            players.load(target.getUniqueId());
            return true;
        }
        profile.setLevelIndex(levelIndex);
        guis.refresh(target);
        messages.send(sender, "level-set",
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("level", LevelScale.display(levelIndex)));
        if (!target.equals(sender)) {
            messages.send(target, "level-set-target",
                    Placeholder.unparsed("level", LevelScale.display(levelIndex)));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender,
                                                Command command,
                                                String label,
                                                String[] args) {
        List<String> options = new ArrayList<>();
        boolean admin = sender.hasPermission(Permissions.ADMIN);
        boolean give = args[0].equalsIgnoreCase("give");
        boolean set = args[0].equalsIgnoreCase("set");
        if (args.length == 1) {
            if (sender.hasPermission(Permissions.GUI)) {
                options.add("gui");
            }
            if (admin) {
                options.add("give");
                options.add("set");
            }
        } else if (args.length == 2 && give && admin) {
            Arrays.stream(HerbItemKind.values()).map(HerbItemKind::id).forEach(options::add);
        } else if (args.length == 3 && give && admin) {
            if (args[1].equalsIgnoreCase(HerbItemKind.SCROLL.id())) {
                Bukkit.getOnlinePlayers().forEach(online -> options.add(online.getName()));
            } else {
                Arrays.stream(HerbType.values()).map(HerbType::id).forEach(options::add);
            }
        } else if (args.length == 4 && give && admin) {
            options.add("1");
            options.add("16");
            options.add("64");
        } else if (args.length == 2 && set && admin) {
            Bukkit.getOnlinePlayers().forEach(online -> options.add(online.getName()));
        } else if (args.length == 3 && set && admin) {
            options.add("0");
            options.add("10");
            options.add("M1");
            options.add("G1");
            options.add("P");
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }
}

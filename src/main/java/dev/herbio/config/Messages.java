package dev.herbio.config;

import dev.herbio.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/** All player-facing strings, loaded from the {@code messages} config section. */
public final class Messages {

    private final Map<String, String> messages = new HashMap<>();
    private final String prefix;

    private Messages(FileConfiguration configuration) {
        ConfigurationSection section = configuration.getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                messages.put(key, section.getString(key, ""));
            }
        }
        this.prefix = messages.getOrDefault("prefix", "");
    }

    public static Messages load(FileConfiguration configuration) {
        return new Messages(configuration);
    }

    public Component get(String key, TagResolver... resolvers) {
        return Text.of(prefix + messages.getOrDefault(key, "<red>Missing message: " + key), resolvers);
    }

    public void send(CommandSender receiver, String key, TagResolver... resolvers) {
        receiver.sendMessage(get(key, resolvers));
    }
}

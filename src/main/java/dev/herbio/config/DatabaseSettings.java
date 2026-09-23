package dev.herbio.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public record DatabaseSettings(String host,
                               int port,
                               String database,
                               String username,
                               String password,
                               int poolSize,
                               Map<String, String> properties) {

    public static DatabaseSettings load(ConfigurationSection section) {
        Map<String, String> properties = new LinkedHashMap<>();
        ConfigurationSection propertySection = section.getConfigurationSection("properties");
        if (propertySection != null) {
            for (String key : propertySection.getKeys(false)) {
                properties.put(key, String.valueOf(propertySection.get(key)));
            }
        }
        return new DatabaseSettings(
                section.getString("host", "localhost"),
                section.getInt("port", 3306),
                section.getString("database", "herbio"),
                section.getString("username", "root"),
                section.getString("password", ""),
                Math.max(1, section.getInt("pool-size", 8)),
                Map.copyOf(properties));
    }

    public String jdbcUrl() {
        StringBuilder url = new StringBuilder("jdbc:mysql://").append(host).append(':').append(port).append('/').append(database);
        char separator = '?';
        for (Map.Entry<String, String> property : properties.entrySet()) {
            url.append(separator).append(property.getKey()).append('=').append(property.getValue());
            separator = '&';
        }
        return url.toString();
    }
}

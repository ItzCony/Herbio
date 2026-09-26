package dev.herbio.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.Nullable;

/**
 * Thin wrapper over the Vault economy service, which is what CMI registers.
 * The provider is looked up lazily because the economy plugin may enable after Herbio.
 */
public final class VaultEconomy {

    private final Server server;
    private @Nullable Economy economy;

    public VaultEconomy(Server server) {
        this.server = server;
    }

    public boolean isAvailable() {
        return economy() != null;
    }

    /**
     * Takes money from the player.
     *
     * @return {@code false} when there is no economy or the player cannot afford it
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        Economy service = economy();
        if (service == null || !service.has(player, amount)) {
            return false;
        }
        return service.withdrawPlayer(player, amount).transactionSuccess();
    }

    /** Formats an amount with the economy plugin, or plain when no economy is installed. */
    public String format(double amount) {
        Economy service = economy();
        if (service != null) {
            return service.format(amount);
        }
        return amount == Math.rint(amount) ? Long.toString((long) amount) : Double.toString(amount);
    }

    private @Nullable Economy economy() {
        if (economy == null && server.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> provider = server.getServicesManager().getRegistration(Economy.class);
            economy = provider == null ? null : provider.getProvider();
        }
        return economy;
    }
}

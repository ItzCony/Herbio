package dev.herbio.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Keeps the profile cache in sync with the online player set. */
public final class PlayerSessionListener implements Listener {

    private final PlayerManager players;

    public PlayerSessionListener(PlayerManager players) {
        this.players = players;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        players.load(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        players.unload(event.getPlayer().getUniqueId());
    }
}

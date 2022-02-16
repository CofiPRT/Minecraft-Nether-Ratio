package ro.cofi.netherratio;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    private final NetherRatio plugin;

    public PlayerTeleportListener(NetherRatio plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // only intervene in nether portal teleportation
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
            return;

        // so, this event will only happen
    }

}

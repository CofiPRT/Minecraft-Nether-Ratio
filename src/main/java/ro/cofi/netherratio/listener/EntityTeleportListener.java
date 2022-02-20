package ro.cofi.netherratio.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import ro.cofi.netherratio.NetherRatio;
import ro.cofi.netherratio.event.CustomEntityTeleportEvent;
import ro.cofi.netherratio.event.CustomPlayerTeleportEvent;
import ro.cofi.netherratio.misc.Constants;

import java.util.Objects;

public class EntityTeleportListener implements Listener {

    private final NetherRatio plugin;

    public EntityTeleportListener(NetherRatio plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event instanceof CustomPlayerTeleportEvent)
            return;

        // only intervene in nether portal teleportation
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
            return;

        handleEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event instanceof CustomEntityTeleportEvent)
            return;

        // only intervene in nether portal teleportation
        if (event.getTo() == null)
            return;

        World from = event.getFrom().getWorld();
        World to = event.getTo().getWorld();

        if (Objects.equals(from, to) ||
                !Constants.VALID_ENVIRONMENTS.contains(from.getEnvironment()) ||
                !Constants.VALID_ENVIRONMENTS.contains(to.getEnvironment())
        )
            return;

        handleEvent(event, event.getEntity());
    }

    public void handleEvent(Cancellable event, Entity entity) {
        Location referencePoint = plugin.getPortalLogicManager().getReferencePoint(entity);
        if (referencePoint == null)
            return;

        // here, what we want to do is simply cancel the event, because we want to handle it differently
        event.setCancelled(true);

        plugin.getPortalLogicManager().handleEntityTeleport(entity, referencePoint);
    }

}

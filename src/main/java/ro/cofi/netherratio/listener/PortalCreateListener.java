package ro.cofi.netherratio.listener;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.PortalCreateEvent;
import ro.cofi.netherratio.NetherRatio;
import ro.cofi.netherratio.event.CustomPortalCreateEvent;

public class PortalCreateListener extends AbstractListener {

    public PortalCreateListener(NetherRatio plugin) {
        super(plugin);
    }

    /**
     * Captured every time a portal pair is created. Normally, when a player touches a portal block that doesn't even
     * have a frame, the game attempts to create a vanilla nether portal in the other dimension. This event gets
     * cancelled if the said portal is part of a custom one (with a custom frame), for we intend to handle this
     * creation differently.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (event instanceof CustomPortalCreateEvent)
            return;

        // only intervene in portal pairing
        if (event.getReason() != PortalCreateEvent.CreateReason.NETHER_PAIR)
            return;

        Entity entity = event.getEntity();
        if (entity == null)
            return;

        // only intervene if the portal type is custom
        Location referencePoint = plugin.getPortalLogicManager().getReferencePoint(entity);
        if (referencePoint == null)
            return;

        // here, what we want to do is simply cancel the event, because we want to handle it differently
        event.setCancelled(true);

        plugin.getPortalLogicManager().handleEntityTeleport(entity, referencePoint);
    }

}

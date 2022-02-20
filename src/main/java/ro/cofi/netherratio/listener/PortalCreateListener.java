package ro.cofi.netherratio.listener;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import ro.cofi.netherratio.NetherRatio;

public class PortalCreateListener implements Listener {

    private final NetherRatio plugin;

    public PortalCreateListener(NetherRatio plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
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

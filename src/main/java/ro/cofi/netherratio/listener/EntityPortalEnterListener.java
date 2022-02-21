package ro.cofi.netherratio.listener;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import ro.cofi.netherratio.NetherRatio;

public class EntityPortalEnterListener extends AbstractListener {

    public EntityPortalEnterListener(NetherRatio plugin) {
        super(plugin);
    }

    /**
     * Captured every time an entity simply touches a portal. Explicitly exclude players.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player)
            return;

        // if the entity has portal cooldown, preserve it, so that the entity has to leave the portal for it to go away
        int portalCooldown = entity.getPortalCooldown();

        if (portalCooldown > 0) {
            entity.setPortalCooldown(portalCooldown + 1);
            return;
        }

        // only intervene in custom nether portal teleportation
        Location referencePoint = plugin.getPortalLogicManager().getReferencePoint(entity);
        if (referencePoint == null)
            return;

        plugin.getPortalLogicManager().handleEntityTeleport(entity, referencePoint);
    }

}

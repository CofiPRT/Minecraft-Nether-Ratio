package ro.cofi.netherratio;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.util.Vector;

public class PlayerPortalCreateListener implements Listener {

    private final NetherRatio plugin;

    public PlayerPortalCreateListener(NetherRatio plugin) {
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
        World world = entity.getWorld();
        Location checkedLocation = entity.getLocation().toBlockLocation();

        // look down until we find the bottom frame of the portal
        while (world.getBlockAt(checkedLocation).getType() == Material.NETHER_PORTAL)
            checkedLocation.subtract(new Vector(0, -1, 0));

        // not the custom material, ignore
        if (world.getBlockAt(checkedLocation).getType() != Material.CRYING_OBSIDIAN)
            return;

        // here, what we want to do is simply cancel the event, because we want to handle it differently
        event.setCancelled(true);
    }

}

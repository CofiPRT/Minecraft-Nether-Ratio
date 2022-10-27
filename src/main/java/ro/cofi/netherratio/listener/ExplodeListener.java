package ro.cofi.netherratio.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import ro.cofi.netherratio.NetherRatio;
import ro.cofi.netherratio.event.CustomBlockExplodeEvent;
import ro.cofi.netherratio.event.CustomEntityExplodeEvent;
import ro.cofi.netherratio.logic.ReferencePoint;
import ro.cofi.netherratio.misc.Constants;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ExplodeListener extends AbstractListener {

    public ExplodeListener(NetherRatio plugin) {
        super(plugin);
    }

    /**
     * Captured whenever a block explodes (like a bed, or an ender crystal).
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event instanceof CustomBlockExplodeEvent)
            return;

        if (!Constants.VALID_ENVIRONMENTS.contains(event.getBlock().getWorld().getEnvironment()))
            return;

        handleNewEvent(event.blockList(), () -> new CustomBlockExplodeEvent(
            event.getBlock(),
            event.blockList(),
            event.getYield()
        ));
    }

    /**
     * Captured whenever an entity explodes (primed TNT, fireballs etc.)
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event instanceof CustomEntityExplodeEvent)
            return;

        if (!Constants.VALID_ENVIRONMENTS.contains(event.getEntity().getWorld().getEnvironment()))
            return;

        handleNewEvent(event.blockList(), () -> new CustomEntityExplodeEvent(
            event.getEntity(),
            event.getLocation(),
            event.blockList(),
            event.getYield()
        ));
    }

    private void handleNewEvent(List<Block> blocks, Supplier<Event> newEventSupplier) {
        List<ReferencePoint> referencePoints = blocks.stream()
            .filter(block -> block.getType() == Material.NETHER_PORTAL)
            .map(block -> plugin.getPortalLogicManager().getReferencePoint(block.getLocation()))
            .filter(Objects::nonNull)
            .toList();

        // no point in firing any event
        if (referencePoints.isEmpty())
            return;

        // fire a custom event - if it gets cancelled, don't explode the blocks
        Event newEvent = newEventSupplier.get();
        Bukkit.getPluginManager().callEvent(newEvent);

        if (((Cancellable) newEvent).isCancelled())
            return;

        // remove from the lookup
        for (ReferencePoint referencePoint : referencePoints)
            plugin.getPortalLocationManager().deletePortal(referencePoint.location(), referencePoint.isCustom());
    }

}

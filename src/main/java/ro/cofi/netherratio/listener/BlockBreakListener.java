package ro.cofi.netherratio.listener;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;
import ro.cofi.netherratio.NetherRatio;
import ro.cofi.netherratio.event.CustomBlockBreakEvent;
import ro.cofi.netherratio.misc.Constants;
import ro.cofi.netherratio.misc.VectorAxis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockBreakListener implements Listener {

    private final NetherRatio plugin;

    public BlockBreakListener(NetherRatio plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBreak(BlockBreakEvent event) {
        List<Vector> portals = plugin.getPortalLocationManager().getPortals(World.Environment.NORMAL);
        plugin.getLogger().info("Overworld:");
        for (Vector portal : portals)
            plugin.getLogger().info("____" + portal.getX() + "," + portal.getY() + "," + portal.getZ() + "____");
        portals = plugin.getPortalLocationManager().getPortals(World.Environment.NETHER);
        plugin.getLogger().info("Nether:");
        for (Vector portal : portals)
            plugin.getLogger().info("____" + portal.getX() + "," + portal.getY() + "," + portal.getZ() + "____");

        if (event instanceof CustomBlockBreakEvent)
            return;

        Block block = event.getBlock();
        List<Location> referencePoints = new ArrayList<>();

        if (block.getType() == Material.NETHER_PORTAL)
            referencePoints.addAll(getFromNetherPortal(block));
        else if (block.getType() == Constants.FRAME_BLOCK)
            referencePoints.addAll(getFromFrame(block));

        // no point in firing any events
        if (referencePoints.isEmpty())
            return;

        // fire the event and check for cancellation
        handleNewEvent(event, referencePoints);
    }

    private List<Location> getFromNetherPortal(Block block) {
        Location referencePoint = plugin.getPortalLogicManager().getReferencePoint(block.getLocation());
        if (referencePoint == null)
            return Collections.emptyList();

        return Collections.singletonList(referencePoint);
    }

    private List<Location> getFromFrame(Block block) {
        Location origin = block.getLocation();
        List<Location> connectedPortalBlocks = new ArrayList<>();

        connectedPortalBlocks.add(checkAdjacent(origin, VectorAxis.X, Axis.X));
        connectedPortalBlocks.add(checkAdjacent(origin, VectorAxis.NX, Axis.X));
        connectedPortalBlocks.add(checkAdjacent(origin, VectorAxis.Z, Axis.Z));
        connectedPortalBlocks.add(checkAdjacent(origin, VectorAxis.NZ, Axis.Z));
        connectedPortalBlocks.add(checkAdjacent(origin, VectorAxis.Y, null));
        connectedPortalBlocks.add(checkAdjacent(origin, VectorAxis.NY, null));

        // compute reference points
        List<Location> referencePoints = new ArrayList<>();

        for (Location connectedPortalBlock : connectedPortalBlocks) {
            if (connectedPortalBlock == null)
                continue;

            Location referencePoint = plugin.getPortalLogicManager().getReferencePoint(connectedPortalBlock);
            if (referencePoint != null)
                referencePoints.add(referencePoint);
        }

        return referencePoints;
    }

    private Location checkAdjacent(Location origin, Vector direction, Axis axis) {
        Block targetBlock = origin.clone().add(direction).getBlock();

        if (targetBlock.getType() == Material.NETHER_PORTAL &&
                (axis == null || ((Orientable) targetBlock.getBlockData()).getAxis() == axis))
            return targetBlock.getLocation();

        return null;
    }

    private void handleNewEvent(BlockBreakEvent event, List<Location> referencePoints) {
        Event newEvent = new CustomBlockBreakEvent(event.getBlock(), event.getPlayer());
        if (((Cancellable) newEvent).isCancelled())
            return;

        // remove from the lookup
        for (Location referencePoint : referencePoints)
            plugin.getPortalLocationManager().deletePortal(referencePoint);
    }

}

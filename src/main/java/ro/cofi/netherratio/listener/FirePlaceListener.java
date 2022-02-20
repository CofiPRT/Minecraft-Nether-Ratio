package ro.cofi.netherratio.listener;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;
import ro.cofi.netherratio.ConfigKey;
import ro.cofi.netherratio.NetherRatio;
import ro.cofi.netherratio.misc.Constants;
import ro.cofi.netherratio.misc.LocationUtil;
import ro.cofi.netherratio.misc.VectorAxis;

import java.util.Arrays;
import java.util.List;

public class FirePlaceListener implements Listener {

    private final NetherRatio plugin;

    public FirePlaceListener(NetherRatio plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFirePlace(BlockPlaceEvent event) {
        Block blockPlaced = event.getBlockPlaced();
        Material type = blockPlaced.getType();

        // don't intercept
        if (!type.equals(Material.FIRE))
            return;

        World world = blockPlaced.getWorld();

        // check valid dimension
        if (!Constants.VALID_ENVIRONMENTS.contains(world.getEnvironment()))
            return;

        PortalFrameData frameData = computePortalBlocks(blockPlaced, world);
        if (frameData == null)
            return;

        // these locations must be replaceable by a literal portal block
        for (Location location : frameData.getInnerLocations())
            if (!Constants.REPLACEABLE_BLOCKS.contains(world.getBlockAt(location).getType()))
                return;

        // everything is validated, perform the placement
        Orientable blockData = (Orientable) Material.NETHER_PORTAL.createBlockData();
        blockData.setAxis(frameData.getHorizontalAxis());

        for (Location location : frameData.getInnerLocations())
            location.getBlock().setBlockData(blockData);

        // save for lookups
        plugin.getPortalLocationManager().savePortal(frameData.getBottomLeft());
    }

    private PortalFrameData computePortalBlocks(Block blockPlaced, World world) {
        Location origin = blockPlaced.getLocation().toBlockLocation();

        int minHeight = plugin.getConfig().getInt(ConfigKey.PORTAL_HEIGHT_MIN);
        int maxHeight = plugin.getConfig().getInt(ConfigKey.PORTAL_HEIGHT_MAX);
        int minWidth = plugin.getConfig().getInt(ConfigKey.PORTAL_WIDTH_MIN);
        int maxWidth = plugin.getConfig().getInt(ConfigKey.PORTAL_WIDTH_MAX);

        // find vertical limits
        Location bottom = LocationUtil.findFrameLimit(origin, VectorAxis.NY, Constants.REPLACEABLE_BLOCKS, maxHeight);
        if (bottom == null)
            return null;

        Location top = LocationUtil.findFrameLimit(origin, VectorAxis.Y, Constants.REPLACEABLE_BLOCKS, maxHeight);
        if (top == null)
            return null;

        // keep within bounds
        double height = Math.sqrt(top.distanceSquared(bottom)) + 1;
        if (height < minHeight || height > maxHeight)
            return null;

        Location left = null, right = null; // according to axis

        // attempt on X axis, then Z axis
        for (Vector axis : Arrays.asList(VectorAxis.X, VectorAxis.Z)) {
            left = LocationUtil.findFrameLimit(origin, axis.clone().multiply(-1), Constants.REPLACEABLE_BLOCKS, maxWidth);
            if (left == null)
                continue;

            right = LocationUtil.findFrameLimit(origin, axis.clone().multiply(1), Constants.REPLACEABLE_BLOCKS, maxWidth);
            if (right == null) {
                left = null; // reset for future iteration
                continue;
            }

            // keep within bounds
            double width = left.distanceSquared(right) + 1;
            if (width >= minWidth || width <= maxWidth)
                break;

            // reset for future iteration
            left = null;
            right = null;
        }

        // horizontal limits not found
        if (left == null)
            return null;

        Location bottomLeft = new Location(world, left.getBlockX(), bottom.getBlockY(), left.getBlockZ());
        Location bottomRight = new Location(world, right.getBlockX(), bottom.getBlockY(), right.getBlockZ());
        Location topLeft = new Location(world, left.getBlockX(), top.getBlockY(), left.getBlockZ());
        Location topRight = new Location(world, right.getBlockX(), top.getBlockY(), right.getBlockZ());

        // check all frames
        Axis horizontalAxis = getAxis(left, right);
        Vector axisDirection = horizontalAxis == Axis.X ? VectorAxis.X : VectorAxis.Z; // what direction gets used below

        if (!checkFrame(bottomLeft, bottomRight, VectorAxis.NY) ||
            !checkFrame(topLeft, topRight, VectorAxis.Y) ||
            !checkFrame(bottomLeft, topLeft, axisDirection.clone().multiply(-1)) ||
            !checkFrame(bottomRight, topRight, axisDirection))
            return null;

        // this is indeed a portal, return its blocks to check for perms
        return new PortalFrameData(bottomLeft, topRight, horizontalAxis);
    }

    private Axis getAxis(Location left, Location right) {
        return left.getBlockX() == right.getBlockX() ? Axis.Z : Axis.X;
    }

    private boolean checkFrame(Location corner1, Location corner2, Vector frameDirection) {
        World world = corner1.getWorld();

        for (Location location : LocationUtil.getLocationsBetween(corner1, corner2))
            if (world.getBlockAt(location.clone().add(frameDirection)).getType() != Constants.FRAME_BLOCK)
                return false;

        return true;
    }

    private static class PortalFrameData {

        private final List<Location> innerLocations;
        private final Location bottomLeft;
        private final Axis horizontalAxis;

        public PortalFrameData(Location bottomLeft, Location topRight, Axis horizontalAxis) {
            this.innerLocations = LocationUtil.getLocationsBetween(bottomLeft, topRight);
            this.horizontalAxis = horizontalAxis;
            this.bottomLeft = bottomLeft;
        }

        public List<Location> getInnerLocations() {
            return innerLocations;
        }

        public Location getBottomLeft() {
            return bottomLeft;
        }

        public Axis getHorizontalAxis() {
            return horizontalAxis;
        }

    }

}

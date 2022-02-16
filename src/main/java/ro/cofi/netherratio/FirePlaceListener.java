package ro.cofi.netherratio;

import com.google.common.collect.ImmutableSet;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class FirePlaceListener implements Listener {

    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;
    private static final int MIN_WIDTH = 2;
    private static final int MAX_WIDTH = 21;

    private static final Vector AXIS_X = new Vector(1, 0, 0);
    private static final Vector AXIS_Y = new Vector(0, 1, 0);
    private static final Vector AXIS_Z = new Vector(0, 0, 1);
    private static final Vector AXIS_NX = new Vector(-1, 0, 0);
    private static final Vector AXIS_NY = new Vector(0, -1, 0);
    private static final Vector AXIS_NZ = new Vector(0, 0, -1);

    private static final Set<Material> replaceableBlocks = ImmutableSet.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.FIRE
    );

    private static final Set<Material> frameBlocks = ImmutableSet.of(
            Material.CRYING_OBSIDIAN
    );

    private static final Set<World.Environment> validEnvironments = EnumSet.of(
            World.Environment.NORMAL,
            World.Environment.NETHER
    );

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
        if (!validEnvironments.contains(world.getEnvironment()))
            return;

        PortalFrameData frameData = computePortalBlocks(blockPlaced, world);
        if (frameData == null)
            return;

        // these locations must be replaceable by a literal portal block
        for (Location location : frameData.getInnerLocations())
            if (!replaceableBlocks.contains(world.getBlockAt(location).getType()))
                return;

        // everything is validated, perform the placement
        Orientable blockData = (Orientable) Material.NETHER_PORTAL.createBlockData();
        blockData.setAxis(frameData.getHorizontalAxis());

        for (Location location : frameData.getInnerLocations())
            location.getBlock().setBlockData(blockData);
    }

    private PortalFrameData computePortalBlocks(Block blockPlaced, World world) {
        Location origin = blockPlaced.getLocation().toBlockLocation();

        // find vertical limits
        Location bottom = findLimit(origin, AXIS_NY, MAX_HEIGHT);
        if (bottom == null)
            return null;

        Location top = findLimit(origin, AXIS_Y, MAX_HEIGHT);
        if (top == null)
            return null;

        // keep within bounds
        double height = Math.sqrt(top.distanceSquared(bottom)) + 1;
        if (height < MIN_HEIGHT || height > MAX_HEIGHT)
            return null;

        Location left = null, right = null; // according to axis

        // attempt on X axis, then Z axis
        for (Vector axis : Arrays.asList(AXIS_X, AXIS_Z)) {
            left = findLimit(origin, axis.clone().multiply(-1), MAX_WIDTH);
            if (left == null)
                continue;

            right = findLimit(origin, axis.clone().multiply(1), MAX_WIDTH);
            if (right == null) {
                left = null; // reset for future iteration
                continue;
            }

            // keep within bounds
            double width = left.distanceSquared(right) + 1;
            if (width >= MIN_WIDTH || width <= MAX_WIDTH)
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
        Vector axisDirection = horizontalAxis == Axis.X ? AXIS_X : AXIS_Z; // what direction gets used below

        if (!checkFrame(bottomLeft, bottomRight, AXIS_NY) ||
            !checkFrame(topLeft, topRight, AXIS_Y) ||
            !checkFrame(bottomLeft, topLeft, axisDirection.clone().multiply(-1)) ||
            !checkFrame(bottomRight, topRight, axisDirection))
            return null;

        // this is indeed a portal, return its blocks to check for perms
        return new PortalFrameData(getLocationsBetween(bottomLeft, topRight), horizontalAxis);
    }

    private Location findLimit(Location origin, Vector direction, int maxOffset) {
        World world = origin.getWorld();

        for (int offset = 0; offset < maxOffset; offset++) {
            Location target = origin.clone().add(direction.clone().multiply(offset + 1));
            Material targetBlockType = world.getBlockAt(target).getType();

            // look for the frame
            if (frameBlocks.contains(targetBlockType))
                return target.subtract(direction);

            // a non-replaceable block has been reached, and it's not a valid frame block
            if (!replaceableBlocks.contains(targetBlockType))
                return null;
        }

        // no limit found within the bounds
        return null;
    }

    private Axis getAxis(Location left, Location right) {
        return left.getBlockX() == right.getBlockX() ? Axis.Z : Axis.X;
    }

    private boolean checkFrame(Location corner1, Location corner2, Vector frameDirection) {
        World world = corner1.getWorld();

        for (Location location : getLocationsBetween(corner1, corner2))
            if (!frameBlocks.contains(world.getBlockAt(location.clone().add(frameDirection)).getType()))
                return false;

        return true;
    }

    private List<Location> getLocationsBetween(Location corner1, Location corner2) {
        int x1 = corner1.getBlockX();
        int y1 = corner1.getBlockY();
        int z1 = corner1.getBlockZ();
        int x2 = corner2.getBlockX();
        int y2 = corner2.getBlockY();
        int z2 = corner2.getBlockZ();

        World world = corner1.getWorld();
        List<Location> locations = new ArrayList<>();

        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    locations.add(new Location(world, x, y, z));

        return locations;
    }

}

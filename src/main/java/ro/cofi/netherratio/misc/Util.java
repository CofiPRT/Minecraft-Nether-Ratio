package ro.cofi.netherratio.misc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Util {

    public static List<Location> getLocationsBetween(Vector vector1, Vector vector2, World world) {
        return getLocationsBetween(
                new Location(world, vector1.getBlockX(), vector1.getBlockY(), vector1.getBlockZ()),
                new Location(world, vector2.getBlockX(), vector2.getBlockY(), vector2.getBlockZ())
        );
    }

    public static List<Location> getLocationsBetween(Location corner1, Location corner2) {
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

    public static Location findFrameLimit(Location origin, Vector direction, Set<Material> innerBlocks, int maxOffset) {
        World world = origin.getWorld();

        for (int offset = 0; offset < maxOffset; offset++) {
            Location target = origin.clone().add(direction.clone().multiply(offset + 1));
            Material targetBlockType = world.getBlockAt(target).getType();

            // look for the frame
            if (Constants.FRAME_BLOCKS.contains(targetBlockType))
                return target.subtract(direction);

            // a non-replaceable block has been reached, and it's not a valid frame block
            if (!innerBlocks.contains(targetBlockType))
                return null;
        }

        // no limit found within the bounds
        return null;
    }
}

package ro.cofi.netherratio.misc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LocationUtil {

    public static List<Location> getLocationsBetween(Vector vector1, Vector vector2, World world) {
        return getLocationsBetween(fromVector(world, vector1), fromVector(world, vector2));
    }

    public static List<Location> getLocationsBetween(Location corner1, Location corner2) {
        return getLocationsBetween(corner1, corner2, false);
    }

    public static List<Location> getLocationsBetween(Location corner1, Location corner2, boolean hollow) {
        int x1 = corner1.getBlockX();
        int y1 = corner1.getBlockY();
        int z1 = corner1.getBlockZ();
        int x2 = corner2.getBlockX();
        int y2 = corner2.getBlockY();
        int z2 = corner2.getBlockZ();

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        World world = corner1.getWorld();
        List<Location> locations = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            if (hollow && !(x == minX || x == maxX))
                continue;

            for (int y = minY; y <= maxY; y++) {
                if (hollow && !(y == minY || y == maxY))
                    continue;

                for (int z = minZ; z <= maxZ; z++) {
                    if (hollow && !(z == minZ || z == maxZ))
                        continue;

                    locations.add(new Location(world, x, y, z));
                }
            }
        }

        return locations;
    }

    public static Location fromVector(World world, Vector vec) {
        return new Location(world, vec.getX(), vec.getY(), vec.getZ());
    }

    public static double mapInterval(double oldStart, double oldEnd, double newStart, double newEnd, double value) {
        return newStart + (newEnd - newStart) / (oldEnd - oldStart) * (value - oldStart);
    }

}

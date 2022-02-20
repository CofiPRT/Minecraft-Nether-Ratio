package ro.cofi.netherratio;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import ro.cofi.netherratio.misc.Util;
import ro.cofi.netherratio.misc.VectorAxis;

import java.util.*;
import java.util.stream.Stream;

public class PortalLogicManager {

    private final NetherRatio plugin;

    public PortalLogicManager(NetherRatio plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempt to teleport an entity to the other dimension.
     *
     * A search for an existing portal is performed in a specific radius around the usual, precomputed destination.
     * If no such portal exists, it is created at the precomputed destination (or as close to it).
     *
     * If a portal is available (either already existing or following its creation), the entity is teleported.
     */
    public void handleEntityTeleport(Entity entity, Location referencePoint) {
        // check if there is an existing portal to teleport to
        List<Vector> destinationPortals;
        World destinationWorld;
        double maxDistance;
        double scaleFactor;

        Stream<World> serverWorlds = plugin.getServer().getWorlds().stream();

        // handle teleport direction
        if (entity.getWorld().getEnvironment() == World.Environment.NORMAL) {
            destinationPortals = plugin.getPortalLocationManager().getNetherPortals();
            maxDistance = plugin.getConfig().getDouble("max_distance.nether");
            scaleFactor = getOverworldToNetherFactor();

            Optional<World> nether = serverWorlds
                    .filter(world -> world.getEnvironment() == World.Environment.NETHER)
                    .findFirst();

            if (nether.isEmpty())
                return;

            destinationWorld = nether.get();
        } else {
            destinationPortals = plugin.getPortalLocationManager().getOverworldPortals();
            maxDistance = plugin.getConfig().getDouble("max_distance.overworld");
            scaleFactor = getNetherToOverworldFactor();

            Optional<World> overworld = serverWorlds
                    .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                    .findFirst();

            if (overworld.isEmpty())
                return;

            destinationWorld = overworld.get();
        }

        Vector scaledReferencePoint = referencePoint.toVector().multiply(scaleFactor);

        // if a portal is available, teleport to it; otherwise, create a new portal and teleport to it
        Vector dest = destinationPortals.stream()
                .map(portalLocation -> {
                    Vector heightAdjustedLocation = portalLocation.clone().setY(scaledReferencePoint.getY());
                    double distance = scaledReferencePoint.distance(heightAdjustedLocation);

                    // map around an entry - we want to compare based on distance but return the original object
                    return new AbstractMap.SimpleEntry<>(portalLocation, distance);
                })
                .filter(entry -> entry.getValue() < maxDistance)
                .min(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
                .map(AbstractMap.SimpleEntry::getKey)
                .orElse(createNewPortal(destinationWorld, scaledReferencePoint, entity));

        // may be null due to other event cancellations
        if (dest != null)
            entity.teleport(new Location(destinationWorld, dest.getX(), dest.getY(), dest.getZ()));
    }

    /**
     * Attempt to create a portal as close to the destination as possible.
     * May fail in special cases (e.g.: a player not having perms to build at the destination).
     */
    public Vector createNewPortal(World destinationWorld, Vector destination, Entity entity) {
        return null;
    }

    /**
     * Get the bottommost and northwesternmost portal block that is part of the portal the entity is touching.
     * This is a reference point for the said portal, to use while computing coordinate scaling.
     *
     * Returns {@code null} if the entity is not touching a portal, or a portal frame couldn't be found.
     */
    public Location getReferencePoint(Entity entity) {
        World world = entity.getWorld();
        Location entityLocation = entity.getLocation();

        BoundingBox boundingBox = entity.getBoundingBox();
        Optional<Location> touchingPortal =
                Util.getLocationsBetween(
                        boundingBox.getMin(),
                        boundingBox.getMax(),
                        world)
                .stream()
                .filter(location -> world.getBlockAt(location).getType() == Material.NETHER_PORTAL)
                .min(Comparator.comparingDouble(entityLocation::distance));

        // not touching a nether portal
        if (touchingPortal.isEmpty())
            return null;

        Location touchingPortalLocation = touchingPortal.get();

        Location bottom = Util.findFrameLimit(
                touchingPortalLocation,
                VectorAxis.NY,
                Collections.singleton(Material.NETHER_PORTAL),
                plugin.getConfig().getInt(ConfigKey.PORTAL_HEIGHT_MAX)
        );

        if (bottom == null)
            return null;

        // look for the northwesternmost nether portal block (a convention, could've been southeasternmost)
        BlockData blockData = touchingPortalLocation.getBlock().getBlockData();
        if (!(blockData instanceof Orientable))
            return null;

        // bottom "left" corner, returns null if not found, just as intended
        return Util.findFrameLimit(
                bottom,
                ((Orientable) blockData).getAxis() == Axis.X ? VectorAxis.NX : VectorAxis.NZ,
                Collections.singleton(Material.NETHER_PORTAL),
                plugin.getConfig().getInt(ConfigKey.PORTAL_WIDTH_MAX)
        );
    }

    private double getOverworldToNetherFactor() {
        return plugin.getConfig().getDouble(ConfigKey.NETHER_RATIO) /
                plugin.getConfig().getDouble(ConfigKey.OVERWORLD_RATIO);
    }

    private double getNetherToOverworldFactor() {
        return plugin.getConfig().getDouble(ConfigKey.OVERWORLD_RATIO) /
                plugin.getConfig().getDouble(ConfigKey.NETHER_RATIO);
    }

}

package ro.cofi.netherratio.logic;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import ro.cofi.netherratio.NetherRatio;
import ro.cofi.netherratio.event.CustomEntityTeleportEvent;
import ro.cofi.netherratio.event.CustomPlayerTeleportEvent;
import ro.cofi.netherratio.event.CustomPortalCreateEvent;
import ro.cofi.netherratio.misc.ConfigKey;
import ro.cofi.netherratio.misc.Constants;
import ro.cofi.netherratio.misc.LocationUtil;
import ro.cofi.netherratio.misc.VectorAxis;

import java.util.*;

public class PortalLogicManager {

    private static final int BACKUP_NETHER_CEILING_LEVEL = 128;

    private final NetherRatio plugin;

    private final World overworld;
    private final World nether;

    private final int netherBedrockCeiling;

    public PortalLogicManager(NetherRatio plugin) {
        this.plugin = plugin;

        // find the overworld and the nether
        List<World> serverWorlds = plugin.getServer().getWorlds();

        nether = serverWorlds.stream()
                .filter(world -> world.getEnvironment() == World.Environment.NETHER)
                .findFirst()
                .orElse(null);

        overworld = serverWorlds.stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(null);

        // obtain the nether bedrock ceiling Y level
        if (nether == null) {
            netherBedrockCeiling = BACKUP_NETHER_CEILING_LEVEL; // last resort, should not happen
            plugin.getLogger().severe(plugin.prefixMessage("Could not find the nether world. " +
                    "Using default value for nether ceiling Y level: " + BACKUP_NETHER_CEILING_LEVEL));
            return;
        }

        Location checkedLocation = new Location(nether, 0, nether.getMaxHeight(), 0);

        while (checkedLocation.getY() >= 0 && checkedLocation.getBlock().getType() != Material.BEDROCK)
            checkedLocation.subtract(VectorAxis.Y);

        // not found, should not happen
        if (checkedLocation.getY() == -1) {
            netherBedrockCeiling = BACKUP_NETHER_CEILING_LEVEL;
            plugin.getLogger().severe(plugin.prefixMessage("Could not find the Y level of the nether ceiling. " +
                    "Using default value: " + BACKUP_NETHER_CEILING_LEVEL));
            return;
        }

        netherBedrockCeiling = checkedLocation.getBlockY();
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
        World destinationWorld;
        double maxDistance;
        double scaleFactor;

        // handle teleport direction
        if (entity.getWorld().getEnvironment() == World.Environment.NORMAL) {
            maxDistance = plugin.getConfig().getDouble("min_distance_between_portals.nether");
            scaleFactor = getOverworldToNetherFactor();
            destinationWorld = nether;
        } else {
            maxDistance = plugin.getConfig().getDouble("min_distance_between_portals.overworld");
            scaleFactor = getNetherToOverworldFactor();
            destinationWorld = overworld;
        }

        List<Vector> destinationPortals = plugin.getPortalLocationManager().getPortals(destinationWorld);

        // scale X and Z, but interpolate Y
        Vector scaledReferencePoint = referencePoint.toVector().multiply(scaleFactor);
        scaledReferencePoint.setY(LocationUtil.mapInterval(
                referencePoint.getWorld().getMinHeight(),
                referencePoint.getWorld().getMaxHeight(),
                destinationWorld.getMinHeight(),
                Objects.equals(destinationWorld, nether) ? netherBedrockCeiling : destinationWorld.getMaxHeight(),
                referencePoint.getY()
        ));

        // if a portal is available, teleport to it; otherwise, create a new portal and teleport to it
        Location destination = destinationPortals.stream()
                .map(portalLocation -> {
                    double distance = scaledReferencePoint.clone()
                            .setY(portalLocation.getY())
                            .distance(portalLocation);

                    // map around an entry - we want to compare based on distance but return the original object
                    return new AbstractMap.SimpleEntry<>(portalLocation, distance);
                })
                .filter(entry -> entry.getValue() < maxDistance)
                .min(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
                .map(AbstractMap.SimpleEntry::getKey)
                .map(vec -> LocationUtil.fromVector(destinationWorld, vec))
                .orElseGet(() -> createNewPortal(
                        LocationUtil.fromVector(destinationWorld, scaledReferencePoint),
                        entity,
                        ((Orientable) referencePoint.getBlock().getBlockData()).getAxis())
                );

        // may be null due to other event cancellations
        if (destination == null)
            return;

        // center on the Z and X axis
        double originalY = destination.getY();
        destination = destination.toCenterLocation();
        destination.setY(originalY);

        // fire an event and check for cancellation
        Event event = entity instanceof Player ?
                new CustomPlayerTeleportEvent(
                        (Player) entity,
                        entity.getLocation(),
                        destination,
                        PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                ) :
                new CustomEntityTeleportEvent(
                        entity,
                        entity.getLocation(),
                        destination
                );

        Bukkit.getPluginManager().callEvent(event);
        if (((Cancellable) event).isCancelled())
            return;

        // keep the entity's orientation
        destination.setYaw(entity.getLocation().getYaw());
        destination.setPitch(entity.getLocation().getPitch());

        entity.teleport(destination);
    }

    /**
     * Attempt to create a portal as close to the destination as possible.
     * May fail in special cases (e.g.: a player not having perms to build at the destination)./
     */
    private Location createNewPortal(Location desiredDestination, Entity entity, Axis preferredAxis) {
        // only players may create portals
        if (!(entity instanceof Player))
            return null;

        // perform vanilla checks - search for a simple, empty location, atop a floor of buildable blocks
        SearchData searchData = findValidLocation(desiredDestination, preferredAxis, true);

        // remove the need for a floor - may generate in the air
        if (searchData == null)
            searchData = findValidLocation(desiredDestination, preferredAxis, false);

        // force a portal at the location, wherever it may happen to generate, and overwrite whatever is there
        if (searchData == null)
            searchData = new SearchData(desiredDestination, preparePortalBlocks(
                    desiredDestination,
                    preferredAxis,
                    true)
            );

        List<PortalBlockData> portalBlocks = searchData.getPortalBlockData();

        List<BlockState> eventBlocks = portalBlocks.stream()
                .map(data -> data.getLocation().getBlock().getState())
                .toList();

        // fire an event and check if it has been cancelled by anything else
        CustomPortalCreateEvent event = new CustomPortalCreateEvent(
                eventBlocks,
                desiredDestination.getWorld(),
                entity,
                PortalCreateEvent.CreateReason.NETHER_PAIR
        );

        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return null;

        // the event has not been cancelled, create the portal
        for (PortalBlockData data : portalBlocks)
            data.getLocation().getBlock().setBlockData(data.getBlockData());

        // save this table for lookup
        plugin.getPortalLocationManager().savePortal(searchData.getLocation());

        return searchData.getLocation();
    }

    /**
     * Find a volume of replaceable blocks (mostly air). Attempt to searching in a cylinder around the desired
     * destination, expanding the search in a spherical manner to ensure that the result is the closest available
     * location.
     */
    private SearchData findValidLocation(Location desiredDestination, Axis preferredAxis, boolean mustHaveFloor) {
        Set<Location> checked = new HashSet<>();
        Queue<Location> toCheck = new LinkedList<>();

        int maxHorizontalOffset = plugin.getConfig().getInt("max_portal_placement_offset.horizontal");
        int maxVerticalOffset = plugin.getConfig().getInt("max_portal_placement_offset.vertical");
        int portalHeight = getNewPortalHeight();

        Vector originVec = desiredDestination.toVector();
        World originWorld = desiredDestination.getWorld();
        Axis otherAxis = preferredAxis == Axis.X ? Axis.Z : Axis.X;

        // start from the desired destination, and expand the search in a spherical manner
        toCheck.offer(desiredDestination);

        while (!toCheck.isEmpty()) {
            Location current = toCheck.poll();

            // skip already checked coordinates
            if (checked.contains(current))
                continue;

            checked.add(current);

            // keep within horizontal bounds
            if (current.toVector().setY(originVec.getY()).distance(originVec) > maxHorizontalOffset)
                continue;

            // keep within world bounds
            if (current.getY() < originWorld.getMinHeight() + 1 ||
                    current.getY() > originWorld.getMaxHeight() - (portalHeight + 1))
                continue;

            // keep within vertical bounds
            if (Math.abs(current.getY() - originVec.getY()) > maxVerticalOffset)
                continue;

            // a check shall be performed - add adjacent locations for future processing
            for (int x = -1; x <= 1; x++)
                for (int y = -1; y <= 1; y++)
                    for (int z = -1; z <= 1; z++)
                        toCheck.add(current.clone().add(x, y, z));

            // stop when a valid location has been found, otherwise continue searching
            SearchData data = isValidLocation(current, preferredAxis, mustHaveFloor);
            if (data != null)
                return data;

            // attempt the other orientation
            data = isValidLocation(current, otherAxis, mustHaveFloor);
            if (data != null)
                return data;
        }

        return null;
    }

    /**
     * Attempt to form a portal at this location.
     * The portal must have a floor below it made out of buildable blocks.
     * If there are blocks that a portal can't replace, the location is invalid.
     */
    private SearchData isValidLocation(Location location, Axis axis, boolean mustHaveFloor) {
        Vector direction = axis == Axis.X ? VectorAxis.X : VectorAxis.Z;
        Vector sideDirection = axis == Axis.X ? VectorAxis.Z : VectorAxis.X;

        int portalWidth = getMinPortalWidth();

        // check floor below volume
        boolean hasFloor = !mustHaveFloor || LocationUtil.getLocationsBetween(
                location.clone()
                        .subtract(sideDirection)
                        .subtract(VectorAxis.Y.clone().multiply(2)),
                location.clone()
                        .add(sideDirection)
                        .add(direction.clone().multiply(portalWidth - 1))
                        .subtract(VectorAxis.Y.clone().multiply(2))
        ).stream().map(Location::getBlock).allMatch(Block::isBuildable);

        if (!hasFloor)
            return null;

        // check volume
        List<PortalBlockData> blocks = preparePortalBlocks(location, axis, !mustHaveFloor);

        // if a block already exists in the world, it is good; if it doesn't, the existing block must be replaceable
        for (PortalBlockData data : blocks) {
            Block existingBlock = data.getLocation().getBlock();

            if (existingBlock.getBlockData().equals(data.getBlockData()))
                continue; // already exists in the world

            if (Constants.REPLACEABLE_BLOCKS.contains(existingBlock.getType()))
                continue; // is replaceable

            return null; // a block is neither existing nor replaceable, stop
        }

        // every block is good
        return new SearchData(location, blocks);
    }

    /**
     * Create portal blocks in memory (don't place in world).
     *
     * Will include the nether portal blocks (inside the frame), the frame (out of crying obsidian),
     * and, possibly, auxiliary blocks to serve as a platform for the player to step on when exiting
     * the portal, (also out of crying obsidian).
     */
    private List<PortalBlockData> preparePortalBlocks(Location referencePoint, Axis axis, boolean generatePlatform) {
        List<PortalBlockData> data = new ArrayList<>();

        int portalHeight = getNewPortalHeight();
        int portalWidth = getNewPortalWidth();

        Vector direction = axis == Axis.X ? VectorAxis.X : VectorAxis.Z;
        Vector sideDirection = axis == Axis.X ? VectorAxis.Z : VectorAxis.X;

        // create the frame
        List<Location> frameBlocks = LocationUtil.getLocationsBetween(
                referencePoint.clone().subtract(direction).subtract(VectorAxis.Y),
                referencePoint.clone()
                        .add(direction.clone().multiply(portalWidth))
                        .add(VectorAxis.Y.clone().multiply(portalHeight))
        );

        BlockData frame = Constants.FRAME_BLOCK.createBlockData();
        BlockData air = Material.AIR.createBlockData();

        for (Location location : frameBlocks)
            data.add(new PortalBlockData(location, frame));

        // create auxiliary blocks to allow the player to step on when exiting the portal
        if (generatePlatform) {
            for (int horizontal = 0; horizontal < portalWidth; horizontal++) {
                Location horizontalReference = referencePoint.clone()
                        .subtract(VectorAxis.Y)
                        .add(direction.clone().multiply(horizontal));

                Location left = horizontalReference.clone().subtract(sideDirection);
                Location right = horizontalReference.clone().add(sideDirection);

                data.add(new PortalBlockData(left, frame));
                data.add(new PortalBlockData(right, frame));

                // add air blocks above these, as high as the portal
                for (int vertical = 1; vertical < portalHeight + 1; vertical++) {
                    Vector verticalOffset = VectorAxis.Y.clone().multiply(vertical);

                    data.add(new PortalBlockData(left.clone().add(verticalOffset), air));
                    data.add(new PortalBlockData(right.clone().add(verticalOffset), air));
                }
            }
        }

        // create the inner portal blocks
        List<Location> innerBlocks = LocationUtil.getLocationsBetween(
                referencePoint,
                referencePoint.clone()
                        .add(direction.clone().multiply(portalWidth - 1))
                        .add(VectorAxis.Y.clone().multiply(portalHeight - 1))
        );

        Orientable blockData = (Orientable) Material.NETHER_PORTAL.createBlockData();
        blockData.setAxis(axis);

        for (Location location : innerBlocks)
            data.add(new PortalBlockData(location, blockData));

        return data;
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
                LocationUtil.getLocationsBetween(
                        boundingBox.getMin(),
                        boundingBox.getMax(),
                        world)
                .stream()
                .filter(location -> world.getBlockAt(location).getType() == Material.NETHER_PORTAL)
                .min(Comparator.comparingDouble(entityLocation::distance));

        // not touching a nether portal
        if (touchingPortal.isEmpty())
            return null;

        return getReferencePoint(touchingPortal.get());
    }

    /**
     * Get the bottommost and northwesternmost portal block that is part of the same portal as the given location.
     * This is a reference point for the said portal, to use while computing coordinate scaling.
     *
     * Returns {@code null} if a portal frame couldn't be found.
     */
    public Location getReferencePoint(Location portalBlockLocation) {
        Location bottom = LocationUtil.findFrameLimit(
                portalBlockLocation,
                VectorAxis.NY,
                Collections.singleton(Material.NETHER_PORTAL),
                getMaxPortalHeight()
        );

        if (bottom == null)
            return null;

        // look for the northwesternmost nether portal block (a convention, could've been southeasternmost)
        BlockData blockData = portalBlockLocation.getBlock().getBlockData();
        if (!(blockData instanceof Orientable))
            return null;

        // bottom "left" corner, returns null if not found, just as intended
        return LocationUtil.findFrameLimit(
                bottom,
                ((Orientable) blockData).getAxis() == Axis.X ? VectorAxis.NX : VectorAxis.NZ,
                Collections.singleton(Material.NETHER_PORTAL),
                getMaxPortalWidth()
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

    private int getMinPortalHeight() {
        return plugin.getConfig().getInt(ConfigKey.PORTAL_HEIGHT_MIN);
    }

    private int getMaxPortalHeight() {
        return plugin.getConfig().getInt(ConfigKey.PORTAL_HEIGHT_MAX);
    }

    private int getNewPortalHeight() {
        return plugin.getConfig().getInt(ConfigKey.PORTAL_HEIGHT_NEW);
    }

    private int getMinPortalWidth() {
        return plugin.getConfig().getInt(ConfigKey.PORTAL_WIDTH_MIN);
    }

    private int getMaxPortalWidth() {
        return plugin.getConfig().getInt(ConfigKey.PORTAL_WIDTH_MAX);
    }

    private int getNewPortalWidth() {
        return plugin.getConfig().getInt(ConfigKey.PORTAL_WIDTH_NEW);
    }

    private record PortalBlockData(Location location, BlockData blockData) {

        public Location getLocation() {
            return location;
        }

        public BlockData getBlockData() {
            return blockData;
        }
    }

    private record SearchData(Location location, List<PortalBlockData> data) {

        public Location getLocation() {
            return location;
        }

        public List<PortalBlockData> getPortalBlockData() {
            return data;
        }
    }

}

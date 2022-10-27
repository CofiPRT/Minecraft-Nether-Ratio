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
import ro.cofi.netherratio.misc.Constants;
import ro.cofi.netherratio.misc.LocationUtil;
import ro.cofi.netherratio.misc.VectorAxis;

import java.util.*;

public class PortalLogicManager {

    private static final int BACKUP_NETHER_CEILING_LEVEL = 128;
    private static final int NON_PLAYER_ENTITY_PORTAL_CD = 10; // in ticks, good enough as it is, no need to change
    private static final int PREFERRED_OVERWORLD_Y = 64;

    private static final double VANILLA_RATIO_OVERWORLD = 8;
    private static final double VANILLA_RATIO_NETHER = 1;
    private static final double VANILLA_MIN_DISTANCE_BETWEEN_PORTALS_OVERWORLD = 128;
    private static final double VANILLA_MIN_DISTANCE_BETWEEN_PORTALS_NETHER = 16;

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
                                                           "Using default value for nether ceiling Y level: " +
                                                           BACKUP_NETHER_CEILING_LEVEL));
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
     * <br><br>
     * A search for an existing portal is performed in a specific radius around the usual, precomputed destination.
     * If no such portal exists, it is created at the precomputed destination (or as close to it).
     * <br><br>
     * If a portal is available (either already existing or following its creation), the entity is teleported.
     */
    public void handleEntityTeleport(Entity entity, ReferencePoint referencePoint) {
        Location referenceLocation = referencePoint.location();
        boolean isCustom = referencePoint.isCustom();

        // check if there is an existing portal to teleport to
        World destinationWorld;
        double maxDistance;
        double scaleFactor;
        double preferredY;

        // handle teleport direction
        if (entity.getWorld().getEnvironment() == World.Environment.NORMAL) {
            if (isCustom) {
                maxDistance = plugin.getConfigManager().getMinDistanceBetweenPortalsNether();
                scaleFactor = plugin.getConfigManager().getRatioNether() /
                              plugin.getConfigManager().getRatioOverworld();
            } else {
                maxDistance = VANILLA_MIN_DISTANCE_BETWEEN_PORTALS_NETHER;
                scaleFactor = VANILLA_RATIO_NETHER / VANILLA_RATIO_OVERWORLD;
            }

            destinationWorld = nether;

            // interpolate overworld height to nether height, not higher than the nether bedrock ceiling
            preferredY = LocationUtil.mapInterval(
                overworld.getMinHeight(),
                overworld.getMaxHeight(),
                nether.getMinHeight(),
                netherBedrockCeiling,
                referenceLocation.getY()
            );
        } else {
            if (isCustom) {
                maxDistance = plugin.getConfigManager().getMinDistanceBetweenPortalsOverworld();
                scaleFactor = plugin.getConfigManager().getRatioOverworld() /
                              plugin.getConfigManager().getRatioNether();
            } else {
                maxDistance = VANILLA_MIN_DISTANCE_BETWEEN_PORTALS_OVERWORLD;
                scaleFactor = VANILLA_RATIO_OVERWORLD / VANILLA_RATIO_NETHER;
            }

            destinationWorld = overworld;

            // fixed Y for overworld
            preferredY = PREFERRED_OVERWORLD_Y;
        }

        // scale X and Z
        Vector scaledReferencePoint = referenceLocation.toVector().multiply(scaleFactor);
        scaledReferencePoint.setX(Math.floor(scaledReferencePoint.getX()));
        scaledReferencePoint.setY(preferredY);
        scaledReferencePoint.setZ(Math.floor(scaledReferencePoint.getZ()));

        List<Vector> destinationPortals = plugin.getPortalLocationManager().getPortals(destinationWorld, isCustom);

        // if a portal is available, teleport to it; otherwise, create a new portal and teleport to it
        Location destination = destinationPortals.stream()
            .map(portalLocation -> {
                double distance = scaledReferencePoint.clone()
                    .setY(portalLocation.getY())
                    .distance(portalLocation);

                // map around an entry - we want to compare based on distance but return the original object
                return new AbstractMap.SimpleEntry<>(portalLocation, distance);
            })
            .filter(entry -> entry.getValue() < maxDistance + scaleFactor) // accept small errors (the scale factor)
            .min(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
            .map(AbstractMap.SimpleEntry::getKey)
            .map(vec -> LocationUtil.fromVector(destinationWorld, vec))
            .orElseGet(() -> createNewPortal(
                LocationUtil.fromVector(destinationWorld, scaledReferencePoint),
                entity,
                ((Orientable) referenceLocation.getBlock().getBlockData()).getAxis(),
                isCustom
            ));

        // may be null due to other event cancellations
        if (destination == null)
            return;

        // adjust the destination based on the entity's position inside the portal, and its hitbox
        destination = adjustDestination(entity, referenceLocation, destination);

        // something bad happened, abort
        if (destination == null)
            return;

        // fire an event and check for cancellation
        Event event = entity instanceof Player player ?
                      new CustomPlayerTeleportEvent(
                          player,
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

        entity.teleport(destination);

        // non-player entities need a portal cooldown to avoid being in a constant teleportation loop
        if (entity instanceof Player player) {
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.AMBIENT, 0.25f, 1);
        } else {
            entity.setPortalCooldown(NON_PLAYER_ENTITY_PORTAL_CD);
            entity.teleport(destination); // teleport again, for the Spigot API can't teleport precisely between worlds
        }
    }

    /**
     * Attempt to create a portal as close to the destination as possible.
     * May fail in special cases (e.g.: a player not having perms to build at the destination).
     */
    private Location createNewPortal(Location desiredDestination, Entity entity, Axis preferredAxis, boolean isCustom) {
        // only players may create portals
        if (!(entity instanceof Player))
            return null;

        // perform vanilla checks - search for a simple, empty location, atop a floor of buildable blocks
        SearchData searchData = findValidLocation(desiredDestination, preferredAxis, true, isCustom);

        // remove the need for a floor - may generate in the air
        if (searchData == null && plugin.getConfigManager().isFloatingPlacementAllowed())
            searchData = findValidLocation(desiredDestination, preferredAxis, false, isCustom);

        // force a portal at the location, wherever it may happen to generate, and overwrite whatever is there
        if (searchData == null && plugin.getConfigManager().isForcedPlacementAllowed())
            searchData = new SearchData(desiredDestination, preparePortalBlocks(
                desiredDestination,
                preferredAxis,
                true,
                isCustom
            ));

        // if after all of this we still don't have a portal, stop
        if (searchData == null)
            return null;

        List<PortalBlockData> portalBlocks = searchData.data();

        List<BlockState> eventBlocks = portalBlocks.stream()
            .map(data -> data.location().getBlock().getState())
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
            data.location().getBlock().setBlockData(data.blockData());

        // save this table for lookup
        plugin.getPortalLocationManager().savePortal(searchData.location(), isCustom);

        return searchData.location();
    }

    /**
     * Find a volume of replaceable blocks (mostly air). Attempt to searching in a cylinder around the desired
     * destination, expanding the search in a spherical manner to ensure that the result is the closest available
     * location.
     */
    private SearchData findValidLocation(
        Location desiredDestination, Axis preferredAxis,
        boolean mustHaveFloor, boolean isCustom
    ) {
        Set<Location> checked = new HashSet<>();
        Queue<Location> toCheck = new LinkedList<>();

        int maxHorizontalOffset = plugin.getConfigManager().getMaxPortalPlacementOffsetHorizontal();
        int maxVerticalOffset = plugin.getConfigManager().getMaxPortalPlacementOffsetVertical();
        int portalHeight = plugin.getConfigManager().getPortalSizeHeightNew();

        Vector originVec = desiredDestination.toVector();
        World originWorld = desiredDestination.getWorld();
        Axis otherAxis = preferredAxis == Axis.X ? Axis.Z : Axis.X;

        // don't generate portals above the nether ceiling
        double minY = originWorld.getMinHeight() + 1d;
        double maxY = (originWorld == nether ? netherBedrockCeiling : originWorld.getMaxHeight()) - (portalHeight + 1d);

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
            if (current.getY() < minY || current.getY() > maxY)
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
            SearchData data = isValidLocation(current, preferredAxis, mustHaveFloor, isCustom);
            if (data != null)
                return data;

            // attempt the other orientation
            data = isValidLocation(current, otherAxis, mustHaveFloor, isCustom);
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
    private SearchData isValidLocation(Location location, Axis axis, boolean mustHaveFloor, boolean isCustom) {
        Vector direction = VectorAxis.of(axis);
        Vector sideDirection = VectorAxis.ofSide(axis);

        int portalWidth = plugin.getConfigManager().getPortalSizeWidthNew();

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
        List<PortalBlockData> blocks = preparePortalBlocks(location, axis, !mustHaveFloor, isCustom);

        // if a block already exists in the world, it is good; if it doesn't, the existing block must be replaceable
        for (PortalBlockData data : blocks) {
            Block existingBlock = data.location().getBlock();

            if (existingBlock.getBlockData().equals(data.blockData()))
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
     * <br><br>
     * Will include the nether portal blocks (inside the frame), the frame (out of crying obsidian),
     * and, possibly, auxiliary blocks to serve as a platform for the player to step on when exiting
     * the portal, (also out of crying obsidian).
     */
    private List<PortalBlockData> preparePortalBlocks(
        Location referencePoint, Axis axis,
        boolean generatePlatform, boolean isCustom
    ) {
        List<PortalBlockData> data = new ArrayList<>();

        int portalHeight = plugin.getConfigManager().getPortalSizeHeightNew();
        int portalWidth = plugin.getConfigManager().getPortalSizeWidthNew();

        Vector direction = VectorAxis.of(axis);
        Vector sideDirection = VectorAxis.ofSide(axis);

        // create the frame
        List<Location> frameBlocks = LocationUtil.getLocationsBetween(
            referencePoint.clone().subtract(direction).subtract(VectorAxis.Y),
            referencePoint.clone()
                .add(direction.clone().multiply(portalWidth))
                .add(VectorAxis.Y.clone().multiply(portalHeight))
        );

        BlockData frame = isCustom ? plugin.getConfigManager().getFrameBlock().createBlockData() :
                          Constants.VANILLA_FRAME_BLOCK.createBlockData();
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
     * The vanilla behavior of the teleportation is to teleport relative to the horizontal position of the entity
     * in regard to the portal's width. For example, if the entity enters the portal through its leftmost side,
     * it should be teleported to the leftmost side of the destination portal. If it enters through the very center,
     * exit through the very center, etc.
     * <br><br>
     * This method ensures that happens. It also handles the case where the axis of the destination portal is different.
     */
    private Location adjustDestination(Entity entity, Location initialLocation, Location destination) {
        Axis initialAxis = ((Orientable) initialLocation.getBlock().getBlockData()).getAxis();
        Axis destinationAxis = ((Orientable) destination.getBlock().getBlockData()).getAxis();

        Set<Material> innerBlocks = Collections.singleton(Material.NETHER_PORTAL);
        int maxWidth = plugin.getConfigManager().getPortalSizeWidthMax();

        // find the other wall of the portal - we know for sure the initial location is next to one of the walls
        ReferencePoint limit = findFrameLimit(
            initialLocation,
            VectorAxis.of(initialAxis),
            innerBlocks,
            maxWidth
        );

        // something terribly bad has happened, nobody knows what, stop - shouldn't happen
        if (limit == null)
            return null;

        // find where the entity is inside the portal, relative to its width
        double positionFactor = initialAxis == Axis.X ?
                                LocationUtil.mapInterval(
                                    initialLocation.getX(), limit.location().getX() + 1,
                                    0, 1,
                                    entity.getLocation().getX()
                                ) :
                                LocationUtil.mapInterval(
                                    initialLocation.getZ(), limit.location().getZ() + 1,
                                    0, 1,
                                    entity.getLocation().getZ()
                                );

        // find the other wall of the portal - for the destination
        ReferencePoint destinationLimit = findFrameLimit(
            destination,
            VectorAxis.of(destinationAxis),
            innerBlocks,
            maxWidth
        );

        // how is this even possible
        if (destinationLimit == null)
            return null;

        double adjustedCoordinate = destinationAxis == Axis.X ?
                                    LocationUtil.mapInterval(
                                        0, 1,
                                        destination.getX(), destinationLimit.location().getX() + 1,
                                        positionFactor
                                    ) :
                                    LocationUtil.mapInterval(
                                        0, 1,
                                        destination.getZ(), destinationLimit.location().getZ() + 1,
                                        positionFactor
                                    );

        Location adjustedLocation = destination.clone();

        // fit the entity's hitbox inside the portal to avoid suffocation
        if (destinationAxis == Axis.X) {
            adjustedLocation.setX(adjustedCoordinate);
            adjustedLocation.add(0, 0, 0.5); // center on Z axis

            double widthOffset = entity.getBoundingBox().getWidthX() / 2;

            // fit hitbox
            adjustedLocation.setX(Math.max(
                adjustedLocation.getX(),
                destination.getX() + widthOffset
            ));
            adjustedLocation.setX(Math.min(
                adjustedLocation.getX(),
                destinationLimit.location().getX() + 1 - widthOffset
            ));
        } else {
            adjustedLocation.setZ(adjustedCoordinate);
            adjustedLocation.add(0.5, 0, 0); // center on X axis

            double widthOffset = entity.getBoundingBox().getWidthZ() / 2;

            // fit hitbox
            adjustedLocation.setZ(Math.max(
                adjustedLocation.getZ(),
                destination.getZ() + widthOffset
            ));
            adjustedLocation.setZ(Math.min(
                adjustedLocation.getZ(),
                destinationLimit.location().getZ() + 1 - widthOffset
            ));
        }

        // the destination should match the orientation of the entity
        adjustedLocation.setPitch(entity.getLocation().getPitch());
        adjustedLocation.setYaw(entity.getLocation().getYaw());

        // if the destination portal has a different axis, rotate 90 degrees
        if (initialAxis != destinationAxis)
            adjustedLocation.setYaw(adjustedLocation.getYaw() + 90);

        return adjustedLocation;
    }

    /**
     * Get the bottommost and northwesternmost portal block that is part of the portal the entity is touching.
     * This is a reference point for the said portal, to use while computing coordinate scaling.
     * <br><br>
     * Returns {@code null} if the entity is not touching a portal, or a portal frame couldn't be found.
     */
    public ReferencePoint getReferencePoint(Entity entity) {
        World world = entity.getWorld();
        Location entityLocation = entity.getLocation();

        BoundingBox boundingBox = entity.getBoundingBox();
        Optional<Location> touchingPortal =
            LocationUtil.getLocationsBetween(
                    boundingBox.getMin(),
                    boundingBox.getMax(),
                    world
                )
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
     * <br><br>
     * Returns {@code null} if a portal frame couldn't be found.
     */
    public ReferencePoint getReferencePoint(Location portalBlockLocation) {
        ReferencePoint bottom = findFrameLimit(
            portalBlockLocation,
            VectorAxis.NY,
            Collections.singleton(Material.NETHER_PORTAL),
            plugin.getConfigManager().getPortalSizeHeightMax()
        );

        if (bottom == null)
            return null;

        // look for the northwesternmost nether portal block (a convention, could've been southeasternmost)
        BlockData blockData = portalBlockLocation.getBlock().getBlockData();
        if (!(blockData instanceof Orientable))
            return null;

        // bottom "left" corner, returns null if not found, just as intended
        return findFrameLimit(
            bottom.location(),
            VectorAxis.of(((Orientable) blockData).getAxis()).clone().multiply(-1),
            Collections.singleton(Material.NETHER_PORTAL),
            plugin.getConfigManager().getPortalSizeWidthMax()
        );
    }

    /**
     * Start traversing from an original location towards a specific direction until encountering a frame block.
     * Return the last location before encountering said block.
     */
    public ReferencePoint findFrameLimit(Location origin, Vector direction, Set<Material> innerBlocks, int maxOffset) {
        World world = origin.getWorld();

        for (int offset = 0; offset < maxOffset; offset++) {
            Location target = origin.clone().add(direction.clone().multiply(offset + 1));
            Material targetBlockType = world.getBlockAt(target).getType();

            // look for the frame
            if (targetBlockType == plugin.getConfigManager().getFrameBlock())
                return new ReferencePoint(target.subtract(direction), true);
            else if (targetBlockType == Constants.VANILLA_FRAME_BLOCK)
                return new ReferencePoint(target.subtract(direction), false);

            // a non-replaceable block has been reached, and it's not a valid frame block
            if (!innerBlocks.contains(targetBlockType))
                return null;
        }

        // no limit found within the bounds
        return null;
    }

    private record PortalBlockData(Location location, BlockData blockData) { }

    private record SearchData(Location location, List<PortalBlockData> data) { }

}

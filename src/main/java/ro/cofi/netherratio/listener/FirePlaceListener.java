package ro.cofi.netherratio.listener;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;
import ro.cofi.netherratio.NetherRatio;
import ro.cofi.netherratio.logic.ReferencePoint;
import ro.cofi.netherratio.misc.Constants;
import ro.cofi.netherratio.misc.LocationUtil;
import ro.cofi.netherratio.misc.VectorAxis;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FirePlaceListener extends AbstractListener {

    private static final Map<Material, Sound> TRIGGER_ITEMS = new EnumMap<>(Material.class);

    static {
        TRIGGER_ITEMS.put(Material.FLINT_AND_STEEL, Sound.ITEM_FLINTANDSTEEL_USE);
        TRIGGER_ITEMS.put(Material.FIRE_CHARGE, Sound.ITEM_FIRECHARGE_USE);
    }

    public FirePlaceListener(NetherRatio plugin) {
        super(plugin);
    }

    /**
     * Only handle cases where the player is using an item that can light up a portal on a frame block.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFirePlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;

        // check valid dimension
        World world = clickedBlock.getWorld();
        if (!Constants.VALID_ENVIRONMENTS.contains(world.getEnvironment()))
            return;

        // only care about frame blocks
        if (clickedBlock.getType() != plugin.getConfigManager().getFrameBlock() &&
            clickedBlock.getType() != Constants.VANILLA_FRAME_BLOCK)
            return;

        // only care about items you can light up a portal with
        ItemStack item = event.getItem();
        if (item == null)
            return;

        if (!TRIGGER_ITEMS.containsKey(item.getType()))
            return;

        Block litUpBlock = clickedBlock.getLocation().add(event.getBlockFace().getDirection()).getBlock();

        PortalFrameData frameData = computePortalBlocks(litUpBlock, world);
        if (frameData == null)
            return;

        // these locations must be replaceable by a literal portal block
        for (Location location : frameData.getInnerLocations())
            if (!Constants.REPLACEABLE_BLOCKS.contains(world.getBlockAt(location).getType()))
                return;

        Player player = event.getPlayer();

        // cancel the event to not interfere with item consumption - manually consume it
        event.setCancelled(true);
        world.playSound(litUpBlock.getLocation(), TRIGGER_ITEMS.get(item.getType()), SoundCategory.BLOCKS, 1, 1);
        consumeItem(player, item);

        // everything is validated, perform the placement
        Orientable blockData = (Orientable) Material.NETHER_PORTAL.createBlockData();
        blockData.setAxis(frameData.getHorizontalAxis());

        for (Location location : frameData.getInnerLocations())
            location.getBlock().setBlockData(blockData);

        // save for lookups
        plugin.getPortalLocationManager().savePortal(frameData.getBottomLeft(), frameData.isCustom());
    }

    /**
     * Either decrements durability by 1 or the stack count by 1.
     */
    private void consumeItem(Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE)
            return;

        // decrement stack
        if (item.getType().getMaxDurability() == 0) {
            item.subtract(1);
            return;
        }

        // decrement durability
        Damageable itemMeta = (Damageable) item.getItemMeta();
        itemMeta.setDamage(itemMeta.getDamage() + 1);
        item.setItemMeta(itemMeta);
    }

    /**
     * From a starting block, attempt to find a portal frame to place portal blocks inside.
     * Attempts to find the top, bottom, left, and right limits of the frame, the last 2 being in the same axis.
     * If all of them are found, these 4 locations identify a rectangle. All the blocks inside this rectangle
     * must be replaceable, for we intend to place portal blocks inside.
     */
    private PortalFrameData computePortalBlocks(Block blockPlaced, World world) {
        Location origin = blockPlaced.getLocation().toBlockLocation();

        int minHeight = plugin.getConfigManager().getPortalSizeHeightMin();
        int maxHeight = plugin.getConfigManager().getPortalSizeHeightMax();
        int minWidth = plugin.getConfigManager().getPortalSizeWidthMin();
        int maxWidth = plugin.getConfigManager().getPortalSizeWidthMax();

        // find vertical limits
        ReferencePoint bottom = plugin.getPortalLogicManager().findFrameLimit(
                origin, VectorAxis.NY,Constants.REPLACEABLE_BLOCKS, maxHeight
        );
        if (bottom == null)
            return null;

        ReferencePoint top = plugin.getPortalLogicManager().findFrameLimit(
                origin, VectorAxis.Y, Constants.REPLACEABLE_BLOCKS, maxHeight
        );
        if (top == null)
            return null;

        // keep within bounds
        double height = Math.sqrt(top.getLocation().distanceSquared(bottom.getLocation())) + 1;
        if (height < minHeight || height > maxHeight)
            return null;

        ReferencePoint left = null, right = null; // according to axis

        // attempt on X axis, then Z axis
        Axis chosenAxis = null;

        for (Axis axis : Arrays.asList(Axis.X, Axis.Z)) {
            Vector direction = VectorAxis.of(axis);

            left = plugin.getPortalLogicManager().findFrameLimit(
                    origin, direction.clone().multiply(-1), Constants.REPLACEABLE_BLOCKS, maxWidth
            );
            if (left == null)
                continue;

            right = plugin.getPortalLogicManager().findFrameLimit(
                    origin, direction.clone().multiply(1), Constants.REPLACEABLE_BLOCKS, maxWidth
            );
            if (right == null) {
                left = null; // reset for future iteration
                continue;
            }

            // keep within bounds
            double width = left.getLocation().distanceSquared(right.getLocation()) + 1;
            if (width >= minWidth && width <= maxWidth) {
                chosenAxis = axis;
                break;
            }

            // reset for future iteration
            left = null;
            right = null;
        }

        // horizontal limits not found
        if (chosenAxis == null)
            return null;

        Location bottomLeft = makeCorner(world, left, bottom);
        Location bottomRight = makeCorner(world, right, bottom);
        Location topLeft = makeCorner(world, left, top);
        Location topRight = makeCorner(world, right, top);

        Vector direction = VectorAxis.of(chosenAxis);

        Material frameBlock = left.isCustom() ?
                plugin.getConfigManager().getFrameBlock() :
                Constants.VANILLA_FRAME_BLOCK;

        // check all frames
        if (!checkFrame(bottomLeft, bottomRight, VectorAxis.NY, frameBlock) ||
            !checkFrame(topLeft, topRight, VectorAxis.Y, frameBlock) ||
            !checkFrame(bottomLeft, topLeft, direction.clone().multiply(-1), frameBlock) ||
            !checkFrame(bottomRight, topRight, direction, frameBlock))
            return null;

        // this is indeed a portal, return its blocks to check for perms
        return new PortalFrameData(bottomLeft, topRight, chosenAxis, left.isCustom());
    }

    /**
     * Simply create a new location out of 2 reference points, one providing the two horizontal coordinates, and the
     * other providing the vertical one.
     */
    private Location makeCorner(World world, ReferencePoint horizontal, ReferencePoint vertical) {
        return new Location(
                world,
                horizontal.getLocation().getBlockX(),
                vertical.getLocation().getBlockY(),
                horizontal.getLocation().getBlockZ()
        );
    }

    /**
     * Given two locations that form a line of blocks, checks if all blocks adjacent to this line (e.g.: above it)
     * are made out of the frame material.
     */
    private boolean checkFrame(Location corner1, Location corner2, Vector frameDirection, Material frameBlock) {
        World world = corner1.getWorld();

        for (Location location : LocationUtil.getLocationsBetween(corner1, corner2))
            if (world.getBlockAt(location.clone().add(frameDirection)).getType() != frameBlock)
                return false;

        return true;
    }

    /**
     * Data to return from the search process, using it to effectively place the portal into the world.
     */
    private static class PortalFrameData {

        private final List<Location> innerLocations;
        private final Location bottomLeft;
        private final Axis horizontalAxis;
        private final boolean isCustom;

        public PortalFrameData(Location bottomLeft, Location topRight, Axis horizontalAxis, boolean isCustom) {
            this.innerLocations = LocationUtil.getLocationsBetween(bottomLeft, topRight);
            this.horizontalAxis = horizontalAxis;
            this.bottomLeft = bottomLeft;
            this.isCustom = isCustom;
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

        public boolean isCustom() {
            return isCustom;
        }
    }

}

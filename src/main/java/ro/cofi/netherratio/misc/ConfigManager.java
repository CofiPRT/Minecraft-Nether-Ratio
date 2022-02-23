package ro.cofi.netherratio.misc;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import ro.cofi.netherratio.NetherRatio;

public class ConfigManager {

    private final NetherRatio plugin;

    public ConfigManager(NetherRatio plugin) {
        this.plugin = plugin;
    }

    public double getRatioOverworld() {
        return plugin.getConfig().getDouble("ratio.overworld");
    }

    public double getRatioNether() {
        return plugin.getConfig().getDouble("ratio.nether");
    }

    public Material getFrameBlock() {
        String backup = Constants.BACKUP_FRAME_BLOCK.getKey().toString();

        String frameBlock = plugin.getConfig().getString("frame_block", backup);
        NamespacedKey namespacedKey = NamespacedKey.fromString(frameBlock);

        Material material = null;

        if (namespacedKey != null)
            material = Registry.MATERIAL.get(namespacedKey);

        if (material == null) {
            plugin.getLogger().severe(plugin.prefixMessage("Unknown block id '%s'. Fixing to default frame block '%s'"
                    .formatted(frameBlock, backup)));

            plugin.getConfig().set("frame_block", backup);

            material = Constants.BACKUP_FRAME_BLOCK;
        }

        return material;
    }

    public boolean isFloatingPlacementAllowed() {
        return plugin.getConfig().getBoolean("allow_floating_placement");
    }

    public boolean isForcedPlacementAllowed() {
        return plugin.getConfig().getBoolean("allow_forced_placement");
    }

    public double getMinDistanceBetweenPortalsOverworld() {
        return plugin.getConfig().getDouble("min_distance_between_portals.overworld");
    }

    public double getMinDistanceBetweenPortalsNether() {
        return plugin.getConfig().getDouble("min_distance_between_portals.nether");
    }

    public int getMaxPortalPlacementOffsetVertical() {
        return plugin.getConfig().getInt("max_portal_placement_offset.vertical");
    }

    public int getMaxPortalPlacementOffsetHorizontal() {
        return plugin.getConfig().getInt("max_portal_placement_offset.horizontal");
    }

    public int getPortalSizeHeightMin() {
        return plugin.getConfig().getInt("portal_size.height.min");
    }

    public int getPortalSizeHeightMax() {
        return plugin.getConfig().getInt("portal_size.height.max");
    }

    public int getPortalSizeHeightNew() {
        return plugin.getConfig().getInt("portal_size.height.new");
    }

    public int getPortalSizeWidthMin() {
        return plugin.getConfig().getInt("portal_size.height.min");
    }

    public int getPortalSizeWidthMax() {
        return plugin.getConfig().getInt("portal_size.width.max");
    }

    public int getPortalSizeWidthNew() {
        return plugin.getConfig().getInt("portal_size.width.new");
    }

}

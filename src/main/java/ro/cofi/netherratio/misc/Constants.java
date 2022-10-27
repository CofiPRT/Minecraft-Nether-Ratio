package ro.cofi.netherratio.misc;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.Set;

public interface Constants {

    Set<Material> REPLACEABLE_BLOCKS = Set.of(
        Material.AIR,
        Material.CAVE_AIR,
        Material.VOID_AIR,
        Material.FIRE
    );

    Material BACKUP_FRAME_BLOCK = Material.CRYING_OBSIDIAN;
    Material VANILLA_FRAME_BLOCK = Material.OBSIDIAN;

    Set<World.Environment> VALID_ENVIRONMENTS = Set.of(
        World.Environment.NORMAL,
        World.Environment.NETHER
    );

}

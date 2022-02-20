package ro.cofi.netherratio.event;

import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.world.PortalCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomPortalCreateEvent extends PortalCreateEvent {
    public CustomPortalCreateEvent(@NotNull List<BlockState> blocks, @NotNull World world,
                                   @Nullable Entity entity, @NotNull CreateReason reason) {
        super(blocks, world, entity, reason);
    }
}

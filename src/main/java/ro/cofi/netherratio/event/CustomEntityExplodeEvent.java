package ro.cofi.netherratio.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomEntityExplodeEvent extends EntityExplodeEvent {
    @SuppressWarnings("squid:S6213") // match parameter name from super class
    public CustomEntityExplodeEvent(
        @NotNull Entity what, @NotNull Location location, @NotNull List<Block> blocks, float yield
    ) {
        super(what, location, blocks, yield);
    }
}

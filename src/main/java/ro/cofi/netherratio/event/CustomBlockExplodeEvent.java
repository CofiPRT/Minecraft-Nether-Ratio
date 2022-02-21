package ro.cofi.netherratio.event;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockExplodeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomBlockExplodeEvent extends BlockExplodeEvent {
    public CustomBlockExplodeEvent(@NotNull Block what, @NotNull List<Block> blocks, float yield) {
        super(what, blocks, yield);
    }
}

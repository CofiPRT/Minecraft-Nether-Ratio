package ro.cofi.netherratio.event;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomBlockBreakBlockEvent extends BlockBreakBlockEvent {
    public CustomBlockBreakBlockEvent(@NotNull Block block, @NotNull Block source, @NotNull List<ItemStack> drops) {
        super(block, source, drops);
    }
}

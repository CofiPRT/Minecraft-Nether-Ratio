package ro.cofi.netherratio.listener;

import org.bukkit.event.Listener;
import ro.cofi.netherratio.NetherRatio;

public abstract class AbstractListener implements Listener {

    protected final NetherRatio plugin;

    protected AbstractListener(NetherRatio plugin) {
        this.plugin = plugin;
    }

}

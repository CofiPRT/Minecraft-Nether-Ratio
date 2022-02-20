package ro.cofi.netherratio.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomEntityTeleportEvent extends EntityTeleportEvent {
    public CustomEntityTeleportEvent(@NotNull Entity what, @NotNull Location from, @Nullable Location to) {
        super(what, from, to);
    }
}

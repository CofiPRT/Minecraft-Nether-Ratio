package ro.cofi.netherratio.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomPlayerTeleportEvent extends PlayerTeleportEvent {
    public CustomPlayerTeleportEvent(@NotNull Player player, @NotNull Location from, @Nullable Location to) {
        super(player, from, to);
    }

    public CustomPlayerTeleportEvent(@NotNull Player player, @NotNull Location from, @Nullable Location to, @NotNull TeleportCause cause) {
        super(player, from, to, cause);
    }
}

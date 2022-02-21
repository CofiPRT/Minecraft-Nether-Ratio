package ro.cofi.netherratio.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ro.cofi.netherratio.NetherRatio;

public class ReloadCommand implements CommandExecutor {

    private static final String RESPONSE = "The config files have been reloaded.";

    private final NetherRatio plugin;

    public ReloadCommand(NetherRatio plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        plugin.getPortalLocationManager().reloadConfig();
        plugin.reloadConfig();

        String message = plugin.prefixMessage(RESPONSE);

        // send the message to the issuing player, don't duplicate any message
        if (sender instanceof Player)
            sender.sendMessage(message);

        plugin.getLogger().info(message);

        return true;
    }
}

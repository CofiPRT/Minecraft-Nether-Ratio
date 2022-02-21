package ro.cofi.netherratio;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import ro.cofi.netherratio.command.ReloadCommand;
import ro.cofi.netherratio.listener.BlockBreakListener;
import ro.cofi.netherratio.listener.EntityTeleportListener;
import ro.cofi.netherratio.listener.FirePlaceListener;
import ro.cofi.netherratio.listener.PortalCreateListener;
import ro.cofi.netherratio.logic.PortalLocationManager;
import ro.cofi.netherratio.logic.PortalLogicManager;

import java.util.Objects;

public final class NetherRatio extends JavaPlugin {

    private static final ChatColor PLUGIN_NAME_COLOR = ChatColor.AQUA;

    private PortalLocationManager portalLocationManager;
    private PortalLogicManager portalLogicManager;

    @Override
    public void onEnable() {
        // save the config file from the jar into the server folder, in case it doesn't exist yet
        saveDefaultConfig();

        // init fields
        portalLocationManager = new PortalLocationManager(this);
        portalLogicManager = new PortalLogicManager(this);

        // register listeners
        getServer().getPluginManager().registerEvents(new FirePlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalCreateListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityTeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);

        // register commands
        Objects.requireNonNull(getServer().getPluginCommand("nrreload")).setExecutor(new ReloadCommand(this));
    }

    @Override
    public void onDisable() {
        portalLocationManager.saveConfig();
    }

    public PortalLocationManager getPortalLocationManager() {
        return portalLocationManager;
    }

    public PortalLogicManager getPortalLogicManager() {
        return portalLogicManager;
    }

    public String prefixMessage(String message) {
        message = "[%s%s%s] %s".formatted(PLUGIN_NAME_COLOR, getName(), ChatColor.RESET, message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

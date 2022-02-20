package ro.cofi.netherratio;

import org.bukkit.plugin.java.JavaPlugin;
import ro.cofi.netherratio.listener.BlockBreakListener;
import ro.cofi.netherratio.listener.EntityTeleportListener;
import ro.cofi.netherratio.listener.FirePlaceListener;
import ro.cofi.netherratio.listener.PortalCreateListener;
import ro.cofi.netherratio.persistence.PortalLocationManager;

public final class NetherRatio extends JavaPlugin {

    public static final String ID = "nether-ratio";

    private PortalLocationManager portalLocationManager;
    private PortalLogicManager portalLogicManager;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new FirePlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalCreateListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityTeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);

        portalLocationManager = new PortalLocationManager(this);
        portalLogicManager = new PortalLogicManager(this);
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
}

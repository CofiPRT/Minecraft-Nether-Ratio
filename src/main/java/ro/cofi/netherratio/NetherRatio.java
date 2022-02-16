package ro.cofi.netherratio;

import org.bukkit.plugin.java.JavaPlugin;

public final class NetherRatio extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new FirePlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerPortalCreateListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

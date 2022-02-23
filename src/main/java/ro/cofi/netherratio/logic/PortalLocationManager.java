package ro.cofi.netherratio.logic;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import ro.cofi.netherratio.NetherRatio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PortalLocationManager {

    private static final String FILE_NAME = "portals.yml";

    private static final Pattern COORD_SEPARATOR = Pattern.compile(",");

    private static final String OVERWORLD_KEY = "overworld";
    private static final String NETHER_KEY = "nether";
    private static final String CUSTOM_KEY = "custom";
    private static final String VANILLA_KEY = "vanilla";

    private final NetherRatio plugin;

    private File configFile;
    private FileConfiguration config;


    public PortalLocationManager(NetherRatio plugin) {
        this.plugin = plugin;

        saveDefaultConfig();
    }

    /**
     * Create the portal data file in the server files, if necessary, thus guaranteeing its existence.
     */
    public void saveDefaultConfig() {
        initConfigFile();

        if (!configFile.exists())
            plugin.saveResource(FILE_NAME, false);
    }

    /**
     * Instantiate the java {@link File} instance representing the portal data file. May not exist.
     */
    private void initConfigFile() {
        if (configFile == null)
            configFile = new File(plugin.getDataFolder(), FILE_NAME);
    }

    /**
     * Loads the config in memory from the server files. In case the config is
     * not yet saved on the server, load default values from the jar.
     */
    public void reloadConfig() {
        initConfigFile();

        config = YamlConfiguration.loadConfiguration(configFile);

        // load default values
        InputStream defaultStream = plugin.getResource(FILE_NAME);

        if (defaultStream != null)
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream)));
    }

    /**
     * Simple getter that guarantees a non-null result.
     * @return	The portal data configuration.
     */
    public FileConfiguration getConfig() {
        if (config == null)
            reloadConfig();

        return config;
    }

    /**
     * A {@link FileConfiguration} is saved in memory unless instructed to be written to the disk.
     */
    public void saveConfig() {
        if (config == null || configFile == null)
            return;

        try {
            getConfig().save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, plugin.prefixMessage("Could not save " + FILE_NAME), e);
        }
    }

    /**
     * Save a portal into the config.
     */
    public void savePortal(Location location, boolean isCustom) {
        getConfig().set(getWorldKey(location, isCustom) + "." + vecToString(location.toVector()), true);

        saveConfig();
    }

    /**
     * Delete a portal from the config.
     */
    public void deletePortal(Location location, boolean isCustom) {
        getConfig().set(getWorldKey(location, isCustom) + "." + vecToString(location.toVector()), null);

        saveConfig();
    }

    /**
     * Get all portals in the given world.
     */
    public List<Vector> getPortals(World world, boolean isCustom) {
        ConfigurationSection section = getConfig().getConfigurationSection(getWorldKey(world, isCustom));
        if (section == null)
            return Collections.emptyList();

        return section.getKeys(false)
                .stream()
                .map(this::stringToVec)
                .collect(Collectors.toList());
    }

    private String vecToString(Vector vec) {
        return vec.getBlockX() + "," + vec.getBlockY() + "," + vec.getBlockZ();
    }

    private Vector stringToVec(String vec) {
        int[] coords = COORD_SEPARATOR.splitAsStream(vec)
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();

        if (coords.length < 3) {
            plugin.getLogger().severe(plugin.prefixMessage(
                    "Invalid coordinates '" + vec + "' in config file " + FILE_NAME
            ));
            return null;
        }

        return new Vector(coords[0], coords[1], coords[2]);
    }

    private String getWorldKey(Location location, boolean isCustom) {
        return getWorldKey(location.getWorld(), isCustom);
    }

    private String getWorldKey(World world, boolean isCustom) {
        return (isCustom ? CUSTOM_KEY : VANILLA_KEY) + "." +
                (world.getEnvironment() == World.Environment.NORMAL ? OVERWORLD_KEY : NETHER_KEY);
    }

}

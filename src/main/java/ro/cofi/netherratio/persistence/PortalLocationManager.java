package ro.cofi.netherratio.persistence;

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
    private static final String OVERWORLD_KEY = "overworld";
    private static final String NETHER_KEY = "nether";
    private static final Pattern COORD_SEPARATOR = Pattern.compile(",");

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
            plugin.getLogger().log(Level.SEVERE, "Could not save " + FILE_NAME, e);
        }
    }

    /**
     * Save an Overworld portal into the config.
     */
    public void saveOverworldPortal(Vector overworldPortalLocation) {
        getConfig().set(OVERWORLD_KEY + "." + vecToString(overworldPortalLocation), true);
    }

    /**
     * Save a Nether portal into the config.
     */
    public void saveNetherPortal(Vector netherPortalLocation) {
        getConfig().set(NETHER_KEY + "." + vecToString(netherPortalLocation), true);
    }

    /**
     * Delete an Overworld portal from the config.
     */
    public void deleteOverworldPortal(Vector overworldPortalLocation) {
        getConfig().set(OVERWORLD_KEY + "." + vecToString(overworldPortalLocation), null);
    }

    /**
     * Delete a Nether portal from the config.
     */
    public void deleteNetherPortal(Vector netherPortalLocation) {
        getConfig().set(NETHER_KEY + "." + vecToString(netherPortalLocation), null);
    }

    public List<Vector> getOverworldPortals() {
        return getPortals(OVERWORLD_KEY);
    }

    public List<Vector> getNetherPortals() {
        return getPortals(NETHER_KEY);
    }

    private List<Vector> getPortals(String key) {
        ConfigurationSection section = getConfig().getConfigurationSection(key);
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
            plugin.getLogger().log(Level.SEVERE, "Invalid coordinates '" + vec + "' in config file " + FILE_NAME);
            return null;
        }

        return new Vector(coords[0], coords[1], coords[2]);
    }

}

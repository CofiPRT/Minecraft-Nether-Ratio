package ro.cofi.netherratio.logic;

import org.bukkit.Location;

public record ReferencePoint(Location location, boolean isCustom) {

    public Location getLocation() {
        return location;
    }

    public boolean isCustom() {
        return isCustom;
    }

}

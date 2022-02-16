package ro.cofi.netherratio;

import org.bukkit.Axis;
import org.bukkit.Location;

import java.util.List;

public class PortalFrameData {

    private final List<Location> innerLocations;
    private final Axis horizontalAxis;

    public PortalFrameData(List<Location> innerLocations, Axis horizontalAxis) {
        this.innerLocations = innerLocations;
        this.horizontalAxis = horizontalAxis;
    }

    public List<Location> getInnerLocations() {
        return innerLocations;
    }

    public Axis getHorizontalAxis() {
        return horizontalAxis;
    }

}

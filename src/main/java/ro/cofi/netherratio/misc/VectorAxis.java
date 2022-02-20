package ro.cofi.netherratio.misc;

import org.bukkit.util.Vector;

public interface VectorAxis {
    Vector X = new Vector(1, 0, 0);
    Vector Y = new Vector(0, 1, 0);
    Vector Z = new Vector(0, 0, 1);
    Vector NX = new Vector(-1, 0, 0);
    Vector NY = new Vector(0, -1, 0);
    Vector NZ = new Vector(0, 0, -1);
}

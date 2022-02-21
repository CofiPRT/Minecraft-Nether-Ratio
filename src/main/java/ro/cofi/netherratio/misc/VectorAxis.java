package ro.cofi.netherratio.misc;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

public interface VectorAxis {
    Vector X = new Vector(1, 0, 0);
    Vector Y = new Vector(0, 1, 0);
    Vector Z = new Vector(0, 0, 1);
    Vector NX = new Vector(-1, 0, 0);
    Vector NY = new Vector(0, -1, 0);
    Vector NZ = new Vector(0, 0, -1);

    static Vector of(Axis axis) {
        return switch (axis) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    static Vector ofSide(Axis axis) {
        return switch (axis) {
            case X -> Z;
            case Y -> Y;
            case Z -> X;
        };
    }
}

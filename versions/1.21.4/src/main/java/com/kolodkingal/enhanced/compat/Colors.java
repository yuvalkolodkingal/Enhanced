package com.kolodkingal.enhanced.compat;

import net.minecraft.util.ARGB;
import org.joml.Vector3f;

/** 1.21.4 only: dust particles take a colour as 0xRRGGBB, not as a vector of 0 to 1. */
public final class Colors {
    private Colors() {
    }

    public static int rgb(Vector3f color) {
        return ARGB.colorFromFloat(1.0F, color.x(), color.y(), color.z()) & 0xFFFFFF;
    }

    public static int rgb(int color) {
        return color;
    }
}

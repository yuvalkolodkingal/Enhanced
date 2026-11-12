package com.kolodkingal.enhanced.compat;
//#norenames

/** 1.21.11: the one uniform the mod set by hand, the model (chunk) offset, is the offset {@link Gl} draws with. */
public final class Uniform {
    public void set(float x, float y, float z) {
        Gl.modelOffset.set(x, y, z);
    }
}

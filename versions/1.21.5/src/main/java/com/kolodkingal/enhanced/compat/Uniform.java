package com.kolodkingal.enhanced.compat;

import com.mojang.blaze3d.systems.RenderSystem;

/** 1.21.5: the one uniform the mod set by hand, the model (chunk) offset, is RenderSystem's model offset. */
public final class Uniform {
    public void set(float x, float y, float z) {
        if (x == 0.0F && y == 0.0F && z == 0.0F) {
            RenderSystem.resetModelOffset();
        } else {
            RenderSystem.setModelOffset(x, y, z);
        }
    }
}

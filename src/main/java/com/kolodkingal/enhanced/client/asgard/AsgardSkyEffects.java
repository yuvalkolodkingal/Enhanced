package com.kolodkingal.enhanced.client.asgard;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * The sky of Asgard (3.5), the dimension effects {@code enhanced:asgard} named by its dimension type. The 3.5 skeleton
 * registers this placeholder (the Overworld's sky, without clouds) so the dimension has its effects from the start; lane L1
 * owns it and draws the realm's sky (the nebula, the stars below the horizon, the ringed planet).
 */
public class AsgardSkyEffects extends DimensionSpecialEffects {
    public AsgardSkyEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}

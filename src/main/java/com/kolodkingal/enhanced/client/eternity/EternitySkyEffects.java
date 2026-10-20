package com.kolodkingal.enhanced.client.eternity;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * The sky of Eternity's realm (3.8, PLAN 10.4), the dimension effects {@code enhanced:eternity}. The 3.8 skeleton's is
 * plain white (no sun, moon, stars or clouds; white fog); lane L8 paints the cloud dome, its reflection and the mist.
 */
public class EternitySkyEffects extends DimensionSpecialEffects {
    public EternitySkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.95, 0.96, 0.97);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}

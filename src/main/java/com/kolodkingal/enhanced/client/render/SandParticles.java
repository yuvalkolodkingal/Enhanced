package com.kolodkingal.enhanced.client.render;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/**
 * The particles of the sand: grains (the sand block's own, which the game shades darker than the block) and a haze, tinted
 * the colour of dry sand (the white puffs of the game's own smoke would look like steam).
 */
public final class SandParticles {
    /** A grain of sand: a small square of the block's texture. */
    public static final ParticleOptions GRAIN = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());

    /** The tint of the haze, 0 to 1 per channel: {@link SandPalette#LIGHT}, a little paler than the grains. */
    private static final Vector3f HAZE = new Vector3f(0xE6 / 255.0F, 0xCB / 255.0F, 0x86 / 255.0F);

    /** A puff of sand-coloured dust, {@code scale} times its usual size. */
    public static ParticleOptions haze(float scale) {
        return new DustParticleOptions(HAZE, scale);
    }

    /** A single tinted grain: a small, darker dust mote. */
    public static ParticleOptions mote() {
        return new DustParticleOptions(MOTE, 0.9F);
    }

    private static final Vector3f MOTE = new Vector3f(0xD8 / 255.0F, 0xB7 / 255.0F, 0x6A / 255.0F);

    private SandParticles() {
    }
}

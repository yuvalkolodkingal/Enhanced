package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a particle is (SlowParticles: whether it is inside a slow-motion zone). */
@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("x")
    double enhanced$getX();

    @Accessor("y")
    double enhanced$getY();

    @Accessor("z")
    double enhanced$getZ();
}

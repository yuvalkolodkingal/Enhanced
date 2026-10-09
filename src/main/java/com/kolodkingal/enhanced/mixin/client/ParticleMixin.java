package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.SlowedParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class ParticleMixin implements SlowedParticle {
    @Unique
    private int enhanced$slowPhase = -1;

    @Override
    public int enhanced$slowPhase() {
        return enhanced$slowPhase;
    }

    @Override
    public void enhanced$setSlowPhase(int phase) {
        enhanced$slowPhase = phase;
    }
}

package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.kolodkingal.enhanced.client.SlowParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.11: slowed particles tick less often (1.21.9 ticks a particle in its group; the name is 1.21.1's mixin's, of the
 * particle engine). Their drawing's partial tick is QuadParticleGroupMixin's.
 */
@Mixin(ParticleGroup.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void enhanced$slowTick(Particle particle, CallbackInfo ci) {
        if (SlowParticles.skipTick(particle)) {
            ci.cancel();
        }
    }
}

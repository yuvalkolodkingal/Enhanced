package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.kolodkingal.enhanced.client.SlowParticles;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 1.21.11 only: a slowed particle is drawn at its own partial tick (1.21.9 extracts a particle's quad in its group). */
@Mixin(QuadParticleGroup.class)
public abstract class QuadParticleGroupMixin {
    @ModifyArg(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"),
            index = 2)
    private float enhanced$slowPartialTick(float partialTick, @Local SingleQuadParticle particle) {
        return SlowParticles.partialTick(particle, partialTick);
    }
}

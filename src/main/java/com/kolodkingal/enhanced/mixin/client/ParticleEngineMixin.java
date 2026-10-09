package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.SlowParticles;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Particles near a running Quicksilver drift in slow motion too. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void enhanced$slowTick(Particle particle, CallbackInfo ci) {
        if (SlowParticles.skipTick(particle)) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"),
            index = 2)
    private float enhanced$slowPartialTick(float partialTick, @Local Particle particle) {
        return SlowParticles.partialTick(particle, partialTick);
    }
}

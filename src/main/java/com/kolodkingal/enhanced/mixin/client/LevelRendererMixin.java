package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.TheNoise;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Billy's The Noise: a creature he hears glows in the colour of its mood (red if it means harm, his blue otherwise)
 * instead of its team colour.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int enhanced$moodColor(Entity entity, Operation<Integer> original) {
        return TheNoise.reveals(entity) ? TheNoise.moodColor(entity) : original.call(entity);
    }
}

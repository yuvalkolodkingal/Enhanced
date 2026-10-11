package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.EntityGlow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A creature some hero senses glows in the colour that hero gives it (Billy's The Noise: the colour of its mood, red if
 * it means harm, his blue otherwise) instead of its team colour: {@link EntityGlow} says which.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int enhanced$senseColor(Entity entity, Operation<Integer> original) {
        return EntityGlow.color(entity, () -> original.call(entity));
    }
}

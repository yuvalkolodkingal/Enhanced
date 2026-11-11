package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.EntityGlow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A sensed creature glows in the colour its hero gives it (1.21.4: the outline colour is chosen in renderEntities). */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int enhanced$senseColor(Entity entity, Operation<Integer> original) {
        return EntityGlow.color(entity, () -> original.call(entity));
    }
}

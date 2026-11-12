package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.kolodkingal.enhanced.client.EntityGlow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.11: a sensed creature glows in its sense's colour. The outline's colour is a render state's from 1.21.2, set when
 * the state is extracted (the name is 1.21.1's mixin's, which hooked the level's drawing of the entities).
 */
@Mixin(EntityRenderer.class)
public abstract class LevelRendererMixin {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int enhanced$senseColor(Entity entity, Operation<Integer> original) {
        return EntityGlow.color(entity, () -> original.call(entity));
    }
}

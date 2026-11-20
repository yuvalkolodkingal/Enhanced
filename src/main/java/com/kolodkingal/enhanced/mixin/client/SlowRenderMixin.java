package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.ability.SlowMotion;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 3.18.0: a creature (or anything else) in slowed time gets every Nth tick only, so it is drawn with the part of its last
 * real tick that the slowed time has reached ({@link SlowMotion#renderPartialTick}): it glides at 1/N speed instead of
 * moving a tick at once and standing. Up to 1.21.4 the level draws each entity with a partial tick; from 1.21.9 the
 * dispatcher extracts its render state with one.
 */
//#if >=1.21.11
//$$ @Mixin(net.minecraft.client.renderer.entity.EntityRenderDispatcher.class)
//#else
@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
//#endif
public abstract class SlowRenderMixin {
    //#if >=1.21.11
    //$$ @WrapMethod(method = "extractEntity")
    //$$ private net.minecraft.client.renderer.entity.state.EntityRenderState enhanced$slowPartialTick(Entity entity, float partialTick,
    //$$         Operation<net.minecraft.client.renderer.entity.state.EntityRenderState> original) {
    //$$     return original.call(entity, SlowMotion.renderPartialTick(entity, partialTick));
    //$$ }
    //#else
    @WrapMethod(method = "renderEntity")
    private void enhanced$slowPartialTick(Entity entity, double camX, double camY, double camZ, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers,
            Operation<Void> original) {
        original.call(entity, camX, camY, camZ, SlowMotion.renderPartialTick(entity, partialTick), poseStack, buffers);
    }
    //#endif
}

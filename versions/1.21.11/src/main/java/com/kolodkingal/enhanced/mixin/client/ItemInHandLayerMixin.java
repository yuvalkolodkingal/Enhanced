package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.kolodkingal.enhanced.client.render.ThorGrip;
import com.kolodkingal.enhanced.compat.RenderBridge;
import com.kolodkingal.enhanced.compat.SubmitBuffers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.21.11: Thor's grip on Mjolnir and Stormbreaker (the item in hand is submitted from 1.21.9; the grip draws into SubmitBuffers). */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @WrapOperation(method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void enhanced$grip(ItemStackRenderState item, PoseStack poseStack, SubmitNodeCollector collector, int light,
                               int overlay, int outline, Operation<Void> original,
                               @Local(argsOnly = true) ArmedEntityRenderState state, @Local(argsOnly = true) HumanoidArm arm) {
        if (RenderBridge.entity(state) instanceof LivingEntity entity) {
            ItemStack stack = entity.getItemHeldByArm(arm);
            boolean left = arm == HumanoidArm.LEFT;
            ItemDisplayContext context = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            Object model = ((RenderLayer<?, ?>) (Object) this).getParentModel();
            SubmitBuffers buffers = new SubmitBuffers(collector);
            boolean drawn = ThorGrip.render(Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer(), entity, stack,
                    context, arm, left, poseStack, buffers, light, model);
            buffers.flush();
            if (drawn) {
                return;
            }
        }
        original.call(item, poseStack, collector, light, overlay, outline);
    }
}

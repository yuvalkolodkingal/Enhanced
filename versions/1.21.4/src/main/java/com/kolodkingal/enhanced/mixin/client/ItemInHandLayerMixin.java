package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.ThorGrip;
import com.kolodkingal.enhanced.compat.RenderBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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

/** Thor's weapon is drawn by {@link ThorGrip} (1.21.4: in place of the item state's own drawing). */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @WrapOperation(method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
    private void enhanced$grip(ItemStackRenderState item, PoseStack poseStack, MultiBufferSource buffer, int light,
                               int overlay, Operation<Void> original, @Local(argsOnly = true) ArmedEntityRenderState state,
                               @Local(argsOnly = true) HumanoidArm arm) {
        if (RenderBridge.entity(state) instanceof LivingEntity entity) {
            ItemStack stack = entity.getItemHeldByArm(arm);
            boolean left = arm == HumanoidArm.LEFT;
            ItemDisplayContext context = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            Object model = ((RenderLayer<?, ?>) (Object) this).getParentModel();
            if (ThorGrip.render(Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer(), entity, stack,
                    context, arm, left, poseStack, buffer, light, model)) {
                return;
            }
        }
        original.call(item, poseStack, buffer, light, overlay);
    }
}

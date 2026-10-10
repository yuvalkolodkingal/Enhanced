package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.ThorGrip;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Thor's weapon is drawn by {@link ThorGrip}, from numbers, so the handle really is in his fist at every pose (the item
 * model's display transform put it three pixels in front of the hand). Everything else is drawn as vanilla does.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> {
    @WrapOperation(method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;"
                    + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void enhanced$grip(ItemInHandRenderer renderer, LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left,
                               PoseStack poseStack, MultiBufferSource buffer, int light, Operation<Void> original,
                               @Local(argsOnly = true) HumanoidArm arm) {
        // The layer also dresses zombies, skeletons and piglins: ThorGrip checks the model is a player's.
        Object model = ((RenderLayer<?, ?>) (Object) this).getParentModel();
        if (!ThorGrip.render(renderer, entity, stack, context, arm, left, poseStack, buffer, light, model)) {
            original.call(renderer, entity, stack, context, left, poseStack, buffer, light);
        }
    }
}

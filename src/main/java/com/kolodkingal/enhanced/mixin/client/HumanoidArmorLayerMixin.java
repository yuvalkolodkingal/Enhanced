package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.HeroArmour;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A hero's armour is not drawn ({@link HeroArmour}): the layer is told to skip the piece before it draws the armour,
 * its trim or its glint. The layer also dresses zombies, skeletons, piglins and armour stands: {@link HeroArmour} only
 * hides the armour of heroes.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, A extends HumanoidModel<T>> {
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
            + "Lnet/minecraft/client/model/HumanoidModel;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void enhanced$hideHeroArmour(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot,
                                         int packedLight, A model, float limbSwing, float limbSwingAmount,
                                         float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
                                         CallbackInfo ci) {
        if (HeroArmour.hidden(entity, slot)) {
            ci.cancel();
        }
    }
}

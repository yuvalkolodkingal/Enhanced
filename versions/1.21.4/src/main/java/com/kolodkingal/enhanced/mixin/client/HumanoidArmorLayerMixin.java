package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.HeroArmour;
import com.kolodkingal.enhanced.compat.RenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A hero's armour is not drawn (1.21.4: the piece no longer knows its wearer, so the layer remembers it). */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Unique
    private LivingEntity enhanced$wearer;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"))
    private void enhanced$wearer(PoseStack poseStack, MultiBufferSource buffer, int light, HumanoidRenderState state,
                                 float yRot, float xRot, CallbackInfo ci) {
        enhanced$wearer = RenderBridge.entity(state) instanceof LivingEntity living ? living : null;
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void enhanced$hideHeroArmour(PoseStack poseStack, MultiBufferSource buffer, ItemStack stack,
                                         EquipmentSlot slot, int light, net.minecraft.client.model.HumanoidModel<?> model,
                                         CallbackInfo ci) {
        if (enhanced$wearer != null && HeroArmour.hidden(enhanced$wearer, slot)) {
            ci.cancel();
        }
    }
}

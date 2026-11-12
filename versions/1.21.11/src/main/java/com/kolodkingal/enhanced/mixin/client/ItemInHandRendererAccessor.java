package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.21.11: the first-person arm and item, which are submitted from 1.21.9. */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("renderArmWithItem")
    void enhanced$renderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress,
                                    ItemStack stack, float equippedProgress, PoseStack poseStack, SubmitNodeCollector collector, int combinedLight);

    @Invoker("renderPlayerArm")
    void enhanced$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, float equippedProgress, float swingProgress,
                                  HumanoidArm side);
}

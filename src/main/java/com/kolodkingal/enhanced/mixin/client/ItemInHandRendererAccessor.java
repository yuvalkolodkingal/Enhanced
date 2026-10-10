package com.kolodkingal.enhanced.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets {@link com.kolodkingal.enhanced.client.render.FirstPersonArms} draw the first-person hands itself, with the extra
 * movement of a gesture, by calling the two private methods that draw a hand with its item and an arm on its own.
 */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("renderArmWithItem")
    void enhanced$renderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress,
                                    ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight);

    @Invoker("renderPlayerArm")
    void enhanced$renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float equippedProgress, float swingProgress,
                                  HumanoidArm side);
}

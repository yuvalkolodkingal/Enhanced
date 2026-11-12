package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 26.3: the first-person arm and item (26.3's first-person hands renderer, drawn from the player's render state). */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("submitArmWithItem")
    void enhanced$submitArmWithItem(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks,
                                    float xRot, InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight,
                                    PoseStack poseStack, SubmitNodeCollector collector, int lightCoords);

    @Invoker("renderPlayerArm")
    void enhanced$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, float inverseArmHeight,
                                  float attackValue, HumanoidArm arm, PlayerRenderState playerState);
}

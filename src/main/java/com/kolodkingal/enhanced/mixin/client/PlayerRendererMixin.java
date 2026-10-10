package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.BodyLean;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Heroes lean into their flight and their run where vanilla tilts the body for elytra and swimming: inside the pose
 * the model and every layer are drawn with, but not the name tag.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL"))
    private void enhanced$lean(AbstractClientPlayer player, PoseStack poseStack, float bob, float yBodyRot,
                               float partialTick, float scale, CallbackInfo ci) {
        BodyLean.apply(player, poseStack, yBodyRot, partialTick);
    }
}

package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.kolodkingal.enhanced.client.render.BodyLean;
import com.kolodkingal.enhanced.client.render.GauntletLayer;
import com.kolodkingal.enhanced.compat.RenderBridge;
import com.kolodkingal.enhanced.compat.SubmitBuffers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.11: the body's lean, and the Gauntlet on the first-person hand (which is submitted from 1.21.9). */
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("TAIL"))
    private void enhanced$lean(AvatarRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        if (RenderBridge.entity(state) instanceof AbstractClientPlayer player) {
            BodyLean.apply(player, poseStack, bodyRot, state.partialTick);
        }
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void enhanced$gauntlet(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin,
                                   ModelPart arm, boolean sleeve, CallbackInfo ci) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            SubmitBuffers buffers = new SubmitBuffers(collector);
            GauntletLayer.renderFirstPerson(poseStack, buffers, light, player, arm, (AvatarRenderer<?>) (Object) this);
            buffers.flush();
        }
    }
}

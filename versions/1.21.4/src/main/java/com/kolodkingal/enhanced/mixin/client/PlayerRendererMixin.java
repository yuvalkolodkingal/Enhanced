package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.BodyLean;
import com.kolodkingal.enhanced.client.render.GauntletLayer;
import com.kolodkingal.enhanced.compat.RenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Heroes lean into their flight and run; the playing Thanos's Gauntlet in first person (1.21.4). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("TAIL"))
    private void enhanced$lean(PlayerRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        if (RenderBridge.entity(state) instanceof AbstractClientPlayer player) {
            BodyLean.apply(player, poseStack, bodyRot, state.partialTick);
        }
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void enhanced$gauntlet(PoseStack poseStack, MultiBufferSource buffer, int light, ResourceLocation skin,
                                   ModelPart arm, boolean sleeve, CallbackInfo ci) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            GauntletLayer.renderFirstPerson(poseStack, buffer, light, player, arm, (PlayerRenderer) (Object) this);
        }
    }
}

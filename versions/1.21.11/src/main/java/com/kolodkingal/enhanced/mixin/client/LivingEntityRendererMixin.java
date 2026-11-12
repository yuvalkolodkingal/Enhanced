package com.kolodkingal.enhanced.mixin.client;
//#norenames

import com.kolodkingal.enhanced.compat.RenderBridge;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.11: Vision phased is drawn see-through (the body is submitted from 1.21.9: its colour is the submit's). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static boolean enhanced$phased(LivingEntityRenderState state) {
        return RenderBridge.entity(state) instanceof Player player && VisionAbilities.isPhased(player);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void enhanced$phaseRenderType(LivingEntityRenderState state, boolean bodyVisible, boolean translucent,
                                          boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (bodyVisible && enhanced$phased(state)) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            LivingEntityRenderer<?, LivingEntityRenderState, ?> self = (LivingEntityRenderer) (Object) this;
            cir.setReturnValue(net.minecraft.client.renderer.rendertype.RenderTypes.itemEntityTranslucentCull(self.getTextureLocation(state)));
        }
    }

    @ModifyArg(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6)
    private int enhanced$phaseAlpha(int color, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LivingEntityRenderState state) {
        return enhanced$phased(state) ? 0x78FFFFFF : color;
    }
}

package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.compat.RenderBridge;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A phasing Vision is drawn see-through, like a ghost (1.21.4: from the render state's entity). */
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
            cir.setReturnValue(RenderType.itemEntityTranslucentCull(self.getTextureLocation(state)));
        }
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int enhanced$phaseAlpha(int color, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LivingEntityRenderState state) {
        return enhanced$phased(state) ? 0x78FFFFFF : color;
    }
}

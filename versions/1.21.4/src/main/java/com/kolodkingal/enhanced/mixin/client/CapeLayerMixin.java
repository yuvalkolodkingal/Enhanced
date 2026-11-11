package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.BodyLean;
import com.kolodkingal.enhanced.compat.RenderBridge;
import net.minecraft.client.model.PlayerCapeModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.4: the cape is a model of its own posed from the render state. The lean of a flying hero is taken off its swing
 * and it sways to the outside of a bank, as in 1.21.1's CapeLayerMixin.
 */
@Mixin(PlayerCapeModel.class)
public abstract class CapeLayerMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("HEAD"))
    private void enhanced$streamBehind(PlayerRenderState state, CallbackInfo ci) {
        if (!(RenderBridge.entity(state) instanceof AbstractClientPlayer player)) {
            return;
        }
        float swing = 6.0F + state.capeLean / 2.0F + state.capeFlap;
        float lean = BodyLean.pitch(player) * Mth.RAD_TO_DEG;
        if (lean > 0.0F) {
            float streamed = Math.max(swing - lean, Math.min(swing, 6.0F));
            state.capeLean += 2.0F * (streamed - swing);
        }
        state.capeLean2 -= 0.8F * BodyLean.roll(player);
    }
}

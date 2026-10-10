package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.render.BodyLean;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla swings a cape out behind a fast player as if the body were upright. On a hero who leans into a flight that
 * would stick it up in the air, so the lean is taken off the swing: the cape streams out behind him. When he banks, it
 * sways to the outside of the turn.
 */
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    /** The first rotation of the cape: how far it swings out from the back. */
    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 0),
            index = 0)
    private float enhanced$streamBehind(float degrees, @Local(argsOnly = true) AbstractClientPlayer player) {
        float lean = BodyLean.pitch(player) * Mth.RAD_TO_DEG;
        return lean > 0.0F ? Math.max(degrees - lean, Math.min(degrees, 6.0F)) : degrees;
    }

    /** The second rotation: the cape's sway to the side. It lags a turn and streams to the outside of a bank. */
    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 1),
            index = 0)
    private float enhanced$swayWithTheBank(float degrees, @Local(argsOnly = true) AbstractClientPlayer player) {
        return degrees - 0.4F * BodyLean.roll(player);
    }
}

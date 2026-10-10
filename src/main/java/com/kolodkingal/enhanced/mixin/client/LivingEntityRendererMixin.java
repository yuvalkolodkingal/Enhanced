package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.ability.PhotonAbilities;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Phasing heroes (Vision, Photon) are drawn see-through, like a ghost. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    private static boolean enhanced$phased(LivingEntity entity) {
        return entity instanceof Player player && (VisionAbilities.isPhased(player) || PhotonAbilities.isPhased(player));
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void enhanced$phaseRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing,
                                          CallbackInfoReturnable<RenderType> cir) {
        if (bodyVisible && enhanced$phased(entity)) {
            @SuppressWarnings("unchecked")
            LivingEntityRenderer<T, ?> self = (LivingEntityRenderer<T, ?>) (Object) this;
            cir.setReturnValue(RenderType.itemEntityTranslucentCull(self.getTextureLocation(entity)));
        }
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int enhanced$phaseAlpha(int color, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LivingEntity entity) {
        return enhanced$phased(entity) ? 0x78FFFFFF : color;
    }
}

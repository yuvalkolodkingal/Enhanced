package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.SuitSkins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swaps in a hero's suit while they are transformed (and Loki's borrowed or Frost Giant skin). */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void enhanced$suit(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin own = cir.getReturnValue();
        PlayerSkin skin = SuitSkins.skinFor((AbstractClientPlayer) (Object) this, own);
        if (skin != own) {
            cir.setReturnValue(skin);
        }
    }
}

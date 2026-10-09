package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.SuitSkins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swaps in Magneto's or the Scarlet Witch's suit while they are transformed. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void enhanced$suit(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin suit = SuitSkins.suitFor((AbstractClientPlayer) (Object) this);
        if (suit != null) {
            cir.setReturnValue(suit);
        }
    }
}

package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.SpectrumVision;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Photon's Spectrum Vision: creatures nearby glow through walls, for her eyes only. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void enhanced$spectrumVision(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && SpectrumVision.reveals(entity)) {
            cir.setReturnValue(true);
        }
    }
}

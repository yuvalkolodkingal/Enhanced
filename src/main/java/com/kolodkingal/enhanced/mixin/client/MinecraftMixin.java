package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.SpectrumVision;
import com.kolodkingal.enhanced.client.TheNoise;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Creatures nearby glow through walls, for one hero's eyes only: Photon's Spectrum Vision, and the creatures Billy hears
 * with The Noise (their colour is set in {@link LevelRendererMixin}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void enhanced$spectrumVision(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (SpectrumVision.reveals(entity) || TheNoise.reveals(entity))) {
            cir.setReturnValue(true);
        }
    }
}

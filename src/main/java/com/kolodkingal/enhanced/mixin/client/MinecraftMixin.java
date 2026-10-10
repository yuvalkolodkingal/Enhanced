package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.TheNoise;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The creatures Billy hears with The Noise glow through walls, for his eyes only (their colour is set in
 * {@link LevelRendererMixin}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void enhanced$theNoise(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && TheNoise.reveals(entity)) {
            cir.setReturnValue(true);
        }
    }
}

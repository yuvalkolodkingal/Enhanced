package com.kolodkingal.enhanced.mixin.client;

import com.kolodkingal.enhanced.client.EntityGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The creatures some hero senses (Billy hears them with The Noise, the Phoenix senses their minds...) glow through
 * walls, for that hero's eyes only: {@link EntityGlow} says who (their colour is set in {@link LevelRendererMixin}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void enhanced$senseGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && EntityGlow.reveals(entity)) {
            cir.setReturnValue(true);
        }
    }
}

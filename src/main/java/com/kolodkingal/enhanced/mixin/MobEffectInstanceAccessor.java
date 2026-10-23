package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An effect's time left, which the healing factor's shrug shortens as the effect is put on ({@code Suppression}). */
@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
    @Accessor("duration")
    void enhanced$setDuration(int ticks);
}

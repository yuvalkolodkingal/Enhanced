package com.kolodkingal.enhanced.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.HangingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code HangingEntity.setDirection}, which is protected: a frame or a painting a Build Magic Move turns (BuildJob). */
@Mixin(HangingEntity.class)
public interface HangingEntityInvoker {
    @Invoker("setDirection")
    void enhanced$setDirection(Direction facing);
}

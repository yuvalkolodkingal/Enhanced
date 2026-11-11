package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Whether an arrow is stuck in a block (from 1.21.4), and its base damage (1.21.5: no getter). */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("isInGround")
    boolean enhanced$isInGround();

    @Invoker("setInGround")
    void enhanced$setInGround(boolean inGround);

    @Accessor("baseDamage")
    double enhanced$baseDamage();
}

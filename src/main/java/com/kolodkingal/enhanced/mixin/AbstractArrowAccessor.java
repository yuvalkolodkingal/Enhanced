package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether an arrow (or a trident) is stuck in a block. Was an access transformer line before the port. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor("inGround")
    boolean enhanced$isInGround();

    @Accessor("inGround")
    void enhanced$setInGround(boolean inGround);
}

package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Whether an arrow (or a trident) is stuck in a block (1.21.4: synced data behind two protected methods). */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("isInGround")
    boolean enhanced$isInGround();

    @Invoker("setInGround")
    void enhanced$setInGround(boolean inGround);
}

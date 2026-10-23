package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A map maker's lock on an item frame ({@code Fixed}), which Magneto's pull from storage respects. Was an access
 * transformer line before the port.
 */
@Mixin(ItemFrame.class)
public interface ItemFrameAccessor {
    @Accessor("fixed")
    boolean enhanced$isFixed();

    @Accessor("fixed")
    void enhanced$setFixed(boolean fixed);
}

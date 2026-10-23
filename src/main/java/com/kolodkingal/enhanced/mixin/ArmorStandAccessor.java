package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A map maker's locks on an armour stand's slots ({@code DisabledSlots}), which Magneto's pull from storage respects.
 * Was an access transformer line before the port.
 */
@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {
    @Accessor("disabledSlots")
    int enhanced$getDisabledSlots();

    @Accessor("disabledSlots")
    void enhanced$setDisabledSlots(int slots);
}

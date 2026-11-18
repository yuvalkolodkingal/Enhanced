package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The top of a ranged attribute (3.17): the game stops a body's scale at 16 times a man, and the Godkiller armour of
 * Ultimate Mode stands 65 blocks tall ({@code ultimate.BespokeForms}), so the scale attribute's top is raised once, on
 * both sides. Nothing is any taller for it: only the Godkiller's own modifier ever asks for more than 16.
 */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {
    @Accessor("maxValue")
    double enhanced$getMaxValue();

    @Mutable
    @Accessor("maxValue")
    void enhanced$setMaxValue(double value);
}

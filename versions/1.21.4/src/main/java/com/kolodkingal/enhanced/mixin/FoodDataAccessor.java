package com.kolodkingal.enhanced.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.4: the food exhaustion, which lost its getter in 1.21.2. */
@Mixin(FoodData.class)
public interface FoodDataAccessor {
    @Accessor("exhaustionLevel")
    float enhanced$getExhaustionLevel();
}

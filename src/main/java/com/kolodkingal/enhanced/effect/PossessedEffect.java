package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A creature whose mind the Phoenix has taken over: it fights for her until the effect ends. A stub: what it does is
 * built by the Phoenix lane (DESIGN section 13); until then it is only registered, named and shown in the inventory.
 */
public class PossessedEffect extends MobEffect {
    public PossessedEffect() {
        super(MobEffectCategory.HARMFUL, 0xA98BFF);
    }
}

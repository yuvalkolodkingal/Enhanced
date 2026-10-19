package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Frightened (3.6, the Hulk's Roar): a hostile creature runs from the one who frightened it. Registered by the 3.6
 * skeleton as a plain harmful effect; lane L7 owns the class (the running away).
 */
public class FrightenedEffect extends MobEffect {
    public FrightenedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5BA33A);
    }
}

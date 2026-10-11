package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * What the Infinity Stones do to a creature Thanos turns on it: one class for the three effects, each with its own
 * colour (petrified by the Reality Stone, in the Soul World, locked in time by the Time Stone). A stub: what they do is
 * built by the Thanos lane (DESIGN section 15); until then they are only registered, named and shown in the inventory.
 */
public class StoneEffect extends MobEffect {
    public StoneEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}

package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Held in a cocoon of Apocalypse's sand: the hands are covered, no ability can be used. A stub: what it does is built
 * by the Apocalypse lane (DESIGN section 17); until then it is only registered, named and shown in the inventory.
 */
public class EntombedEffect extends MobEffect {
    public EntombedEffect() {
        super(MobEffectCategory.HARMFUL, 0xD9A93A);
    }
}

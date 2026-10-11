package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Stuck in Spider-Man's web: held in place until it ends or is torn. A stub: what it does is built by the Spider lane
 * (DESIGN section 14); until then it is only registered, named and shown in the inventory.
 */
public class WebbedEffect extends MobEffect {
    public WebbedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF2F6FA);
    }
}

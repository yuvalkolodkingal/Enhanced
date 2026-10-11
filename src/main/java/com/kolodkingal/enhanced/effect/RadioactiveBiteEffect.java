package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The seven seconds of sickness after the Radioactive Spider bites, before Spider-Man wakes in the player. A stub: what
 * it does is built by the Spider lane (DESIGN section 14); until then it is only registered, named and shown in the
 * inventory.
 */
public class RadioactiveBiteEffect extends MobEffect {
    public RadioactiveBiteEffect() {
        super(MobEffectCategory.HARMFUL, 0x6BE04A);
    }
}

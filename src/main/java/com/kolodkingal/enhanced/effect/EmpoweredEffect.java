package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Empowered by Apocalypse's Power Bestowal: stronger, tougher, quicker to recover. A stub: what it does is built by the
 * Apocalypse lane (DESIGN section 17); until then it is only registered, named and shown in the inventory.
 */
public class EmpoweredEffect extends MobEffect {
    public EmpoweredEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2B84B);
    }
}

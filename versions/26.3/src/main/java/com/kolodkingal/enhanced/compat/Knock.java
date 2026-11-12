package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.world.entity.LivingEntity;

/** 26.3 only: 1.21.1's knockback without a damage source (26.3's takes the source and the damage, here none). */
public final class Knock {
    private Knock() {
    }

    public static void back(LivingEntity entity, double strength, double x, double z) {
        entity.knockback(strength, x, z, entity.damageSources().generic(), 0.0F);
    }
}

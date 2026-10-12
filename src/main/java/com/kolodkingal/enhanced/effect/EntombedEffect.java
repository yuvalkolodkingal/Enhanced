package com.kolodkingal.enhanced.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Held in a cocoon of Apocalypse's sand: the hands are covered, no ability can be used, and the creature cannot move,
 * jump, fly, attack or use items. The cocoon ({@code SandCocoonEntity}) renews this effect every tick and breaks it when
 * it breaks; what the effect does by itself is to stop a creature that thinks (a mob stands still and drops its target).
 * A player is held by the cocoon, which puts them back where the sand closed on them, and by the checks that read this
 * effect: the ability keys ({@code AbilityHandler}), attacks and the use of items ({@code ApocalypseEvents}).
 */
public class EntombedEffect extends MobEffect {
    public EntombedEffect() {
        super(MobEffectCategory.HARMFUL, 0xD9A93A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            Vec3 motion = mob.getDeltaMovement();
            mob.setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        return true;
    }
}

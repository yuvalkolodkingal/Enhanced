package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * A creature whose mind the Phoenix has taken over: it fights for her until the effect ends (the Phoenix's Mind
 * Control). Every few ticks {@link PhoenixAbilities#steer} sends it after her enemies, and it takes no target of its
 * own (see {@code PhoenixEvents.onChangeTarget}); with nothing to fight it stays near her. The effect ends at once if
 * the Phoenix is gone (a creature that remembers it from a saved world, whose Phoenix left, wakes up). A ring of violet
 * sparks circles its head.
 */
public class PossessedEffect extends MobEffect {
    /** How often the creature is steered (ticks). */
    private static final int STEER_PERIOD = 10;

    public PossessedEffect() {
        super(MobEffectCategory.HARMFUL, PhoenixAbilities.PSY_MAIN);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        if (entity instanceof Mob mob) {
            if (entity.tickCount % STEER_PERIOD == 0 && !PhoenixAbilities.steer(mob)) {
                return false; // nobody holds it any more
            }
            PhoenixAbilities.guardBrain(mob);
        }
        if (entity.tickCount % 3 == 0) {
            // Three violet sparks circling the head.
            double radius = entity.getBbWidth() * 0.5 + 0.25;
            double y = entity.getEyeY() + 0.35;
            for (int i = 0; i < 3; i++) {
                double angle = entity.tickCount * 0.3 - i * Mth.TWO_PI / 3;
                int colour = i == 0 ? PhoenixAbilities.PSY_CORE : PhoenixAbilities.PSY_MAIN;
                level.sendParticles(new EnergyParticleOptions(colour, 0.8F), entity.getX() + Math.cos(angle) * radius, y,
                        entity.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }
}

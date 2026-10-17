package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Drained by Agatha Harkness (3.3; WandaVision: she draws the Scarlet Witch's power out of her): the hero's power is hers
 * for a while. While it lasts no ability key works ({@code AbilityHandler}), a flier comes down ({@code PowerTicker}) and
 * whatever was held lets go ({@code AgathaAbilities.everyPlayerTick}); the power itself comes back when it ends. A thin
 * violet haze still seeps off the body.
 */
public class DrainedEffect extends MobEffect {
    public DrainedEffect() {
        super(MobEffectCategory.HARMFUL, Power.AGATHA.color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            double y = entity.getY() + entity.getBbHeight() * (0.3 + 0.5 * entity.getRandom().nextDouble());
            level.sendParticles(new EnergyParticleOptions(Power.AGATHA.color, 0.7F), entity.getX(), y, entity.getZ(), 1,
                    entity.getBbWidth() * 0.4, 0.1, entity.getBbWidth() * 0.4, 0.01);
        }
        return true;
    }
}

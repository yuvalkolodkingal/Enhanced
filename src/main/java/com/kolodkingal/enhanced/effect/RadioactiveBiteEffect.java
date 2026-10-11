package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The sickness after the Radioactive Spider bites, before Spider-Man wakes in the player (DESIGN section 14). It carries
 * no attributes of its own: the bite gives the player Nausea, Weakness and Slowness for as long, and this effect adds
 * Poison for the last three seconds (vanilla's Poison stops at half a heart, so the bite never kills) and a few green
 * motes. When it expires (and not when milk or death removes it) the player, if they still have no power, becomes
 * Spider-Man: that is {@code SpiderEvents}.
 */
public class RadioactiveBiteEffect extends MobEffect {
    /** The sickly radioactive green of the spider and its bite. */
    public static final int GREEN = 0x7CFC00;
    /** The poison of the bite lasts this many ticks, at the end of it. */
    public static final int POISON_TICKS = 60;

    public RadioactiveBiteEffect() {
        super(MobEffectCategory.HARMFUL, GREEN);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(new EnergyParticleOptions(GREEN, 0.7F), entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                    2, 0.25, 0.3, 0.25, 0.02);
            MobEffectInstance bite = entity.getEffect(ModEffects.RADIOACTIVE_BITE);
            if (bite != null && bite.getDuration() == POISON_TICKS) {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0));
            }
        }
        return true;
    }
}

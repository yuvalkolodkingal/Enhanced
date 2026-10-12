package com.kolodkingal.enhanced.effect;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Wrapped in Spider-Man's webs: it crawls (a bat or a phantom barely flies), cannot jump, and can still hit what is next
 * to it. Silver motes circle the creature. It lasts as long as the web that made it, and a hard blow tears it sooner
 * (see {@code SpiderEvents}). Spider-Man himself, the bosses and (when PvP is off) players are never webbed.
 */
public class WebbedEffect extends MobEffect {
    /** The colour of the web: silver-white. */
    public static final int WEB = 0xF2F6FA;

    public WebbedEffect() {
        super(MobEffectCategory.HARMFUL, WEB);
        AttributeModifier.Operation total = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Enhanced.id("webbed_movement"), -0.85, total);
        addAttributeModifier(Attributes.FLYING_SPEED, Enhanced.id("webbed_flying"), -0.85, total);
        addAttributeModifier(Attributes.JUMP_STRENGTH, Enhanced.id("webbed_jump"), -1.0, total);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            // Two silver motes circling the body.
            double radius = entity.getBbWidth() * 0.5 + 0.2;
            double y = entity.getY() + entity.getBbHeight() * (0.3 + 0.4 * ((entity.tickCount / 4) % 2));
            for (int i = 0; i < 2; i++) {
                double angle = entity.tickCount * 0.4 + i * Mth.PI;
                level.sendParticles(new EnergyParticleOptions(WEB, 0.6F), entity.getX() + Math.cos(angle) * radius, y,
                        entity.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }
}
